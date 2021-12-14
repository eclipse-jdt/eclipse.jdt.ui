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

import org.eclipse.jdt.ui.tests.core.rules.Java16ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

/**
 * Tests the cleanup features related to Java 16.
 */
public class CleanUpTest16 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java16ProjectTestSetup(false);

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testRegexPatternForRecord() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public record E(int width, int height) {\n" //
				+ "    public void foo() {\n" //
				+ "        String k = \"bcd\";\n" //
				+ "        String m = \"abcdef\";\n" //
				+ "        String n = \"bcdefg\";\n" //
				+ "        String[] a = m.split(k);\n" //
				+ "        String[] b = n.split(k);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.regex.Pattern;\n" //
				+ "\n" //
				+ "public record E(int width, int height) {\n" //
				+ "    private static final Pattern k_pattern = Pattern.compile(\"bcd\");\n" //
				+ "\n" //
				+ "    public void foo() {\n" //
				+ "        Pattern k = k_pattern;\n" //
				+ "        String m = \"abcdef\";\n" //
				+ "        String n = \"bcdefg\";\n" //
				+ "        String[] a = k.split(m);\n" //
				+ "        String[] b = k.split(n);\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveRedundantSemicolonsForRecord() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public record E(int width, int height) {\n" //
				+ "    public void foo() {\n" //
				+ "    }\n" //
				+ "};;\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public record E(int width, int height) {\n" //
				+ "    public void foo() {\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testPatternMatchingForInstanceof() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Date;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public long matchPatternForInstanceof(Object object) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (object instanceof Date) {\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternForInstanceofOnFinalVariable(Object object) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (object instanceof Date) {\n" //
				+ "            final Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInConditionalAndExpression(Object object, boolean isValid) {\n" //
				+ "        if (isValid && object instanceof Date) {\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "\n" //
				+ "    public long matchPatternInAndExpression(Object object, boolean isValid) {\n" //
				+ "        if (object instanceof Date & isValid) {\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInElse(Object object) {\n" //
				+ "        if (!(object instanceof Date)) {\n" //
				+ "            return 0;\n" //
				+ "        } else {\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInConditionalOrExpression(Object object, boolean isValid) {\n" //
				+ "        if (!(object instanceof Date) || isValid) {\n" //
				+ "            return 0;\n" //
				+ "        } else {\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInOrExpression(Object object, boolean isValid) {\n" //
				+ "        if (isValid | !(object instanceof Date)) {\n" //
				+ "            return 0;\n" //
				+ "        } else {\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInElse(Object object) {\n" //
				+ "        if (!(object instanceof Date)) {\n" //
				+ "            return 0;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        Date date = (Date) object;\n" //
				+ "        return date.getTime();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int matchPatternOnLoneStatement(Object object) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (!(object instanceof Date)) object.toString();\n" //
				+ "        else {Date date = (Date) object;}\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Date;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public long matchPatternForInstanceof(Object object) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (object instanceof Date date) {\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternForInstanceofOnFinalVariable(Object object) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (object instanceof Date date) {\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInConditionalAndExpression(Object object, boolean isValid) {\n" //
				+ "        if (isValid && object instanceof Date date) {\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "\n" //
				+ "    public long matchPatternInAndExpression(Object object, boolean isValid) {\n" //
				+ "        if (object instanceof Date date & isValid) {\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInElse(Object object) {\n" //
				+ "        if (!(object instanceof Date date)) {\n" //
				+ "            return 0;\n" //
				+ "        } else {\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInConditionalOrExpression(Object object, boolean isValid) {\n" //
				+ "        if (!(object instanceof Date date) || isValid) {\n" //
				+ "            return 0;\n" //
				+ "        } else {\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInOrExpression(Object object, boolean isValid) {\n" //
				+ "        if (isValid | !(object instanceof Date date)) {\n" //
				+ "            return 0;\n" //
				+ "        } else {\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInElse(Object object) {\n" //
				+ "        if (!(object instanceof Date date)) {\n" //
				+ "            return 0;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return date.getTime();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int matchPatternOnLoneStatement(Object object) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (!(object instanceof Date date)) object.toString();\n" //
				+ "        else {}\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.PatternMatchingForInstanceofCleanup_description)));
	}

	@Test
	public void testDoNotMatchPatternForInstanceof() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Date;\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public long doNotMatchInOppositeCondition(Object object) {\n" //
				+ "        if (!(object instanceof Date)) {\n" //
				+ "            Date theDate = (Date) object;\n" //
				+ "            return theDate.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchWithBadOperator(Object object, boolean isEnabled) {\n" //
				+ "        if (object instanceof Date || isEnabled) {\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchElseWithAndOperator(Object object, boolean isValid) {\n" //
				+ "        if (isValid && !(object instanceof Date)) {\n" //
				+ "            return 0;\n" //
				+ "        } else {\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchWrongObject(Object object, Object object2) {\n" //
				+ "        if (object instanceof Date) {\n" //
				+ "            Date date = (Date) object2;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchWrongType(Object object) {\n" //
				+ "        if (object instanceof Date) {\n" //
				+ "            java.sql.Date date = (java.sql.Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchActiveExpression(List<Object> objects) {\n" //
				+ "        if (objects.remove(0) instanceof Date) {\n" //
				+ "            Date date = (Date) objects.remove(0);\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchAlreadyMatchedInstanceof(Object object) {\n" //
				+ "        if (object instanceof Date anotherDate) {\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            date = new Date();\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchReassignedObject(Object object, Object object2) {\n" //
				+ "        if (object instanceof Date) {\n" //
				+ "            object = object2;\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchPatternInWhile(Object object) {\n" //
				+ "        while (object instanceof Date) {\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchMultiDeclaration(Object object) {\n" //
				+ "        if (object instanceof Date) {\n" //
				+ "            Date date = (Date) object, anotherDate = null;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotMatchOppositeStatements() {\n" //
				+ "        Object bah = 1;\n" //
				+ "        if (bah instanceof Integer) return;\n" //
				+ "        Integer i = (Integer) bah;\n" //
				+ "        System.out.println(i);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotAddFinalForRecordComponent() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public record E (String abc) {\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testStringBufferToStringBuilderLocalsOnly() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample0= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class SuperClass {\n" //
				+ "    public static StringBuffer field0;\n" //
				+ "}";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class TestStringBuilderCleanup extends SuperClass {\n" //
				+ "    private TestStringBuilderCleanup(){\n" //
				+ "    }\n" //
				+ "    public record K(StringBuffer comp1) {\n" //
				+ "        public static StringBuffer field1;\n" //
				+ "        public static StringBuffer field2;\n" //
				+ "        public static void changeWithFieldAccess(StringBuffer parm) {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            field1 = field2;\n" //
				+ "            TestStringBuilderCleanup.field0 = parm;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class TestStringBuilderCleanup extends SuperClass {\n" //
				+ "    private TestStringBuilderCleanup(){\n" //
				+ "    }\n" //
				+ "    public record K(StringBuffer comp1) {\n" //
				+ "        public static StringBuffer field1;\n" //
				+ "        public static StringBuffer field2;\n" //
				+ "        public static void changeWithFieldAccess(StringBuffer parm) {\n" //
				+ "            StringBuilder a = new StringBuilder();\n" //
				+ "            field1 = field2;\n" //
				+ "            TestStringBuilderCleanup.field0 = parm;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu0, cu1 }, new String[] { sample0, expected1 }, null);
	}

	@Test
	public void testDoNotChangeStringBufferToStringBuilderLocalsOnly() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample0= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class SuperClass {\n" //
				+ "    public static StringBuffer field0;\n" //
				+ "}";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class TestStringBuilderCleanup extends SuperClass {\n" //
				+ "    public static StringBuffer field1;\n" //
				+ "    public record K(StringBuffer comp1) {\n" //
				+ "        public static StringBuffer field0;\n" //
				+ "        public static K doNotChangeWithFieldAssignment(StringBuffer x) {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            a = field0;\n" //
				+ "            return new K(a);\n" //
				+ "        }\n" //
				+ "        public static void doNotChangeWithParmAssignment(StringBuffer parm) {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            a = parm;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        }\n" //
				+ "        public static void doNotChangeWithParentFieldAssignment(StringBuffer parm) {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            a = TestStringBuilderCleanup.field0;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        }\n" //
				+ "        public static void doNotChangeWithMethodCall(StringBuffer parm) {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            doNotChangeWithParentFieldAssignment(a);\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu0, cu1 });
	}

	@Test
	public void testChangeStringBufferToStringBuilderAll() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample0= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class SuperClass {\n" //
				+ "    public static StringBuffer field0;\n" //
				+ "}";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "\n" //
				+ "public class TestStringBuilderCleanup extends SuperClass {\n" //
				+ "    public static StringBuffer field1;\n" //
				+ "    public record K(StringBuffer comp1) {\n" //
				+ "        public static StringBuffer field0;\n" //
				+ "        public static K changeWithFieldAssignment(StringBuffer x) {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            List<StringBuffer> = new ArrayList<>();\n" //
				+ "            a = field0;\n" //
				+ "            return new K(a);\n" //
				+ "        }\n" //
				+ "        public static void changeWithParmAssignment(StringBuffer parm) {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            a = parm;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        }\n" //
				+ "        public static void changeWithParentFieldAssignment(StringBuffer parm) {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            a = TestStringBuilderCleanup.field0;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        }\n" //
				+ "        public static void changeWithMethodCall(StringBuffer parm) {\n" //
				+ "            StringBuffer a = new StringBuffer();\n" //
				+ "            changeWithParentFieldAssignment(a);\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		disable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		String expected0= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class SuperClass {\n" //
				+ "    public static StringBuilder field0;\n" //
				+ "}";

		String expected1= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "\n" //
				+ "public class TestStringBuilderCleanup extends SuperClass {\n" //
				+ "    public static StringBuilder field1;\n" //
				+ "    public record K(StringBuilder comp1) {\n" //
				+ "        public static StringBuilder field0;\n" //
				+ "        public static K changeWithFieldAssignment(StringBuilder x) {\n" //
				+ "            StringBuilder a = new StringBuilder();\n" //
				+ "            List<StringBuilder> = new ArrayList<>();\n" //
				+ "            a = field0;\n" //
				+ "            return new K(a);\n" //
				+ "        }\n" //
				+ "        public static void changeWithParmAssignment(StringBuilder parm) {\n" //
				+ "            StringBuilder a = new StringBuilder();\n" //
				+ "            a = parm;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        }\n" //
				+ "        public static void changeWithParentFieldAssignment(StringBuilder parm) {\n" //
				+ "            StringBuilder a = new StringBuilder();\n" //
				+ "            a = TestStringBuilderCleanup.field0;\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        }\n" //
				+ "        public static void changeWithMethodCall(StringBuilder parm) {\n" //
				+ "            StringBuilder a = new StringBuilder();\n" //
				+ "            changeWithParentFieldAssignment(a);\n" //
				+ "            a.append(\"abc\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu0, cu1 }, new String[] { expected0, expected1 }, null);
	}

	@Test
	public void testDoNotRemoveParenthesesFromPatternInstanceof() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class TestParenthesesRemoval {\n" //
				+ "        public static void doNotChangeParenthesesForInstanceof(Object o) {\n" //
				+ "            if (!(o instanceof String)) {\n" //
				+ "                System.out.println(\"not a String\");\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "        public static void doNotChangeParenthesesForPatternInstanceof(Object o) {\n" //
				+ "            if (!(o instanceof String s)) {\n" //
				+ "                System.out.println(\"not a String\");\n" //
				+ "            } else {\n" //
				+ "                System.out.println(\"String length is \" + s.length());\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestParenthesesRemoval.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

}
