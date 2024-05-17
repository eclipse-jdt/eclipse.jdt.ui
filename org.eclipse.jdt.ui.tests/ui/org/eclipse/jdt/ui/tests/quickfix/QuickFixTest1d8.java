/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.FixMessages;

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
		String str= """
			package test2;
			import java.io.IOException;
			public interface Inter {
			    int getCount(Object[] o) throws IOException;
			    static int staticMethod(Object[] o) throws IOException{return 10;}
			    default int defaultMethod(Object[] o) throws IOException{return 20;}
			}
			""";
		pack2.createCompilationUnit("Inter.java", str, false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import test2.Inter;
			public class E implements Inter{
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test2.Inter;
			public abstract class E implements Inter{
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;

			import test2.Inter;
			public class E implements Inter{

			    @Override
			    public int getCount(Object[] o) throws IOException {
			        return 0;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testUnimplementedMethods2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class MyString implements CharSequence{
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("MyString.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public abstract class MyString implements CharSequence{
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class MyString implements CharSequence{

			    @Override
			    public char charAt(int arg0) {
			        return 0;
			    }

			    @Override
			    public int length() {
			        return 0;
			    }

			    @Override
			    public CharSequence subSequence(int arg0, int arg1) {
			        return null;
			    }
			}
			""";

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
		String str= """
			package test1;
			import test2.Inter;
			public class E implements Inter{
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 4);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test2.Inter;
			public abstract class E implements Inter{
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			import java.util.List;

			import org.eclipse.jdt.annotation.NonNull;
			import org.eclipse.jdt.annotation.Nullable;

			import test2.Inter;
			public class E implements Inter{

			    @Override
			    public int foo(@NonNull String @Nullable... s) throws IOException {
			        return 0;
			    }

			    @Override
			    public int bug(@NonNull String... s) throws IOException {
			        return 0;
			    }

			    @Override
			    public @NonNull String bar(@NonNull String s, @Nullable List<String> l1,
			            test2.@NonNull List l2) {
			        return null;
			    }

			    @Override
			    public int boo(Object @NonNull [] @Nullable... o1) {
			        return 0;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	// bug 420116 : test for annotated varargs and return type
	@Test
	public void testUnimplementedMethods4() throws Exception {
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);


		String str= """
			package test1;
			import java.io.IOException;

			import org.eclipse.jdt.annotation.*;
			public interface Inter {
			    public int foo(@NonNull String @Nullable... s) throws IOException;
			    public int bar(@NonNull String... s) throws IOException;
			    static int staticMethod(Object[] o) throws IOException{return 10;}
			    default int defaultMethod(Object[] o) throws IOException{return 20;}
			}
			class E implements Inter{
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("Inter.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;

			import org.eclipse.jdt.annotation.*;
			public interface Inter {
			    public int foo(@NonNull String @Nullable... s) throws IOException;
			    public int bar(@NonNull String... s) throws IOException;
			    static int staticMethod(Object[] o) throws IOException{return 10;}
			    default int defaultMethod(Object[] o) throws IOException{return 20;}
			}
			abstract class E implements Inter{
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;

			import org.eclipse.jdt.annotation.*;
			public interface Inter {
			    public int foo(@NonNull String @Nullable... s) throws IOException;
			    public int bar(@NonNull String... s) throws IOException;
			    static int staticMethod(Object[] o) throws IOException{return 10;}
			    default int defaultMethod(Object[] o) throws IOException{return 20;}
			}
			class E implements Inter{

			    @Override
			    public int foo(@NonNull String @Nullable... s) throws IOException {
			        return 0;
			    }

			    @Override
			    public int bar(@NonNull String... s) throws IOException {
			        return 0;
			    }
			}
			""";

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
		String str= """
			package test1;
			import test2.Inter;
			public class E implements Inter{
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(1);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test2.Inter;
			public abstract class E implements Inter{
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(0);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test2.Inter;
			import test2.N1;
			import test2.N2;
			import test2.N3;
			import test2.N4;
			public class E implements Inter{

			    @Override
			    public int foo2U(@N1 String @N3 [] @N2 [] s1,
			            @N1 String @N2 [] @N3 [] @N4... s2) {
			        return 0;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnimplementedMethods6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.math.BigInteger;
			interface IHasInt {
			    int getInt();
			}
			interface IHasIntAsBigInteger extends IHasInt {
			    default int getInt() {
			        return getIntAsBigInteger().intValue();
			    }
			    BigInteger getIntAsBigInteger();
			}
			class C implements IHasIntAsBigInteger {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("IHasInt.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			import java.math.BigInteger;
			interface IHasInt {
			    int getInt();
			}
			interface IHasIntAsBigInteger extends IHasInt {
			    default int getInt() {
			        return getIntAsBigInteger().intValue();
			    }
			    BigInteger getIntAsBigInteger();
			}
			class C implements IHasIntAsBigInteger {

			    @Override
			    public BigInteger getIntAsBigInteger() {
			        return null;
			    }
			}
			""";

		expected[1]= """
			package test1;
			import java.math.BigInteger;
			interface IHasInt {
			    int getInt();
			}
			interface IHasIntAsBigInteger extends IHasInt {
			    default int getInt() {
			        return getIntAsBigInteger().intValue();
			    }
			    BigInteger getIntAsBigInteger();
			}
			abstract class C implements IHasIntAsBigInteger {
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodReference1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    void foo(Runnable r) {
			    }
			    void bar() {
			        foo(this::action);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= new AssistContext(cu, str.indexOf("::"), 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			class E {
			    void foo(Runnable r) {
			    }
			    void bar() {
			        foo(this::action);
			    }
			    private void action() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodReference2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Comparator;
			class E {
			    void foo(Comparator<String> c) {
			    }
			    void bar() {
			        foo(this::action);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= new AssistContext(cu, str.indexOf("::"), 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			import java.util.Comparator;
			class E {
			    void foo(Comparator<String> c) {
			    }
			    void bar() {
			        foo(this::action);
			    }
			    private int action(String string1, String string2) {
			        return 0;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodReference3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Function;
			class E {
			    void foo(Function<Float, String> f) {
			    }
			    void bar() {
			        foo(this::action);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= new AssistContext(cu, str.indexOf("::"), 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			import java.util.function.Function;
			class E {
			    void foo(Function<Float, String> f) {
			    }
			    void bar() {
			        foo(this::action);
			    }
			    private String action(Float float1) {
			        return "";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodReference4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface F<T>
			    T baz();
			}
			class E {
			    <T> void foo(F<T> f) {
			    }
			    void bar() {
			        foo(this::action);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= new AssistContext(cu, str.indexOf("::"), 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			public interface F<T>
			    T baz();
			}
			class E {
			    <T> void foo(F<T> f) {
			    }
			    void bar() {
			        foo(this::action);
			    }
			    private <T> T action() {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUnimplementedMethodReference5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.BiFunction;
			class E {
			    <A extends Object, B extends Object> void foo(BiFunction<A, B, Float> bf) {
			    }
			    void bar() {
			        foo(this::action);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= new AssistContext(cu, str.indexOf("::"), 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package test1;
			import java.util.function.BiFunction;
			class E {
			    <A extends Object, B extends Object> void foo(BiFunction<A, B, Float> bf) {
			    }
			    void bar() {
			        foo(this::action);
			    }
			    private <A extends Object, B extends Object> Float action(A a1, B b2) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testLambdaReturnType1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			@FunctionalInterface
			interface I {
			     int foo(int x);   \s
			}

			public class A {   \s
			    int fun2() {
			        I i= (int x) -> {
			            x++;
			            System.out.println(x);
			        };
			        return 10;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		String str1= """
			package test1;
			@FunctionalInterface
			interface I {
			     int foo(int x);   \s
			}

			public class A {   \s
			    int fun2() {
			        I i= (int x) -> {
			            x++;
			            System.out.println(x);
			            return x;
			        };
			        return 10;
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

	@Test
	public void testLambdaReturnType2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			@FunctionalInterface
			interface I {
			     int foo(int x);   \s
			}

			public class A {   \s
			    void fun2() {
			        I i= (int x) -> {
			            x++;
			            System.out.println(x);
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		String str1= """
			package test1;
			@FunctionalInterface
			interface I {
			     int foo(int x);   \s
			}

			public class A {   \s
			    void fun2() {
			        I i= (int x) -> {
			            x++;
			            System.out.println(x);
			            return x;
			        };
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

	@Test
	public void testLambdaReturnType3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			@FunctionalInterface
			interface I {
			     int foo(int x);   \s
			}

			public class A {   \s
			    void fun2() {
			        I i= (x) -> {
			            x++;
			            System.out.println(x);
			        };
			        i.foo(10);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		String str1= """
			package test1;
			@FunctionalInterface
			interface I {
			     int foo(int x);   \s
			}

			public class A {   \s
			    void fun2() {
			        I i= (x) -> {
			            x++;
			            System.out.println(x);
			            return x;
			        };
			        i.foo(10);
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

	@Test
	public void testLambdaReturnType4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			@FunctionalInterface
			interface I {
			     int foo(int x);   \s
			}

			public class A {   \s
			    I i2= (int x) -> {
			        x++;
			        System.out.println(x);
			    };
			   \s
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		String str1= """
			package test1;
			@FunctionalInterface
			interface I {
			     int foo(int x);   \s
			}

			public class A {   \s
			    I i2= (int x) -> {
			        x++;
			        System.out.println(x);
			        return x;
			    };
			   \s
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

	// bug 424172
	@Test
	public void testImportTypeInMethodReference() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test.one", false, null);
		String str= """
			package test.one;

			public class X {
			    public static F2 staticMethod() {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("X.java", str, false, null);
		String str1= """
			package test.one;

			@FunctionalInterface
			public interface F2 {
			    void foo();
			}
			""";
		pack1.createCompilationUnit("F2.java", str1, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test.two", false, null);
		String str2= """
			package test.two;

			import test.one.F2;

			public class C1 {
			    public void fun1() {
			        F2 f = X::staticMethod;
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("C1.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		String str3= """
			package test.two;

			import test.one.F2;
			import test.one.X;

			public class C1 {
			    public void fun1() {
			        F2 f = X::staticMethod;
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str3 });
	}

	@Test
	public void testLambdaReturnType5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.math.BigInteger;

			interface A { Object m(Class c); }
			interface B<S extends Number> { Object m(Class<S> c); }
			interface C<T extends BigInteger> { Object m(Class<T> c); }
			@FunctionalInterface
			interface D<S,T> extends A, B<BigInteger>, C<BigInteger> {}

			class E {
			    private void foo() {
			         D<BigInteger,BigInteger> d1= (x) -> {
			            };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		String str1= """
			package test1;
			import java.math.BigInteger;

			interface A { Object m(Class c); }
			interface B<S extends Number> { Object m(Class<S> c); }
			interface C<T extends BigInteger> { Object m(Class<T> c); }
			@FunctionalInterface
			interface D<S,T> extends A, B<BigInteger>, C<BigInteger> {}

			class E {
			    private void foo() {
			         D<BigInteger,BigInteger> d1= (x) -> {
			            return x;
			            };
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

	@Test
	public void testChangeModifierToStatic1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface A {
			    int i = foo();
			    default int foo() {
			    }
			   \s
			    int j = bar1();
			    abstract int bar1();
			    static void temp() {
			        bar2();
			    }
			    abstract void bar2();
			   \s
			    int k = fun1();
			    int fun1();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 4, 0);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String str1= """
			package test1;
			interface A {
			    int i = foo();
			    static int foo() {
			    }
			   \s
			    int j = bar1();
			    abstract int bar1();
			    static void temp() {
			        bar2();
			    }
			    abstract void bar2();
			   \s
			    int k = fun1();
			    int fun1();
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });

		proposals= collectCorrections(cu, astRoot, 4, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		String str2= """
			package test1;
			interface A {
			    int i = foo();
			    default int foo() {
			    }
			   \s
			    int j = bar1();
			    static int bar1() {
			        return 0;
			    }
			    static void temp() {
			        bar2();
			    }
			    abstract void bar2();
			   \s
			    int k = fun1();
			    int fun1();
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str2 });

		proposals= collectCorrections(cu, astRoot, 4, 2);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		String str3= """
			package test1;
			interface A {
			    int i = foo();
			    default int foo() {
			    }
			   \s
			    int j = bar1();
			    abstract int bar1();
			    static void temp() {
			        bar2();
			    }
			    static void bar2() {
			    }
			   \s
			    int k = fun1();
			    int fun1();
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str3 });

		proposals= collectCorrections(cu, astRoot, 4, 3);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		proposal= (CUCorrectionProposal)proposals.get(0);
		String str4= """
			package test1;
			interface A {
			    int i = foo();
			    default int foo() {
			    }
			   \s
			    int j = bar1();
			    abstract int bar1();
			    static void temp() {
			        bar2();
			    }
			    abstract void bar2();
			   \s
			    int k = fun1();
			    static int fun1() {
			        return 0;
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str4 });
	}

	@Test
	public void testChangeModifierToStatic2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface Test {
			    int i= foo();
			    abstract int foo() {
			        return 100;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);

		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, 0);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String str1= """
			package test1;
			interface Test {
			    int i= foo();
			    static int foo() {
			        return 100;
			    }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });
	}

	// bug 410170
	@Test
	public void testInvalidInterfaceMethodModifier1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public interface Inter {
			    private static int staticMethod(Object[] o) throws IOException{return 10;}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Inter.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;
			public interface Inter {
			    static int staticMethod(Object[] o) throws IOException{return 10;}
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	//  bug 410170
	@Test
	public void testInvalidInterfaceMethodModifier2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.IOException;
			public interface Inter {
			    protected default int defaultMethod(Object[] o) throws IOException{return 10;}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Inter.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;
			public interface Inter {
			    default int defaultMethod(Object[] o) throws IOException{return 10;}
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	// bug 414084
	@Test
	public void testAbstractInterfaceMethodWithBody1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface Snippet{

			    public char m1(int arg0) {
			    }

			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public interface Snippet{

			    public char m1(int arg0);

			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public interface Snippet{

			    public static char m1(int arg0) {
			    }

			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public interface Snippet{

			    public default char m1(int arg0) {
			    }

			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}

	// bug 414084
	@Test
	public void testAbstractInterfaceMethodWithBody2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface Snippet{

			    public abstract char m1(int arg0) {
			    }

			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public interface Snippet{

			    public abstract char m1(int arg0);

			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public interface Snippet{

			    public static char m1(int arg0) {
			    }

			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public interface Snippet{

			    public default char m1(int arg0) {
			    }

			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	// bug 434173
	@Test
	public void testAbstractInterfaceMethodWithBody3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    public strictfp native void foo() {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<ICompletionProposal> proposals= collectAllCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(3);

		String str1= """
			package test1;
			interface I {
			    public void foo() {}
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });

		proposal= (CUCorrectionProposal)proposals.get(0);

		String str2= """
			package test1;
			interface I {
			    public void foo();
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str2 });
	}

	// bug 424616
	@Test
	public void testInferredExceptionType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class AddThrows {
			  interface Getter2<T, E extends Exception> { T get() throws E; }
			 \s
			  public static Long main2(Getter2<Long, ?> getter) {
			    Long value = getter == null ? 0l : 1l;
			    value = getter.get();
			    return value;
			  }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("AddThrows.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		String str1= """
			package test1;
			public class AddThrows {
			  interface Getter2<T, E extends Exception> { T get() throws E; }
			 \s
			  public static Long main2(Getter2<Long, ?> getter) {
			    Long value = getter == null ? 0l : 1l;
			    try {
			        value = getter.get();
			    } catch (Exception e) {
			        // TODO Auto-generated catch block
			        e.printStackTrace();
			    }
			    return value;
			  }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str1 });

		proposal= (CUCorrectionProposal)proposals.get(1);

		String str2= """
			package test1;
			public class AddThrows {
			  interface Getter2<T, E extends Exception> { T get() throws E; }
			 \s
			  public static Long main2(Getter2<Long, ?> getter) throws Exception {
			    Long value = getter == null ? 0l : 1l;
			    value = getter.get();
			    return value;
			  }
			}
			""";
		assertEqualStringsIgnoreOrder(new String[] { getPreviewContent(proposal) }, new String[] { str2 });
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
		String str= """
			package test1;
			public class E {
			    public <T extends Number> double foo(T t) {
			        Number n=t;
			        return n.doubleValue();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;

			import org.eclipse.jdt.annotation.NonNull;

			public class E {
			    public <T extends Number> double foo(T t) {
			        @NonNull Number n=t;
			        return n.doubleValue();
			    }
			}
			""";
		assertEqualString(preview, str1);
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
		String str= """
			package test1;
			public class E {
			    public <T extends Number> double foo(T t) {
			        Number n=t;
			        return n.doubleValue();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;

			import org.eclipse.jdt.annotation.NonNull;

			public class E {
			    public <T extends Number> double foo(T t) {
			        @NonNull
			        Number n=t;
			        return n.doubleValue();
			    }
			}
			""";
		assertEqualString(preview, str1);
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
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNullByDefault({DefaultLocation.PARAMETER})
			    Runnable f=new Runnable() {
			      @Override
			      @NonNullByDefault({DefaultLocation.PARAMETER})
			      public void run() {
			      }
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNullByDefault({DefaultLocation.PARAMETER})
			    Runnable f=new Runnable() {
			      @Override
			      public void run() {
			      }
			    };
			}
			""";
		assertEqualString(preview, str1);
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
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			  public void f() {
			    @NonNullByDefault({DefaultLocation.PARAMETER})
			    Runnable local=new Runnable() {
			      @Override
			      @NonNullByDefault({DefaultLocation.PARAMETER})
			      public void run() {
			      }
			    };
			  }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			  public void f() {
			    @NonNullByDefault({DefaultLocation.PARAMETER})
			    Runnable local=new Runnable() {
			      @Override
			      public void run() {
			      }
			    };
			  }
			}
			""";
		assertEqualString(preview, str1);
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
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    @NonNullByDefault
			    Object f=new Object();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    Object f=new Object();
			}
			""";
		assertEqualString(preview, str1);
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

		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			  public void f() {
			    @NonNullByDefault({DefaultLocation.PARAMETER})
			    Object local=new Object() {
			      public void g() {
			        Object nested;
			      };
			    };
			  }
			}
			""";
		assertEqualString(preview, str);
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

		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    Object f=new Object();
			}
			""";
		assertEqualString(preview, str);
	}

	@Test
	public void testBug514580_avoidRedundantNonNullInChangeMethodSignatureFix() throws Exception {
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

			import org.eclipse.jdt.annotation.NonNull;
			import org.eclipse.jdt.annotation.NonNullByDefault;
			import org.eclipse.jdt.annotation.Nullable;

			@NonNullByDefault({})
			interface I1 {
				void g(@NonNull Number n1, @Nullable Number n2);
			}

			interface I2 {

				void h(Number n1, @Nullable Number n2);
			}

			class X {
				public int f(Boolean n1, @Nullable Boolean n2, I1 i1, I2 i2) {
					i1.g(n1, n2);
					i2.h(n1, n2);
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

			import org.eclipse.jdt.annotation.NonNull;
			import org.eclipse.jdt.annotation.NonNullByDefault;
			import org.eclipse.jdt.annotation.Nullable;

			@NonNullByDefault({})
			interface I1 {
				void g(@NonNull Boolean n1, @Nullable Boolean n2);
			}

			interface I2 {

				void h(Number n1, @Nullable Number n2);
			}

			class X {
				public int f(Boolean n1, @Nullable Boolean n2, I1 i1, I2 i2) {
					i1.g(n1, n2);
					i2.h(n1, n2);
				}
			}""";
		assertProposalPreviewEquals(str2, "Change method 'g(Number, Number)' to 'g(Boolean, Boolean)'", proposals1);

		String str3= """
			package test1;

			import org.eclipse.jdt.annotation.NonNull;
			import org.eclipse.jdt.annotation.NonNullByDefault;
			import org.eclipse.jdt.annotation.Nullable;

			@NonNullByDefault({})
			interface I1 {
				void g(@NonNull Number n1, @Nullable Number n2);
			}

			interface I2 {

				void h(Boolean n1, @Nullable Boolean n2);
			}

			class X {
				public int f(Boolean n1, @Nullable Boolean n2, I1 i1, I2 i2) {
					i1.g(n1, n2);
					i2.h(n1, n2);
				}
			}""";
		assertProposalPreviewEquals(str3, "Change method 'h(Number, Number)' to 'h(Boolean, Boolean)'", proposals2);
	}
	@Test
	public void testBug514580_avoidRedundantNonNullInTypeChange_field() throws Exception {
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

			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.Map;

			import org.eclipse.jdt.annotation.*;

			@Target({ElementType.TYPE_USE})
			@interface Pure {}

			@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
			@interface Mixed {}

			@NonNullByDefault({})
			interface I1 {
				@NonNull @Mixed @Pure
				public String g();
			}

			interface I2 {
				@Mixed @Pure
				public String g();
			}

			class X {
				Map<? extends Number, @Nullable Integer> @Nullable [] f1;
				Map<? extends Number, @Nullable Integer> @Nullable [] f2;

				public void f(I1 i1, I2 i2) {
					f1 = i1.g();
					f2 = i2.g();

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

			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.Map;

			import org.eclipse.jdt.annotation.*;

			@Target({ElementType.TYPE_USE})
			@interface Pure {}

			@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
			@interface Mixed {}

			@NonNullByDefault({})
			interface I1 {
				@NonNull @Mixed @Pure
				public String g();
			}

			interface I2 {
				@Mixed @Pure
				public String g();
			}

			class X {
				@Mixed @Pure String f1;
				Map<? extends Number, @Nullable Integer> @Nullable [] f2;

				public void f(I1 i1, I2 i2) {
					f1 = i1.g();
					f2 = i2.g();

				}
			}""";
		assertProposalPreviewEquals(str2, "Change type of 'f1' to 'String'", proposals1);

		String str3= """
			package test1;

			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.Map;

			import org.eclipse.jdt.annotation.*;

			@Target({ElementType.TYPE_USE})
			@interface Pure {}

			@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
			@interface Mixed {}

			@NonNullByDefault({})
			interface I1 {
				@NonNull @Mixed @Pure
				public String g();
			}

			interface I2 {
				@Mixed @Pure
				public String g();
			}

			class X {
				Map<? extends Number, @Nullable Integer> @Nullable [] f1;
				@Mixed @Pure String f2;

				public void f(I1 i1, I2 i2) {
					f1 = i1.g();
					f2 = i2.g();

				}
			}""";
		assertProposalPreviewEquals(str3, "Change type of 'f2' to 'String'", proposals2);

		String str4= """
			package test1;

			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.Map;

			import org.eclipse.jdt.annotation.*;

			@Target({ElementType.TYPE_USE})
			@interface Pure {}

			@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
			@interface Mixed {}

			@NonNullByDefault({})
			interface I1 {
				@Mixed public Map<? extends @NonNull Number, @Nullable Integer> @Nullable [] g();
			}

			interface I2 {
				@Mixed @Pure
				public String g();
			}

			class X {
				Map<? extends Number, @Nullable Integer> @Nullable [] f1;
				Map<? extends Number, @Nullable Integer> @Nullable [] f2;

				public void f(I1 i1, I2 i2) {
					f1 = i1.g();
					f2 = i2.g();

				}
			}""";
		assertProposalPreviewEquals(str4, "Change return type of 'g(..)' to 'Map<? extends Number, Integer>[]'", proposals1);

		String str5= """
			package test1;

			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.Map;

			import org.eclipse.jdt.annotation.*;

			@Target({ElementType.TYPE_USE})
			@interface Pure {}

			@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
			@interface Mixed {}

			@NonNullByDefault({})
			interface I1 {
				@NonNull @Mixed @Pure
				public String g();
			}

			interface I2 {
				@Mixed public Map<? extends Number, @Nullable Integer> @Nullable [] g();
			}

			class X {
				Map<? extends Number, @Nullable Integer> @Nullable [] f1;
				Map<? extends Number, @Nullable Integer> @Nullable [] f2;

				public void f(I1 i1, I2 i2) {
					f1 = i1.g();
					f2 = i2.g();

				}
			}""";
		assertProposalPreviewEquals(str5, "Change return type of 'g(..)' to 'Map<? extends Number, Integer>[]'", proposals2);
	}
	@Test
	public void testBug514580_avoidRedundantNonNullInTypeChange_local() throws Exception {
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

			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.Map;

			import org.eclipse.jdt.annotation.*;

			@Target({ElementType.TYPE_USE})
			@interface Pure {}

			@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
			@interface Mixed {}

			@NonNullByDefault({})
			interface I1 {
				@Nullable @Mixed @Pure
				abstract String h();
			}

			interface I2 {
				@Nullable @Mixed @Pure
				abstract String h();
			}

			class X {
				public void f(I1 i1, I2 i2) {
					Map<? extends Number, @Nullable Integer>[] l1;
					Map<? extends Number, @Nullable Integer>[] l2;
					l1 = i1.h();
					l2 = i2.h();
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(2, problems);
		List<IJavaCompletionProposal> proposals1= collectCorrections(cu, problems[0], null);
		List<IJavaCompletionProposal> proposals2= collectCorrections(cu, problems[1], null);

		String str2= """
			package test1;

			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.Map;

			import org.eclipse.jdt.annotation.*;

			@Target({ElementType.TYPE_USE})
			@interface Pure {}

			@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
			@interface Mixed {}

			@NonNullByDefault({})
			interface I1 {
				@Nullable @Mixed @Pure
				abstract String h();
			}

			interface I2 {
				@Nullable @Mixed @Pure
				abstract String h();
			}

			class X {
				public void f(I1 i1, I2 i2) {
					@Mixed @Pure String l1;
					Map<? extends Number, @Nullable Integer>[] l2;
					l1 = i1.h();
					l2 = i2.h();
				}
			}
			""";
		assertProposalPreviewEquals(str2, "Change type of 'l1' to 'String'", proposals1);

		String str3= """
			package test1;

			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.Map;

			import org.eclipse.jdt.annotation.*;

			@Target({ElementType.TYPE_USE})
			@interface Pure {}

			@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
			@interface Mixed {}

			@NonNullByDefault({})
			interface I1 {
				@Nullable @Mixed @Pure
				abstract String h();
			}

			interface I2 {
				@Nullable @Mixed @Pure
				abstract String h();
			}

			class X {
				public void f(I1 i1, I2 i2) {
					Map<? extends Number, @Nullable Integer>[] l1;
					@Mixed @Pure String l2;
					l1 = i1.h();
					l2 = i2.h();
				}
			}
			""";
		assertProposalPreviewEquals(str3, "Change type of 'l2' to 'String'", proposals2);

		String str4= """
			package test1;

			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.Map;

			import org.eclipse.jdt.annotation.*;

			@Target({ElementType.TYPE_USE})
			@interface Pure {}

			@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
			@interface Mixed {}

			@NonNullByDefault({})
			interface I1 {
				@Mixed abstract Map<? extends @NonNull Number, @Nullable Integer>[] h();
			}

			interface I2 {
				@Nullable @Mixed @Pure
				abstract String h();
			}

			class X {
				public void f(I1 i1, I2 i2) {
					Map<? extends Number, @Nullable Integer>[] l1;
					Map<? extends Number, @Nullable Integer>[] l2;
					l1 = i1.h();
					l2 = i2.h();
				}
			}
			""";
		assertProposalPreviewEquals(str4, "Change return type of 'h(..)' to 'Map<? extends Number, Integer>[]'", proposals1);

		String str5= """
			package test1;

			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.Map;

			import org.eclipse.jdt.annotation.*;

			@Target({ElementType.TYPE_USE})
			@interface Pure {}

			@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
			@interface Mixed {}

			@NonNullByDefault({})
			interface I1 {
				@Nullable @Mixed @Pure
				abstract String h();
			}

			interface I2 {
				@Mixed abstract Map<? extends Number, @Nullable Integer>[] h();
			}

			class X {
				public void f(I1 i1, I2 i2) {
					Map<? extends Number, @Nullable Integer>[] l1;
					Map<? extends Number, @Nullable Integer>[] l2;
					l1 = i1.h();
					l2 = i2.h();
				}
			}
			""";
		assertProposalPreviewEquals(str5, "Change return type of 'h(..)' to 'Map<? extends Number, Integer>[]'", proposals2);
	}

	// bug 420116 : create parameter quickfix should be offered
	@Test
	public void testBug496103_createParam() throws Exception {
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);


		String str= """
			package test1;
			class E {
				protected final String param1;
				public E(String param1) {
					this.param1 = param1;
				}
			}
			public class F extends E {
				public F() {
					super(param1);
				}
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("F.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			class E {
				protected final static String param1;
				public E(String param1) {
					this.param1 = param1;
				}
			}
			public class F extends E {
				public F() {
					super(param1);
				}
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected});

		expected= """
			package test1;
			class E {
				protected final String param1;
				public E(String param1) {
					this.param1 = param1;
				}
			}
			public class F extends E {
				private static String param1;

			    public F() {
					super(param1);
				}
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected});

		expected= """
			package test1;
			class E {
				protected final String param1;
				public E(String param1) {
					this.param1 = param1;
				}
			}
			public class F extends E {
				public F(String param1) {
					super(param1);
				}
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected});

		expected= """
			package test1;
			class E {
				protected final String param1;
				public E(String param1) {
					this.param1 = param1;
				}
			}
			public class F extends E {
				private static final String param1 = null;

			    public F() {
					super(param1);
				}
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected});
	}

	// bug 576502 : try-with-resources quick fix should be offered for resource leaks
	@Test
	public void testBug576502_potentiallyUnclosedCloseable() throws Exception {
		Hashtable<String, String> options = JavaCore.getOptions();
		options.put(CompilerOptions.OPTION_ReportPotentiallyUnclosedCloseable, CompilerOptions.ERROR);
		JavaCore.setOptions(options);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);


		String str= """
			package test1;
			import java.io.File;
			import java.io.FileReader;
			import java.io.IOException;
			class E {
			    public void foo() throws IOException {
			        final File file = new File("somefile");
			        FileReader fileReader = new FileReader(file);
			        char[] in = new char[50];
			        fileReader.read(in);
			        new Runnable() {
			            public void run() {\s
			                try {
			                    fileReader.close();
			                } catch (IOException ex) { /* nop */ }
			            }}.run();
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			import java.io.File;
			import java.io.FileReader;
			import java.io.IOException;
			class E {
			    public void foo() throws IOException {
			        final File file = new File("somefile");
			        try (FileReader fileReader = new FileReader(file)) {
			            char[] in = new char[50];
			            fileReader.read(in);
			            new Runnable() {
			                public void run() {\s
			                    try {
			                        fileReader.close();
			                    } catch (IOException ex) { /* nop */ }
			                }}.run();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected});
	}

	// bug 576502 : try-with-resources quick fix should be offered for resource leaks
	@Test
	public void testBug576502_unclosedCloseable() throws Exception {
		Hashtable<String, String> options = JavaCore.getOptions();
		options.put(CompilerOptions.OPTION_ReportUnclosedCloseable, CompilerOptions.ERROR);
		JavaCore.setOptions(options);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);


		String str1= """
			package test1;
			import java.io.File;
			import java.io.FileReader;
			import java.io.IOException;
			class E {
			    public void foo(boolean f1, boolean f2) throws IOException {
			        File file = new File("somefile");
			        if (f1) {
			            FileReader fileReader = new FileReader(file); // err: not closed
			            char[] in = new char[50];
			            fileReader.read(in);
			            while (true) {
			                FileReader loopReader = new FileReader(file); // don't warn, properly closed
			                loopReader.close();
			                break;
			            }
			        } else {
			            FileReader fileReader = new FileReader(file); // warn: not closed on all paths
			            if (f2)
			                fileReader.close();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str1, false, null);
        String str= "fileReader";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);


		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			import java.io.File;
			import java.io.FileReader;
			import java.io.IOException;
			class E {
			    public void foo(boolean f1, boolean f2) throws IOException {
			        File file = new File("somefile");
			        if (f1) {
			            try (FileReader fileReader = new FileReader(file)) {
			                char[] in = new char[50];
			                fileReader.read(in);
			            }
			            while (true) {
			                FileReader loopReader = new FileReader(file); // don't warn, properly closed
			                loopReader.close();
			                break;
			            }
			        } else {
			            FileReader fileReader = new FileReader(file); // warn: not closed on all paths
			            if (f2)
			                fileReader.close();
			        }
			    }
			}
			""";

		// bug 576701 - ensure we don't duplicate try-with-resources as assist as well
		assertExpectedExistInProposals(proposals, new String[] {expected});

		List<IJavaCompletionProposal> assists= collectAssistsWithProblems(context);
		assertProposalDoesNotExist(assists, CorrectionMessages.QuickAssistProcessor_convert_to_try_with_resource);
	}

	// issue 352 : make suggested imports smarter
	@Test
	public void testIssue352_smarterImports() throws Exception {
		Hashtable<String, String> options = JavaCore.getOptions();
		JavaCore.setOptions(options);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack3= fSourceFolder.createPackageFragment("test2", false, null);
		IPackageFragment pack4= fSourceFolder.createPackageFragment("test3", false, null);


		String str= """
			package test2;

			public interface ISomePath {
			   \s
			    public String getPath();

			}
			""";
		pack3.createCompilationUnit("ISomePath.java", str, false, null);

		String str1= """
			package test2;

			public class BundleInfo extends BundleInfoBasic implements ISomePath {

			    @Override
			    public String getPath() {
			        // TODO Auto-generated method stub
			        return null;
			    }

			}
			""";
		pack3.createCompilationUnit("BundleInfo.java", str1, false, null);

		String str2= """
			package test3;

			public class BundleInfo {
			   \s
			    public int getInt() {
			        return 43;
			    }

			}
			""";
		pack4.createCompilationUnit("BundleInfo.java", str2, false, null);

		String str3= """
			package test1;

			import test2.ISomePath;

			public class E {
			   \s
			    public ISomePath getSomePath() {
			        return new BundleInfo();
			    }

			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str3, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;

			import test2.BundleInfo;
			import test2.ISomePath;

			public class E {
			   \s
			    public ISomePath getSomePath() {
			        return new BundleInfo();
			    }

			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected});
	}

	// issue 485 : don't check inheritance for equivalent types
	@Test
	public void testIssue485_smarterImports() throws Exception {
		Hashtable<String, String> options = JavaCore.getOptions();
		JavaCore.setOptions(options);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack3= fSourceFolder.createPackageFragment("test2", false, null);

		String str= """
			package test2;

			public class Date {
			   \s
			    public String getDateString();

			}
			""";
		pack3.createCompilationUnit("Date.java", str, false, null);

		String str1= """
			package test1;

			public class E {
			    public static void main(String[] args) {
			        Date d1= new Date();

			        Date d2;
			        d2=new Date();
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 4, null);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;

			import test2.Date;

			public class E {
			    public static void main(String[] args) {
			        Date d1= new Date();

			        Date d2;
			        d2=new Date();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected});
	}

	@Test
	public void testIssue721_fixDeprecatedCall1() throws Exception {
		Hashtable<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION, CompilerOptions.WARNING);
		JavaCore.setOptions(options);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class E1 {
			    public int foo(int a, int b) {
			        return a + b;
			    }

			    /**
			     * @deprecated use {@link #foo(int, int)} instead
			     * @param x - x
			     * @param y - y
			     * @param z - z
			     */
			    @Deprecated
			    public int foo(int x, int y, int z) {
			        int k = 2 * y + 3 * z;
			        return foo(x, k);
			    }
			}
			""";
		pack2.createCompilationUnit("E1.java", str, false, null);

		String str1= """
			package test1;

			class E {

			    public int callfoo(int a, int b, int c) {
			        E1 e1 = new E1();
			        return e1.foo(a, b, c);
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1, null);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;

			class E {

			    public int callfoo(int a, int b, int c) {
			        E1 e1 = new E1();
			        int k = 2 * b + 3 * c;
			        return e1.foo(a, k);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected});
	}

	@Test
	public void testIssue721_fixDeprecatedCall2() throws Exception {
		Hashtable<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION, CompilerOptions.WARNING);
		JavaCore.setOptions(options);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;
			public class E1 {
			    private int k = 4;
			    public int foo(int a, int b) {
			        return a + b;
			    }

			    /**
			     * @deprecated use {@link #foo(int, int)} instead
			     * @param x - x
			     * @param y - y
			     * @param z - z
			     */
			    @Deprecated
			    public int foo(int x, int y, int z) {
			        int k = 2 * y + 3 * z + k;
			        return foo(x, k);
			    }
			}
			""";
		pack2.createCompilationUnit("E1.java", str, false, null);

		String str1= """
			package test1;

			class E {

			    public int callfoo(int a, int b, int c) {
			        E1 e1 = new E1();
			        return e1.foo(a, b, c);
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertProposalDoesNotExist(proposals, FixMessages.InlineDeprecatedMethod_msg);
	}

	// issue 717 : support import quick fix for annotations
	@Test
	public void testIssue717_1() throws Exception {
		Hashtable<String, String> options = JavaCore.getOptions();
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);


		String str= """
			package test1;
			import java.lang.annotation.Documented;
			import java.lang.annotation.Target;

			@Target(ElementType.TYPE_USE)
			@Documented
			@interface NonCritical { }
			class E {

			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(1, problems);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		assertCorrectLabels(proposals);

		String expected1 = """
			package test1;
			import java.lang.annotation.Documented;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;

			@Target(ElementType.TYPE_USE)
			@Documented
			@interface NonCritical { }
			class E {

			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	// issue 717 : support import quick fix for annotations
	@Test
	public void testIssue717_2() throws Exception {
		Hashtable<String, String> options = JavaCore.getOptions();
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);


		String str= """
			package test1;
			import java.lang.annotation.Documented;
			import java.lang.annotation.Target;

			@Target(value=ElementType.TYPE_USE)
			@Documented
			@interface NonCritical { }
			class E {

			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(1, problems);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);
		assertCorrectLabels(proposals);

		String expected1 = """
			package test1;
			import java.lang.annotation.Documented;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;

			@Target(value=ElementType.TYPE_USE)
			@Documented
			@interface NonCritical { }
			class E {

			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	// Bug 148012 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=148012
	@Test
	public void testBug148012() throws Exception {
		Hashtable<String, String> options = JavaCore.getOptions();
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);


		String str= """
			package test1;
			public class E {
			    public static void main(String[] args) {
			        foo()[0];
			    }

			    static Object[] foo() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(1, problems);
		List<IJavaCompletionProposal> proposals= collectCorrectionsNoCheck(cu, problems[0], null);
		assertCorrectLabels(proposals);

		String expected1 = """
			package test1;
			public class E {
			    public static void main(String[] args) {
			        Object object = foo()[0];
			    }

			    static Object[] foo() {
			        return null;
			    }
			}
			""";

		String expected2 = """
			package test1;
			public class E {
			    private static Object object;

			    public static void main(String[] args) {
			        object = foo()[0];
			    }

			    static Object[] foo() {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2});
	}

}
