/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.search;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
/**
 * @author Thomas Mäder
 *  
 */
public class CountingTreeContentProvider extends JavaSearchContentProvider implements ITreeContentProvider {
	private TreeViewer fTreeViewer;
	private Map fChildrenMap;
	private StandardJavaElementContentProvider fContentProvider;
	
	private static final int GROUPING_THRESHOLD= 30;
	private static int[][] ELEMENT_TYPES= {
			{ IJavaElement.TYPE },
			{ IJavaElement.CLASS_FILE, IJavaElement.COMPILATION_UNIT },
			{ IJavaElement.PACKAGE_FRAGMENT },
			{ IJavaElement.JAVA_PROJECT, IJavaElement.PACKAGE_FRAGMENT_ROOT },
			{ IJavaElement.JAVA_MODEL },
	};
	private static final int MAX_LEVEL= ELEMENT_TYPES.length-1;
	private int fCurrentLevel= 0;
	
	static class FastJavaElementProvider extends StandardJavaElementContentProvider {
		public Object getParent(Object element) {
			return internalGetParent(element);
		}
	}
	CountingTreeContentProvider(TreeViewer viewer) {
		fTreeViewer= viewer;
		fContentProvider= new FastJavaElementProvider();
	}
	
	public Object getParent(Object child) {
		Object possibleParent= fContentProvider.getParent(child);
		if (possibleParent instanceof IJavaElement) {
			IJavaElement javaElement= (IJavaElement) possibleParent;
			for (int j= fCurrentLevel; j < MAX_LEVEL+1; j++) {
				for (int i= 0; i < ELEMENT_TYPES[j].length; i++) {
					if (javaElement.getElementType() == ELEMENT_TYPES[j][i]) {
						return null;
					}
				}
			}
		}
		return possibleParent;
	}
	
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}
	
	protected void initialize(JavaSearchResult result) {
		super.initialize(result);
		fCurrentLevel= 0;
		fChildrenMap= new HashMap();
		if (result != null) {
			Object[] elements= result.getElements();
			for (int i= 0; i < elements.length; i++) {
				insert(elements[i], false);
			}
		}
	}
	protected void insert(Object child, boolean refreshViewer) {
		try {
			Object parent= getParent(child);
			while (parent != null) {
				if (insertChild(parent, child)) {
					if (refreshViewer)
						fTreeViewer.add(parent, child);
				} else {
					if (refreshViewer)
						fTreeViewer.refresh(parent);
					return;
				}
				child= parent;
				parent= getParent(child);
			}
			if (insertChild(fResult, child)) {
				if (refreshViewer)
					fTreeViewer.add(fResult, child);
			}
		} finally {
			perhapsGroup(refreshViewer);
		}
	}
	/**
	 * returns true if the child already was a child of parent.
	 * 
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
			fChildrenMap.remove(parent);
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
	/**
	 * 
	 */
	private void perhapsGroup(boolean refreshViewer) {
		if (fCurrentLevel == MAX_LEVEL)
			return;
		Set children= (Set) fChildrenMap.get(fResult);
		if (children != null && children.size() > GROUPING_THRESHOLD) {
			fChildrenMap.remove(fResult);
			fCurrentLevel++;
			for (Iterator iter= children.iterator(); iter.hasNext();) {
				Object element= iter.next();
				Object parent= getParent(element);
				while (parent != null) {
					insertChild(parent, element);
					element= parent;
					parent= getParent(element);
				} 
				insertChild(fResult, element);
				
			}
			if (refreshViewer)
				fTreeViewer.refresh();
		}
	}
	
	public void clear() {
		fChildrenMap.clear();
		fTreeViewer.refresh();
	}
}
