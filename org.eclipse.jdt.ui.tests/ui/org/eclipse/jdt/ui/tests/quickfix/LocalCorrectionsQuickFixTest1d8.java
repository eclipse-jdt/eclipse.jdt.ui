/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.NullTestUtils;
import org.eclipse.jdt.testplugin.TestOptions;

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
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class LocalCorrectionsQuickFixTest1d8 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_HASHCODE_METHOD, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_TYPE_ARGUMENTS, JavaCore.WARNING);

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testUncaughtExceptionTypeUseAnnotation1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void test(int a) {\n");
		buf.append("        throw new @Marker FileNotFoundException();\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface Marker { }\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void test(int a) throws @Marker FileNotFoundException {\n");
		buf.append("        throw new @Marker FileNotFoundException();\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("@interface Marker { }\n");
		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUncaughtExceptionInLambda1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class C1 {\n");
		buf.append("    Runnable r = () -> info(\"Processing rule #{} {}\", \"\");\n");
		buf.append("\n");
		buf.append("    private void info(String string, Object object) throws GridException1 {}\n");
		buf.append("}\n");
		buf.append("@SuppressWarnings(\"serial\")\n");
		buf.append("class GridException1 extends Exception {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class C1 {\n");
		buf.append("    Runnable r = () -> {\n");
		buf.append("        try {\n");
		buf.append("            info(\"Processing rule #{} {}\", \"\");\n");
		buf.append("        } catch (GridException1 e) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("\n");
		buf.append("    private void info(String string, Object object) throws GridException1 {}\n");
		buf.append("}\n");
		buf.append("@SuppressWarnings(\"serial\")\n");
		buf.append("class GridException1 extends Exception {}\n");
		String expected= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testUncaughtExceptionInLambda2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class C2 {\n");
		buf.append("    void test() {\n");
		buf.append("        Runnable r = () -> info(\"Processing rule #{} {}\", \"\");\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void info(String string, Object object) throws GridException2 {}\n");
		buf.append("}\n");
		buf.append("@SuppressWarnings(\"serial\")\n");
		buf.append("class GridException2 extends Exception {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class C2 {\n");
		buf.append("    void test() {\n");
		buf.append("        Runnable r = () -> {\n");
		buf.append("            try {\n");
		buf.append("                info(\"Processing rule #{} {}\", \"\");\n");
		buf.append("            } catch (GridException2 e) {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void info(String string, Object object) throws GridException2 {}\n");
		buf.append("}\n");
		buf.append("@SuppressWarnings(\"serial\")\n");
		buf.append("class GridException2 extends Exception {}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);

		assertProposalDoesNotExist(proposals, CorrectionMessages.LocalCorrectionsSubProcessor_addthrows_description);
	}

	@Test
	public void testUncaughtExceptionInLambda3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class C3 {\n");
		buf.append("    void test(ArrayList<Integer> ruleIds) {\n");
		buf.append("        Runnable r = () -> {\n");
		buf.append("            for (int ruleId : ruleIds) {\n");
		buf.append("                info(\"Processing rule #{} {}\", ruleId);\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void info(String string, Object object) throws GridException3 {}\n");
		buf.append("}\n");
		buf.append("@SuppressWarnings(\"serial\")\n");
		buf.append("class GridException3 extends Exception {}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C3.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class C3 {\n");
		buf.append("    void test(ArrayList<Integer> ruleIds) {\n");
		buf.append("        Runnable r = () -> {\n");
		buf.append("            for (int ruleId : ruleIds) {\n");
		buf.append("                try {\n");
		buf.append("                    info(\"Processing rule #{} {}\", ruleId);\n");
		buf.append("                } catch (GridException3 e) {\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    private void info(String string, Object object) throws GridException3 {}\n");
		buf.append("}\n");
		buf.append("@SuppressWarnings(\"serial\")\n");
		buf.append("class GridException3 extends Exception {}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInLambda4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.stream.Stream;\n");
		buf.append("public class C4 {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            Files.walk(new File(\".\").toPath()).filter(p -> p.toString().endsWith(\".java\"))\n");
		buf.append("                    .forEach(p -> Files.lines(p).forEach(System.out::println));\n");
		buf.append("        } catch (IOException e) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Files {\n");
		buf.append("    public static Stream<Object> walk(Object start) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    public static Stream<String> lines(Object path) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C4.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.stream.Stream;\n");
		buf.append("public class C4 {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("            Files.walk(new File(\".\").toPath()).filter(p -> p.toString().endsWith(\".java\"))\n");
		buf.append("                    .forEach(p -> {\n");
		buf.append("                        try {\n");
		buf.append("                            Files.lines(p).forEach(System.out::println);\n");
		buf.append("                        } catch (IOException e) {\n");
		buf.append("                        }\n");
		buf.append("                    });\n");
		buf.append("        } catch (IOException e) {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Files {\n");
		buf.append("    public static Stream<Object> walk(Object start) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    public static Stream<String> lines(Object path) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInLambda5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("import java.util.stream.Stream;\n");
		buf.append("\n");
		buf.append("public class C5 {\n");
		buf.append("    void foo() {\n");
		buf.append("        Consumer<Object> s = (a) -> Files.walk(new File(\".\").toPath())\n");
		buf.append("                .filter(p -> p.toString().endsWith(\".java\"))\n");
		buf.append("                .forEach(p -> {\n");
		buf.append("                    try {\n");
		buf.append("                        Files.lines(p).forEach(System.out::println);\n");
		buf.append("                    } catch (IOException e) {}\n");
		buf.append("                });\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Files {\n");
		buf.append("    public static Stream<Object> walk(Object start) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    public static Stream<String> lines(Object path) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C5.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("import java.util.stream.Stream;\n");
		buf.append("\n");
		buf.append("public class C5 {\n");
		buf.append("    void foo() {\n");
		buf.append("        Consumer<Object> s = (a) -> {\n");
		buf.append("            try {\n");
		buf.append("                Files.walk(new File(\".\").toPath())\n");
		buf.append("                        .filter(p -> p.toString().endsWith(\".java\"))\n");
		buf.append("                        .forEach(p -> {\n");
		buf.append("                            try {\n");
		buf.append("                                Files.lines(p).forEach(System.out::println);\n");
		buf.append("                            } catch (IOException e) {}\n");
		buf.append("                        });\n");
		buf.append("            } catch (IOException e) {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Files {\n");
		buf.append("    public static Stream<Object> walk(Object start) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    public static Stream<String> lines(Object path) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInLambda6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("import java.util.stream.Stream;\n");
		buf.append("\n");
		buf.append("public class C5 {\n");
		buf.append("    void foo() {\n");
		buf.append("        Consumer<Object> s = (a) -> {\n");
		buf.append("            try {\n");
		buf.append("                Files.walk(new File(\".\").toPath())\n");
		buf.append("                        .filter(p -> p.toString().endsWith(\".java\"))\n");
		buf.append("                        .forEach(p -> Files.lines(p).forEach(System.out::println));\n");
		buf.append("            } catch (IOException e) {}\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Files {\n");
		buf.append("    public static Stream<Object> walk(Object start) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    public static Stream<String> lines(Object path) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C5.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("import java.util.stream.Stream;\n");
		buf.append("\n");
		buf.append("public class C5 {\n");
		buf.append("    void foo() {\n");
		buf.append("        Consumer<Object> s = (a) -> {\n");
		buf.append("            try {\n");
		buf.append("                Files.walk(new File(\".\").toPath())\n");
		buf.append("                        .filter(p -> p.toString().endsWith(\".java\"))\n");
		buf.append("                        .forEach(p -> {\n");
		buf.append("                            try {\n");
		buf.append("                                Files.lines(p).forEach(System.out::println);\n");
		buf.append("                            } catch (IOException e) {\n");
		buf.append("                            }\n");
		buf.append("                        });\n");
		buf.append("            } catch (IOException e) {}\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Files {\n");
		buf.append("    public static Stream<Object> walk(Object start) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    public static Stream<String> lines(Object path) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInLambda7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.InvalidClassException;\n");
		buf.append("interface C7 {\n");
		buf.append("    int foo(int i);\n");
		buf.append("    default C7 method1() {\n");
		buf.append("        return x -> {\n");
		buf.append("            try {\n");
		buf.append("                if (x == -1)\n");
		buf.append("                    throw new InvalidClassException(\"ex\");\n");
		buf.append("                if (x == 0)\n");
		buf.append("                    throw new FileNotFoundException();\n");
		buf.append("            } catch (InvalidClassException e) {\n");
		buf.append("            }\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C7.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.InvalidClassException;\n");
		buf.append("interface C7 {\n");
		buf.append("    int foo(int i);\n");
		buf.append("    default C7 method1() {\n");
		buf.append("        return x -> {\n");
		buf.append("            try {\n");
		buf.append("                if (x == -1)\n");
		buf.append("                    throw new InvalidClassException(\"ex\");\n");
		buf.append("                if (x == 0)\n");
		buf.append("                    throw new FileNotFoundException();\n");
		buf.append("            } catch (InvalidClassException e) {\n");
		buf.append("            } catch (FileNotFoundException e) {\n");
		buf.append("            }\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.io.InvalidClassException;\n");
		buf.append("interface C7 {\n");
		buf.append("    int foo(int i);\n");
		buf.append("    default C7 method1() {\n");
		buf.append("        return x -> {\n");
		buf.append("            try {\n");
		buf.append("                if (x == -1)\n");
		buf.append("                    throw new InvalidClassException(\"ex\");\n");
		buf.append("                if (x == 0)\n");
		buf.append("                    throw new FileNotFoundException();\n");
		buf.append("            } catch (InvalidClassException | FileNotFoundException e) {\n");
		buf.append("            }\n");
		buf.append("            return x;\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference1() throws Exception { // ExpressionMethodReference
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("public class E1 {\n");
		buf.append("    private static Transformer TRANSFORMER = new Transformer();\n");
		buf.append("    Consumer<? super String> mapper = TRANSFORMER::transform;\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Transformer {\n");
		buf.append("    void transform(String number) throws FileNotFoundException {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("public class E1 {\n");
		buf.append("    private static Transformer TRANSFORMER = new Transformer();\n");
		buf.append("    Consumer<? super String> mapper = arg0 -> {\n");
		buf.append("        try {\n");
		buf.append("            TRANSFORMER.transform(arg0);\n");
		buf.append("        } catch (FileNotFoundException e) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Transformer {\n");
		buf.append("    void transform(String number) throws FileNotFoundException {}\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static Transformer TRANSFORMER = new Transformer();\n");
		buf.append("    public void test() {\n");
		buf.append("        Optional.ofNullable(\"10\").map(TRANSFORMER::transform).ifPresent(System.out::print);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Transformer {\n");
		buf.append("    Long transform(String number) throws FileNotFoundException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("public class E2 {\n");
		buf.append("    private static Transformer TRANSFORMER = new Transformer();\n");
		buf.append("    public void test() {\n");
		buf.append("        Optional.ofNullable(\"10\").map(arg0 -> {\n");
		buf.append("            try {\n");
		buf.append("                return TRANSFORMER.transform(arg0);\n");
		buf.append("            } catch (FileNotFoundException e) {\n");
		buf.append("            }\n");
		buf.append("        }).ifPresent(System.out::print);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Transformer {\n");
		buf.append("    Long transform(String number) throws FileNotFoundException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("\n");
		buf.append("public class E3 {\n");
		buf.append("    private static Transformer TRANSFORMER = new Transformer();\n");
		buf.append("    public void test() {\n");
		buf.append("        Consumer<Object> s = (a) -> Optional.ofNullable(\"10\").map(TRANSFORMER::transform).ifPresent(System.out::print);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Transformer {\n");
		buf.append("    Long transform(String number) throws FileNotFoundException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E3.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("import java.util.Optional;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("\n");
		buf.append("public class E3 {\n");
		buf.append("    private static Transformer TRANSFORMER = new Transformer();\n");
		buf.append("    public void test() {\n");
		buf.append("        Consumer<Object> s = (a) -> Optional.ofNullable(\"10\").map(arg0 -> {\n");
		buf.append("            try {\n");
		buf.append("                return TRANSFORMER.transform(arg0);\n");
		buf.append("            } catch (FileNotFoundException e) {\n");
		buf.append("            }\n");
		buf.append("        }).ifPresent(System.out::print);\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Transformer {\n");
		buf.append("    Long transform(String number) throws FileNotFoundException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference4() throws Exception { // Generic lambda not allowed
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E4 {\n");
		buf.append("    {\n");
		buf.append("        FI fi = this::test;\n");
		buf.append("    }\n");
		buf.append("    private void test() throws IOException {}\n");
		buf.append("    interface FI {\n");
		buf.append("        <T> void foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E4.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, CorrectionMessages.LocalCorrectionsSubProcessor_surroundwith_trycatch_description);
	}

	@Test
	public void testUncaughtExceptionInMethodReference5() throws Exception { // CreationReference
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.HashSet;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E5 {\n");
		buf.append("    void test() {\n");
		buf.append("        Supplier<HashSet<String>> c = MyHashSet::new;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("@SuppressWarnings(\"serial\")\n");
		buf.append("class MyHashSet extends HashSet<String> {\n");
		buf.append("    public MyHashSet() throws IOException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E5.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.HashSet;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E5 {\n");
		buf.append("    void test() {\n");
		buf.append("        Supplier<HashSet<String>> c = () -> {\n");
		buf.append("            try {\n");
		buf.append("                return new MyHashSet();\n");
		buf.append("            } catch (IOException e) {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("@SuppressWarnings(\"serial\")\n");
		buf.append("class MyHashSet extends HashSet<String> {\n");
		buf.append("    public MyHashSet() throws IOException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference6() throws Exception { // TypeMethodReference
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E6 {\n");
		buf.append("    void test() {\n");
		buf.append("        Function<Clazz<Integer>, String> c = Clazz<Integer>::searchForRefs1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Clazz<E> {\n");
		buf.append("    <F> String searchForRefs1() throws IOException {\n");
		buf.append("        return \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E6.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.function.Function;\n");
		buf.append("public class E6 {\n");
		buf.append("    void test() {\n");
		buf.append("        Function<Clazz<Integer>, String> c = arg0 -> {\n");
		buf.append("            try {\n");
		buf.append("                return arg0.searchForRefs1();\n");
		buf.append("            } catch (IOException e) {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class Clazz<E> {\n");
		buf.append("    <F> String searchForRefs1() throws IOException {\n");
		buf.append("        return \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference7() throws Exception { // SuperMethodReference
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E7 extends Clazz<Object> {\n");
		buf.append("    Supplier<String> c = super::searchForRefs1;\n");
		buf.append("}\n");
		buf.append("class Clazz<E> {\n");
		buf.append("    <F> String searchForRefs1() throws IOException {\n");
		buf.append("        return \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E7.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.util.function.Supplier;\n");
		buf.append("public class E7 extends Clazz<Object> {\n");
		buf.append("    Supplier<String> c = () -> {\n");
		buf.append("        try {\n");
		buf.append("            return super.searchForRefs1();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");
		buf.append("class Clazz<E> {\n");
		buf.append("    <F> String searchForRefs1() throws IOException {\n");
		buf.append("        return \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testOverrideDefaultMethod1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public class E1 implements FI1, FI2 {\n");
		buf.append("\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public class E1 implements FI1, FI2 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public void foo(int j, String s) {\n");
		buf.append("        FI2.super.foo(j, s);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public class E1 implements FI1, FI2 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public void foo(int i, String s) {\n");
		buf.append("        FI1.super.foo(i, s);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOverrideDefaultMethod2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public interface E1 extends FI1, FI2 {\n");
		buf.append("    \n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public interface E1 extends FI1, FI2 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void foo(int j, String s) {\n");
		buf.append("        FI2.super.foo(j, s);\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public interface E1 extends FI1, FI2 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void foo(int i, String s) {\n");
		buf.append("        FI1.super.foo(i, s);\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOverrideDefaultMethod3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public enum E1 implements FI1, FI2 {\n");
		buf.append("    \n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public enum E1 implements FI1, FI2 {\n");
		buf.append("    ;\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public void foo(int j, String s) {\n");
		buf.append("        FI2.super.foo(j, s);\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface FI1 {\n");
		buf.append("    default void foo(int i, String s) {}\n");
		buf.append("}\n");
		buf.append("interface FI2 {\n");
		buf.append("    default void foo(int j, String s) {}\n");
		buf.append("}\n");
		buf.append("public enum E1 implements FI1, FI2 {\n");
		buf.append("    ;\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    public void foo(int i, String s) {\n");
		buf.append("        FI1.super.foo(i, s);\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOverrideDefaultMethod4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void m1() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    void m1();\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("I1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void m1() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    void m1();\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void m1() {\n");
		buf.append("        I1.super.m1();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void m1() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    void m1();\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void m1() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOverrideDefaultMethod_multiLevel() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("interface I1<T1> {\n");
		buf.append("    default void def(T1 t1) { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def(T2 t2) { }\n");
		buf.append("}\n");
		buf.append("interface I22<T22> extends I2<T22> { }\n");
		buf.append("interface Both extends I1<List<String>>, I22<List<String>> {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("I1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("interface I1<T1> {\n");
		buf.append("    default void def(T1 t1) { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def(T2 t2) { }\n");
		buf.append("}\n");
		buf.append("interface I22<T22> extends I2<T22> { }\n");
		buf.append("interface Both extends I1<List<String>>, I22<List<String>> {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void def(List<String> t2) {\n");
		buf.append("        I22.super.def(t2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("interface I1<T1> {\n");
		buf.append("    default void def(T1 t1) { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def(T2 t2) { }\n");
		buf.append("}\n");
		buf.append("interface I22<T22> extends I2<T22> { }\n");
		buf.append("interface Both extends I1<List<String>>, I22<List<String>> {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void def(List<String> t1) {\n");
		buf.append("        I1.super.def(t1);\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOverrideDefaultMethod_noParam() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("I1.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void def() {\n");
		buf.append("        I22.super.def();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[0]= buf.toString();

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface I1 {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I2<T2> {\n");
		buf.append("    default void def() { }\n");
		buf.append("}\n");
		buf.append("interface I22 extends I2<String> { }\n");
		buf.append("interface Both extends I1, I22 {\n");
		buf.append("\n");
		buf.append("    @Override\n");
		buf.append("    default void def() {\n");
		buf.append("        I1.super.def();\n");
		buf.append("    }\n");
		buf.append("}\n");
		expected[1]= buf.toString();

		assertExpectedExistInProposals(proposals, expected);
	}
	@Test
	public void testCorrectVariableType() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Collections;\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("\n");
		buf.append("    public void method() {\n");
		buf.append("        List<String> strings1 = strings().toArray(new String[0]);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public List<String> strings() {\n");
		buf.append("        return Collections.emptyList();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Collections;\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("public class Test {\n");
		buf.append("\n");
		buf.append("    public void method() {\n");
		buf.append("        String[] strings1 = strings().toArray(new String[0]);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public List<String> strings() {\n");
		buf.append("        return Collections.emptyList();\n");
		buf.append("    }\n");
		buf.append("}\n");

		String expected1= buf.toString();
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}
	@Test
	public void testBug528875() throws Exception {
		try {
			Hashtable<String, String> options= JavaCore.getOptions();
			options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
			JavaCore.setOptions(options);
			NullTestUtils.prepareNullTypeAnnotations(fSourceFolder);
			IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
			StringBuilder buf= new StringBuilder();
			buf.append("package pack;\n");
			buf.append("import java.util.*;\n");
			buf.append("import annots.*;\n");
			buf.append("@NonNullByDefault\n");
			buf.append("public class E {\n");
			buf.append("    private void foo() {\n");
			buf.append("        ArrayList x=new ArrayList<String>();\n");
			buf.append("    }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 6);

			buf= new StringBuilder();
			buf.append("package pack;\n");
			buf.append("import java.util.*;\n");
			buf.append("import annots.*;\n");
			buf.append("@NonNullByDefault\n");
			buf.append("public class E {\n");
			buf.append("    private void foo() {\n");
			buf.append("        ArrayList<String> x=new ArrayList<String>();\n");
			buf.append("    }\n");
			buf.append("}\n");

			assertProposalPreviewEquals(buf.toString(), "Change type to 'ArrayList<String>'", proposals);
		} finally {
			NullTestUtils.disableAnnotationBasedNullAnalysis(fSourceFolder);
		}
	}
}
