/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.jeview.views;

import org.eclipse.core.resources.IMarker;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.jeview.JEPluginImages;



public class JEViewLabelProvider extends LabelProvider /*implements IColorProvider, IFontProvider*/ {
		
	JavaElementLabelProvider fJavaElementLabelProvider;
	private Image fChildrenImg;
	private Image fInfoImg;
	
	public JEViewLabelProvider() {
		fChildrenImg= JEPluginImages.IMG_CHILDREN.createImage();
		fInfoImg= JEPluginImages.IMG_INFO.createImage();
		fJavaElementLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS);
	}
	
	
	@Override
	public String getText(Object element) {
		if (element instanceof JEAttribute)
			return ((JEAttribute) element).getLabel();
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof JavaElement) {
			return fJavaElementLabelProvider.getImage(((JavaElement) element).getJavaElement());
			
		} else if (element instanceof JEResource) {
			return fJavaElementLabelProvider.getImage(((JEResource) element).getResource());
			
		} else if (element instanceof JEJarEntryResource) {
			return fJavaElementLabelProvider.getImage(((JEJarEntryResource) element).getJarEntryResource());
			
		} else if (element instanceof JavaElementProperty) {
			return fInfoImg;
			
		} else if (element instanceof JavaElementChildrenProperty) {
			return fChildrenImg;
			
		} else if (element instanceof Error) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
			
		} else if (element instanceof JEMarker) {
			JEMarker marker= (JEMarker) element;
			Object severity= marker.getMarkerAttribute(IMarker.SEVERITY);
			if (severity instanceof Integer) {
				Integer sev= (Integer) severity;
				switch (sev.intValue()) {
					case IMarker.SEVERITY_INFO:
						return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);
					case IMarker.SEVERITY_WARNING:
						return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
					case IMarker.SEVERITY_ERROR:
						return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
				}
			}
			return null;
			
		} else {
			return super.getImage(element);
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		fChildrenImg.dispose();
		fInfoImg.dispose();
		fJavaElementLabelProvider.dispose();
	}

}
