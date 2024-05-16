/*******************************************************************************
 * Copyright (c) 2021, 2024 Red Hat Inc. and others.
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

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java15ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 16.
 */
public class CleanUpTest15 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java15ProjectTestSetup(false);

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testConcatToTextBlock() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			/**
			 * Performs:
			 * <pre>{@code
			 *    for (String s : strings) {
			 *        if (s.equals(value)) {
			 *            return \\u0030;
			 *        }
			 *        if (s.startsWith(value)) {
			 *            return 1;
			 *        }
			 *    }
			 *    return -1;
			 * }</pre>
			 */
			public class E {
			    static String str = "" + //$NON-NLS-1$
			            "public class B { \\n" + //$NON-NLS-1$
			            "   public \\nvoid foo() {\\n" + //$NON-NLS-1$
			            "       System.out.println(\\"abc\\");\\n" + //$NON-NLS-1$
			            "   }\\n" + //$NON-NLS-1$
			            "}"; //$NON-NLS-1$
			
			    private static final String CU_POSTFIX= " {\\n" +
			            "	\\n" +
			            "}\\n" +
			            "}\\n";
			
			    public void testSimple() {
			        // comment 1
			        String x = "" + //$NON-NLS-1$
			            "public void foo() {\\n" + //$NON-NLS-1$
			            "    System.out.println(\\"abc\\");\\n" + //$NON-NLS-1$
			            "}\\n"; //$NON-NLS-1$ // comment 2
			    }
			
			    public void testTrailingSpacesAndInnerNewlines() {
			        String x = "" +
			            "public \\nvoid foo() {  \\n" +
			            "    System.out.println\\\\(\\"abc\\");\\n" +
			            "}\\n";
			    }
			
			    public void testLineContinuationAndTripleQuotes() {
			        String x = "" +
			            "abcdef" +
			            "ghijkl\\"\\"\\"\\"123\\"\\"\\"" +
			            "mnop\\\\";
			    }
			
			    public void testNoChange() {
			        StringBuffer buf = new StringBuffer();
			        buf.append("abcdef\\n");
			        buf.append("123456\\n");
			        buf.append("ghijkl\\n");
			        String k = buf.toString();
			    }
			    public Integer foo(String x) {
			        return Integer.valueOf(x.length());
			    }
			    public void testParameter() {
			        Integer k = foo("" +\s
			                  "abcdef\\n" +\s
			                  "123456\\n" +\s
			                  "klm");
			    }
			    public void testAssignment() {
			        Integer k = null;
			        k = foo("" +\s
			                  "abcdef\\n" +\s
			                  "123456\\n" +\s
			                  "klm");
			    }
			    public void testConcatInConstructor() {
			        new StringBuffer("abc\\n" + "def\\n" + "ghi");
			    }
			    public void testTabStart() {
			        String x ="\\tif (true) {\\n" +
			                "\\t\\tstuff();\\n" +
			                "\\t} else\\n" +
			                "\\t\\tnoStuff";
			    }
			    public void testEndEscapedQuotes() {
			        String a =
			                "1\\n" +
			                "2\\n" +
			                "3\\n" +
			                "4\\n" +
			                "\\"\\"\\"\\"";
			    }
			    public void testNoEndNewlineIndented() {
			        String x= ""
			                + "    /** bar\\n" //
			                + "     * foo\\n" //
			                + "     */"; //
			    }
			    public void testNoEndNewlineWithSpace() {
			        String x= ""
			                + "/** bar\\n" //
			                + " * foo\\n" //
			                + " */ "; //
			    }
			    public void testEscapedSingleQuote() {
			        String x= ""
			                + "public class Test {\\n"
			                + "  static String C = \\"\\\\n\\";\\n"
			                + "  \\n"
			                + "  public static void main(String[] args) {\\n"
			                + "      System.out.print(C.length());\\n"
			                + "      System.out.print(C.charAt(0) == \\'\\\\n\\');\\n"
			                + "  }\\n"
			                + "}";
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK);

		String expected1= """
			package test1;
			
			/**
			 * Performs:
			 * <pre>{@code
			 *    for (String s : strings) {
			 *        if (s.equals(value)) {
			 *            return \\u0030;
			 *        }
			 *        if (s.startsWith(value)) {
			 *            return 1;
			 *        }
			 *    }
			 *    return -1;
			 * }</pre>
			 */
			public class E {
			    static String str = \"""
			        public class B {\\s
			           public\\s
			        void foo() {
			               System.out.println("abc");
			           }
			        }\"""; //$NON-NLS-1$
			
			    private static final String CU_POSTFIX= \"""
			         {
			        \\t
			        }
			        }
			        \""";
			
			    public void testSimple() {
			        // comment 1
			        String x = \"""
			            public void foo() {
			                System.out.println("abc");
			            }
			            \"""; //$NON-NLS-1$ // comment 2
			    }
			
			    public void testTrailingSpacesAndInnerNewlines() {
			        String x = \"""
			            public\\s
			            void foo() { \\s
			                System.out.println\\\\("abc");
			            }
			            \""";
			    }
			
			    public void testLineContinuationAndTripleQuotes() {
			        String x = \"""
			            abcdef\\
			            ghijkl\\\"""\\"123\\\"""\\
			            mnop\\\\\""";
			    }
			
			    public void testNoChange() {
			        StringBuffer buf = new StringBuffer();
			        buf.append("abcdef\\n");
			        buf.append("123456\\n");
			        buf.append("ghijkl\\n");
			        String k = buf.toString();
			    }
			    public Integer foo(String x) {
			        return Integer.valueOf(x.length());
			    }
			    public void testParameter() {
			        Integer k = foo(\"""
			            abcdef
			            123456
			            klm\""");
			    }
			    public void testAssignment() {
			        Integer k = null;
			        k = foo(\"""
			            abcdef
			            123456
			            klm\""");
			    }
			    public void testConcatInConstructor() {
			        new StringBuffer(\"""
			            abc
			            def
			            ghi\""");
			    }
			    public void testTabStart() {
			        String x =\"""
			            	if (true) {
			            		stuff();
			            	} else
			            		noStuff\\
			            \""";
			    }
			    public void testEndEscapedQuotes() {
			        String a =
			                \"""
			            1
			            2
			            3
			            4
			            \\\"""\\\"""\";
			    }
			    public void testNoEndNewlineIndented() {
			        String x= \"""
			                /** bar
			                 * foo
			                 */\\
			            \"""; //
			    }
			    public void testNoEndNewlineWithSpace() {
			        String x= \"""
			            /** bar
			             * foo
			             */\\s\"""; //
			    }
			    public void testEscapedSingleQuote() {
			        String x= \"""
			            public class Test {
			              static String C = "\\\\n";
			             \\s
			              public static void main(String[] args) {
			                  System.out.print(C.length());
			                  System.out.print(C.charAt(0) == '\\\\n');
			              }
			            }\""";
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConcatToTextBlock2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void foo() {
			        // comment 1
			        StringBuffer buf= new StringBuffer("intro string\\n"); //$NON-NLS-1$
			        buf.append("public void foo() {\\n"); //$NON-NLS-1$
			        buf.append("    return null;\\n"); //$NON-NLS-1$
			        buf.append("}\\n"); //$NON-NLS-1$
			        buf.append("\\n"); //$NON-NLS-1$
			        System.out.println(buf.toString());
			        System.out.println(buf.toString() + "abc");
			        // comment 2
			        buf = new StringBuffer("intro string 2\\n");
			        buf.append("some string\\n");
			        buf.append("    another string\\n");
			        // comment 3
			        String k = buf.toString();
			        // comment 4
			        StringBuilder buf2= new StringBuilder();
			        buf2.append("public String metaPhone(final String txt2){\\n");
			        buf2.append("    return null;\\n");
			        buf2.append("}\\n");
			        buf2.append("\\n");
			        // comment 5
			        k = buf2.toString();
			        System.out.println(buf2.toString());
			        StringBuilder buf3= new StringBuilder();
			        buf3.append("public void foo() {\\n");
			        buf3.append("    return null;\\n");
			        buf3.append("}\\n");
			        buf3.append("\\n");
			        // comment 6
			        k = buf3.toString();
			        buf3= new StringBuilder();
			        buf3.append(4);
			
			        String x = "abc\\n" +
			            "def\\n" +
			            "ghi\\n";
			        new StringBuffer("abc\\n" + "def\\n" + "ghi");
			        new StringBuffer("1\\n" +
			                "2\\n" +
			                "3\\n" +
			                "4\\n" +
			                "\\"\\"\\"");
			        StringBuilder buf4= new StringBuilder();
			        buf4.append("    /** bar\\n");
			        buf4.append("     * foo\\n");
			        buf4.append("     */");
			        String expected= buf4.toString();
			        StringBuilder buf5= new StringBuilder();
			        buf5.append(3);
			        buf5= new StringBuilder();
			        buf5.append(
			                "package pack1;\\n" +
			                "\\n" +
			                "import java.util.*;\\n" +
			                "\\n" +
			                "public class C {\\n" +
			                "}");
			        System.out.println(buf5.toString());
			        buf5= new StringBuilder();
			        buf5.append(7);
			        String str3= "abc";
			        String x2= "" +
			                "abc\\n" +
			                "def\\n" +
			                "ghi\\n" +
			                "jki\\n";
			        StringBuilder buf6 = new StringBuilder(x2);
			        System.out.println(buf6.toString());
			        StringBuilder buf7 = new StringBuilder("" +
			                "abc\\n" +
			                "def\\n" +
			                "ghi\\n" +
			                "jki\\n");
			        System.out.println(buf7.toString());
			        buf7 = new StringBuilder();
			        buf7.append("abc" + x2 + "def");
			        StringBuilder buf8 = new StringBuilder("");
			        System.out.println(buf8.toString());
			        StringBuilder buf9 = new StringBuilder("abc\\n").append("def\\n").append("ghi");
			        buf9.append("jkl\\n").append("mno");
			        System.out.println(buf9.toString());
			    }
			}""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);
		assertNoCompilationError(cu1);

		enable(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK);
		enable(CleanUpConstants.STRINGCONCAT_STRINGBUFFER_STRINGBUILDER);

		String expected1= """
			package test1;
			
			public class E {
			    public void foo() {
			        // comment 1
			        String str = \"""
			            intro string
			            public void foo() {
			                return null;
			            }
			           \s
			            \"""; //$NON-NLS-1$
			        System.out.println(str);
			        System.out.println(str + "abc");
			        // comment 2
			        String str1 = \"""
			            intro string 2
			            some string
			                another string
			            \""";
			        // comment 3
			        String k = str1;
			        // comment 4
			        String str2 = \"""
			            public String metaPhone(final String txt2){
			                return null;
			            }
			           \s
			            \""";
			        // comment 5
			        k = str2;
			        System.out.println(str2);
			        // comment 6
			        k = \"""
			            public void foo() {
			                return null;
			            }
			           \s
			            \""";
			        StringBuilder buf3 = new StringBuilder();
			        buf3.append(4);
			
			        String x = \"""
			            abc
			            def
			            ghi
			            \""";
			        new StringBuffer(\"""
			            abc
			            def
			            ghi\""");
			        new StringBuffer(\"""
			            1
			            2
			            3
			            4
			            \\"\\"\\\"""\");
			        String expected= \"""
			                /** bar
			                 * foo
			                 */\\
			            \""";
			        StringBuilder buf5= new StringBuilder();
			        buf5.append(3);
			        String str4 = \"""
			            package pack1;
			           \s
			            import java.util.*;
			           \s
			            public class C {
			            }\""";
			        System.out.println(str4);
			        buf5= new StringBuilder();
			        buf5.append(7);
			        String str3= "abc";
			        String x2= \"""
			            abc
			            def
			            ghi
			            jki
			            \""";
			        StringBuilder buf6 = new StringBuilder(x2);
			        System.out.println(buf6.toString());
			        String str5 = \"""
			            abc
			            def
			            ghi
			            jki
			            \""";
			        System.out.println(str5);
			        StringBuilder buf7 = new StringBuilder();
			        buf7.append("abc" + x2 + "def");
			        String str6 = \"""
			            \""";
			        System.out.println(str6);
			        String str7 = \"""
			            abc
			            def
			            ghi\\
			            jkl
			            mno\""";
			        System.out.println(str7);
			    }
			}""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConcatInAnnotation1() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/824
		IJavaProject project1= getProject();
		JavaProjectHelper.addLibrary(project1, new Path(Java15ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import static java.lang.annotation.ElementType.TYPE;
			import static java.lang.annotation.RetentionPolicy.RUNTIME;
			
			import java.lang.annotation.Retention;
			import java.lang.annotation.Target;
			
			@Target({TYPE})\s
			@Retention(RUNTIME)
			public @interface SampleAnnotation {\s
			
			    String name();
			
			    String query();
			
			}
			""";
		pack1.createCompilationUnit("SampleAnnotation.java", sample, false, null);

		String sample2= """
			package test1;
			
			@SampleAnnotation(name = "testQuery",
			 query = "select * " +
			 "from test_entities " + \s
			 "where test = :test" ) //comment 1
			public class E {
			    public static void main(String[] args) {
			        final String foo = \s
			            ("Line1"+\s
			            "Line2"+ \s
			            "Line3"+
			            "Line4"//comment2
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample2, false, null);

		enable(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK);

		String expected1= """
			package test1;
			
			@SampleAnnotation(name = "testQuery",
			 query = \"""
			    select * \\
			    from test_entities \\
			    where test = :test\""" ) //comment 1
			public class E {
			    public static void main(String[] args) {
			        final String foo = \s
			            (\"""
			                Line1\\
			                Line2\\
			                Line3\\
			                Line4\"""//comment2
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConcatInAnnotation2() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/824
		IJavaProject project1= getProject();
		JavaProjectHelper.addLibrary(project1, new Path(Java15ProjectTestSetup.getJdtAnnotations20Path()));
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import static java.lang.annotation.ElementType.TYPE;
			import static java.lang.annotation.RetentionPolicy.RUNTIME;
			
			import java.lang.annotation.Retention;
			import java.lang.annotation.Target;
			
			@Target({TYPE})\s
			@Retention(RUNTIME)
			public @interface SampleAnnotation {\s
			
			    String[] value ();
			
			}
			""";
		pack1.createCompilationUnit("SampleAnnotation.java", sample, false, null);

		String sample2= """
			package test1;
			
			@SampleAnnotation({
			"select * " +
			 "from test_entities " + \s
			 "where test = :test"}) //comment 1
			public class E {
			    public static void main(String[] args) {
			        final String foo = \s
			            ("Line1"+\s
			            "Line2"+ \s
			            "Line3"+
			            "Line4"//comment2
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample2, false, null);

		enable(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK);

		String expected1= """
			package test1;
			
			@SampleAnnotation({
			\"""
			    select * \\
			    from test_entities \\
			    where test = :test\"""}) //comment 1
			public class E {
			    public static void main(String[] args) {
			        final String foo = \s
			            (\"""
			                Line1\\
			                Line2\\
			                Line3\\
			                Line4\"""//comment2
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testNoConcatToTextBlock() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void testNotThreeStrings() {
			        String x =\s
			            "abcdef" +
			            "ghijkl";\
			    }
			
			    public void testNotAllLiterals() {
			        String x = "" +
			            "abcdef" +
			            "ghijkl" +
			            String.valueOf(true)
			;\
			    }
			
			    public void testNotAllLiterals2(String a) {
			        String x = "" +
			            "abcdef" +
			            "ghijkl" +
			            a
			;\
			    }
			
			    public void testNotAllStrings() {
			        String x = "" +
			            "abcdef" +
			            "ghijkl" +
			            3;
			;\
			    }
			
			    public void testInconsistentNLS() {
			        String x = "" +
			            "abcdef" +
			            "ghijkl" + //$NON-NLS-1$
			            "mnop";
			    }
			
			    public void testArrayInitializer() {
			        String[] x = { "" +
			            "abcdef" +
			            "ghijkl" + //$NON-NLS-1$
			            "mnop"};
			    }
			
			    public void testCommentsThatWillBeLost() {
			        String x = "" +
			            "abcdef" +
			            "ghijkl" + // a comment
			            "mnop";
			    }
			
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testNoConcatToTextBlock2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void testNoToString() {
			        StringBuffer buf = new StringBuffer();
			        buf.append("abcdef\\n");
			        buf.append("123456\\n");
			        buf.append("ghijkl\\n");
			    }
			
			    public void testExtraCallsAfter() {
			        StringBuffer buf = new StringBuffer();
			        buf.append("abcdef\\n");
			        buf.append("123456\\n");
			        buf.append("ghijkl\\n");
			        String x = buf.toString();
			        buf.append("abcdef\\n");
			    }
			
			    public void testExtraCallsBetween(String a) {
			        StringBuffer buf = new StringBuffer();
			        buf.append("abcdef\\n");
			        buf.reverse();
			        buf.append("ghijkl\\n");
			        String x = buf.toString();
			    }
			
			    public void testSerialNLSCallsNotSupported() {
			        StringBuffer buf = new StringBuffer();
			        buf.append("abcdef\\n"); //$NON-NLS-1$
			        buf.append("123456\\n"); //$NON-NLS-1$
			        buf.append("ghijkl\\n").append("mnopqrst\\n"); //$NON-NLS-1$ //$NON-NLS-2$
			        String x = buf.toString();
			    }
			
			    public void testAppendingNonString() {
			        StringBuffer buf = new StringBuffer();
			        buf.append("abcdef\\n");
			        buf.append("123456\\n");
			        buf.append("ghijkl\\n");
			        buf.append(3);
			        String x = buf.toString();
			    }
			
			    public void testInconsistentNLS() {
			        StringBuffer buf = new StringBuffer();
			        buf.append("abcdef\\n");
			        buf.append("123456\\n"); //$NON-NLS-1$
			        buf.append("ghijkl\\n");
			        buf.append(3);
			        String x = buf.toString();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK);
		enable(CleanUpConstants.STRINGCONCAT_STRINGBUFFER_STRINGBUILDER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

}
