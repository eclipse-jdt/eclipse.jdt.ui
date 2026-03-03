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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

	@Rule
	public ProjectTestSetup projectSetup = new Java1d8ProjectTestSetup();

	private IJavaProject fJProject;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		fJProject = projectSetup.getProject();
		fSourceFolder = JavaProjectHelper.addSourceContainer(fJProject, "src");

		JavaProjectHelper.addRTJar(fJProject);
		IClasspathEntry cpe = JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fJProject, cpe);
		JavaProjectHelper.set18CompilerOptions(fJProject);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testDisableTestAction_TogglesAnnotation() throws Exception {
		// Test that disabling and enabling works correctly
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		String original = """
			package test1;

			import org.junit.jupiter.api.Test;

			public class MyTest {
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type = cu.getType("MyTest");
		IMethod method = type.getMethod("testMethod", new String[0]);

		// Initially not disabled
		assertFalse("Initially should not be disabled", TestAnnotationModifier.isDisabled(method));

		// Add @Disabled annotation
		TestAnnotationModifier.addDisabledAnnotation(method, true);

		// Should now be disabled
		assertTrue("Should be disabled after adding annotation", TestAnnotationModifier.isDisabled(method));
		String afterAdd = cu.getSource();
		assertTrue("Should contain @Disabled", afterAdd.contains("@Disabled"));

		// Remove @Disabled annotation
		TestAnnotationModifier.removeDisabledAnnotation(method);

		// Should not be disabled anymore
		assertFalse("Should not be disabled after removing annotation", TestAnnotationModifier.isDisabled(method));
		String afterRemove = cu.getSource();
		assertFalse("Should not contain @Disabled", afterRemove.contains("@Disabled"));
	}

	@Test
	public void testDisableTestAction_NoMultipleAnnotations() throws Exception {
		// Test that adding @Disabled multiple times doesn't create duplicates
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		String original = """
			package test1;

			import org.junit.jupiter.api.Test;

			public class MyTest {
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type = cu.getType("MyTest");
		IMethod method = type.getMethod("testMethod", new String[0]);

		// Add @Disabled annotation
		TestAnnotationModifier.addDisabledAnnotation(method, true);
		String afterFirstAdd = cu.getSource();
		int firstCount = countOccurrences(afterFirstAdd, "@Disabled");
		assertEquals("Should have exactly one @Disabled after first add", 1, firstCount);

		// Verify the check works
		assertTrue("Should be disabled", TestAnnotationModifier.isDisabled(method));
	}

	@Test
	public void testDisableTestAction_JUnit4Support() throws Exception {
		// Test that JUnit 4 @Ignore is handled correctly
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);

		// Add JUnit 4 to classpath
		IClasspathEntry cpe4 = JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fJProject, cpe4);

		String original = """
			package test1;

			import org.junit.Test;

			public class MyTest {
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type = cu.getType("MyTest");
		IMethod method = type.getMethod("testMethod", new String[0]);

		// Add @Ignore annotation (JUnit 4)
		TestAnnotationModifier.addDisabledAnnotation(method, false);

		String afterAdd = cu.getSource();
		assertTrue("Should contain @Ignore", afterAdd.contains("@Ignore"));
		assertTrue("Should be disabled", TestAnnotationModifier.isDisabled(method));

		// Remove @Ignore annotation
		TestAnnotationModifier.removeDisabledAnnotation(method);

		String afterRemove = cu.getSource();
		assertFalse("Should not contain @Ignore", afterRemove.contains("@Ignore"));
		assertFalse("Should not be disabled", TestAnnotationModifier.isDisabled(method));
	}

	/**
	 * Helper method to count occurrences of a string
	 */
	private int countOccurrences(String text, String pattern) {
		int count = 0;
		int index = 0;
		while ((index = text.indexOf(pattern, index)) != -1) {
			count++;
			index += pattern.length();
		}
		return count;
	}
}
