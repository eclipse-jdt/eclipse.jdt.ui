/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0f
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ITypeNameRequestor;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.CorextMessages;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Manages a search cache for types in the workspace. Instead of returning objects of type <code>IType</code>
 * the methods of this class returns a list of the lightweight objects <code>TypeInfo</code>.
 * <P>
 * The AllTypesCache supports a synchronous and an asynchronous (background) mode.
 * In synchronous mode a dirty cache is rebuild as soon as <code>getAllTypes()</code> is called directly (or indirectly).
 * In asynchronous mode dirtying the cache starts a timer thread which rebuilds the cache after TIMEOUT seconds.
 * If the cache becomes dirty again while the background query is running, the query is aborted and the timer reset.
 * If <code>getAllTypes</code> is called before the background job has finished, getAllTypes waits
 * for the termination of the background query.
 * <P>
 * AllTypesCache automatically switches from synchronous to asynchronous mode when it detects
 * that the UI event loop is running.
 */
public class AllTypesCache {
	
	private static final int INITIAL_DELAY= 4000;
	private static final int TIMEOUT= 3000;
	private static final int INITIAL_SIZE= 2000;
	private static final int CANCEL_POLL_INTERVAL= 300;
	
	static class DelegatedProgressMonitor implements IProgressMonitor {
		private IProgressMonitor fDelegate;
		private String fTaskName;
//		private String fSubTask;
		private int fTotalWork= IProgressMonitor.UNKNOWN;
		private double fWorked;
		
		public synchronized void beginTask(String name, int totalWork) {
			fTaskName= name;
			fTotalWork= totalWork;
			if (fDelegate != null)
				fDelegate.beginTask(name, totalWork);
		}
		public void done() {
		}
		public synchronized void internalWorked(double work) {
			fWorked+= work;
			if (fDelegate != null)
				fDelegate.internalWorked(work);
		}
		public boolean isCanceled() {
			return false;
		}
		public void setCanceled(boolean value) {
		}
		public synchronized void setTaskName(String name) {
			fTaskName= name;
			if (fDelegate != null)
				fDelegate.setTaskName(name);
		}
		public synchronized void subTask(String name) {
//			fSubTask= name;
			if (fDelegate != null)
				fDelegate.subTask(name);
		}
		public void worked(int work) {
			internalWorked(work);
		}
		synchronized void setDelegate(IProgressMonitor delegate) {
			fDelegate= delegate;
			if (fDelegate != null) {
				if (fTaskName != null) {
					fDelegate.beginTask(fTaskName, fTotalWork);
					fDelegate.internalWorked(fWorked);
				}				
			} else {
				fTaskName= null;
				fTotalWork= IProgressMonitor.UNKNOWN;
				fWorked= 0.0;
			}
		}
	}

	/* debugging flag to enable tracing */
	private static final boolean TRACING;
	static {
		String value= Platform.getDebugOption("org.eclipse.jdt.ui/debug/allTypesCache"); //$NON-NLS-1$
		TRACING= value != null && value.equalsIgnoreCase("true"); //$NON-NLS-1$
	}
	
	static class TypeCacher extends Thread {
		
		private int fDelay;
		private int fSizeHint;
		private volatile boolean fRestart;
		private volatile boolean fAbort;
		
		TypeCacher(int sizeHint, int delay) {
			super("All Types Caching"); //$NON-NLS-1$
			fSizeHint= sizeHint;
			fDelay= delay;
			setPriority(Thread.NORM_PRIORITY - 1);
		}
		
		void restart() {
			fRestart= true;
			interrupt();
		}
		
		public void abort() {
			fRestart= fAbort= true;
			interrupt();
		}
		
		public void run() {
			// we loop until we got a complete result
			while (!fAbort) {
				if (fDelay > 0) {
					try {
						Thread.sleep(fDelay);
					} catch (InterruptedException e) {
						if (fAbort)
							break;
						continue;	// restart timer
					}
				}
				
				fRestart= false;
				Collection searchResult= doSearchTypes();
				if (searchResult != null) {
					if (!fAbort && !fRestart) {
						TypeInfo[] result= (TypeInfo[]) searchResult.toArray(new TypeInfo[searchResult.size()]);
						Arrays.sort(result, fgTypeNameComparator);
						setCache(result);
					}
					if (!fRestart)
						break;
				}
			}
		}
		
		private Collection doSearchTypes() {
			
			if (ResourcesPlugin.getWorkspace() == null)
				return null;
			
			final ArrayList typesFound= new ArrayList(fSizeHint);

			class RequestorAbort extends Error { }
		
			ITypeNameRequestor requestor= new TypeInfoRequestor(typesFound) {
				protected boolean inScope(char[] packageName) {
					if (fRestart)
						throw new RequestorAbort();
					return super.inScope(packageName);
				}
			};

			try {
				if (! search(requestor, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null))
					return null;
			} catch (RequestorAbort e) {
				// query cancelled
				return null;
			}
			return typesFound;
		}
	}
	
	/**
	 * The lock for synchronizing all activity in the AllTypesCache.
	 */
	private static Object fgLock= new Object();
		private volatile static boolean fgIsLocked;
		private volatile static TypeCacher fgTypeCacherThread;
		private static TypeInfo[] fgTypeCache;
		private static int fgSizeHint= INITIAL_SIZE;
		private volatile static boolean fgTerminated;	
		private volatile static boolean fgAsyncMode;		// if true cache updates are run via a background thread
	
	
	private static int fgNumberOfCacheFlushes;
	private static DelegatedProgressMonitor fDelegatedProgressMonitor= new DelegatedProgressMonitor();	
	private static TypeCacheDeltaListener fgDeltaListener;

	/**
	 * A comparator for simple type names
	 */
	private static Comparator fgTypeNameComparator= new Comparator() {
		public int compare(Object o1, Object o2) {
			return ((TypeInfo)o1).getTypeName().compareTo(((TypeInfo)o2).getTypeName());
		}
	};
		
	/**
	 * Returns all types in the given scope.
	 * @param kind IJavaSearchConstants.CLASS, IJavaSearchConstants.INTERFACE
	 * or IJavaSearchConstants.TYPE
	 * @param typesFound The resulting <code>TypeInfo</code> elements are added to this collection
	 */		
	public static void getTypes(IJavaSearchScope scope, int kind, IProgressMonitor monitor, Collection typesFound) throws JavaModelException {
		
		TypeInfo[] allTypes= getAllTypes(monitor);
		
		boolean isWorkspaceScope= scope.equals(SearchEngine.createWorkspaceScope());
		boolean isBoth= (kind == IJavaSearchConstants.TYPE);
		boolean isInterface= (kind == IJavaSearchConstants.INTERFACE);
		
		for (int i= 0; i < allTypes.length; i++) {
			TypeInfo info= fgTypeCache[i];
			if (isWorkspaceScope || info.isEnclosed(scope)) {
				if (isBoth || (isInterface == info.isInterface())) {
					typesFound.add(info);
				}
			}
		}
	}
	
	private static void setCache(TypeInfo[] cache) {
		synchronized(fgLock) {
			fgTypeCache= cache;
			if (fgTypeCache != null)
				fgSizeHint= fgTypeCache.length;
			fgTypeCacherThread= null;
			fgLock.notifyAll();
		}		
	}

	/**
	 * Returns all types in the workspace. The returned array must not be
	 * modified. The elements in the array are sorted by simple type name.
	 */
	public static TypeInfo[] getAllTypes(IProgressMonitor monitor) {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		monitor.beginTask("", 10); //$NON-NLS-1$
		SubProgressMonitor checkingMonitor = new SubProgressMonitor(monitor, 1);
		SubProgressMonitor searchingMonitor= new SubProgressMonitor(monitor, 9);
		forceDeltaComplete(checkingMonitor);
		searchingMonitor.setTaskName(CorextMessages.getString("AllTypesCache.searching")); //$NON-NLS-1$
		
		synchronized(fgLock) {
			try {
				if (fgTypeCache == null) {
					// cache is empty
					
					if (fgTypeCacherThread == null) {
						// cache synchroneously
						ArrayList searchResult= new ArrayList(fgSizeHint);
						try {
							fgIsLocked= true;
							if (search(new TypeInfoRequestor(searchResult), IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, searchingMonitor)) {
								TypeInfo[] result= (TypeInfo[]) searchResult.toArray(new TypeInfo[searchResult.size()]);
								Arrays.sort(result, fgTypeNameComparator);
								fgTypeCache= result;
								fgSizeHint= result.length;
							}
						} finally {
							fgIsLocked= false;
						}	
					} else {
						fDelegatedProgressMonitor.setDelegate(searchingMonitor);
						// wait for the thread to finished
						try {
							while (fgTypeCache == null) {
								fgLock.wait(CANCEL_POLL_INTERVAL);	// poll for cancel
								if (searchingMonitor.isCanceled())
									throw new OperationCanceledException();
							}
						} catch (InterruptedException e) {
							JavaPlugin.log(e);
						} finally {
							fDelegatedProgressMonitor.setDelegate(null);						
						}
					}
				}
				
			} finally {
				monitor.done();
			}
			return fgTypeCache;
		}
	}
		
	/**
	 * Returns true if the type cache is up to date.
	 */
	public static boolean isCacheUpToDate(IProgressMonitor pm) {
		forceDeltaComplete(pm);
		return fgTypeCache != null;
	}
	
	private static void forceDeltaComplete(IProgressMonitor pm) {
		if (pm == null)
			pm= new NullProgressMonitor();
		if (JavaPlugin.USE_WORKING_COPY_OWNERS) {
			ICompilationUnit[] primaryWorkingCopies= JavaCore.getWorkingCopies(null);
			pm.beginTask("", primaryWorkingCopies.length); //$NON-NLS-1$
			pm.setTaskName(CorextMessages.getString("AllTypesCache.checking_type_cache")); //$NON-NLS-1$
			for (int i= 0; i < primaryWorkingCopies.length; i++) {
				ICompilationUnit curr= primaryWorkingCopies[i];
				try {
					JavaModelUtil.reconcile(curr);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
				pm.worked(1);
				if (pm.isCanceled())
					throw new OperationCanceledException();
			}
		} else {
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.worked(1);
		}
		pm.done();
	}
	
	/**
	 * Returns a hint for the number of all types in the workspace.
	 */
	public static int getNumberOfAllTypesHint() {
		return fgSizeHint;
	}
	
	
	public static void forceCacheFlush() {
		if (fgTerminated)
			return;
		if (TRACING) {
			System.out.println("All Types Cache -- cache flushed."); //$NON-NLS-1$
		}
		
		synchronized(fgLock) {
			fgTypeCache= null;
			fgNumberOfCacheFlushes++;
			
			if (fgTypeCacherThread != null) {
				// if caching thread is already running, restart it
				fgTypeCacherThread.restart();
			} else {
				if (fgAsyncMode) {	// start thread only if we are in background mode
					fgTypeCacherThread= new TypeCacher(fgSizeHint, TIMEOUT);
					fgTypeCacherThread.start();
				}
			}
		}
	}
	
	
	private static class TypeCacheDeltaListener implements IElementChangedListener {
		
		/*
		 * @see IElementChangedListener#elementChanged
		 */
		public void elementChanged(ElementChangedEvent event) {
			if (fgTerminated)
				return;
			boolean needsFlushing= processDelta(event.getDelta());
			if (needsFlushing) {
				forceCacheFlush();
			}
		}
		
		/*
		 * returns true if the cache needs to be flushed
		 */
		private boolean processDelta(IJavaElementDelta delta) {
			IJavaElement elem= delta.getElement();
			boolean isAddedOrRemoved= (delta.getKind() != IJavaElementDelta.CHANGED)
			 || (delta.getFlags() & (IJavaElementDelta.F_ADDED_TO_CLASSPATH | IJavaElementDelta.F_REMOVED_FROM_CLASSPATH)) != 0;
			
			switch (elem.getElementType()) {
				case IJavaElement.JAVA_MODEL:
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.PACKAGE_FRAGMENT:
				case IJavaElement.CLASS_FILE:
				case IJavaElement.TYPE: // type children can be inner classes
					if (isAddedOrRemoved) {
						return true;
					}				
					return processChildrenDelta(delta);
				case IJavaElement.COMPILATION_UNIT: // content change means refresh from local
					if (!JavaModelUtil.isPrimary((ICompilationUnit) elem)) {
						return false;
					}

					if (((ICompilationUnit) elem).isWorkingCopy() && !JavaPlugin.USE_WORKING_COPY_OWNERS) {
						return false;
					}
					if (isAddedOrRemoved || isPossibleStructuralChange(delta.getFlags())) {
						return true;
					}
					return processChildrenDelta(delta);
				default:
					// fields, methods, imports ect
					return false;
			}	
		}
		
		private boolean isPossibleStructuralChange(int flags) {
			return (flags & (IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_FINE_GRAINED)) == IJavaElementDelta.F_CONTENT;
		}		
		
		private boolean processChildrenDelta(IJavaElementDelta delta) {
			IJavaElementDelta[] children= delta.getAffectedChildren();
			for (int i= 0; i < children.length; i++) {
				if (processDelta(children[i])) {
					return true;
				}
			}
			return false;
		}
	}
	

	/**
	 * Gets the number of times the cache was flushed. Used for testing.
	 * @return Returns a int
	 */
	public static int getNumberOfCacheFlushes() {
		return fgNumberOfCacheFlushes;
	}

	/**
	 * Returns all types for a given name in the scope. The elements in the array are sorted by simple type name.
	 */
	public static TypeInfo[] getTypesForName(String simpleTypeName, IJavaSearchScope searchScope, IProgressMonitor monitor) throws JavaModelException {
		Collection result= new ArrayList();
		Set namesFound= new HashSet();
		TypeInfo[] allTypes= AllTypesCache.getAllTypes(monitor); // all types in workspace, sorted by type name
		TypeInfo key= new UnresolvableTypeInfo("", simpleTypeName, null, true, null); //$NON-NLS-1$
		int index= Arrays.binarySearch(allTypes, key, fgTypeNameComparator);
		if (index >= 0 && index < allTypes.length) {
			for (int i= index - 1; i>= 0; i--) {
				TypeInfo curr= allTypes[i];
				if (simpleTypeName.equals(curr.getTypeName())) {
					if (!namesFound.contains(curr.getFullyQualifiedName()) && curr.isEnclosed(searchScope)) {
						result.add(curr);
						namesFound.add(curr.getFullyQualifiedName());
					}
				} else {
					break;
				}
			}
	
			for (int i= index; i < allTypes.length; i++) {
				TypeInfo curr= allTypes[i];
				if (simpleTypeName.equals(curr.getTypeName())) {
					if (!namesFound.contains(curr.getFullyQualifiedName()) && curr.isEnclosed(searchScope)) {
						result.add(curr);
						namesFound.add(curr.getFullyQualifiedName());
					}
				} else {
					break;
				}
			}
		}
		return (TypeInfo[]) result.toArray(new TypeInfo[result.size()]);		
	}
	
	/**
	 * Checks if the search index is up to date.
	 */
	public static boolean isIndexUpToDate() {
		class TypeFoundException extends Error {
		}
		ITypeNameRequestor requestor= new ITypeNameRequestor() {
			public void acceptClass(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
				throw new TypeFoundException();
			}
			public void acceptInterface(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
				throw new TypeFoundException();
			}
		};
		try {
			if (! search(requestor, IJavaSearchConstants.CANCEL_IF_NOT_READY_TO_SEARCH, null))
				return false;
		} catch (OperationCanceledException e) {
			return false;
		} catch (TypeFoundException e) {
		}
		return true;
	}

	/*
	 * A precanned type search.
	 * Returns false if a JavaModelException occured.
	 */
	static boolean search(ITypeNameRequestor requestor, int waitingPolicy, IProgressMonitor monitor) {
		long start= System.currentTimeMillis();
		try {
			if (monitor == null)
				monitor= fDelegatedProgressMonitor;
		
			new SearchEngine().searchAllTypeNames(ResourcesPlugin.getWorkspace(),
				null,
				null,
				IJavaSearchConstants.PATTERN_MATCH,
				IJavaSearchConstants.CASE_INSENSITIVE,
				IJavaSearchConstants.TYPE,
				SearchEngine.createWorkspaceScope(),
				requestor,
				waitingPolicy,
				monitor);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			if (TRACING) {
				System.out.println("All Types Cache -- building cache failed"); //$NON-NLS-1$
			}
			return false;
		}
		if (TRACING) {
			System.out.println("All Types Cache"); //$NON-NLS-1$
			System.out.println("\t cache populated in thread: " + Thread.currentThread().getName()); //$NON-NLS-1$
			System.out.println("\t time needed to perform search: " + (System.currentTimeMillis() - start) + "ms");  //$NON-NLS-1$//$NON-NLS-2$)
		}
		return true;
	}
	
	/**
	 * Initialize the AllTypesCache.
	 */
	public static void initialize() {
		
		fgDeltaListener= new TypeCacheDeltaListener();
		JavaCore.addElementChangedListener(fgDeltaListener);

		Display d= Display.getDefault();
		if (d != null)
			d.asyncExec(
				new Runnable() {
					public void run() {
						startBackgroundMode();
					}
				}
			);
	}
	
	private static void startBackgroundMode() {
		
		long start= System.currentTimeMillis();
		
		// forceDeltaComplete(new NullProgressMonitor());
		
		if (fgIsLocked) {
			fgAsyncMode= true;
			if (TRACING) {
				System.out.println("All Types Cache -- Background Mode started. Time needed " + (System.currentTimeMillis() - start) + "ms.");  //$NON-NLS-1$//$NON-NLS-2$
			}
			return;
		}
		
		synchronized(fgLock) {
			if (fgTypeCacherThread != null) {
				// there is already a thread
				if (fgTerminated) {
					if (fgTypeCacherThread != null)
						fgTypeCacherThread.abort();																		
				} else {
					// there is already a thread running
					// do nothing
				}							
			} else {
				if (fgTerminated) {
					// already terminated: do nothing
				} else {
					fgAsyncMode= true;
					if (fgTypeCache != null) {
						// the cache is already up to date
					} else {
						fgTypeCacherThread= new TypeCacher(fgSizeHint, INITIAL_DELAY);
						fgTypeCacherThread.start();
					}							
				}
			}
		}
		if (TRACING) {
			System.out.println("All Types Cache -- Background Mode started. Time needed: " + (System.currentTimeMillis() - start) + "ms");  //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	/**
	 * Terminate the service provided by AllTypesCache.
	 */
	public static void terminate() {
		
		if (fgDeltaListener != null)
			JavaCore.removeElementChangedListener(fgDeltaListener);
		fgDeltaListener= null;
		
		synchronized(fgLock) {
			fgTerminated= true;
			fgAsyncMode= false;
			
			if (fDelegatedProgressMonitor != null && !fDelegatedProgressMonitor.isCanceled())
				fDelegatedProgressMonitor.setCanceled(true);
			
			if (fgTypeCacherThread != null) {
				fgTypeCacherThread.abort();
				try {
					fgTypeCacherThread.join(1000);
				} catch (InterruptedException e) {
					JavaPlugin.log(e);
				}
				fgTypeCacherThread= null;
			}
		}
	}
}
