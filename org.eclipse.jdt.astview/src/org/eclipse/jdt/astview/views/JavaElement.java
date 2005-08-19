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

package org.eclipse.jdt.astview.views;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.ui.JavaElementLabels;



public class JavaElement extends ASTAttribute {

	private static final long LABEL_OPTIONS=
		JavaElementLabels.F_APP_TYPE_SIGNATURE | JavaElementLabels.M_PARAMETER_TYPES | 
		JavaElementLabels.M_APP_RETURNTYPE | JavaElementLabels.ALL_FULLY_QUALIFIED |
		JavaElementLabels.T_TYPE_PARAMETERS |JavaElementLabels.USE_RESOLVED;
	
	private final IJavaElement fJavaElement;
	private final Object fParent;

	public JavaElement(Object parent, IJavaElement javaElement) {
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
			return "> " + classname.substring(classname.lastIndexOf('.') + 1) + ": " //$NON-NLS-1$ //$NON-NLS-2$
					+ JavaElementLabels.getElementLabel(fJavaElement, LABEL_OPTIONS)
					+ (fJavaElement.exists() ? "" : " (does not exist)");  //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	public Image getImage() {
		return null;
		//TODO: looks ugly when not all nodes have an icon
//		return new JavaElementImageProvider().getImageLabel(fJavaElement, JavaElementImageProvider.SMALL_ICONS | JavaElementImageProvider.OVERLAY_ICONS);
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		
		JavaElement other= (JavaElement) obj;
		if (fParent == null) {
			if (other.fParent != null)
				return false;
		} else if (! fParent.equals(other.fParent)) {
			return false;
		}
		
		if (fJavaElement == null) {
			if (other.fJavaElement != null)
				return false;
		} else if (! fJavaElement.equals(other.fJavaElement)) {
			return false;
		}
		
		return true;
	}
	
	/*
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0) + (fJavaElement != null ? fJavaElement.hashCode() : 0);
	}
}
