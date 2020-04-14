/*******************************************************************************
 * Copyright (c) 2017, 2020 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.reorg.IsCompletelySelected;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests for {@link IsCompletelySelected}.
 *
 * Validates that given specific selected packages, the predicate correctly indicates which packages
 * are fully selected.
 *
 * E.g. given the following package structure and selection
 *
 * <pre>
 * package1        -- selected
 * package1.sub1
 * package1.sub2   -- selected
 * package2        -- selected
 * </pre>
 *
 * only {@code package1.sub2} and {@code package2} are fully selected. {@code package1} is not fully
 * selected, since only one sub-package of {@code package1} is selected.
 *
 * @author Simeon Andreev
 */
public class IsCompletelySelectedTest  {

	private IJavaProject testProject;

	private IPackageFragment package_a;
	private IPackageFragment package_a_b;
	private IPackageFragment package_a_b_c;
	private IPackageFragment package_a_b_c_d1;
	private IPackageFragment package_a_b_c_d2;
	private IPackageFragment package_a_b_e;

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	@Before
	public void setUp() throws Exception {

		IProgressMonitor monitor= new NullProgressMonitor();

		String projectName= getClass().getSimpleName();
		testProject= JavaProjectHelper.createJavaProject(projectName, "bin");
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(testProject, "src");

		boolean force= true;
		package_a= src.createPackageFragment("a", force, monitor);
		package_a_b= src.createPackageFragment("a.b", force, monitor);
		package_a_b_c= src.createPackageFragment("a.b.c", force, monitor);
		package_a_b_c_d1= src.createPackageFragment("a.b.c.d1", force, monitor);
		package_a_b_c_d2= src.createPackageFragment("a.b.c.d2", force, monitor);
		package_a_b_e= src.createPackageFragment("a.b.e", force, monitor);
	}

	@After
	public void tearDown() throws Exception {
		testProject.getProject().delete(true, false, new NullProgressMonitor());
	}

	@Test
	public void testPartialSelection1() throws Exception {
		IPackageFragment[] selectedPackages= { package_a, package_a_b_e };
		IPackageFragment[] expectedCompletelySelected= { package_a_b_e };
		assertCorrectCompleteSelection(selectedPackages, expectedCompletelySelected);
	}

	@Test
	public void testPartialSelection2() throws Exception {
		IPackageFragment[] selectedPackages= { package_a_b, package_a_b_c_d1 };
		IPackageFragment[] expectedCompletelySelected= { package_a_b_c_d1 };
		assertCorrectCompleteSelection(selectedPackages, expectedCompletelySelected);
	}

	@Test
	public void testPartialSelection3() throws Exception {
		IPackageFragment[] selectedPackages= { package_a, package_a_b_c };
		IPackageFragment[] expectedCompletelySelected= {};
		assertCorrectCompleteSelection(selectedPackages, expectedCompletelySelected);
	}

	@Test
	public void testPartialSelection4() throws Exception {
		IPackageFragment[] selectedPackages= { package_a_b_c, package_a_b_c_d1 };
		IPackageFragment[] expectedCompletelySelected= { package_a_b_c_d1 };
		assertCorrectCompleteSelection(selectedPackages, expectedCompletelySelected);
	}

	@Test
	public void testCompleteSelection() throws Exception {
		IPackageFragment[] selectedPackages= allPackages();
		IPackageFragment[] expectedCompletelySelected= selectedPackages;
		assertCorrectCompleteSelection(selectedPackages, expectedCompletelySelected);
	}

	@Test
	public void testCompleteSelectionOfSubPackage1() throws Exception {
		IPackageFragment[] selectedPackages= { package_a_b_c, package_a_b_c_d1, package_a_b_c_d2 };
		IPackageFragment[] expectedCompletelySelected= selectedPackages;
		assertCorrectCompleteSelection(selectedPackages, expectedCompletelySelected);
	}

	@Test
	public void testCompleteSelectionOfSubPackage2() throws Exception {
		IPackageFragment[] selectedPackages= { package_a_b_e, package_a_b_c_d1, package_a_b_c_d2 };
		IPackageFragment[] expectedCompletelySelected= { package_a_b_e, package_a_b_c_d1, package_a_b_c_d2 };
		assertCorrectCompleteSelection(selectedPackages, expectedCompletelySelected);
	}

	private void assertCorrectCompleteSelection(IPackageFragment[] selectedPackages, IPackageFragment[] expectedCompletelySelected) throws Exception {
		IsCompletelySelected predicate= new IsCompletelySelected(Arrays.asList(selectedPackages));
		assertMatchesOnly(predicate, expectedCompletelySelected);
	}

	private void assertMatchesOnly(IsCompletelySelected predicate, IPackageFragment... expectedCompletelySelectedPackages) {
		List<IPackageFragment> allPackages= Arrays.asList(allPackages());

		Set<IPackageFragment> actualCompletelySelectedPackages= new LinkedHashSet<>();
		for (IPackageFragment packageFragment : allPackages) {
			if (predicate.test(packageFragment)) {
				actualCompletelySelectedPackages.add(packageFragment);
			}
		}

		assertEquals("wrong set of completely selected packages",
				new LinkedHashSet<>(Arrays.asList(expectedCompletelySelectedPackages)), actualCompletelySelectedPackages);
	}

	private IPackageFragment[] allPackages() {
		IPackageFragment[] allPackages= {
				package_a, package_a_b, package_a_b_c, package_a_b_c_d1, package_a_b_c_d2, package_a_b_e
		};
		return allPackages;
	}
}
