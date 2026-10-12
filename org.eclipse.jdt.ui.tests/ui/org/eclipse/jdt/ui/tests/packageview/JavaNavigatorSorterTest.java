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
package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.navigator.JavaNavigatorSorter;

public class JavaNavigatorSorterTest {

	private IJavaProject fJavaProject;
	private IPackageFragmentRoot fZRoot;
	private IPackageFragmentRoot fARoot;
	private IPreferenceStore fPreferenceStore;
	private boolean fOriginalSortByName;

	@BeforeEach
	public void setUp() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("JavaNavigatorSorterTest", "bin");
		fZRoot= JavaProjectHelper.addClassFolder(fJavaProject, "z-library", null, null);
		fARoot= JavaProjectHelper.addClassFolder(fJavaProject, "a-library", null, null);
		fPreferenceStore= PreferenceConstants.getPreferenceStore();
		fOriginalSortByName= fPreferenceStore.getBoolean(
				PreferenceConstants.APPEARANCE_SORT_LIBRARY_ENTRIES_BY_NAME);
	}

	@AfterEach
	public void tearDown() throws Exception {
		fPreferenceStore.setValue(PreferenceConstants.APPEARANCE_SORT_LIBRARY_ENTRIES_BY_NAME, fOriginalSortByName);
		JavaProjectHelper.delete(fJavaProject);
	}

	@Test
	public void testLibraryEntriesFollowPreference() {
		JavaNavigatorSorter sorter= new JavaNavigatorSorter();

		fPreferenceStore.setValue(PreferenceConstants.APPEARANCE_SORT_LIBRARY_ENTRIES_BY_NAME, false);
		assertTrue(sorter.compare(null, fZRoot, fARoot) < 0, "Classpath order should place z-library first");

		fPreferenceStore.setValue(PreferenceConstants.APPEARANCE_SORT_LIBRARY_ENTRIES_BY_NAME, true);
		assertTrue(sorter.compare(null, fZRoot, fARoot) > 0, "Alphabetical order should place a-library first");
	}
}
