/*******************************************************************************
 * Copyright (c) 2020, 2025 Red Hat Inc. and others.
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

import java.util.Hashtable;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java22ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 22.
 */
public class CleanUpTest22 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java22ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testUnusedCleanUpUnnamedVariable() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNUSED_LAMBDA_PARAMETER, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
				private interface J {
					public void run(String a, String b);
				}
				record R(int i, long l) {}
				public void test () {
				  	J j = (a, b) -> System.out.println(a);
					j.run("a", "b");
					R r = new R(1, 1);
					switch (r) {
					case R(_, long l) -> {}
					case R r2 -> {}
					}
				}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= """
			package test1;

			public class E {
				private interface J {
					public void run(String a, String b);
				}
				record R(int i, long l) {}
				public void test () {
				  	J j = (a, _) -> System.out.println(a);
					j.run("a", "b");
					R r = new R(1, 1);
					switch (r) {
					case R(_, long _) -> {}
					case R _ -> {}
					}
				}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testPatternInstanceofToSwitch() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

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
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_SWITCH_FOR_INSTANCEOF_PATTERN);
		sample= """
				package test1;

				public class E {
					public int square(int x) {
						return x * x;
					}
					public int foo(Object x, Object y) {
						int i, j;
						double d;
						boolean b;
						switch (y) {
							case Integer _ -> {
								return 7;
							}
							case Double _ -> {
								return square(8); // square
							}
							case Boolean _ -> throw new NullPointerException();
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
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testPatternInstanceofToSwitchExpression() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
				int i;
				double d;
				boolean b;

				public int square(int x) {
					return x*x;
				}
				public int foo(Object y) {
					if (y instanceof final Integer xint) {
						return xint;
					}
					if (y instanceof final Double xdouble) {
						return square(8); // square
					} else if (y instanceof final Boolean xboolean) {
						throw new NullPointerException();
					} else {
						i = 0;
						d = 0.0D;
						b = false;
						return 11;
					}
				}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_SWITCH_FOR_INSTANCEOF_PATTERN);

		sample= """
			package test1;

			public class E {
				int i;
				double d;
				boolean b;

				public int square(int x) {
					return x*x;
				}
				public int foo(Object y) {
					return switch (y) {
						case Integer xint -> xint;
						case Double _ -> square(8); // square
						case Boolean _ -> throw new NullPointerException();
						case null, default -> {
							i = 0;
							d = 0.0D;
							b = false;
							yield 11;
						}
					};
				}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}
	@Test
	public void testPatternInstanceofToSwitchExpression2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
				int i;
				double d;
				boolean b;

				public int square(int x) {
					return x*x;
				}
				public int foo(Object y) {
					if (y instanceof final Integer xint) {
						return xint;
					}
					if (y instanceof final Double xdouble) {
						return square(8); // square
					} else if (y instanceof final Boolean xboolean) {
						throw new NullPointerException();
					}
					return 11;
				}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_SWITCH_FOR_INSTANCEOF_PATTERN);

		sample= """
			package test1;

			public class E {
				int i;
				double d;
				boolean b;

				public int square(int x) {
					return x*x;
				}
				public int foo(Object y) {
					return switch (y) {
						case Integer xint -> xint;
						case Double _ -> square(8); // square
						case Boolean _ -> throw new NullPointerException();
						case null, default -> 11;
					};
				}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

}
