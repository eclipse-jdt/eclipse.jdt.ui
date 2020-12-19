/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.tests.core.rules.Java16ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

@RunWith(JUnit4.class)
public class QuickFixTest16 extends QuickFixTest {

	@Rule
	public ProjectTestSetup projectsetup= new Java16ProjectTestSetup(false);

	private IJavaProject fJProject;

	private IPackageFragmentRoot fSourceFolder;

	@After
	public void tearDown() throws Exception {
		if (fJProject != null) {
			JavaProjectHelper.delete(fJProject);
		}
	}

	@Test
	public void testRecordGetNeedHigherComplianceProposal() throws Exception {
		fJProject= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");


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
		String label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_change_project_compliance_description, "16");
		assertProposalExists(proposals, label);
	}

	@Test
	public void testRecordConstructorIncorrectParamsProposal1() throws Exception {
		fJProject= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
					"package test;\n" +
					"\n" +
					"public record Rec1(int a){\n" +
					"\n" +
					"	public static void main(String[] args) {\n" +
					"		Rec1 abc = new Rec1();\n" +
					"	}\n" +
					"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" +
						"package test;\n" +
						"\n" +
						"public record Rec1(int a){\n" +
						"\n" +
						"	public static void main(String[] args) {\n" +
						"		Rec1 abc = new Rec1(a);\n" +
						"	}\n" +
						"}\n";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= "" +
						"package test;\n" +
						"\n" +
						"public record Rec1(int a){\n" +
						"\n" +
						"	/**\n" +
						"	 * \n" +
						"	 */\n" +
						"	public Rec1() {\n" +
						"		// TODO Auto-generated constructor stub\n" +
						"	}\n" +
						"\n" +
						"	public static void main(String[] args) {\n" +
						"		Rec1 abc = new Rec1();\n" +
						"	}\n" +
						"}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRecordConstructorIncorrectParamsProposal2() throws Exception {
		fJProject= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
					"package test;\n" +
					"\n" +
					"public record Rec1(int a){\n" +
					"\n" +
					"	public static void main(String[] args) {\n" +
					"		Rec1 abc = new Rec1(10, 20);\n" +
					"	}\n" +
					"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" +
						"package test;\n" +
						"\n" +
						"public record Rec1(int a){\n" +
						"\n" +
						"	public static void main(String[] args) {\n" +
						"		Rec1 abc = new Rec1(10);\n" +
						"	}\n" +
						"}\n";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= "" +
						"package test;\n" +
						"\n" +
						"public record Rec1(int a){\n" +
						"\n" +
						"	/**\n" +
						"	 * @param i\n" +
						"	 * @param j\n" +
						"	 */\n" +
						"	public Rec1(int i, int j) {\n" +
						"		// TODO Auto-generated constructor stub\n" +
						"	}\n" +
						"\n" +
						"	public static void main(String[] args) {\n" +
						"		Rec1 abc = new Rec1(10, 20);\n" +
						"	}\n" +
						"}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRecordConstructorIncorrectParamsProposal3() throws Exception {
		fJProject= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
					"package test;\n" +
					"\n" +
					"public record Rec1(int a){\n" +
					"\n" +
					"	public static void main(String[] args) {\n" +
					"		Rec1 abc = new Rec1(\"str\");\n" +
					"	}\n" +
					"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" +
						"package test;\n" +
						"\n" +
						"public record Rec1(int a){\n" +
						"\n" +
						"	/**\n" +
						"	 * @param string\n" +
						"	 */\n" +
						"	public Rec1(String string) {\n" +
						"		// TODO Auto-generated constructor stub\n" +
						"	}\n" +
						"\n" +
						"	public static void main(String[] args) {\n" +
						"		Rec1 abc = new Rec1(\"str\");\n" +
						"	}\n" +
						"}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testRecordCanonicalConstructordUninitializedFieldProposal() throws Exception {
		fJProject= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
					"package test;\n" +
					"\n" +
					"public record Rec1(int a, int b){\n" +
					"\n" +
					"	public Rec1(int a, int b) {\n" +
					"		\n" +
					"	}\n\n" +
					"	public Rec1(int a) {\n" +
					"		this(a, a);\n" +
					"	}\n\n" +
					"	public Rec1(int a, int b, int c) {\n" +
					"		this(a, b+c);\n" +
					"	}\n\n" +
					"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Rec1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" +
					"package test;\n" +
					"\n" +
					"public record Rec1(int a, int b){\n" +
					"\n" +
					"	public Rec1(int a, int b) {\n" +
					"		this.a = 0;\n" +
					"		\n" +
					"	}\n\n" +
					"	public Rec1(int a) {\n" +
					"		this(a, a);\n" +
					"	}\n\n" +
					"	public Rec1(int a, int b, int c) {\n" +
					"		this(a, b+c);\n" +
					"	}\n\n" +
					"}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}
}
