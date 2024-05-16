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
		String sample= """
			package test1;
			
			import java.util.function.Predicate
			
			public class E {
			    public void foo() {
			        Predicate<String> cc = (String s) -> { return s.length() > 0; };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			import java.util.function.Predicate
			
			public class E {
			    public void foo() {
			        Predicate<String> cc = (var s) -> { return s.length() > 0; };
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceInLambda2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    private interface I1 {
			        public void run(String s, int i, Boolean b);
			    }
			    public void foo(int doNotRefactorParameter) {
			        I1 i1 = (String s, int i, Boolean b) -> { System.out.println("hello"); };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			public class E1 {
			    private interface I1 {
			        public void run(String s, int i, Boolean b);
			    }
			    public void foo(int doNotRefactorParameter) {
			        I1 i1 = (var s, var i, var b) -> { System.out.println("hello"); };
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceInParamCallWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.function.Function;
			
			public class E1 {
			    public void foo() {
			        debug((String a) -> a.length());
			    }
			
			    private void debug(Function<String, Object> function) {
			        System.out.println(function);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			import java.util.function.Function;
			
			public class E1 {
			    public void foo() {
			        debug((var a) -> a.length());
			    }
			
			    private void debug(Function<String, Object> function) {
			        System.out.println(function);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseLocalVariableTypeInferenceInWildCardParamCallWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.function.Function;
			
			public class E1 {
			    public void foo() {
			        debug((String a) -> a.length());
			    }
			
			    private void debug(Function<?, ?> function) {
			        System.out.println(function);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseLocalVariableTypeInferenceInWildCardConstructorWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.function.Function;
			
			public class E1 {
			    public static void main(String[] args) {
			        new E1((String a) -> a.length());
			    }
			
			    public E1(Function<?, ?> function) {
			        System.out.println(function);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseLocalVariableTypeInferenceInWildCardSuperCallsWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.function.Function;
			
			public class E1 {
			    public E1(Function<?, ?> function) {
			        System.out.println(function);
			    }
			
			    public void method(Function<?, ?> function) {
			        System.out.println(function);
			    }
			
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		String sample2= """
			package test1;
			
			import java.util.function.Function;
			
			public class E2 extends E1 {
			    public E2(Function<?, ?> function) {
			        super((String a) -> a.length());
			    }
			
			    public void method(Function<?, ?> function) {
			        super.method((String a) -> a.length());
			    }
			
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample2, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1, cu2 });
	}

	@Test
	public void testUseLocalVariableTypeInferenceInParamTypeDeclarationWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.function.Predicate;
			
			public class E1 {
			    public void foo() {
			        Predicate<String> cc = (String s) -> (s.length() > 0);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			import java.util.function.Predicate;
			
			public class E1 {
			    public void foo() {
			        Predicate<String> cc = (var s) -> (s.length() > 0);
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseLocalVariableTypeInferenceInWildCardParamDeclarationWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.function.Predicate;
			
			public class E1 {
			    public void foo() {
			        Predicate<?> cc = (String s) -> (s.length() > 0);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseLocalVariableTypeInferenceInParamFieldDeclarationWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.function.Predicate;
			
			public class E1 {
			    public Predicate<String> cc = (String s) -> (s.length() > 0);
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			import java.util.function.Predicate;
			
			public class E1 {
			    public Predicate<String> cc = (var s) -> (s.length() > 0);
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseLocalVariableTypeInferenceInWildCardParamFieldDeclarationWithLambda() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=570058
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.function.Predicate;
			
			public class E1 {
			    public Predicate<?> cc = (String s) -> (s.length() > 0);
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}


	@Test
	public void testUseStringIsBlank() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			import java.util.List;
			
			public class E {
			    private static final int ZERO = 0;
			    private static final int THREE = 3;
			    private static final String EMPTY_STRING = "";
			
			    void isBlank(String text) {
			        if (text.strip().isEmpty()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.stripLeading().isEmpty()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.stripTrailing().isEmpty()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.strip().length() == 0) {
			            System.err.println("The text must not be blank");
			        } else if (text.strip().length() <= 0) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.strip().length() < 1) {
			            System.err.println("This text must not be blank");
			        }
			        if (0 == text.strip().length()) {
			            System.err.println("Text must not be blank");
			        } else if (0 >= text.strip().length()) {
			            System.err.println("Text must not be blank");
			        }
			        if (1 > text.strip().length()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.strip().length() == ZERO) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.strip().equals("")) {
			            System.err.println("Text must not be blank");
			        }
			        if ("".equals(text.stripLeading())) {
			            System.err.println("Text must not be blank");
			        }
			        if (EMPTY_STRING.equals(text.stripTrailing())) {
			            System.err.println("Text must not be blank");
			        }
			    }
			
			    void isNotBlank(String text, StringBuilder builder) {
			        if (!text.strip().isEmpty()) {
			            System.out.println(text)
			        }
			        if (text.strip().length() != 0) {
			            System.out.println(text)
			        } else if (text.strip().length() > 0) {
			            System.out.println(text)
			        }
			        if (text.strip().length() >= 1) {
			            System.out.println(text)
			        }
			        if (0 != text.strip().length()) {
			            System.out.println(text)
			        } else if (0 < text.strip().length()) {
			            System.out.println(text)
			        }
			        if (1 <= text.strip().length()) {
			            System.out.println(text)
			        }
			        if (4 - THREE <= builder.toString().strip().length()) {
			            System.out.println(text)
			        }
			    }
			
			    void printList(List<String> list) {
			        list.stream().filter(s -> !s.strip().isEmpty()).map(String::strip);
			        list.stream().filter(s -> s.strip().length() != 0).map(String::strip);
			    }
			}
			""";

		String expected= """
			package test1;
			
			import java.util.List;
			
			public class E {
			    private static final int ZERO = 0;
			    private static final int THREE = 3;
			    private static final String EMPTY_STRING = "";
			
			    void isBlank(String text) {
			        if (text.isBlank()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.isBlank()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.isBlank()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.isBlank()) {
			            System.err.println("The text must not be blank");
			        } else if (text.isBlank()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.isBlank()) {
			            System.err.println("This text must not be blank");
			        }
			        if (text.isBlank()) {
			            System.err.println("Text must not be blank");
			        } else if (text.isBlank()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.isBlank()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.isBlank()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.isBlank()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.isBlank()) {
			            System.err.println("Text must not be blank");
			        }
			        if (text.isBlank()) {
			            System.err.println("Text must not be blank");
			        }
			    }
			
			    void isNotBlank(String text, StringBuilder builder) {
			        if (!text.isBlank()) {
			            System.out.println(text)
			        }
			        if (!text.isBlank()) {
			            System.out.println(text)
			        } else if (!text.isBlank()) {
			            System.out.println(text)
			        }
			        if (!text.isBlank()) {
			            System.out.println(text)
			        }
			        if (!text.isBlank()) {
			            System.out.println(text)
			        } else if (!text.isBlank()) {
			            System.out.println(text)
			        }
			        if (!text.isBlank()) {
			            System.out.println(text)
			        }
			        if (!builder.toString().isBlank()) {
			            System.out.println(text)
			        }
			    }
			
			    void printList(List<String> list) {
			        list.stream().filter(s -> !s.isBlank()).map(String::strip);
			        list.stream().filter(s -> !s.isBlank()).map(String::strip);
			    }
			}
			""";

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
		String sample= """
			package test1;
			
			public class NotAString {
			    int mostlyZero= 0;
			    private static int NON_FINAL_ZERO = 0;
			
			    public String strip() {
			        return "";
			    }
			
			    void doNotUseStringIsBlank(NotAString noString, String text) {
			        if (noString.strip().length() == 0) {
			            System.err.println("Text must not be blank");
			        }
			
			        if (text.strip().length() == mostlyZero) {
			            System.err.println("Text must not be blank");
			        } else if (text.strip().length() <= NON_FINAL_ZERO) {
			            System.err.println("Text must not be blank");
			        }
			    }
			
			    void doNotUseStringIsBlankWithUnknownString(String text, String emptyString) {
			        if (text.strip().equals(emptyString)) {
			            System.err.println("Text must not be blank");
			        }
			
			        if (emptyString.equals(text.strip())) {
			            System.err.println("Text must not be blank");
			        }
			    }
			
			    void bug_573831(String text) {
			        if (equals(text.strip())) {
			            System.err.println("Applying the cleanup should not cause NPE");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("NotAString.java", sample, false, null);

		enable(CleanUpConstants.USE_STRING_IS_BLANK);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}
