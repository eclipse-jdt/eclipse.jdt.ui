/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

/**
 * Manages a search cache for types in the workspace. Instead of returning objects of type <code>IType</code>
 * the methods of this class returns a list of the lightweight objects <code>TypeInfo</code>.
 */
public class AllTypesCache {
	
	private static TypeInfo[] fgTypeCache= null;
	private static int fgSizeHint= 2000;
	
	private static int fgNumberOfCacheFlushes= 0;

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
			TypeInfo info= (TypeInfo) fgTypeCache[i];
			if (isWorkspaceScope || info.isEnclosed(scope)) {
				if (isBoth || (isInterface == info.isInterface())) {
					typesFound.add(info);
				}
			}
		}
	}
	

	/**
	 * Returns all types in the workspace. The returned array must not be
	 * modified. The elements in the array are sorted by simple type name.
	 */
	public static synchronized TypeInfo[] getAllTypes(IProgressMonitor monitor) throws JavaModelException { 	
		if (fgTypeCache == null) {
			ArrayList searchResult= new ArrayList(fgSizeHint);
			doSearchTypes(SearchEngine.createWorkspaceScope(), IJavaSearchConstants.TYPE, monitor, searchResult);
			if (monitor != null && monitor.isCanceled()) {
				return null;
			}
			monitor= null; // prevents duplicated invocation of monitor.done
			fgTypeCache= (TypeInfo[]) searchResult.toArray(new TypeInfo[searchResult.size()]);
			Arrays.sort(fgTypeCache, getTypeNameComperator());
			fgSizeHint= fgTypeCache.length;
	
			JavaCore.addElementChangedListener(new TypeCacheDeltaListener());
		}
		if (monitor != null) {
			monitor.done();
		}		
		return fgTypeCache;
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


	private static void doSearchTypes(IJavaSearchScope scope, int style, IProgressMonitor monitor, Collection typesFound) throws JavaModelException {
		new SearchEngine().searchAllTypeNames(ResourcesPlugin.getWorkspace(),
			null,
			null,
			IJavaSearchConstants.PATTERN_MATCH,
			IJavaSearchConstants.CASE_INSENSITIVE,
			style,
			scope,
			new TypeInfoRequestor(typesFound),
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
			monitor);
	}
	
	private static class TypeCacheDeltaListener implements IElementChangedListener {
		
		/*
		 * @see IElementChangedListener#elementChanged
		 */
		public void elementChanged(ElementChangedEvent event) {
			boolean needsFlushing= processDelta(event.getDelta());
			if (needsFlushing) {
				fgTypeCache= null;
				fgNumberOfCacheFlushes++;
				JavaCore.removeElementChangedListener(this); // it's ok to remove listener while delta processing
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
					if (((ICompilationUnit) elem).isWorkingCopy()) {
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

}
