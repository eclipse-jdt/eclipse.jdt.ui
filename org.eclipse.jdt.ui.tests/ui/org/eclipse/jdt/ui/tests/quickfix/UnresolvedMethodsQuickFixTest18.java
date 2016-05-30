/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
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
import java.util.Hashtable;

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

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

import junit.framework.Test;
import junit.framework.TestSuite;

public class UnresolvedMethodsQuickFixTest18 extends QuickFixTest {
	private static final Class<UnresolvedMethodsQuickFixTest18> THIS= UnresolvedMethodsQuickFixTest18.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public UnresolvedMethodsQuickFixTest18(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT, JavaCore.IGNORE);
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		options.put(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "fg");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= Java18ProjectTestSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}

	public void testStaticInterfaceMethodNotInherited() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("interface I {\n");
		buf.append("    public static void bar(int i) {}\n");
		buf.append("    public default void bar() {}\n");
		buf.append("}\n");
		buf.append("class Y implements I {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        new Y().ba(0);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Y.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		assertProposalDoesNotExist(proposals, Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changemethod_description, "bar"));
	}

	public void testCreateMethodQuickFix1() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface Snippet {\n");
		buf.append("    public abstract String name();\n");
		buf.append("}\n");
		buf.append("class Ref {\n");
		buf.append("    void foo(Snippet c) {\n");
		buf.append("        int[] v= c.values();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface Snippet {\n");
		buf.append("    public abstract String name();\n");
		buf.append("\n");
		buf.append("    public abstract int[] values();\n");
		buf.append("}\n");
		buf.append("class Ref {\n");
		buf.append("    void foo(Snippet c) {\n");
		buf.append("        int[] v= c.values();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testCreateMethodQuickFix2() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface Snippet {\n");
		buf.append("    public abstract String name();\n");
		buf.append("}\n");
		buf.append("class Ref {\n");
		buf.append("    void foo(Snippet c) {\n");
		buf.append("        int[] v= Snippet.values();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface Snippet {\n");
		buf.append("    public abstract String name();\n");
		buf.append("\n");
		buf.append("    public static int[] values() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Ref {\n");
		buf.append("    void foo(Snippet c) {\n");
		buf.append("        int[] v= Snippet.values();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testCreateMethodQuickFix3() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf.append("package test1;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        default void foo() {\n");
		buf.append("            int[] a = values1();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInInterface.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal1= (CUCorrectionProposal) proposals.get(0);
		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);

		StringBuffer buf1= new StringBuffer();
		buf1.append("package test1;\n");
		buf1.append("public interface NestedInterfaceInInterface {\n");
		buf1.append("    interface Interface {\n");
		buf1.append("        default void foo() {\n");
		buf1.append("            int[] a = values1();\n");
		buf1.append("        }\n\n");
		buf1.append("        int[] values1();\n");
		buf1.append("    }\n");
		buf1.append("}\n");

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        default void foo() {\n");
		buf.append("            int[] a = values1();\n");
		buf.append("        }\n");
		buf.append("    }\n\n");
		buf.append("    static int[] values1() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal1), getPreviewContent(proposal2) }, new String[] { buf1.toString(), buf.toString() });
	}

	public void testCreateMethodQuickFix4() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        public default void foo() {\n");
		buf.append("            Arrays.sort(this.values2());\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInInterface.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        public default void foo() {\n");
		buf.append("            Arrays.sort(this.values2());\n");
		buf.append("        }\n\n");
		buf.append("        public int[] values2();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testCreateMethodQuickFix5() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        public default void foo() {\n");
		buf.append("            Object o = Interface.getGlobal();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInInterface.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface NestedInterfaceInInterface {\n");
		buf.append("    interface Interface {\n");
		buf.append("        public default void foo() {\n");
		buf.append("            Object o = Interface.getGlobal();\n");
		buf.append("        }\n\n");
		buf.append("        public static Object getGlobal() {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	public void testCreateMethodQuickFix6() throws Exception {
		StringBuffer buf1= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf1.append("package test1;\n");
		buf1.append("public class NestedInterfaceInClass {\n");
		buf1.append("    public static final int total= 10;\n");
		buf1.append("    interface Interface {\n");
		buf1.append("        public default void foo() {\n");
		buf1.append("            int[] a = values1();\n");
		buf1.append("        }\n");
		buf1.append("    }\n");
		buf1.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInClass.java", buf1.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal1= (CUCorrectionProposal) proposals.get(0);
		CUCorrectionProposal proposal2= (CUCorrectionProposal) proposals.get(1);

		buf1= new StringBuffer();
		buf1.append("package test1;\n");
		buf1.append("public class NestedInterfaceInClass {\n");
		buf1.append("    public static final int total= 10;\n");
		buf1.append("    interface Interface {\n");
		buf1.append("        public default void foo() {\n");
		buf1.append("            int[] a = values1();\n");
		buf1.append("        }\n");
		buf1.append("\n");
		buf1.append("        public int[] values1();\n");
		buf1.append("    }\n");
		buf1.append("}\n");

		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class NestedInterfaceInClass {\n");
		buf.append("    public static final int total= 10;\n");
		buf.append("    interface Interface {\n");
		buf.append("        public default void foo() {\n");
		buf.append("            int[] a = values1();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    protected static int[] values1() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal1), getPreviewContent(proposal2) }, new String[] { buf1.toString(), buf.toString() });
	}

	public void testCreateMethodQuickFix7() throws Exception {
		StringBuffer buf= new StringBuffer();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class NestedInterfaceInClass {\n");
		buf.append("    int total= 10;\n");
		buf.append("    interface Interface {\n");
		buf.append("            int[] a = NestedInterfaceInClass.values1();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInClass.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class NestedInterfaceInClass {\n");
		buf.append("    int total= 10;\n");
		buf.append("    interface Interface {\n");
		buf.append("            int[] a = NestedInterfaceInClass.values1();\n");
		buf.append("    }\n");
		buf.append("    static int[] values1() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

}
