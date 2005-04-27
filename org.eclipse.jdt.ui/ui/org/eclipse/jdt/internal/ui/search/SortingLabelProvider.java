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

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.IColorProvider;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.JavaElementLabels;

public class SortingLabelProvider extends SearchLabelProvider implements IColorProvider {
	public static final int SHOW_ELEMENT_CONTAINER= 1; // default
	public static final int SHOW_CONTAINER_ELEMENT= 2;
	public static final int SHOW_PATH= 3;
	
	public SortingLabelProvider(JavaSearchResultPage page) {
		super(page);
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
		return getLabelWithCounts(element, internalGetText(element));
	}

	private String internalGetText(Object o) {
		if (o instanceof IImportDeclaration)
			o= ((IImportDeclaration)o).getParent().getParent();

		String text= super.getText(o);
		if (text != null && (text.length() > 0))
			return text;
		return getParticipantText(o);	
	}

	public void setOrder(int orderFlag) {
		long flags= DEFAULT_SEARCH_TEXTFLAGS;
		if (orderFlag == SHOW_ELEMENT_CONTAINER)
			flags |= JavaElementLabels.F_POST_QUALIFIED | JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.I_POST_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
							| JavaElementLabels.T_POST_QUALIFIED | JavaElementLabels.D_POST_QUALIFIED | JavaElementLabels.CF_POST_QUALIFIED  | JavaElementLabels.CU_POST_QUALIFIED;
			
		else if (orderFlag == SHOW_CONTAINER_ELEMENT)
			flags |= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
				| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
		else if (orderFlag == SHOW_PATH) {
			flags |= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
				| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
			flags |= JavaElementLabels.PREPEND_ROOT_PATH;
		}
		setTextFlags(flags);
	}
}
