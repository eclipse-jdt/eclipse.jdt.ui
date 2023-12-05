/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.launcher.JUnit5TestFinder;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;


/**
 * Test if the <code>JUnit5TestFinder</code> can find tests annotated with the API of JUnit5
 * (Jupiter)
 */
@RunWith(Parameterized.class)
public class JUnit5TestFinderJupiterTest {

	record TestScenario(String testClass, int testTypesCount) {
	}

	private static final String JAVA_CLASS_NAME= "JupiterTests";

	private static final String JAVA_FILE_NAME= JAVA_CLASS_NAME + ".java";

	private static final Path TEST_CLASS_FILE= Path.of("testresources").resolve("testClasses").resolve(JAVA_FILE_NAME);

	private static IJavaProject javaProject;

	@Parameters(name= "{0}")
	public static Collection<TestScenario> getCompilationUnits() {

		// These are the current "valid" results
		return List.of(new TestScenario(JAVA_CLASS_NAME, 7), //
				new TestScenario("CustomTestAnnotation", 0), // Not a test class
				new TestScenario("FoundStatic", 1), //
				new TestScenario("FoundStaticCustomTestAnnotation", 1), //
				new TestScenario("NotFoundPrivate", 0), // private class
				new TestScenario("NotFoundHasNoTests", 0), // empty class (no tests)
				new TestScenario("FoundExtendsTestCase", 1), //
				new TestScenario("FoundExtendsTestCaseCustomTestAnnotation", 1), //
				new TestScenario("NotFoundPrivateExtendsTestCase", 0), // private class
				new TestScenario("FoundTestTemplateClass", 1), //
				new TestScenario("NotFoundAbstractWithInnerClass", 0), // can't be instantiated
				new TestScenario("NotFoundExtendsAbstractWithInnerWithTest", 0), // FIXME: why isn't this one found even though it can be instantiated?
				new TestScenario("FoundHasInnerClassWithNested", 1), //
				new TestScenario("NotFoundInnerInstanceClassWithTest", 0), // has test but it can't be instantiated (needs enclosing instance)
				new TestScenario("FoundExtendsAbstractWithNested", 1) //
		);

	}

	@Parameter
	public TestScenario scenario;

	private static ICompilationUnit compilationUnit;

	@BeforeClass
	public static void beforeClass() throws Exception {
		javaProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(javaProject);
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(javaProject, cpe);
		JavaProjectHelper.set18CompilerOptions(javaProject);

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(javaProject, "src");
		IPackageFragment packageFragment= root.createPackageFragment("somepackage", true, null);

		compilationUnit= createCompilationUnit(packageFragment);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		JavaProjectHelper.delete(javaProject);
	}

	@Test
	public void testTestKindRegistryGetContainerTestKind_isJUnit5TestFinder() throws Exception {
		IType type= findTypeWithName(scenario.testClass());

		ITestFinder finder= TestKindRegistry.getContainerTestKind(type).getFinder();
		assertThat(finder).isInstanceOf(JUnit5TestFinder.class);
	}

	@Test
	public void testFindTestsInContainer() throws Exception {
		IType type= findTypeWithName(scenario.testClass());
		Set<IType> foundTestTypes= new HashSet<>();

		JUnit5TestFinder objectUnderTest= new JUnit5TestFinder();
		objectUnderTest.findTestsInContainer(type, foundTestTypes, null);

		assertThat(foundTestTypes).hasSize(scenario.testTypesCount());
	}


	private IType findTypeWithName(String name) throws JavaModelException {
		for (IType type : compilationUnit.getAllTypes()) {
			if (type.getElementName().equals(name))
				return type;
		}
		return null;
	}

	private static ICompilationUnit createCompilationUnit(IPackageFragment packageFragment) throws Exception {
		String content= Files.readString(TEST_CLASS_FILE);
		return packageFragment.createCompilationUnit(JAVA_FILE_NAME, content, false, null);
	}
}
