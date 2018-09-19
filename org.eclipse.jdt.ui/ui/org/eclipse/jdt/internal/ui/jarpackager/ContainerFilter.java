/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.jarpackager;

import org.eclipse.core.resources.IContainer;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IJavaElement;

/**
 * Filters out all packages and folders
 */
class ContainerFilter  extends ViewerFilter {

	private boolean fFilterContainers;

	public static boolean FILTER_CONTAINERS= true;
	public static boolean FILTER_NON_CONTAINERS= false;

	public ContainerFilter(boolean filterContainers) {
		fFilterContainers= filterContainers;
	}

	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		boolean isContainer= element instanceof IContainer;
		if (!isContainer && element instanceof IJavaElement) {
			int type= ((IJavaElement)element).getElementType();
			isContainer= type == IJavaElement.JAVA_MODEL
						|| type == IJavaElement.JAVA_PROJECT
						|| type == IJavaElement.PACKAGE_FRAGMENT
						|| type ==IJavaElement.PACKAGE_FRAGMENT_ROOT;
		}
		return (fFilterContainers && !isContainer) || (!fFilterContainers && isContainer);
	}
}
