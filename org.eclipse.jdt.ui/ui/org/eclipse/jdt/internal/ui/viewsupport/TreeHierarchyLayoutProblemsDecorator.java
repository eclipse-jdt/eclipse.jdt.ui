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
import org.eclipse.jface.util.ListenerList;
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

	private static final int NO_ADORNMENT= 0;
	private ImageDescriptorRegistry fRegistry;
	private boolean fIsFlatLayout;
	
	private ListenerList fListenerList= new ListenerList(1);
	
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

//		XXX: Work in progress for problem decorator being a workbench decorator
//		IDecoratorManager decoratorMgr= PlatformUI.getWorkbench().getDecoratorManager();
//		if (!decoratorMgr.getEnabled("org.eclipse.jdt.ui.problem.decorator")) //$NON-NLS-1$
//			return image;

		try {
			if (!fIsFlatLayout && element instanceof IPackageFragment) {
				IPackageFragment fragment= (IPackageFragment) element;
				
				if(!fragment.exists())
					return image;
				
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
	
	public Image decorateImage(Image image, int adornment){	

//		XXX: Work in progress for problem decorator being a workbench decorator
//		IDecoratorManager decoratorMgr= PlatformUI.getWorkbench().getDecoratorManager();
//		if (!decoratorMgr.getEnabled("org.eclipse.jdt.ui.problem.decorator")) //$NON-NLS-1$
//			return image;		

		return adornImage(image, adornment);
	}
	
	
	protected Image decorateBasedOnHierarchy(IResource resource, Image image) {
		
		if (resource==null)
			return image;	
		int adornmentFlags= computeAdornmentFlags(resource);
		return adornImage(image, adornmentFlags);
	}
	
	private Image adornImage(Image image, int adornmentFlags) {
		if (adornmentFlags != NO_ADORNMENT && image!=null) {
			ImageDescriptor baseImage= new ImageImageDescriptor(image);
			Rectangle bounds= image.getBounds();
			return fRegistry.get(new JavaElementImageDescriptor(baseImage, adornmentFlags, new Point(bounds.width, bounds.height)));
		}
		return image;
	}
	
	
	/**
	 * Computes the adornment for the given package fragment based on the most
	 * severe marker of its children.
	 * <p>
	 * The default package gets no adornment.
	 * </p>
	 *
	 * @param fragment the package fragment on which adornment is being calculated
	 * @return int the adornment flag value
	 */
	public int computeAdornmentFlags(IPackageFragment fragment) throws JavaModelException {
		if (fragment.isDefaultPackage())
			return NO_ADORNMENT;
			
		return computeAdornmentFlags(fragment.getUnderlyingResource());
	}

	private int computeAdornmentFlags(IResource resource) {
		if (resource == null || !resource.isAccessible()) {
			return NO_ADORNMENT;
		}
		int flag= NO_ADORNMENT;
		try {
			IMarker[] markers= resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

			// Find marker with highest severity
			if (markers != null) {
				for (int i= 0; i < markers.length && (flag != JavaElementImageDescriptor.ERROR); i++) {
					IMarker curr= markers[i];

					int priority= curr.getAttribute(IMarker.SEVERITY, -1);
					if (priority == IMarker.SEVERITY_WARNING && flag==NO_ADORNMENT) {
						flag= JavaElementImageDescriptor.WARNING;
					} else if (priority == IMarker.SEVERITY_ERROR && (flag==NO_ADORNMENT || flag==JavaElementImageDescriptor.WARNING)) {
						return JavaElementImageDescriptor.ERROR;
					}
				}
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return flag;
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
		fListenerList.add(listener);
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
		fListenerList.remove(listener);
	}
}
