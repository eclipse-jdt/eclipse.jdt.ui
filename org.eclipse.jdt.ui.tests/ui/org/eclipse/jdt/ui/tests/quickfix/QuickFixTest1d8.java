/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

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
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class QuickFixTest1d8 extends QuickFixTest {
	@Rule
	public ProjectTestSetup projectSetup= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_NO_SPLIT));


		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testUnimplementedMethods1() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    int getCount(Object[] o) throws IOException;\n");
		buf.append("    static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("    default int defaultMethod(Object[] o) throws IOException{return 20;}\n");
		buf.append("}\n");
		pack2.createCompilationUnit("Inter.java", buf.toString(), false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public abstract class E implements Inter{\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int getCount(Object[] o) throws IOException {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testUnimplementedMethods2() throws Exception {
		StringBuilder buf= new StringBuilder();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class MyString implements CharSequence{\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("MyString.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public abstract class MyString implements CharSequence{\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class MyString implements CharSequence{\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public char charAt(int arg0) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public int length() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public CharSequence subSequence(int arg0, int arg1) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}


	// bug 420116 : test for annotated varargs and return type
	@Test
	public void testUnimplementedMethods3() throws Exception {
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);


		StringBuilder buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("public interface List {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("List.java", buf.toString(), false, null);

		buf.append("package test2;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.List;\n\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public interface Inter {\n");
		buf.append("    public int foo(@NonNull String @Nullable... s) throws IOException;\n");
		buf.append("    public int bug(@NonNull String... s) throws IOException;\n");
		buf.append("    public @NonNull String bar(@NonNull String s, @Nullable List<String> l1, test2.@NonNull List l2);\n");
		buf.append("    public int boo(Object @NonNull [] @Nullable... o1);\n");
		buf.append("    static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("    default int defaultMethod(Object[] o) throws IOException{return 20;}\n");
		buf.append("}\n");
		pack2.createCompilationUnit("Inter.java", buf.toString(), false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 4);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public abstract class E implements Inter{\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.List;\n\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n\n");
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int foo(@NonNull String @Nullable... s) throws IOException {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public int bug(@NonNull String... s) throws IOException {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public @NonNull String bar(@NonNull String s, @Nullable List<String> l1,\n");
		buf.append("            test2.@NonNull List l2) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public int boo(Object @NonNull [] @Nullable... o1) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	// bug 420116 : test for annotated varargs and return type
	@Test
	public void testUnimplementedMethods4() throws Exception {
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);


		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public interface Inter {\n");
		buf.append("    public int foo(@NonNull String @Nullable... s) throws IOException;\n");
		buf.append("    public int bar(@NonNull String... s) throws IOException;\n");
		buf.append("    static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("    default int defaultMethod(Object[] o) throws IOException{return 20;}\n");
		buf.append("}\n");
		buf.append("class E implements Inter{\n");
		buf.append("}\n");

		ICompilationUnit cu= pack2.createCompilationUnit("Inter.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public interface Inter {\n");
		buf.append("    public int foo(@NonNull String @Nullable... s) throws IOException;\n");
		buf.append("    public int bar(@NonNull String... s) throws IOException;\n");
		buf.append("    static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("    default int defaultMethod(Object[] o) throws IOException{return 20;}\n");
		buf.append("}\n");
		buf.append("abstract class E implements Inter{\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public interface Inter {\n");
		buf.append("    public int foo(@NonNull String @Nullable... s) throws IOException;\n");
		buf.append("    public int bar(@NonNull String... s) throws IOException;\n");
		buf.append("    static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("    default int defaultMethod(Object[] o) throws IOException{return 20;}\n");
		buf.append("}\n");
		buf.append("class E implements Inter{\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int foo(@NonNull String @Nullable... s) throws IOException {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n\n");
		buf.append("    @Override\n");
		buf.append("    public int bar(@NonNull String... s) throws IOException {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	// bug 420116 : test for user defined annotation in varargs
	@Test
	public void testUnimplementedMethods5() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);


		StringBuilder buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("public interface List {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("List.java", buf.toString(), false, null);

		buf.append("package test2;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("@Target({ ElementType.TYPE_USE }) @interface N1 { }\n");
		buf.append("@Target({ ElementType.TYPE_USE }) @interface N2 { }\n");
		buf.append("@Target({ ElementType.TYPE_USE }) @interface N3 { }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public interface Inter {\n");
		buf.append("    int foo2U(@N1 String @N2 [] s1 @N3 [], @N1 String @N2 [] @N3 [] @N4 ... s2);\n");
		buf.append("}\n");
		pack2.createCompilationUnit("Inter.java", buf.toString(), false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("public abstract class E implements Inter{\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");
		buf.append("import test2.N1;\n");
		buf.append("import test2.N2;\n");
		buf.append("import test2.N3;\n");
		buf.append("import test2.N4;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public int foo2U(@N1 String @N3 [] @N2 [] s1,\n");
		buf.append("            @N1 String @N2 [] @N3 [] @N4... s2) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnimplementedMethods6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.math.BigInteger;\n");
		buf.append("interface IHasInt {\n");
		buf.append("    int getInt();\n");
		buf.append("}\n");
		buf.append("interface IHasIntAsBigInteger extends IHasInt {\n");
		buf.append("    default int getInt() {\n");
		buf.append("        return getIntAsBigInteger().intValue();\n");
		buf.append("    }\n");
		buf.append("    BigInteger getIntAsBigInteger();\n");
		buf.append("}\n");
		buf.append("class C implements IHasIntAsBigInteger {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("IHasInt.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.math.BigInteger;\n");
		buf.append("interface IHasInt {\n");
		buf.append("    int getInt();\n");
		buf.append("}\n");
		buf.append("interface IHasIntAsBigInteger extends IHasInt {\n");
		buf.append("    default int getInt() {\n");
		buf.append("        return getIntAsBigInteger().intValue();\n");
		buf.append("    }\n");
		buf.append("    BigInteger getIntAsBigInteger();\n");
		buf.append("}\n");
		buf.append("class C implements IHasIntAsBigInteger {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public BigInteger getIntAsBigInteger() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.math.BigInteger;\n");
		buf.append("interface IHasInt {\n");
		buf.append("    int getInt();\n");
		buf.append("}\n");
		buf.append("interface IHasIntAsBigInteger extends IHasInt {\n");
		buf.append("    default int getInt() {\n");
		buf.append("        return getIntAsBigInteger().intValue();\n");
		buf.append("    }\n");
		buf.append("    BigInteger getIntAsBigInteger();\n");
		buf.append("}\n");
		buf.append("abstract class C implements IHasIntAsBigInteger {\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodReference1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    void foo(Runnable r) {\n");
		buf.append("    }\n");
		buf.append("    void bar() {\n");
		buf.append("        foo(this::action);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		AssistContext context= new AssistContext(cu, buf.toString().indexOf("::"), 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("    void foo(Runnable r) {\n");
		buf.append("    }\n");
		buf.append("    void bar() {\n");
		buf.append("        foo(this::action);\n");
		buf.append("    }\n");
		buf.append("    private void action() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodReference2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("class E {\n");
		buf.append("    void foo(Comparator<String> c) {\n");
		buf.append("    }\n");
		buf.append("    void bar() {\n");
		buf.append("        foo(this::action);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		AssistContext context= new AssistContext(cu, buf.toString().indexOf("::"), 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Comparator;\n");
		buf.append("class E {\n");
		buf.append("    void foo(Comparator<String> c) {\n");
		buf.append("    }\n");
		buf.append("    void bar() {\n");
		buf.append("        foo(this::action);\n");
		buf.append("    }\n");
		buf.append("    private int action(String string1, String string2) {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodReference3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("class E {\n");
		buf.append("    void foo(Function<Float, String> f) {\n");
		buf.append("    }\n");
		buf.append("    void bar() {\n");
		buf.append("        foo(this::action);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		AssistContext context= new AssistContext(cu, buf.toString().indexOf("::"), 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("class E {\n");
		buf.append("    void foo(Function<Float, String> f) {\n");
		buf.append("    }\n");
		buf.append("    void bar() {\n");
		buf.append("        foo(this::action);\n");
		buf.append("    }\n");
		buf.append("    private String action(Float float1) {\n");
		buf.append("        return \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodReference4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface F<T>\n");
		buf.append("    T baz();\n");
		buf.append("}\n");
		buf.append("class E {\n");
		buf.append("    <T> void foo(F<T> f) {\n");
		buf.append("    }\n");
		buf.append("    void bar() {\n");
		buf.append("        foo(this::action);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		AssistContext context= new AssistContext(cu, buf.toString().indexOf("::"), 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface F<T>\n");
		buf.append("    T baz();\n");
		buf.append("}\n");
		buf.append("class E {\n");
		buf.append("    <T> void foo(F<T> f) {\n");
		buf.append("    }\n");
		buf.append("    void bar() {\n");
		buf.append("        foo(this::action);\n");
		buf.append("    }\n");
		buf.append("    private <T> T action() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodReference5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.BiFunction;\n");
		buf.append("class E {\n");
		buf.append("    <A extends Object, B extends Object> void foo(BiFunction<A, B, Float> bf) {\n");
		buf.append("    }\n");
		buf.append("    void bar() {\n");
		buf.append("        foo(this::action);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		AssistContext context= new AssistContext(cu, buf.toString().indexOf("::"), 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.function.BiFunction;\n");
		buf.append("class E {\n");
		buf.append("    <A extends Object, B extends Object> void foo(BiFunction<A, B, Float> bf) {\n");
		buf.append("    }\n");
		buf.append("    void bar() {\n");
		buf.append("        foo(this::action);\n");
		buf.append("    }\n");
		buf.append("    private <A extends Object, B extends Object> Float action(A a1, B b2) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testLambdaReturnType1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    int fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("        };\n");
		buf.append("        return 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    int fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("        return 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	@Test
	public void testLambdaReturnType2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		buf.append("        I i= (int x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	@Test
	public void testLambdaReturnType3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		// Inferred parameter type
		buf.append("        I i= (x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("        };\n");
		buf.append("        i.foo(10);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    void fun2() {\n");
		buf.append("        I i= (x) -> {\n");
		buf.append("            x++;\n");
		buf.append("            System.out.println(x);\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("        i.foo(10);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	@Test
	public void testLambdaReturnType4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    I i2= (int x) -> {\n");
		buf.append("        x++;\n");
		buf.append("        System.out.println(x);\n");
		buf.append("    };\n");
		buf.append("    \n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface I {\n");
		buf.append("     int foo(int x);    \n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class A {    \n");
		buf.append("    I i2= (int x) -> {\n");
		buf.append("        x++;\n");
		buf.append("        System.out.println(x);\n");
		buf.append("        return x;\n");
		buf.append("    };\n");
		buf.append("    \n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	// bug 424172
	@Test
	public void testImportTypeInMethodReference() throws Exception {
		StringBuilder buf= new StringBuilder();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test.one", false, null);
		buf= new StringBuilder();
		buf.append("package test.one;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("    public static F2 staticMethod() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);
		buf= new StringBuilder();
		buf.append("package test.one;\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("public interface F2 {\n");
		buf.append("    void foo();\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test.two", false, null);
		buf= new StringBuilder();
		buf.append("package test.two;\n");
		buf.append("\n");
		buf.append("import test.one.F2;\n");
		buf.append("\n");
		buf.append("public class C1 {\n");
		buf.append("    public void fun1() {\n");
		buf.append("        F2 f = X::staticMethod;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack2.createCompilationUnit("C1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuilder();
		buf.append("package test.two;\n");
		buf.append("\n");
		buf.append("import test.one.F2;\n");
		buf.append("import test.one.X;\n");
		buf.append("\n");
		buf.append("public class C1 {\n");
		buf.append("    public void fun1() {\n");
		buf.append("        F2 f = X::staticMethod;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	@Test
	public void testLambdaReturnType5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.math.BigInteger;\n");
		buf.append("\n");
		buf.append("interface A { Object m(Class c); }\n");
		buf.append("interface B<S extends Number> { Object m(Class<S> c); }\n");
		buf.append("interface C<T extends BigInteger> { Object m(Class<T> c); }\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface D<S,T> extends A, B<BigInteger>, C<BigInteger> {}\n");
		buf.append("\n");
		buf.append("class E {\n");
		buf.append("    private void foo() {\n");
		buf.append("         D<BigInteger,BigInteger> d1= (x) -> {\n");
		buf.append("            };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.math.BigInteger;\n");
		buf.append("\n");
		buf.append("interface A { Object m(Class c); }\n");
		buf.append("interface B<S extends Number> { Object m(Class<S> c); }\n");
		buf.append("interface C<T extends BigInteger> { Object m(Class<T> c); }\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface D<S,T> extends A, B<BigInteger>, C<BigInteger> {}\n");
		buf.append("\n");
		buf.append("class E {\n");
		buf.append("    private void foo() {\n");
		buf.append("         D<BigInteger,BigInteger> d1= (x) -> {\n");
		buf.append("            return x;\n");
		buf.append("            };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	@Test
	public void testChangeModifierToStatic1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 4, 0);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    static int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });

		proposals= collectCorrections(cu, astRoot, 4, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    static int bar1() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });

		proposals= collectCorrections(cu, astRoot, 4, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    static void bar2() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    int fun1();\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });

		proposals= collectCorrections(cu, astRoot, 4, 3);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface A {\n");
		buf.append("    int i = foo();\n");
		buf.append("    default int foo() {\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    int j = bar1();\n");
		buf.append("    abstract int bar1();\n");
		buf.append("    static void temp() {\n");
		buf.append("        bar2();\n");
		buf.append("    }\n");
		buf.append("    abstract void bar2();\n");
		buf.append("    \n");
		buf.append("    int k = fun1();\n");
		buf.append("    static int fun1() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	@Test
	public void testChangeModifierToStatic2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface Test {\n");
		buf.append("    int i= foo();\n");
		buf.append("    abstract int foo() {\n");
		buf.append("        return 100;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, 0);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface Test {\n");
		buf.append("    int i= foo();\n");
		buf.append("    static int foo() {\n");
		buf.append("        return 100;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	// bug 410170
	@Test
	public void testInvalidInterfaceMethodModifier1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    private static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Inter.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    static int staticMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	//  bug 410170
	@Test
	public void testInvalidInterfaceMethodModifier2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    protected default int defaultMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Inter.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    default int defaultMethod(Object[] o) throws IOException{return 10;}\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	// bug 414084
	@Test
	public void testAbstractInterfaceMethodWithBody1() throws Exception {
		StringBuilder buf= new StringBuilder();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public char m1(int arg0);\n\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public static char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public default char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}

	// bug 414084
	@Test
	public void testAbstractInterfaceMethodWithBody2() throws Exception {
		StringBuilder buf= new StringBuilder();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public abstract char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public abstract char m1(int arg0);\n\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public static char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface Snippet{\n");
		buf.append("\n");
		buf.append("    public default char m1(int arg0) {\n");
		buf.append("    }\n\n");
		buf.append("}\n");
		String expected3= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	// bug 434173
	@Test
	public void testAbstractInterfaceMethodWithBody3() throws Exception {
		StringBuilder buf= new StringBuilder();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    public strictfp native void foo() {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<ICompletionProposal> proposals= collectAllCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(3);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    public void foo() {}\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });

		proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("interface I {\n");
		buf.append("    public void foo();\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	// bug 424616
	@Test
	public void testInferredExceptionType() throws Exception {
		StringBuilder buf= new StringBuilder();
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class AddThrows {\n");
		buf.append("  interface Getter2<T, E extends Exception> { T get() throws E; }\n");
		buf.append("  \n");
		buf.append("  public static Long main2(Getter2<Long, ?> getter) {\n");
		buf.append("    Long value = getter == null ? 0l : 1l;\n");
		buf.append("    value = getter.get();\n");
		buf.append("    return value;\n");
		buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("AddThrows.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class AddThrows {\n");
		buf.append("  interface Getter2<T, E extends Exception> { T get() throws E; }\n");
		buf.append("  \n");
		buf.append("  public static Long main2(Getter2<Long, ?> getter) {\n");
		buf.append("    Long value = getter == null ? 0l : 1l;\n");
		buf.append("    try {\n");
		buf.append("        value = getter.get();\n");
		buf.append("    } catch (Exception e) {\n");
		buf.append("        // TODO Auto-generated catch block\n");
		buf.append("        e.printStackTrace();\n");
		buf.append("    }\n");
		buf.append("    return value;\n");
		buf.append("  }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });

		proposal= (CUCorrectionProposal)proposals.get(1);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class AddThrows {\n");
		buf.append("  interface Getter2<T, E extends Exception> { T get() throws E; }\n");
		buf.append("  \n");
		buf.append("  public static Long main2(Getter2<Long, ?> getter) throws Exception {\n");
		buf.append("    Long value = getter == null ? 0l : 1l;\n");
		buf.append("    value = getter.get();\n");
		buf.append("    return value;\n");
		buf.append("  }\n");
		buf.append("}\n");
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { buf.toString() });
	}

	@Test
	public void testAddNonNull1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_NULL_SPECIFICATION_VIOLATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		JavaCore.setOptions(options);

		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public <T extends Number> double foo(T t) {\n");
		buf.append("        Number n=t;\n");
		buf.append("        return n.doubleValue();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public <T extends Number> double foo(T t) {\n");
		buf.append("        @NonNull Number n=t;\n");
		buf.append("        return n.doubleValue();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	@Test
	public void testAddNonNull2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_NULL_SPECIFICATION_VIOLATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE, DefaultCodeFormatterConstants.createAlignmentValue(true, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE));
		JavaCore.setOptions(options);

		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public <T extends Number> double foo(T t) {\n");
		buf.append("        Number n=t;\n");
		buf.append("        return n.doubleValue();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public <T extends Number> double foo(T t) {\n");
		buf.append("        @NonNull\n");
		buf.append("        Number n=t;\n");
		buf.append("        return n.doubleValue();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	// remove redundant NonNullByDefault _caused_ by NonNullByDefault on field
	@Test
	public void testRemoveRedundantNonNullByDefault1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_NULL_SPECIFICATION_VIOLATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE, DefaultCodeFormatterConstants.createAlignmentValue(true, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE));
		JavaCore.setOptions(options);

		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNullByDefault({DefaultLocation.PARAMETER})\n");
		buf.append("    Runnable f=new Runnable() {\n");
		buf.append("      @Override\n");
		buf.append("      @NonNullByDefault({DefaultLocation.PARAMETER})\n");
		buf.append("      public void run() {\n");
		buf.append("      }\n");
		buf.append("    };\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("    @NonNullByDefault({DefaultLocation.PARAMETER})\n");
		buf.append("    Runnable f=new Runnable() {\n");
		buf.append("      @Override\n");
		buf.append("      public void run() {\n");
		buf.append("      }\n");
		buf.append("    };\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	// remove redundant NonNullByDefault _caused_ by NonNullByDefault on local
	@Test
	public void testRemoveRedundantNonNullByDefault2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_NULL_SPECIFICATION_VIOLATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE, DefaultCodeFormatterConstants.createAlignmentValue(true, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE));
		JavaCore.setOptions(options);

		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("  public void f() {\n");
		buf.append("    @NonNullByDefault({DefaultLocation.PARAMETER})\n");
		buf.append("    Runnable local=new Runnable() {\n");
		buf.append("      @Override\n");
		buf.append("      @NonNullByDefault({DefaultLocation.PARAMETER})\n");
		buf.append("      public void run() {\n");
		buf.append("      }\n");
		buf.append("    };\n");
		buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("  public void f() {\n");
		buf.append("    @NonNullByDefault({DefaultLocation.PARAMETER})\n");
		buf.append("    Runnable local=new Runnable() {\n");
		buf.append("      @Override\n");
		buf.append("      public void run() {\n");
		buf.append("      }\n");
		buf.append("    };\n");
		buf.append("  }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	// remove redundant NonNullByDefault on field
	@Test
	public void testRemoveRedundantNonNullByDefault3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_NULL_SPECIFICATION_VIOLATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE, DefaultCodeFormatterConstants.createAlignmentValue(true, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE));
		JavaCore.setOptions(options);

		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    @NonNullByDefault\n");
		buf.append("    Object f=new Object();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    Object f=new Object();\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	// remove redundant NonNullByDefault on local
	@Test
	public void testRemoveRedundantNonNullByDefault4() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_NULL_SPECIFICATION_VIOLATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE, DefaultCodeFormatterConstants.createAlignmentValue(true, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE));
		JavaCore.setOptions(options);

		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("  public void f() {\n");
		buf.append("    @NonNullByDefault({DefaultLocation.PARAMETER})\n");
		buf.append("    Object local=new Object() {\n");
		buf.append("      public void g() {\n");
		buf.append("        @NonNullByDefault(value={DefaultLocation.PARAMETER})\n");
		buf.append("        Object nested;\n");
		buf.append("      };\n");
		buf.append("    };\n");
		buf.append("  }\n");
		buf.append("}\n");
		System.out.println(buf);
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class E {\n");
		buf.append("  public void f() {\n");
		buf.append("    @NonNullByDefault({DefaultLocation.PARAMETER})\n");
		buf.append("    Object local=new Object() {\n");
		buf.append("      public void g() {\n");
		buf.append("        Object nested;\n");
		buf.append("      };\n");
		buf.append("    };\n");
		buf.append("  }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	// remove redundant @NonNull on field type
	@Test
	public void testRemoveRedundantNonNull() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    @NonNull Object f=new Object();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		int offset= buf.indexOf("Object f");
		AssistContext context= new AssistContext(cu, offset, 0);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, context);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class E {\n");
		buf.append("    Object f=new Object();\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	@Test
	public void testBug514580_avoidRedundantNonNullInChangeMethodSignatureFix() throws Exception {
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
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	void g(@NonNull Number n1, @Nullable Number n2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("\n");
		buf.append("	void h(Number n1, @Nullable Number n2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	public int f(Boolean n1, @Nullable Boolean n2, I1 i1, I2 i2) {\n");
		buf.append("		i1.g(n1, n2);\n");
		buf.append("		i2.h(n1, n2);\n");
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
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	void g(@NonNull Boolean n1, @Nullable Boolean n2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("\n");
		buf.append("	void h(Number n1, @Nullable Number n2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	public int f(Boolean n1, @Nullable Boolean n2, I1 i1, I2 i2) {\n");
		buf.append("		i1.g(n1, n2);\n");
		buf.append("		i2.h(n1, n2);\n");
		buf.append("	}\n");
		buf.append("}");
		assertProposalPreviewEquals(buf.toString(), "Change method 'g(Number, Number)' to 'g(Boolean, Boolean)'", proposals1);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	void g(@NonNull Number n1, @Nullable Number n2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("\n");
		buf.append("	void h(Boolean n1, @Nullable Boolean n2);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	public int f(Boolean n1, @Nullable Boolean n2, I1 i1, I2 i2) {\n");
		buf.append("		i1.g(n1, n2);\n");
		buf.append("		i2.h(n1, n2);\n");
		buf.append("	}\n");
		buf.append("}");
		assertProposalPreviewEquals(buf.toString(), "Change method 'h(Number, Number)' to 'h(Boolean, Boolean)'", proposals2);
	}
	@Test
	public void testBug514580_avoidRedundantNonNullInTypeChange_field() throws Exception {
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
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE})\n");
		buf.append("@interface Pure {}\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})\n");
		buf.append("@interface Mixed {}\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	@NonNull @Mixed @Pure\n");
		buf.append("	public String g();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("	@Mixed @Pure\n");
		buf.append("	public String g();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	Map<? extends Number, @Nullable Integer> @Nullable [] f1;\n");
		buf.append("	Map<? extends Number, @Nullable Integer> @Nullable [] f2;\n");
		buf.append("\n");
		buf.append("	public void f(I1 i1, I2 i2) {\n");
		buf.append("		f1 = i1.g();\n");
		buf.append("		f2 = i2.g();\n");
		buf.append("\n");
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
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE})\n");
		buf.append("@interface Pure {}\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})\n");
		buf.append("@interface Mixed {}\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	@NonNull @Mixed @Pure\n");
		buf.append("	public String g();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("	@Mixed @Pure\n");
		buf.append("	public String g();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	@Mixed @Pure String f1;\n");
		buf.append("	Map<? extends Number, @Nullable Integer> @Nullable [] f2;\n");
		buf.append("\n");
		buf.append("	public void f(I1 i1, I2 i2) {\n");
		buf.append("		f1 = i1.g();\n");
		buf.append("		f2 = i2.g();\n");
		buf.append("\n");
		buf.append("	}\n");
		buf.append("}");
		assertProposalPreviewEquals(buf.toString(), "Change type of 'f1' to 'String'", proposals1);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE})\n");
		buf.append("@interface Pure {}\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})\n");
		buf.append("@interface Mixed {}\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	@NonNull @Mixed @Pure\n");
		buf.append("	public String g();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("	@Mixed @Pure\n");
		buf.append("	public String g();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	Map<? extends Number, @Nullable Integer> @Nullable [] f1;\n");
		buf.append("	@Mixed @Pure String f2;\n");
		buf.append("\n");
		buf.append("	public void f(I1 i1, I2 i2) {\n");
		buf.append("		f1 = i1.g();\n");
		buf.append("		f2 = i2.g();\n");
		buf.append("\n");
		buf.append("	}\n");
		buf.append("}");
		assertProposalPreviewEquals(buf.toString(), "Change type of 'f2' to 'String'", proposals2);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE})\n");
		buf.append("@interface Pure {}\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})\n");
		buf.append("@interface Mixed {}\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	@Mixed public Map<? extends @NonNull Number, @Nullable Integer> @Nullable [] g();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("	@Mixed @Pure\n");
		buf.append("	public String g();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	Map<? extends Number, @Nullable Integer> @Nullable [] f1;\n");
		buf.append("	Map<? extends Number, @Nullable Integer> @Nullable [] f2;\n");
		buf.append("\n");
		buf.append("	public void f(I1 i1, I2 i2) {\n");
		buf.append("		f1 = i1.g();\n");
		buf.append("		f2 = i2.g();\n");
		buf.append("\n");
		buf.append("	}\n");
		buf.append("}");
		assertProposalPreviewEquals(buf.toString(), "Change return type of 'g(..)' to 'Map<? extends Number, Integer>[]'", proposals1);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE})\n");
		buf.append("@interface Pure {}\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})\n");
		buf.append("@interface Mixed {}\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	@NonNull @Mixed @Pure\n");
		buf.append("	public String g();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("	@Mixed public Map<? extends Number, @Nullable Integer> @Nullable [] g();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	Map<? extends Number, @Nullable Integer> @Nullable [] f1;\n");
		buf.append("	Map<? extends Number, @Nullable Integer> @Nullable [] f2;\n");
		buf.append("\n");
		buf.append("	public void f(I1 i1, I2 i2) {\n");
		buf.append("		f1 = i1.g();\n");
		buf.append("		f2 = i2.g();\n");
		buf.append("\n");
		buf.append("	}\n");
		buf.append("}");
		assertProposalPreviewEquals(buf.toString(), "Change return type of 'g(..)' to 'Map<? extends Number, Integer>[]'", proposals2);
	}
	@Test
	public void testBug514580_avoidRedundantNonNullInTypeChange_local() throws Exception {
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
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE})\n");
		buf.append("@interface Pure {}\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})\n");
		buf.append("@interface Mixed {}\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	@Nullable @Mixed @Pure\n");
		buf.append("	abstract String h();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("	@Nullable @Mixed @Pure\n");
		buf.append("	abstract String h();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	public void f(I1 i1, I2 i2) {\n");
		buf.append("		Map<? extends Number, @Nullable Integer>[] l1;\n");
		buf.append("		Map<? extends Number, @Nullable Integer>[] l2;\n");
		buf.append("		l1 = i1.h();\n");
		buf.append("		l2 = i2.h();\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("");
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(2, problems);
		List<IJavaCompletionProposal> proposals1= collectCorrections(cu, problems[0], null);
		List<IJavaCompletionProposal> proposals2= collectCorrections(cu, problems[1], null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE})\n");
		buf.append("@interface Pure {}\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})\n");
		buf.append("@interface Mixed {}\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	@Nullable @Mixed @Pure\n");
		buf.append("	abstract String h();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("	@Nullable @Mixed @Pure\n");
		buf.append("	abstract String h();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	public void f(I1 i1, I2 i2) {\n");
		buf.append("		@Mixed @Pure String l1;\n");
		buf.append("		Map<? extends Number, @Nullable Integer>[] l2;\n");
		buf.append("		l1 = i1.h();\n");
		buf.append("		l2 = i2.h();\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("");
		assertProposalPreviewEquals(buf.toString(), "Change type of 'l1' to 'String'", proposals1);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE})\n");
		buf.append("@interface Pure {}\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})\n");
		buf.append("@interface Mixed {}\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	@Nullable @Mixed @Pure\n");
		buf.append("	abstract String h();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("	@Nullable @Mixed @Pure\n");
		buf.append("	abstract String h();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	public void f(I1 i1, I2 i2) {\n");
		buf.append("		Map<? extends Number, @Nullable Integer>[] l1;\n");
		buf.append("		@Mixed @Pure String l2;\n");
		buf.append("		l1 = i1.h();\n");
		buf.append("		l2 = i2.h();\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("");
		assertProposalPreviewEquals(buf.toString(), "Change type of 'l2' to 'String'", proposals2);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE})\n");
		buf.append("@interface Pure {}\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})\n");
		buf.append("@interface Mixed {}\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	@Mixed abstract Map<? extends @NonNull Number, @Nullable Integer>[] h();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("	@Nullable @Mixed @Pure\n");
		buf.append("	abstract String h();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	public void f(I1 i1, I2 i2) {\n");
		buf.append("		Map<? extends Number, @Nullable Integer>[] l1;\n");
		buf.append("		Map<? extends Number, @Nullable Integer>[] l2;\n");
		buf.append("		l1 = i1.h();\n");
		buf.append("		l2 = i2.h();\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("");
		assertProposalPreviewEquals(buf.toString(), "Change return type of 'h(..)' to 'Map<? extends Number, Integer>[]'", proposals1);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE})\n");
		buf.append("@interface Pure {}\n");
		buf.append("\n");
		buf.append("@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})\n");
		buf.append("@interface Mixed {}\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("interface I1 {\n");
		buf.append("	@Nullable @Mixed @Pure\n");
		buf.append("	abstract String h();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("interface I2 {\n");
		buf.append("	@Mixed abstract Map<? extends Number, @Nullable Integer>[] h();\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class X {\n");
		buf.append("	public void f(I1 i1, I2 i2) {\n");
		buf.append("		Map<? extends Number, @Nullable Integer>[] l1;\n");
		buf.append("		Map<? extends Number, @Nullable Integer>[] l2;\n");
		buf.append("		l1 = i1.h();\n");
		buf.append("		l2 = i2.h();\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("");
		assertProposalPreviewEquals(buf.toString(), "Change return type of 'h(..)' to 'Map<? extends Number, Integer>[]'", proposals2);
	}

	// bug 420116 : create parameter quickfix should be offered
	@Test
	public void testBug496103_createParam() throws Exception {
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);


		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	protected final String param1;\n");
		buf.append("	public E(String param1) {\n");
		buf.append("		this.param1 = param1;\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("public class F extends E {\n");
		buf.append("	public F() {\n");
		buf.append("		super(param1);\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("");


		ICompilationUnit cu= pack2.createCompilationUnit("F.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	protected final static String param1;\n");
		buf.append("	public E(String param1) {\n");
		buf.append("		this.param1 = param1;\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("public class F extends E {\n");
		buf.append("	public F() {\n");
		buf.append("		super(param1);\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("");
		String expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] {expected});

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	protected final String param1;\n");
		buf.append("	public E(String param1) {\n");
		buf.append("		this.param1 = param1;\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("public class F extends E {\n");
		buf.append("	private static String param1;\n");
		buf.append("\n");
		buf.append("    public F() {\n");
		buf.append("		super(param1);\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] {expected});

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	protected final String param1;\n");
		buf.append("	public E(String param1) {\n");
		buf.append("		this.param1 = param1;\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("public class F extends E {\n");
		buf.append("	public F(String param1) {\n");
		buf.append("		super(param1);\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] {expected});

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class E {\n");
		buf.append("	protected final String param1;\n");
		buf.append("	public E(String param1) {\n");
		buf.append("		this.param1 = param1;\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("public class F extends E {\n");
		buf.append("	private static final String param1 = null;\n");
		buf.append("\n");
		buf.append("    public F() {\n");
		buf.append("		super(param1);\n");
		buf.append("	}\n");
		buf.append("}\n");
		buf.append("");
		expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] {expected});
	}

}
