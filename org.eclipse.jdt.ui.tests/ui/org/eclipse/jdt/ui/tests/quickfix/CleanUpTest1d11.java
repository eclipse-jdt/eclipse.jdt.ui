/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
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
package org.eclipse.jdt.ui.tests.quickfix;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java11ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 11.
 */
public class CleanUpTest1d11 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java11ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testUseLocalVariableTypeInferenceInLambda1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.function.Predicate\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        Predicate<String> cc = (String s) -> { return s == null; };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.function.Predicate\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        Predicate<String> cc = (var s) -> { return s == null; };\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testUseLocalVariableTypeInferenceInLambda2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private interface I1 {\n" //
				+ "        public void run(String s, int i, Boolean b);\n" //
				+ "    }\n" //
				+ "    public void foo(int doNotRefactorParameter) {\n" //
				+ "        I1 i1 = (String s, int i, Boolean b) -> { System.out.println(\"hello\"); };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private interface I1 {\n" //
				+ "        public void run(String s, int i, Boolean b);\n" //
				+ "    }\n" //
				+ "    public void foo(int doNotRefactorParameter) {\n" //
				+ "        I1 i1 = (var s, var i, var b) -> { System.out.println(\"hello\"); };\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

}
