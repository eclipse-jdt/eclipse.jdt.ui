/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
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
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.util.Assert;

/**
 * Manages a search cache for types in the workspace. Instead of returning objects of type <code>IType</code>
 * the methods of this class returns a list of the lightweight objects <code>TypeInfo</code>.
 */
public class AllTypesCache {
	
	private static final boolean DEBUG= true;
	
	private static final int INITIAL_DELAY= 6000;
	private static final int TIMEOUT= 3000;
	private static final int INITIAL_SIZE= 2000;

	static class TypeCacher extends Thread {
		
		private int fDelay;
		private int fSizeHint;
		private volatile boolean fReset;
		private volatile boolean fAbort;
		private IProgressMonitor fMonitor;
		
		TypeCacher(int sizeHint, int delay, IProgressMonitor monitor) {
			super("AllTypesCacher"); //$NON-NLS-1$
			fSizeHint= sizeHint;
			fDelay= delay;
			fMonitor= monitor;
			setPriority(Thread.NORM_PRIORITY-1);
		}
		
		void reset() {
			fReset= true;
			interrupt();
		}
		
		public void abort() {
			fReset= fAbort= true;
			interrupt();
		}
		
		public void run() {
			if (DEBUG) System.out.println("thread started"); //$NON-NLS-1$
			while (true) {
				if (fDelay > 0) {
					try {
						Thread.sleep(fDelay);
					} catch (InterruptedException e) {
						if (DEBUG) System.out.println("timer restarted"); //$NON-NLS-1$
						if (fAbort)
							return;
						continue;
					}
				}
								
				fReset= false;
				try {
					if (DEBUG) System.out.println("query started"); //$NON-NLS-1$
					Collection searchResult= doSearchTypes(SearchEngine.createWorkspaceScope(), IJavaSearchConstants.TYPE);
					if (searchResult != null) {
						TypeInfo[] result= (TypeInfo[]) searchResult.toArray(new TypeInfo[searchResult.size()]);
						Arrays.sort(result, getTypeNameComperator());
						if (!fAbort && !fReset)
							setCache(result);
						if (!fReset)
							return;
					}
					if (fAbort)
						return;
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		}

		private Collection doSearchTypes(IJavaSearchScope scope, int style) throws JavaModelException {

			final ArrayList typesFound= new ArrayList(fSizeHint);
			final TypeInfoFactory factory= new TypeInfoFactory();

			class RequestorAbort extends Error { }
		
			ITypeNameRequestor requestor= new ITypeNameRequestor() {
				public void acceptInterface(char[] packageName, char[] typeName, char[][] enclosingTypeNames,String path) {
					if (fReset)
						throw new RequestorAbort();
					typesFound.add(factory.create(packageName, typeName, enclosingTypeNames, true, path));
				}
				public void acceptClass(char[] packageName, char[] typeName, char[][] enclosingTypeNames, String path) {
					if (fReset)
						throw new RequestorAbort();
					typesFound.add(factory.create(packageName, typeName, enclosingTypeNames, false, path));
				}
			};

			if (fMonitor == null)
				fMonitor= new NullProgressMonitor();
			try {
				new SearchEngine().searchAllTypeNames(ResourcesPlugin.getWorkspace(),
					null,
					null,
					IJavaSearchConstants.PATTERN_MATCH,
					IJavaSearchConstants.CASE_INSENSITIVE,
					style,
					scope,
					requestor,
					IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
					fMonitor);
			} catch (RequestorAbort e) {
				if (DEBUG) System.out.println("  **** query cancelled"); //$NON-NLS-1$
				return null;
			}
			return typesFound;
		}

	}
	
	/**
	 * The lock for synchronizing all activity in the AllTypesCache.
	 */
	private static Object fgLock= new Object();
	private static TypeCacher fgTypeCacherThread;

	private static TypeInfo[] fgTypeCache;
	private static int fgSizeHint= INITIAL_SIZE;
	
	private static int fgNumberOfCacheFlushes;
	
	private static TypeCacheDeltaListener fgDeltaListener;

	private static class TypeNameComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			return ((TypeInfo)o1).getTypeName().compareTo(((TypeInfo)o2).getTypeName());
		}
	}
	private static Comparator fgTypeNameComparator= new TypeNameComparator();
	
	
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
		if (DEBUG) System.out.println("  setCache: " + fgSizeHint); //$NON-NLS-1$
	}

	/**
	 * Returns all types in the workspace. The returned array must not be
	 * modified. The elements in the array are sorted by simple type name.
	 */
	public static TypeInfo[] getAllTypes(IProgressMonitor monitor) throws JavaModelException {
		
		synchronized(fgLock) {
			
			if (fgTypeCache == null) {
				
				if (fgTypeCacherThread == null) {
					fgTypeCacherThread= new TypeCacher(fgSizeHint, 0, null);
					fgTypeCacherThread.start();
				}
				
				// wait for the thread to finished
				if (fgTypeCacherThread != null) {
					try {
						while (fgTypeCache == null)
							fgLock.wait();
					} catch (InterruptedException e) {
						JavaPlugin.log(e);
					}
				}
				Assert.isNotNull(fgTypeCache);
			}
			
			if (monitor != null)
				monitor.done();
			
			return fgTypeCache;
		}
	}
	
	/**
	 * Returns true if the type cache is up to date.
	 */
	public static boolean isCacheUpToDate() {
		return fgTypeCache != null;
	}
	
	/**
	 * Returns a hint for the number of all types in the workspace.
	 */
	public static int getNumberOfAllTypesHint() {
		return fgSizeHint;
	}
	
	/**
	 * Returns a compartor that compares the simple type names
	 */
	public static Comparator getTypeNameComperator() {
		return fgTypeNameComparator;
	}	

	
	private static class TypeCacheDeltaListener implements IElementChangedListener {
		
		/*
		 * @see IElementChangedListener#elementChanged
		 */
		public void elementChanged(ElementChangedEvent event) {
			boolean needsFlushing= processDelta(event.getDelta());
			if (needsFlushing) {
				synchronized(fgLock) {
					fgTypeCache= null;
					fgNumberOfCacheFlushes++;
					
					if (fgTypeCacherThread != null) {
						fgTypeCacherThread.reset();
					} else {
						fgTypeCacherThread= new TypeCacher(fgSizeHint, TIMEOUT, null);
						fgTypeCacherThread.start();
					}
				}
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
		int index= Arrays.binarySearch(allTypes, key, AllTypesCache.getTypeNameComperator());
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
			new SearchEngine().searchAllTypeNames(ResourcesPlugin.getWorkspace(),
				null,
				null,
				IJavaSearchConstants.PATTERN_MATCH,
				IJavaSearchConstants.CASE_INSENSITIVE,
				IJavaSearchConstants.TYPE,
				SearchEngine.createWorkspaceScope(),
				requestor,
				IJavaSearchConstants.CANCEL_IF_NOT_READY_TO_SEARCH,
				new NullProgressMonitor());
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return false;
		} catch (OperationCanceledException e) {
			return false;			
		} catch (TypeFoundException e) {
		}
		return true;
	}

	public static void initialize() {
		fgTypeCacherThread= new TypeCacher(fgSizeHint, INITIAL_DELAY, null);
		fgTypeCacherThread.start();
		fgDeltaListener= new TypeCacheDeltaListener();
		JavaCore.addElementChangedListener(fgDeltaListener);
	}
	
	public static void terminate() {
		if (fgDeltaListener != null)
			JavaCore.removeElementChangedListener(fgDeltaListener);
		fgDeltaListener= null;
		synchronized(fgLock) {
			if (fgTypeCacherThread != null)
				fgTypeCacherThread.abort();
			fgTypeCacherThread= null;
		}
	}
}
