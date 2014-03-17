/*******************************************************************************
 * Copyright (c) 2012, 2014 GK Software AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *     IBM Corporation - bug fixes
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.osgi.framework.Bundle;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class NullAnnotationsQuickFixTest extends QuickFixTest {

	private static final Class THIS= NullAnnotationsQuickFixTest.class;
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private String ANNOTATION_JAR_PATH;

	public NullAnnotationsQuickFixTest(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_HASHCODE_METHOD, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_NULL_SPECIFICATION_VIOLATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fJProject1= ProjectTestSetup.getProject();

		if (this.ANNOTATION_JAR_PATH == null) {
			String version= "[1.1.0,2.0.0)"; // tests run at 1.5, need the "old" null annotations
			Bundle[] bundles= Platform.getBundles("org.eclipse.jdt.annotation", version);
			File bundleFile= FileLocator.getBundleFile(bundles[0]);
			if (bundleFile.isDirectory())
				this.ANNOTATION_JAR_PATH= bundleFile.getPath() + "/bin";
			else
				this.ANNOTATION_JAR_PATH= bundleFile.getPath();
		}
		JavaProjectHelper.addLibrary(fJProject1, new Path(ANNOTATION_JAR_PATH));

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	// ==== Problem:	dereferencing a @Nullable field
	// ==== Fix:		extract field access to a fresh local variable and add a null-check

	// basic case
	public void testExtractNullableField1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(f.toUpperCase());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        final String f2 = f;\n");
		buf.append("        if (f2 != null) {\n");
		buf.append("            System.out.println(f2.toUpperCase());\n");
		buf.append("        } else {\n");
		buf.append("            // TODO handle null value\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// statement is not element of a block - need to create a new block - local name f2 already in use
	public void testExtractNullableField2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public void foo(boolean b) {\n");
		buf.append("        @SuppressWarnings(\"unused\") boolean f2 = false;\n");
		buf.append("        if (b)\n");
		buf.append("          System.out.println(f.toUpperCase());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public void foo(boolean b) {\n");
		buf.append("        @SuppressWarnings(\"unused\") boolean f2 = false;\n");
		buf.append("        if (b) {\n");
		buf.append("            final String f3 = f;\n");
		buf.append("            if (f3 != null) {\n");
		buf.append("                System.out.println(f3.toUpperCase());\n");
		buf.append("            } else {\n");
		buf.append("                // TODO handle null value\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// field name is part of a qualified field reference - inside a return statement (type: int)
	public void testExtractNullableField3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable E other;\n");
		buf.append("    int f;\n");
		buf.append("    public int foo(E that) {\n");
		buf.append("        return that.other.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable E other;\n");
		buf.append("    int f;\n");
		buf.append("    public int foo(E that) {\n");
		buf.append("        final E other2 = that.other;\n");
		buf.append("        if (other2 != null) {\n");
		buf.append("            return other2.f;\n");
		buf.append("        } else {\n");
		buf.append("            // TODO handle null value\n");
		buf.append("            return 0;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// field name is part of a this-qualified field reference - inside a return statement (type: String)
	public void testExtractNullableField4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable E other;\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public String foo() {\n");
		buf.append("        return this.other.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable E other;\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public String foo() {\n");
		buf.append("        final E other2 = this.other;\n");
		buf.append("        if (other2 != null) {\n");
		buf.append("            return other2.f;\n");
		buf.append("        } else {\n");
		buf.append("            // TODO handle null value\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// field referenced inside the rhs of an assignment-as-expression
	public void testExtractNullableField5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable E other;\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        String lo;\n");
		buf.append("        if ((lo = this.other.f) != null)\n");
		buf.append("            System.out.println(lo);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable E other;\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        String lo;\n");
		buf.append("        final E other2 = this.other;\n");
		buf.append("        if (other2 != null) {\n");
		buf.append("            if ((lo = other2.f) != null)\n");
		buf.append("                System.out.println(lo);\n");
		buf.append("        } else {\n");
		buf.append("            // TODO handle null value\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// reference to field of array type - dereferenced by f[0] and f.length
	public void testExtractNullableField6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String[] f1;\n");
		buf.append("    @Nullable String[] f2;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(f1[0]);\n");
		buf.append("        System.out.println(f2.length);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 2, 0); // get correction for first of two problems
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String[] f1;\n");
		buf.append("    @Nullable String[] f2;\n");
		buf.append("    public void foo() {\n");
		buf.append("        final String[] f12 = f1;\n");
		buf.append("        if (f12 != null) {\n");
		buf.append("            System.out.println(f12[0]);\n");
		buf.append("        } else {\n");
		buf.append("            // TODO handle null value\n");
		buf.append("        }\n");
		buf.append("        System.out.println(f2.length);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposals= collectCorrections(cu, astRoot, 2, 1); // get correction for second of two problems
		assertNumberOfProposals(proposals, 1);
		proposal= (CUCorrectionProposal) proposals.get(0);
		preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String[] f1;\n");
		buf.append("    @Nullable String[] f2;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(f1[0]);\n");
		buf.append("        final String[] f22 = f2;\n");
		buf.append("        if (f22 != null) {\n");
		buf.append("            System.out.println(f22.length);\n");
		buf.append("        } else {\n");
		buf.append("            // TODO handle null value\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// field has a generic type
	public void testExtractNullableField7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable List<String> f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(f.size());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable List<String> f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        final List<String> f2 = f;\n");
		buf.append("        if (f2 != null) {\n");
		buf.append("            System.out.println(f2.size());\n");
		buf.append("        } else {\n");
		buf.append("            // TODO handle null value\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// occurrences inside a class initializer
	public void testExtractNullableField8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable Exception e;\n");
		buf.append("    {\n");
		buf.append("        e.printStackTrace();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable Exception e;\n");
		buf.append("    {\n");
		buf.append("        final Exception e2 = e;\n");
		buf.append("        if (e2 != null) {\n");
		buf.append("            e2.printStackTrace();\n");
		buf.append("        } else {\n");
		buf.append("            // TODO handle null value\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// field reference inside a local variable initialization - ensure correct scoping of this local
	public void testExtractNullableField9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public String foo() {\n");
		buf.append("        String upper = f.toUpperCase();\n");
		buf.append("        System.out.println(upper);\n");
		buf.append("        return upper;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public String foo() {\n");
		buf.append("        final String f2 = f;\n");
		buf.append("        if (f2 != null) {\n");
		buf.append("            String upper = f2.toUpperCase();\n");
		buf.append("            System.out.println(upper);\n");
		buf.append("            return upper;\n");
		buf.append("        } else {\n");
		buf.append("            // TODO handle null value\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// ==== Problem:	using a @Nullable or un-annotated field in assignment/return context expecting @NonNull
	// ==== Fix:		extract field access to a fresh local variable and add a null-check

	// return situation, field reference is this.f
	public void testExtractPotentiallyNullField1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public @NonNull String foo() {\n");
		buf.append("        return this.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		// primary proposal: Extract to checked local variable
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public @NonNull String foo() {\n");
		buf.append("        final String f2 = this.f;\n");
		buf.append("        if (f2 != null) {\n");
		buf.append("            return f2;\n");
		buf.append("        } else {\n");
		buf.append("            // TODO handle null value\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
		
		// secondary proposal: Change return type of 'foo(..)' to '@Nullable'
		proposal= (CUCorrectionProposal) proposals.get(1);
		preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public @Nullable String foo() {\n");
		buf.append("        return this.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// message send argument situation, field reference is local.f
	public void testExtractPotentiallyNullField2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        E local = this;\n");
		buf.append("        bar(local.f);\n");
		buf.append("    }\n");
		buf.append("    public void bar(@NonNull String s) { }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable String f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        E local = this;\n");
		buf.append("        final String f2 = local.f;\n");
		buf.append("        if (f2 != null) {\n");
		buf.append("            bar(f2);\n");
		buf.append("        } else {\n");
		buf.append("            // TODO handle null value\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void bar(@NonNull String s) { }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}


	// @Nullable argument is used where @NonNull is required -> change to @NonNull
	public void testChangeParameter1a() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@Nullable Exception e1) {\n");
		buf.append("        @NonNull Exception e = new Exception();\n");
		buf.append("        e = e1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'e1' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@NonNull Exception e1) {\n");
		buf.append("        @NonNull Exception e = new Exception();\n");
		buf.append("        e = e1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// unspec'ed argument is used where @NonNull is required -> change to @NonNull
	public void testChangeParameter1b() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Exception e1) {\n");
		buf.append("        @NonNull Exception e = new Exception();\n");
		buf.append("        e = e1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2); // other is add @SW - TODO: check when this is offered
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'e1' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@NonNull Exception e1) {\n");
		buf.append("        @NonNull Exception e = new Exception();\n");
		buf.append("        e = e1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testChangeParameter1c() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNull Object foo(@Nullable Object o) {\n");
		buf.append("        return o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNull Object foo(@NonNull Object o) {\n");
		buf.append("        return o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testChangeParameter1d() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNull Object foo(Object o) {\n");
		buf.append("        return o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNull Object foo(@NonNull Object o) {\n");
		buf.append("        return o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// don't propose to change argument if mismatch is in an assignment to the argument
	public void testChangeParameter2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@NonNull Exception e1) {\n");
		buf.append("        e1 = null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 0);
	}

	// Attempt to override a @Nullable argument with a @NonNull argument
	// -> change to @Nullable
	// -> change overridden to @NonNull
	public void testChangeParameter3a() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@Nullable Exception e1) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E2 extends E {\n");
		buf.append("    void foo(@NonNull Exception e1) {\n");
		buf.append("        e1.printStackTrace();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'e1' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E2 extends E {\n");
		buf.append("    void foo(@Nullable Exception e1) {\n"); // change override to accept @Nullable
		buf.append("        e1.printStackTrace();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change parameter in overridden 'foo(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@NonNull Exception e1) {\n"); // change the overridden method to force @NonNull
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// Attempt to override a @Nullable argument with an unspec'ed argument
	// -> change to @Nullable
	public void testChangeParameter3b() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@Nullable Exception e1) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E {\n");
		buf.append("    void foo(Exception e1) {\n");
		buf.append("        e1.printStackTrace();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'e1' to '@Nullable'");
		
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("public class E2 extends E {\n");
		buf.append("    void foo(@Nullable Exception e1) {\n"); // change override to accept @Nullable
		buf.append("        e1.printStackTrace();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// Attempt to override a @NonNull argument with an unspec'ed argument
	// -> change to @NonNull
	public void testChangeParameter3c() throws Exception {
		// quickfix only offered with this warning enabled, but no need to say, because default is already "warning"
//		this.fJProject1.setOption(JavaCore.COMPILER_PB_NONNULL_PARAMETER_ANNOTATION_DROPPED, JavaCore.WARNING);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@NonNull Exception e1) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E {\n");
		buf.append("    void foo(Exception e1) {\n");
		buf.append("        e1.printStackTrace();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3); // one real change plus two @SuppressWarnings proposals
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'e1' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("\n");
		buf.append("public class E2 extends E {\n");
		buf.append("    void foo(@NonNull Exception e1) {\n"); // change override to keep @NonNull
		buf.append("        e1.printStackTrace();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// http://bugs.eclipse.org/400668 - [quick fix] The fix change parameter type to @Nonnull generated a null change
	// don't confuse changing arguments of current method and target method
	// -> split into two proposals
	public void testChangeParameter4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@NonNull Object o) {\n");
		buf.append("        // nop\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E2 {\n");
		buf.append("    void test(E e, @Nullable Object in) {\n");
		buf.append("        e.foo(in);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'in' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E2 {\n");
		buf.append("    void test(E e, @NonNull Object in) {\n");
		buf.append("        e.foo(in);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change parameter of 'foo(..)' to '@Nullable'");

		preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@Nullable Object o) {\n");
		buf.append("        // nop\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// http://bugs.eclipse.org/400668 - [quick fix] The fix change parameter type to @Nonnull generated a null change
	// don't confuse changing arguments of current method and target method
	// -> split into two proposals
	// variant with un-annotated parameter
	public void testChangeParameter4a() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@NonNull Object o) {\n");
		buf.append("        // nop\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    void test(E e, Object in) {\n");
		buf.append("        e.foo(in);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3); // third (uninteresting) is "add @SW"
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'in' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("\n");
		buf.append("public class E2 {\n");
		buf.append("    void test(E e, @NonNull Object in) {\n");
		buf.append("        e.foo(in);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change parameter of 'foo(..)' to '@Nullable'");

		preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@Nullable Object o) {\n");
		buf.append("        // nop\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// Bug 405086 - [quick fix] don't propose null annotations when those are disabled
	public void testChangeParameter5() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.DISABLED);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E {\n");
			buf.append("    void foo(Object o) {\n");
			buf.append("        if (o == null) return;\n");
			buf.append("        if (o != null) System.out.print(o.toString());\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
	
			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 1); // only "add @SW"
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);			
		}
	}

	// Bug 405086 - [quick fix] don't propose null annotations when those are disabled
	// don't propose a parameter change if there was no parameter annotation being the cause for the warning
	public void testChangeParameter6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object o) {\n");
		buf.append("        if (o == null) return;\n");
		buf.append("        if (o != null) System.out.print(o.toString());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1); // only "add @SW"
	}

	// Bug 405086 - [quick fix] don't propose null annotations when those are disabled
	// positive case (redundant check)
	public void testChangeParameter7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@NonNull Object o) {\n");
		buf.append("        if (o != null) System.out.print(o.toString());\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2); // ignore 2nd ("add @SW")
		
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@Nullable Object o) {\n");
		buf.append("        if (o != null) System.out.print(o.toString());\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// Bug 405086 - [quick fix] don't propose null annotations when those are disabled
	// positive case 2 (check always false)
	public void testChangeParameter8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@org.eclipse.jdt.annotation.NonNull Object o) {\n");
		buf.append("        if (o == null) System.out.print(\"NOK\");\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2); // ignore 2nd ("add @SW")
		
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@Nullable Object o) {\n");
		buf.append("        if (o == null) System.out.print(\"NOK\");\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// http://bugs.eclipse.org/395555 - [quickfix] Update null annotation quick fixes for bug 388281
	// conflict between inherited and default nullness
	public void testChangeParameter9() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.ENABLED);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import org.eclipse.jdt.annotation.*;\n");
			buf.append("public class E {\n");
			buf.append("    void foo(@Nullable Object o) {\n");
			buf.append("        // nop\n");
			buf.append("    }\n");
			buf.append("}\n");
			pack1.createCompilationUnit("E.java", buf.toString(), false, null);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import org.eclipse.jdt.annotation.*;\n");
			buf.append("@NonNullByDefault\n"); 
			buf.append("public class E2 extends E {\n");
			buf.append("    void foo(Object o) {\n");
			buf.append("        System.out.print(\"E2\");\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 2);

			CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

			assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@Nullable'");

			String preview= getPreviewContent(proposal);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import org.eclipse.jdt.annotation.*;\n");
			buf.append("@NonNullByDefault\n"); 
			buf.append("public class E2 extends E {\n");
			buf.append("    void foo(@Nullable Object o) {\n");
			buf.append("        System.out.print(\"E2\");\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());

			proposal= (CUCorrectionProposal)proposals.get(1);

			assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@NonNull'");

			preview= getPreviewContent(proposal);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import org.eclipse.jdt.annotation.*;\n");
			buf.append("@NonNullByDefault\n"); 
			buf.append("public class E2 extends E {\n");
			buf.append("    void foo(@NonNull Object o) {\n");
			buf.append("        System.out.print(\"E2\");\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.DISABLED);
		}
	}

	// returning @Nullable value from @NonNull method -> change to @Nullable return
	public void testChangeReturn1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNull Object foo() {\n");
		buf.append("        @Nullable Object o = null;\n");
		buf.append("        return o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable Object foo() {\n");
		buf.append("        @Nullable Object o = null;\n");
		buf.append("        return o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testChangeReturn2a() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNull Object foo() {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E2 extends E {\n");
		buf.append("    @Nullable Object foo() {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E2 extends E {\n");
		buf.append("    @NonNull Object foo() {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change return type of overridden 'foo(..)' to '@Nullable'");

		preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable Object foo() {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testChangeReturn2b() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNull Object foo() {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E {\n");
		buf.append("    Object foo() {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("\n");
		buf.append("public class E2 extends E {\n");
		buf.append("    @NonNull\n");
		buf.append("    Object foo() {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// https://bugs.eclipse.org/395555 - [quickfix] Update null annotation quick fixes for bug 388281
	// conflict between nullness inherited from different parents
	public void testChangeReturn3() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.ENABLED);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import org.eclipse.jdt.annotation.*;\n");
			buf.append("public class E {\n");
			buf.append("    @NonNull Object foo() {\n");
			buf.append("        // nop\n");
			buf.append("    }\n");
			buf.append("}\n");
			pack1.createCompilationUnit("E.java", buf.toString(), false, null);
			
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import org.eclipse.jdt.annotation.*;\n");
			buf.append("public interface IE {\n");
			buf.append("    @Nullable Object foo();\n");
			buf.append("}\n");
			pack1.createCompilationUnit("IE.java", buf.toString(), false, null);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E2 extends E implements IE {\n");
			buf.append("    public Object foo() {\n");
			buf.append("        return this;\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 2);

			CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

			assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@Nullable'");

			String preview= getPreviewContent(proposal);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
			buf.append("\n");
			buf.append("public class E2 extends E implements IE {\n");
			buf.append("    public @Nullable Object foo() {\n");
			buf.append("        return this;\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());

			proposal= (CUCorrectionProposal)proposals.get(1);

			assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@NonNull'");

			preview= getPreviewContent(proposal);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
			buf.append("\n");
			buf.append("public class E2 extends E implements IE {\n");
			buf.append("    public @NonNull Object foo() {\n");
			buf.append("        return this;\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.DISABLED);
		}
	}

	// https://bugs.eclipse.org/378724 - Null annotations are extremely hard to use in an existing project
	// see comment 12
	// remove @Nullable without adding redundant @NonNull (due to @NonNullByDefault)
	public void testChangeReturn4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    @Nullable Object bar() {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E2 {\n");
		buf.append("    @NonNull Object foo(E e) {\n");
		buf.append("        return e.bar();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E2 {\n");
		buf.append("    @Nullable Object foo(E e) {\n");
		buf.append("        return e.bar();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'bar(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    Object bar() {\n"); // here's the rub: don't add redundant @NonNull, just remove @Nullable
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// https://bugs.eclipse.org/378724 - Null annotations are extremely hard to use in an existing project
	// see comment 12
	// remove @Nullable without adding redundant @NonNull (due to @NonNullByDefault)
	// variant: package-level default
	public void testChangeReturn5() throws Exception {
		String suppressOptionalErrors= this.fJProject1.getOption(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, true);
		try {
			this.fJProject1.setOption(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, JavaCore.ENABLED);
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

			StringBuffer buf= new StringBuffer();
			buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
			buf.append("package test1;\n");
			pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import org.eclipse.jdt.annotation.*;\n");
			buf.append("public class E {\n");
			buf.append("    @Nullable Object bar() {\n");
			buf.append("        return new Object();\n");
			buf.append("    }\n");
			buf.append("}\n");
			pack1.createCompilationUnit("E.java", buf.toString(), false, null);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E2 {\n");
			buf.append("    public Object foo(E e) {\n"); // non-null by default
			buf.append("        return e.bar();\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 3); // includes "add @SW"

			CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

			assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@Nullable'");

			String preview= getPreviewContent(proposal);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
			buf.append("\n");
			buf.append("public class E2 {\n");
			buf.append("    public @Nullable Object foo(E e) {\n");
			buf.append("        return e.bar();\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());

			proposal= (CUCorrectionProposal)proposals.get(1);

			assertEqualString(proposal.getDisplayString(), "Change return type of 'bar(..)' to '@NonNull'");

			preview= getPreviewContent(proposal);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import org.eclipse.jdt.annotation.*;\n");
			buf.append("public class E {\n");
			buf.append("    Object bar() {\n"); // here's the rub: don't add redundant @NonNull, just remove @Nullable
			buf.append("        return new Object();\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		} finally {
			this.fJProject1.setOption(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, suppressOptionalErrors);
		}
	}

	// https://bugs.eclipse.org/378724 - Null annotations are extremely hard to use in an existing project
	// see comment 12
	// remove @Nullable without adding redundant @NonNull (due to @NonNullByDefault)
	// variant: cancelled default
	public void testChangeReturn6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuffer buf= new StringBuffer();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault(false)\n"); // <- HERE
		buf.append("public class E {\n");
		buf.append("    @Nullable Object bar() {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public Object foo(E e) {\n"); // non-null by default
		buf.append("        return e.bar();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("public class E2 {\n");
		buf.append("    public @Nullable Object foo(E e) {\n");
		buf.append("        return e.bar();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'bar(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault(false)\n");
		buf.append("public class E {\n");
		buf.append("    @NonNull Object bar() {\n"); // here's the rub: DO add redundant @NonNull, default is cancelled here
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testRemoveRedundantAnnotation1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    void foo(@NonNull Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testRemoveRedundantAnnotation2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    @NonNull\n");
		buf.append("    Object foo(Object o) {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    Object foo(Object o) {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testRemoveRedundantAnnotation3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    @NonNull\n");
		buf.append("    public Object foo(Object o) {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    public Object foo(Object o) {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testRemoveRedundantAnnotation4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    @NonNullByDefault\n");
		buf.append("    void foo(Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testRemoveRedundantAnnotation5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    @NonNullByDefault\n");
		buf.append("    class E1 {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    class E1 {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testRemoveRedundantAnnotation6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNullByDefault\n");
		buf.append("    void foo(Object o) {\n");
		buf.append("        @NonNullByDefault\n");
		buf.append("        class E1 {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNullByDefault\n");
		buf.append("    void foo(Object o) {\n");
		buf.append("        class E1 {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testRemoveRedundantAnnotation7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testRemoveRedundantAnnotation8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNullByDefault\n");
		buf.append("    void foo(Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object o) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
}
