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

import org.eclipse.swt.graphics.Image;

import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredJavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredString;
import org.eclipse.jdt.internal.ui.viewsupport.IRichLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredString.Style;

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

	private String getLineNumberLabel(JavaElementLine element) {
		return Messages.format(SearchMessages.OccurrencesSearchLabelProvider_line_number, new Integer(element.getLine()));
	}
	
	private String internalGetText(Object element) {
		JavaElementLine jel= (JavaElementLine) element;
		return getLineNumberLabel(jel) + jel.getLineContents();
	}
	
	private ColoredString internalGetRichText(Object element) {
		JavaElementLine jel= (JavaElementLine) element;

		String lineNumberString= getLineNumberLabel(jel);
		
		Style highlightStyle= ColoredJavaElementLabels.HIGHLIGHT_STYLE;
		if (jel.getFlags() == IOccurrencesFinder.F_WRITE_OCCURRENCE) {
			highlightStyle= ColoredJavaElementLabels.HIGHLIGHT_WRITE_STYLE;
		}
		
		ColoredString res= new ColoredString();
		res.append(lineNumberString, ColoredJavaElementLabels.QUALIFIER_STYLE);
		res.append(jel.getLineContents());
		Match[] matches= getPage().getInput().getMatches(jel);
		for (int i= 0; i < matches.length; i++) {
			Match curr= matches[i];
			int offset= curr.getOffset() - jel.getLineStartOffset() + lineNumberString.length();
			int length= curr.getLength();
			
			if (offset >= 0 && (offset + length < res.length())) {
				res.colorize(offset, length, highlightStyle);
			}
		}
		return res;
	}
	
	public Image getImage(Object element) {
		if (element instanceof JavaElementLine) {
			int flags= ((JavaElementLine) element).getFlags();
			if (flags == IOccurrencesFinder.F_READ_OCCURRENCE) {
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_READACCESS);
			} else if (flags == IOccurrencesFinder.F_WRITE_OCCURRENCE) {
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_WRITEACCESS);
			} else if (flags == IOccurrencesFinder.F_EXCEPTION_DECLARATION) {
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			}
		}
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_OCCURRENCE);
	}
}
