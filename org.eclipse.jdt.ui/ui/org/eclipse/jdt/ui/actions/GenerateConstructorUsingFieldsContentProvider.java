/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;


class GenerateConstructorUsingFieldsContentProvider implements ITreeContentProvider {
	List fFieldsList;
	static final Object[] EMPTY= new Object[0];

	public GenerateConstructorUsingFieldsContentProvider(List fieldList) {
		fFieldsList= fieldList;
	}

	/*
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parentElement) {
		return EMPTY;
	}

	/*
	 * @see ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		return null;
	}

	/*
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	/*
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		return fFieldsList.toArray();
	}

	/*
	 * @see IContentProvider#dispose()
	 */
	public void dispose() {
	}

	/*
	 * @see IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	List moveUp(List elements, List move) {
		int nElements= elements.size();
		List res= new ArrayList(nElements);
		Object floating= null;
		for (int i= 0; i < nElements; i++) {
			Object curr= elements.get(i);
			if (move.contains(curr)) {
				res.add(curr);
			} else {
				if (floating != null) {
					res.add(floating);
				}
				floating= curr;
			}
		}
		if (floating != null) {
			res.add(floating);
		}
		return res;
	}

	List reverse(List p) {
		List reverse= new ArrayList(p.size());
		for (int i= p.size() - 1; i >= 0; i--) {
			reverse.add(p.get(i));
		}
		return reverse;
	}

	public void setElements(List elements, CheckboxTreeViewer tree) {
		fFieldsList= new ArrayList(elements);
		if (tree != null)
			tree.refresh();
	}

	public void up(List checkedElements, CheckboxTreeViewer tree) {
		if (checkedElements.size() > 0) {
			setElements(moveUp(fFieldsList, checkedElements), tree);
			tree.reveal(checkedElements.get(0));
		}
		tree.setSelection(new StructuredSelection(checkedElements));
	}

	public void down(List checkedElements, CheckboxTreeViewer tree) {
		if (checkedElements.size() > 0) {
			setElements(reverse(moveUp(reverse(fFieldsList), checkedElements)), tree);
			tree.reveal(checkedElements.get(checkedElements.size() - 1));
		}
		tree.setSelection(new StructuredSelection(checkedElements));
	}

	public boolean canMoveUp(List selectedElements) {
		int nSelected= selectedElements.size();
		int nElements= fFieldsList.size();
		for (int i= 0; i < nElements && nSelected > 0; i++) {
			if (!selectedElements.contains(fFieldsList.get(i))) {
				return true;
			}
			nSelected--;
		}
		return false;
	}

	public boolean canMoveDown(List selectedElements) {
		int nSelected= selectedElements.size();
		for (int i= fFieldsList.size() - 1; i >= 0 && nSelected > 0; i--) {
			if (!selectedElements.contains(fFieldsList.get(i))) {
				return true;
			}
			nSelected--;
		}
		return false;
	}

	public List getFieldsList() {
		return fFieldsList;
	}

}