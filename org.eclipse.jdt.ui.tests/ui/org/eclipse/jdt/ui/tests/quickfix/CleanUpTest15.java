/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java15ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

/**
 * Tests the cleanup features related to Java 15.
 */
public class CleanUpTest15 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java15ProjectTestSetup(true);

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

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	@Ignore
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
				+ "            final Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInConditionalAndExpression(Object object, boolean isValid) {\n" //
				+ "        if (isValid && object instanceof Date) {\n" //
				+ "            final Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "\n" //
				+ "    public long matchPatternInAndExpression(Object object, boolean isValid) {\n" //
				+ "        if (object instanceof Date & isValid) {\n" //
				+ "            final Date date = (Date) object;\n" //
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
				+ "            final Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInConditionalOrExpression(Object object, boolean isValid) {\n" //
				+ "        if (!(object instanceof Date) || isValid) {\n" //
				+ "            return 0;\n" //
				+ "        } else {\n" //
				+ "            final Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInOrExpression(Object object, boolean isValid) {\n" //
				+ "        if (isValid | !(object instanceof Date)) {\n" //
				+ "            return 0;\n" //
				+ "        } else {\n" //
				+ "            final Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternInElse(Object object) {\n" //
				+ "        if (!(object instanceof Date)) {\n" //
				+ "            return 0;\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        final Date date = (Date) object;\n" //
				+ "        return date.getTime();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long matchPatternOnLoneStatement(Object object) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (object instanceof Date) final Date date = (Date) object;\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int matchPatternOnLoneElse(Object object) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (!(object instanceof Date)) object.toString();\n" //
				+ "        else final Date date = (Date) object;\n" //
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
				+ "    public long matchPatternOnLoneStatement(Object object) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (object instanceof Date date) {}\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public int matchPatternOnLoneElse(Object object) {\n" //
				+ "        // Keep this comment\n" //
				+ "        if (!(object instanceof Date date)) object.toString();\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", given, expected);
		assertGroupCategoryUsed(new ICompilationUnit[] { cu }, new HashSet<>(Arrays.asList(MultiFixMessages.PatternMatchingForInstanceofCleanup_description)));
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected });
	}

	@Ignore
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
				+ "            final Date theDate = (Date) object;\n" //
				+ "            return theDate.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchNotFinalVariable(Object object) {\n" //
				+ "        if (object instanceof Date) {\n" //
				+ "            Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchWithBadOperator(Object object, boolean isEnabled) {\n" //
				+ "        if (object instanceof Date || isEnabled) {\n" //
				+ "            final Date date = (Date) object;\n" //
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
				+ "            final Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchWrongObject(Object object, Object object2) {\n" //
				+ "        if (object instanceof Date) {\n" //
				+ "            final Date date = (Date) object2;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchWrongType(Object object) {\n" //
				+ "        if (object instanceof Date) {\n" //
				+ "            final java.sql.Date date = (java.sql.Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchActiveExpression(List<Object> objects) {\n" //
				+ "        if (objects.remove(0) instanceof Date) {\n" //
				+ "            final Date date = (Date) objects.remove(0);\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchAlreadyMatchedInstanceof(Object object) {\n" //
				+ "        if (object instanceof Date anotherDate) {\n" //
				+ "            final Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchReassignedObject(Object object, Object object2) {\n" //
				+ "        if (object instanceof Date) {\n" //
				+ "            object = object2;\n" //
				+ "            final Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchPatternInWhile(Object object) {\n" //
				+ "        while (object instanceof Date) {\n" //
				+ "            final Date date = (Date) object;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchActiveExpression(List<Object> objects) {\n" //
				+ "        if (objects.remove(0) instanceof Date) {\n" //
				+ "            final Date date = (Date) objects.remove(0);\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public long doNotMatchMultiDeclaration(Object object) {\n" //
				+ "        if (object instanceof Date) {\n" //
				+ "            final Date date = (Date) object, anotherDate = null;\n" //
				+ "            return date.getTime();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return 0;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void doNotMatchOppositeStatements() {\n" //
				+ "        Object bah = 1;\n" //
				+ "        if (bah instanceof Integer) return;\n" //
				+ "        final Integer i = (Integer) bah;\n" //
				+ "        System.out.println(i);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}
