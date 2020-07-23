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

import org.eclipse.jdt.ui.tests.core.rules.Java15ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

@RunWith(JUnit4.class)
public class QuickFixTestPreview extends QuickFixTest {

//	private static final Class<QuickFixTest14> THIS= QuickFixTest14.class;

    @Rule
    public ProjectTestSetup projectsetup = new Java15ProjectTestSetup(true);

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
		fJProject1.setRawClasspath(Java15ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);

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
		fJProject1.setRawClasspath(Java15ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, true);

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
		fJProject1.setRawClasspath(Java15ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);

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
		fJProject1.setRawClasspath(Java15ProjectTestSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, true);

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
}
