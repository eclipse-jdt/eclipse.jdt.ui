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
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * Label provider for the hierarchy method viewers. 
 */
public class MethodsLabelProvider extends AppearanceAwareLabelProvider {

	private Color fResolvedBackground;
	
	private boolean fShowDefiningType;
	private TypeHierarchyLifeCycle fHierarchy;
	private MethodsViewer fMethodsViewer;

	public MethodsLabelProvider(TypeHierarchyLifeCycle lifeCycle, MethodsViewer methodsViewer) {
		super(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS);
		fHierarchy= lifeCycle;
		fShowDefiningType= false;
		fMethodsViewer= methodsViewer;
		fResolvedBackground= null;
	}
	
	public void setShowDefiningType(boolean showDefiningType) {
		fShowDefiningType= showDefiningType;
	}
	
	public boolean isShowDefiningType() {
		return fShowDefiningType;
	}	
			

	private IType getDefiningType(Object element) throws JavaModelException {
		int kind= ((IJavaElement) element).getElementType();
	
		if (kind != IJavaElement.METHOD && kind != IJavaElement.FIELD && kind != IJavaElement.INITIALIZER) {
			return null;
		}
		IType declaringType= ((IMember) element).getDeclaringType();
		if (kind != IJavaElement.METHOD) {
			return declaringType;
		}
		ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
		if (hierarchy == null) {
			return declaringType;
		}
		IMethod method= (IMethod) element;
		int flags= method.getFlags();
		if (Flags.isPrivate(flags) || Flags.isStatic(flags) || method.isConstructor()) {
			return declaringType;
		}
		IMethod res= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, declaringType, method.getElementName(), method.getParameterTypes(), false);
		if (res == null || method.equals(res)) {
			return declaringType;
		}
		return res.getDeclaringType();
	}

	/* (non-Javadoc)
	 * @see ILabelProvider#getText
	 */ 	
	public String getText(Object element) {
		String text= super.getText(element);
		if (fShowDefiningType) {
			try {
				IType type= getDefiningType(element);
				if (type != null) {
					StringBuffer buf= new StringBuffer(super.getText(type));
					buf.append(JavaElementLabels.CONCAT_STRING);
					buf.append(text);
					return buf.toString();			
				}
			} catch (JavaModelException e) {
			}
		}
		return text;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(Object element) {
		if (fMethodsViewer.isShowInheritedMethods() && element instanceof IMethod) {
			IMethod curr= (IMethod) element;
			IMember declaringType= curr.getDeclaringType();
			
			if (declaringType.equals(fMethodsViewer.getInput())) {
				if (fResolvedBackground == null) {
					Display display= Display.getCurrent();
					fResolvedBackground= display.getSystemColor(SWT.COLOR_DARK_BLUE);
				}
				return fResolvedBackground;
			}
		}
		return null;
	}
	
}
