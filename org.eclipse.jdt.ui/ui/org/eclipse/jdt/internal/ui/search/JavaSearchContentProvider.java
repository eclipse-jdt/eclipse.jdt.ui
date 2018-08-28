/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.search.ui.text.AbstractTextSearchResult;

public abstract class JavaSearchContentProvider implements IStructuredContentProvider {
	protected final Object[] EMPTY_ARR= new Object[0];

	private AbstractTextSearchResult fResult;
	private JavaSearchResultPage fPage;

	JavaSearchContentProvider(JavaSearchResultPage page) {
		fPage= page;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		initialize((AbstractTextSearchResult) newInput);

	}

	protected void initialize(AbstractTextSearchResult result) {
		fResult= result;
	}

	public abstract void elementsChanged(Object[] updatedElements);
	public abstract void clear();

	@Override
	public void dispose() {
		// nothing to do
	}

	JavaSearchResultPage getPage() {
		return fPage;
	}

	AbstractTextSearchResult getSearchResult() {
		return fResult;
	}

}
