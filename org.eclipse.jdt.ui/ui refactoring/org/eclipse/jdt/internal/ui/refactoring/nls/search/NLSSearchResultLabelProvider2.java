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

package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.graphics.Image;

import org.eclipse.search.ui.text.AbstractTextSearchViewPage;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.search.TextSearchLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredJavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredString;
import org.eclipse.jdt.internal.ui.viewsupport.IRichLabelProvider;


class NLSSearchResultLabelProvider2 extends TextSearchLabelProvider implements IRichLabelProvider {
	
	private AppearanceAwareLabelProvider fLabelProvider;
	
	public NLSSearchResultLabelProvider2(AbstractTextSearchViewPage page) {
		super(page);
		fLabelProvider= new AppearanceAwareLabelProvider(JavaElementLabels.ALL_POST_QUALIFIED, 0);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		return getLabelWithCounts(element, internalGetText(element).toString()); 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.viewsupport.IRichLabelProvider#getRichTextLabel(java.lang.Object)
	 */
	public ColoredString getRichTextLabel(Object element) {
		return getColoredLabelWithCounts(element, internalGetText(element)); 
	}
	
	private ColoredString internalGetText(Object element) {
		String description;
		ColoredString elementLabel;
		
		if (element instanceof FileEntry) {
			FileEntry fileEntry= (FileEntry) element;
			description= fileEntry.getMessage();
			elementLabel= getPropertiesName(fileEntry.getPropertiesFile());
		} else if (element instanceof CompilationUnitEntry) {
			CompilationUnitEntry cuEntry= (CompilationUnitEntry) element;
			description= cuEntry.getMessage();
			elementLabel= ColoredJavaElementLabels.getTextLabel(cuEntry.getCompilationUnit(), JavaElementLabels.ALL_POST_QUALIFIED | ColoredJavaElementLabels.COLORIZE);
		} else {
			description= NLSSearchMessages.NLSSearchResultLabelProvider2_undefinedKeys;
			elementLabel= ColoredJavaElementLabels.getTextLabel(element, JavaElementLabels.ALL_POST_QUALIFIED | ColoredJavaElementLabels.COLORIZE);
		}
		return new ColoredString(description).append(' ').append(elementLabel);
	}
	
	private ColoredString getPropertiesName(IFile propertiesFile) {
		String path= propertiesFile.getFullPath().removeLastSegments(1).makeRelative().toString();
		return new ColoredString(propertiesFile.getName()).append(" - " + path, ColoredJavaElementLabels.QUALIFIER_STYLE); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof FileEntry)
			element= ((FileEntry) element).getPropertiesFile();
		if (element instanceof CompilationUnitEntry)
			element= ((CompilationUnitEntry)element).getCompilationUnit();
		
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
