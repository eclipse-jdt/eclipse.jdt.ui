/*******************************************************************************
 * Copyright (c) 2025 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer using github copilot - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.ui.TestAnnotationModifier;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests for JUnit context menu actions (DisableTestAction).
 */
public class JUnitContextMenuTest {

	@RegisterExtension
	public ProjectTestSetup projectSetup= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject;
	private IPackageFragmentRoot fSourceFolder;

	@BeforeEach
	public void setUp() throws Exception {
		fJProject= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");

		JavaProjectHelper.addRTJar(fJProject);
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fJProject, cpe);
		JavaProjectHelper.set18CompilerOptions(fJProject);
	}

	@AfterEach
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testDisableTestAction_TogglesAnnotation() throws Exception {
		// Test that disabling and enabling works correctly
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String original= """
			package test1;

			import org.junit.jupiter.api.Test;

			public class MyTest {
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type= cu.getType("MyTest");
		IMethod method= type.getMethod("testMethod", new String[0]);

		// Initially not disabled
		assertFalse(TestAnnotationModifier.isDisabled(method), "Initially should not be disabled");

		// Add @Disabled annotation
		TestAnnotationModifier.addDisabledAnnotation(method, true);

		// Should now be disabled
		assertTrue(TestAnnotationModifier.isDisabled(method), "Should be disabled after adding annotation");
		String afterAdd= cu.getSource();
		assertTrue(afterAdd.contains("@Disabled"), "Should contain @Disabled");

		// Remove @Disabled annotation
		TestAnnotationModifier.removeDisabledAnnotation(method);

		// Should not be disabled anymore
		assertFalse(TestAnnotationModifier.isDisabled(method), "Should not be disabled after removing annotation");
		String afterRemove= cu.getSource();
		assertFalse(afterRemove.contains("@Disabled"), "Should not contain @Disabled");
	}

	@Test
	public void testDisableTestAction_NoMultipleAnnotations() throws Exception {
		// Test that adding @Disabled multiple times doesn't create duplicates
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String original= """
			package test1;

			import org.junit.jupiter.api.Test;

			public class MyTest {
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type= cu.getType("MyTest");
		IMethod method= type.getMethod("testMethod", new String[0]);

		// Add @Disabled annotation
		TestAnnotationModifier.addDisabledAnnotation(method, true);
		String afterFirstAdd= cu.getSource();
		int firstCount= countOccurrences(afterFirstAdd, "@Disabled");
		assertEquals(1, firstCount, "Should have exactly one @Disabled after first add");

		// Verify the check works
		assertTrue(TestAnnotationModifier.isDisabled(method), "Should be disabled");
	}

	@Test
	public void testDisableTestAction_JUnit4Support() throws Exception {
		// Test that JUnit 4 @Ignore is handled correctly
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		// Add JUnit 4 to classpath
		IClasspathEntry cpe4= JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fJProject, cpe4);

		String original= """
			package test1;

			import org.junit.Test;

			public class MyTest {
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type= cu.getType("MyTest");
		IMethod method= type.getMethod("testMethod", new String[0]);

		// Add @Ignore annotation (JUnit 4)
		TestAnnotationModifier.addDisabledAnnotation(method, false);

		String afterAdd= cu.getSource();
		assertTrue(afterAdd.contains("@Ignore"), "Should contain @Ignore");
		assertTrue(TestAnnotationModifier.isDisabled(method), "Should be disabled");

		// Remove @Ignore annotation
		TestAnnotationModifier.removeDisabledAnnotation(method);

		String afterRemove= cu.getSource();
		assertFalse(afterRemove.contains("@Ignore"), "Should not contain @Ignore");
		assertFalse(TestAnnotationModifier.isDisabled(method), "Should not be disabled");
	}

	@Test
	public void testDisableTestAction_ImportRetainedWhenOtherTestStillDisabled() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String original= """
			package test1;

			import org.junit.jupiter.api.Disabled;
			import org.junit.jupiter.api.Test;

			public class MyTest {
			    @Disabled
			    @Test
			    public void testMethod1() {
			        // test code
			    }

			    @Disabled
			    @Test
			    public void testMethod2() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type= cu.getType("MyTest");
		IMethod methodToEnable= type.getMethod("testMethod1", new String[0]);

		TestAnnotationModifier.removeDisabledAnnotation(methodToEnable);

		String source= cu.getSource();
		int method1Index= source.indexOf("public void testMethod1()");
		int method2Index= source.indexOf("public void testMethod2()");
		assertTrue(method1Index > 0, "First method should remain");
		assertTrue(method2Index > 0, "Second method should remain");
		assertFalse(source.substring(0, method1Index).contains("@Disabled"), "Should remove @Disabled from first method");
		assertTrue(source.substring(0, method2Index).contains("@Disabled"), "Second method should remain disabled");
		assertTrue(source.contains("import org.junit.jupiter.api.Disabled;"), "Disabled import must remain because second method still uses it");
	}

	@Test
	public void testDisableTestAction_ParameterizedTest() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String original= """
			package test1;

			import org.junit.jupiter.params.ParameterizedTest;
			import org.junit.jupiter.params.provider.ValueSource;

			public class MyTest {
			    @ParameterizedTest
			    @ValueSource(strings = {"test1", "test2"})
			    public void parameterizedMethod(String param) {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type= cu.getType("MyTest");
		IMethod method= type.getMethod("parameterizedMethod", new String[] { "QString;" });

		assertFalse(TestAnnotationModifier.isDisabled(method), "Parameterized test should initially not be disabled");

		TestAnnotationModifier.addDisabledAnnotation(method, true);

		String afterAdd= cu.getSource();
		assertTrue(afterAdd.contains("import org.junit.jupiter.api.Disabled;"), "Should add Disabled import");
		assertTrue(afterAdd.contains("@Disabled"), "Should add @Disabled annotation");
		assertTrue(TestAnnotationModifier.isDisabled(method), "Parameterized test should be disabled after adding annotation");

		TestAnnotationModifier.removeDisabledAnnotation(method);

		String afterRemove= cu.getSource();
		assertFalse(afterRemove.contains("@Disabled"), "Should remove @Disabled annotation");
		assertFalse(afterRemove.contains("import org.junit.jupiter.api.Disabled;"), "Should remove Disabled import when no longer used");
		assertTrue(afterRemove.contains("import org.junit.jupiter.params.ParameterizedTest;"), "Should retain ParameterizedTest import");
		assertTrue(afterRemove.contains("import org.junit.jupiter.params.provider.ValueSource;"), "Should retain ValueSource import");
		assertFalse(TestAnnotationModifier.isDisabled(method), "Parameterized test should be enabled after removing annotation");
	}

	/**
	 * Helper method to count occurrences of a string
	 */
	private int countOccurrences(String text, String pattern) {
		int count= 0;
		int index= 0;
		while ((index= text.indexOf(pattern, index)) != -1) {
			count++;
			index += pattern.length();
		}
		return count;
	}
}
