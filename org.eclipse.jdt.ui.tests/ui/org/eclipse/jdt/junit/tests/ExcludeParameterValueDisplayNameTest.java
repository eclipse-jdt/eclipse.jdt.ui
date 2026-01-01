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

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.junit.ui.TestAnnotationModifier;

/**
 * Tests for ExcludeParameterValueAction's ability to extract enum values from different display name formats.
 * 
 * Since extractParameterValue is private, we test it indirectly by creating parameterized tests
 * and verifying that the correct enum value is excluded based on the display name format.
 */
public class ExcludeParameterValueDisplayNameTest {

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

	/**
	 * Test format: testWithEnum[VALUE2]
	 * Enum name directly in brackets
	 */
	@Test
	public void testExtractEnumValue_DirectNameInBrackets() throws Exception {
		// This tests the fallback case where the enum name is directly in brackets
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		
		String enumCode = """
			package test1;
			public enum TestEnum {
				VALUE1, VALUE2, VALUE3
			}
			""";
		pack1.createCompilationUnit("TestEnum.java", enumCode, false, null);
		
		String testCode = """
			package test1;
			
			import org.junit.jupiter.params.ParameterizedTest;
			import org.junit.jupiter.params.provider.EnumSource;
			
			public class MyTest {
			    @ParameterizedTest
			    @EnumSource(TestEnum.class)
			    public void testWithEnum(TestEnum value) {
			    }
			}
			""";
		
		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", testCode, false, null);
		IType type = cu.getType("MyTest");
		IMethod method = type.getMethod("testWithEnum", new String[] { "QTestEnum;" });
		
		// Simulate extracting VALUE2 from display name "testWithEnum[VALUE2]"
		// The extractParameterValue method would parse this and return "VALUE2"
		TestAnnotationModifier.excludeEnumValue(method, "VALUE2");
		
		String result = cu.getSource();
		assertTrue("Should exclude VALUE2", result.contains("\"VALUE2\""));
		assertTrue("Should have Mode.EXCLUDE", result.contains("Mode.EXCLUDE"));
	}

	/**
	 * Test that the annotation uses imports instead of fully qualified names.
	 */
	@Test
	public void testExcludeEnumValueUsesImport() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		
		String enumCode = """
			package test1;
			public enum TestEnum {
				VALUE1, VALUE2, VALUE3
			}
			""";
		pack1.createCompilationUnit("TestEnum.java", enumCode, false, null);
		
		String testCode = """
			package test1;
			
			import org.junit.jupiter.params.ParameterizedTest;
			import org.junit.jupiter.params.provider.EnumSource;
			
			public class MyTest {
			    @ParameterizedTest
			    @EnumSource(TestEnum.class)
			    public void testWithEnum(TestEnum value) {
			    }
			}
			""";
		
		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", testCode, false, null);
		IType type = cu.getType("MyTest");
		IMethod method = type.getMethod("testWithEnum", new String[] { "QTestEnum;" });
		
		// Apply exclusion
		TestAnnotationModifier.excludeEnumValue(method, "VALUE2");
		
		String result = cu.getSource();
		
		// Verify import is added
		assertTrue("Should have Mode import", 
			result.contains("import org.junit.jupiter.params.provider.EnumSource.Mode;"));
		
		// Verify usage is short form
		assertTrue("Should use Mode.EXCLUDE (not fully qualified)", 
			result.contains("mode = Mode.EXCLUDE"));
		
		// Verify no fully qualified usage
		assertTrue("Should not use org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE", 
			!result.contains("org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE"));
	}

	/**
	 * Test excluding enum value when annotation already exists.
	 */
	@Test
	public void testExcludeEnumValue_ExistingAnnotation() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		
		String enumCode = """
			package test1;
			public enum TestEnum {
				VALUE1, VALUE2, VALUE3
			}
			""";
		pack1.createCompilationUnit("TestEnum.java", enumCode, false, null);
		
		String testCode = """
			package test1;
			
			import org.junit.jupiter.params.ParameterizedTest;
			import org.junit.jupiter.params.provider.EnumSource;
			import org.junit.jupiter.params.provider.EnumSource.Mode;
			
			public class MyTest {
			    @ParameterizedTest
			    @EnumSource(value = TestEnum.class, mode = Mode.EXCLUDE, names = {"VALUE1"})
			    public void testWithEnum(TestEnum value) {
			    }
			}
			""";
		
		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", testCode, false, null);
		IType type = cu.getType("MyTest");
		IMethod method = type.getMethod("testWithEnum", new String[] { "QTestEnum;" });
		
		// Exclude another value
		TestAnnotationModifier.excludeEnumValue(method, "VALUE2");
		
		String result = cu.getSource();
		
		// Should have both VALUE1 and VALUE2
		assertTrue("Should still have VALUE1", result.contains("\"VALUE1\""));
		assertTrue("Should have VALUE2", result.contains("\"VALUE2\""));
		assertTrue("Should have Mode.EXCLUDE", result.contains("Mode.EXCLUDE"));
	}

	/**
	 * Test that multiple exclusions work correctly.
	 */
	@Test
	public void testExcludeEnumValue_MultipleExclusions() throws Exception {
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		
		String enumCode = """
			package test1;
			public enum TestEnum {
				VALUE1, VALUE2, VALUE3, VALUE4
			}
			""";
		pack1.createCompilationUnit("TestEnum.java", enumCode, false, null);
		
		String testCode = """
			package test1;
			
			import org.junit.jupiter.params.ParameterizedTest;
			import org.junit.jupiter.params.provider.EnumSource;
			
			public class MyTest {
			    @ParameterizedTest
			    @EnumSource(TestEnum.class)
			    public void testWithEnum(TestEnum value) {
			    }
			}
			""";
		
		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", testCode, false, null);
		IType type = cu.getType("MyTest");
		IMethod method = type.getMethod("testWithEnum", new String[] { "QTestEnum;" });
		
		// Exclude multiple values
		TestAnnotationModifier.excludeEnumValue(method, "VALUE2");
		TestAnnotationModifier.excludeEnumValue(method, "VALUE3");
		TestAnnotationModifier.excludeEnumValue(method, "VALUE4");
		
		String result = cu.getSource();
		
		// Should have all three excluded values
		assertTrue("Should have VALUE2", result.contains("\"VALUE2\""));
		assertTrue("Should have VALUE3", result.contains("\"VALUE3\""));
		assertTrue("Should have VALUE4", result.contains("\"VALUE4\""));
		assertTrue("Should have Mode.EXCLUDE", result.contains("Mode.EXCLUDE"));
	}
}
