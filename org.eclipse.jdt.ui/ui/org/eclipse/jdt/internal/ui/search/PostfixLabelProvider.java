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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.search.*;

import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;

public class PostfixLabelProvider extends LabelProvider {
	private AppearanceAwareLabelProvider fLabelProvider;
	private ITreeContentProvider fContentProvider;
	private JavaSearchResultPage fPage;
	
	public PostfixLabelProvider(JavaSearchResultPage page) {
		fPage= page;
		fLabelProvider= new AppearanceAwareLabelProvider();
		fLabelProvider.addListener(new ILabelProviderListener() {
			public void labelProviderChanged(LabelProviderChangedEvent event) {
				LabelProviderChangedEvent evt= new LabelProviderChangedEvent(PostfixLabelProvider.this, event.getElements());
				fireLabelProviderChanged(evt);
			}
		});
		fContentProvider= new LevelTreeContentProvider.FastJavaElementProvider();
	}

	public void dispose() {
		fLabelProvider.dispose();
	}

	public Image getImage(Object element) {
		Image image= fLabelProvider.getImage(element);
		if (image != null)
			return image;
		ISearchUIParticipant participant= ((JavaSearchResult)fPage.getInput()).getSearchParticpant(element);
		if (participant != null)
			return participant.getImage(element);
		return null;
	}
	
	public String getText(Object element) {
		ITreeContentProvider provider= (ITreeContentProvider) fPage.getViewer().getContentProvider();
		Object visibleParent= provider.getParent(element);
		Object realParent= fContentProvider.getParent(element);
		Object lastElement= element;
		StringBuffer postfix= new StringBuffer();
		while (realParent != null && !(realParent instanceof IJavaModel) && !realParent.equals(visibleParent)) {
			if (!isSameInformation(realParent, lastElement))  {
				postfix.append(" - "); //$NON-NLS-1$
				postfix.append(internalGetText(realParent));
			}
			lastElement= realParent;
			realParent= fContentProvider.getParent(realParent);
		}
		int matchCount= fPage.getInput().getMatchCount(element);
		String text=internalGetText(element);
		if (matchCount == 0)
			return text+postfix;
		if (matchCount == 1)
			return text+ " (1 match)"+postfix; //$NON-NLS-1$ //$NON-NLS-2$
		return text + " (" + matchCount + " matches)"+postfix; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String internalGetText(Object element) {
		String text= fLabelProvider.getText(element);
		if (text != null && !"".equals(text)) //$NON-NLS-1$
			return text;
		ISearchUIParticipant participant= ((JavaSearchResult)fPage.getInput()).getSearchParticpant(element);
		if (participant != null)
			return participant.getText(element);
		return null;
	}

	private boolean isSameInformation(Object realParent, Object lastElement) {
		if (lastElement instanceof IType) {
			IType type= (IType)lastElement;
			if (realParent instanceof IClassFile) {
				if (type.getClassFile().equals(realParent))
					return true;
			} else if (realParent instanceof ICompilationUnit) {
				if (type.getCompilationUnit().equals(realParent))
					return true;
			}
		}
		return false;
	}

	public boolean isLabelProperty(Object element, String property) {
		return fLabelProvider.isLabelProperty(element, property);
	}
}
