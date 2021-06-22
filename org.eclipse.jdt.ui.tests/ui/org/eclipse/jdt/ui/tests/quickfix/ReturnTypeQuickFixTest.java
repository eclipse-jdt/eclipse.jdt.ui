/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ReturnTypeQuickFixTest extends QuickFixTest {
	@Rule
    public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testReturnTypeMissingWithSimpleType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        return (new Vector()).elements();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean addReturnType= true, doRename= true;

		for (IJavaCompletionProposal elem : proposals) {
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= getPreviewContent(proposal);

				buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("import java.util.Enumeration;\n");
				buf.append("import java.util.Vector;\n");
				buf.append("public class E {\n");
				buf.append("    public Enumeration foo() {\n");
				buf.append("        return (new Vector()).elements();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("import java.util.Vector;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        return (new Vector()).elements();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}
	}


	@Test
	public void testReturnTypeMissingWithVoid() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        //do nothing\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean addReturnType= true, doRename= true;

		for (IJavaCompletionProposal elem : proposals) {
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= getPreviewContent(proposal);

				buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public void foo() {\n");
				buf.append("        //do nothing\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        //do nothing\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}

	}


	@Test
	public void testReturnTypeMissing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface E {\n");
		buf.append("    public foo();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface E {\n");
		buf.append("    public void foo();\n");
		buf.append("}\n");
		String expected1= buf.toString();


		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}


	@Test
	public void testReturnTypeMissingWithVoid2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("           return;\n");
		buf.append("        }\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean addReturnType= true, doRename= true;

		for (IJavaCompletionProposal elem : proposals) {
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= getPreviewContent(proposal);

				buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public void foo() {\n");
				buf.append("        if (true) {\n");
				buf.append("           return;\n");
				buf.append("        }\n");
				buf.append("        return;\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        if (true) {\n");
				buf.append("           return;\n");
				buf.append("        }\n");
				buf.append("        return;\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}
	}

	@Test
	public void testVoidMissingInAnonymousType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("            }\n");
		buf.append("            public foo() {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("            }\n");
		buf.append("            public void foo() {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}


	@Test
	public void testReturnTypeMissingWithNull() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean addReturnType= true, doRename= true;

		for (IJavaCompletionProposal elem : proposals) {
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= getPreviewContent(proposal);

				buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public Object foo() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}
	}

	@Test
	public void testReturnTypeMissingWithArrayType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        return new int[][] { { 1, 2 }, { 2, 3 } };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean addReturnType= true, doRename= true;

		for (IJavaCompletionProposal elem : proposals) {
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= getPreviewContent(proposal);

				buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public int[][] foo() {\n");
				buf.append("        return new int[][] { { 1, 2 }, { 2, 3 } };\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				buf= new StringBuilder();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        return new int[][] { { 1, 2 }, { 2, 3 } };\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}
	}

	@Test
	public void testVoidMethodReturnsStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    Vector fList= new Vector();\n");
		buf.append("    public void elements() {\n");
		buf.append("        return fList.toArray();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
			String preview= getPreviewContent(proposal);

			buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("public class E {\n");
			buf.append("    Vector fList= new Vector();\n");
			buf.append("    public Object[] elements() {\n");
			buf.append("        return fList.toArray();\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}
		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
			String preview= getPreviewContent(proposal);

			buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("public class E {\n");
			buf.append("    Vector fList= new Vector();\n");
			buf.append("    public void elements() {\n");
			buf.append("        return;\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}
	}

	@Test
	public void testVoidMethodReturnsAnonymClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void getOperation() {\n");
		buf.append("        return new Runnable() {\n");
		buf.append("            public void run() {}\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public Runnable getOperation() {\n");
		buf.append("        return new Runnable() {\n");
		buf.append("            public void run() {}\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void getOperation() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testReturnTypeMissingWithWildcardSuper() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public x(ArrayList<? super Number> b) {\n");
		buf.append("        return b.get(0);\n");
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
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public Object x(ArrayList<? super Number> b) {\n");
		buf.append("        return b.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public E(ArrayList<? super Number> b) {\n");
		buf.append("        return b.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testReturnTypeMissingWithWildcardExtends() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public x() {\n");
		buf.append("        ArrayList<? extends Number> b= null;\n");
		buf.append("        return b.get(0);\n");
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
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public Number x() {\n");
		buf.append("        ArrayList<? extends Number> b= null;\n");
		buf.append("        return b.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        ArrayList<? extends Number> b= null;\n");
		buf.append("        return b.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testConstructorReturnsValue() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        return System.getProperties();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
			String preview= getPreviewContent(proposal);

			buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.util.Properties;\n");
			buf.append("\n");
			buf.append("public class E {\n");
			buf.append("    public Properties E() {\n");
			buf.append("        return System.getProperties();\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}
		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
			String preview= getPreviewContent(proposal);

			buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("public class E {\n");
			buf.append("    public E() {\n");
			buf.append("        return;\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}
	}

	@Test
	public void testVoidMethodReturnsWildcardSuper() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public void getIt(ArrayList<? super Number> b) {\n");
		buf.append("        return b.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public Object getIt(ArrayList<? super Number> b) {\n");
		buf.append("        return b.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public void getIt(ArrayList<? super Number> b) {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testVoidMethodReturnsWildcardExtends() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public void getIt(ArrayList<? extends Number> b) {\n");
		buf.append("        return b.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public Number getIt(ArrayList<? extends Number> b) {\n");
		buf.append("        return b.get(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class E {\n");
		buf.append("    public void getIt(ArrayList<? extends Number> b) {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	@Test
	public void testCorrectReturnStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public Runnable getOperation() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public Runnable getOperation() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void getOperation() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testCorrectReturnStatementInConditional() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String input= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    <K1, K2, V> V foo(K1 key1, Map<K1, Map<K2, V>> map) {\n" //
				+ "        Map<K2, V> map2 = map.get(key1);\n" //
				+ "        return map2 == null ? null : map2.entrySet();\n" //
				+ "    }\n" //
				+ "}\n"; //
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", input, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    <K1, K2, V> V foo(K1 key1, Map<K1, Map<K2, V>> map) {\n" //
				+ "        Map<K2, V> map2 = map.get(key1);\n" //
				+ "        return (V) (map2 == null ? null : map2.entrySet());\n" //
				+ "    }\n" //
				+ "}\n"; //

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Map.Entry;\n" //
				+ "import java.util.Set;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    <K1, K2, V> Set<Entry<K2, V>> foo(K1 key1, Map<K1, Map<K2, V>> map) {\n" //
				+ "        Map<K2, V> map2 = map.get(key1);\n" //
				+ "        return map2 == null ? null : map2.entrySet();\n" //
				+ "    }\n" //
				+ "}\n"; //

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testCorrectReturnStatementInChainedConditional() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String input= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    <K1, K2, V> V foo(K1 key1, Map<K1, Map<K2, V>> aMap) {\n" //
				+ "        Map<K2, V> newMap = aMap.get(key1);\n" //
				+ "        return newMap == null ? null : aMap == null ? null : newMap.entrySet();\n" //
				+ "    }\n" //
				+ "}\n"; //
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", input, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    <K1, K2, V> V foo(K1 key1, Map<K1, Map<K2, V>> aMap) {\n" //
				+ "        Map<K2, V> newMap = aMap.get(key1);\n" //
				+ "        return (V) (newMap == null ? null : aMap == null ? null : newMap.entrySet());\n" //
				+ "    }\n" //
				+ "}\n"; //

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Map;\n" //
				+ "import java.util.Map.Entry;\n" //
				+ "import java.util.Set;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    <K1, K2, V> Set<Entry<K2, V>> foo(K1 key1, Map<K1, Map<K2, V>> aMap) {\n" //
				+ "        Map<K2, V> newMap = aMap.get(key1);\n" //
				+ "        return newMap == null ? null : aMap == null ? null : newMap.entrySet();\n" //
				+ "    }\n" //
				+ "}\n"; //

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testReturnVoid() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * Does it.\n");
		buf.append("     */\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    public int goo() {\n");
		buf.append("        return foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * Does it.\n");
		buf.append("     * @return \n");
		buf.append("     */\n");
		buf.append("    public int foo() {\n");
		buf.append("    }\n");
		buf.append("    public int goo() {\n");
		buf.append("        return foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testCorrectReturnStatementForArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void getArray() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMethodWithConstructorName() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] E() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	@Test
	public void testMissingReturnStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void getArray() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMissingReturnStatementWithCode() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int[] fArray;\n");
		buf.append("    public int getArray()[] {\n");
		buf.append("        if (true) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int[] fArray;\n");
		buf.append("    public int getArray()[] {\n");
		buf.append("        if (true) {\n");
		buf.append("        }\n");
		buf.append("        return fArray;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int[] fArray;\n");
		buf.append("    public void getArray()[] {\n");
		buf.append("        if (true) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMissingReturnStatementWithCode2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean isVisible() {\n");
		buf.append("        boolean res= false;\n");
		buf.append("        if (true) {\n");
		buf.append("            res= false;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean isVisible() {\n");
		buf.append("        boolean res= false;\n");
		buf.append("        if (true) {\n");
		buf.append("            res= false;\n");
		buf.append("        }\n");
		buf.append("        return res;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void isVisible() {\n");
		buf.append("        boolean res= false;\n");
		buf.append("        if (true) {\n");
		buf.append("            res= false;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMissingReturnStatementWithCode3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public String getDebugInfo() {\n");
		buf.append("        getClass().getName();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public String getDebugInfo() {\n");
		buf.append("        return getClass().getName();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();


		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testMissingReturnStatementWithCode4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public String getDebugInfo() {\n");
		buf.append("        getClass().notify();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public String getDebugInfo() {\n");
		buf.append("        getClass().notify();\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();


		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testMethodInEnum_bug239887() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("enum Bug {X;\n");
		buf.append("        wrap(){}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Bug.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("enum Bug {X;\n");
		buf.append("        void wrap(){}\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("enum Bug {X;\n");
		buf.append("        Bug(){}\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}
}
