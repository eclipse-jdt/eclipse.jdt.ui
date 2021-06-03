/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.tests.core.rules.Java11ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 11.
 */
public class CleanUpTest11 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java11ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testUseLocalVariableTypeInferenceInLambda1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Predicate\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        Predicate<String> cc = (String s) -> { return s.length() > 0; };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Predicate\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        Predicate<String> cc = (var s) -> { return s.length() > 0; };\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceInLambda2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private interface I1 {\n" //
				+ "        public void run(String s, int i, Boolean b);\n" //
				+ "    }\n" //
				+ "    public void foo(int doNotRefactorParameter) {\n" //
				+ "        I1 i1 = (String s, int i, Boolean b) -> { System.out.println(\"hello\"); };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private interface I1 {\n" //
				+ "        public void run(String s, int i, Boolean b);\n" //
				+ "    }\n" //
				+ "    public void foo(int doNotRefactorParameter) {\n" //
				+ "        I1 i1 = (var s, var i, var b) -> { System.out.println(\"hello\"); };\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceInParamCallWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        debug((String a) -> a.length());\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private void debug(Function<String, Object> function) {\n" //
				+ "        System.out.println(function);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        debug((var a) -> a.length());\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private void debug(Function<String, Object> function) {\n" //
				+ "        System.out.println(function);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseLocalVariableTypeInferenceInWildCardParamCallWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        debug((String a) -> a.length());\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    private void debug(Function<?, ?> function) {\n" //
				+ "        System.out.println(function);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseLocalVariableTypeInferenceInWildCardConstructorWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public static void main(String[] args) {\n" //
				+ "        new E1((String a) -> a.length());\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public E1(Function<?, ?> function) {\n" //
				+ "        System.out.println(function);\n" //
    			+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseLocalVariableTypeInferenceInWildCardSuperCallsWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public E1(Function<?, ?> function) {\n" //
				+ "        System.out.println(function);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void method(Function<?, ?> function) {\n" //
				+ "        System.out.println(function);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		String sample2= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
				+ "public class E2 extends E1 {\n" //
				+ "    public E2(Function<?, ?> function) {\n" //
				+ "        super((String a) -> a.length());\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void method(Function<?, ?> function) {\n" //
				+ "        super.method((String a) -> a.length());\n" //
				+ "    }\n" //
				+ "\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample2, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1, cu2 });
	}

	@Test
	public void testUseLocalVariableTypeInferenceInParamTypeDeclarationWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Predicate;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        Predicate<String> cc = (String s) -> (s.length() > 0);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Predicate;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        Predicate<String> cc = (var s) -> (s.length() > 0);\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseLocalVariableTypeInferenceInWildCardParamDeclarationWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Predicate;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        Predicate<?> cc = (String s) -> (s.length() > 0);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseLocalVariableTypeInferenceInParamFieldDeclarationWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Predicate;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public Predicate<String> cc = (String s) -> (s.length() > 0);\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Predicate;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public Predicate<String> cc = (var s) -> (s.length() > 0);\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseLocalVariableTypeInferenceInWildCardParamFieldDeclarationWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Predicate;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public Predicate<?> cc = (String s) -> (s.length() > 0);\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}


	@Test
	public void testUseStringIsBlank() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private static final int ZERO = 0;\n" //
				+ "    private static final int THREE = 3;\n" //
				+ "    private static final String EMPTY_STRING = \"\";\n" //
				+ "\n" //
				+ "    void isBlank(String text) {\n" //
				+ "        if (text.strip().isEmpty()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.stripLeading().isEmpty()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.stripTrailing().isEmpty()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.strip().length() == 0) {\n" //
				+ "            System.err.println(\"The text must not be blank\");\n" //
				+ "        } else if (text.strip().length() <= 0) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.strip().length() < 1) {\n" //
				+ "            System.err.println(\"This text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (0 == text.strip().length()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        } else if (0 >= text.strip().length()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (1 > text.strip().length()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.strip().length() == ZERO) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.strip().equals(\"\")) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (\"\".equals(text.stripLeading())) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (EMPTY_STRING.equals(text.stripTrailing())) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void isNotBlank(String text, StringBuilder builder) {\n" //
				+ "        if (!text.strip().isEmpty()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "        if (text.strip().length() != 0) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        } else if (text.strip().length() > 0) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "        if (text.strip().length() >= 1) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "        if (0 != text.strip().length()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        } else if (0 < text.strip().length()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "        if (1 <= text.strip().length()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "        if (4 - THREE <= builder.toString().strip().length()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void printList(List<String> list) {\n" //
				+ "        list.stream().filter(s -> !s.strip().isEmpty()).map(String::strip);\n" //
				+ "        list.stream().filter(s -> s.strip().length() != 0).map(String::strip);\n" //
				+ "    }\n" //
				+ "}\n";

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    private static final int ZERO = 0;\n" //
				+ "    private static final int THREE = 3;\n" //
				+ "    private static final String EMPTY_STRING = \"\";\n" //
				+ "\n" //
				+ "    void isBlank(String text) {\n" //
				+ "        if (text.isBlank()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.isBlank()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.isBlank()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.isBlank()) {\n" //
				+ "            System.err.println(\"The text must not be blank\");\n" //
				+ "        } else if (text.isBlank()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.isBlank()) {\n" //
				+ "            System.err.println(\"This text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.isBlank()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        } else if (text.isBlank()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.isBlank()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.isBlank()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.isBlank()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.isBlank()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "        if (text.isBlank()) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void isNotBlank(String text, StringBuilder builder) {\n" //
				+ "        if (!text.isBlank()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "        if (!text.isBlank()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        } else if (!text.isBlank()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "        if (!text.isBlank()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "        if (!text.isBlank()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        } else if (!text.isBlank()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "        if (!text.isBlank()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "        if (!builder.toString().isBlank()) {\n" //
				+ "            System.out.println(text)\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void printList(List<String> list) {\n" //
				+ "        list.stream().filter(s -> !s.isBlank()).map(String::strip);\n" //
				+ "        list.stream().filter(s -> !s.isBlank()).map(String::strip);\n" //
				+ "    }\n" //
				+ "}\n";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.USE_STRING_IS_BLANK);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, new HashSet<>(Arrays.asList(FixMessages.UseStringIsBlankCleanUp_description)));
	}

	@Test
	public void testDoNotUseStringIsBlank() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class NotAString {\n" //
				+ "    int mostlyZero= 0;\n" //
				+ "    private static int NON_FINAL_ZERO = 0;\n" //
				+ "\n" //
				+ "    public String strip() {\n" //
				+ "        return \"\";\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void doNotUseStringIsBlank(NotAString noString, String text) {\n" //
				+ "        if (noString.strip().length() == 0) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        if (text.strip().length() == mostlyZero) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        } else if (text.strip().length() <= NON_FINAL_ZERO) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void doNotUseStringIsBlankWithUnknownString(String text, String emptyString) {\n" //
				+ "        if (text.strip().equals(emptyString)) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        if (emptyString.equals(text.strip())) {\n" //
				+ "            System.err.println(\"Text must not be blank\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    void bug_573831(String text) {\n" //
				+ "        if (equals(text.strip())) {\n" //
				+ "            System.err.println(\"Applying the cleanup should not cause NPE\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("NotAString.java", sample, false, null);

		enable(CleanUpConstants.USE_STRING_IS_BLANK);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}
