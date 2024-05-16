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
		String str= """
			package test1;
			import java.io.FileNotFoundException;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			public class E {
			    void test(int a) {
			        throw new @Marker FileNotFoundException();
			    }
			}
			
			@Target(ElementType.TYPE_USE)
			@interface Marker { }
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.io.FileNotFoundException;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			public class E {
			    void test(int a) throws @Marker FileNotFoundException {
			        throw new @Marker FileNotFoundException();
			    }
			}
			
			@Target(ElementType.TYPE_USE)
			@interface Marker { }
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUncaughtExceptionInLambda1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class C1 {
			    Runnable r = () -> info("Processing rule #{} {}", "");
			
			    private void info(String string, Object object) throws GridException1 {}
			}
			@SuppressWarnings("serial")
			class GridException1 extends Exception {}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected= """
			package test;
			public class C1 {
			    Runnable r = () -> {
			        try {
			            info("Processing rule #{} {}", "");
			        } catch (GridException1 e) {
			        }
			    };
			
			    private void info(String string, Object object) throws GridException1 {}
			}
			@SuppressWarnings("serial")
			class GridException1 extends Exception {}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testUncaughtExceptionInLambda2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class C2 {
			    void test() {
			        Runnable r = () -> info("Processing rule #{} {}", "");
			    }
			
			    private void info(String string, Object object) throws GridException2 {}
			}
			@SuppressWarnings("serial")
			class GridException2 extends Exception {}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C2.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			public class C2 {
			    void test() {
			        Runnable r = () -> {
			            try {
			                info("Processing rule #{} {}", "");
			            } catch (GridException2 e) {
			            }
			        };
			    }
			
			    private void info(String string, Object object) throws GridException2 {}
			}
			@SuppressWarnings("serial")
			class GridException2 extends Exception {}
			""";

		assertExpectedExistInProposals(proposals, expected);

		assertProposalDoesNotExist(proposals, CorrectionMessages.LocalCorrectionsSubProcessor_addthrows_description);
	}

	@Test
	public void testUncaughtExceptionInLambda3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.util.ArrayList;
			public class C3 {
			    void test(ArrayList<Integer> ruleIds) {
			        Runnable r = () -> {
			            for (int ruleId : ruleIds) {
			                info("Processing rule #{} {}", ruleId);
			            }
			        };
			    }
			
			    private void info(String string, Object object) throws GridException3 {}
			}
			@SuppressWarnings("serial")
			class GridException3 extends Exception {}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C3.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.util.ArrayList;
			public class C3 {
			    void test(ArrayList<Integer> ruleIds) {
			        Runnable r = () -> {
			            for (int ruleId : ruleIds) {
			                try {
			                    info("Processing rule #{} {}", ruleId);
			                } catch (GridException3 e) {
			                }
			            }
			        };
			    }
			
			    private void info(String string, Object object) throws GridException3 {}
			}
			@SuppressWarnings("serial")
			class GridException3 extends Exception {}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInLambda4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.File;
			import java.io.IOException;
			import java.util.stream.Stream;
			public class C4 {
			    void foo() {
			        try {
			            Files.walk(new File(".").toPath()).filter(p -> p.toString().endsWith(".java"))
			                    .forEach(p -> Files.lines(p).forEach(System.out::println));
			        } catch (IOException e) {}
			    }
			}
			class Files {
			    public static Stream<Object> walk(Object start) throws IOException {
			        return null;
			    }
			    public static Stream<String> lines(Object path) throws IOException {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C4.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.io.File;
			import java.io.IOException;
			import java.util.stream.Stream;
			public class C4 {
			    void foo() {
			        try {
			            Files.walk(new File(".").toPath()).filter(p -> p.toString().endsWith(".java"))
			                    .forEach(p -> {
			                        try {
			                            Files.lines(p).forEach(System.out::println);
			                        } catch (IOException e) {
			                        }
			                    });
			        } catch (IOException e) {}
			    }
			}
			class Files {
			    public static Stream<Object> walk(Object start) throws IOException {
			        return null;
			    }
			    public static Stream<String> lines(Object path) throws IOException {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInLambda5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.File;
			import java.io.IOException;
			import java.util.function.Consumer;
			import java.util.stream.Stream;
			
			public class C5 {
			    void foo() {
			        Consumer<Object> s = (a) -> Files.walk(new File(".").toPath())
			                .filter(p -> p.toString().endsWith(".java"))
			                .forEach(p -> {
			                    try {
			                        Files.lines(p).forEach(System.out::println);
			                    } catch (IOException e) {}
			                });
			    }
			}
			class Files {
			    public static Stream<Object> walk(Object start) throws IOException {
			        return null;
			    }
			    public static Stream<String> lines(Object path) throws IOException {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C5.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.io.File;
			import java.io.IOException;
			import java.util.function.Consumer;
			import java.util.stream.Stream;
			
			public class C5 {
			    void foo() {
			        Consumer<Object> s = (a) -> {
			            try {
			                Files.walk(new File(".").toPath())
			                        .filter(p -> p.toString().endsWith(".java"))
			                        .forEach(p -> {
			                            try {
			                                Files.lines(p).forEach(System.out::println);
			                            } catch (IOException e) {}
			                        });
			            } catch (IOException e) {
			            }
			        };
			    }
			}
			class Files {
			    public static Stream<Object> walk(Object start) throws IOException {
			        return null;
			    }
			    public static Stream<String> lines(Object path) throws IOException {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInLambda6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.File;
			import java.io.IOException;
			import java.util.function.Consumer;
			import java.util.stream.Stream;
			
			public class C5 {
			    void foo() {
			        Consumer<Object> s = (a) -> {
			            try {
			                Files.walk(new File(".").toPath())
			                        .filter(p -> p.toString().endsWith(".java"))
			                        .forEach(p -> Files.lines(p).forEach(System.out::println));
			            } catch (IOException e) {}
			        };
			    }
			}
			class Files {
			    public static Stream<Object> walk(Object start) throws IOException {
			        return null;
			    }
			    public static Stream<String> lines(Object path) throws IOException {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C5.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.io.File;
			import java.io.IOException;
			import java.util.function.Consumer;
			import java.util.stream.Stream;
			
			public class C5 {
			    void foo() {
			        Consumer<Object> s = (a) -> {
			            try {
			                Files.walk(new File(".").toPath())
			                        .filter(p -> p.toString().endsWith(".java"))
			                        .forEach(p -> {
			                            try {
			                                Files.lines(p).forEach(System.out::println);
			                            } catch (IOException e) {
			                            }
			                        });
			            } catch (IOException e) {}
			        };
			    }
			}
			class Files {
			    public static Stream<Object> walk(Object start) throws IOException {
			        return null;
			    }
			    public static Stream<String> lines(Object path) throws IOException {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInLambda7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.FileNotFoundException;
			import java.io.InvalidClassException;
			interface C7 {
			    int foo(int i);
			    default C7 method1() {
			        return x -> {
			            try {
			                if (x == -1)
			                    throw new InvalidClassException("ex");
			                if (x == 0)
			                    throw new FileNotFoundException();
			            } catch (InvalidClassException e) {
			            }
			            return x;
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C7.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test;
			import java.io.FileNotFoundException;
			import java.io.InvalidClassException;
			interface C7 {
			    int foo(int i);
			    default C7 method1() {
			        return x -> {
			            try {
			                if (x == -1)
			                    throw new InvalidClassException("ex");
			                if (x == 0)
			                    throw new FileNotFoundException();
			            } catch (InvalidClassException e) {
			            } catch (FileNotFoundException e) {
			            }
			            return x;
			        };
			    }
			}
			""";

		expected[1]= """
			package test;
			import java.io.FileNotFoundException;
			import java.io.InvalidClassException;
			interface C7 {
			    int foo(int i);
			    default C7 method1() {
			        return x -> {
			            try {
			                if (x == -1)
			                    throw new InvalidClassException("ex");
			                if (x == 0)
			                    throw new FileNotFoundException();
			            } catch (InvalidClassException | FileNotFoundException e) {
			            }
			            return x;
			        };
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference1() throws Exception { // ExpressionMethodReference
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.FileNotFoundException;
			import java.util.function.Consumer;
			public class E1 {
			    private static Transformer TRANSFORMER = new Transformer();
			    Consumer<? super String> mapper = TRANSFORMER::transform;
			}
			
			class Transformer {
			    void transform(String number) throws FileNotFoundException {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.io.FileNotFoundException;
			import java.util.function.Consumer;
			public class E1 {
			    private static Transformer TRANSFORMER = new Transformer();
			    Consumer<? super String> mapper = arg0 -> {
			        try {
			            TRANSFORMER.transform(arg0);
			        } catch (FileNotFoundException e) {
			        }
			    };
			}
			
			class Transformer {
			    void transform(String number) throws FileNotFoundException {}
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.FileNotFoundException;
			import java.util.Optional;
			public class E2 {
			    private static Transformer TRANSFORMER = new Transformer();
			    public void test() {
			        Optional.ofNullable("10").map(TRANSFORMER::transform).ifPresent(System.out::print);
			    }
			}
			class Transformer {
			    Long transform(String number) throws FileNotFoundException {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.io.FileNotFoundException;
			import java.util.Optional;
			public class E2 {
			    private static Transformer TRANSFORMER = new Transformer();
			    public void test() {
			        Optional.ofNullable("10").map(arg0 -> {
			            try {
			                return TRANSFORMER.transform(arg0);
			            } catch (FileNotFoundException e) {
			            }
			        }).ifPresent(System.out::print);
			    }
			}
			class Transformer {
			    Long transform(String number) throws FileNotFoundException {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.FileNotFoundException;
			import java.util.Optional;
			import java.util.function.Consumer;
			
			public class E3 {
			    private static Transformer TRANSFORMER = new Transformer();
			    public void test() {
			        Consumer<Object> s = (a) -> Optional.ofNullable("10").map(TRANSFORMER::transform).ifPresent(System.out::print);
			    }
			}
			class Transformer {
			    Long transform(String number) throws FileNotFoundException {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E3.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.io.FileNotFoundException;
			import java.util.Optional;
			import java.util.function.Consumer;
			
			public class E3 {
			    private static Transformer TRANSFORMER = new Transformer();
			    public void test() {
			        Consumer<Object> s = (a) -> Optional.ofNullable("10").map(arg0 -> {
			            try {
			                return TRANSFORMER.transform(arg0);
			            } catch (FileNotFoundException e) {
			            }
			        }).ifPresent(System.out::print);
			    }
			}
			class Transformer {
			    Long transform(String number) throws FileNotFoundException {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference4() throws Exception { // Generic lambda not allowed
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.IOException;
			public class E4 {
			    {
			        FI fi = this::test;
			    }
			    private void test() throws IOException {}
			    interface FI {
			        <T> void foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E4.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, CorrectionMessages.LocalCorrectionsSubProcessor_surroundwith_trycatch_description);
	}

	@Test
	public void testUncaughtExceptionInMethodReference5() throws Exception { // CreationReference
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.IOException;
			import java.util.HashSet;
			import java.util.function.Supplier;
			public class E5 {
			    void test() {
			        Supplier<HashSet<String>> c = MyHashSet::new;
			    }
			}
			@SuppressWarnings("serial")
			class MyHashSet extends HashSet<String> {
			    public MyHashSet() throws IOException {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E5.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.io.IOException;
			import java.util.HashSet;
			import java.util.function.Supplier;
			public class E5 {
			    void test() {
			        Supplier<HashSet<String>> c = () -> {
			            try {
			                return new MyHashSet();
			            } catch (IOException e) {
			            }
			        };
			    }
			}
			@SuppressWarnings("serial")
			class MyHashSet extends HashSet<String> {
			    public MyHashSet() throws IOException {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference6() throws Exception { // TypeMethodReference
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.IOException;
			import java.util.function.Function;
			public class E6 {
			    void test() {
			        Function<Clazz<Integer>, String> c = Clazz<Integer>::searchForRefs1;
			    }
			}
			class Clazz<E> {
			    <F> String searchForRefs1() throws IOException {
			        return "";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E6.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.io.IOException;
			import java.util.function.Function;
			public class E6 {
			    void test() {
			        Function<Clazz<Integer>, String> c = arg0 -> {
			            try {
			                return arg0.searchForRefs1();
			            } catch (IOException e) {
			            }
			        };
			    }
			}
			class Clazz<E> {
			    <F> String searchForRefs1() throws IOException {
			        return "";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testUncaughtExceptionInMethodReference7() throws Exception { // SuperMethodReference
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.IOException;
			import java.util.function.Supplier;
			public class E7 extends Clazz<Object> {
			    Supplier<String> c = super::searchForRefs1;
			}
			class Clazz<E> {
			    <F> String searchForRefs1() throws IOException {
			        return "";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E7.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package test;
			import java.io.IOException;
			import java.util.function.Supplier;
			public class E7 extends Clazz<Object> {
			    Supplier<String> c = () -> {
			        try {
			            return super.searchForRefs1();
			        } catch (IOException e) {
			        }
			    };
			}
			class Clazz<E> {
			    <F> String searchForRefs1() throws IOException {
			        return "";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	@Test
	public void testOverrideDefaultMethod1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			interface FI1 {
			    default void foo(int i, String s) {}
			}
			interface FI2 {
			    default void foo(int j, String s) {}
			}
			public class E1 implements FI1, FI2 {
			
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			
			interface FI1 {
			    default void foo(int i, String s) {}
			}
			interface FI2 {
			    default void foo(int j, String s) {}
			}
			public class E1 implements FI1, FI2 {
			
			    @Override
			    public void foo(int j, String s) {
			        FI2.super.foo(j, s);
			    }
			
			}
			""";

		expected[1]= """
			package test1;
			
			interface FI1 {
			    default void foo(int i, String s) {}
			}
			interface FI2 {
			    default void foo(int j, String s) {}
			}
			public class E1 implements FI1, FI2 {
			
			    @Override
			    public void foo(int i, String s) {
			        FI1.super.foo(i, s);
			    }
			
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOverrideDefaultMethod2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			interface FI1 {
			    default void foo(int i, String s) {}
			}
			interface FI2 {
			    default void foo(int j, String s) {}
			}
			public interface E1 extends FI1, FI2 {
			   \s
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			
			interface FI1 {
			    default void foo(int i, String s) {}
			}
			interface FI2 {
			    default void foo(int j, String s) {}
			}
			public interface E1 extends FI1, FI2 {
			
			    @Override
			    default void foo(int j, String s) {
			        FI2.super.foo(j, s);
			    }
			   \s
			}
			""";

		expected[1]= """
			package test1;
			
			interface FI1 {
			    default void foo(int i, String s) {}
			}
			interface FI2 {
			    default void foo(int j, String s) {}
			}
			public interface E1 extends FI1, FI2 {
			
			    @Override
			    default void foo(int i, String s) {
			        FI1.super.foo(i, s);
			    }
			   \s
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOverrideDefaultMethod3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			interface FI1 {
			    default void foo(int i, String s) {}
			}
			interface FI2 {
			    default void foo(int j, String s) {}
			}
			public enum E1 implements FI1, FI2 {
			   \s
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			
			interface FI1 {
			    default void foo(int i, String s) {}
			}
			interface FI2 {
			    default void foo(int j, String s) {}
			}
			public enum E1 implements FI1, FI2 {
			    ;
			
			    @Override
			    public void foo(int j, String s) {
			        FI2.super.foo(j, s);
			    }
			   \s
			}
			""";

		expected[1]= """
			package test1;
			
			interface FI1 {
			    default void foo(int i, String s) {}
			}
			interface FI2 {
			    default void foo(int j, String s) {}
			}
			public enum E1 implements FI1, FI2 {
			    ;
			
			    @Override
			    public void foo(int i, String s) {
			        FI1.super.foo(i, s);
			    }
			   \s
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOverrideDefaultMethod4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			interface I1 {
			    default void m1() { }
			}
			interface I2<T2> {
			    void m1();
			}
			interface I22 extends I2<String> { }
			interface Both extends I1, I22 {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			
			interface I1 {
			    default void m1() { }
			}
			interface I2<T2> {
			    void m1();
			}
			interface I22 extends I2<String> { }
			interface Both extends I1, I22 {
			
			    @Override
			    default void m1() {
			        I1.super.m1();
			    }
			}
			""";

		expected[1]= """
			package test1;
			
			interface I1 {
			    default void m1() { }
			}
			interface I2<T2> {
			    void m1();
			}
			interface I22 extends I2<String> { }
			interface Both extends I1, I22 {
			
			    @Override
			    default void m1() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testOverrideDefaultMethod_multiLevel() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			import java.util.List;
			
			interface I1<T1> {
			    default void def(T1 t1) { }
			}
			interface I2<T2> {
			    default void def(T2 t2) { }
			}
			interface I22<T22> extends I2<T22> { }
			interface Both extends I1<List<String>>, I22<List<String>> {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			
			import java.util.List;
			
			interface I1<T1> {
			    default void def(T1 t1) { }
			}
			interface I2<T2> {
			    default void def(T2 t2) { }
			}
			interface I22<T22> extends I2<T22> { }
			interface Both extends I1<List<String>>, I22<List<String>> {
			
			    @Override
			    default void def(List<String> t2) {
			        I22.super.def(t2);
			    }
			}
			""";

		expected[1]= """
			package test1;
			
			import java.util.List;
			
			interface I1<T1> {
			    default void def(T1 t1) { }
			}
			interface I2<T2> {
			    default void def(T2 t2) { }
			}
			interface I22<T22> extends I2<T22> { }
			interface Both extends I1<List<String>>, I22<List<String>> {
			
			    @Override
			    default void def(List<String> t1) {
			        I1.super.def(t1);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testCreateMethodInSuperTypeQuickFix1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E implements I {
			    @Override
			    void foo(int... i) {
			    }
			}
			interface I {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			class E implements I {
			    @Override
			    void foo(int... i) {
			    }
			}
			interface I {
			
			    void foo(int... i);
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodInSuperTypeQuickFix2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E implements I {
			    @Override
			    void foo(int[]... i) {
			    }
			}
			interface I {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			class E implements I {
			    @Override
			    void foo(int[]... i) {
			    }
			}
			interface I {
			
			    void foo(int[]... i);
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodInSuperTypeQuickFix3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E implements I {
			    @Override
			    void foo(double d, int[]... i) {
			    }
			}
			interface I {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			class E implements I {
			    @Override
			    void foo(double d, int[]... i) {
			    }
			}
			interface I {
			
			    void foo(double d, int[]... i);
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodInSuperTypeQuickFix4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E implements I {
			    @Override
			    <T> void foo(T t) {
			    }
			}
			interface I {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			class E implements I {
			    @Override
			    <T> void foo(T t) {
			    }
			}
			interface I {
			
			    <T> void foo(T t);
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodInSuperTypeQuickFix5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E implements I {
			    @Override
			    <T> void foo(T... t) {
			    }
			}
			interface I {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			class E implements I {
			    @Override
			    <T> void foo(T... t) {
			    }
			}
			interface I {
			
			    <T> void foo(T... t);
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testCreateMethodInSuperTypeQuickFix6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E implements I {
			    @Override
			    <T> void foo(T[]... t) {
			    }
			}
			interface I {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			class E implements I {
			    @Override
			    <T> void foo(T[]... t) {
			    }
			}
			interface I {
			
			    <T> void foo(T[]... t);
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testOverrideDefaultMethod_noParam() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			interface I1 {
			    default void def() { }
			}
			interface I2<T2> {
			    default void def() { }
			}
			interface I22 extends I2<String> { }
			interface Both extends I1, I22 {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I1.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			
			interface I1 {
			    default void def() { }
			}
			interface I2<T2> {
			    default void def() { }
			}
			interface I22 extends I2<String> { }
			interface Both extends I1, I22 {
			
			    @Override
			    default void def() {
			        I22.super.def();
			    }
			}
			""";

		expected[1]= """
			package test1;
			
			interface I1 {
			    default void def() { }
			}
			interface I2<T2> {
			    default void def() { }
			}
			interface I22 extends I2<String> { }
			interface Both extends I1, I22 {
			
			    @Override
			    default void def() {
			        I1.super.def();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}
	@Test
	public void testCorrectVariableType() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collections;
			import java.util.List;
			
			public class Test {
			
			    public void method() {
			        List<String> strings1 = strings().toArray(new String[0]);
			    }
			
			    public List<String> strings() {
			        return Collections.emptyList();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Collections;
			import java.util.List;
			
			public class Test {
			
			    public void method() {
			        String[] strings1 = strings().toArray(new String[0]);
			    }
			
			    public List<String> strings() {
			        return Collections.emptyList();
			    }
			}
			""";
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
			String str= """
				package pack;
				import java.util.*;
				import annots.*;
				@NonNullByDefault
				public class E {
				    private void foo() {
				        ArrayList x=new ArrayList<String>();
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 6);

			String str1= """
				package pack;
				import java.util.*;
				import annots.*;
				@NonNullByDefault
				public class E {
				    private void foo() {
				        ArrayList<String> x=new ArrayList<String>();
				    }
				}
				""";
			assertProposalPreviewEquals(str1, "Change type to 'ArrayList<String>'", proposals);
		} finally {
			NullTestUtils.disableAnnotationBasedNullAnalysis(fSourceFolder);
		}
	}
}
