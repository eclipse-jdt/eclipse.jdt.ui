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

import org.eclipse.jdt.ui.tests.core.rules.Java21ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 22.
 */
public class CleanUpTest21 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java21ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testPatternInstanceofToSwitch1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

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
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_SWITCH_FOR_INSTANCEOF_PATTERN);

		sample= """
			package test1;

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
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testPatternInstanceofToSwitch2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

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
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_SWITCH_FOR_INSTANCEOF_PATTERN);

		sample= """
			package test1;

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
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testPatternInstanceofToSwitch3() throws Exception {
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
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testPatternInstanceofToSwitchExpression1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

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
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_SWITCH_FOR_INSTANCEOF_PATTERN);

		sample= """
			package test1;

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
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	public void testNoPatternInstanceofToSwitch1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

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
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_SWITCH_FOR_INSTANCEOF_PATTERN);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}


}
