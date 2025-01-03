/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
 *     Red Hat Inc. - modified to test Java 15 quick assists
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.tests.core.rules.Java15ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class AssistQuickFixTest15 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java15ProjectTestSetup(true);

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}

	}

	@Test
	public void testConcatToTextBlock1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = "" + //$NON-NLS-1$
			            "public void foo() {\\n" + //$NON-NLS-1$
			            "    System.out.println(\\"abc\\");\\n" + //$NON-NLS-1$
			            "}\\n"; //$NON-NLS-1$ // comment 2
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = \"""
						public void foo() {
						    System.out.println("abc");
						}
						\"""; //$NON-NLS-1$ // comment 2
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = "" +
			            "public void foo() { \\n" +
			            "    System.out.println(\\"abc\\");\\n" +
			            "}\\n"; // comment 2
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("System");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = \"""
						public void foo() {\\s
						    System.out.println("abc");
						}
						\"""; // comment 2
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = "" +
			            "public void foo() { \\n" +
			            "    System.out.println(\\"\\"\\"abc\\"\\"\\");\\n" +
			            "}\\n"; // comment 2
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("System");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = \"""
						public void foo() {\\s
						    System.out.println(\\\"""abc\\\""");
						}
						\"""; // comment 2
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = "" +
			            "abcdef" +
			            "ghijkl\\"\\"\\"123\\"\\"\\"" +
			            "mnop";
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("abcdef");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = \"""
						abcdef\\
						ghijkl\\\"""123\\\"""\\
						mnop\""";
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock5() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        StringBuffer buf= new StringBuffer("intro string\\n");
			        buf.append("public void foo() {\\n");
			        buf.append("    return null;\\n");
			        buf.append("}\\n");
			        buf.append("\\n");
			        System.out.println(buf.toString());
			        System.out.println(buf.toString() + "abc");
			        // comment 2
			        buf = new StringBuffer("intro string 2\\n");
			        buf.append("some string\\n");
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("buf");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String str = \"""
						intro string
						public void foo() {
						    return null;
						}
					\t
						\""";
			        System.out.println(str);
			        System.out.println(str + "abc");
			        // comment 2
			        StringBuffer buf = new StringBuffer("intro string 2\\n");
			        buf.append("some string\\n");
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock6() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        StringBuilder buf3= new StringBuilder();
			        buf3.append("public void foo() {\\n");
			        buf3.append("    return null;\\n");
			        buf3.append("}\\n");
			        buf3.append("\\n");
			        // comment 1
			        String k = buf3.toString();
			       \s
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("buf");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String k = \"""
						public void foo() {
						    return null;
						}
					\t
						\""";
			       \s
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock7() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        StringBuilder buf3= new StringBuilder();
			        buf3.append("public void foo() {\\n"); //$NON-NLS-1$
			        buf3.append("    return null;\\n"); //$NON-NLS-1$
			        buf3.append("}\\n"); //$NON-NLS-1$
			        buf3.append("\\n"); //$NON-NLS-1$
			        // comment 1
			        String k = buf3.toString();
			       \s
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("buf3");
		IInvocationContext ctx= getCorrectionContext(cu, index, 4);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        String str = \"""
						public void foo() {
						    return null;
						}
					\t
						\"""; //$NON-NLS-1$
			        // comment 1
			        String k = str;
			       \s
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock8() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        StringBuilder buf3= new StringBuilder();
			        buf3.append("public void foo() {\\n"); //$NON-NLS-1$
			        buf3.append("    return null;\\n"); //$NON-NLS-1$
			        buf3.append("}\\n"); //$NON-NLS-1$
			        buf3.append("\\n"); //$NON-NLS-1$
			        // comment 1
			        String k = buf3.toString();
			       \s
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("StringBuilder");
		IInvocationContext ctx= getCorrectionContext(cu, index, 4);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        String str = \"""
						public void foo() {
						    return null;
						}
					\t
						\"""; //$NON-NLS-1$
			        // comment 1
			        String k = str;
			       \s
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock9() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1111
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = "foo \\n" + "bar  " + "baz" + "biz";
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = \"""
						foo\\s
						bar  \\
						baz\\
						biz\""";
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock10() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1111
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		JavaCore.setOptions(hashtable);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = "foo \\n" + "bar  " + "baz" + "biz";
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x = \"""
			            foo\\s
			            bar  \\
			            baz\\
			            biz\""";
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock11() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1240
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		Hashtable<String, String> hashtable= JavaCore.getOptions();
		hashtable.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		JavaCore.setOptions(hashtable);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x ="\\tif (true) {\\n" +
			                "\\t\\tstuff();\\n" +
			                "\\t} else\\n" +
			                "\\t\\tnoStuff";
			        System.out.println(x);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x =\"""
			            	if (true) {
			            		stuff();
			            	} else
			            		noStuff\\
			            \""";
			        System.out.println(x);
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock12() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        StringBuilder buf3= new StringBuilder();
			        buf3.append("public void foo() {\\n"); //$NON-NLS-1$
			        buf3.append("    return null;\\n"); //$NON-NLS-1$
			        buf3.append("}\\n"); //$NON-NLS-1$
			        buf3.append("\\n"); //$NON-NLS-1$
			        // comment 1
			        write(buf3);
			       \s
			    }
			    private void write(CharSequence c) {
			        System.out.println(c);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("buf3");
		IInvocationContext ctx= getCorrectionContext(cu, index, 4);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        String str = \"""
						public void foo() {
						    return null;
						}
					\t
						\"""; //$NON-NLS-1$
			        // comment 1
			        write(str);
			       \s
			    }
			    private void write(CharSequence c) {
			        System.out.println(c);
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock13() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        StringBuilder buf3= new StringBuilder();
			        buf3.append("public void foo() {\\n");
			        buf3.append("    return null;\\n");
			        buf3.append("}\\n");
			        buf3.append("\\n");
			        // comment 1
			        int index = buf3.indexOf("null");
			        String k = buf3.toString();
			       \s
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("StringBuilder");
		IInvocationContext ctx= getCorrectionContext(cu, index, 4);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        String str = \"""
						public void foo() {
						    return null;
						}
					\t
						\""";
			        // comment 1
			        int index = str.indexOf("null");
			        String k = str;
			       \s
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToTextBlock14() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        StringBuilder buf3= new StringBuilder();
			        buf3.append("public void foo() {\\n");
			        buf3.append("    return null;\\n");
			        buf3.append("}\\n");
			        buf3.append("\\n");
			        // comment 1
			        int index = buf3.indexOf("null");
			        bufFunc(buf3);
			       \s
			    }
			    public void bufFunc(StringBuilder x) {
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("StringBuilder");
		IInvocationContext ctx= getCorrectionContext(cu, index, 4);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        StringBuilder buf3= new StringBuilder(\"""
						public void foo() {
						    return null;
						}
					\t
						\""");
			        // comment 1
			        int index = buf3.indexOf("null");
			        bufFunc(buf3);
			       \s
			    }
			    public void bufFunc(StringBuilder x) {
			    }
			}
			""";

		assertProposalExists(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testNoConcatToTextBlock1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x =\s
			            "abcdef" +
			            "ghijkl";
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("abcdef");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x =\s
			            "abcdef" +
			            "ghijkl" +
			            String.valueOf(true);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("abcdef");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        // comment 1
			        String x =\s
			            "abcdef" +
			            "ghijkl" +
			            3;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("abcdef");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo(String a) {
			        // comment 1
			        String x =\s
			            "abcdef" +
			            "ghijkl" +
			            a;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("abcdef");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock5() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void noToString() {
			        StringBuilder buf3= new StringBuilder();
			        buf3.append("public void foo() {\\n");
			        buf3.append("    return null;\\n");
			        buf3.append("}\\n");
			        buf3.append("\\n");
			       \s
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("buf3");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock6() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void extraAppend() {
			        StringBuilder buf3= new StringBuilder();
			        buf3.append("public void foo() {\\n");
			        buf3.append("    return null;\\n");
			        buf3.append("}\\n");
			        buf3.append("\\n");
			        String k = buf3.toString();
			        buf3.append("extra stuff\\n");
			       \s
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("buf3");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock7() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void combinedAppendWithNLS() {
			        StringBuffer buf3= new StringBuffer();
			        buf3.append("public void foo() {\\n"); //$NON-NLS-1$
			        buf3.append("    return null;\\n"); //$NON-NLS-1$
			        buf3.append("}\\n"); //$NON-NLS-1$
			        buf3.append("\\n").append("extra append\\n"); //$NON-NLS-1$ //$NON-NLS-2$
			        String k = buf3.toString();
			       \s
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("buf3");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock8() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void inconsistentNLSMarkers() {
			        StringBuffer buf3= new StringBuffer();
			        buf3.append("public void foo() {\\n"); //$NON-NLS-1$
			        buf3.append("    return null;\\n");
			        buf3.append("}\\n");
			        buf3.append("\\n");
			        String k = buf3.toString();
			       \s
			    }
			    public void indexOfInside() {
			        StringBuffer buf3= new StringBuffer();
			        buf3.append("public void foo() {\\n");
			        buf3.append("    return null;\\n");
			        buf3.append("}\\n");
			        int index = buf3.indexOf("foo");
			        buf3.append("\\n");
			        String k = buf3.toString();
			       \s
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("buf3");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock9() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void inconsistentNLS() {
			        // comment 1
			        String x = "" + //$NON-NLS-1$
			            "public void foo() {\\n" +
			            "    System.out.println(\\"abc\\");\\n" + //$NON-NLS-1$
			            "}\\n"; //$NON-NLS-1$ // comment 2
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testNoConcatToTextBlock10() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void inconsistentNLS() {
			        // comment 1
			        String y = "something";
			        String x = "" +
			            "public void foo() {\\n" +
			            "    System.out.println(\\"abc\\");\\n" +
			                  y +\s
			            "}\\n"; //$NON-NLS-1$ // comment 2
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("x");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.StringConcatToTextBlockFix_convert_msg);
	}

	@Test
	public void testConcatToMessageFormatTextBlock1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                "/***********\\n" +
			                " * simple {} \\n" +
			                " * copyright\\n" +
			                statement +
			                " * notice {0}\\n" +
			                "***********/\\n"; // comment 2
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;

			import java.text.MessageFormat;

			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                MessageFormat.format(\"""
					                /***********
					                 * simple '{}'\\s
					                 * copyright
					                {0}\\
					                 * notice '{'0'}'
					                ***********/
					                \""", statement); // comment 2
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_message_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToMessageFormatTextBlock2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                "/******************************\\n" + //$NON-NLS-1$
			                " * simple   \\n" + //$NON-NLS-1$
			                statement +
			                " * copyright\\n" + //$NON-NLS-1$
			                " ******************************/\\n"; //$NON-NLS-1$
			        System.out.println(copyright);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;

			import java.text.MessageFormat;

			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                MessageFormat.format(\"""
					                /******************************
					                 * simple  \\s
					                {0}\\
					                 * copyright
					                 ******************************/
					                \""", statement); //$NON-NLS-1$
			        System.out.println(copyright);
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_message_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToMessageFormatTextBlock3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                "/***************************************************\\n" + //$NON-NLS-1$
			                " * simple   \\n" + //$NON-NLS-1$
			                " * copyright\\n" + //$NON-NLS-1$
			                statement;
			        System.out.println(copyright);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;

			import java.text.MessageFormat;

			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
					        String copyright=
					                MessageFormat.format(\"""
					                /***************************************************
					                 * simple  \\s
					                 * copyright
					                {0}\""", statement); //$NON-NLS-1$
			        System.out.println(copyright);
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_message_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToMessageFormatTextBlock4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                "/*********************\\n" + //$NON-NLS-1$
			                " * simple   \\n" + //$NON-NLS-1$
			                " * copyright\\n" + //$NON-NLS-1$
			                statement + " * notice\\n" + "*********************/\\n"; //comment 2 //$NON-NLS-1$ //$NON-NLS-2$
			        System.out.println(copyright);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;

			import java.text.MessageFormat;

			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
					        String copyright=
					                MessageFormat.format(\"""
					                /*********************
					                 * simple  \\s
					                 * copyright
					                {0}\\
					                 * notice
					                *********************/
					                \""", statement); //comment 2 //$NON-NLS-1$
			        System.out.println(copyright);
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_message_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToMessageFormatTextBlock5() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo(String name, String id) {
			        String title = "Name: " + name + " ID: " + id;
			        System.out.println(title);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("title");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;

			import java.text.MessageFormat;

			public class Cls {
			    public void foo(String name, String id) {
			        String title = MessageFormat.format("Name: {0} ID: {1}", name, id);
			        System.out.println(title);
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToMessageFormatTextBlock6() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo(String name, String id) {
			        String title = "Name: " +
			                          name +\s
			                          " ID: " +\s
			                          id;
			        System.out.println(title);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("title");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;

			import java.text.MessageFormat;

			public class Cls {
			    public void foo(String name, String id) {
			        String title = MessageFormat.format("Name: {0} ID: {1}", name, id);
			        System.out.println(title);
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToStringFormatTextBlock1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                "/*******************************\\n" +
			                " * simple   \\n" +
			                " * copyright %\\n" +
			                statement +
			                " * notice\\n" +
			                " *******************************/\\n"; // comment 2
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                String.format(\"""
					                /*******************************
					                 * simple  \\s
					                 * copyright %%
					                %s\\
					                 * notice
					                 *******************************/
					                \""", statement); // comment 2
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToStringFormatTextBlock2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                "/******************************\\n" + //$NON-NLS-1$
			                " * simple   \\n" + //$NON-NLS-1$
			                statement +
			                " * copyright\\n" + //$NON-NLS-1$
			                " ******************************/\\n"; //$NON-NLS-1$
			        System.out.println(copyright);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                String.format(\"""
					                /******************************
					                 * simple  \\s
					                %s\\
					                 * copyright
					                 ******************************/
					                \""", statement); //$NON-NLS-1$
			        System.out.println(copyright);
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToStringFormatTextBlock3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                "/***************************************************\\n" + //$NON-NLS-1$
			                " * simple   \\n" + //$NON-NLS-1$
			                " * copyright\\n" + //$NON-NLS-1$
			                statement;
			        System.out.println(copyright);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
					        String copyright=
					                String.format(\"""
					                /***************************************************
					                 * simple  \\s
					                 * copyright
					                %s\""", statement); //$NON-NLS-1$
			        System.out.println(copyright);
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToStringFormatTextBlock4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
			        String copyright=
			                "/*********************\\n" + //$NON-NLS-1$
			                " * simple   \\n" + //$NON-NLS-1$
			                " * copyright\\n" + //$NON-NLS-1$
			                statement + " * notice\\n" + "*********************/\\n"; //comment 2 //$NON-NLS-1$ //$NON-NLS-2$
			        System.out.println(copyright);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("simple");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo() {
			        String statement= " * statement\\n";
			        // comment 1
					        String copyright=
					                String.format(\"""
					                /*********************
					                 * simple  \\s
					                 * copyright
					                %s\\
					                 * notice
					                *********************/
					                \""", statement); //comment 2 //$NON-NLS-1$
			        System.out.println(copyright);
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToStringFormatTextBlock5() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo(String name, String id) {
			        String title = "Name: " + name + " ID: " + id;
			        System.out.println(title);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("title");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo(String name, String id) {
			        String title = String.format("Name: %s ID: %s", name, id);
			        System.out.println(title);
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConcatToStringFormatTextBlock6() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			module test {
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String str1= """
			package test;
			public class Cls {
			    public void foo(String name, String id) {
			        String title = "Name: " +
			                          name +\s
			                          " ID: " +\s
			                          id;
			        System.out.println(title);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);
		int index= str1.indexOf("title");
		IInvocationContext ctx= getCorrectionContext(cu, index, 6);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;
			public class Cls {
			    public void foo(String name, String id) {
			        String title = String.format("Name: %s ID: %s", name, id);
			        System.out.println(title);
			    }
			}
			""";

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

}
