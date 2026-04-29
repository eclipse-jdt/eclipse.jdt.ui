/*******************************************************************************
* Copyright (c) 2026 Vector Informatik GmbH and others.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Vector Informatik GmbH  - initial API and implementation
*******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchyCore;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;

public class CallHierarchyViewPartContentDescriptionTest {

	private static final String SCOPE_DESCRIPTION= JavaSearchScopeFactory.getInstance().getWorkspaceScopeDescription(true);

	private static final IPreferenceStore PREFERENCE_STORE= PreferenceConstants.getPreferenceStore();

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fProject;

	private CallHierarchyViewPart fViewPart;

	@Before
	public void setUp() throws CoreException {
		fProject= pts.getProject();
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fProject, "src");

		IPackageFragment pack= sourceFolder.createPackageFragment("org.eclipse.test", false, null);
		String str= """
				package org.eclipse.test;
				public class TestClass {}
				""";
		ICompilationUnit compilationUnit= pack.createCompilationUnit("TestClass.java", str, false, null);
		IType type= compilationUnit.findPrimaryType();

		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		fViewPart= CallHierarchyUI.openView(new IMember[] { type }, window);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fProject, pts.getDefaultClasspath());

		PREFERENCE_STORE.setToDefault(CallHierarchyCore.PREF_USE_FILTERS);
		PREFERENCE_STORE.setToDefault(CallHierarchyCore.PREF_FILTERS_LIST);
		PREFERENCE_STORE.setToDefault(CallHierarchyCore.PREF_SHOW_TEST_CODE_ONLY);
		PREFERENCE_STORE.setToDefault(CallHierarchyCore.PREF_HIDE_TEST_CODE);
	}

	@Test
	public void testContentDescriptionNoFilters() {
		assertThat(fViewPart.getContentDescription()).endsWith(SCOPE_DESCRIPTION);
	}

	@Test
	public void testContentDescriptionWithFilterPatterns() {
		String filters= "A,B,C";

		PREFERENCE_STORE.setValue(CallHierarchyCore.PREF_USE_FILTERS, true);
		PREFERENCE_STORE.setValue(CallHierarchyCore.PREF_FILTERS_LIST, filters);

		fViewPart.refresh();

		String expected= SCOPE_DESCRIPTION + " " + Messages.format(CallHierarchyMessages.CallHierarchyViewPart_scopeDescriptionWithFilters_activeFilters, filters);
		assertThat(fViewPart.getContentDescription()).endsWith(expected);
	}

	@Test
	public void testContentDescriptionTestCodeOnly() {
		PREFERENCE_STORE.setValue(CallHierarchyCore.PREF_SHOW_TEST_CODE_ONLY, true);

		fViewPart.refresh();

		String expected= CallHierarchyMessages.CallHierarchyViewPart_scopeDescriptionWithFilters_inTestCode + " " + SCOPE_DESCRIPTION;
		assertThat(fViewPart.getContentDescription()).endsWith(expected);
	}

	@Test
	public void testContentDescriptionMainCodeOnly() {
		PREFERENCE_STORE.setValue(CallHierarchyCore.PREF_HIDE_TEST_CODE, true);

		fViewPart.refresh();

		String expected= CallHierarchyMessages.CallHierarchyViewPart_scopeDescriptionWithFilters_inMainCode + " " + SCOPE_DESCRIPTION;
		assertThat(fViewPart.getContentDescription()).endsWith(expected);
	}

}
