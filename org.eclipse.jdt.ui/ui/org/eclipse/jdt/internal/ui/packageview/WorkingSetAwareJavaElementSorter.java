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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaElementComparator;

public class WorkingSetAwareJavaElementSorter extends JavaElementComparator {
	/**
	 * Constructor.
	 * @since 3.14
	 */
	public WorkingSetAwareJavaElementSorter () {
	}

	/**
	 * Constructor.
	 *
	 * @param sortPFRByName When <code>true</code> {@link IPackageFragmentRoot}s are sorted by name and not by their classpath order
	 *
	 * @since 3.14
	 */
	public WorkingSetAwareJavaElementSorter(boolean sortPFRByName) {
		super(sortPFRByName);
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof IWorkingSet || e2 instanceof IWorkingSet)
			return 0;

		return super.compare(viewer, e1, e2);
	}
}
