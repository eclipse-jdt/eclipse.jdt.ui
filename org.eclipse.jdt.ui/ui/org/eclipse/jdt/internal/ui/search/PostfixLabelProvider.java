/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.viewsupport.ColoredJavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredString;

public class PostfixLabelProvider extends SearchLabelProvider {
	private ITreeContentProvider fContentProvider;
	
	public PostfixLabelProvider(JavaSearchResultPage page) {
		super(page);
		fContentProvider= new LevelTreeContentProvider.FastJavaElementProvider();
	}

	public Image getImage(Object element) {
		Image image= super.getImage(element);
		if (image != null)
			return image;
		return getParticipantImage(element);
	}
	
	public String getText(Object element) {
		String labelWithCounts= getLabelWithCounts(element, internalGetText(element));
		return labelWithCounts + getQualification(element);
	}
	
	private String getQualification(Object element) {
		StringBuffer res= new StringBuffer();
		
		ITreeContentProvider provider= (ITreeContentProvider) fPage.getViewer().getContentProvider();
		Object visibleParent= provider.getParent(element);
		Object realParent= fContentProvider.getParent(element);
		Object lastElement= element;
		while (realParent != null && !(realParent instanceof IJavaModel) && !realParent.equals(visibleParent)) {
			if (!isSameInformation(realParent, lastElement))  {
				res.append(JavaElementLabels.CONCAT_STRING).append(internalGetText(realParent));
			}
			lastElement= realParent;
			realParent= fContentProvider.getParent(realParent);
		}
		return res.toString();
	}

	protected boolean hasChildren(Object element) {
		ITreeContentProvider contentProvider= (ITreeContentProvider) fPage.getViewer().getContentProvider();
		return contentProvider.hasChildren(element);
	}

	private String internalGetText(Object element) {
		String text= super.getText(element);
		if (text != null && text.length() > 0)
			return text;
		return getParticipantText(element);
	}

	private boolean isSameInformation(Object realParent, Object lastElement) {
		if (lastElement instanceof IType) {
			IType type= (IType) lastElement;
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.viewsupport.IRichLabelProvider#getRichTextLabel(java.lang.Object)
	 */
	public ColoredString getRichTextLabel(Object element) {
		ColoredString coloredString= getColoredLabelWithCounts(element, super.getRichTextLabel(element));
		coloredString.append(getQualification(element), ColoredJavaElementLabels.QUALIFIER_STYLE);
		return coloredString;
	}

}
