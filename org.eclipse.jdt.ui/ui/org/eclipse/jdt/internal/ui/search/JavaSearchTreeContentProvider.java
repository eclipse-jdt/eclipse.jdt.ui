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
public class JavaSearchTreeContentProvider extends JavaSearchContentProvider implements ITreeContentProvider {
	private TreeViewer fTreeViewer;
	private Map fChildrenMap;
	private StandardJavaElementContentProvider fContentProvider;
	
	static class FastJavaElementProvider extends StandardJavaElementContentProvider {
		public Object getParent(Object element) {
			return internalGetParent(element);
		}
	}
	
	JavaSearchTreeContentProvider(TreeViewer viewer) {
		fTreeViewer= viewer;
		fContentProvider= new FastJavaElementProvider();
	}

	public Object getParent(Object child) {
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
		Object[] elements= result.getElements();
		for (int i = 0; i < elements.length; i++) {
			insert(elements[i], false);
		}
	}


	protected void insert(Object child, boolean refreshViewer) {
		Object parent= getParent(child);
		while(parent != null) {
			if (insertChild(parent, child)) {
				if (refreshViewer)
					fTreeViewer.add(parent, child);
			} else {
				return;
			}
			child= parent;
			parent= getParent(child);
		}
		if (insertChild(fResult, child)) {
			if (refreshViewer)
				fTreeViewer.add(fResult, child);
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
		Object parent= getParent(child);
		if (fResult.getMatchCount(child) == 0) {
			fChildrenMap.remove(child);
			Set container= (Set) fChildrenMap.get(parent);
			if (container != null) {
				container.remove(child);
				if (container.size() == 0)
					remove(parent, refreshViewer);
			}
			if (refreshViewer) {
				fTreeViewer.remove(child);
			}
		} else {
			if (refreshViewer) {
				fTreeViewer.refresh(child);
			}
		}
	}

	
	public Object[] getChildren(Object parentElement) {
		Set children= (Set) fChildrenMap.get(parentElement);
		if (children == null)
			return EMPTY_ARR;
		return children.toArray();
	}
	
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}
	
	public void elementsChanged(Object[] updatedElements) {
		for (int i= 0; i < updatedElements.length; i++) {
			if (fResult.getMatchCount(updatedElements[i]) > 0)
				insert(updatedElements[i], true);
			else
				remove(updatedElements[i], true);
			
		}
	}
	
	public void clear() {
		fChildrenMap.clear();
		fTreeViewer.refresh();
	}

}
