/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.JavaModelException;

public class GenerateConstructorUsingFieldsContentProvider implements ITreeContentProvider {

	private static final Object[] EMPTY= new Object[0];

	private List fFields;
	private List fSelected;

	public GenerateConstructorUsingFieldsContentProvider(List/*IVariableBinding*/ fields, List/*IVariableBinding*/ selected) throws JavaModelException {
		fFields= fields;
		fSelected= selected;
	}

	public boolean canMoveDown(List selectedElements) {
		int nSelected= selectedElements.size();
		for (int index= fFields.size() - 1; index >= 0 && nSelected > 0; index--) {
			if (!selectedElements.contains(fFields.get(index))) {
				return true;
			}
			nSelected--;
		}
		return false;
	}

	public boolean canMoveUp(List selected) {
		int nSelected= selected.size();
		for (int index= 0; index < fFields.size() && nSelected > 0; index++) {
			if (!selected.contains(fFields.get(index))) {
				return true;
			}
			nSelected--;
		}
		return false;
	}

	/*
	 * @see IContentProvider#dispose()
	 */
	public void dispose() {
	}

	public void down(List checked, CheckboxTreeViewer tree) {
		if (checked.size() > 0) {
			setElements(reverse(moveUp(reverse(fFields), checked)), tree);
			tree.reveal(checked.get(checked.size() - 1));
		}
		tree.setSelection(new StructuredSelection(checked));
	}

	/*
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parentElement) {
		return EMPTY;
	}

	/*
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		return fFields.toArray();
	}

	public List getFieldsList() {
		return fFields;
	}

	public Object[] getInitiallySelectedElements() {
		if (fSelected.isEmpty())
			return getElements(null);
		return fSelected.toArray();
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
	 * @see IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	private List moveUp(List elements, List move) {
		List result= new ArrayList(elements.size());
		Object floating= null;
		for (int index= 0; index < elements.size(); index++) {
			Object current= elements.get(index);
			if (move.contains(current)) {
				result.add(current);
			} else {
				if (floating != null) {
					result.add(floating);
				}
				floating= current;
			}
		}
		if (floating != null) {
			result.add(floating);
		}
		return result;
	}

	private List reverse(List list) {
		List reverse= new ArrayList(list.size());
		for (int index= list.size() - 1; index >= 0; index--) {
			reverse.add(list.get(index));
		}
		return reverse;
	}

	public void setElements(List elements, CheckboxTreeViewer tree) {
		fFields= new ArrayList(elements);
		if (tree != null)
			tree.refresh();
	}

	public void up(List checked, CheckboxTreeViewer tree) {
		if (checked.size() > 0) {
			setElements(moveUp(fFields, checked), tree);
			tree.reveal(checked.get(0));
		}
		tree.setSelection(new StructuredSelection(checked));
	}
}
