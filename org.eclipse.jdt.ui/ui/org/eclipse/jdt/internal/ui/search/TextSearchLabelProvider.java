/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledStringBuilder;

import org.eclipse.search.ui.text.AbstractTextSearchViewPage;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.viewsupport.ColoredJavaElementLabels;

public abstract class TextSearchLabelProvider extends LabelProvider {

	private AbstractTextSearchViewPage fPage;

	public TextSearchLabelProvider(AbstractTextSearchViewPage page) {
		fPage= page;
	}
	
	public AbstractTextSearchViewPage getPage() {
		return fPage;
	}
			
	protected final StyledStringBuilder getColoredLabelWithCounts(Object element, StyledStringBuilder coloredName) {
		String name= coloredName.toString();
		String decorated= getLabelWithCounts(element, name);
		if (decorated.length() > name.length()) {
			ColoredJavaElementLabels.decorateStyledString(coloredName, decorated, ColoredJavaElementLabels.COUNTER_STYLE);
		}
		return coloredName;
	}
	
	protected final String getLabelWithCounts(Object element, String elementName) {
		int matchCount= fPage.getInput().getMatchCount(element);
		if (matchCount < 2)
			return elementName;
		
		return Messages.format(SearchMessages.TextSearchLabelProvider_matchCountFormat, new String[] { elementName, String.valueOf(matchCount)});
	}
}
