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

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeHierarchyChangedListener;
import org.eclipse.jdt.core.JavaModelException;

public class SuperTypeHierarchyCache {
	
	private static class HierarchyCacheEntry implements ITypeHierarchyChangedListener {
		
		private ITypeHierarchy fTypeHierarchy;
		private long fLastAccess;
		
		public HierarchyCacheEntry(ITypeHierarchy hierarchy) {
			fTypeHierarchy= hierarchy;
			fTypeHierarchy.addTypeHierarchyChangedListener(this);
			markAsAccessed();
		}
		
		public void typeHierarchyChanged(ITypeHierarchy typeHierarchy) {
			synchronized (fgHierarchyCache) {
				freeHierarchy();
			}
		}

		public ITypeHierarchy getTypeHierarchy() {
			return fTypeHierarchy;
		}

		public void markAsAccessed() {
			fLastAccess= System.currentTimeMillis();
		}
		
		public long getLastAccess() {
			return fLastAccess;
		}
		
		public void freeHierarchy() {
			fTypeHierarchy.removeTypeHierarchyChangedListener(this);
			fgHierarchyCache.remove(this);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "Superhierarchy of: " + fTypeHierarchy.getType().getElementName(); //$NON-NLS-1$
		}

	}
	

	private static final int CACHE_SIZE= 8;

	private static ArrayList fgHierarchyCache= new ArrayList(CACHE_SIZE);
	private static int fgCacheHits= 0;
	private static int fgCacheMisses= 0;
	
	/**
	 * Get a hierarchy for the given type
	 * @param type
	 * @return
	 * @throws JavaModelException
	 */
	public static ITypeHierarchy getTypeHierarchy(IType type) throws JavaModelException {
		return getTypeHierarchy(type, null);
	}

	/**
	 * Get a hierarchy for the given type
	 * @param type
	 * @param progressMonitor the progress monitor
	 * @return
	 * @throws JavaModelException
	 */
	public static ITypeHierarchy getTypeHierarchy(IType type, IProgressMonitor progressMonitor) throws JavaModelException {
		ITypeHierarchy hierarchy= findTypeHierarchyInCache(type);
		if (hierarchy == null) {
			fgCacheMisses++;
			hierarchy= type.newSupertypeHierarchy(progressMonitor);
			addTypeHierarchyToCache(hierarchy);
		} else {
			fgCacheHits++;
		}
		return hierarchy;
	}
	
	private static void addTypeHierarchyToCache(ITypeHierarchy hierarchy) {
		synchronized (fgHierarchyCache) {
			int nEntries= fgHierarchyCache.size();
			if (nEntries >= CACHE_SIZE) {
				// find obsolete entries or remove entry that was least recently accessed
				HierarchyCacheEntry oldest= null;
				ArrayList obsoleteHierarchies= new ArrayList(CACHE_SIZE);
				for (int i= 0; i < nEntries; i++) {
					HierarchyCacheEntry entry= (HierarchyCacheEntry) fgHierarchyCache.get(i);
					ITypeHierarchy curr= entry.getTypeHierarchy();
					if (!curr.exists() || hierarchy.contains(curr.getType())) {
						obsoleteHierarchies.add(entry);
					} else {
						if (oldest == null || entry.getLastAccess() < oldest.getLastAccess()) {
							oldest= entry;
						}
					}
				}
				if (!obsoleteHierarchies.isEmpty()) {
					for (int i= 0; i < obsoleteHierarchies.size(); i++) {
						((HierarchyCacheEntry) fgHierarchyCache.get(i)).freeHierarchy();
					}			
				} else if (oldest != null) {
					oldest.freeHierarchy();
				}
			}
			HierarchyCacheEntry newEntry= new HierarchyCacheEntry(hierarchy);
			fgHierarchyCache.add(newEntry);
		}
	}
	

	/**
	 * Check if the given type is in the hierarchy
	 * @param type
	 * @return Return <code>true</code> if a hierarchy for the given type is cached.
	 */	
	public static boolean hasInCache(IType type) {
		return findTypeHierarchyInCache(type) != null;
	}
	
			
	private static ITypeHierarchy findTypeHierarchyInCache(IType type) {
		synchronized (fgHierarchyCache) {
			for (int i= fgHierarchyCache.size() - 1; i>= 0; i--) {
				HierarchyCacheEntry curr= (HierarchyCacheEntry) fgHierarchyCache.get(i);
				ITypeHierarchy hierarchy= curr.getTypeHierarchy();
				if (!hierarchy.exists()) {
					curr.freeHierarchy();
				} else {
					if (hierarchy.contains(type)) {
						curr.markAsAccessed();
						return hierarchy;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Gets the number of times the hierarchy could be taken from the hierarchy.
	 * @return Returns a int
	 */
	public static int getCacheHits() {
		return fgCacheHits;
	}
	
	/**
	 * Gets the number of times the hierarchy was build. Used for testing.
	 * @return Returns a int
	 */	
	public static int getCacheMisses() {
		return fgCacheMisses;
	}
}
