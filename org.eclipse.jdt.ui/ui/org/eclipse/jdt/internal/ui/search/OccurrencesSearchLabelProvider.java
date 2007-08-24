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

package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredJavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredString;
import org.eclipse.jdt.internal.ui.viewsupport.IRichLabelProvider;

import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;

import org.eclipse.swt.graphics.Image;

class OccurrencesSearchLabelProvider extends TextSearchLabelProvider implements IRichLabelProvider {
	
	public OccurrencesSearchLabelProvider(AbstractTextSearchViewPage page) {
		super(page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		return getLabelWithCounts(element, internalGetText(element)); 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.viewsupport.IRichLabelProvider#getRichTextLabel(java.lang.Object)
	 */
	public ColoredString getRichTextLabel(Object element) {
		return getColoredLabelWithCounts(element, internalGetRichText(element)); 
	}
	
	private String internalGetText(Object element) {
		JavaElementLine jel= (JavaElementLine) element;
		return jel.getLineContents();
	}
	
	private ColoredString internalGetRichText(Object element) {
		JavaElementLine jel= (JavaElementLine) element;
		ColoredString res= new ColoredString(jel.getLineContents());
		Match[] matches= getPage().getInput().getMatches(jel);
		for (int i= 0; i < matches.length; i++) {
			Match curr= matches[i];
			int offset= curr.getOffset() - jel.getLineStartOffset();
			int length= curr.getLength();
			
			if (offset >= 0 && (offset + length < res.length())) {
				res.colorize(offset, length, ColoredJavaElementLabels.HIGHLIGHT_STYLE);
			}
		}
		return res;
	}
	
	public Image getImage(Object element) {
		if (element instanceof OccurrencesGroupKey) {
			OccurrencesGroupKey group= (OccurrencesGroupKey) element;
			if (group.isVariable()) {
				if (group.isWriteAccess())
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_WRITEACCESS);
				else
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_READACCESS);
			}
			
		} else if (element instanceof ExceptionOccurrencesGroupKey) {
			ExceptionOccurrencesGroupKey group= (ExceptionOccurrencesGroupKey) element;
			if (group.isException())
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		}
		
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_OCCURRENCE);
	}
}
