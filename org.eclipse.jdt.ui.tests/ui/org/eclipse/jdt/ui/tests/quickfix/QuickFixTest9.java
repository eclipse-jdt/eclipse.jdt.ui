/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
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
import java.util.List;

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

import org.eclipse.jdt.ui.tests.core.Java9ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

import junit.framework.Test;
import junit.framework.TestSuite;

public class QuickFixTest9 extends QuickFixTest {

	private static final Class<QuickFixTest9> THIS= QuickFixTest9.class;

	private IJavaProject fJProject1;

	private IJavaProject fJProject2;

	private IPackageFragmentRoot fSourceFolder;

	private List<ICompilationUnit> fCus;

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
		JavaProjectHelper.addRequiredModularProject(fJProject2, Java9ProjectTestSetup.getProject());
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
		JavaProjectHelper.addRequiredModularProject(fJProject1, fJProject2);
		JavaProjectHelper.addRequiredModularProject(fJProject1, Java9ProjectTestSetup.getProject());

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		fCus= new ArrayList<>();
	}

	@Override
	protected void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}
		if (fJProject2 != null) {
			JavaProjectHelper.delete(fJProject2);
		}
		for (ICompilationUnit cu : fCus) {
			IEditorPart part= EditorUtility.isOpenInEditor(cu);
			if (part != null && part instanceof ITextEditor) {
				((ITextEditor)part).close(false);
			}
			if (cu.getJavaProject().exists()) {
				JavaProjectHelper.delete(cu.getJavaProject());
			}
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
	
	public void testAddModuleRequiresProposalForFullyQualifiedType() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public class Cls {\n");
		buf.append("    java.defaultProject.One one;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", buf.toString(), false, null);
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);

		String[] args= { "java.defaultProject" };
		final String proposalStr= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_requires_module_info, args);

		assertProposalExists(proposals, proposalStr);
	}
	
	public void testAddNewTypeProposals() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("  exports test.examples;");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", buf.toString(), false, null);

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

	public void testBasicNewServiceProvider() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("import java.sql.Driver;\n");
		buf.append("module test {\n");
		buf.append("provides Driver with test.IFoo;\n");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("import java.sql.Driver;\n");
		buf.append("package test;\n\n");
		buf.append("public interface IFoo extends Driver {\n");
		buf.append("}\n");
		pack.createCompilationUnit("IFoo.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);
		String proposalStr= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_add_provider_method_description, "Driver");
		assertProposalExists(proposals, proposalStr);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String actual= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("import java.sql.Driver;\n");
		buf.append("package test;\n\n");
		buf.append("public interface IFoo extends Driver {\n\n");
		buf.append("	/**\n");
		buf.append("	 * @return\n");
		buf.append("	 */\n");
		buf.append("	public static Driver provider() {\n");
		buf.append("		// TODO Auto-generated method stub\n");
		buf.append("		return null;\n");
		buf.append("	}\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEquals(expected, actual);
	}

	public void testMultipleNewServiceProvider() throws Exception {
		IJavaProject jProject1= JavaProjectHelper.createJavaProject("TestProject_1", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject1, Java9ProjectTestSetup.getProject());
		IPackageFragmentRoot fProject1Src = JavaProjectHelper.addSourceContainer(jProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("provides test.IFoo with test.Foo;\n");
		buf.append("}\n");
		IPackageFragment def= fProject1Src.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack=fProject1Src.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public interface IFoo {\n");
		buf.append("}\n");
		pack.createCompilationUnit("IFoo.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public interface Foo extends test.IFoo {\n");
		buf.append("}\n");
		fCus.add(pack.createCompilationUnit("Foo.java", buf.toString(), false, null));

		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public interface Bar extends test.IFoo {\n");
		buf.append("}\n");
		pack.createCompilationUnit("Bar.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public interface FooFoo extends test.Foo {\n");
		buf.append("}\n");
		pack.createCompilationUnit("FooFoo.java", buf.toString(), false, null);
		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);
		assertNumberOfProposals(proposals, 1);

		IJavaCompletionProposal proposal= proposals.get(0);
		assertEquals("Create 'IFoo' provider method", proposal.getDisplayString());

		proposal.apply(null); // force computing the proposal details

		String[] choices= { "IFoo - test", "Foo - test", "FooFoo - test", "Bar - test" };
		assertLinkedChoices(proposal, "return_type", choices);
	}

	public void testServiceProviderVisibility() throws Exception {
		// Project 1 (The Libraries)
		IJavaProject jProject1= JavaProjectHelper.createJavaProject("TestProject_1", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject1, Java9ProjectTestSetup.getProject());
		IPackageFragmentRoot fProject1Src = JavaProjectHelper.addSourceContainer(jProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test1 {\n");
		buf.append("exports test1;");
		buf.append("}\n");
		IPackageFragment def= fProject1Src.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fProject1Src.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n\n");
		buf.append("public interface IFoo {\n");
		buf.append("}\n");
		fCus.add(pack.createCompilationUnit("IFoo.java", buf.toString(), false, null));

		// Project 2 (The HiddenFoo class is a valid proposal, if visible)
		IJavaProject jProject2= JavaProjectHelper.createJavaProject("TestProject_2", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject2);
		JavaProjectHelper.addRequiredModularProject(jProject2, jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject2, Java9ProjectTestSetup.getProject());
		IPackageFragmentRoot fProject2Src = JavaProjectHelper.addSourceContainer(jProject2, "src");

		buf= new StringBuffer();
		buf.append("module test2 {\n");
		buf.append("requires test1;");
		buf.append("}\n");
		def= fProject2Src.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		pack= fProject2Src.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n\n");
		buf.append("public interface HiddenFoo extends test1.IFoo {\n");
		buf.append("}\n");
		fCus.add(pack.createCompilationUnit("HiddenFoo.java", buf.toString(), false, null));

		// Project 3  (The client toggles visibility to HiddenFoo, of Project 2)
		IJavaProject jProject3= JavaProjectHelper.createJavaProject("TestProject_3", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject3);
		JavaProjectHelper.addRequiredModularProject(jProject3, jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject3, Java9ProjectTestSetup.getProject());
		IPackageFragmentRoot fProject3Src = JavaProjectHelper.addSourceContainer(jProject3, "src");

		pack= fProject3Src.createPackageFragment("test3", false, null);
		buf= new StringBuffer();
		buf.append("package test3;\n\n");
		buf.append("public interface Foo extends test1.IFoo {\n");
		buf.append("}\n");
		fCus.add(pack.createCompilationUnit("Foo.java", buf.toString(), false, null));

		buf= new StringBuffer();
		buf.append("module test3 {\n");
		buf.append("requires test1;");
		buf.append("provides test1.IFoo with test3.Foo;\n");
		buf.append("}\n");
		def= fProject3Src.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", buf.toString(), false, null);

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

	public void testServiceProviderLocalTypeVisibility() throws Exception {
		IJavaProject jProject1= JavaProjectHelper.createJavaProject("TestProject_1", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject1, Java9ProjectTestSetup.getProject());
		IPackageFragmentRoot fProject1Src = JavaProjectHelper.addSourceContainer(jProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("provides test.IFoo with test.IFoo;\n");
		buf.append("}\n");
		IPackageFragment def= fProject1Src.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack= fProject1Src.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public interface IFoo {\n");
		buf.append("}\n");
		fCus.add(pack.createCompilationUnit("IFoo.java", buf.toString(), false, null));

		// NonPublicFoo should be accessible
		// PrivateFoo should not be accessible
		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("class NonPublicFoo implements test.IFoo {\n");
		buf.append("private interface PrivateFoo extends test.IFoo {\n");
		buf.append("}\n");
		buf.append("}\n");
		pack.createCompilationUnit("Foo.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);
		assertNumberOfProposals(proposals, 1);
		IJavaCompletionProposal proposal= proposals.get(0);
		proposal.apply(null); // force computing the proposal details

		String[] choices= new String [] { "IFoo - test", "NonPublicFoo - test", };
		assertLinkedChoices(proposal, "return_type", choices);
	}

	public void testServiceProviderConstructorProposal () throws Exception {
		IJavaProject jProject1= JavaProjectHelper.createJavaProject("TestProject_1", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject1, Java9ProjectTestSetup.getProject());
		IPackageFragmentRoot fProject1Src = JavaProjectHelper.addSourceContainer(jProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("provides test.IFoo with test.Foo;\n");
		buf.append("}\n");
		IPackageFragment def= fProject1Src.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack=fProject1Src.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public interface IFoo {\n");
		buf.append("}\n");
		pack.createCompilationUnit("IFoo.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public class Foo implements test.IFoo {\n");
		buf.append("public Foo (String arg) {\n");
		buf.append("}\n");
		buf.append("}\n");
		fCus.add(pack.createCompilationUnit("Foo.java", buf.toString(), false, null));

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);
		String proposalStr= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createconstructor_description, "Foo()");
		assertProposalExists(proposals, proposalStr);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String actual= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public class Foo implements test.IFoo {\n");
		buf.append("public Foo (String arg) {\n");
		buf.append("}\n\n");
		buf.append("/**\n");
		buf.append(" * \n");
		buf.append(" */\n");
		buf.append("public Foo() {\n");
		buf.append("\t// TODO Auto-generated constructor stub\n");
		buf.append("}\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEquals(expected, actual);
	}

	public void testServiceProviderVisibilityProposal () throws Exception {
		IJavaProject jProject1= JavaProjectHelper.createJavaProject("TestProject_1", "bin");
		JavaProjectHelper.set9CompilerOptions(jProject1);
		JavaProjectHelper.addRequiredModularProject(jProject1, Java9ProjectTestSetup.getProject());
		IPackageFragmentRoot fProject1Src = JavaProjectHelper.addSourceContainer(jProject1, "src");

		StringBuffer buf= new StringBuffer();
		buf.append("module test {\n");
		buf.append("provides test.IFoo with test.Foo;\n");
		buf.append("}\n");
		IPackageFragment def= fProject1Src.createPackageFragment("", false, null);
		ICompilationUnit cu= def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack=fProject1Src.createPackageFragment("test", false, null);
		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public interface IFoo {\n");
		buf.append("}\n");
		pack.createCompilationUnit("IFoo.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public class Foo implements test.IFoo {\n");
		buf.append("private Foo () {\n");
		buf.append("}\n");
		buf.append("}\n");
		fCus.add(pack.createCompilationUnit("Foo.java", buf.toString(), false, null));

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);
		String proposalStr= CorrectionMessages.LocalCorrectionsSubProcessor_changeconstructor_public_description;
		assertProposalExists(proposals, proposalStr);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String actual= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test;\n\n");
		buf.append("public class Foo implements test.IFoo {\n");
		buf.append("public Foo () {\n");
		buf.append("}\n");
		buf.append("}\n");
		String expected= buf.toString();
		assertEquals(expected, actual);
	}
}
