/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.internal.expressions.Messages;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.tests.core.rules.Java17ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class QuickFixTest17 extends QuickFixTest {

	@Rule
	public ProjectTestSetup projectsetup= new Java17ProjectTestSetup(false);

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@After
	public void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}
	}

	@Test
	public void testAddSealedMissingClassModifierProposal() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed class Shape permits Square {}\n" +
				"\n" +
				"class Square extends Shape {}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Shape.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" +
				"package test;\n" +
				"\n" +
				"public sealed class Shape permits Square {}\n" +
				"\n" +
				"final class Square extends Shape {}\n";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= "" +
				"package test;\n" +
				"\n" +
				"public sealed class Shape permits Square {}\n" +
				"\n" +
				"non-sealed class Square extends Shape {}\n";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= "" +
				"package test;\n" +
				"\n" +
				"public sealed class Shape permits Square {}\n" +
				"\n" +
				"sealed class Square extends Shape {}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}

	@Test
	public void testAddSealedMissingInterfaceModifierProposal() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface Shape permits Square {}\n" +
				"\n" +
				"interface Square extends Shape {}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Shape.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface Shape permits Square {}\n" +
				"\n" +
				"sealed interface Square extends Shape {}\n";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface Shape permits Square {}\n" +
				"\n" +
				"non-sealed interface Square extends Shape {}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testAddSealedAsDirectSuperTypeProposal1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed class Shape permits Square {\n" +
				"}\n";
		pack1.createCompilationUnit("Shape.java", test, false, null);

		test= "" +
				"package test;\n" +
				"\n" +
				"public non-sealed class Square extends Shape {\n" +
				"}\n";
		pack1.createCompilationUnit("Square.java", test, false, null);

		test= "" +
				"package test;\n" +
				"\n" +
				"public non-sealed class Circle extends Shape {\n" +
				"}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("Circle.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu2);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu2, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= "" +
				"package test;\n" +
				"\n" +
				"public sealed class Shape permits Square, Circle {\n" +
				"}\n";

		assertEqualString(preview, expected);
	}

	@Test
	public void testAddSealedAsDirectSuperTypeProposal2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface Shape permits Square {\n" +
				"}\n";
		pack1.createCompilationUnit("Shape.java", test, false, null);

		test= "" +
				"package test;\n" +
				"\n" +
				"public non-sealed class Square implements Shape {\n" +
				"}\n";
		pack1.createCompilationUnit("Square.java", test, false, null);

		test= "" +
				"package test;\n" +
				"\n" +
				"public non-sealed class Circle implements Shape {\n" +
				"}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("Circle.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu2);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu2, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface Shape permits Square, Circle {\n" +
				"}\n";

		assertEqualString(preview, expected);
	}

	@Test
	public void testAddSealedAsDirectSuperTypeProposal3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface Shape permits Square {\n" +
				"}\n";
		pack1.createCompilationUnit("Shape.java", test, false, null);

		test= "" +
				"package test;\n" +
				"\n" +
				"public non-sealed class Square implements Shape {\n" +
				"}\n";
		pack1.createCompilationUnit("Square.java", test, false, null);

		test= "" +
				"package test;\n" +
				"\n" +
				"public non-sealed interface Circle extends Shape {\n" +
				"}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("Circle.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu2);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu2, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface Shape permits Square, Circle {\n" +
				"}\n";

		assertEqualString(preview, expected);
	}

	@Test
	public void testAddSealedAsDirectSuperTypeProposal4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface Shape permits Square {\n" +
				"}\n";
		pack1.createCompilationUnit("Shape.java", test, false, null);

		test= "" +
				"package test;\n" +
				"\n" +
				"public non-sealed class Square implements Shape {\n" +
				"}\n";
		pack1.createCompilationUnit("Square.java", test, false, null);


		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		test= "" +
				"package test2;\n" +
				"\n" +
				"import test.Shape;\n" +
				"\n" +
				"public non-sealed interface Circle extends Shape {\n" +
				"}\n";
		ICompilationUnit cu2= pack2.createCompilationUnit("Circle.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu2);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu2, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= "" +
				"package test;\n" +
				"\n" +
				"import test2.Circle;\n" +
				"\n" +
				"public sealed interface Shape permits Square, Circle {\n" +
				"}\n";

		assertEqualString(preview, expected);
	}

	@Test
	public void testAddSealedAsDirectSuperInterface1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface IShape permits Circle {\\n" +
				"\n" +
				"}\n" +
				"\n" +
				"class Circle {\n" +
				"    \n" +
				"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("IShape.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface IShape permits Circle {\\n" +
				"\n" +
				"}\n" +
				"\n" +
				"class Circle implements IShape {\n" +
				"    \n" +
				"}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testAddSealedAsDirectSuperInterface2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface IShape permits IRectangle {\\n" +
				"\n" +
				"}\n" +
				"\n" +
				"interface IRectangle {\n" +
				"    \n" +
				"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("IShape.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface IShape permits IRectangle {\\n" +
				"\n" +
				"}\n" +
				"\n" +
				"interface IRectangle extends IShape {\n" +
				"    \n" +
				"}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testAddSealedAsDirectSuperInterface3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"import java.lang.annotation.IncompleteAnnotationException;\n" +
				"\n" +
				"public sealed interface IShape permits IncompleteAnnotationException {\\n" +
				"\n" +
				"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("IShape.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testAddSealedAsDirectSuperInterface4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment defaultPkg= fSourceFolder.createPackageFragment("", false, null);
		defaultPkg.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String test= "" +
				"package test1;\n" +
				"\n" +
				"import test2.IRectangle;\n" +
				"\n" +
				"public sealed interface IShape permits IRectangle {\n" +
				"}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("IShape.java", test, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		test= "" +
				"package test2;\n" +
				"\n" +
				"public interface IRectangle {\n" +
				"}\n";
		pack2.createCompilationUnit("IRectangle.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= "" +
				"package test2;\n" +
				"\n" +
				"import test1.IShape;\n" +
				"\n" +
				"public interface IRectangle extends IShape {\n" +
				"}\n";
		assertEqualString(preview, expected);
	}

	@Test
	public void testAddSealedAsDirectSuperClass1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed class Shape permits Circle {\\n" +
				"\n" +
				"}\n" +
				"\n" +
				"class Circle extends AssertionError {\n" +
				"    \n" +
				"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Shape.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" +
				"package test;\n" +
				"\n" +
				"public sealed class Shape permits Circle {\\n" +
				"\n" +
				"}\n" +
				"\n" +
				"class Circle extends Shape {\n" +
				"    \n" +
				"}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testAddRecordAsSubType() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test=  "" +
				"package test;\n" +
				"\n" +
				"public sealed interface IShape permits Circle, Square {\\n" +
				"\n" +
				"}\n" +
				"\n" +
				"class Circle implements IShape {\n" +
				"    \n" +
				"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("IShape.java", test, false, null);
		String expectedProposal= Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createrecord_description, "Square");

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);
		assertProposalExists(proposals, expectedProposal);
	}

	@Test
	public void testDoNotAddRecordAsSubType() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed class Shape permits Circle, Square {\\n" +
				"\n" +
				"}\n" +
				"\n" +
				"class Circle extends Shape {\n" +
				"    \n" +
				"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Shape.java", test, false, null);
		String expectedProposal= Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createrecord_description, "Square");

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);
		assertProposalDoesNotExist(proposals, expectedProposal);
	}

	@Test
	public void testCreatePermittedNewSubType1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed class Shape {\n" +
				"\n" +
				"}\n" +
				"\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Shape.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		String expectedProposal1= CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewinnerclass_description;
		String expectedProposal2= Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewclass_inpackage_description, "test");
		assertProposalExists(proposals, expectedProposal1);
		assertProposalExists(proposals, expectedProposal2);
	}

	@Test
	public void testCreatePermittedNewSubType2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set17CompilerOptions(fJProject1, false);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
				"package test;\n" +
				"\n" +
				"public sealed interface IShape {\n" +
				"\n" +
				"}\n" +
				"\n";
		ICompilationUnit cu= pack1.createCompilationUnit("IShape.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);
		String expectedProposal1= CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewinnerclass_description;
		String expectedProposal2= Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewclass_inpackage_description, "test");
		String expectedProposal3= CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewinnerrecord_description;
		String expectedProposal4= Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewrecord_inpackage_description, "test");
		String expectedProposal5= CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewinnerinterface_description;
		String expectedProposal6= Messages.format(CorrectionMessages.NewCUCompletionUsingWizardProposal_createnewinterface_inpackage_description, "test");

		assertProposalExists(proposals, expectedProposal1);
		assertProposalExists(proposals, expectedProposal2);
		assertProposalExists(proposals, expectedProposal3);
		assertProposalExists(proposals, expectedProposal4);
		assertProposalExists(proposals, expectedProposal5);
		assertProposalExists(proposals, expectedProposal6);
	}

}
