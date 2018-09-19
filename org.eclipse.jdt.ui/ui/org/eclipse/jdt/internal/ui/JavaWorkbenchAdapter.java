/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui;


import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * An imlementation of the IWorkbenchAdapter for IJavaElements.
 */
public class JavaWorkbenchAdapter implements IWorkbenchAdapter {

	protected static final Object[] NO_CHILDREN= new Object[0];

	private JavaElementImageProvider fImageProvider;

	public JavaWorkbenchAdapter() {
		fImageProvider= new JavaElementImageProvider();
	}

	@Override
	public Object[] getChildren(Object element) {
		IJavaElement je= getJavaElement(element);
		if (je instanceof IParent) {
			try {
				return ((IParent)je).getChildren();
			} catch(JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return NO_CHILDREN;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object element) {
		IJavaElement je= getJavaElement(element);
		if (je != null)
			return fImageProvider.getJavaImageDescriptor(je, JavaElementImageProvider.OVERLAY_ICONS | JavaElementImageProvider.SMALL_ICONS);

		return null;

	}

	@Override
	public String getLabel(Object element) {
		return JavaElementLabels.getTextLabel(getJavaElement(element), JavaElementLabels.ALL_DEFAULT);
	}

	@Override
	public Object getParent(Object element) {
		IJavaElement je= getJavaElement(element);
		return je != null ? je.getParent() :  null;
	}

	private IJavaElement getJavaElement(Object element) {
		if (element instanceof IJavaElement)
			return (IJavaElement)element;
		if (element instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput)element).getClassFile().getPrimaryElement();

		return null;
	}
}
