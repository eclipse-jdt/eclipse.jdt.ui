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
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.PreferenceConstants;

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
class PackagesViewLabelProvider implements ILabelProvider, IPropertyChangeListener {
	
	private static final int NO_ADORNMENT= 0;
	
	static final int HIERARCHICAL_VIEW_STATE= 0;
	static final int FLAT_VIEW_STATE= 1;

	private int fViewState;

	private ListenerList fListeners= new ListenerList(1);
	private int fTextFlagsMask;
	private int fTextFlags;
	private int fImageFlags;

	private ElementImageProvider fElementImageProvider;
	private ImageDescriptorRegistry fRegistry;

	private ILabelDecorator[] fDecorators;
	private ILabelDecorator fProblemDecorator;


	PackagesViewLabelProvider(int state, ILabelDecorator[] labelDecorators) {
		this(state, AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED, AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS, labelDecorators);
	}

	PackagesViewLabelProvider(int state, int textFlags, int imageFlags, ILabelDecorator[] labelDecorators) {
		
		Assert.isTrue(isValidState(state));
		fViewState= state;
		
		fRegistry= JavaPlugin.getImageDescriptorRegistry();
		fDecorators= labelDecorators;
		fElementImageProvider= new ElementImageProvider();
		
		fTextFlags= textFlags;
		fImageFlags= imageFlags;
		initTextFlagsMask();
		
		JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}
	
	private boolean isValidState(int state) {
		return state == FLAT_VIEW_STATE || state == HIERARCHICAL_VIEW_STATE;
	}
	
	private int getTextFlags() {
		return fTextFlags & fTextFlagsMask;
	}

	private int getImageFlags() {
		return fImageFlags;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof LogicalPackage) {
			LogicalPackage cp= (LogicalPackage) element;
			return getLogicalPackageImage(cp);
		} else if (element instanceof IPackageFragment) {
			IPackageFragment fragment= (IPackageFragment) element;
			return getPackageFragmentImage(fragment);
		}
		return null;
	}
	
	
	private Image getPackageFragmentImage(IPackageFragment fragment) {
		Image image= fRegistry.get(fElementImageProvider.getBaseImageDescriptor(fragment, getImageFlags()));
		
		if (fDecorators == null)
			return image;

		// Decorate
		Image decoratedImage= image;		
		for (int i= 0; i < fDecorators.length; i++) {
			ILabelDecorator decorator= fDecorators[i];
			decoratedImage= decorator.decorateImage(decoratedImage, fragment);
		}
		return decoratedImage;
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
		if (fDecorators == null)
			return image;
		
		int currentAdornment= NO_ADORNMENT;
		
		for (int i= 0; i < fragments.length; i++) {
			IPackageFragment fragment= fragments[i];
			for (int j= 0; j < fDecorators.length; j++) {
				ILabelDecorator decorator= fDecorators[j];
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

		Assert.isLegal(false);
		return null;
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
		if (fDecorators == null)
			return name;

		for (int i= 0; i < fDecorators.length; i++) {
			ILabelDecorator decorator= fDecorators[i];
			name= decorator.decorateText(name, element);
		}
		return name;	
	}
	
	
	private void initTextFlagsMask() {
		fTextFlagsMask= 0xFFFFFFFF;

		if (!compressPackageNames())
			fTextFlagsMask^= JavaElementLabels.P_COMPRESSED;
	}

	private static boolean compressPackageNames() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES);
	}

	/*
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (property.equals(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW)
				|| property.equals(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES)) {
			initTextFlagsMask();
			LabelProviderChangedEvent lpEvent= new LabelProviderChangedEvent(this, null); // refresh all
			fireLabelProviderChanged(lpEvent);
		}		
	}
	
	private void fireLabelProviderChanged(LabelProviderChangedEvent event) {
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++)
			((ILabelProviderListener) listeners[i]).labelProviderChanged(event);
	}

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
		if (fDecorators != null) {
			for (int i= 0; i < fDecorators.length; i++) {
				fDecorators[i].addListener(listener);
			}
		}
		fListeners.add(listener);
	}

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		if (fDecorators != null) {
			for (int i= 0; i < fDecorators.length; i++) {
				ILabelDecorator decorator= fDecorators[i];
				decorator.dispose();
			}
		}
		JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
	}

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	/*
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
		if (fDecorators != null) {
			for (int i= 0; i < fDecorators.length; i++) {
				fDecorators[i].removeListener(listener);
			}
		}
		fListeners.remove(listener);
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
