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
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

/**
 * @author Thomas Mäder
 *  
 */
public class LevelTreeContentProvider extends JavaSearchContentProvider implements ITreeContentProvider {
	private AbstractTreeViewer fTreeViewer;
	private Map fChildrenMap;
	private StandardJavaElementContentProvider fContentProvider;
	public static final int LEVEL_TYPE= 1;
	public static final int LEVEL_FILE= 2;
	public static final int LEVEL_PACKAGE= 3;
	public static final int LEVEL_PROJECT= 4;
	private static int[][] JAVA_ELEMENT_TYPES= {{IJavaElement.TYPE},
			{IJavaElement.CLASS_FILE, IJavaElement.COMPILATION_UNIT},
			{IJavaElement.PACKAGE_FRAGMENT},
			{IJavaElement.JAVA_PROJECT, IJavaElement.PACKAGE_FRAGMENT_ROOT},
			{IJavaElement.JAVA_MODEL}};
	private static int[][] RESOURCE_TYPES= {
			{}, 
			{IResource.FILE},
			{IResource.FOLDER}, 
			{IResource.PROJECT}, 
			{IResource.ROOT}};
	private static final int MAX_LEVEL= JAVA_ELEMENT_TYPES.length - 1;
	private int fCurrentLevel;
	static class FastJavaElementProvider extends StandardJavaElementContentProvider {
		public Object getParent(Object element) {
			return internalGetParent(element);
		}
	}

	public LevelTreeContentProvider(AbstractTreeViewer viewer, int level) {
		fTreeViewer= viewer;
		fCurrentLevel= level;
		fContentProvider= new FastJavaElementProvider();
	}

	public Object getParent(Object child) {
		Object possibleParent= internalGetParent(child);
		if (possibleParent instanceof IJavaElement) {
			IJavaElement javaElement= (IJavaElement) possibleParent;
			for (int j= fCurrentLevel; j < MAX_LEVEL + 1; j++) {
				for (int i= 0; i < JAVA_ELEMENT_TYPES[j].length; i++) {
					if (javaElement.getElementType() == JAVA_ELEMENT_TYPES[j][i]) {
						return null;
					}
				}
			}
		} else if (possibleParent instanceof IResource) {
			IResource resource= (IResource) possibleParent;
			for (int j= fCurrentLevel; j < MAX_LEVEL + 1; j++) {
				for (int i= 0; i < RESOURCE_TYPES[j].length; i++) {
					if (resource.getType() == RESOURCE_TYPES[j][i]) {
						return null;
					}
				}
			}
		}
		if (fCurrentLevel != LEVEL_FILE && child instanceof IType) {
			IType type= (IType) child;
			if (possibleParent instanceof ICompilationUnit
					|| possibleParent instanceof IClassFile)
				possibleParent= type.getPackageFragment();
		}
		return possibleParent;
	}

	private Object internalGetParent(Object child) {
		return fContentProvider.getParent(child);
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	protected synchronized void initialize(JavaSearchResult result) {
		super.initialize(result);
		fChildrenMap= new HashMap();
		if (result != null) {
			Object[] elements= result.getElements();
			for (int i= 0; i < elements.length; i++) {
				insert(elements[i], false);
			}
		}
	}

	protected void insert(Object child, boolean refreshViewer) {
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

	protected void remove(Object element, boolean refreshViewer) {
		// precondition here:  fResult.getMatchCount(child) <= 0
	
		if (hasChildren(element)) {
			if (refreshViewer)
				fTreeViewer.refresh(element);
		} else {
			if (fResult.getMatchCount(element) == 0) {
				fChildrenMap.remove(element);
				Object parent= getParent(element);
				if (parent != null) {
					removeFromSiblings(element, parent);
					remove(parent, refreshViewer);
				} else {
					removeFromSiblings(element, fResult);
					if (refreshViewer)
						fTreeViewer.refresh();
				}
			} else {
				if (refreshViewer) {
					fTreeViewer.refresh(element);
				}
			}
		}
	}

	private void removeFromSiblings(Object element, Object parent) {
		Set siblings= (Set) fChildrenMap.get(parent);
		if (siblings != null) {
			siblings.remove(element);
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

	public synchronized void elementsChanged(Object[] updatedElements) {
		if (fResult == null)
			return;
		for (int i= 0; i < updatedElements.length; i++) {
			if (fResult.getMatchCount(updatedElements[i]) > 0)
				insert(updatedElements[i], true);
			else
				remove(updatedElements[i], true);
		}
	}

	public void clear() {
		initialize(fResult);
		fTreeViewer.refresh();
	}

	public void setLevel(int level) {
		fCurrentLevel= level;
		Control control= fTreeViewer.getControl();
		if (control != null)
			control.setRedraw(false);
		initialize(fResult);
		fTreeViewer.refresh();
		if (control != null)
			control.setRedraw(true);
	}
}