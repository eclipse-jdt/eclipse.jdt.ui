/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.tests.core.Java12ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

import junit.framework.Test;
import junit.framework.TestSuite;

public class QuickFixTest12 extends QuickFixTest {

	private static final Class<QuickFixTest12> THIS= QuickFixTest12.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public QuickFixTest12(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java12ProjectTestSetup(new TestSuite(THIS), true);
	}

	public static Test setUpTest(Test test) {
		Test testToReturn= new Java12ProjectTestSetup(test, true);
		return testToReturn;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}

		super.tearDown();
	}

	public void testEnablePreviewsAndOpenCompilerPropertiesProposals() throws Exception {

		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java12ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("  String foo(Day day) {\n");
		buf.append("	int x = 0;\n");
		buf.append("	var today = switch(day){\n");
		buf.append("		case SATURDAY, SUNDAY: break \"Weekend day\";\n");
		buf.append("		case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY: {\n");
		buf.append("   	 		var kind = \"Working day\";\n");
		buf.append("    		break kind;\n");
		buf.append("		}\n");
		buf.append("		default: {\n");
		buf.append("    		var kind = day.name();\n");
		buf.append("   	 		System.out.println(kind + x);\n");
		buf.append("   	 		throw new IllegalArgumentException(\"Invalid day: \" + kind);\n");
		buf.append("		}\n");
		buf.append("  	};\n");
		buf.append("  	return today;\n");
		buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public enum Day {\n");
		buf.append("	SUNDAY,\n");
		buf.append("	MONDAY,\n");
		buf.append("	TUESDAY,\n");
		buf.append("	WEDNESDAY,\n");
		buf.append("	THURSDAY,\n");
		buf.append("	FRIDAY,\n");
		buf.append("	SATURDAY\n");
		buf.append("}\n");
		pack.createCompilationUnit("Day.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3, null);

		assertNumberOfProposals(proposals, 2);
		String label1= CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features;
		assertProposalExists(proposals, label1);
		String label2= CorrectionMessages.PreviewFeaturesSubProcessor_open_compliance_properties_page_enable_preview_features;
		assertProposalExists(proposals, label2);
	}

	public void testNoEnablePreviewProposal() throws Exception {

		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java12ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, true);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("  String foo(Day day) {\n");
		buf.append("	int x = 0;\n");
		buf.append("	var today = switch(day){\n");
		buf.append("		case SATURDAY, SUNDAY: break \"Weekend day\";\n");
		buf.append("		case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY: {\n");
		buf.append("   	 		var kind = \"Working day\";\n");
		buf.append("    		break kind;\n");
		buf.append("		}\n");
		buf.append("		default: {\n");
		buf.append("    		var kind = day.name();\n");
		buf.append("   	 		System.out.println(kind + x);\n");
		buf.append("   	 		throw new IllegalArgumentException(\"Invalid day: \" + kind);\n");
		buf.append("		}\n");
		buf.append("  	};\n");
		buf.append("  	return today;\n");
		buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public enum Day {\n");
		buf.append("	SUNDAY,\n");
		buf.append("	MONDAY,\n");
		buf.append("	TUESDAY,\n");
		buf.append("	WEDNESDAY,\n");
		buf.append("	THURSDAY,\n");
		buf.append("	FRIDAY,\n");
		buf.append("	SATURDAY\n");
		buf.append("}\n");
		pack.createCompilationUnit("Day.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<ICompletionProposal> proposals= collectAllCorrections(cu, astRoot, 0);

		assertNumberOfProposals(proposals, 0);
	}

	public void testAddDefaultCaseSwitchStatement1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java12ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, true);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void foo(Day day) {\n");
		buf.append("        switch (day) {\n");
		buf.append("        case SATURDAY, SUNDAY -> System.out.println(\"Weekend\");\n");
		buf.append("        case MONDAY, TUESDAY, WEDNESDAY -> System.out.println(\"Weekday\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void foo(Day day) {\n");
		buf.append("        switch (day) {\n");
		buf.append("        case SATURDAY, SUNDAY -> System.out.println(\"Weekend\");\n");
		buf.append("        case MONDAY, TUESDAY, WEDNESDAY -> System.out.println(\"Weekday\");\n");
		buf.append("			default -> throw new IllegalArgumentException(\"Unexpected value: \" + day);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	public void testAddDefaultCaseSwitchStatement2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java12ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, true);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void foo(Day day) {\n");
		buf.append("        switch (day) {\n");
		buf.append("        case SATURDAY, SUNDAY: System.out.println(\"Weekend\");\n");
		buf.append("        case MONDAY, TUESDAY, WEDNESDAY: System.out.println(\"Weekday\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void foo(Day day) {\n");
		buf.append("        switch (day) {\n");
		buf.append("        case SATURDAY, SUNDAY: System.out.println(\"Weekend\");\n");
		buf.append("        case MONDAY, TUESDAY, WEDNESDAY: System.out.println(\"Weekday\");\n");
		buf.append("			default :\n");
		buf.append("				break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	public void testAddDefaultCaseSwitchStatement3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java12ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, true);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void foo(Day day) {\n");
		buf.append("        switch (day) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 7);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void foo(Day day) {\n");
		buf.append("        switch (day) {\n");
		buf.append("			default :\n");
		buf.append("				break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	public void testAddMissingCaseSwitchStatement1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java12ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, true);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void bar1(Day day) {\n");
		buf.append("        switch (day) {\n");
		buf.append("            case MONDAY, FRIDAY -> System.out.println(Day.SUNDAY);\n");
		buf.append("            case TUESDAY                -> System.out.println(7);\n");
		buf.append("            case THURSDAY, SATURDAY     -> System.out.println(8);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void bar1(Day day) {\n");
		buf.append("        switch (day) {\n");
		buf.append("            case MONDAY, FRIDAY -> System.out.println(Day.SUNDAY);\n");
		buf.append("            case TUESDAY                -> System.out.println(7);\n");
		buf.append("            case THURSDAY, SATURDAY     -> System.out.println(8);\n");
		buf.append("			case SUNDAY -> throw new UnsupportedOperationException(\"Unimplemented case: \" + day);\n");
		buf.append("			case WEDNESDAY -> throw new UnsupportedOperationException(\"Unimplemented case: \" + day);\n");
		buf.append("			default -> throw new IllegalArgumentException(\"Unexpected value: \" + day);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	public void testAddDefaultCaseSwitchExpression1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java12ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, true);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void bar3(int input) {\n");
		buf.append("        int num = switch (input) {\n");
		buf.append("        case 60, 600 -> 6;\n");
		buf.append("        case 70 -> 7;\n");
		buf.append("        case 80 -> 8;\n");
		buf.append("        case 90, 900 -> {\n");
		buf.append("            break 9;\n");
		buf.append("        }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void bar3(int input) {\n");
		buf.append("        int num = switch (input) {\n");
		buf.append("        case 60, 600 -> 6;\n");
		buf.append("        case 70 -> 7;\n");
		buf.append("        case 80 -> 8;\n");
		buf.append("        case 90, 900 -> {\n");
		buf.append("            break 9;\n");
		buf.append("        }\n");
		buf.append("			default -> throw new IllegalArgumentException(\"Unexpected value: \" + input);\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	public void testAddDefaultCaseSwitchExpression2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java12ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, true);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void bar4(int input) {\n");
		buf.append("        int num = switch (input) {\n");
		buf.append("        case 60, 600:\n");
		buf.append("            break 6;\n");
		buf.append("        case 70:\n");
		buf.append("            break 7;\n");
		buf.append("        case 80:\n");
		buf.append("            break 8;\n");
		buf.append("        case 90, 900:\n");
		buf.append("            break 9;\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void bar4(int input) {\n");
		buf.append("        int num = switch (input) {\n");
		buf.append("        case 60, 600:\n");
		buf.append("            break 6;\n");
		buf.append("        case 70:\n");
		buf.append("            break 7;\n");
		buf.append("        case 80:\n");
		buf.append("            break 8;\n");
		buf.append("        case 90, 900:\n");
		buf.append("            break 9;\n");
		buf.append("			default :\n");
		buf.append("				throw new IllegalArgumentException(\n");
		buf.append("						\"Unexpected value: \" + input);\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	public void testAddMissingCaseSwitchExpression() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(Java12ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, true);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void bar1(Day day) {\n");
		buf.append("        int len = switch (day) {\n");
		buf.append("        case MONDAY, FRIDAY:\n");
		buf.append("            break 6;\n");
		buf.append("        case TUESDAY:\n");
		buf.append("            break 7;\n");
		buf.append("        case THURSDAY, SATURDAY:\n");
		buf.append("            break 8;\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public void bar1(Day day) {\n");
		buf.append("        int len = switch (day) {\n");
		buf.append("        case MONDAY, FRIDAY:\n");
		buf.append("            break 6;\n");
		buf.append("        case TUESDAY:\n");
		buf.append("            break 7;\n");
		buf.append("        case THURSDAY, SATURDAY:\n");
		buf.append("            break 8;\n");
		buf.append("			case SUNDAY :\n");
		buf.append("				throw new UnsupportedOperationException(\n");
		buf.append("						\"Unimplemented case: \" + day);\n");
		buf.append("			case WEDNESDAY :\n");
		buf.append("				throw new UnsupportedOperationException(\n");
		buf.append("						\"Unimplemented case: \" + day);\n");
		buf.append("			default :\n");
		buf.append("				throw new IllegalArgumentException(\n");
		buf.append("						\"Unexpected value: \" + day);\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

}
