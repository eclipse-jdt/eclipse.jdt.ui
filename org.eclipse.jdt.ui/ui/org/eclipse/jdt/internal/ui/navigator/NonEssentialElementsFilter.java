/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
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

	/**
	 *
	 * @param delegateFilter A filter to delegate to, can be null
	 */
	protected NonEssentialElementsFilter(ViewerFilter delegateFilter) {
		fDelegateFilter = delegateFilter;
	}

	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (!isStateModelInitialized) {
			initStateModel(viewer);
		}
		if (fContentService == null || fStateModel == null) {
			return true;
		} else if (element instanceof IPackageFragment) {
			if (isApplicable() && viewer instanceof StructuredViewer) {
				boolean isHierarchicalLayout= !fStateModel.getBooleanProperty(IExtensionStateConstants.Values.IS_LAYOUT_FLAT);
				try {
					IPackageFragment fragment = (IPackageFragment) element;
					if (isHierarchicalLayout && !fragment.isDefaultPackage() && fragment.hasSubpackages()) {
						return hasFilteredChildren((StructuredViewer) viewer, fragment);
					}
				} catch (JavaModelException e) {
					return false;
				}
			}
		}
		return doSelect(viewer, parent, element);
	}

	protected boolean hasFilteredChildren(StructuredViewer viewer, Object element) {
		Object[] children= getRawChildren(viewer, element);
		for (ViewerFilter filter : viewer.getFilters()) {
			boolean hasSelectedChildren= false;
			// next lines are identical to ViewerFilter.filter(Viewer, Object, Object[]), but exit early when first child is found
			int size = children.length;
			for (int j = 0; j < size; ++j) {
				if (filter.select(viewer, element, children[j])) {
					hasSelectedChildren= true;
					break;
				}
			}
			if (!hasSelectedChildren) {
				return false;
			}
		}
		return true;
	}

	private Object[] getRawChildren(StructuredViewer viewer, Object element) {
		IStructuredContentProvider provider = (IStructuredContentProvider) viewer.getContentProvider();
		if (provider instanceof ITreeContentProvider) {
			return ((ITreeContentProvider)provider).getChildren(element);
		}
		return provider.getElements(element);
	}

	protected boolean doSelect(Viewer viewer, Object parent, Object element) {
		return fDelegateFilter != null ? fDelegateFilter.select(viewer, parent, element) : true;
	}

	protected boolean isApplicable() {
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
