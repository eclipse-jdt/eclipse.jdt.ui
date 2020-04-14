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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.reorg.ParentChecker;

public class ParentCheckerTest {

	private IJavaProject testProject;

	private IPackageFragmentRoot src;

	private IPackageFragment package_a_b;
	private IPackageFragment package_a_b_c;
	private IPackageFragment package_a_b_d;

	private IResource classAB;
	private IResource classC;
	private IResource classD1;
	private IResource classD2;


	@Before
	public void setUp() throws Exception {

		/*
		 * The test structure is:
		 *
		 * src
		 * |
		 * |-- a.b
		 *     |-- AB.java
		 *     |
		 *     |-- a.b.c
		 *     |   |
		 *     |   |-- C.java
		 *     |
		 *     |-- a.b.d
		 *         |
		 *         |-- D1.java
		 *         |-- D2.java
		 */

		IProgressMonitor monitor= new NullProgressMonitor();

		String projectName= getClass().getSimpleName();
		testProject= JavaProjectHelper.createJavaProject(projectName, "bin");
		src= JavaProjectHelper.addSourceContainer(testProject, "src");

		boolean force= true;

		package_a_b= src.createPackageFragment("a.b", force, monitor);
		package_a_b_c= src.createPackageFragment("a.b.c", force, monitor);
		package_a_b_d= src.createPackageFragment("a.b.d", force, monitor);

		classAB= createClass(package_a_b, "AB");
		classC= createClass(package_a_b_c, "C");
		classD1= createClass(package_a_b_d, "D1");
		classD2= createClass(package_a_b_d, "D2");
	}

	@After
	public void tearDown() throws Exception {
		testProject.getProject().delete(true, true, null);
	}


	private IResource createClass(IPackageFragment classPackage, String className) throws Exception {
		IProgressMonitor monitor= new NullProgressMonitor();
		boolean force= true;
		String packageName= classPackage.getElementName();
		ICompilationUnit compilationUnit= classPackage.createCompilationUnit(className + ".java", "package " + packageName + ";\n public class " + className + " {}", force, monitor);
		return compilationUnit.getResource();
	}


	/**
	 * Test removing the src folder.
	 * All input packages should be removed, since the source folder (their parent) is removed.
	 */
	@Test
	public void testRemovePackages1() {
		IJavaElement[] folders= { src, package_a_b_c, package_a_b_d };
		IResource[] resources= {};
		IJavaElement[] expectedRemainingFolders= { src };

		assertCorrectRemovalOfElements(folders, resources, expectedRemainingFolders);
	}

	/**
	 * Test removing only packages and not the source folder.
	 * No packages should be removed, since the source folder (their parent), is not removed.
	 */
	@Test
	public void testRemovePackages2() {
		IJavaElement[] folders= { package_a_b_c, package_a_b_d };
		IResource[] resources= {};
		IJavaElement[] expectedRemainingFolders= folders;

		assertCorrectRemovalOfElements(folders, resources, expectedRemainingFolders);
	}

	/**
	 * Test removing resources and the highest level package.
	 * All resources should be removed.
	 */
	@Test
	public void testRemoveResources1() {
		IJavaElement[] packages= { package_a_b };
		IResource[] resources= { classAB, classC, classD1, classD2 };
		IJavaElement[] expectedRemainingPackages= packages;
		IResource[] expectedRemainingResources= {};

		assertCorrectRemovalOfElements(packages, resources, expectedRemainingPackages, expectedRemainingResources);
	}

	/**
	 * Test removing resources and a package.
	 * Only resources of that package should be removed.
	 */
	@Test
	public void testRemoveResources2() {
		IJavaElement[] packages= { package_a_b_c };
		IResource[] resources= { classAB, classC, classD1, classD2 };
		IJavaElement[] expectedRemainingPackages= packages;
		IResource[] expectedRemainingResources= { classAB, classD1, classD2 };

		assertCorrectRemovalOfElements(packages, resources, expectedRemainingPackages, expectedRemainingResources);
	}

	/**
	 * Test removing resources and a package.
	 * Only resources of that package should be removed.
	 */
	@Test
	public void testRemoveResources3() {
		IJavaElement[] packages= { package_a_b_d };
		IResource[] resources= { classAB, classC, classD1, classD2 };
		IJavaElement[] expectedRemainingPackages= packages;
		IResource[] expectedRemainingResources= { classAB, classC };

		assertCorrectRemovalOfElements(packages, resources, expectedRemainingPackages, expectedRemainingResources);
	}

	/**
	 * Test removing everything.
	 * Only the source folder should remain, since its parent is not in the input.
	 */
	@Test
	public void testRemoveAll() {
		IJavaElement[] folders= { src, package_a_b, package_a_b_c, package_a_b_d };
		IResource[] resources= { classAB, classC, classD1, classD2 };
		IJavaElement[] expectedRemainingFolders= { src };
		IResource[] expectedRemainingResources= {};

		assertCorrectRemovalOfElements(folders, resources, expectedRemainingFolders, expectedRemainingResources);
	}

	private static void assertCorrectRemovalOfElements(IJavaElement[] folders, IResource[] resources, IJavaElement[] expectedRemainingFolders) {
		boolean removeOnlyPackages= true;
		IResource[] expectedRemainingResources= resources;
		assertCorrectRemovalOfElements(folders, resources, expectedRemainingFolders, expectedRemainingResources, removeOnlyPackages);
	}

	private static void assertCorrectRemovalOfElements(IJavaElement[] folders, IResource[] resources, IJavaElement[] expectedRemainingFolders, IResource[] expectedRemainingResources) {
		boolean removeOnlyPackages= false;
		assertCorrectRemovalOfElements(folders, resources, expectedRemainingFolders, expectedRemainingResources, removeOnlyPackages);
	}

	private static void assertCorrectRemovalOfElements(IJavaElement[] folders, IResource[] resources, IJavaElement[] expectedRemainingFolders, IResource[] expectedRemainingResources,
			boolean removeOnlyPackages) {
		ParentChecker checker= new ParentChecker(resources, folders);
		checker.removeElementsWithAncestorsOnList(removeOnlyPackages);
		IJavaElement[] actualRemainingPackages= checker.getJavaElements();
		IResource[] actualRemainingResources= checker.getResources();

		assertEquals("wrong folders remained after removing packages with ancestors",
				asLinkedHashSet(expectedRemainingFolders), asLinkedHashSet(actualRemainingPackages));
		assertEquals("wrong resources remain after removing resources with ancestors",
				asLinkedHashSet(expectedRemainingResources), asLinkedHashSet(actualRemainingResources));
	}

	private static <T> LinkedHashSet<T> asLinkedHashSet(T[] elements) {
		return new LinkedHashSet<>(Arrays.asList(elements));
	}
}
