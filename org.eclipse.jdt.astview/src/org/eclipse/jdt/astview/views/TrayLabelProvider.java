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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;

public class TrayLabelProvider extends LabelProvider implements IColorProvider {
	
	private Color fBlue, fRed;
	private Color fWidgetForeground;
	
	private Object fViewerElement;
	
	public TrayLabelProvider() {
		Display display= Display.getCurrent();
		
		fRed= display.getSystemColor(SWT.COLOR_RED);
		fBlue= display.getSystemColor(SWT.COLOR_DARK_BLUE);
		fWidgetForeground= display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
	}
	
	public void setViewerElement(Object viewerElement) {
		if (fViewerElement != viewerElement) {
			fViewerElement= viewerElement;
			fireLabelProviderChanged(new LabelProviderChangedEvent(this));
		}
	}
	
	public String getText(Object obj) {
		if (obj instanceof DynamicBindingProperty) {
			DynamicBindingProperty dynamicBindingProperty= (DynamicBindingProperty) obj;
			dynamicBindingProperty.setViewerElement(fViewerElement instanceof Binding ? (Binding) fViewerElement : null);
			return dynamicBindingProperty.getLabel();
		} else if (obj instanceof DynamicAttributeProperty) {
			DynamicAttributeProperty dynamicAttributeProperty= (DynamicAttributeProperty) obj;
			dynamicAttributeProperty.setViewerElement(fViewerElement);
			return dynamicAttributeProperty.getLabel();
		} else if (obj instanceof ASTAttribute) {
			return ((ASTAttribute) obj).getLabel();
		} else if (obj instanceof ASTNode) {
			return Signature.getSimpleName(((ASTNode) obj).getClass().getName());
		} else {
			return ""; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=126017
		}
	}
	
	public Image getImage(Object obj) {
		if (obj instanceof DynamicBindingProperty) {
			DynamicBindingProperty dynamicBindingProperty= (DynamicBindingProperty) obj;
			dynamicBindingProperty.setViewerElement(fViewerElement instanceof Binding ? (Binding) fViewerElement : null);
			return dynamicBindingProperty.getImage();
		} else if (obj instanceof DynamicAttributeProperty) {
			DynamicAttributeProperty dynamicAttributeProperty= (DynamicAttributeProperty) obj;
			dynamicAttributeProperty.setViewerElement(fViewerElement);
			return dynamicAttributeProperty.getImage();
		} else if (obj instanceof ASTAttribute) {
			return ((ASTAttribute) obj).getImage();
		} else {
			return null;
		}
	}
	
	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		super.dispose();
		fViewerElement= null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(Object element) {
		if (element instanceof Binding) {
			return fBlue;
			
		} else if (element instanceof ExceptionAttribute) {
			if (element instanceof DynamicBindingProperty) {
				((DynamicBindingProperty) element).setViewerElement(fViewerElement instanceof Binding ? (Binding) fViewerElement : null);
			} else if (element instanceof DynamicAttributeProperty) {
				((DynamicAttributeProperty) element).setViewerElement(fViewerElement);
			}
			
			if (((ExceptionAttribute) element).getException() == null)
//				return null; //Bug 75022: Does not work when label is updated (retains old color, doesn't get default)
				//TODO remove hackaround when bug 75022 is fixed
				return fWidgetForeground;
			else
				return fRed;
			
		} else {
			return null; // normal color
		}
	}
	
	/*
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	public Color getBackground(Object element) {
		return null;
	}
	
}
