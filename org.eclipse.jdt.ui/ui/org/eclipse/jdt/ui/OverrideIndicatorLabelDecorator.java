/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.ImageImageDescriptor;

/**
 * LabelDecorator that decorates an method's image with override or implements overlays.
 * The viewer using this decorator is responsible for updating the images on element changes.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OverrideIndicatorLabelDecorator implements ILabelDecorator, ILightweightLabelDecorator {

	private ImageDescriptorRegistry fRegistry;
	private boolean fUseNewRegistry= false;

	/**
	 * Creates a decorator. The decorator creates an own image registry to cache
	 * images. 
	 */
	public OverrideIndicatorLabelDecorator() {
		this(null);
		fUseNewRegistry= true;
	}	

	/*
	 * Creates decorator with a shared image registry.
	 * 
	 * @param registry The registry to use or <code>null</code> to use the Java plugin's
	 * image registry.
	 */	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public OverrideIndicatorLabelDecorator(ImageDescriptorRegistry registry) {
		fRegistry= registry;
	}
	
	private ImageDescriptorRegistry getRegistry() {
		if (fRegistry == null) {
			fRegistry= fUseNewRegistry ? new ImageDescriptorRegistry() : JavaPlugin.getImageDescriptorRegistry();
		}
		return fRegistry;
	}	
	
	
	/* (non-Javadoc)
	 * @see ILabelDecorator#decorateText(String, Object)
	 */
	public String decorateText(String text, Object element) {
		return text;
	}	

	/* (non-Javadoc)
	 * @see ILabelDecorator#decorateImage(Image, Object)
	 */
	public Image decorateImage(Image image, Object element) {
		int adornmentFlags= computeAdornmentFlags(element);
		if (adornmentFlags != 0) {
			ImageDescriptor baseImage= new ImageImageDescriptor(image);
			Rectangle bounds= image.getBounds();
			return getRegistry().get(new JavaElementImageDescriptor(baseImage, adornmentFlags, new Point(bounds.width, bounds.height)));
		}
		return image;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public int computeAdornmentFlags(Object element) {
		if (element instanceof IMethod) {
			if (!PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_OVERRIDE_INDICATOR)) {
				return 0;
			}
			
			try {
				IMethod method= (IMethod) element;
				int flags= method.getFlags();
				if (method.getDeclaringType().isClass() && !method.isConstructor() && !Flags.isPrivate(flags) && !Flags.isStatic(flags)) {
					return getOverrideIndicators(method);
				}
			} catch (JavaModelException e) {
				if (!e.isDoesNotExist()) {
					JavaPlugin.log(e);
				}
			}
		}
		return 0;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected int getOverrideIndicators(IMethod method) throws JavaModelException {
		IType type= method.getDeclaringType();
		ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(type);
		if (hierarchy != null) {
			return findInHierarchy(type, hierarchy, method.getElementName(), method.getParameterTypes());
		}
		return 0;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	protected int findInHierarchy(IType type, ITypeHierarchy hierarchy, String name, String[] paramTypes) throws JavaModelException {
		IMethod impl= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, name, paramTypes, false);
		if (impl != null) {
			IMethod overridden= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, type, name, paramTypes, false);
			if (overridden != null) {
				return JavaElementImageDescriptor.OVERRIDES;
			} else {
				return JavaElementImageDescriptor.IMPLEMENTS;
			}
		}
		return 0;
	}	 

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		if (fRegistry != null && fUseNewRegistry) {
			fRegistry.dispose();
		}
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	public void decorate(Object element, IDecoration decoration) { 
		int adornmentFlags= computeAdornmentFlags(element);
		if (adornmentFlags != 0) {
			decoration.addOverlay(JavaPluginImages.DESC_OVR_OVERRIDES);
		}
	}

}
