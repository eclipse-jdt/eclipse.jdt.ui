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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.tests.core.Java12ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AssistQuickFixTest12 extends QuickFixTest {

	private static final Class<AssistQuickFixTest12> THIS= AssistQuickFixTest12.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public AssistQuickFixTest12(String name) {
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

	public void testSplitSwitchCaseStatement() throws Exception {
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
		buf.append("            case SATURDAY, SUNDAY: System.out.println(\"Weekend\");\n");
		buf.append("            case MONDAY, TUESDAY, WEDNESDAY: System.out.println(\"Weekday\");\n");
		buf.append("            case THURSDAY, FRIDAY: System.out.println(\"Weekday\");\n");
		buf.append("            default :\n");
		buf.append("                break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		IInvocationContext ctx= getCorrectionContext(cu, 185, 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void foo(Day day) {\n");
		buf.append("        switch (day) {\n");
		buf.append("            case SATURDAY, SUNDAY: System.out.println(\"Weekend\");\n");
		buf.append("            case MONDAY :\n");
		buf.append("				System.out.println(\"Weekday\");\n");
		buf.append("			case TUESDAY :\n");
		buf.append("				System.out.println(\"Weekday\");\n");
		buf.append("			case WEDNESDAY :\n");
		buf.append("				System.out.println(\"Weekday\");\n");
		buf.append("			case THURSDAY, FRIDAY: System.out.println(\"Weekday\");\n");
		buf.append("            default :\n");
		buf.append("                break;\n");
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

	public void testSplitSwitchCaseLabelRuleStatement() throws Exception {
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
		buf.append("        String weekDayOrEnd = switch (day) {\n");
		buf.append("            case SATURDAY, SUNDAY -> \"Weekend\";\n");
		buf.append("            case MONDAY, TUESDAY, WEDNESDAY -> \"Weekday\";\n");
		buf.append("            case THURSDAY, FRIDAY -> \"Weekday\";\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		IInvocationContext ctx= getCorrectionContext(cu, 239, 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    public static void foo(Day day) {\n");
		buf.append("        String weekDayOrEnd = switch (day) {\n");
		buf.append("            case SATURDAY, SUNDAY -> \"Weekend\";\n");
		buf.append("            case MONDAY, TUESDAY, WEDNESDAY -> \"Weekday\";\n");
		buf.append("            case THURSDAY -> \"Weekday\";\n");
		buf.append("			case FRIDAY -> \"Weekday\";\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("enum Day {\n");
		buf.append("    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}
}

