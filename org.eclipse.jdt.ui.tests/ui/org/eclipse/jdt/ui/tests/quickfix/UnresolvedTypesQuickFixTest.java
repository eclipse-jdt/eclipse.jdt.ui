/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Rabea Gransberger <rgransberger@gmx.de> - [quick fix] Fix several visibility issues - https://bugs.eclipse.org/394692
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class UnresolvedTypesQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		String newFileTemplate= "${package_declaration}\n\n${type_declaration}";
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, newFileTemplate, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testTypeInFieldDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    Vector1 vec;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    Vector vec;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Vector1 {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface Vector1 {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected3= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public enum Vector1 {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected4= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<Vector1> {\n");
		buf.append("    Vector1 vec;\n");
		buf.append("}\n");
		String expected5= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5 });

	}

	@Test
	public void testTypeInMethodArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vect1or[] vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector[] vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Vect1or {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface Vect1or {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected3= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public enum Vect1or {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected4= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<Vect1or> {\n");
		buf.append("    void foo(Vect1or[] vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected5= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    <Vect1or> void foo(Vect1or[] vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected6= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5, expected6 });
	}

	@Test
	public void testTypeInMethodReturnType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    Vect1or[] foo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    Vector[] foo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Vect1or {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface Vect1or {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected3= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public enum Vect1or {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected4= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<Vect1or> {\n");
		buf.append("    Vect1or[] foo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected5= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    <Vect1or> Vect1or[] foo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected6= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5, expected6 });
	}

	@Test
	public void testTypeInExceptionType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() throws IOExcpetion {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo() throws IOException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class IOExcpetion extends Exception {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}


	@Test
	public void testTypeInVarDeclWithWildcard() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(ArrayList<? extends Runnable> a) {\n");
		buf.append("        XY v= a.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);


		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(ArrayList<? extends Runnable> a) {\n");
		buf.append("        Runnable v= a.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class XY {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface XY {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected3= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public enum XY {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected4= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    <XY> void foo(ArrayList<? extends Runnable> a) {\n");
		buf.append("        XY v= a.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected5= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E<XY> {\n");
		buf.append("    void foo(ArrayList<? extends Runnable> a) {\n");
		buf.append("        XY v= a.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected6= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5, expected6 });
	}

	@Test
	public void testTypeInStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        ArrayList v= new ArrayListist();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        ArrayList v= new ArrayList();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("\n");
		buf.append("public class ArrayListist extends ArrayList {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}


	@Test
	public void testArrayTypeInStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Serializable[] v= new ArrayListExtra[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Serializable[] v= new Serializable[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Serializable[] v= new ArrayList[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("\n");
		buf.append("public class ArrayListExtra implements Serializable {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected3= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("\n");
		buf.append("public interface ArrayListExtra extends Serializable {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected4= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("\n");
		buf.append("public enum ArrayListExtra implements Serializable {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected5= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class E<ArrayListExtra> {\n");
		buf.append("    void foo() {\n");
		buf.append("        Serializable[] v= new ArrayListExtra[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected6= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class E {\n");
		buf.append("    <ArrayListExtra> void foo() {\n");
		buf.append("        Serializable[] v= new ArrayListExtra[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected7= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5, expected6, expected7});
	}

	@Test
	public void testQualifiedType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        test2.Test t= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("public interface Test {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("public enum Test {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testInnerType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Object object= new F.Inner() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Object object= new Object() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("\n");
		buf.append("    public class Inner {\n");
		buf.append("\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("\n");
		buf.append("    public interface Inner {\n");
		buf.append("\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testTypeInCatchBlock() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("        } catch (XXX x) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class XXX extends Exception {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testTypeInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E extends XXX {\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class XXX {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testTypeInSuperInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface E extends XXX {\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface XXX {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testTypeInAnnotation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@Xyz\n");
		buf.append("public interface E {\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public @interface Xyz {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testTypeInAnnotation_bug153881() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("a", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package a;\n");
		buf.append("public class SomeClass {\n");
		buf.append("        @scratch.Unimportant void foo() {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("SomeClass.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package scratch;\n");
		buf.append("\n");
		buf.append("public @interface Unimportant {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testPrimitiveTypeInFieldDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    floot vec= 1.0;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    double vec= 1.0;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    Float vec= 1.0;\n");
		buf.append("}\n");
		String expected2= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    float vec= 1.0;\n");
		buf.append("}\n");
		String expected3= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class floot {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected4= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface floot {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected5= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public enum floot {\n");
		buf.append("\n");
		buf.append("}\n");
		String expected6= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<floot> {\n");
		buf.append("    floot vec= 1.0;\n");
		buf.append("}\n");
		String expected7= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5, expected6, expected7});
	}

	@Test
	public void testTypeInTypeArguments1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<T> {\n");
		buf.append("    class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<XYX> list= new E<SomeType>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String[] expected= new String[6];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<T> {\n");
		buf.append("    class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<SomeType> list= new E<SomeType>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class XYX {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface XYX {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[2]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public enum XYX {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[3]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<T> {\n");
		buf.append("    class SomeType { }\n");
		buf.append("    <XYX> void foo() {\n");
		buf.append("        E<XYX> list= new E<SomeType>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[4]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E<T, XYX> {\n");
		buf.append("    class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<XYX> list= new E<SomeType>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[5]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testTypeInTypeArguments2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T> {\n");
		buf.append("    static class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<Map<String, ? extends XYX>> list= new E<Map<String, ? extends SomeType>>() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String[] expected= new String[6];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T> {\n");
		buf.append("    static class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<Map<String, ? extends SomeType>> list= new E<Map<String, ? extends SomeType>>() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import test1.E.SomeType;\n");
		buf.append("\n");
		buf.append("public class XYX extends SomeType {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface XYX {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[2]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public enum XYX {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[3]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T> {\n");
		buf.append("    static class SomeType { }\n");
		buf.append("    <XYX> void foo() {\n");
		buf.append("        E<Map<String, ? extends XYX>> list= new E<Map<String, ? extends SomeType>>() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[4]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T, XYX> {\n");
		buf.append("    static class SomeType { }\n");
		buf.append("    void foo() {\n");
		buf.append("        E<Map<String, ? extends XYX>> list= new E<Map<String, ? extends SomeType>>() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[5]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testParameterizedType1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo(XXY<String> b) {\n");
		buf.append("        b.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class XXY<T> {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface XXY<T> {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testParameterizedType2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T> {\n");
		buf.append("    static class SomeType<S1, S2> { }\n");
		buf.append("    void foo() {\n");
		buf.append("        SomeType<String, String> list= new XXY<String, String>() { };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(2, problems);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		proposals.addAll(collectCorrections(cu, problems[1], null));

		assertCorrectLabels(proposals);

		String[] expected= new String[3];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class E<T> {\n");
		buf.append("    static class SomeType<S1, S2> { }\n");
		buf.append("    void foo() {\n");
		buf.append("        SomeType<String, String> list= new SomeType<String, String>() { };\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import test1.E.SomeType;\n");
		buf.append("\n");
		buf.append("public class XXY<T1, T2> extends SomeType<String, String> {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface XXY<T1, T2> {\n");
		buf.append("\n");
		buf.append("}\n");
		expected[2]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	private void createSomeAmbiguity(boolean ifc, boolean isException) throws Exception {

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test3", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("public "); buf.append(ifc ? "interface" : "class");
		buf.append(" A "); buf.append(isException ? "extends Exception " : ""); buf.append("{\n");
		buf.append("}\n");
		pack3.createCompilationUnit("A.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test3;\n");
		buf.append("public class B {\n");
		buf.append("}\n");
		pack3.createCompilationUnit("B.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("public "); buf.append(ifc ? "interface" : "class");
		buf.append(" A "); buf.append(isException ? "extends Exception " : ""); buf.append("{\n");
		buf.append("}\n");
		pack2.createCompilationUnit("A.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);
	}


	@Test
	public void testAmbiguousTypeInSuperClass() throws Exception {
		createSomeAmbiguity(false, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E extends A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E extends A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E extends A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInInterface() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E implements A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E implements A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E implements A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInField() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    A a;\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    A a;\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    A a;\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInArgument() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo(A a) {");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo(A a) {");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo(A a) {");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInReturnType() throws Exception {
		createSomeAmbiguity(false, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public A foo() {");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public A foo() {");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public A foo() {");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInExceptionType() throws Exception {
		createSomeAmbiguity(false, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() throws A {");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() throws A {");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() throws A {");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAmbiguousTypeInCatchBlock() throws Exception {
		createSomeAmbiguity(false, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() {");
		buf.append("        try {\n");
		buf.append("        } catch (A e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() {");
		buf.append("        try {\n");
		buf.append("        } catch (A e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");
		buf.append("    public void foo() {");
		buf.append("        try {\n");
		buf.append("        } catch (A e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	/**
	 * Offers to raise visibility of method instead of class.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=94755
	 *
	 * @throws Exception if anything goes wrong
	 * @since 3.9
	 */
	@Test
	public void testIndirectRefDefaultClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		StringBuilder buf= new StringBuilder();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class B {\n");
		buf.append("    public Object get(Object c) {\n");
		buf.append("    	return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    B b = new B();\n");
		buf.append("    public B getB() {\n");
		buf.append("    	return b;\n");
		buf.append("    }\n");
		buf.append("}\n");
		cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import test1.A;\n");
		buf.append("public class C {\n");
		buf.append("    public Object getSide(A a) {\n");
		buf.append("    	return a.getB().get(this);\n");
		buf.append("    }\n");
		buf.append("}\n");
		cu= pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class B {\n");
		buf.append("    public Object get(Object c) {\n");
		buf.append("    	return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testForEachMissingType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("import java.util.*;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(ArrayList<? extends HashSet<? super Integer>> list) {\n");
		buf.append("        for (element: list) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3, 1);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("import java.util.*;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(ArrayList<? extends HashSet<? super Integer>> list) {\n");
		buf.append("        for (HashSet<? super Integer> element: list) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}
	@Test
	public void testBug530193() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragmentRoot testSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src-tests", new Path[0], new Path[0], "bin-tests",
				new IClasspathAttribute[] { JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, "true") });

		IPackageFragment pack1= sourceFolder.createPackageFragment("pp", false, null);
		StringBuilder buf1= new StringBuilder();
		buf1.append("package pp;\n");
		buf1.append("public class C1 {\n");
		buf1.append("    Tests at=new Tests();\n");
		buf1.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("C1.java", buf1.toString(), false, null);

		IPackageFragment pack2= testSourceFolder.createPackageFragment("pt", false, null);
		StringBuilder buf2= new StringBuilder();
		buf2.append("package pt;\n");
		buf2.append("public class Tests {\n");
		buf2.append("}\n");
		pack2.createCompilationUnit("Tests.java", buf2.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot, 2, 1);
		assertFalse(proposals.stream().anyMatch(p -> "Import 'Tests' (pt)".equals(p.getDisplayString())));
	}

	@Test
	public void testBug321464() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("class E<T extends StaticInner> {\n");
		buf.append("    public static class StaticInner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, 0);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("class E<T extends pack.E.StaticInner> {\n");
		buf.append("    public static class StaticInner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testIssue485() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Date d1= new Date();\n");
		buf.append("        Date d2;\n");
		buf.append("        d2=new Date();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 4, 0);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("import java.util.Date;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Date d1= new Date();\n");
		buf.append("        Date d2;\n");
		buf.append("        d2=new Date();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testIssue649() throws Exception {
		IPackageFragment defaultPackage= fSourceFolder.createPackageFragment("", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("//Comment 1\n");
		buf.append("//Comment 2\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Date d1= new Date();\n");
		buf.append("        Date d2;\n");
		buf.append("        d2=new Date();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= defaultPackage.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 4, 0);

		assertCorrectLabels(proposals);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("//Comment 1\n");
		buf.append("//Comment 2\n");
		buf.append("\n");
		buf.append("import java.util.Date;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Date d1= new Date();\n");
		buf.append("        Date d2;\n");
		buf.append("        d2=new Date();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}
}
