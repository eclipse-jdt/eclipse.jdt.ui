/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.search.*;


public class DelegatingLabelProvider extends LabelProvider {

	private ILabelProvider fLabelProvider;
	private JavaSearchResultPage fPage;

	public DelegatingLabelProvider(JavaSearchResultPage page, ILabelProvider inner) {
		fPage= page;
		fLabelProvider= inner;
	}
	
	public ILabelProvider getLabelProvider() {
		return fLabelProvider;
	}

	public Image getImage(Object element) {
		Image image= null;
		if (element instanceof IJavaElement || element instanceof IResource)
			image= fLabelProvider.getImage(element);
		if (image != null)
			return image;
		ISearchUIParticipant participant= ((JavaSearchResult)fPage.getInput()).getSearchParticpant(element);
		if (participant != null)
			return participant.getImage(element);
		return null;
	}
	
	public String getText(Object element) {
		int matchCount= fPage.getInput().getMatchCount(element);
		String text= internalGetText(element);
		if (matchCount == 0)
			return text;
		if (matchCount == 1)
			return text;
		return text + " (" + matchCount + " matches)";
	}

	private String internalGetText(Object element) {
		String text= fLabelProvider.getText(element);
		if (text != null)
			return text;
		ISearchUIParticipant participant= ((JavaSearchResult)fPage.getInput()).getSearchParticpant(element);
		if (participant != null)
			return participant.getText(element);
		return null;
	}

	public void dispose() {
		fLabelProvider.dispose();
		super.dispose();
	}
}
