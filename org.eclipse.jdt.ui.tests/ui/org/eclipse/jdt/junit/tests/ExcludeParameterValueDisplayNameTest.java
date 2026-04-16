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

import java.util.List;

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

import org.eclipse.jdt.internal.junit.ui.EnumSourceValidator;
import org.eclipse.jdt.internal.junit.ui.TestAnnotationModifier;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests for {@code @EnumSource} exclude/re-include functionality.
 *
 * <p>Validates {@link TestAnnotationModifier#excludeEnumValue(IMethod, String)},
 * {@link EnumSourceValidator#removeExcludeMode(IMethod)}, and
 * {@link EnumSourceValidator#removeValueFromExclusion(IMethod, String)}.
 */
public class ExcludeParameterValueDisplayNameTest {

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
	public void testExcludeEnumValue_AddsExcludeMode() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String original= """
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(Color.class)
				    public void testWithEnum(Color color) {
				    }
				}
				""";

		ICompilationUnit cu= pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type= cu.getType("MyTest");
		IMethod method= type.getMethod("testWithEnum", new String[] { "QColor;" });

		assertFalse(EnumSourceValidator.isExcludeMode(method), "Initially should not be in EXCLUDE mode");

		TestAnnotationModifier.excludeEnumValue(method, "RED");

		String source= cu.getSource();
		assertTrue(source.contains("mode"), "Should add mode attribute");
		assertTrue(source.contains("EXCLUDE"), "Should set EXCLUDE mode");
		assertTrue(source.contains("\"RED\""), "Should add RED to names");
		assertTrue(EnumSourceValidator.isExcludeMode(method), "Should now be in EXCLUDE mode");
	}

	@Test
	public void testExcludeEnumValue_AppendsToExistingExclusions() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String original= """
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				import org.junit.jupiter.params.provider.EnumSource.Mode;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, mode = Mode.EXCLUDE, names = {"RED"})
				    public void testWithEnum(Color color) {
				    }
				}
				""";

		ICompilationUnit cu= pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type= cu.getType("MyTest");
		IMethod method= type.getMethod("testWithEnum", new String[] { "QColor;" });

		assertTrue(EnumSourceValidator.isExcludeMode(method), "Initially should be in EXCLUDE mode");
		List<String> before= EnumSourceValidator.getExcludedNames(method);
		assertEquals(1, before.size(), "Should have one excluded name before");

		TestAnnotationModifier.excludeEnumValue(method, "GREEN");

		String source= cu.getSource();
		assertTrue(source.contains("\"RED\""), "Should still contain RED");
		assertTrue(source.contains("\"GREEN\""), "Should now contain GREEN");
		List<String> after= EnumSourceValidator.getExcludedNames(method);
		assertEquals(2, after.size(), "Should have two excluded names after");
		assertTrue(after.contains("RED"), "Should contain RED");
		assertTrue(after.contains("GREEN"), "Should contain GREEN");
	}

	@Test
	public void testRemoveExcludeMode_RemovesModeAndNames() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String original= """
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				import org.junit.jupiter.params.provider.EnumSource.Mode;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, mode = Mode.EXCLUDE, names = {"RED", "GREEN"})
				    public void testWithEnum(Color color) {
				    }
				}
				""";

		ICompilationUnit cu= pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type= cu.getType("MyTest");
		IMethod method= type.getMethod("testWithEnum", new String[] { "QColor;" });

		assertTrue(EnumSourceValidator.isExcludeMode(method), "Initially should be in EXCLUDE mode");

		EnumSourceValidator.removeExcludeMode(method);

		assertFalse(EnumSourceValidator.isExcludeMode(method), "Should no longer be in EXCLUDE mode");
		String source= cu.getSource();
		assertFalse(source.contains("mode"), "Should remove mode attribute");
		assertFalse(source.contains("EXCLUDE"), "Should remove EXCLUDE");
		assertFalse(source.contains("\"RED\""), "Should remove names");
	}

	@Test
	public void testRemoveValueFromExclusion_RemovesSingleValue() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String original= """
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				import org.junit.jupiter.params.provider.EnumSource.Mode;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, mode = Mode.EXCLUDE, names = {"RED", "GREEN"})
				    public void testWithEnum(Color color) {
				    }
				}
				""";

		ICompilationUnit cu= pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type= cu.getType("MyTest");
		IMethod method= type.getMethod("testWithEnum", new String[] { "QColor;" });

		List<String> before= EnumSourceValidator.getExcludedNames(method);
		assertEquals(2, before.size(), "Should have two excluded names before");

		EnumSourceValidator.removeValueFromExclusion(method, "RED");

		List<String> after= EnumSourceValidator.getExcludedNames(method);
		assertEquals(1, after.size(), "Should have one excluded name after");
		assertFalse(after.contains("RED"), "RED should be removed");
		assertTrue(after.contains("GREEN"), "GREEN should remain");
		assertTrue(EnumSourceValidator.isExcludeMode(method), "Should still be in EXCLUDE mode");
	}

	@Test
	public void testRemoveValueFromExclusion_LastValueRemovesMode() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String original= """
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				import org.junit.jupiter.params.provider.EnumSource.Mode;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, mode = Mode.EXCLUDE, names = {"RED"})
				    public void testWithEnum(Color color) {
				    }
				}
				""";

		ICompilationUnit cu= pack1.createCompilationUnit("MyTest.java", original, false, null);
		IType type= cu.getType("MyTest");
		IMethod method= type.getMethod("testWithEnum", new String[] { "QColor;" });

		assertTrue(EnumSourceValidator.isExcludeMode(method), "Initially should be in EXCLUDE mode");

		EnumSourceValidator.removeValueFromExclusion(method, "RED");

		assertFalse(EnumSourceValidator.isExcludeMode(method), "Should no longer be in EXCLUDE mode when all values re-included");
	}

	@Test
	public void testExtractEnumConstantFromDisplayName_StripsBracketPrefix() {
		assertEquals("RED", EnumSourceValidator.extractEnumConstantFromDisplayName("[1] RED"));
		assertEquals("GREEN", EnumSourceValidator.extractEnumConstantFromDisplayName("[2] GREEN"));
		assertEquals("BLUE", EnumSourceValidator.extractEnumConstantFromDisplayName("[10] BLUE"));
		assertEquals("SOME_VALUE", EnumSourceValidator.extractEnumConstantFromDisplayName("SOME_VALUE"));
		assertEquals("MY_CONSTANT", EnumSourceValidator.extractEnumConstantFromDisplayName("[5] MY_CONSTANT"));
	}

	@Test
	public void testComputeNamesAfterReinclude_RemovesValue() {
		java.util.List<String> excluded= java.util.Arrays.asList("RED", "GREEN", "BLUE");

		java.util.List<String> result= EnumSourceValidator.computeNamesAfterReinclude(excluded, "GREEN");

		assertEquals(2, result.size(), "Should have two names after reinclude");
		assertTrue(result.contains("RED"), "RED should remain");
		assertFalse(result.contains("GREEN"), "GREEN should be removed");
		assertTrue(result.contains("BLUE"), "BLUE should remain");
	}

	@Test
	public void testComputeNamesAfterReinclude_EmptyAfterLastRemoval() {
		java.util.List<String> excluded= java.util.Arrays.asList("RED");

		java.util.List<String> result= EnumSourceValidator.computeNamesAfterReinclude(excluded, "RED");

		assertTrue(result.isEmpty(), "Should be empty after removing the last name");
	}

	@Test
	public void testComputeNamesAfterReinclude_ValueNotPresent() {
		java.util.List<String> excluded= java.util.Arrays.asList("RED", "GREEN");

		java.util.List<String> result= EnumSourceValidator.computeNamesAfterReinclude(excluded, "BLUE");

		assertEquals(2, result.size(), "Should still have two names when value not present");
		assertTrue(result.contains("RED"), "RED should remain");
		assertTrue(result.contains("GREEN"), "GREEN should remain");
	}
}
