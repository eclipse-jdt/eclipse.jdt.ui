/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.tests.core.Java9ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

import junit.framework.Test;
import junit.framework.TestSuite;

public class QuickFixTest9 extends QuickFixTest {

	private static final Class<QuickFixTest9> THIS= QuickFixTest9.class;

	private IJavaProject fJProject1;

	private IJavaProject fJProject2;

	private IPackageFragmentRoot fSourceFolder;

	public QuickFixTest9(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java9ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		Test testToReturn= new Java9ProjectTestSetup(test);
		return testToReturn;
	}

	@Override
	protected void setUp() throws CoreException {
		fJProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
		JavaProjectHelper.set9CompilerOptions(fJProject2);
		JavaProjectHelper.addRequiredProject(fJProject2, Java9ProjectTestSetup.getProject());
		IPackageFragmentRoot java9Src= JavaProjectHelper.addSourceContainer(fJProject2, "src");
		IPackageFragment def= java9Src.createPackageFragment("", false, null);
		IPackageFragment pkgFrag= java9Src.createPackageFragment("java.defaultProject", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("module java.defaultProject {\n");
		buf.append("     exports java.defaultProject; \n");
		buf.append("}\n");
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);
		StringBuffer buf2= new StringBuffer();
		buf2.append("package java.defaultProject; \n\n public class One { \n\n");
		buf2.append("}\n");
		pkgFrag.createCompilationUnit("One.java", buf2.toString(), false, null);

		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.set9CompilerOptions(fJProject1);
		JavaProjectHelper.addRequiredProject(fJProject1, fJProject2);
		JavaProjectHelper.addRequiredProject(fJProject1, Java9ProjectTestSetup.getProject());

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@Override
	protected void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.clear(fJProject1, Java9ProjectTestSetup.getDefaultClasspath());
		}
		if (fJProject2 != null) {
			JavaProjectHelper.clear(fJProject2, Java9ProjectTestSetup.getDefaultClasspath());
		}
		super.tearDown();
	}

	public void testAddModuleRequiresAndImportProposal() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Cls {\n");
		buf.append("    One one;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		String[] args= { "java.defaultProject" };
		String requiresStr= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_requires_module_info, args);
		requiresStr= requiresStr.substring(0, 1).toLowerCase() + requiresStr.substring(1);

		String[] args2= { "One", "java.defaultProject" };
		final String importStr= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_importtype_description, args2);
		String proposalStr= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_combine_two_proposals_info, new String[] { importStr, requiresStr });
		assertProposalExists(proposals, proposalStr);
	}

	public void testAddModuleRequiresProposal() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("import java.defaultProject.One;\n\n");
		buf.append("public class Cls {\n");
		buf.append("    One one;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, 0);

		String[] args= { "java.defaultProject" };
		final String proposalStr= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_requires_module_info, args);

		assertProposalExists(proposals, proposalStr);

		proposals= collectCorrections(cu, astRoot, 2, 1);
		assertProposalExists(proposals, proposalStr);
	}
}
