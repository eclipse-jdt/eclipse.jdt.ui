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

package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.search.TextSearchTableContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;


public class NLSSearchResultPage extends AbstractTextSearchViewPage {

	private TextSearchTableContentProvider fContentProvider;
	private NLSSearchEditorOpener fEditorOpener= new NLSSearchEditorOpener();

	public NLSSearchResultPage() {
		super(AbstractTextSearchViewPage.FLAG_LAYOUT_FLAT);
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#showMatch(org.eclipse.search.ui.text.Match,
	 *      int, int)
	 */
	protected void showMatch(Match match, int currentOffset, int currentLength, boolean activate) throws PartInitException {
		try {
			IEditorPart editor= fEditorOpener.open(match);
			if (editor != null && activate)
				editor.getEditorSite().getPage().activate(editor);
			if (editor instanceof ITextEditor) {
				ITextEditor textEditor= (ITextEditor) editor;
				textEditor.selectAndReveal(currentOffset, currentLength);
			}
		} catch (JavaModelException e1) {
			throw new PartInitException(e1.getStatus());
		}
	}
	
	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#elementsChanged(java.lang.Object[])
	 */
	protected void elementsChanged(Object[] objects) {
		if (fContentProvider != null)
			fContentProvider.elementsChanged(objects);
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#clear()
	 */
	protected void clear() {
		if (fContentProvider != null)
			fContentProvider.clear();
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTreeViewer(org.eclipse.jface.viewers.TreeViewer)
	 */
	protected void configureTreeViewer(TreeViewer viewer) {
		throw new IllegalStateException("Doesn't support tree mode."); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTableViewer(org.eclipse.jface.viewers.TableViewer)
	 */
	protected void configureTableViewer(TableViewer viewer) {
		viewer.setSorter(new NLSSorter());
		viewer.setLabelProvider(new NLSSearchResultLabelProvider2(this));
		fContentProvider= new TextSearchTableContentProvider();
		viewer.setContentProvider(fContentProvider);
	}
	
}
