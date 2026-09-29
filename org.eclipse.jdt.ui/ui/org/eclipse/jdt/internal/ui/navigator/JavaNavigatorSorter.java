/*******************************************************************************
 * Copyright (c) 2026 Bas Gooren and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.PreferenceConstants;

@SuppressWarnings("deprecation")
public class JavaNavigatorSorter extends ViewerSorter {

	private final JavaElementComparator fClasspathOrderComparator= new JavaElementComparator(false);
	private final JavaElementComparator fAlphabeticalComparator= new JavaElementComparator(true);

	@Override
	public int category(Object element) {
		return fClasspathOrderComparator.category(element);
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		return getJavaElementComparator().compare(viewer, e1, e2);
	}

	private JavaElementComparator getJavaElementComparator() {
		boolean sortByName= PreferenceConstants.getPreferenceStore().getBoolean(
				PreferenceConstants.APPEARANCE_SORT_LIBRARY_ENTRIES_BY_NAME);
		return sortByName ? fAlphabeticalComparator : fClasspathOrderComparator;
	}
}
