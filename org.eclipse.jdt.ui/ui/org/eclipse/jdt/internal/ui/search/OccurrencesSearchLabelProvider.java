/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;

import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.internal.core.manipulation.search.IOccurrencesFinder;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ColoringLabelProvider;

import org.eclipse.jdt.internal.core.manipulation.search.SearchMessages;

class OccurrencesSearchLabelProvider extends TextSearchLabelProvider implements IStyledLabelProvider {

	public OccurrencesSearchLabelProvider(AbstractTextSearchViewPage page) {
		super(page);
	}

	@Override
	public String getText(Object element) {
		return getLabelWithCounts(element, internalGetText(element));
	}

	private String getLineNumberLabel(JavaElementLine element) {
		return Messages.format(SearchMessages.OccurrencesSearchLabelProvider_line_number, Integer.valueOf(element.getLineNumber() + 1));
	}

	private String internalGetText(Object element) {
		JavaElementLine jel= (JavaElementLine) element;
		return getLineNumberLabel(jel) + jel.getLineContents();
	}

	private StyledString internalGetRichText(Object element) {
		JavaElementLine jel= (JavaElementLine) element;

		String lineNumberString= getLineNumberLabel(jel);

		Styler highlightStyle= ColoringLabelProvider.HIGHLIGHT_STYLE;

		StyledString res= new StyledString();
		res.append(lineNumberString, StyledString.QUALIFIER_STYLER);
		res.append(jel.getLineContents());
		for (Match match : getPage().getInput().getMatches(jel)) {
			OccurrenceMatch curr= (OccurrenceMatch) match;
			int offset= curr.getOriginalOffset() - jel.getLineStartOffset() + lineNumberString.length();
			int length= curr.getOriginalLength();
			if (offset >= 0 && (offset + length <= res.length())) {
				if ((curr.getFlags() & IOccurrencesFinder.F_WRITE_OCCURRENCE) != 0) {
					res.setStyle(offset, length, ColoringLabelProvider.HIGHLIGHT_WRITE_STYLE);
				} else {
					res.setStyle(offset, length, highlightStyle);
				}
			}
		}
		return res;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof JavaElementLine) {
			int flags= ((JavaElementLine) element).getFlags();
			if ((flags & IOccurrencesFinder.F_WRITE_OCCURRENCE) != 0) {
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_WRITEACCESS);
			}
			if ((flags & IOccurrencesFinder.F_READ_OCCURRENCE) != 0) {
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_READACCESS);
			}
			if ((flags & IOccurrencesFinder.F_EXCEPTION_DECLARATION) != 0) {
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			}
		}
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_OCCURRENCE);
	}

	@Override
	public StyledString getStyledText(Object element) {
		return getColoredLabelWithCounts(element, internalGetRichText(element));
	}
}
