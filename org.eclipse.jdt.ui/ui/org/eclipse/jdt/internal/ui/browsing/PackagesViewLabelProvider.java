/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelDecorator;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.TreeHierarchyLayoutProblemsDecorator;

/**
 * XXX: not yet reviewed - part of experimental logical packages view
 */
class PackagesViewLabelProvider extends AppearanceAwareLabelProvider {
	
	private static final int NO_ADORNMENT= 0;
	
	static final int HIERARCHICAL_VIEW_STATE= 0;
	static final int FLAT_VIEW_STATE= 1;

	private int fViewState;

	private ElementImageProvider fElementImageProvider;
	private ImageDescriptorRegistry fRegistry;

	PackagesViewLabelProvider(int state) {
		this(state, AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED, AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS);
	}

	PackagesViewLabelProvider(int state, int textFlags, int imageFlags) {
		super(textFlags, imageFlags);
		
		Assert.isTrue(isValidState(state));
		fViewState= state;
		fElementImageProvider= new ElementImageProvider();
		fRegistry= JavaPlugin.getImageDescriptorRegistry();
	}
	
	private boolean isValidState(int state) {
		return state == FLAT_VIEW_STATE || state == HIERARCHICAL_VIEW_STATE;
	}
	
	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof LogicalPackage) {
			LogicalPackage cp= (LogicalPackage) element;
			return getLogicalPackageImage(cp);
		}
		return super.getImage(element);
	}

	/**
	 * Decoration is only concerned with error ticks
	 */
	private Image getLogicalPackageImage(LogicalPackage cp) {
		IPackageFragment[] fragments= cp.getFragments();
		for (int i= 0; i < fragments.length; i++) {
			IPackageFragment fragment= fragments[i];

			if(!isEmpty(fragment)) {
				return decorateCompoundElement(fRegistry.get(fElementImageProvider.getCPImageDescriptor(cp, false)), cp.getFragments());
			}
		}
		return decorateCompoundElement(fRegistry.get(fElementImageProvider.getCPImageDescriptor(cp, true)), cp.getFragments()); 
	}
	
	
	private Image decorateCompoundElement(Image image, IPackageFragment[] fragments) {
		if (fLabelDecorators == null)
			return image;
		
		int currentAdornment= NO_ADORNMENT;
		
		for (int i= 0; i < fragments.length; i++) {
			IPackageFragment fragment= fragments[i];
			for (int j= 0; j < fLabelDecorators.size(); j++) {
				ILabelDecorator decorator= (ILabelDecorator) fLabelDecorators.get(j);
				if (decorator instanceof TreeHierarchyLayoutProblemsDecorator) {
					TreeHierarchyLayoutProblemsDecorator dec= (TreeHierarchyLayoutProblemsDecorator) decorator;
					try {
						int adornment= dec.computeAdornmentFlags(fragment);
						//only adorn if severity has increased / fix priority clash
						if ((adornment == JavaElementImageDescriptor.WARNING) && (currentAdornment == NO_ADORNMENT)) {
							currentAdornment= adornment;
							image= dec.decorateImage(image, currentAdornment);
						} else if((adornment == JavaElementImageDescriptor.ERROR) && (currentAdornment == NO_ADORNMENT || currentAdornment == JavaElementImageDescriptor.WARNING)) {
							currentAdornment= adornment;
							image= dec.decorateImage(image, currentAdornment);
						}						
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
				} else {
					image= decorator.decorateImage(image, fragment);
				}
			}
		}
		return image;
	}
	
	private boolean isEmpty(IPackageFragment fragment) { 
		try {
			return (fragment.getCompilationUnits().length == 0) && (fragment.getClassFiles().length == 0);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return false;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof IPackageFragment)
			return getText((IPackageFragment)element);
		else if (element instanceof LogicalPackage)
			return getText((LogicalPackage)element);
		else
			return super.getText(element);
	}
	
	private String getText(IPackageFragment fragment) {
		if(isFlatView())
			return getFlatText(fragment);
		else
			return getHierarchicalText(fragment);
	}

	private String getText(LogicalPackage logicalPackage) {
		IPackageFragment[] fragments= logicalPackage.getFragments();
		return getText(fragments[0]);
	}
	
	private String getFlatText(IPackageFragment fragment) {
		return decorateText(JavaElementLabels.getElementLabel(fragment, getTextFlags()), fragment);
	}
	
	private boolean isFlatView() {
		return fViewState==FLAT_VIEW_STATE;
	}

	private String getHierarchicalText(IPackageFragment fragment) {
		if (fragment.isDefaultPackage())
			//this must exist already but not in JavaElementLable
			return decorateText(JavaElementLabels.getElementLabel(fragment, getTextFlags()), fragment);
		String name= JavaElementLabels.getElementLabel(fragment, getTextFlags());
		if (name.indexOf(".") != -1)//$NON-NLS-1$
			name= name.substring(name.lastIndexOf(".") + 1);//$NON-NLS-1$
		return decorateText(name, fragment);
	}
	
	private String decorateText(String name, Object element) {
		if (fLabelDecorators == null)
			return name;

		for (int i= 0; i < fLabelDecorators.size(); i++) {
			ILabelDecorator decorator= (ILabelDecorator) fLabelDecorators.get(i);
			name= decorator.decorateText(name, element);
		}
		return name;	
	}
	

	private class ElementImageProvider extends JavaElementImageProvider{
		
		public ElementImageProvider() {
			super();
		}
		
		public ImageDescriptor getCPImageDescriptor(LogicalPackage element, boolean isEmpty) {
			if(isEmpty)
				return JavaPluginImages.DESC_OBJS_COMPOUND_EMPTY_PACKAGE;
			else return JavaPluginImages.DESC_OBJS_COMPOUND_PACKAGE;		
		}
	}
}
