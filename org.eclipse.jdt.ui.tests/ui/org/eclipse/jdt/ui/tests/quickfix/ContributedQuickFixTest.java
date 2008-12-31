/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> - testInvertEquals1-23
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CUCorrectionProposal;

public class ContributedQuickFixTest extends QuickFixTest {

	private static final Class THIS= ContributedQuickFixTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;


	public ContributedQuickFixTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ContributedQuickFixTest("testContributedQuickFix1"));
			return new ProjectTestSetup(suite);
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");

		JavaCore.setOptions(options);

		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testContributedQuickAssist1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public String foo() {\n");
		buf.append("        return \"hello\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("hello");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public String foo() {\n");
		buf.append("        return \"HELLO\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });

	}

	public void testContributedQuickFix1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 99999999999999999;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });

	}

	public void testContributedQuickFix2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class B {\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 99999999999999999;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);

		assertNumberOfProposals(proposals, 0);
	}


}
