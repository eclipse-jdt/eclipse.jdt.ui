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

import org.eclipse.search.ui.text.AbstractTextSearchViewPage;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.internal.ui.search.TextSearchLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;


class NLSSearchResultLabelProvider2 extends TextSearchLabelProvider {
	
	private AppearanceAwareLabelProvider fLabelProvider;
	
	public NLSSearchResultLabelProvider2(AbstractTextSearchViewPage page) {
		super(page);
		fLabelProvider= new AppearanceAwareLabelProvider(JavaElementLabels.ALL_POST_QUALIFIED, 0);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.search.TextSearchLabelProvider#doGetText(java.lang.Object)
	 */
	protected String doGetText(Object element) {
		if (element instanceof FileEntry) {
			FileEntry fileEntry= (FileEntry) element;
			return fileEntry.getMessage();
		} else {
			return fLabelProvider.getText(element);
		}
	}
	
	/*
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof FileEntry)
			element= ((FileEntry) element).getPropertiesFile();
		
		return fLabelProvider.getImage(element);
	}
	
	/*
	 * @see org.eclipse.jface.viewers.LabelProvider#dispose()
	 */
	public void dispose() {
		fLabelProvider.dispose();
		fLabelProvider= null;
		super.dispose();
	}
}
