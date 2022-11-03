/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.NullTestUtils;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.IPreferenceStore;

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
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class UnresolvedMethodsQuickFixTest1d8 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT, JavaCore.IGNORE);
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		options.put(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "fg");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testStaticInterfaceMethodNotInherited() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testCreateMethodQuickFix1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

		buf= new StringBuilder();
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

	@Test
	public void testCreateMethodQuickFix2() throws Exception {
		StringBuilder buf;
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuilder();
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

		buf= new StringBuilder();
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

	@Test
	public void testCreateMethodQuickFix3() throws Exception {
		StringBuilder buf= new StringBuilder();
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

		StringBuilder buf1= new StringBuilder();
		buf1.append("package test1;\n");
		buf1.append("public interface NestedInterfaceInInterface {\n");
		buf1.append("    interface Interface {\n");
		buf1.append("        default void foo() {\n");
		buf1.append("            int[] a = values1();\n");
		buf1.append("        }\n\n");
		buf1.append("        int[] values1();\n");
		buf1.append("    }\n");
		buf1.append("}\n");

		buf= new StringBuilder();
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

	@Test
	public void testCreateMethodQuickFix4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

		buf= new StringBuilder();
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

	@Test
	public void testCreateMethodQuickFix5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

		buf= new StringBuilder();
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

	@Test
	public void testCreateMethodQuickFix6() throws Exception {
		StringBuilder buf1= new StringBuilder();
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

		buf1= new StringBuilder();
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

		StringBuilder buf= new StringBuilder();
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

	@Test
	public void testCreateMethodQuickFix7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

		buf= new StringBuilder();
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

	@Test
	public void testCreateMethodIssue322_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static <T> void test(T a) {\n");
		buf.append("        test(a, a);      // error here\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static <T> void test(T a) {\n");
		buf.append("        test(a, a);      // error here\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static <T> void test(T a, T a2) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] { buf.toString() });
	}

	@Test
	public void testCreateMethodIssue322_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static <T, U> void test(T a, U b) {\n");
		buf.append("        test(a);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static <T, U> void test(T a, U b) {\n");
		buf.append("        test(a);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static <T> void test(T a) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] { buf.toString() });
	}

	@Test
	public void testCreateMethodIssue322_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static <T extends U, U> void test(T a, U b) {\n");
		buf.append("        test(a);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static <T extends U, U> void test(T a, U b) {\n");
		buf.append("        test(a);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static <T extends U, U> void test(T a) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] { buf.toString() });
	}

	@Test
	public void testCreateMethodIssue322_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static <T extends U, U> U test(T a, U b) {\n");
		buf.append("        return test(a);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static <T extends U, U> U test(T a, U b) {\n");
		buf.append("        return test(a);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static <U, T extends U> U test(T a) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] { buf.toString() });
	}

	@Test
	public void testCreateMethodIssue322_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static <T extends U, U> U test(T a) {\n");
		buf.append("        return test(a, a);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static <T extends U, U> U test(T a) {\n");
		buf.append("        return test(a, a);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private static <U, T extends U> U test(T a, T a2) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] { buf.toString() });
	}

	@Test
	public void testBug514213_avoidRedundantNonNullWhenCreatingMissingMethodForOverride() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf;

		buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X implements I1, I2 {\n");
		buf.append("	@Override\n");
		buf.append("	public Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] f(Number n1, @Nullable Number n2) {\n");
		buf.append("		return null;\n");
		buf.append("	}\n");
		buf.append("}");
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf("f("), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("\n");
		buf.append("    Comparator<@NonNull List<? extends @NonNull Map<@Nullable Number, String @NonNull []>>> @Nullable [] f(\n");
		buf.append("            @NonNull Number n1, @Nullable Number n2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X implements I1, I2 {\n");
		buf.append("	@Override\n");
		buf.append("	public Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] f(Number n1, @Nullable Number n2) {\n");
		buf.append("		return null;\n");
		buf.append("	}\n");
		buf.append("}");
		assertProposalPreviewEquals(buf.toString(), "Create 'f()' in super type 'I1'", proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("\n");
		buf.append("    Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] f(\n");
		buf.append("            Number n1, @Nullable Number n2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X implements I1, I2 {\n");
		buf.append("	@Override\n");
		buf.append("	public Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] f(Number n1, @Nullable Number n2) {\n");
		buf.append("		return null;\n");
		buf.append("	}\n");
		buf.append("}");
		assertProposalPreviewEquals(buf.toString(), "Create 'f()' in super type 'I2'", proposals);
	}

	@Test
	public void testBug514213_avoidRedundantNonNullWhenCreatingMissingMethodForInvocation() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf;

		buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	public boolean f(Number n1, @Nullable Number n2, I1 i1, I2 i2) {\n");
		buf.append("		Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x1 = i1.g(n1, n2);\n");
		buf.append("		Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x2 = i2.g(n1, n2);\n");
		buf.append("		return x1 == x2;\n");
		buf.append("	}\n");
		buf.append("}");
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(2, problems);
		List<IJavaCompletionProposal> proposals1= collectCorrections(cu, problems[0], null);
		List<IJavaCompletionProposal> proposals2= collectCorrections(cu, problems[1], null);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("\n");
		buf.append("    Comparator<@NonNull List<? extends @NonNull Map<@Nullable Number, String @NonNull []>>> @Nullable [] g(\n");
		buf.append("            @NonNull Number n1, @Nullable Number n2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	public boolean f(Number n1, @Nullable Number n2, I1 i1, I2 i2) {\n");
		buf.append("		Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x1 = i1.g(n1, n2);\n");
		buf.append("		Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x2 = i2.g(n1, n2);\n");
		buf.append("		return x1 == x2;\n");
		buf.append("	}\n");
		buf.append("}");
		assertProposalPreviewEquals(buf.toString(), "Create method 'g(Number, Number)' in type 'I1'", proposals1);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("\n");
		buf.append("    Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] g(\n");
		buf.append("            Number n1, @Nullable Number n2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	public boolean f(Number n1, @Nullable Number n2, I1 i1, I2 i2) {\n");
		buf.append("		Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x1 = i1.g(n1, n2);\n");
		buf.append("		Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x2 = i2.g(n1, n2);\n");
		buf.append("		return x1 == x2;\n");
		buf.append("	}\n");
		buf.append("}");
		assertProposalPreviewEquals(buf.toString(), "Create method 'g(Number, Number)' in type 'I2'", proposals2);
	}
	@Test
	public void testBug528876() throws Exception {
		NullTestUtils.prepareNullTypeAnnotations(fSourceFolder);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
			StringBuilder buf= new StringBuilder();
			buf.append("package pack;\n");
			buf.append("import annots.*;\n");
			buf.append("@NonNullByDefault\n");
			buf.append("@interface Annot {\n");
			buf.append("}\n");
			buf.append("\n");
			buf.append("@NonNullByDefault\n");
			buf.append("@Annot(x = Bla.VALUE)\n");
			buf.append("public class Bla {\n");
			buf.append("    public static final String VALUE = \"\";\n");
			buf.append("}\n");
			buf.append("\n");
			buf.append("");
			ICompilationUnit cu= pack1.createCompilationUnit("Bla.java", buf.toString(), false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 1);

			buf= new StringBuilder();
			buf.append("package pack;\n");
			buf.append("import annots.*;\n");
			buf.append("@NonNullByDefault\n");
			buf.append("@interface Annot {\n");
			buf.append("\n");
			buf.append("    String x();\n");
			buf.append("}\n");
			buf.append("\n");
			buf.append("@NonNullByDefault\n");
			buf.append("@Annot(x = Bla.VALUE)\n");
			buf.append("public class Bla {\n");
			buf.append("    public static final String VALUE = \"\";\n");
			buf.append("}\n");
			buf.append("\n");
			buf.append("");

			assertProposalPreviewEquals(buf.toString(), "Create attribute 'x()'", proposals);
		} finally {
			NullTestUtils.disableAnnotationBasedNullAnalysis(fSourceFolder);
		}
	}

	/*
	 * Test that a default method that is overridden with an abstract method is
	 * added when invoking "Add unimplemented methods"
	 */
	@Test
	public void testOverrideDefaultMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();

		buf.append("package test1;\n");
		buf.append("interface I1 {\n");
		buf.append("    default int gogo() {\n");
		buf.append("        return 24;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 extends I1 {\n");
		buf.append("    int gogo();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class XX implements I1, I2 {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("XX.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I1 {\n");
		buf.append("    default int gogo() {\n");
		buf.append("        return 24;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 extends I1 {\n");
		buf.append("    int gogo();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class XX implements I1, I2 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int gogo() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

}
