/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.search.ui.text.AbstractTextSearchResult;

/**
 * TODO: this class should replace JavaSearchTableContentProvider
 * (must generalize type of fResult to AbstractTextSearchResult in JavaSearchContentProvider)
 */
public class TextSearchTableContentProvider implements IStructuredContentProvider {
	protected final Object[] EMPTY_ARRAY= new Object[0];
	private AbstractTextSearchResult fSearchResult;
	private TableViewer fTableViewer;

	/*
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof AbstractTextSearchResult)
			return ((AbstractTextSearchResult) inputElement).getElements();
		return EMPTY_ARRAY;
	}

	/*
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		//nothing
	}

	/*
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fTableViewer= (TableViewer) viewer;
		fSearchResult= (AbstractTextSearchResult) newInput;
	}

	public void elementsChanged(Object[] updatedElements) {
		//TODO: copied from JavaSearchTableContentProvider
		for (Object updatedElement : updatedElements) {
			if (fSearchResult.getMatchCount(updatedElement) > 0) {
				if (fTableViewer.testFindItem(updatedElement) != null) {
					fTableViewer.refresh(updatedElement);
				} else {
					fTableViewer.add(updatedElement);
				}
			} else {
				fTableViewer.remove(updatedElement);
			}
		}
	}

	public void clear() {
		//TODO: copied from JavaSearchTableContentProvider
		fTableViewer.refresh();
	}
}
