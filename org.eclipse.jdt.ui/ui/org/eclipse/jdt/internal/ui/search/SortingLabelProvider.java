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

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.viewsupport.ColoredJavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredString;

public class SortingLabelProvider extends SearchLabelProvider {
	
	public static final int SHOW_ELEMENT_CONTAINER= 1; // default
	public static final int SHOW_CONTAINER_ELEMENT= 2;
	public static final int SHOW_PATH= 3;
	
	private static final long FLAGS_QUALIFIED= DEFAULT_SEARCH_TEXTFLAGS | JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED
		| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED | ColoredJavaElementLabels.COLORIZE;
	
	
	private int fCurrentOrder;
	
	public SortingLabelProvider(JavaSearchResultPage page) {
		super(page);
		fCurrentOrder= SHOW_ELEMENT_CONTAINER;
	}	

	public Image getImage(Object element) {
		Image image= null;
		if (element instanceof IJavaElement || element instanceof IResource)
			image= super.getImage(element);
		if (image != null)
			return image;
		return getParticipantImage(element);
	}
		
	public final String getText(Object element) {
		if (element instanceof IImportDeclaration)
			element= ((IImportDeclaration)element).getParent().getParent();
		
		String text= super.getText(element);
		if (text.length() > 0) {
			String labelWithCount= getLabelWithCounts(element, text);
			if (fCurrentOrder == SHOW_ELEMENT_CONTAINER) {
				labelWithCount += getPostQualification(element, text);
			}
			return labelWithCount;
		}
		return getParticipantText(element);	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider#getRichTextLabel(java.lang.Object)
	 */
	public ColoredString getRichTextLabel(Object element) {
		if (element instanceof IImportDeclaration)
			element= ((IImportDeclaration)element).getParent().getParent();
		
		ColoredString text= super.getRichTextLabel(element);
		if (text.length() > 0) {
			ColoredString countLabel= getColoredLabelWithCounts(element, text);
			if (fCurrentOrder == SHOW_ELEMENT_CONTAINER) {
				countLabel.append(getPostQualification(element, text.getString()), ColoredJavaElementLabels.QUALIFIER_STYLE);
			}
			return countLabel;
		}
		return new ColoredString(getParticipantText(element));	
	}

	private String getPostQualification(Object element, String text) {
		String textLabel= JavaElementLabels.getTextLabel(element, JavaElementLabels.ALL_POST_QUALIFIED);
		int indexOf= textLabel.indexOf(JavaElementLabels.CONCAT_STRING);
		if (indexOf != -1) {
			return textLabel.substring(indexOf);
		}
		return new String();
	}

	public void setOrder(int orderFlag) {
		fCurrentOrder= orderFlag;
		long flags= 0;
		if (orderFlag == SHOW_ELEMENT_CONTAINER)
			flags= DEFAULT_SEARCH_TEXTFLAGS;
		else if (orderFlag == SHOW_CONTAINER_ELEMENT)
			flags= FLAGS_QUALIFIED;
		else if (orderFlag == SHOW_PATH) {
			flags= FLAGS_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH;
		}
		setTextFlags(flags);
	}
}
