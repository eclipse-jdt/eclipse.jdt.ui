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
package org.eclipse.jdt.internal.ui.search;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
/**
 * @author Thomas Mäder
 *  
 */
public class JavaSearchTreeContentProvider2 extends JavaSearchContentProvider implements ITreeContentProvider {
	private TreeViewer fTreeViewer;
	private Map fChildrenMap;
	private StandardJavaElementContentProvider fContentProvider;
	
	static class FastJavaElementProvider extends StandardJavaElementContentProvider {
		public Object getParent(Object element) {
			return internalGetParent(element);
		}
	}
	
	JavaSearchTreeContentProvider2(TreeViewer viewer) {
		fTreeViewer= viewer;
		fContentProvider= new FastJavaElementProvider();
	}

	Object internalGetParent(Object child) {
		if (child instanceof IProject || child instanceof IJavaProject)
			return null;
		return fContentProvider.getParent(child);
		
	}
	
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}
	

	protected void initialize(JavaSearchResult result) {
		super.initialize(result);
		fChildrenMap= new HashMap();
		if (result != null) {
			Object[] elements= result.getElements();
			for (int i = 0; i < elements.length; i++) {
				insert(elements[i], false);
			}
		}
	}


	protected void insert(Object child, boolean refreshViewer) {
		Object parent= internalGetParent(child);
		while(parent != null) {
			if (insertChild(parent, child)) {
				if (refreshViewer) {
					Object parent2= getParent(child);
					while (parent2 != null && fTreeViewer.testFindItem(parent2) == null)
						parent2= getParent(parent2);
					if (parent2 != null)
						fTreeViewer.refresh(parent2);
					else
						fTreeViewer.refresh();
				}
			} else {
				fTreeViewer.refresh(getParent(child));
				return;
			}
			child= parent;
			parent= internalGetParent(child);
		}
		if (insertChild(fResult, child)) {
			if (refreshViewer)
				fTreeViewer.refresh(fResult);
		}
	}	
	
	/**
	 * returns true if the child already was a child of parent.
	 * @param parent
	 * @param child
	 * @return
	 */
	private boolean insertChild(Object parent, Object child) {
		Set children= (Set) fChildrenMap.get(parent);
		if (children == null) {
			children= new HashSet();
			fChildrenMap.put(parent, children);
		}
		return children.add(child);
	}
	
	protected void remove(Object child, boolean refreshViewer) {
		Object parent= internalGetParent(child);
		if (fResult.getMatchCount(child) == 0) {
			if (fChildrenMap.get(child) == null) {
				Set container= (Set) fChildrenMap.get(parent);
				if (container != null) {
					container.remove(child);
					if (container.size() == 0) {
						fChildrenMap.remove(parent);
						remove(parent, refreshViewer);
					}
				}
			}
			if (refreshViewer) {
				fTreeViewer.refresh(getParent(child));
			}
		} else {
			if (refreshViewer) {
				fTreeViewer.refresh(getParent(child));
			}
		}
	}

	
	public Object[] getChildren(Object parentElement) {
		Set children= (Set) fChildrenMap.get(parentElement);
		if (children == null)
			return EMPTY_ARR;
		HashSet filteredChildren= new HashSet(children.size());
		for (Iterator rawElements = children.iterator(); rawElements.hasNext();) {
 			Object element = rawElements.next();
			if (getRawChildrenCount(element) != 1 || fResult.getMatchCount(element) != 0)
				filteredChildren.add(element);
			else {
				Object[] recursiveChildren= getChildren(element);
				for (int i = 0; i < recursiveChildren.length; i++) {
					filteredChildren.add(recursiveChildren[i]);
				}
			}
		}
		return filteredChildren.toArray();
	}
	
	public boolean hasChildren(Object element) {
		return getRawChildrenCount(element) > 0;
	}
	
	public void elementsChanged(Object[] updatedElements) {
		if (fResult != null) {
			for (int i= 0; i < updatedElements.length; i++) {
				if (fResult.getMatchCount(updatedElements[i]) > 0)
					insert(updatedElements[i], true);
				else
					remove(updatedElements[i], true);
			}
		}
	}
	
	public void clear() {
		fChildrenMap.clear();
		fTreeViewer.refresh();
	}

	public Object getParent(Object element) {
		Object rawParent= internalGetParent(element);
		if (getRawChildrenCount(rawParent) == 1)
			return getParent(rawParent);
		return rawParent;
	}

	/**
	 * @param rawParent
	 * @return
	 */
	private int getRawChildrenCount(Object rawParent) {
		Set children= (Set) fChildrenMap.get(rawParent);
		if (children == null)
			return 0;
		return children.size();

	}


}
