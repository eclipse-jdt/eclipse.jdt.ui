/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

@RunWith(JUnit4.class)
public class CleanUpTest1d8 extends CleanUpTestCase {
	@Rule
    public ProjectTestSetup projectsetup= new Java1d8ProjectTestSetup();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "", null);
	}

	@Override
	protected IJavaProject getProject() {
		return Java1d8ProjectTestSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return Java1d8ProjectTestSetup.getDefaultClasspath();
	}

	@Test
	public void testConvertToLambdaAndQualifyNextField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= ""
				+ "package test;\n"
				+ "\n"
				+ "public class C1 {\n"
				+ "    static final String previousField = \"abc\";\n"
				+ "\n"
				+ "    Runnable run1 = new Runnable() {\n"
				+ "        @Override\n"
				+ "        public void run() {\n"
				+ "            System.out.println(previousField + instanceField + classField + getString());\n"
				+ "        }\n"
				+ "    };\n"
				+ "\n"
				+ "    static final String classField = \"abc\";\n"
				+ "    final String instanceField = \"abc\";\n"
				+ "    public String getString() {\n"
				+ "        return \"\";\n"
				+ "    }\n"
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		String expected= ""
				+ "package test;\n"
				+ "\n"
				+ "public class C1 {\n"
				+ "    static final String previousField = \"abc\";\n"
				+ "\n"
				+ "    Runnable run1 = () -> System.out.println(previousField + this.instanceField + C1.classField + getString());\n"
				+ "\n"
				+ "    static final String classField = \"abc\";\n"
				+ "    final String instanceField = \"abc\";\n"
				+ "    public String getString() {\n"
				+ "        return \"\";\n"
				+ "    }\n"
				+ "}\n";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected });
	}

	@Test
	public void testConvertToLambdaWithQualifiedField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= ""
				+ "package test;\n"
				+ "\n"
				+ "public class C1 {\n"
				+ "    static final String previousField = \"abc\";\n"
				+ "\n"
				+ "    Runnable run1 = new Runnable() {\n"
				+ "        @Override\n"
				+ "        public void run() {\n"
				+ "            System.out.println(C1.previousField + C1.this.instanceField + C1.classField + C1.this.getString());\n"
				+ "        }\n"
				+ "    };\n"
				+ "\n"
				+ "    static final String classField = \"def\";\n"
				+ "    final String instanceField = \"abc\";\n"
				+ "    public String getString() {\n"
				+ "        return \"\";\n"
				+ "    }\n"
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		String expected= ""
				+ "package test;\n"
				+ "\n"
				+ "public class C1 {\n"
				+ "    static final String previousField = \"abc\";\n"
				+ "\n"
				+ "    Runnable run1 = () -> System.out.println(C1.previousField + C1.this.instanceField + C1.classField + C1.this.getString());\n"
				+ "\n"
				+ "    static final String classField = \"def\";\n"
				+ "    final String instanceField = \"abc\";\n"
				+ "    public String getString() {\n"
				+ "        return \"\";\n"
				+ "    }\n"
				+ "}\n";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected });
	}

	@Test
	public void testDoNotRefactorWithExpressions() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.function.Supplier;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public Supplier<Date> doNotRefactorWithAnonymousBody() {\n" //
				+ "        return () -> new Date() {\n" //
				+ "            @Override\n" //
				+ "            public String toString() {\n" //
				+ "                return \"foo\";\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testSimplifyLambdaExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import static java.util.Calendar.getInstance;\n" //
				+ "import static java.util.Calendar.getAvailableLocales;\n" //
				+ "\n" //
				+ "import java.time.Instant;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.Calendar;\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.Locale;\n" //
				+ "import java.util.Vector;\n" //
				+ "import java.util.function.BiFunction;\n" //
				+ "import java.util.function.Function;\n" //
				+ "import java.util.function.Supplier;\n" //
				+ "\n" //
				+ "public class E extends Date {\n" //
				+ "    public String changeableText = \"foo\";\n" //
				+ "\n" //
				+ "    public Function<String, String> removeParentheses() {\n" //
				+ "        return (someString) -> someString.trim().toLowerCase();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> doNotRemoveParenthesesWithSingleVariableDeclaration() {\n" //
				+ "        return (String someString) -> someString.trim().toLowerCase();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<String, String, Integer> doNotRemoveParenthesesWithTwoParameters() {\n" //
				+ "        return (someString, anotherString) -> someString.trim().compareTo(anotherString.trim());\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Supplier<Boolean> doNotRemoveParenthesesWithNoParameter() {\n" //
				+ "        return () -> {System.out.println(\"foo\");return true;};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> removeReturnAndBrackets() {\n" //
				+ "        return someString -> {return someString.trim().toLowerCase();};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> removeReturnAndBracketsWithParentheses() {\n" //
				+ "        return someString -> {return someString.trim().toLowerCase() + \"bar\";};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> doNotRemoveReturnWithSeveralStatements() {\n" //
				+ "        return someString -> {String trimmed = someString.trim();\n" //
				+ "        return trimmed.toLowerCase();};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Supplier<ArrayList<String>> useCreationReference() {\n" //
				+ "        return () -> new ArrayList<>();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameter() {\n" //
				+ "        return capacity -> new ArrayList<>(capacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameterAndType() {\n" //
				+ "        // TODO this can be refactored like useCreationReferenceWithParameter\n" //
				+ "        return (Integer capacity) -> new ArrayList<>(capacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> doNotRefactorWithExpressions() {\n" //
				+ "        return capacity -> new ArrayList<>(capacity + 1);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Integer, Integer, Vector<String>> useCreationReferenceWithParameters() {\n" //
				+ "        return (initialCapacity, capacityIncrement) -> new Vector<>(initialCapacity, capacityIncrement);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Integer, Integer, Vector<String>> doNotRefactorShuffledParams() {\n" //
				+ "        return (initialCapacity, capacityIncrement) -> new Vector<>(capacityIncrement, initialCapacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Long> useMethodReference() {\n" //
				+ "        return date -> date.getTime();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Date, Date, Integer> useMethodReferenceWithParameter() {\n" //
				+ "        return (date, anotherDate) -> date.compareTo(anotherDate);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Long> useTypeReference() {\n" //
				+ "        return numberInText -> Long.getLong(numberInText);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<Instant, Date> useTypeReferenceOnClassMethod() {\n" //
				+ "        return instant -> Date.from(instant);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<Locale, Calendar> useTypeReferenceOnImportedMethod() {\n" //
				+ "        return locale -> Calendar.getInstance(locale);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Supplier<Locale[]> useTypeReferenceAsSupplier() {\n" //
				+ "        return () -> Calendar.getAvailableLocales();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Integer> useExpressionMethodReferenceOnLiteral() {\n" //
				+ "        return textToSearch -> \"AutoRefactor\".indexOf(textToSearch);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Integer> doNotUseExpressionMethodReferenceOnVariable() {\n" //
				+ "        return textToSearch -> this.changeableText.indexOf(textToSearch);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useThisMethodReference() {\n" //
				+ "        return anotherDate -> compareTo(anotherDate);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public class InnerClass {\n" //
				+ "        public Function<Date, Integer> doNotUseThisMethodReferenceOnTopLevelClassMethod() {\n" //
				+ "            return anotherDate -> compareTo(anotherDate);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useThisMethodReferenceAddThis() {\n" //
				+ "        return anotherDate -> this.compareTo(anotherDate);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useSuperMethodReference() {\n" //
				+ "        return anotherDate -> super.compareTo(anotherDate);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, String> doNotUseConflictingMethodReference() {\n" //
				+ "        return numberToPrint -> numberToPrint.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, String> doNotUseConflictingStaticMethodReference() {\n" //
				+ "        return numberToPrint -> Integer.toString(numberToPrint);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import static java.util.Calendar.getInstance;\n" //
				+ "import static java.util.Calendar.getAvailableLocales;\n" //
				+ "\n" //
				+ "import java.time.Instant;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.Calendar;\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.Locale;\n" //
				+ "import java.util.Vector;\n" //
				+ "import java.util.function.BiFunction;\n" //
				+ "import java.util.function.Function;\n" //
				+ "import java.util.function.Supplier;\n" //
				+ "\n" //
				+ "public class E extends Date {\n" //
				+ "    public String changeableText = \"foo\";\n" //
				+ "\n" //
				+ "    public Function<String, String> removeParentheses() {\n" //
				+ "        return someString -> someString.trim().toLowerCase();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> doNotRemoveParenthesesWithSingleVariableDeclaration() {\n" //
				+ "        return (String someString) -> someString.trim().toLowerCase();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<String, String, Integer> doNotRemoveParenthesesWithTwoParameters() {\n" //
				+ "        return (someString, anotherString) -> someString.trim().compareTo(anotherString.trim());\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Supplier<Boolean> doNotRemoveParenthesesWithNoParameter() {\n" //
				+ "        return () -> {System.out.println(\"foo\");return true;};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> removeReturnAndBrackets() {\n" //
				+ "        return someString -> someString.trim().toLowerCase();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> removeReturnAndBracketsWithParentheses() {\n" //
				+ "        return someString -> (someString.trim().toLowerCase() + \"bar\");\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, String> doNotRemoveReturnWithSeveralStatements() {\n" //
				+ "        return someString -> {String trimmed = someString.trim();\n" //
				+ "        return trimmed.toLowerCase();};\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Supplier<ArrayList<String>> useCreationReference() {\n" //
				+ "        return ArrayList::new;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameter() {\n" //
				+ "        return ArrayList::new;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameterAndType() {\n" //
				+ "        // TODO this can be refactored like useCreationReferenceWithParameter\n" //
				+ "        return (Integer capacity) -> new ArrayList<>(capacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, ArrayList<String>> doNotRefactorWithExpressions() {\n" //
				+ "        return capacity -> new ArrayList<>(capacity + 1);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Integer, Integer, Vector<String>> useCreationReferenceWithParameters() {\n" //
				+ "        return Vector::new;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Integer, Integer, Vector<String>> doNotRefactorShuffledParams() {\n" //
				+ "        return (initialCapacity, capacityIncrement) -> new Vector<>(capacityIncrement, initialCapacity);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Long> useMethodReference() {\n" //
				+ "        return Date::getTime;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public BiFunction<Date, Date, Integer> useMethodReferenceWithParameter() {\n" //
				+ "        return Date::compareTo;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Long> useTypeReference() {\n" //
				+ "        return Long::getLong;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<Instant, Date> useTypeReferenceOnClassMethod() {\n" //
				+ "        return instant -> Date.from(instant);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Function<Locale, Calendar> useTypeReferenceOnImportedMethod() {\n" //
				+ "        return Calendar::getInstance;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public static Supplier<Locale[]> useTypeReferenceAsSupplier() {\n" //
				+ "        return Calendar::getAvailableLocales;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Integer> useExpressionMethodReferenceOnLiteral() {\n" //
				+ "        return \"AutoRefactor\"::indexOf;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<String, Integer> doNotUseExpressionMethodReferenceOnVariable() {\n" //
				+ "        return textToSearch -> this.changeableText.indexOf(textToSearch);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useThisMethodReference() {\n" //
				+ "        return this::compareTo;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public class InnerClass {\n" //
				+ "        public Function<Date, Integer> doNotUseThisMethodReferenceOnTopLevelClassMethod() {\n" //
				+ "            return anotherDate -> compareTo(anotherDate);\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useThisMethodReferenceAddThis() {\n" //
				+ "        return this::compareTo;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Date, Integer> useSuperMethodReference() {\n" //
				+ "        return super::compareTo;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, String> doNotUseConflictingMethodReference() {\n" //
				+ "        return numberToPrint -> numberToPrint.toString();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public Function<Integer, String> doNotUseConflictingStaticMethodReference() {\n" //
				+ "        return numberToPrint -> Integer.toString(numberToPrint);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambdaWithRecursion() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= ""
				+ "import java.util.function.Function;\n" //
				+ "public class C1 {\n" //
				+ "\n" //
				+ "    public interface I1 {\n" //
				+ "        public int add(int a);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    I1 k = new I1() {\n" //
				+ "        @Override\n" //
				+ "        public int add(int a) {\n" //
				+ "            if (a == 2) {\n" //
				+ "                return add(3);\n" //
				+ "            }\n" //
				+ "            return a + 7;\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "\n" //
				+ "    public static I1 j = new I1() {\n" //
				+ "        @Override\n" //
				+ "        public int add(int a) {\n" //
				+ "            if (a == 2) {\n" //
				+ "                return add(4);\n" //
				+ "            }\n" //
				+ "            return a + 8;\n" //
				+ "        }\n" //
				+ "    };\n" //
				+ "}\n"; //
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		String expected= ""
				+ "import java.util.function.Function;\n" //
				+ "public class C1 {\n" //
				+ "\n" //
				+ "    public interface I1 {\n" //
				+ "        public int add(int a);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    I1 k = a -> {\n" //
				+ "        if (a == 2) {\n" //
				+ "            return this.k.add(3);\n" //
				+ "        }\n" //
				+ "        return a + 7;\n" //
				+ "    };\n" //
				+ "\n" //
				+ "    public static I1 j = a -> {\n" //
				+ "        if (a == 2) {\n" //
				+ "            return C1.j.add(4);\n" //
				+ "        }\n" //
				+ "        return a + 8;\n" //
				+ "    };\n" //
				+ "}\n"; //
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected });
	}

	@Test
	public void testDoNotConvertLocalRecursiveClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
				+ "public class C2 {\n" //
				+ "\n" //
				+ "    public interface I1 {\n" //
				+ "        public int add(int a);\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int foo() {\n" //
				+ "        I1 doNotConvert = new I1() {\n" //
				+ "            @Override\n" //
				+ "            public int add(int a) {\n" //
				+ "                if (a == 2) {\n" //
				+ "                    return add(5);\n" //
				+ "                }\n" //
				+ "                return a + 9;\n" //
				+ "            }\n" //
				+ "        };\n" //
				+ "        return n.add(9);\n" //
				+ "    }\n" //
				+ "}\n"; //
		ICompilationUnit cu= pack1.createCompilationUnit("C2.java", sample, false, null);

		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}
