/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

import org.eclipse.jdt.ui.tests.core.rules.Java16ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;


public class JUnit4TestFinderTest16 {

	private IJavaProject fProject;
	private IPackageFragmentRoot fRoot;

	@Rule
	public ProjectTestSetup projectsetup= new Java16ProjectTestSetup(false);


	@Before
	public void setUp() throws Exception {
		fProject= projectsetup.getProject();
		fProject.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fProject, cpe);

		fRoot= JavaProjectHelper.addSourceContainer(fProject, "src");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testTestAnnotation_bug575762() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		String str= """
			package p;
			
			import org.junit.Test;
			
			public record Test1() {
			        @Test public void testFoo() {
			        }
			}
			""";
		ICompilationUnit cu1= p.createCompilationUnit("Test1.java", str, true, null);

		IType[] types= cu1.getTypes();

		IType validTest1= types[0];

		assertTestFound(validTest1, new String[] { "p.Test1" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.Test1" });

		String[] validTests= { "p.Test1" };

		assertTestFound(p, validTests);
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}

	private void assertTestFound(IJavaElement container, String[] expectedTypes) throws CoreException {
		ITestKind testKind= TestKindRegistry.getContainerTestKind(container);
		assertEquals(TestKindRegistry.JUNIT4_TEST_KIND_ID, testKind.getId());

		ITestFinder finder= testKind.getFinder();

		if (container instanceof IType) {
			IType type= (IType) container;
			boolean isTest= expectedTypes.length == 1 && type.getFullyQualifiedName('.').equals(expectedTypes[0]);
			assertEquals(type.getFullyQualifiedName(), isTest, finder.isTest(type));
		}

		HashSet<IType> set= new HashSet<>(Arrays.asList(JUnitCore.findTestTypes(container, null)));
		HashSet<String> namesFound= new HashSet<>();
		for (IType curr : set) {
			namesFound.add(curr.getFullyQualifiedName('.'));
		}
		String[] actuals= namesFound.toArray(new String[namesFound.size()]);
		StringAsserts.assertEqualStringsIgnoreOrder(actuals, expectedTypes);
	}


}
