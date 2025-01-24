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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.tests.core.rules.Java21ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

public class AssistQuickFixTest21 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java21ProjectTestSetup();

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
	public void testConvertPatternInstanceofToSwitch1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set21CompilerOptions(fJProject1);
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

			public class E {
				public void foo(Object x, Object y) {
					int i, j;
					double d;
					boolean b;
					if (x instanceof Integer xint) {
						i = xint.intValue();
					} else if (x instanceof Double xdouble) {
						d = xdouble.doubleValue();
					} else if (x instanceof Boolean xboolean) {
						b = xboolean.booleanValue();
					} else {
						i = 0;
						d = 0.0D;
						b = false;
					}
				}
			}
			""";

		ICompilationUnit cu= pack.createCompilationUnit("E.java", str1, false, null);

		int index= str1.indexOf("doubleValue");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;

			public class E {
				public void foo(Object x, Object y) {
					int i, j;
					double d;
					boolean b;
					switch (x) {
						case Integer xint -> i = xint.intValue();
						case Double xdouble -> d = xdouble.doubleValue();
						case Boolean xboolean -> b = xboolean.booleanValue();
						case null, default -> {
							i = 0;
							d = 0.0D;
							b = false;
						}
					}
				}
			}
			""";

		assertProposalExists(proposals, FixMessages.PatternInstanceof_convert_if_to_switch);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertPatternInstanceofToSwitch2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set21CompilerOptions(fJProject1);
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

			public class E {
				public void foo(Object x, Object y) {
					int i, j;
					double d;
					boolean b;
					if (x instanceof Integer xint) {
						j = 7;
					} else if (x instanceof Double xdouble) {
						j = 8; // comment
					} else if (x instanceof Boolean xboolean) {
						j = 9;
					} else {
						i = 0;
						d = 0.0D;
						j = 10;
						b = false;
					}
				}
			}
			""";

		ICompilationUnit cu= pack.createCompilationUnit("E.java", str1, false, null);

		int index= str1.indexOf("xboolean");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;

			public class E {
				public void foo(Object x, Object y) {
					int i, j;
					double d;
					boolean b;
					switch (x) {
						case Integer xint -> j = 7;
						case Double xdouble -> j = 8; // comment
						case Boolean xboolean -> j = 9;
						case null, default -> {
							i = 0;
							d = 0.0D;
							j = 10;
							b = false;
						}
					}
				}
			}
			""";

		assertProposalExists(proposals, FixMessages.PatternInstanceof_convert_if_to_switch);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertPatternInstanceofToSwitch3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set21CompilerOptions(fJProject1);
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

			public class E {
				public int square(int x) {
					return x * x;
				}
				public int foo(Object x, Object y) {
					int i, j;
					double d;
					boolean b;
					if (y instanceof Integer xint) {
						return 7;
					} else if (y instanceof final Double xdouble) {
						return square(8); // square
					} else if (y instanceof final Boolean xboolean) {
						throw new NullPointerException();
					} else {
						i = 0;
						d = 0.0D;
						b = false;
						if (x instanceof Integer) {
							return 10;
						}
						return 11;
					}
				}
			}
			""";

		ICompilationUnit cu= pack.createCompilationUnit("E.java", str1, false, null);

		int index= str1.indexOf("throw");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;

			public class E {
				public int square(int x) {
					return x * x;
				}
				public int foo(Object x, Object y) {
					int i, j;
					double d;
					boolean b;
					switch (y) {
						case Integer xint -> {
							return 7;
						}
						case Double xdouble -> {
							return square(8); // square
						}
						case Boolean xboolean -> throw new NullPointerException();
						case null, default -> {
							i = 0;
							d = 0.0D;
							b = false;
							if (x instanceof Integer) {
								return 10;
							}
							return 11;
						}
					}
				}
			}
			""";

		assertProposalExists(proposals, FixMessages.PatternInstanceof_convert_if_to_switch);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertPatternInstanceofToSwitch4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set21CompilerOptions(fJProject1);
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

			public class E {
				public void foo(Object x, Object y) {
					int i, j;
					double d;
					boolean b;
					if (x instanceof Integer xint) {
						j = 7;
					} else if (x instanceof Double xdouble) {
						j = 8; // comment
					} else if (x instanceof Boolean xboolean) {
						j = 9;
					} else {
						i = 0;
						d = 0.0D;
						b = false;
						j = 10;
					}
				}
			}
			""";

		ICompilationUnit cu= pack.createCompilationUnit("E.java", str1, false, null);

		int index= str1.indexOf("false");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;

			public class E {
				public void foo(Object x, Object y) {
					int i, j;
					double d;
					boolean b;
					j = switch (x) {
						case Integer xint -> 7;
						case Double xdouble -> 8; // comment
						case Boolean xboolean -> 9;
						case null, default -> {
							i = 0;
							d = 0.0D;
							b = false;
							yield 10;
						}
					};
				}
			}
			""";

		assertProposalExists(proposals, FixMessages.PatternInstanceof_convert_if_to_switch);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertPatternInstanceofToSwitch5() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set21CompilerOptions(fJProject1);
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

			public class E {
				public int square(int x) {
					return x * x;
				}
				public int foo(Object x, Object y) {
					int i, j;
					double d;
					boolean b;
					if (y instanceof Integer xint) {
						return 7;
					} else if (y instanceof final Double xdouble) {
						return square(8); // square
					} else if (y instanceof final Boolean xboolean) {
						throw new NullPointerException();
					} else {
						i = 0;
						d = 0.0D;
						b = false;
						return 10;
					}
				}
			}
			""";

		ICompilationUnit cu= pack.createCompilationUnit("E.java", str1, false, null);

		int index= str1.indexOf("false");
		IInvocationContext ctx= getCorrectionContext(cu, index, 1);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		String expected= """
			package test;

			public class E {
				public int square(int x) {
					return x * x;
				}
				public int foo(Object x, Object y) {
					int i, j;
					double d;
					boolean b;
					return switch (y) {
						case Integer xint -> 7;
						case Double xdouble -> square(8); // square
						case Boolean xboolean -> throw new NullPointerException();
						case null, default -> {
							i = 0;
							d = 0.0D;
							b = false;
							yield 10;
						}
					};
				}
			}
			""";

		assertProposalExists(proposals, FixMessages.PatternInstanceof_convert_if_to_switch);
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testDoNotConvertPatternInstanceofToSwitch1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set21CompilerOptions(fJProject1);
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

			public class E {
				public void foo(Object x, Object y) {
					int i, j;
					double d;
					boolean b;
					if (x instanceof Integer xint) {
						i = xint.intValue();
					} else if (y instanceof Double xdouble) {
						d = xdouble.doubleValue();
					} else if (x instanceof Boolean xboolean) {
						b = xboolean.booleanValue();
					} else {
						i = 0;
						d = 0.0D;
						b = false;
					}
				}
			}
			""";

		ICompilationUnit cu= pack.createCompilationUnit("E.java", str1, false, null);

		int index= str1.indexOf("false");
		IInvocationContext ctx= getCorrectionContext(cu, index, 5);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		assertProposalDoesNotExist(proposals, FixMessages.PatternInstanceof_convert_if_to_switch);
	}

}
