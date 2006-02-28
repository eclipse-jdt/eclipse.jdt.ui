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
package org.eclipse.jdt.internal.ui;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

public class InterfaceIndicatorLabelDecorator implements ILabelDecorator, ILightweightLabelDecorator {

	public InterfaceIndicatorLabelDecorator() {
	}

	/**
	 * {@inheritDoc}
	 */
	public Image decorateImage(Image image, Object element) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String decorateText(String text, Object element) {
		return text;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeListener(ILabelProviderListener listener) {
	}

	/**
	 * {@inheritDoc}
	 */
	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof ICompilationUnit) {
			ICompilationUnit unit= (ICompilationUnit)element;
			try {
				IType type= JavaElementUtil.getMainType(unit);
				if (type != null) {
					ImageDescriptor overlay= getOverlay(type);
					if (overlay != null) {
						decoration.addOverlay(overlay, IDecoration.TOP_LEFT);
					}
				}
			} catch (JavaModelException e) {
				return;
			}
		}
	}
	
	private ImageDescriptor getOverlay(IType type) throws JavaModelException {
		if (type.isAnnotation()) {
			return JavaPluginImages.DESC_OVR_ANNOTATION;
		} else if (type.isInterface()) {
			return JavaPluginImages.DESC_OVR_INTERFACE;
		} else if (type.isEnum()) {
			return JavaPluginImages.DESC_OVR_ENUM;
		}
		return null;
	}

}
