/***************************************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 **************************************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;


import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.eclipse.ui.navigator.INavigatorContentService;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;



/**
 * 
 * This filter is only applicable to instances of the Common Navigator.
 * 
 * This filter will not allow essential elements to be blocked.
 */
public abstract class NonEssentialElementsFilter extends ViewerFilter {

	private static final String JAVA_EXTENSION_ID = "org.eclipse.jdt.java.ui.javaContent"; //$NON-NLS-1$

	private boolean isStateModelInitialized = false;
	private IExtensionStateModel fStateModel = null;

	private INavigatorContentService fContentService;

	private ViewerFilter fDelegateFilter;

	protected NonEssentialElementsFilter(ViewerFilter delegateFilter) {
		fDelegateFilter = delegateFilter;
	}

	/**
	 * Returns the result of this filter, when applied to the given inputs.
	 * 
	 * @return Returns true if element should be included in filtered set
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {

		if (!isStateModelInitialized) {
			initStateModel(viewer);
		}
		if (fContentService == null || fStateModel == null) {
			return true;
		} else if (element instanceof IPackageFragment) {
			if (isApplicable()) {
				boolean isFlatLayout = fStateModel.getBooleanProperty(IExtensionStateConstants.Values.IS_LAYOUT_FLAT);
				try {
					IPackageFragment fragment = (IPackageFragment) element;
					if (!isFlatLayout && (!fragment.isDefaultPackage() && fragment.hasSubpackages()))
						return true;
				} catch (JavaModelException e) {
					return false;
				}
			}
		}

		return doSelect(viewer, parent, element);
	}

	protected boolean doSelect(Viewer viewer, Object parent, Object element) {
		return fDelegateFilter.select(viewer, parent, element);
	}

	private boolean isApplicable() {
		return fContentService != null && fContentService.isVisible(JAVA_EXTENSION_ID) && fContentService.isActive(JAVA_EXTENSION_ID);
	}

	private synchronized void initStateModel(Viewer viewer) {

		if (!isStateModelInitialized) {
			if (viewer instanceof CommonViewer) {

				CommonViewer commonViewer = (CommonViewer) viewer;
				fContentService = commonViewer.getNavigatorContentService();
				fStateModel = fContentService.findStateModel(JAVA_EXTENSION_ID);

				isStateModelInitialized = true;
			}
		}
	}



}
