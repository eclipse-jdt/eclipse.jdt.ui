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
import java.util.Map;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.tests.core.rules.Java14ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

@RunWith(JUnit4.class)
public class QuickFixTest14 extends QuickFixTest {

//	private static final Class<QuickFixTest14> THIS= QuickFixTest14.class;

    @Rule
    public ProjectTestSetup projectsetup = new Java14ProjectTestSetup(true);

    private IJavaProject fJProject1;

    private IPackageFragmentRoot fSourceFolder;

	private static String MODULE_INFO_FILE_CONTENT = ""
										+ "module test {\n"
										+ "}\n";

	@After
	public void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}
	}

	@Test
	public void testEnablePreviewsAndOpenCompilerPropertiesProposals() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public record Rec1() {\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, null);

		assertNumberOfProposals(proposals, 2);
		String label1= CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features;
		assertProposalExists(proposals, label1);
		String label2= CorrectionMessages.PreviewFeaturesSubProcessor_open_compliance_properties_page_enable_preview_features;
		assertProposalExists(proposals, label2);
	}

	@Test
	public void testRecordSuppressWarningsProposals() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, true);

		Map<String, String> options= fJProject1.getOptions(false);
		options.put(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, JavaCore.WARNING);
		fJProject1.setOptions(options);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public record Rec1() {\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, null);

		assertNumberOfProposals(proposals, 2);
		String label= Messages.format(CorrectionMessages.SuppressWarningsSubProcessor_suppress_warnings_label, new String[] { "preview", "Rec1" });
		assertProposalExists(proposals, label);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= ""
						+ "package test;\n"
						+ "@SuppressWarnings(\"preview\")\n"
						+ "public record Rec1() {\n"
						+ "}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Ignore("See bug 562103 comment 4")
	@Test
	public void testGetNeedHigherComplianceProposalsAndEnablePreviewsProposal() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set13CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public record Rec1() {\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, null);

		assertNumberOfProposals(proposals, 1);
		String label1= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_change_project_compliance_description, "14");
		String label2= CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features;
		String label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_combine_two_quickfixes, new String[] {label1, label2});
		assertProposalExists(proposals, label);
	}

	@Test
	public void testNoEnablePreviewProposal() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, true);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= ""
					+ "package test;\n"
					+ "public record Rec() {\n"
					+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("Rec.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<ICompletionProposal> proposals= collectAllCorrections(cu, astRoot, 0);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testAddDefaultCaseSwitchStatement1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

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
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

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
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

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
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

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
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

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
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
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
	public void testAddMissingCaseSwitchExpression() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

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
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java14ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

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
}
