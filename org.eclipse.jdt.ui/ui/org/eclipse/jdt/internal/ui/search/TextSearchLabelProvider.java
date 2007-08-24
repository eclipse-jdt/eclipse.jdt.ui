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

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.search.ui.text.AbstractTextSearchViewPage;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.viewsupport.ColoredJavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredString;

public abstract class TextSearchLabelProvider extends LabelProvider {

	private AbstractTextSearchViewPage fPage;

	public TextSearchLabelProvider(AbstractTextSearchViewPage page) {
		fPage= page;
	}
	
	public AbstractTextSearchViewPage getPage() {
		return fPage;
	}
			
	protected final ColoredString getColoredLabelWithCounts(Object element, ColoredString coloredName) {
		String name= coloredName.getString();
		String decorated= getLabelWithCounts(element, name);
		if (decorated.length() > name.length()) {
			ColoredJavaElementLabels.decorateColoredString(coloredName, decorated, ColoredJavaElementLabels.COUNTER_STYLE);
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
