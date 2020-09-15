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

import org.eclipse.jdt.ui.tests.core.rules.Java15ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 15.
 */
public class CleanUpTest1d15 extends CleanUpTestCase {

	@Rule
    public ProjectTestSetup projectSetup = new Java15ProjectTestSetup(true);

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testRegexPatternForRecord() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public record E(int width, int height) {\n" //
				+ "    public void foo() {\n" //
				+ "        String k = \"bcd\";\n" //
				+ "        String m = \"abcdef\";\n" //
				+ "        String n = \"bcdefg\";\n" //
				+ "        String[] a = m.split(k);\n" //
				+ "        String[] b = n.split(k);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.regex.Pattern;\n" //
				+ "\n" //
				+ "public record E(int width, int height) {\n" //
				+ "    private static final Pattern k_pattern = Pattern.compile(\"bcd\");\n" //
				+ "\n" //
				+ "    public void foo() {\n" //
				+ "        Pattern k = k_pattern;\n" //
				+ "        String m = \"abcdef\";\n" //
				+ "        String n = \"bcdefg\";\n" //
				+ "        String[] a = k.split(m);\n" //
				+ "        String[] b = k.split(n);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

}
