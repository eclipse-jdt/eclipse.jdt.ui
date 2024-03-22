/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IEditorPart;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.tests.core.rules.Java9ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class QuickFixTest9 extends QuickFixTest {

	/**
	 * Project 3 depends on Project 2
	 * Project 2 depends on Project 1
	 */
	@Rule
	public ProjectTestSetup projectSetup1= new Java9ProjectTestSetup();

	@Rule
	public ProjectTestSetup projectSetup2= new Java9ProjectTestSetup("TestProject2");

	@Rule
	public ProjectTestSetup projectSetup3= new Java9ProjectTestSetup("TestProject3");

	private IJavaProject fJProject2;
	private IJavaProject fJProject3;

	private IPackageFragmentRoot fSourceFolder3;

	private List<ICompilationUnit> fCus;

	@Before
	public void setUp() throws CoreException {
		fJProject2= projectSetup2.getProject();
		JavaProjectHelper.set9CompilerOptions(fJProject2);
		JavaProjectHelper.addRequiredModularProject(fJProject2, projectSetup1.getProject());
		IPackageFragmentRoot java9Src= JavaProjectHelper.addSourceContainer(fJProject2, "src");
		IPackageFragment def= java9Src.createPackageFragment("", false, null);
		IPackageFragment pkgFrag= java9Src.createPackageFragment("java.defaultProject", false, null);
		String str= """
			module java.defaultProject {
			     exports java.defaultProject;\s
			}
			""";
		def.createCompilationUnit("module-info.java", str, false, null);
		String str1= """
			package java.defaultProject;\s
			
			 public class One {\s
			
			}
			""";
		pkgFrag.createCompilationUnit("One.java", str1, false, null);

		fJProject3= projectSetup3.getProject();
		JavaProjectHelper.set9CompilerOptions(fJProject3);
		JavaProjectHelper.addRequiredModularProject(fJProject3, fJProject2);
		JavaProjectHelper.addRequiredModularProject(fJProject3, projectSetup1.getProject());

		fSourceFolder3= JavaProjectHelper.addSourceContainer(fJProject3, "src");

		fCus= new ArrayList<>();
	}

	@After
	public void tearDown() throws Exception {
		for (ICompilationUnit cu : fCus) {
			IEditorPart part= EditorUtility.isOpenInEditor(cu);
			if (part instanceof ITextEditor) {
				((ITextEditor)part).close(false);
			}
			if (cu.getJavaProject().exists()) {
				JavaProjectHelper.delete(cu.getJavaProject());
			}
		}
	}

	@Test
	public void testAddModuleRequiresAndImportProposal() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder3.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder3.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    One one;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

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

	@Test
	public void testAddModuleRequiresProposal() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder3.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder3.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			import java.defaultProject.One;
			
			public class Cls {
			    One one;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, 0);

		String[] args= { "java.defaultProject" };
		final String proposalStr= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_requires_module_info, args);

		assertProposalExists(proposals, proposalStr);

		proposals= collectCorrections(cu, astRoot, 2, 1);
		assertProposalExists(proposals, proposalStr);
	}

	@Test
	public void testAddModuleRequiresProposalForFullyQualifiedType() throws Exception {
		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder3.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder3.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			public class Cls {
			    java.defaultProject.One one;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);

		String[] args= { "java.defaultProject" };
		final String proposalStr= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_requires_module_info, args);

		assertProposalExists(proposals, proposalStr);
	}

	@Test
	public void testAddNewTypeProposals() throws Exception {
		String str= """
			module test {
			  exports test.examples;\
			}
			""";
		IPackageFragment def= fSourceFolder3.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);

		String[] args= { "test.examples" };
		String proposalStr= Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewclass_inpackage_description, args);
		assertProposalExists(proposals, proposalStr);
		proposalStr= Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewinterface_inpackage_description, args);
		assertProposalExists(proposals, proposalStr);
		proposalStr= Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewannotation_inpackage_description, args);
		assertProposalExists(proposals, proposalStr);
		proposalStr= Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewenum_inpackage_description, args);
		assertProposalExists(proposals, proposalStr);
	}

	@Test
	public void testBasicNewServiceProvider() throws Exception {
		String str= """
			import java.sql.Driver;
			module test {
			provides Driver with test.IFoo;
			}
			""";
		IPackageFragment def= fSourceFolder3.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder3.createPackageFragment("test", false, null);
		String str1= """
			import java.sql.Driver;
			package test;
			
			public interface IFoo extends Driver {
			}
			""";
		pack.createCompilationUnit("IFoo.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);
		String proposalStr= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_add_provider_method_description, "Driver");
		assertProposalExists(proposals, proposalStr);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String actual= getPreviewContent(proposal);

		String expected= """
			import java.sql.Driver;
			package test;
			
			public interface IFoo extends Driver {
			
				/**
				 * @return
				 */
				public static Driver provider() {
					// TODO Auto-generated method stub
					return null;
				}
			}
			""";
		assertEquals(expected, actual);
	}

	@Test
	public void testMultipleNewServiceProvider() throws Exception {
		IJavaProject jProject1= JavaProjectHelper.createJavaProject("TestProject_1", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject1, projectSetup1.getProject());
		IPackageFragmentRoot fProject1Src = JavaProjectHelper.addSourceContainer(jProject1, "src");

		String str= """
			module test {
			provides test.IFoo with test.Foo;
			}
			""";
		IPackageFragment def= fProject1Src.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack=fProject1Src.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			public interface IFoo {
			}
			""";
		pack.createCompilationUnit("IFoo.java", str1, false, null);

		String str2= """
			package test;
			
			public interface Foo extends test.IFoo {
			}
			""";
		fCus.add(pack.createCompilationUnit("Foo.java", str2, false, null));

		String str3= """
			package test;
			
			public interface Bar extends test.IFoo {
			}
			""";
		pack.createCompilationUnit("Bar.java", str3, false, null);

		String str4= """
			package test;
			
			public interface FooFoo extends test.Foo {
			}
			""";
		pack.createCompilationUnit("FooFoo.java", str4, false, null);
		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);
		assertNumberOfProposals(proposals, 1);

		IJavaCompletionProposal proposal= proposals.get(0);
		assertEquals("Create 'IFoo' provider method", proposal.getDisplayString());

		proposal.apply(null); // force computing the proposal details

		String[] choices= { "IFoo - test", "Foo - test", "FooFoo - test", "Bar - test" };
		assertLinkedChoices(proposal, "return_type", choices);
	}

	@Test
	public void testServiceProviderVisibility() throws Exception {
		// Project 1 (The Libraries)
		IJavaProject jProject1= JavaProjectHelper.createJavaProject("TestProject_1", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject1, projectSetup1.getProject());
		IPackageFragmentRoot fProject1Src = JavaProjectHelper.addSourceContainer(jProject1, "src");

		String str= """
			module test1 {
			exports test1;\
			}
			""";
		IPackageFragment def= fProject1Src.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fProject1Src.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			
			public interface IFoo {
			}
			""";
		fCus.add(pack.createCompilationUnit("IFoo.java", str1, false, null));

		// Project 2 (The HiddenFoo class is a valid proposal, if visible)
		IJavaProject jProject2= JavaProjectHelper.createJavaProject("TestProject_2", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject2);
		JavaProjectHelper.addRequiredModularProject(jProject2, jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject2, projectSetup1.getProject());
		IPackageFragmentRoot fProject2Src = JavaProjectHelper.addSourceContainer(jProject2, "src");

		String str2= """
			module test2 {
			requires test1;\
			}
			""";
		def= fProject2Src.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str2, false, null);

		pack= fProject2Src.createPackageFragment("test2", false, null);
		String str3= """
			package test2;
			
			public interface HiddenFoo extends test1.IFoo {
			}
			""";
		fCus.add(pack.createCompilationUnit("HiddenFoo.java", str3, false, null));

		// Project 3  (The client toggles visibility to HiddenFoo, of Project 2)
		IJavaProject jProject3= JavaProjectHelper.createJavaProject("TestProject_3", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject3);
		JavaProjectHelper.addRequiredModularProject(jProject3, jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject3, projectSetup1.getProject());
		IPackageFragmentRoot fProject3Src = JavaProjectHelper.addSourceContainer(jProject3, "src");

		pack= fProject3Src.createPackageFragment("test3", false, null);
		String str4= """
			package test3;
			
			public interface Foo extends test1.IFoo {
			}
			""";
		fCus.add(pack.createCompilationUnit("Foo.java", str4, false, null));

		String str5= """
			module test3 {
			requires test1;\
			provides test1.IFoo with test3.Foo;
			}
			""";
		def= fProject3Src.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", str5, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		// HiddenFoo is not visible
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);
		assertNumberOfProposals(proposals, 1);
		IJavaCompletionProposal proposal= proposals.get(0);
		proposal.apply(null); // force computing the proposal details

		String[] choices= new String [] { "IFoo - test1", "Foo - test3" }; // NOT containing IllegalFoo
		assertLinkedChoices(proposal, "return_type", choices);

		JavaProjectHelper.addRequiredModularProject(jProject3, jProject2);

		// HiddenFoo is visible
		proposals= collectCorrections(cu, astRoot, 1, 0);
		assertNumberOfProposals(proposals, 1);
		proposal= proposals.get(0);
		proposal.apply(null); // force computing the proposal details

		choices= new String [] { "IFoo - test1", "Foo - test3", "HiddenFoo - test2" }; // containing IllegalFoo
		assertLinkedChoices(proposal, "return_type", choices);
	}

	@Test
	public void testServiceProviderLocalTypeVisibility() throws Exception {
		IJavaProject jProject1= JavaProjectHelper.createJavaProject("TestProject_1", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject1, projectSetup1.getProject());
		IPackageFragmentRoot fProject1Src = JavaProjectHelper.addSourceContainer(jProject1, "src");

		String str= """
			module test {
			provides test.IFoo with test.IFoo;
			}
			""";
		IPackageFragment def= fProject1Src.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fProject1Src.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			public interface IFoo {
			}
			""";
		fCus.add(pack.createCompilationUnit("IFoo.java", str1, false, null));

		// NonPublicFoo should be accessible
		// PrivateFoo should not be accessible
		String str2= """
			package test;
			
			class NonPublicFoo implements test.IFoo {
			private interface PrivateFoo extends test.IFoo {
			}
			}
			""";
		pack.createCompilationUnit("Foo.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);
		assertNumberOfProposals(proposals, 1);
		IJavaCompletionProposal proposal= proposals.get(0);
		proposal.apply(null); // force computing the proposal details

		String[] choices= new String [] { "IFoo - test", "NonPublicFoo - test", };
		assertLinkedChoices(proposal, "return_type", choices);
	}

	@Test
	public void testServiceProviderConstructorProposal () throws Exception {
		IJavaProject jProject1= JavaProjectHelper.createJavaProject("TestProject_1", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject1, projectSetup1.getProject());
		IPackageFragmentRoot fProject1Src = JavaProjectHelper.addSourceContainer(jProject1, "src");

		String str= """
			module test {
			provides test.IFoo with test.Foo;
			}
			""";
		IPackageFragment def= fProject1Src.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack=fProject1Src.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			public interface IFoo {
			}
			""";
		pack.createCompilationUnit("IFoo.java", str1, false, null);

		String str2= """
			package test;
			
			public class Foo implements test.IFoo {
			public Foo (String arg) {
			}
			}
			""";
		fCus.add(pack.createCompilationUnit("Foo.java", str2, false, null));

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);
		String proposalStr= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createconstructor_description, "Foo()");
		assertProposalExists(proposals, proposalStr);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String actual= getPreviewContent(proposal);

		String expected= """
			package test;
			
			public class Foo implements test.IFoo {
			public Foo (String arg) {
			}
			
			/**
			 *\s
			 */
			public Foo() {
				// TODO Auto-generated constructor stub
			}
			}
			""";
		assertEquals(expected, actual);
	}

	@Test
	public void testServiceProviderVisibilityProposal () throws Exception {
		IJavaProject jProject1= JavaProjectHelper.createJavaProject("TestProject_1", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject1, projectSetup1.getProject());
		IPackageFragmentRoot fProject1Src = JavaProjectHelper.addSourceContainer(jProject1, "src");

		String str= """
			module test {
			provides test.IFoo with test.Foo;
			}
			""";
		IPackageFragment def= fProject1Src.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack=fProject1Src.createPackageFragment("test", false, null);
		String str1= """
			package test;
			
			public interface IFoo {
			}
			""";
		pack.createCompilationUnit("IFoo.java", str1, false, null);

		String str2= """
			package test;
			
			public class Foo implements test.IFoo {
			private Foo () {
			}
			}
			""";
		fCus.add(pack.createCompilationUnit("Foo.java", str2, false, null));

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);
		String proposalStr= CorrectionMessages.LocalCorrectionsSubProcessor_changeconstructor_public_description;
		assertProposalExists(proposals, proposalStr);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String actual= getPreviewContent(proposal);

		String expected= """
			package test;
			
			public class Foo implements test.IFoo {
			public Foo () {
			}
			}
			""";
		assertEquals(expected, actual);
	}
}
