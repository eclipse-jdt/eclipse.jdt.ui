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

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.tests.core.rules.Java15ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class QuickFixTest15 extends QuickFixTest {

	@Rule
	public ProjectTestSetup projectsetup= new Java15ProjectTestSetup(true);

	private IJavaProject fJProject;

	private IPackageFragmentRoot fSourceFolder;

	@Test
	public void testTextBlockGetNeedHigherComplianceProposal() throws Exception {
		fJProject= projectsetup.getProject();
		fJProject.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");


		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(QuickFixTest14.MODULE_INFO_FILE, QuickFixTest.MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			public class Cls1 {
				public static void main(String[] args) {
					String str= \"""
								Hello
								World
								\""";
					System.out.println(str);
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls1.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, null);

		assertNumberOfProposals(proposals, 1);
		String label= Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_change_project_compliance_description, "15");
		assertProposalExists(proposals, label);
	}


}
