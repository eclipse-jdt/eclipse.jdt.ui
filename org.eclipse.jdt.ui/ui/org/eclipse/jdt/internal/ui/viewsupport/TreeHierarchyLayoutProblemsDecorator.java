/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Special problem decorator for hierarchical package layout.
 * <p>
 * It only decorates package fragments which are not covered by the
 * <code>ProblemsLabelDecorator</code>.
 * </p>
 * 
 * @see org.eclipse.jdt.ui.ProblemsLabelDecorator 
 * @since 2.1
 */
public class TreeHierarchyLayoutProblemsDecorator implements ILabelDecorator {

	private ImageDescriptorRegistry fRegistry;
	private boolean fIsFlatLayout;
	
	public TreeHierarchyLayoutProblemsDecorator(ImageDescriptorRegistry registry) {
		if (registry == null) {
			registry= JavaPlugin.getImageDescriptorRegistry();
		}
		fRegistry= registry;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateImage(org.eclipse.swt.graphics.Image, java.lang.Object)
	 */
	public Image decorateImage(Image image, Object element) {

		try {
			if (!fIsFlatLayout && element instanceof IPackageFragment) {
				IPackageFragment fragment= (IPackageFragment) element;
				
				//the default package will be marked by the other decorator
				if(fragment.isDefaultPackage())
					return image;
				
				IResource resource= fragment.getUnderlyingResource();

				return decorateBasedOnHierarchy(resource, image);

			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}

		return image;
	}
	
	private Image decorateBasedOnHierarchy(IResource resource, Image image) {
		
		if(resource==null)
			return image;
		
		int adornmentFlags= computeAdornmentFlags(resource);
		if (adornmentFlags != 0) {
			ImageDescriptor baseImage= new ImageImageDescriptor(image);
			Rectangle bounds= image.getBounds();
			return fRegistry.get(new JavaElementImageDescriptor(baseImage, adornmentFlags, new Point(bounds.width, bounds.height)));
		}
		return image;
	}

	private int computeAdornmentFlags(IResource resource) {
		if (resource == null || !resource.isAccessible()) {
			return 0;
		}
		int info= 0;
		try {

			IMarker[] markers= resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
			if (markers.length != 0) {
				//it's already been marked by a ProblemsLabelDecorator
				return info;
			}

			markers= resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			if (markers != null) {
				for (int i= 0; i < markers.length && (info != JavaElementImageDescriptor.ERROR); i++) {
					IMarker curr= markers[i];

					int priority= curr.getAttribute(IMarker.SEVERITY, -1);
					if (priority == IMarker.SEVERITY_WARNING) {
						info= JavaElementImageDescriptor.WARNING;
					} else if (priority == IMarker.SEVERITY_ERROR) {
						info= JavaElementImageDescriptor.ERROR;
					}
				}
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return info;
	}

	public void setIsFlatLayout(boolean state) {
		fIsFlatLayout= state;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateText(java.lang.String, java.lang.Object)
	 */
	public String decorateText(String text, Object element) {
		return text;
	}

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
		
	}

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}
}
