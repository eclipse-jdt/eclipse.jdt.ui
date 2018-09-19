/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.internal.ui.packageview.PackageFragmentRootContainer;

/**
 * The library container filter is a filter used to determine whether library containers are shown
 * that are empty or have all children filtered out by other filters.
 */
public class NonEssentialEmptyLibraryContainerFilter extends NonEssentialElementsFilter {

	public NonEssentialEmptyLibraryContainerFilter() {
		super(null);
	}

	@Override
	protected boolean doSelect(Viewer viewer, Object parent, Object element) {
		if (element instanceof PackageFragmentRootContainer) {
			if (isApplicable() && viewer instanceof StructuredViewer) {
				PackageFragmentRootContainer rootContainer= (PackageFragmentRootContainer) element;
				return rootContainer.getChildren().length > 0 ? hasFilteredChildren((StructuredViewer) viewer, rootContainer) : false;
			}
		}
		return true;
	}
}
