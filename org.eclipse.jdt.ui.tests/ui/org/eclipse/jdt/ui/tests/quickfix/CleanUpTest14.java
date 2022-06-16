/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat Inc. and others.
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

import org.eclipse.jdt.ui.tests.core.rules.Java14ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 14.
 */
public class CleanUpTest14 extends CleanUpTestCase {

	@Rule
	public ProjectTestSetup projectSetup= new Java14ProjectTestSetup(false);

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testConvertToSwitchExpressionMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public int foo(int j) {\n" //
				+ "        // return value\n" //
				+ "        int i;\n" //
				+ "        // logic comment\n" //
				+ "        switch (j) {\n" //
				+ "            case 1:\n" //
				+ "            case 2:\n" //
				+ "                System.out.println(\"here\"); // comment 1\n" //
				+ "                // comment 2\n" //
				+ "                i = 7; // comment 3\n" //
				+ "            break;\n" //
				+ "            case 3: throw new RuntimeException(); // throw comment\n" //
				+ "            default:\n" //
				+ "                i = 8; // value 8\n" //
				+ "        }\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public int foo(int j) {\n" //
				+ "        // return value\n" //
				+ "        int i = switch (j) {\n" //
				+ "            case 1, 2 -> {\n"
				+ "                System.out.println(\"here\"); // comment 1\n" //
				+ "                // comment 2\n" //
				+ "                yield 7; // comment 3\n" //
				+ "            }\n" //
				+ "            case 3 -> throw new RuntimeException(); // throw comment\n" //
				+ "            default -> 8; // value 8\n" //
				+ "        };\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private int i;\n" //
				+ "    public void foo(int j) {\n" //
				+ "        // logic comment\n" //
				+ "        switch (j) {\n" //
				+ "            case 1:\n" //
				+ "            case 2:\n" //
				+ "                System.out.println(\"here\");\n" //
				+ "                // comment 1\n" //
				+ "                i = 7; // comment 2\n" //
				+ "            break;\n" //
				+ "            default:\n" //
				+ "                i = 8; // value 8\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private int i;\n" //
				+ "    public void foo(int j) {\n" //
				+ "        // logic comment\n" //
				+ "        i = switch (j) {\n" //
				+ "            case 1, 2 -> {\n"
				+ "                System.out.println(\"here\");\n" //
				+ "                // comment 1\n" //
				+ "                yield 7; // comment 2\n" //
				+ "            }\n" //
				+ "            default -> 8; // value 8\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionStaticInitializer() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Random;\n" //
				+ "public class E {\n" //
				+ "    private static int i;\n" //
				+ "    static {\n" //
				+ "        Random rand= new Random();\n" //
				+ "        int j = rand.nextInt(10);\n" //
				+ "        // logic comment\n" //
				+ "        switch (j) {\n" //
				+ "            case 1:\n" //
				+ "            case 2:\n" //
				+ "                System.out.println(\"here\");\n" //
				+ "                // comment 2\n" //
				+ "                i = 7; // comment 3\n" //
				+ "            break;\n" //
				+ "            default:\n" //
				+ "                i = 8; // value 8\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.Random;\n" //
				+ "public class E {\n" //
				+ "    private static int i;\n" //
				+ "    static {\n" //
				+ "        Random rand= new Random();\n" //
				+ "        int j = rand.nextInt(10);\n" //
				+ "        // logic comment\n" //
				+ "        i = switch (j) {\n" //
				+ "            case 1, 2 -> {\n"
				+ "                System.out.println(\"here\");\n" //
				+ "                // comment 2\n" //
				+ "                yield 7; // comment 3\n" //
				+ "            }\n" //
				+ "            default -> 8; // value 8\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionEnumsNoDefault() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public enum Day {\n" //
				+ "        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n" //
				+ "    }\n" //
				+ "    public int foo(Day day) {\n" //
				+ "        // return value\n" //
				+ "        int i = 0;\n" //
				+ "        // logic comment\n" //
				+ "        switch (day) {\n" //
				+ "            case SATURDAY:\n" //
				+ "            case SUNDAY:\n" //
				+ "                i = 5;\n" //
				+ "            break;\n" //
				+ "            case MONDAY:\n" //
				+ "            case TUESDAY:\n" //
				+ "            case WEDNESDAY:\n" //
				+ "                i = 7;\n" //
				+ "            break;\n" //
				+ "            case THURSDAY:\n" //
				+ "            case FRIDAY:\n" //
				+ "                i = 14;\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public enum Day {\n" //
				+ "        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n" //
				+ "    }\n" //
				+ "    public int foo(Day day) {\n" //
				+ "        // return value\n" //
				+ "        int i = 0;\n" //
				+ "        // logic comment\n" //
				+ "        i = switch (day) {\n" //
				+ "            case SATURDAY, SUNDAY -> 5;\n" //
				+ "            case MONDAY, TUESDAY, WEDNESDAY -> 7;\n" //
				+ "            case THURSDAY, FRIDAY -> 14;\n" //
				+ "        };\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionBug574824() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(String[] args) {\n" //
				+ "        // comment 1\n" //
				+ "        final File file;\n" //
				+ "        switch (args[1]) {\n" //
				+ "            case \"foo\":\n" //
				+ "                file = new File(\"foo.txt\");\n" //
				+ "                break;\n" //
				+ "            case \"bar\":\n" //
				+ "                file = new File(\"bar.txt\");\n" //
				+ "                break;\n" //
				+ "            default:\n" //
				+ "                file = new File(\"foobar.txt\");\n" //
				+ "        }\n" //
				+ "        System.err.println(file);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(String[] args) {\n" //
				+ "        // comment 1\n" //
				+ "        final File file = switch (args[1]) {\n" //
				+ "            case \"foo\" -> new File(\"foo.txt\");\n" //
				+ "            case \"bar\" -> new File(\"bar.txt\");\n" //
				+ "            default -> new File(\"foobar.txt\");\n" //
				+ "        };\n" //
				+ "        System.err.println(file);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionBug578130() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
			    + "    public void foo(String[] args) throws Exception {\n" //
			    + "        boolean isWhiteSpace;\n" //
			    + "        switch (args[0].charAt(0)) {\n" //
			    + "            case 10: /* \\ u000a: LINE FEED */\n" //
			    + "            case 12: /* \\ u000c: FORM FEED */\n" //
			    + "            case 13: /* \\ u000d: CARRIAGE RETURN */\n" //
			    + "            case 32: /* \\ u0020: SPACE */\n" //
			    + "            case 9: /* \\ u0009: HORIZONTAL TABULATION */\n" //
			    + "                isWhiteSpace = true; /* comment x */\n" //
			    + "                break;\n" //
			    + "            case 0:\n" //
			    + "            	   throw new Exception(\"invalid char\"); //$NON-NLS-1$\n" //
			    + "            case 95:\n" //
			    + "            {\n" //
			    + "                System.out.println(\"here\"); //$NON-NLS-1$\n" //
			    + "            	   isWhiteSpace = false;\n" //
			    + "            }\n" //
			    + "            break;\n" //
			    + "            default:\n" //
			    + "                isWhiteSpace = false;\n" //
			    + "        }\n" //
			    + "        System.out.println(isWhiteSpace);\n" //
			    + "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
			    + "    public void foo(String[] args) throws Exception {\n" //
			    + "        boolean isWhiteSpace = switch (args[0].charAt(0)) {\n" //
			    + "            case 10: /* \\ u000a: LINE FEED */\n" //
			    + "            case 12: /* \\ u000c: FORM FEED */\n" //
			    + "            case 13: /* \\ u000d: CARRIAGE RETURN */\n" //
			    + "            case 32: /* \\ u0020: SPACE */\n" //
			    + "            case 9: /* \\ u0009: HORIZONTAL TABULATION */\n" //
			    + "                yield true; /* comment x */\n" //
			    + "            case 0:\n" //
			    + "                throw new Exception(\"invalid char\"); //$NON-NLS-1$\n" //
			    + "            case 95: {\n" //
			    + "                System.out.println(\"here\"); //$NON-NLS-1$\n" //
			    + "                yield false;\n" //
			    + "            }\n" //
			    + "            default:\n" //
			    + "                yield false;\n" //
			    + "        };\n" //
			    + "        System.out.println(isWhiteSpace);\n" //
			    + "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionBug578129_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
			    + "    public void foo(String[] args) throws Exception {\n" //
			    + "        boolean isWhiteSpace;\n" //
			    + "        switch (args[0].charAt(0)) {\n" //
			    + "            case 10:\n" //
			    + "            case 12:\n" //
			    + "            case 13:\n" //
			    + "            case 32:\n" //
			    + "            case 9:\n" //
			    + "                isWhiteSpace = true; /* comment x */\n" //
			    + "                break;\n" //
			    + "            case 0:\n" //
			    + "            	   throw new Exception(\"invalid char\"); //$NON-NLS-1$\n" //
			    + "            case 95:\n" //
			    + "            default:\n" //
			    + "                isWhiteSpace = false;\n" //
			    + "        }\n" //
			    + "        System.out.println(isWhiteSpace);\n" //
			    + "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
			    + "    public void foo(String[] args) throws Exception {\n" //
			    + "        boolean isWhiteSpace = switch (args[0].charAt(0)) {\n" //
			    + "            case 10, 12, 13, 32, 9 -> true; /* comment x */\n" //
			    + "            case 0 -> throw new Exception(\"invalid char\"); //$NON-NLS-1$\n" //
			    + "            case 95 -> false;\n" //
			    + "            default -> false;\n" //
			    + "        };\n" //
			    + "        System.out.println(isWhiteSpace);\n" //
			    + "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionBug578129_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
			    + "    public void foo(String[] args) throws Exception {\n" //
			    + "        boolean isWhiteSpace;\n" //
			    + "        switch (args[0].charAt(0)) {\n" //
			    + "            case 10:\n" //
			    + "            case 12:\n" //
			    + "            case 13:\n" //
			    + "            case 32:\n" //
			    + "            case 9:\n" //
			    + "                // comment 1\n"
			    + "                isWhiteSpace = true; /* comment x */\n" //
			    + "                break;\n" //
			    + "            case 0:\n" //
			    + "            	   throw new Exception(\"invalid char\"); //$NON-NLS-1$\n" //
			    + "            case 95:\n" //
			    + "            default: {\n" //
			    + "                System.out.println(\"non-whitespace\");\n" //
			    + "                isWhiteSpace = false;\n" //
			    + "            }\n" //
			    + "        }\n" //
			    + "        System.out.println(isWhiteSpace);\n" //
			    + "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
			    + "    public void foo(String[] args) throws Exception {\n" //
			    + "        boolean isWhiteSpace = switch (args[0].charAt(0)) {\n" //
			    + "            case 10, 12, 13, 32, 9 -> /* comment 1 */ true; /* comment x */\n" //
			    + "            case 0 -> throw new Exception(\"invalid char\"); //$NON-NLS-1$\n" //
			    + "            case 95 -> {\n" //
			    + "                System.out.println(\"non-whitespace\");\n" //
			    + "                yield false;\n" //
			    + "            }\n" //
			    + "            default -> {\n" //
			    + "                System.out.println(\"non-whitespace\");\n" //
			    + "                yield false;\n" //
			    + "            }\n" //
			    + "        };\n" //
			    + "        System.out.println(isWhiteSpace);\n" //
			    + "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToReturnSwitchExpressionIssue104_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public enum InnerEnum {\n"
				+ "        A, B, C, D;\n"
				+ "    }\n"
				+ "    public int foo(InnerEnum k) {\n"
				+ "        switch (k) {\n"
				+ "            case A:\n"
				+ "            case B:\n"
				+ "                /* comment 1 */\n"
				+ "                return 6; /* abc */\n"
				+ "            case C: {\n"
				+ "                System.out.println(\"x\"); //$NON-NLS-1$\n"
				+ "                /* comment 2 */\n"
				+ "                return 8; /* def */\n"
				+ "            }\n"
				+ "            case D:\n"
				+ "                // comment 3\n"
				+ "                return 9;\n"
				+ "            default:\n"
				+ "                throw new NullPointerException();\n"
				+ "        }\n"
				+ "    }\n"
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public enum InnerEnum {\n"
				+ "        A, B, C, D;\n"
				+ "    }\n"
				+ "    public int foo(InnerEnum k) {\n"
				+ "        return switch (k) {\n"
				+ "            case A, B -> /* comment 1 */ 6; /* abc */\n"
				+ "            case C -> {\n"
				+ "                System.out.println(\"x\"); //$NON-NLS-1$\n"
				+ "                /* comment 2 */\n"
				+ "                yield 8; /* def */\n"
				+ "            }\n"
				+ "            case D -> /* comment 3 */ 9;\n"
				+ "            default -> throw new NullPointerException();\n"
				+ "        };\n"
				+ "    }\n"
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToReturnSwitchExpressionIssue104_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public enum InnerEnum {\n"
				+ "        A, B, C, D;\n"
				+ "    }\n"
				+ "    public int foo(InnerEnum k) {\n"
				+ "        switch (k) {\n"
				+ "            case A:\n"
				+ "            case B:\n"
				+ "                /* comment 1 */\n"
				+ "                return 6; /* abc */\n"
				+ "            case C:\n"
				+ "                System.out.println(\"x\"); //$NON-NLS-1$\n"
				+ "                /* comment 2 */\n"
				+ "                return 8; /* def */\n"
				+ "            case D:\n"
				+ "                return 9;\n"
				+ "        }\n"
				+ "    }\n"
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public enum InnerEnum {\n"
				+ "        A, B, C, D;\n"
				+ "    }\n"
				+ "    public int foo(InnerEnum k) {\n"
				+ "        return switch (k) {\n"
				+ "            case A, B -> /* comment 1 */ 6; /* abc */\n"
				+ "            case C -> {\n"
				+ "                System.out.println(\"x\"); //$NON-NLS-1$\n"
				+ "                /* comment 2 */\n"
				+ "                yield 8; /* def */\n"
				+ "            }\n"
				+ "            case D -> 9;\n"
				+ "        };\n"
				+ "    }\n"
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testDoNotConvertToReturnSwitchExpressionIssue104_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public enum InnerEnum {\n"
				+ "        A, B, C, D;\n"
				+ "    }\n"
				+ "    public int foo(InnerEnum k) {\n"
				+ "        switch (k) {\n"
				+ "            case A:\n"
				+ "                System.out.println(\"a\");\n"
				+ "            case B:\n"
				+ "                /* comment 1 */\n"
				+ "                return 6; /* abc */\n"
				+ "            case C: {\n"
				+ "                System.out.println(\"x\"); //$NON-NLS-1$\n"
				+ "                /* comment 2 */\n"
				+ "                return 8; /* def */\n"
				+ "            }\n"
				+ "            case D:\n"
				+ "                return 9;\n"
				+ "            default:\n"
				+ "                throw new NullPointerException();\n"
				+ "        }\n"
				+ "    }\n"
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToReturnSwitchExpressionIssue104_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public enum InnerEnum {\n"
				+ "        A, B, C, D;\n"
				+ "    }\n"
				+ "    public int foo(InnerEnum k, int x) {\n"
				+ "        switch (k) {\n"
				+ "            case A:\n"
				+ "                System.out.println(\"a\");\n"
				+ "            case B:\n"
				+ "                /* comment 1 */\n"
				+ "                if (x > 3)\n"
				+ "                    return 6; /* abc */\n"
				+ "                else\n"
				+ "                    return 10;\n"
				+ "            case C: {\n"
				+ "                System.out.println(\"x\"); //$NON-NLS-1$\n"
				+ "                /* comment 2 */\n"
				+ "                return 8; /* def */\n"
				+ "            }\n"
				+ "            case D:\n"
				+ "                return 9;\n"
				+ "            default:\n"
				+ "                throw new NullPointerException();\n"
				+ "        }\n"
				+ "    }\n"
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionNoBreak() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int foo(int j) {\n" //
				+ "        // return value\n" //
				+ "        int i;\n" //
				+ "        switch (j) {\n" //
				+ "            case 1:\n" //
				+ "                i = 8; // can't refactor with no break\n" //
				+ "            case 2:\n" //
				+ "                i = 7; // value 7\n" //
				+ "            break;\n" //
				+ "            default:\n" //
				+ "                i = 8; // value 8\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionNoStatements() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int foo(int j) {\n" //
				+ "        // return value\n" //
				+ "        int i = 0;\n" //
				+ "        switch (j) {\n" //
				+ "            case 1:\n" //
				+ "                break; // can't refactor with no statements\n" //
				+ "            case 2:\n" //
				+ "                i = 7; // value 7\n" //
				+ "            break;\n" //
				+ "            default:\n" //
				+ "                i = 8; // value 8\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionNoAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int foo(int j) {\n" //
				+ "        // return value\n" //
				+ "        int i = 0;\n" //
				+ "        switch (j) {\n" //
				+ "            case 1:\n" //
				+ "                System.out.println(\"here\");\n" //
				+ "                break; // can't refactor with no assignment to i\n" //
				+ "            case 2:\n" //
				+ "                i = 7; // value 7\n" //
				+ "            break;\n" //
				+ "            default:\n" //
				+ "                i = 8; // value 8\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionNoLastAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int foo(int j) {\n" //
				+ "        // return value\n" //
				+ "        int i = 0;\n" //
				+ "        switch (j) {\n" //
				+ "            case 1:\n" //
				+ "                i = 6; // assignment not last statement\n" //
				+ "                System.out.println(\"here\");\n" //
				+ "                break;\n" //
				+ "            case 2:\n" //
				+ "                i = 7; // value 7\n" //
				+ "            break;\n" //
				+ "            default:\n" //
				+ "                i = 8; // value 8\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionIfElse() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int foo(int j, int k) {\n" //
				+ "        // return value\n" //
				+ "        int i;\n" //
				+ "        switch (j) {\n" //
				+ "            case 1:\n" //
				+ "                if (k < 4) { // we don't delve into control statements\n" //
				+ "                    i = 6;\n" //
				+ "                } else {\n" //
				+ "                    i = 9;\n" //
				+ "                }\n" //
				+ "                break;\n" //
				+ "            case 2:\n" //
				+ "                i = 7; // value 7\n" //
				+ "            break;\n" //
				+ "            default:\n" //
				+ "                i = 8; // value 8\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionWithTry() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int foo(int j, int k) {\n" //
				+ "        // return value\n" //
				+ "        int i;\n" //
				+ "        switch (j) {\n" //
				+ "            case 1:\n" //
				+ "                try { // we don't delve into try statements\n" //
				+ "                    i = 6;\n" //
				+ "                } finally {\n" //
				+ "                    i = 9;\n" //
				+ "                }\n" //
				+ "                break;\n" //
				+ "            case 2:\n" //
				+ "                i = 7; // value 7\n" //
				+ "            break;\n" //
				+ "            default:\n" //
				+ "                i = 8; // value 8\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionReturn() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public int foo(int j) {\n" //
				+ "        // return value\n" //
				+ "        int i;\n" //
				+ "        switch (j) {\n" //
				+ "            case 1:\n" //
				+ "                return 6; // we don't support return\n" //
				+ "            case 2:\n" //
				+ "                i = 7; // value 7\n" //
				+ "            break;\n" //
				+ "            default:\n" //
				+ "                i = 8; // value 8\n" //
				+ "            break;\n" //
				+ "        }\n" //
				+ "        return i;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionBug578128() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
			    + "    public static void main(String[] args) {\n" //
			    + "        boolean rulesOK = true;\n" //
			    + "        switch (args[0].charAt(0)) {\n" //
			    + "            case '+':\n" //
			    + "                args[0] = \"+\";\n" //
			    + "                break;\n" //
			    + "            case '~':\n" //
			    + "                args[0] = \"~\";\n" //
			    + "                break;\n" //
			    + "            case '-':\n" //
			    + "                args[0] = \"-\";\n" //
			    + "                break;\n" //
			    + "            case '?':\n" //
			    + "                args[0] = \"?\";\n" //
			    + "                break;\n" //
			    + "            default:\n" //
			    + "                rulesOK = false;\n" //
			    + "        }\n" //
			    + "        System.out.println(rulesOK);\n" //
			    + "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

}
