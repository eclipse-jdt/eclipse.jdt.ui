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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

public class TrayLabelProvider extends LabelProvider implements IColorProvider {
	
	private Color fBlue, fRed;
	private Binding fViewerElement;
	
	public TrayLabelProvider() {
		Display display= Display.getCurrent();
		
		fRed= display.getSystemColor(SWT.COLOR_RED);
		fBlue= display.getSystemColor(SWT.COLOR_DARK_BLUE);
	}
	
	public void setViewerElement(Binding viewerElement) {
		if (fViewerElement != viewerElement) {
			fViewerElement= viewerElement;
			fireLabelProviderChanged(new LabelProviderChangedEvent(this));
		}
	}
	
	public String getText(Object obj) {
		if (obj instanceof DynamicBindingProperty) {
			return ((DynamicBindingProperty) obj).getLabel(fViewerElement);
		} else if (obj instanceof ASTAttribute) {
			return ((ASTAttribute) obj).getLabel();
		} else {
			return null;
		}
	}
	
	public Image getImage(Object obj) {
		if (obj instanceof ASTAttribute) {
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
			Binding binding= (Binding) element;
			if (binding.isRequired() && binding.getBinding() == null) {
				return fRed;
			} else {
				return fBlue;
			}
		}
		return null; // normal color
	}
	
	/*
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	public Color getBackground(Object element) {
		return null;
	}
	
}
