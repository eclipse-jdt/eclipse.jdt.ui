/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.tests.core.rules.Java10ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class AdvancedQuickAssistTest10 extends QuickFixTest {
	@Rule
    public ProjectTestSetup projectSetup= new Java10ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		JavaCore.setOptions(options);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testSplitLocalVarTypeVariable1() throws Exception {
		// 'if' in lambda body - positive cases
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String buf= """
			package test1;
			
			public class Test {
				public static void main(String[] args) {
				\t
			//		comment before
					var x = "abc";
			//		comment after
					System.out.println(x);\t
				}\t
			}""" ;
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("x");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= """
			package test1;
			
			public class Test {
				public static void main(String[] args) {
				\t
			//		comment before
					String x;
					x = "abc";
			//		comment after
					System.out.println(x);\t
				}\t
			}""";

		assertProposalPreviewEquals(buf, CorrectionMessages.QuickAssistProcessor_splitdeclaration_description, proposals);
	}

	@Test
	public void testSplitLocalVarTypeVariable2() throws Exception {
		// 'if' in lambda body - positive cases
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String buf= """
			package test1;
			
			public class Test {
				public static void main(String[] args) {
				\t
					System.out.println("Hello");
					for (/*var variable*/var x = 0; x< 10 ; x++) {
					\t
					}
			//		comment after
				}\t
			}""" ;
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("x");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= """
			package test1;
			
			public class Test {
				public static void main(String[] args) {
				\t
					System.out.println("Hello");
					/*var variable*/int x;
					for (x = 0; x< 10 ; x++) {
					\t
					}
			//		comment after
				}\t
			}""";

		assertProposalPreviewEquals(buf, CorrectionMessages.QuickAssistProcessor_splitdeclaration_description, proposals);
	}

	@Test
	public void testSplitLocalVarTypeVariable3() throws Exception {
		// 'if' in lambda body - positive cases
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String buf= """
			package test1;
			
			import java.util.Date;
			import java.util.HashMap;
			import java.util.HashSet;
			public class Helper {
				static HashMap<String, HashSet<Date>> getVal(){
					return null;
				}
			}""" ;
		pack1.createCompilationUnit("Helper.java", buf.toString(), false, null);
		buf= """
			package test1;
			
			public class Test {
				public static void main(String[] args) {
				\t
			//		comment before
					var x = Helper.getVal();
			//		comment after
					System.out.println(x);\t
				}\t
			}""" ;
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("x");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= """
			package test1;
			
			import java.util.Date;
			import java.util.HashMap;
			import java.util.HashSet;
			
			public class Test {
				public static void main(String[] args) {
				\t
			//		comment before
					HashMap<String, HashSet<Date>> x;
					x = Helper.getVal();
			//		comment after
					System.out.println(x);\t
				}\t
			}""";

		assertProposalPreviewEquals(buf, CorrectionMessages.QuickAssistProcessor_splitdeclaration_description, proposals);
	}

	@Test
	public void testSplitLocalVarTypeVariable4() throws Exception {
		// 'if' in lambda body - positive cases
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String buf= """
			package test1;
			
			import java.util.Date;
			import java.util.HashMap;
			import java.util.HashSet;
			public class Helper {
				static HashMap<String, HashSet<Date>> getVal(){
					return null;
				}
			}""" ;
		pack1.createCompilationUnit("Helper.java", buf.toString(), false, null);
		buf= """
			package test1;
			
			public class Test {
				public static void main(String[] args) {
				\t
					System.out.println("Hello");
					for (/*var variable*/var x = Helper.getVal();;) {
						System.out.println(x);
					}
			//		comment after
				}\t
			}""" ;
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		int offset= buf.toString().indexOf("x");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= """
			package test1;
			
			import java.util.Date;
			import java.util.HashMap;
			import java.util.HashSet;
			
			public class Test {
				public static void main(String[] args) {
				\t
					System.out.println("Hello");
					/*var variable*/HashMap<String,HashSet<Date>> x;
					for (x = Helper.getVal();;) {
						System.out.println(x);
					}
			//		comment after
				}\t
			}""";

		assertProposalPreviewEquals(buf, CorrectionMessages.QuickAssistProcessor_splitdeclaration_description, proposals);
	}

	@Test
	public void testVarOptionAvailableForLocalVarTypeVariable() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String buf= """
			package test1;
			
			import java.io.BufferedReader;
			
			public class Test {
				public void sample() {
					new BufferedReader(null);
				}
			}""";

		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		String str= "new BufferedReader(null)";
		AssistContext context= getCorrectionContext(cu, buf.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertProposalExists(proposals, CorrectionMessages.AssignToVariableAssistProposal_assigntolocal_description);
		assertProposalExists(proposals, CorrectionMessages.AssignToVariableAssistProposal_assignintrywithresources_description);
		String[] choices= new String [] { "BufferedReader - java.io", "var" }; // NOT containing IllegalFoo

		ICompletionProposal proposal= findProposalByName(CorrectionMessages.AssignToVariableAssistProposal_assigntolocal_description, proposals);
		ICompletionProposal proposal2=  findProposalByName(CorrectionMessages.AssignToVariableAssistProposal_assignintrywithresources_description, proposals);

		assertNotNull("Proposal `" + CorrectionMessages.AssignToVariableAssistProposal_assigntolocal_description + "` missing",proposal);
		assertNotNull("Proposal `" + CorrectionMessages.AssignToVariableAssistProposal_assignintrywithresources_description + "` missing",proposal2);

		assertLinkedChoicesContains(proposal, "type", choices);
        assertLinkedChoicesContains(proposal2, "type", choices);
	}
}
