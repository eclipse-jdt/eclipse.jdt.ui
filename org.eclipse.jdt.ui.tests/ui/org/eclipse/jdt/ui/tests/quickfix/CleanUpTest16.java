/*******************************************************************************
 * Copyright (c) 2020, 2024 Red Hat Inc. and others.
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
		String sample= """
			package test1;
			
			public record E(int width, int height) {
			    public void foo() {
			        String k = "bcd";
			        String m = "abcdef";
			        String n = "bcdefg";
			        String[] a = m.split(k);
			        String[] b = n.split(k);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PRECOMPILE_REGEX);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		sample= """
			package test1;
			
			import java.util.regex.Pattern;
			
			public record E(int width, int height) {
			    private static final Pattern k_pattern = Pattern.compile("bcd");
			
			    public void foo() {
			        Pattern k = k_pattern;
			        String m = "abcdef";
			        String n = "bcdefg";
			        String[] a = k.split(m);
			        String[] b = k.split(n);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testRemoveRedundantSemicolonsForRecord() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public record E(int width, int height) {
			    public void foo() {
			    }
			};;
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_SEMICOLONS);

		sample= """
			package test1;
			
			public record E(int width, int height) {
			    public void foo() {
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testPatternMatchingForInstanceof() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			import java.util.Date;
			
			public class E {
			    public long matchPatternForInstanceof(Object object) {
			        // Keep this comment
			        if (object instanceof Date) {
			            // Keep this comment too
			            Date date = (Date) object;
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long matchPatternForInstanceofOnFinalVariable(Object object) {
			        // Keep this comment
			        if (object instanceof Date) {
			            // Keep this comment too
			            final Date date = (Date) object;
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long matchPatternInConditionalAndExpression(Object object, boolean isValid) {
			        if (isValid && object instanceof Date) {
			            // Keep this comment
			            Date date = (Date) object;
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long matchPatternInElse(Object object) {
			        if (!(object instanceof Date)) {
			            return 0;
			        } else {
			            Date date = (Date) object;
			            return date.getTime();
			        }
			    }
			
			    public long matchPatternInConditionalOrExpression(Object object, boolean isValid) {
			        if (!(object instanceof Date) || isValid) {
			            return 0;
			        } else {
			            Date date = (Date) object;
			            return date.getTime();
			        }
			    }
			
			    public long matchPatternInElse(Object object) {
			        if (!(object instanceof Date)) {
			            return 0;
			        }
			
			        Date date = (Date) object;
			        return date.getTime();
			    }
			
			    public int matchPatternOnLoneStatement(Object object) {
			        // Keep this comment
			        if (!(object instanceof Date)) object.toString();
			        else {Date date = (Date) object;}
			
			        return 0;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF);

		String expected= """
			package test1;
			
			import java.util.Date;
			
			public class E {
			    public long matchPatternForInstanceof(Object object) {
			        // Keep this comment
			        if (object instanceof Date date) {
			            // Keep this comment too
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long matchPatternForInstanceofOnFinalVariable(Object object) {
			        // Keep this comment
			        if (object instanceof final Date date) {
			            // Keep this comment too
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long matchPatternInConditionalAndExpression(Object object, boolean isValid) {
			        if (isValid && object instanceof Date date) {
			            // Keep this comment
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long matchPatternInElse(Object object) {
			        if (!(object instanceof Date date)) {
			            return 0;
			        } else {
			            return date.getTime();
			        }
			    }
			
			    public long matchPatternInConditionalOrExpression(Object object, boolean isValid) {
			        if (!(object instanceof Date date) || isValid) {
			            return 0;
			        } else {
			            return date.getTime();
			        }
			    }
			
			    public long matchPatternInElse(Object object) {
			        if (!(object instanceof Date date)) {
			            return 0;
			        }
			
			        return date.getTime();
			    }
			
			    public int matchPatternOnLoneStatement(Object object) {
			        // Keep this comment
			        if (!(object instanceof Date date)) object.toString();
			        else {}
			
			        return 0;
			    }
			}
			""";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.PatternMatchingForInstanceofCleanup_description)));
	}

	@Test
	public void testPatternMatchingForInstanceof2() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/780
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			   \s
			    static class InternalStaticClass {
			        private int k;
			       \s
			        public InternalStaticClass(int val) {
			            this.k= val;
			        }
			       \s
			        public int getK() {
			            return k;
			        }
			    }
			
			}
			"""; //
		pack.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			
			import test1.E1.InternalStaticClass;
			
			public class E {
			   \s
			    public void foo(Object x) {
			        if (x instanceof E1.InternalStaticClass) {
			            // comment 1
			            InternalStaticClass t = (InternalStaticClass)x;
			            System.out.println(t.getK());
			        }
			    }
			
			}
			"""; //
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF);

		String expected= """
			package test1;
			
			public class E {
			   \s
			    public void foo(Object x) {
			        if (x instanceof E1.InternalStaticClass t) {
			            // comment 1
			            System.out.println(t.getK());
			        }
			    }
			
			}
			"""; //
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.PatternMatchingForInstanceofCleanup_description)));
	}

	@Test
	public void testOneIfWithPatternInstanceof() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1200
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			
			    protected String getString(Number number) {
			
			        if (number instanceof Long n) {
			            return n.toString();
			        }
			        if (number instanceof Float n) {
			            return n.toString();
			        }
			        if (number instanceof Double n) {
			            return n.toString();
			        }
			        if (number instanceof Float n && n.isInfinite()) {
			            return "Inf"; //$NON-NLS-1$
			        }
			        if (number instanceof Double m && m.isInfinite()) {
			            return "Inf"; //$NON-NLS-1$
			        }
			
			        return null;
			    }
			
			}
			"""; //
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.ONE_IF_RATHER_THAN_DUPLICATE_BLOCKS_THAT_FALL_THROUGH);

		String expected= """
			package test1;
			
			public class E {
			
			    protected String getString(Number number) {
			
			        if (number instanceof Long n) {
			            return n.toString();
			        }
			        if (number instanceof Float n) {
			            return n.toString();
			        }
			        if (number instanceof Double n) {
			            return n.toString();
			        }
			        if ((number instanceof Float n && n.isInfinite()) || (number instanceof Double m && m.isInfinite())) {
			            return "Inf"; //$NON-NLS-1$
			        }
			
			        return null;
			    }
			
			}
			"""; //
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp_description)));
	}

	@Test
	public void testDoNotMatchPatternForInstanceof() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.Date;
			import java.util.List;
			
			public class E {
			    public long doNotMatchInOppositeCondition(Object object) {
			        if (!(object instanceof Date)) {
			            Date theDate = (Date) object;
			            return theDate.getTime();
			        }
			
			        return 0;
			    }
			
			    public long doNotMatchWithBadOperator(Object object, boolean isEnabled) {
			        if (object instanceof Date || isEnabled) {
			            Date date = (Date) object;
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long doNotMatchElseWithAndOperator(Object object, boolean isValid) {
			        if (isValid && !(object instanceof Date)) {
			            return 0;
			        } else {
			            Date date = (Date) object;
			            return date.getTime();
			        }
			    }
			
			    public long doNotMatchWrongObject(Object object, Object object2) {
			        if (object instanceof Date) {
			            Date date = (Date) object2;
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long doNotMatchWrongType(Object object) {
			        if (object instanceof Date) {
			            java.sql.Date date = (java.sql.Date) object;
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long doNotMatchActiveExpression(List<Object> objects) {
			        if (objects.remove(0) instanceof Date) {
			            Date date = (Date) objects.remove(0);
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long doNotMatchAlreadyMatchedInstanceof(Object object) {
			        if (object instanceof Date anotherDate) {
			            Date date = (Date) object;
			            date = new Date();
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long doNotMatchReassignedObject(Object object, Object object2) {
			        if (object instanceof Date) {
			            object = object2;
			            Date date = (Date) object;
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long doNotMatchPatternInWhile(Object object) {
			        while (object instanceof Date) {
			            Date date = (Date) object;
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public long doNotMatchMultiDeclaration(Object object) {
			        if (object instanceof Date) {
			            Date date = (Date) object, anotherDate = null;
			            return date.getTime();
			        }
			
			        return 0;
			    }
			
			    public void doNotMatchOppositeStatements() {
			        Object bah = 1;
			        if (bah instanceof Integer) return;
			        Integer i = (Integer) bah;
			        System.out.println(i);
			    }
			
			    public void doNotMatchBitWiseAnd(boolean useStrikethroughForCompleted, Object data) {
			        if (data instanceof Long & useStrikethroughForCompleted) {
			            Long task = (Long)data;
			            if (task.intValue() == 0) {
			                int i = 0;
			            }
			        }
			    }
			
			    public void doNotMatchBitWiseOr(boolean useStrikethroughForCompleted, Object data) {
			        if (data instanceof Long | useStrikethroughForCompleted) {
			            Long task = (Long)data;
			            if (task.intValue() == 0) {
			                int i = 0;
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotAddFinalForRecordComponent() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public record E (String abc) {
			}
			""";
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
		String sample0= """
			package test1;
			
			public class SuperClass {
			    public static StringBuffer field0;
			}""";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= """
			package test1;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    private TestStringBuilderCleanup(){
			    }
			    public record K(StringBuffer comp1) {
			        public static StringBuffer field1;
			        public static StringBuffer field2;
			        public static void changeWithFieldAccess(StringBuffer parm) {
			            StringBuffer a = new StringBuffer();
			            field1 = field2;
			            TestStringBuilderCleanup.field0 = parm;
			            a.append("abc");
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		sample= """
			package test1;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    private TestStringBuilderCleanup(){
			    }
			    public record K(StringBuffer comp1) {
			        public static StringBuffer field1;
			        public static StringBuffer field2;
			        public static void changeWithFieldAccess(StringBuffer parm) {
			            StringBuilder a = new StringBuilder();
			            field1 = field2;
			            TestStringBuilderCleanup.field0 = parm;
			            a.append("abc");
			        };
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu0, cu1 }, new String[] { sample0, expected1 }, null);
	}

	@Test
	public void testDoNotChangeStringBufferToStringBuilderLocalsOnly() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample0= """
			package test1;
			
			public class SuperClass {
			    public static StringBuffer field0;
			}""";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= """
			package test1;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    public static StringBuffer field1;
			    public record K(StringBuffer comp1) {
			        public static StringBuffer field0;
			        public static K doNotChangeWithFieldAssignment(StringBuffer x) {
			            StringBuffer a = new StringBuffer();
			            a = field0;
			            return new K(a);
			        }
			        public static void doNotChangeWithParmAssignment(StringBuffer parm) {
			            StringBuffer a = new StringBuffer();
			            a = parm;
			            a.append("abc");
			        }
			        public static void doNotChangeWithParentFieldAssignment(StringBuffer parm) {
			            StringBuffer a = new StringBuffer();
			            a = TestStringBuilderCleanup.field0;
			            a.append("abc");
			        }
			        public static void doNotChangeWithMethodCall(StringBuffer parm) {
			            StringBuffer a = new StringBuffer();
			            doNotChangeWithParentFieldAssignment(a);
			            a.append("abc");
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu0, cu1 });
	}

	@Test
	public void testChangeStringBufferToStringBuilderAll() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample0= """
			package test1;
			
			public class SuperClass {
			    public static StringBuffer field0;
			}""";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= """
			package test1;
			
			import java.util.List;
			import java.util.ArrayList;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    public static StringBuffer field1;
			    public record K(StringBuffer comp1) {
			        public static StringBuffer field0;
			        public static K changeWithFieldAssignment(StringBuffer x) {
			            StringBuffer a = new StringBuffer();
			            List<StringBuffer> = new ArrayList<>();
			            a = field0;
			            return new K(a);
			        }
			        public static void changeWithParmAssignment(StringBuffer parm) {
			            StringBuffer a = new StringBuffer();
			            a = parm;
			            a.append("abc");
			        }
			        public static void changeWithParentFieldAssignment(StringBuffer parm) {
			            StringBuffer a = new StringBuffer();
			            a = TestStringBuilderCleanup.field0;
			            a.append("abc");
			        }
			        public static void changeWithMethodCall(StringBuffer parm) {
			            StringBuffer a = new StringBuffer();
			            changeWithParentFieldAssignment(a);
			            a.append("abc");
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		disable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		String expected0= """
			package test1;
			
			public class SuperClass {
			    public static StringBuilder field0;
			}""";

		String expected1= """
			package test1;
			
			import java.util.List;
			import java.util.ArrayList;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    public static StringBuilder field1;
			    public record K(StringBuilder comp1) {
			        public static StringBuilder field0;
			        public static K changeWithFieldAssignment(StringBuilder x) {
			            StringBuilder a = new StringBuilder();
			            List<StringBuilder> = new ArrayList<>();
			            a = field0;
			            return new K(a);
			        }
			        public static void changeWithParmAssignment(StringBuilder parm) {
			            StringBuilder a = new StringBuilder();
			            a = parm;
			            a.append("abc");
			        }
			        public static void changeWithParentFieldAssignment(StringBuilder parm) {
			            StringBuilder a = new StringBuilder();
			            a = TestStringBuilderCleanup.field0;
			            a.append("abc");
			        }
			        public static void changeWithMethodCall(StringBuilder parm) {
			            StringBuilder a = new StringBuilder();
			            changeWithParentFieldAssignment(a);
			            a.append("abc");
			        }
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu0, cu1 }, new String[] { expected0, expected1 }, null);
	}

	@Test
	public void testDoNotRemoveParenthesesFromPatternInstanceof() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String sample= """
			package test1;
			
			public class TestParenthesesRemoval {
			        public static void doNotChangeParenthesesForInstanceof(Object o) {
			            if (!(o instanceof String)) {
			                System.out.println("not a String");
			            }
			        }
			        public static void doNotChangeParenthesesForPatternInstanceof(Object o) {
			            if (!(o instanceof String s)) {
			                System.out.println("not a String");
			            } else {
			                System.out.println("String length is " + s.length());
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestParenthesesRemoval.java", sample, false, null);

		enable(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_NEVER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

}
