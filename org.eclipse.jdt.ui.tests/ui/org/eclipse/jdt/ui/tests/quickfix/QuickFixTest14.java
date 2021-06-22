/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.tests.core.rules.Java14ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

public class QuickFixTest14 extends QuickFixTest {
	@Rule
	public ProjectTestSetup projectSetup= new Java14ProjectTestSetup(true);

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public static final String MODULE_INFO_FILE= "module-info.java";
	public static final String MODULE_INFO_FILE_CONTENT= ""
										+ "module test {\n"
										+ "}\n";

	@Test
	public void testAddDefaultCaseSwitchStatement1() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public class Cls {\n"
					+ "    public static void foo(Day day) {\n"
					+ "        switch (day) {\n"
					+ "        case SATURDAY, SUNDAY -> System.out.println(\"Weekend\");\n"
					+ "        case MONDAY, TUESDAY, WEDNESDAY -> System.out.println(\"Weekday\");\n"
					+ "        }\n"
					+ "    }\n"
					+ "}\n"
					+ "\n"
					+ "enum Day {\n"
					+ "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String expected= ""
						+ "package test;\n"
						+ "public class Cls {\n"
						+ "    public static void foo(Day day) {\n"
						+ "        switch (day) {\n"
						+ "        case SATURDAY, SUNDAY -> System.out.println(\"Weekend\");\n"
						+ "        case MONDAY, TUESDAY, WEDNESDAY -> System.out.println(\"Weekday\");\n"
						+ "			default -> throw new IllegalArgumentException(\"Unexpected value: \" + day);\n"
						+ "        }\n"
						+ "    }\n"
						+ "}\n"
						+ "\n"
						+ "enum Day {\n"
						+ "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n"
						+ "}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testAddDefaultCaseSwitchStatement2() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public class Cls {\n"
					+ "    public static void foo(Day day) {\n"
					+ "        switch (day) {\n"
					+ "        case SATURDAY, SUNDAY: System.out.println(\"Weekend\");\n"
					+ "        case MONDAY, TUESDAY, WEDNESDAY: System.out.println(\"Weekday\");\n"
					+ "        }\n"
					+ "    }\n"
					+ "}\n"
					+ "\n"
					+ "enum Day {\n"
					+ "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String expected= ""
						+ "package test;\n"
						+ "public class Cls {\n"
						+ "    public static void foo(Day day) {\n"
						+ "        switch (day) {\n"
						+ "        case SATURDAY, SUNDAY: System.out.println(\"Weekend\");\n"
						+ "        case MONDAY, TUESDAY, WEDNESDAY: System.out.println(\"Weekday\");\n"
						+ "			default :\n"
						+ "				break;\n"
						+ "        }\n"
						+ "    }\n"
						+ "}\n"
						+ "\n"
						+ "enum Day {\n"
						+ "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n"
						+ "}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testAddDefaultCaseSwitchStatement3() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public class Cls {\n"
					+ "    public static void foo(Day day) {\n"
					+ "        switch (day) {\n"
					+ "        }\n"
					+ "    }\n"
					+ "}\n"
					+ "\n"
					+ "enum Day {\n"
					+ "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 7);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String expected= ""
						+ "package test;\n"
						+ "public class Cls {\n"
						+ "    public static void foo(Day day) {\n"
						+ "        switch (day) {\n"
						+ "			default :\n"
						+ "				break;\n"
						+ "        }\n"
						+ "    }\n"
						+ "}\n"
						+ "\n"
						+ "enum Day {\n"
						+ "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n"
						+ "}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testAddMissingCaseSwitchStatement1() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public class Cls {\n"
					+ "    public void bar1(Day day) {\n"
					+ "        switch (day) {\n"
					+ "            case MONDAY, FRIDAY -> System.out.println(Day.SUNDAY);\n"
					+ "            case TUESDAY                -> System.out.println(7);\n"
					+ "            case THURSDAY, SATURDAY     -> System.out.println(8);\n"
					+ "        }\n"
					+ "    }\n"
					+ "}\n"
					+ "enum Day {\n"
					+ "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= ""
						+ "package test;\n"
						+ "public class Cls {\n"
						+ "    public void bar1(Day day) {\n"
						+ "        switch (day) {\n"
						+ "            case MONDAY, FRIDAY -> System.out.println(Day.SUNDAY);\n"
						+ "            case TUESDAY                -> System.out.println(7);\n"
						+ "            case THURSDAY, SATURDAY     -> System.out.println(8);\n"
						+ "			case SUNDAY -> throw new UnsupportedOperationException(\"Unimplemented case: \" + day);\n"
						+ "			case WEDNESDAY -> throw new UnsupportedOperationException(\"Unimplemented case: \" + day);\n"
						+ "			default -> throw new IllegalArgumentException(\"Unexpected value: \" + day);\n"
						+ "        }\n"
						+ "    }\n"
						+ "}\n"
						+ "enum Day {\n"
						+ "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n"
						+ "}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testAddDefaultCaseSwitchExpression1() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public class Cls {\n"
					+ "    public static void bar3(int input) {\n"
					+ "        int num = switch (input) {\n"
					+ "        case 60, 600 -> 6;\n"
					+ "        case 70 -> 7;\n"
					+ "        case 80 -> 8;\n"
					+ "        case 90, 900 -> {\n"
					+ "            yield 9;\n"
					+ "        }\n"
					+ "        };\n"
					+ "    }\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= ""
						+ "package test;\n"
						+ "public class Cls {\n"
						+ "    public static void bar3(int input) {\n"
						+ "        int num = switch (input) {\n"
						+ "        case 60, 600 -> 6;\n"
						+ "        case 70 -> 7;\n"
						+ "        case 80 -> 8;\n"
						+ "        case 90, 900 -> {\n"
						+ "            yield 9;\n"
						+ "        }\n"
						+ "			default -> throw new IllegalArgumentException(\"Unexpected value: \" + input);\n"
						+ "        };\n"
						+ "    }\n"
						+ "}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testAddDefaultCaseSwitchExpression2() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public class Cls {\n"
					+ "    public static void bar4(int input) {\n"
					+ "        int num = switch (input) {\n"
					+ "        case 60, 600:\n"
					+ "            yield 6;\n"
					+ "        case 70:\n"
					+ "            yield 7;\n"
					+ "        case 80:\n"
					+ "            yield 8;\n"
					+ "        case 90, 900:\n"
					+ "            yield 9;\n"
					+ "        };\n"
					+ "    }\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= ""
						+ "package test;\n"
						+ "public class Cls {\n"
						+ "    public static void bar4(int input) {\n"
						+ "        int num = switch (input) {\n"
						+ "        case 60, 600:\n"
						+ "            yield 6;\n"
						+ "        case 70:\n"
						+ "            yield 7;\n"
						+ "        case 80:\n"
						+ "            yield 8;\n"
						+ "        case 90, 900:\n"
						+ "            yield 9;\n"
						+ "			default :\n"
						+ "				throw new IllegalArgumentException(\n"
						+ "						\"Unexpected value: \" + input);\n"
						+ "        };\n"
						+ "    }\n"
						+ "}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testAddDefaultCaseSwitchExpression3() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public class Cls {\n"
					+ "    public static void bar4(int input) {\n"
					+ "        int num = switch (input) {\n"
					+ "        };\n"
					+ "    }\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, 1, null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= ""
						+ "package test;\n"
						+ "public class Cls {\n"
						+ "    public static void bar4(int input) {\n"
						+ "        int num = switch (input) {\n"
						+ "			default :\n"
						+ "				throw new IllegalArgumentException(\n"
						+ "						\"Unexpected value: \" + input);\n"
						+ "        };\n"
						+ "    }\n"
						+ "}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testAddMissingCaseSwitchExpression() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public class Cls {\n"
					+ "    public void bar1(Day day) {\n"
					+ "        int len = switch (day) {\n"
					+ "        case MONDAY, FRIDAY:\n"
					+ "            yield 6;\n"
					+ "        case TUESDAY:\n"
					+ "            yield 7;\n"
					+ "        case THURSDAY, SATURDAY:\n"
					+ "            yield 8;\n"
					+ "        };\n"
					+ "    }\n"
					+ "}\n"
					+ "enum Day {\n"
					+ "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= ""
						+ "package test;\n"
						+ "public class Cls {\n"
						+ "    public void bar1(Day day) {\n"
						+ "        int len = switch (day) {\n"
						+ "        case MONDAY, FRIDAY:\n"
						+ "            yield 6;\n"
						+ "        case TUESDAY:\n"
						+ "            yield 7;\n"
						+ "        case THURSDAY, SATURDAY:\n"
						+ "            yield 8;\n"
						+ "			case SUNDAY :\n"
						+ "				throw new UnsupportedOperationException(\n"
						+ "						\"Unimplemented case: \" + day);\n"
						+ "			case WEDNESDAY :\n"
						+ "				throw new UnsupportedOperationException(\n"
						+ "						\"Unimplemented case: \" + day);\n"
						+ "			default :\n"
						+ "				throw new IllegalArgumentException(\n"
						+ "						\"Unexpected value: \" + day);\n"
						+ "        };\n"
						+ "    }\n"
						+ "}\n"
						+ "enum Day {\n"
						+ "    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n"
						+ "}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testReplaceIncorrectReturnInSwitchExpressionWithYieldStatement() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public class Cls {\n"
					+ "	public static int process(int i) {\n"
					+ "		var t = switch (i) {\n"
					+ "			case 0 -> {\n"
					+ "				return 99;\n"
					+ "			}\n"
					+ "			default ->100;\n"
					+ "		};\n"
					+ "		return t;\n"
					+ "	}\n\n"
					+ "	public static void main(String[] args) {\n"
					+ "		System.out.println(process(1));\n"
					+ "		System.out.println(process(0));\n"
					+ "	}\n"
					+ "}";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= ""
						+ "package test;\n"
						+ "public class Cls {\n"
						+ "	public static int process(int i) {\n"
						+ "		var t = switch (i) {\n"
						+ "			case 0 -> {\n"
						+ "				yield 99;\n"
						+ "			}\n"
						+ "			default ->100;\n"
						+ "		};\n"
						+ "		return t;\n"
						+ "	}\n\n"
						+ "	public static void main(String[] args) {\n"
						+ "		System.out.println(process(1));\n"
						+ "		System.out.println(process(0));\n"
						+ "	}\n"
						+ "}";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testReplaceIncorrectReturnInSwitchExpressionWithYieldStatement2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuilder buf= new StringBuilder();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= "" +
					"package test;\n" +
					"public class Cls {\n" +
					"\n" +
					"	public static int process(int i) {\n" +
					"		var t = switch (i) {\n" +
					"	        case 0:\n" +
					"	             return 1; // Error - Quick Fix works only if the return statement is surrounded in curly braces\n" +
					"	        default:\n" +
					"                     yield 100;\n" +
					"		};\n" +
					"       System.out.println(t);\n" +
					"		return t;\n" +
					"	}\n" +
					"	public static void main(String[] args) {\n" +
					"		process(1);\n" +
					"		process(0);\n" +
					"		process(2);\n" +
					"	}\n" +
					"}";

		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		String expected= "" +
				"package test;\n" +
				"public class Cls {\n" +
				"\n" +
				"	public static int process(int i) {\n" +
				"		var t = switch (i) {\n" +
				"	        case 0:\n" +
				"	             yield 1;\n" +
				"	        default:\n" +
				"                     yield 100;\n" +
				"		};\n" +
				"       System.out.println(t);\n" +
				"		return t;\n" +
				"	}\n" +
				"	public static void main(String[] args) {\n" +
				"		process(1);\n" +
				"		process(0);\n" +
				"		process(2);\n" +
				"	}\n" +
				"}";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}
}
