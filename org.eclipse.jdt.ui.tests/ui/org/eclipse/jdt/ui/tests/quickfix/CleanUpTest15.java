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
 * Tests the cleanup features related to Java 16.
 */
public class CleanUpTest15 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java15ProjectTestSetup(false);

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testConcatToTextBlock() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void testSimple() {\n"
				+ "        // comment 1\n" //
				+ "        String x = \"\" +\n" //
    	        + "            \"public void foo() {\\n\" +\n" //
    	        + "            \"    System.out.println(\\\"abc\\\");\\n\" +\n" //
    	        + "            \"}\\n\"; // comment 2\n" //
    	        + "    }\n" //
    	        + "\n" //
				+ "    public void testTrailingSpacesAndInnerNewlines() {\n"
				+ "        String x = \"\" +\n" //
    	        + "            \"public \\nvoid foo() {  \\n\" +\n" //
    	        + "            \"    System.out.println\\\\(\\\"abc\\\");\\n\" +\n" //
    	        + "            \"}\\n\";\n" //
    	        + "    }\n" //
    	        + "\n" //
    	        + "    public void testLineContinuationAndTripleQuotes() {\n" //
				+ "        String x = \"\" +\n" //
    	        + "            \"abcdef\" +\n" //
    	        + "            \"ghijkl\\\"\\\"\\\"\\\"123\\\"\\\"\\\"\" +\n" //
    	        + "            \"mnop\\\\\";\n" //
    	        + "    }\n" //
				+ "}\n";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK);

		String expected1= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void testSimple() {\n" //
				+ "        // comment 1\n" //
				+ "        String x = \"\"\"\n" //
    	        + "        \tpublic void foo() {\n" //
    	        + "        \t    System.out.println(\"abc\");\n" //
    	        + "        \t}\n" //
    	        + "        \t\"\"\"; // comment 2\n" //
    	        + "    }\n" //
    	        + "\n" //
				+ "    public void testTrailingSpacesAndInnerNewlines() {\n" //
				+ "        String x = \"\"\"\n" //
    	        + "        \tpublic\\s\n"
    	        + "        \tvoid foo() {\\s\\s\n" //
    	        + "        \t    System.out.println\\\\(\"abc\");\n" //
    	        + "        \t}\n" //
    	        + "        \t\"\"\";\n" //
    	        + "    }\n" //
    	        + "\n" //
    	        + "    public void testLineContinuationAndTripleQuotes() {\n" //
				+ "        String x = \"\"\"\n" //
    	        + "        \tabcdef\\\n" //
    	        + "        \tghijkl\\\"\"\"\\\"123\\\"\"\"\\\n" //
    	        + "        \tmnop\\\\\"\"\";\n" //
    	        + "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testNoConcatToTextBlock() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
    	        + "    public void testNotThreeStrings() {\n" //
				+ "        String x = \n" //
    	        + "            \"abcdef\" +\n" //
    	        + "            \"ghijkl\";" //
    	        + "    }\n" //
    	        + "\n" //
    	        + "    public void testNotAllLiterals() {\n" //
				+ "        String x = \"\" +\n" //
    	        + "            \"abcdef\" +\n" //
    	        + "            \"ghijkl\" +\n" //
    	        + "            String.valueOf(true)\n;"
    	        + "    }\n" //
    	        + "\n" //
      	        + "    public void testNotAllLiterals2(String a) {\n" //
				+ "        String x = \"\" +\n" //
    	        + "            \"abcdef\" +\n" //
    	        + "            \"ghijkl\" +\n" //
    	        + "            a\n;"
    	        + "    }\n" //
    	        + "\n" //
   	            + "    public void testNotAllStrings() {\n" //
				+ "        String x = \"\" +\n" //
    	        + "            \"abcdef\" +\n" //
    	        + "            \"ghijkl\" +\n" //
    	        + "            3;\n;"
    	        + "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

}
