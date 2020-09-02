/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - https://bugs.eclipse.org/bugs/show_bug.cgi?id=563562
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;

public class TypeHierarchyViewPartTest {
	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private static final String SRC_CONTAINER= "src";

	private IJavaProject fJProject1;

	@Before
	public void setUp() throws Exception {
		fJProject1= projectSetup.getProject();

		JavaProjectHelper.addSourceContainer(fJProject1, "src");

		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
		fJProject1= null;
	}

	private void addAllCUs(IJavaElement[] children, List<IJavaElement> result) throws JavaModelException {
		for (IJavaElement element : children) {
			if (element instanceof ICompilationUnit) {
				result.add(element);
			} else if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot)element;
				addAllCUs(root.getChildren(), result);
			} else if (element instanceof IPackageFragment) {
				IPackageFragment pack= (IPackageFragment)element;
				addAllCUs(pack.getChildren(), result);
			}
		}
	}

	@Test
	public void testTypeHierarchyViewPart() throws Exception {
		// Given
		List<IJavaElement> cus= new ArrayList<>();
		addAllCUs(fJProject1.getChildren(), cus);
		IJavaElement element= cus.get(0);
		IJavaElement[] javaElements1= new IJavaElement[] {cus.get(0).getAncestor(IJavaElement.PACKAGE_FRAGMENT)};
		IJavaElement[] javaElements2= new IJavaElement[] {cus.get(0).getAncestor(IJavaElement.PACKAGE_FRAGMENT), cus.get(0).getAncestor(IJavaElement.PACKAGE_FRAGMENT)};
		IWorkbenchWindow workbenchWindow= JavaPlugin.getActiveWorkbenchWindow();

		TypeHierarchyViewPart viewPart= OpenTypeHierarchyUtil.open(element, workbenchWindow);

		// When
		viewPart.setInputElements(javaElements1);
		assertEquals("Wrong first history entry number ", 2, viewPart.getHistoryEntries().size());

		viewPart.setInputElements(javaElements2);
		assertEquals("Wrong second history entry number ", 3, viewPart.getHistoryEntries().size());

		viewPart.setInputElements(javaElements1);
		assertEquals("Wrong third history entry number ", 3, viewPart.getHistoryEntries().size());

		viewPart.setInputElements(javaElements2);
		assertEquals("Wrong fourth history entry number ", 3, viewPart.getHistoryEntries().size());
    }
}
