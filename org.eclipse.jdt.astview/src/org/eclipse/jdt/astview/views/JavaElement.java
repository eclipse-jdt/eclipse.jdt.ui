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

package org.eclipse.jdt.astview.views;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;



public class JavaElement extends ASTAttribute {

	private static final long LABEL_OPTIONS=
		JavaElementLabels.F_APP_TYPE_SIGNATURE | JavaElementLabels.M_PARAMETER_TYPES | 
		JavaElementLabels.M_APP_RETURNTYPE | JavaElementLabels.ALL_FULLY_QUALIFIED;
	
	private final IJavaElement fJavaElement;
	private final Binding fParent;

	public JavaElement(Binding parent, IJavaElement javaElement) {
		fParent= parent;
		fJavaElement= javaElement;
	}
	
	public IJavaElement getJavaElement() {
		return fJavaElement;
	}
	
	public Object getParent() {
		return fParent;
	}

	public Object[] getChildren() {
		return EMPTY;
	}

	public String getLabel() {
		if (fJavaElement == null) {
			return ">java element: null"; //$NON-NLS-1$
		} else {
			String classname= fJavaElement.getClass().getName();
			return "> " + classname.substring(classname.lastIndexOf('.') + 1) + ": " + JavaElementLabels.getElementLabel(fJavaElement, LABEL_OPTIONS); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public Image getImage() {
		return null;
		//TODO: looks ugly when not all nodes have an icon
//		return new JavaElementImageProvider().getImageLabel(fJavaElement, JavaElementImageProvider.SMALL_ICONS | JavaElementImageProvider.OVERLAY_ICONS);
	}

}
