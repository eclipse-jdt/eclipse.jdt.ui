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
		String str= """
			package pack;
			interface I {
			    public static void bar(int i) {}
			    public default void bar() {}
			}
			class Y implements I {
			    public static void main(String[] args) {
			        new Y().ba(0);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Y.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		assertProposalDoesNotExist(proposals, Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changemethod_description, "bar"));
	}

	@Test
	public void testCreateMethodQuickFix1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface Snippet {
			    public abstract String name();
			}
			class Ref {
			    void foo(Snippet c) {
			        int[] v= c.values();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String str1= """
			package test1;
			interface Snippet {
			    public abstract String name();
			
			    public abstract int[] values();
			}
			class Ref {
			    void foo(Snippet c) {
			        int[] v= c.values();
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

	@Test
	public void testCreateMethodQuickFix2() throws Exception {
		String str= """
			package test1;
			interface Snippet {
			    public abstract String name();
			}
			class Ref {
			    void foo(Snippet c) {
			        int[] v= Snippet.values();
			    }
			}
			""";
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String str1= """
			package test1;
			interface Snippet {
			    public abstract String name();
			
			    public static int[] values() {
			        return null;
			    }
			}
			class Ref {
			    void foo(Snippet c) {
			        int[] v= Snippet.values();
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
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

		String str= """
			package test1;
			public interface NestedInterfaceInInterface {
			    interface Interface {
			        default void foo() {
			            int[] a = values1();
			        }
			
			        int[] values1();
			    }
			}
			""";
		String str1= """
			package test1;
			public interface NestedInterfaceInInterface {
			    interface Interface {
			        default void foo() {
			            int[] a = values1();
			        }
			    }
			
			    static int[] values1() {
			        return null;
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal1), getPreviewContent(proposal2) }, new String[] { str, str1 });
	}

	@Test
	public void testCreateMethodQuickFix4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Arrays;
			public interface NestedInterfaceInInterface {
			    interface Interface {
			        public default void foo() {
			            Arrays.sort(this.values2());
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInInterface.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String str1= """
			package test1;
			import java.util.Arrays;
			public interface NestedInterfaceInInterface {
			    interface Interface {
			        public default void foo() {
			            Arrays.sort(this.values2());
			        }
			
			        public int[] values2();
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

	@Test
	public void testCreateMethodQuickFix5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface NestedInterfaceInInterface {
			    interface Interface {
			        public default void foo() {
			            Object o = Interface.getGlobal();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInInterface.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String str1= """
			package test1;
			public interface NestedInterfaceInInterface {
			    interface Interface {
			        public default void foo() {
			            Object o = Interface.getGlobal();
			        }
			
			        public static Object getGlobal() {
			            return null;
			        }
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
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

		String str= """
			package test1;
			public class NestedInterfaceInClass {
			    public static final int total= 10;
			    interface Interface {
			        public default void foo() {
			            int[] a = values1();
			        }
			
			        public int[] values1();
			    }
			}
			""";
		String str1= """
			package test1;
			public class NestedInterfaceInClass {
			    public static final int total= 10;
			    interface Interface {
			        public default void foo() {
			            int[] a = values1();
			        }
			    }
			    protected static int[] values1() {
			        return null;
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal1), getPreviewContent(proposal2) }, new String[] { str, str1 });
	}

	@Test
	public void testCreateMethodQuickFix7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class NestedInterfaceInClass {
			    int total= 10;
			    interface Interface {
			            int[] a = NestedInterfaceInClass.values1();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("NestedInterfaceInClass.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String str1= """
			package test1;
			public class NestedInterfaceInClass {
			    int total= 10;
			    interface Interface {
			            int[] a = NestedInterfaceInClass.values1();
			    }
			    static int[] values1() {
			        return null;
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

	@Test
	public void testCreateMethodIssue322_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static <T> void test(T a) {
			        test(a, a);      // error here
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class E {
			    public static <T> void test(T a) {
			        test(a, a);      // error here
			    }
			
			    private static <T> void test(T a, T a2) {
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodIssue322_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static <T, U> void test(T a, U b) {
			        test(a);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class E {
			    public static <T, U> void test(T a, U b) {
			        test(a);
			    }
			
			    private static <T> void test(T a) {
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodIssue322_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static <T extends U, U> void test(T a, U b) {
			        test(a);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class E {
			    public static <T extends U, U> void test(T a, U b) {
			        test(a);
			    }
			
			    private static <T extends U, U> void test(T a) {
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodIssue322_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static <T extends U, U> U test(T a, U b) {
			        return test(a);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class E {
			    public static <T extends U, U> U test(T a, U b) {
			        return test(a);
			    }
			
			    private static <U, T extends U> U test(T a) {
			        return null;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodIssue322_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static <T extends U, U> U test(T a) {
			        return test(a, a);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class E {
			    public static <T extends U, U> U test(T a) {
			        return test(a, a);
			    }
			
			    private static <U, T extends U> U test(T a, T a2) {
			        return null;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodIssue330_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private static class Class1<T> {
			        T t;
			        Class2<T> c2;
			       \s
			        T method() {
			            return c2.useT(t);
			        }
			    }
			
			    private static class Class2<U> {
			
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class E {
			    private static class Class1<T> {
			        T t;
			        Class2<T> c2;
			       \s
			        T method() {
			            return c2.useT(t);
			        }
			    }
			
			    private static class Class2<U> {
			
			        public U useT(U t) {
			            return null;
			        }
			
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodIssue330_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static <K> void test(K t) {
			        Class2<K> c2 = new Class2<>();
			        c2.useT(t);
			      \s
			    }
			
			    private static class Class2<U> {
			
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class E {
			    public static <K> void test(K t) {
			        Class2<K> c2 = new Class2<>();
			        c2.useT(t);
			      \s
			    }
			
			    private static class Class2<U> {
			
			        public void useT(U t) {
			        }
			
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodIssue330_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private static class Class1<T> {
			        <K> void test(T t) {
			            Class2<K> c2 = new Class2<>();
			            c2.useT(t);
			        }
			    }
			
			    private static class Class2<U> {
			
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class E {
			    private static class Class1<T> {
			        <K> void test(T t) {
			            Class2<K> c2 = new Class2<>();
			            c2.useT(t);
			        }
			    }
			
			    private static class Class2<U> {
			
			        public <T> void useT(T t) {
			        }
			
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testBug514213_avoidRedundantNonNullWhenCreatingMissingMethodForOverride() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			@org.eclipse.jdt.annotation.NonNullByDefault
			package test1;
			""";

		pack1.createCompilationUnit("package-info.java", str, false, null);

		String str1= """
			package test1;
			
			import java.util.Comparator;
			import java.util.List;
			import java.util.Map;
			
			import org.eclipse.jdt.annotation.NonNullByDefault;
			import org.eclipse.jdt.annotation.Nullable;
			
			@NonNullByDefault({})
			interface I1 {
			}
			
			interface I2 {
			}
			
			class X implements I1, I2 {
				@Override
				public Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] f(Number n1, @Nullable Number n2) {
					return null;
				}
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", str1, false, null);

		AssistContext context= getCorrectionContext(cu, str1.indexOf("f("), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		String str2= """
			package test1;
			
			import java.util.Comparator;
			import java.util.List;
			import java.util.Map;
			
			import org.eclipse.jdt.annotation.NonNull;
			import org.eclipse.jdt.annotation.NonNullByDefault;
			import org.eclipse.jdt.annotation.Nullable;
			
			@NonNullByDefault({})
			interface I1 {
			
			    Comparator<@NonNull List<? extends @NonNull Map<@Nullable Number, String @NonNull []>>> @Nullable [] f(
			            @NonNull Number n1, @Nullable Number n2);
			}
			
			interface I2 {
			}
			
			class X implements I1, I2 {
				@Override
				public Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] f(Number n1, @Nullable Number n2) {
					return null;
				}
			}""";
		assertProposalPreviewEquals(str2, "Create 'f()' in super type 'I1'", proposals);

		String str3= """
			package test1;
			
			import java.util.Comparator;
			import java.util.List;
			import java.util.Map;
			
			import org.eclipse.jdt.annotation.NonNullByDefault;
			import org.eclipse.jdt.annotation.Nullable;
			
			@NonNullByDefault({})
			interface I1 {
			}
			
			interface I2 {
			
			    Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] f(
			            Number n1, @Nullable Number n2);
			}
			
			class X implements I1, I2 {
				@Override
				public Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] f(Number n1, @Nullable Number n2) {
					return null;
				}
			}""";
		assertProposalPreviewEquals(str3, "Create 'f()' in super type 'I2'", proposals);
	}

	@Test
	public void testBug514213_avoidRedundantNonNullWhenCreatingMissingMethodForInvocation() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			@org.eclipse.jdt.annotation.NonNullByDefault
			package test1;
			""";

		pack1.createCompilationUnit("package-info.java", str, false, null);

		String str1= """
			package test1;
			
			import java.util.Comparator;
			import java.util.List;
			import java.util.Map;
			
			import org.eclipse.jdt.annotation.NonNullByDefault;
			import org.eclipse.jdt.annotation.Nullable;
			
			@NonNullByDefault({})
			interface I1 {
			}
			
			interface I2 {
			}
			
			class X {
				public boolean f(Number n1, @Nullable Number n2, I1 i1, I2 i2) {
					Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x1 = i1.g(n1, n2);
					Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x2 = i2.g(n1, n2);
					return x1 == x2;
				}
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(2, problems);
		List<IJavaCompletionProposal> proposals1= collectCorrections(cu, problems[0], null);
		List<IJavaCompletionProposal> proposals2= collectCorrections(cu, problems[1], null);
		String str2= """
			package test1;
			
			import java.util.Comparator;
			import java.util.List;
			import java.util.Map;
			
			import org.eclipse.jdt.annotation.NonNull;
			import org.eclipse.jdt.annotation.NonNullByDefault;
			import org.eclipse.jdt.annotation.Nullable;
			
			@NonNullByDefault({})
			interface I1 {
			
			    Comparator<@NonNull List<? extends @NonNull Map<@Nullable Number, String @NonNull []>>> @Nullable [] g(
			            @NonNull Number n1, @Nullable Number n2);
			}
			
			interface I2 {
			}
			
			class X {
				public boolean f(Number n1, @Nullable Number n2, I1 i1, I2 i2) {
					Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x1 = i1.g(n1, n2);
					Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x2 = i2.g(n1, n2);
					return x1 == x2;
				}
			}""";
		assertProposalPreviewEquals(str2, "Create method 'g(Number, Number)' in type 'I1'", proposals1);

		String str3= """
			package test1;
			
			import java.util.Comparator;
			import java.util.List;
			import java.util.Map;
			
			import org.eclipse.jdt.annotation.NonNullByDefault;
			import org.eclipse.jdt.annotation.Nullable;
			
			@NonNullByDefault({})
			interface I1 {
			}
			
			interface I2 {
			
			    Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] g(
			            Number n1, @Nullable Number n2);
			}
			
			class X {
				public boolean f(Number n1, @Nullable Number n2, I1 i1, I2 i2) {
					Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x1 = i1.g(n1, n2);
					Comparator<List<? extends Map<@Nullable Number, String[]>>> @Nullable [] x2 = i2.g(n1, n2);
					return x1 == x2;
				}
			}""";
		assertProposalPreviewEquals(str3, "Create method 'g(Number, Number)' in type 'I2'", proposals2);
	}
	@Test
	public void testBug528876() throws Exception {
		NullTestUtils.prepareNullTypeAnnotations(fSourceFolder);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
			String str= """
				package pack;
				import annots.*;
				@NonNullByDefault
				@interface Annot {
				}
				
				@NonNullByDefault
				@Annot(x = Bla.VALUE)
				public class Bla {
				    public static final String VALUE = "";
				}
				
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("Bla.java", str, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 1);

			String str1= """
				package pack;
				import annots.*;
				@NonNullByDefault
				@interface Annot {
				
				    String x();
				}
				
				@NonNullByDefault
				@Annot(x = Bla.VALUE)
				public class Bla {
				    public static final String VALUE = "";
				}
				
				""";
			assertProposalPreviewEquals(str1, "Create attribute 'x()'", proposals);
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
		String str= """
			package test1;
			interface I1 {
			    default int gogo() {
			        return 24;
			    }
			}
			
			interface I2 extends I1 {
			    int gogo();
			}
			
			public class XX implements I1, I2 {
			}
			""";

		ICompilationUnit cu= pack1.createCompilationUnit("XX.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String str1= """
			package test1;
			interface I1 {
			    default int gogo() {
			        return 24;
			    }
			}
			
			interface I2 extends I1 {
			    int gogo();
			}
			
			public class XX implements I1, I2 {
			
			    @Override
			    public int gogo() {
			        return 0;
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

}
