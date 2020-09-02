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
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.packageview.PackageCache;

/**
 * Tests for {@link PackageCache}.
 *
 */
public class PackageCacheTest {

	private IJavaProject testProject;

	private IPackageFragmentRoot src;

	private IPackageFragment package_a;
	private IPackageFragment package_a_b;
	private IPackageFragment package_a_b_c;
	private IPackageFragment package_a_b_c_d1;
	private IPackageFragment package_a_b_c_d2;
	private IPackageFragment package_a_b_e;
	private IPackageFragment package_f;
	private IPackageFragment package_f_g;

	private PackageCache packageCache;


	@Before
	public void setUp() throws Exception {

		IProgressMonitor monitor= new NullProgressMonitor();

		String projectName= getClass().getSimpleName();
		testProject= JavaProjectHelper.createJavaProject(projectName, "bin");
		src= JavaProjectHelper.addSourceContainer(testProject, "src");

		boolean force= true;
		package_a= src.createPackageFragment("a", force, monitor);
		package_a_b= src.createPackageFragment("a.b", force, monitor);
		package_a_b_c= src.createPackageFragment("a.b.c", force, monitor);
		package_a_b_c_d1= src.createPackageFragment("a.b.c.d1", force, monitor);
		package_a_b_c_d2= src.createPackageFragment("a.b.c.d2", force, monitor);
		package_a_b_e= src.createPackageFragment("a.b.e", force, monitor);
		package_f= src.createPackageFragment("f", force, monitor);
		package_f_g= src.createPackageFragment("f.g", force, monitor);

		packageCache= new PackageCache(src);
	}

	@After
	public void tearDown() throws Exception {
		testProject.getProject().delete(true, false, new NullProgressMonitor());
	}


	@Test
	public void testGetDirectChildren() throws Exception {
		List<IPackageFragment> allPackages= allPackages();

		Map<IPackageFragment, List<IPackageFragment>> actualChildren = new LinkedHashMap<>();
		for (IPackageFragment packageFragment : allPackages) {
			List<IPackageFragment> childrenOfPackage= packageCache.getDirectChildren(packageFragment);
			actualChildren.put(packageFragment, childrenOfPackage);
		}

		Map<IPackageFragment, List<IPackageFragment>> expectedChildren = new LinkedHashMap<>();
		expectedChildren.put(package_a, Arrays.asList(package_a_b));
		expectedChildren.put(package_a_b, Arrays.asList(package_a_b_c, package_a_b_e));
		expectedChildren.put(package_a_b_c, Arrays.asList(package_a_b_c_d1, package_a_b_c_d2));
		expectedChildren.put(package_a_b_c_d1, Collections.emptyList());
		expectedChildren.put(package_a_b_c_d2, Collections.emptyList());
		expectedChildren.put(package_a_b_e, Collections.emptyList());
		expectedChildren.put(package_f, Arrays.asList(package_f_g));
		expectedChildren.put(package_f_g, Collections.emptyList());

		assertEquals("method returned wrong results",
				expectedChildren, actualChildren);
	}

	@Test
	public void testGetSingleChild() throws Exception {
		Map<IPackageFragment, IPackageFragment> actualSingleChildren= actualSingleChildren();
		Map<IPackageFragment, IPackageFragment> expectedSingleChildren= expectedSingleChildren();

		assertEquals("method returned wrong results",
				expectedSingleChildren, actualSingleChildren);
	}

	@Test
	public void testSingleChildAgainstOldImplementation() throws Exception {
		Map<IPackageFragment, IPackageFragment> actualSingleChildren= actualSingleChildren();

		List<IPackageFragment> allPackages= allPackages();
		Map<IPackageFragment, IPackageFragment> expectedSingleChildren= new LinkedHashMap<>();
		for (IPackageFragment packageFragment : allPackages) {
			IPackageFragment singleChild= findSinglePackageChild(packageFragment);
			if (singleChild != null) {
				expectedSingleChildren.put(packageFragment, singleChild);
			}
		}

		assertEquals("method returned wrong results",
				expectedSingleChildren, actualSingleChildren);
	}

	@Test
	public void testHasSingleChild() throws Exception {
		List<IPackageFragment> actualPackagesWithSingleChild= actualPackagesWithSingleChild();
		List<IPackageFragment> expectedPackagesWithSingleChild= expectedPackagesWithSingleChild();

		assertEquals("method returned wrong results",
				expectedPackagesWithSingleChild, actualPackagesWithSingleChild);
	}

	@Test
	public void testHasSingleChildAgainstOldImplementation() throws Exception {
		List<IPackageFragment> actualPackagesWithSingleChild= actualPackagesWithSingleChild();

		List<IPackageFragment> allPackages= allPackages();
		List<IPackageFragment> expectedPackagesWithSingleChild= new ArrayList<>();
		for (IPackageFragment packageFragment : allPackages) {
			IPackageFragment singleChild= findSinglePackageChild(packageFragment);
			if (singleChild != null) {
				expectedPackagesWithSingleChild.add(packageFragment);
			}
		}

		assertEquals("method returned wrong results",
				expectedPackagesWithSingleChild, actualPackagesWithSingleChild);
	}

	private Map<IPackageFragment, IPackageFragment> actualSingleChildren() throws Exception {
		List<IPackageFragment> allPackages= allPackages();
		Map<IPackageFragment, IPackageFragment> actualSingleChildren= new LinkedHashMap<>();
		for (IPackageFragment packageFragment : allPackages) {
			IPackageFragment singleChild= packageCache.getSingleChild(packageFragment);
			if (singleChild != null) {
				actualSingleChildren.put(packageFragment, singleChild);
			}
		}
		return actualSingleChildren;
	}

	private Map<IPackageFragment, IPackageFragment> expectedSingleChildren() {
		Map<IPackageFragment, IPackageFragment> expectedSingleChildren= new LinkedHashMap<>();
		expectedSingleChildren.put(package_a, package_a_b);
		expectedSingleChildren.put(package_f, package_f_g);
		return expectedSingleChildren;
	}

	private List<IPackageFragment> actualPackagesWithSingleChild() throws Exception {
		List<IPackageFragment> allPackages= allPackages();
		List<IPackageFragment> actualPackagesWithSingleChild= new ArrayList<>();
		for (IPackageFragment packageFragment : allPackages) {
			if (packageCache.hasSingleChild(packageFragment)) {
				actualPackagesWithSingleChild.add(packageFragment);
			}
		}
		return actualPackagesWithSingleChild;
	}

	private List<IPackageFragment> expectedPackagesWithSingleChild() throws Exception {
		Map<IPackageFragment, IPackageFragment> expectedSingleChildren= expectedSingleChildren();
		return new ArrayList<>(expectedSingleChildren.keySet());
	}

	private List<IPackageFragment> allPackages() {
		return Arrays.asList(
				package_a,
				package_a_b,
				package_a_b_c,
				package_a_b_c_d1,
				package_a_b_c_d2,
				package_a_b_e,
				package_f,
				package_f_g);
	}

	/*
	 * Old "reference" implementation from org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider,
	 * which computes the single child package of a package (if any).
	 */
	private IPackageFragment findSinglePackageChild(IPackageFragment fragment) throws Exception {
		String prefix= fragment.getElementName() + '.';
		int prefixLen= prefix.length();
		IPackageFragment found= null;
		for (IJavaElement element : src.getChildren()) {
			String name= element.getElementName();
			if (name.startsWith(prefix) && name.length() > prefixLen && name.indexOf('.', prefixLen) == -1) {
				if (found == null) {
					found= (IPackageFragment) element;
				} else {
					return null;
				}
			}
		}
		return found;
	}
}
