/*******************************************************************************
 * Copyright (c) 2016, 2018 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import junit.framework.Test;
import junit.framework.TestSuite;


public class TypeAnnotationQuickFixTest extends QuickFixTest {

	private static final Class<TypeAnnotationQuickFixTest> THIS= TypeAnnotationQuickFixTest.class;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public TypeAnnotationQuickFixTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java18ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.DO_NOT_INSERT);


		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= Java18ProjectTestSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}

	public void runQuickFixTest(String testCaseSpecificInput, String... testCaseSpecificOutputs) throws Exception {
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

		JavaProjectHelper.addLibrary(fJProject1, new Path(Java18ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String prefix= "package test;\n" +
				"\n" +
				"import java.lang.annotation.ElementType;\n" +
				"import java.lang.annotation.Target;\n" +
				"\n" +
				"import org.eclipse.jdt.annotation.Nullable;\n" +
				"\n" +
				"@Target(ElementType.TYPE_USE) @interface X {}\n" +
				"@Target(ElementType.TYPE_USE) @interface Y {}\n" +
				"@Target(ElementType.TYPE_USE) @interface N { int[] v1(); String v2(); }\n" +
				"@Target(ElementType.TYPE_USE) @interface S { int[] value(); }\n" +
				"@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE }) @interface Old {}\n" +
				"\n" +
				"class File {}\n" +
				"\n" +
				"class Outer {\n" +
				"	static class StaticInner {}\n" +
				"	class Inner {}\n" +
				"}\n" +
				"\n" +
				"class Generic<T> {@Nullable Object f;}\n";

		String input= prefix +
				"public class Test {\n" +
				testCaseSpecificInput + "\n" +
				"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", input, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		int nProblems= testCaseSpecificOutputs.length;
		for (int p= 0; p < nProblems; p++) {
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, nProblems, p, null);
			assertCorrectLabels(proposals);
			
			CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
			String preview= getPreviewContent(proposal);
			
			String expected= prefix +
					"public class Test {\n" +
					testCaseSpecificOutputs[p] + "\n" +
					"}\n";
			assertEqualString(preview, expected);
		}
	}

	public void testMoveAnnotationM1() throws Exception {
		runQuickFixTest(
				"@Old @Nullable test.File m1() { return null; }",
				"@Old test.@Nullable File m1() { return null; }");
	}

	// assert that multiple problems produce their expected proposals independently
	public void testMoveAnnotationM1a() throws Exception {
		runQuickFixTest(
				"@Old @X @Nullable test.File m1() { return null; }",
				"@Old @Nullable test.@X File m1() { return null; }",
				"@Old @X test.@Nullable File m1() { return null; }"
				);
	}

	public void testMoveAnnotationM2() throws Exception {
		runQuickFixTest(
				"@Old @Nullable test.@X File m2() { return null; }",
				"@Old test.@X @Nullable File m2() { return null; }");
	}

	public void testMoveAnnotationM3() throws Exception {
		runQuickFixTest(
				"@Nullable Outer.StaticInner m4() { return null; }",
				"Outer.@Nullable StaticInner m4() { return null; }");
	}

	public void testMoveAnnotationM4() throws Exception {
		runQuickFixTest(
				"@Nullable Outer.@X StaticInner m3() { return null; }",
				"Outer.@X @Nullable StaticInner m3() { return null; }");
	}

	public void testMoveAnnotationM5() throws Exception {
		runQuickFixTest(
				"@Nullable Outer.Inner m5() { return null; }",
				"Outer.@Nullable Inner m5() { return null; }");
	}

	public void testMoveAnnotationM6() throws Exception {
		runQuickFixTest(
				"@Nullable @Y Outer.@X Inner m6() { return null; }",
				"@Y Outer.@X @Nullable Inner m6() { return null; }");
	}

	public void testMoveAnnotationM7() throws Exception {
		runQuickFixTest(
				"@Nullable test.Generic<?> m7() { return null; }",
				"test.@Nullable Generic<?> m7() { return null; }");
	}

	public void testMoveAnnotationM8() throws Exception {
		runQuickFixTest(
				"@Old @Nullable test.Generic<?> m8() { return null; }",
				"@Old test.@Nullable Generic<?> m8() { return null; }");
	}


	public void testMoveAnnotationM9() throws Exception {
		runQuickFixTest(
				"@Nullable test.Generic<?>[] m9() { return null; }",
				"test.Generic<?> @Nullable [] m9() { return null; }");
	}

	public void testMoveAnnotationM10() throws Exception {
		runQuickFixTest(
				"@Nullable test.Generic<?> @X [] m10() { return null; }",
				"test.Generic<?> @X @Nullable [] m10() { return null; }");
	}

	public void testMoveAnnotationM11() throws Exception {
		runQuickFixTest(
				"@Old @Nullable test.Generic<?>[] m11() { return null; }",
				"@Old test.Generic<?> @Nullable [] m11() { return null; }");
	}

	public void testMoveAnnotationM12() throws Exception {
		runQuickFixTest(
				"@Old @Nullable test.Generic<?> @Y [] @X [] m12() { return null; }",
				"@Old test.Generic<?> @Y @Nullable [] @X [] m12() { return null; }");
	}

	public void testMoveAnnotationP1() throws Exception {
		runQuickFixTest(
				"void p1(@Nullable test.File arg) {}",
				"void p1(test.@Nullable File arg) {}");
	}

	public void testMoveAnnotationP2() throws Exception {
		runQuickFixTest(
				"void p2(@Nullable test.@X File arg) {}",
				"void p2(test.@X @Nullable File arg) {}");
	}

	public void testMoveAnnotationP3() throws Exception {
		runQuickFixTest(
				"void p3(@Nullable @Y Outer.@X Inner arg) {}",
				"void p3(@Y Outer.@X @Nullable Inner arg) {}");
	}

	public void testMoveAnnotationP4() throws Exception {
		runQuickFixTest(
				"void p4(@Nullable test.Generic<?> @Y [] @X [] arg) {}",
				"void p4(test.Generic<?> @Y @Nullable [] @X [] arg) {}");
	}

	public void testMoveAnnotationL1() throws Exception {
		runQuickFixTest(
				"void l1() { @Nullable test.@X File var1, var2;}",
				"void l1() { test.@X @Nullable File var1, var2;}");
	}

	public void testMoveAnnotationB1() throws Exception {
		runQuickFixTest(
				"<T extends @Nullable test.File> T b1() { return null; }",
				"<T extends test.@Nullable File> T b1() { return null; }");
	}

	public void testMoveAnnotationB2() throws Exception {
		runQuickFixTest(
				"<T extends @org.eclipse.jdt.annotation.Nullable test.File> T b2() { return null; }",
				"<T extends test.@org.eclipse.jdt.annotation.Nullable File> T b2() { return null; }");
	}

	public void testMoveAnnotationB3() throws Exception {
		runQuickFixTest(
				"<T extends @org.eclipse.jdt.annotation.Nullable test.@X File> T b3() { return null; }",
				"<T extends test.@X @org.eclipse.jdt.annotation.Nullable File> T b3() { return null; }");
	}

	public void testMoveAnnotationC1() throws Exception {
		runQuickFixTest(
				"Object c1() { return (@Nullable test.File) null; }",
				"Object c1() { return (test.@Nullable File) null; }");
	}

	public void testMoveAnnotationF1() throws Exception {
		runQuickFixTest(
				"@Nullable test.File f1a, f1b;",
				"test.@Nullable File f1a, f1b;");
	}
	
	public void testNormalAnnotationP4() throws Exception {
		runQuickFixTest(
				"void p4(@N(v1 = {100,101}, v2 = \"someString\") test.Generic<?> @Y [] @X [] arg) {}",
				"void p4(test.Generic<?> @Y @N(v1 = {100,101}, v2 = \"someString\") [] @X [] arg) {}");
	}

	// assert that formatting (here space after ,) is preserved by the quickfix
	public void testNormalAnnotationP4a() throws Exception {
		runQuickFixTest(
				"void p4(@N(v1 = {100, 101}, v2 = \"someString\") test.Generic<?> @Y [] @X [] arg) {}",
				"void p4(test.Generic<?> @Y @N(v1 = {100, 101}, v2 = \"someString\") [] @X [] arg) {}");
	}

	public void testMoveNormalAnnotationL1() throws Exception {
		runQuickFixTest(
				"void l1() { @N(v1 = 100, v2 = \"someString\") test.@X File var1, var2;}",
				"void l1() { test.@X @N(v1 = 100, v2 = \"someString\") File var1, var2;}");
	}

	public void testMoveSingleValueAnnotationB2() throws Exception {
		runQuickFixTest(
				"<T extends @S(1) test.File> T b2() { return null; }",
				"<T extends test.@S(1) File> T b2() { return null; }");
	}
	public void testMoveSingleValueAnnotationF1() throws Exception {
		runQuickFixTest(
				"@S({1,2}) test.File f1a, f1b;",
				"test.@S({1,2}) File f1a, f1b;");
	}
	// assert that a comment is preserved:
	public void testMoveSingleValueAnnotationF1a() throws Exception {
		runQuickFixTest(
				"@S({1,2}) test/*here*/.File f1a, f1b;",
				"test/*here*/.@S({1,2}) File f1a, f1b;");
	}
	public void testMoveAnnotationOnByteArray() throws Exception {
		runQuickFixTest(
				"@Old @Nullable byte [] m12() { return null; }",
				"@Old byte @Nullable [] m12() { return null; }");
	}
	public void testMoveAnnotationOnByteMultiDimArray() throws Exception {
		runQuickFixTest(
				"@Old @Nullable byte [][] m12() { return null; }",
				"@Old byte @Nullable [][] m12() { return null; }");
	}
	public void testMoveAnnotationOnByteArraySuffixSyntax() throws Exception {
		runQuickFixTest(
				"@Old @Nullable byte m12()[] { return null; }",
				"@Old byte m12() @Nullable [] { return null; }");
	}
	public void testMoveAnnotationOnByteArrayMixedSyntax() throws Exception {
		runQuickFixTest(
				"@Old @Nullable byte [] m12()[][] { return null; }",
				"@Old byte [] m12() @Nullable [][] { return null; }");
	}

	public void testRemoveAnnotationOnByteNoArray() throws Exception {
		runQuickFixTest(
				"@Old @Nullable byte m12() { return 0; }",
				"@Old byte m12() { return 0; }");
	}
}
