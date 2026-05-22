/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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

import org.eclipse.jdt.ui.tests.core.rules.Java12ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

public class AssistQuickFixTest12 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java12ProjectTestSetup(true);

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
	public void testSplitSwitchCaseStatement() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set12CompilerOptions(fJProject1, true);
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
			    public static void foo(Day day) {
			        switch (day) {
			            case SATURDAY, SUNDAY: System.out.println("Weekend");
			            case MONDAY, TUESDAY, WEDNESDAY: System.out.println("Weekday");
			            case THURSDAY, FRIDAY: System.out.println("Weekday");
			            default :
			                break;
			        }
			    }
			}

			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		IInvocationContext ctx= getCorrectionContext(cu, 185, 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public static void foo(Day day) {
			        switch (day) {
			            case SATURDAY, SUNDAY: System.out.println("Weekend");
			            case MONDAY :
							System.out.println("Weekday");
						case TUESDAY :
							System.out.println("Weekday");
						case WEDNESDAY :
							System.out.println("Weekday");
						case THURSDAY, FRIDAY: System.out.println("Weekday");
			            default :
			                break;
			        }
			    }
			}

			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testSplitSwitchCaseLabelRuleStatement() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, true);
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
			    public static void foo(Day day) {
			        String weekDayOrEnd = switch (day) {
			            case SATURDAY, SUNDAY -> "Weekend";
			            case MONDAY, TUESDAY, WEDNESDAY -> "Weekday";
			            case THURSDAY, FRIDAY -> "Weekday";
			        };
			    }
			}

			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		IInvocationContext ctx= getCorrectionContext(cu, 239, 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public static void foo(Day day) {
			        String weekDayOrEnd = switch (day) {
			            case SATURDAY, SUNDAY -> "Weekend";
			            case MONDAY, TUESDAY, WEDNESDAY -> "Weekday";
			            case THURSDAY -> "Weekday";
						case FRIDAY -> "Weekday";
			        };
			    }
			}

			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testConvertToSwitchStatement1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
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
				public int foo2(int x) {
					if (x == 2) {
						return 4;
					}
					if (x == 3) {
						return 6;
					}
					if (x == 8) {
						return 16;
					}
					if (x == 10) {
						return 18;
					}
					return 7;
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int startIndex= str1.indexOf("if");
		int endIndex= str1.indexOf("return 7;");

		IInvocationContext ctx= getCorrectionContext(cu, startIndex, endIndex + 9 - startIndex);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
				public int foo2(int x) {
					return switch (x) {
						case 2 -> 4;
						case 3 -> 6;
						case 8 -> 16;
						case 10 -> 18;
						default -> 7;
					};
				}
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testConvertToSwitchStatement2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
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
				public int foo2(int x) throws Exception {
					if (x == 2) {
						return 4;
					}
					if (x == 3) {
						return 6;
					}
					if (x == 8) {
						return 16;
					}
					if (x == 10) {
						return 18;
					}
					throw new Exception();
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int startIndex= str1.indexOf("if");
		int endIndex= str1.indexOf("throw ");

		IInvocationContext ctx= getCorrectionContext(cu, startIndex, endIndex + 22 - startIndex);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
				public int foo2(int x) throws Exception {
					return switch (x) {
						case 2 -> 4;
						case 3 -> 6;
						case 8 -> 16;
						case 10 -> 18;
						default -> throw new Exception();
					};
				}
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testConvertToSwitchStatement3() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
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
				public int foo2(int x) {
					if (x == 2) {
						return 4;
					}
					if (x == 3) {
						return 6;
					}
					if (x == 8) {
						return 16;
					}
					if (x == 10) {
						return 18;
					}
					return 7;
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int startIndex= str1.indexOf("if");
		int endIndex= str1.indexOf("return 7;");

		IInvocationContext ctx= getCorrectionContext(cu, startIndex, endIndex - startIndex);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
				public int foo2(int x) {
					switch (x) {
						case 2 -> {
							return 4;
						}
						case 3 -> {
							return 6;
						}
						case 8 -> {
							return 16;
						}
						case 10 -> {
							return 18;
						}
						default -> {
						}
					}
					return 7;
				}
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testConvertToSwitchStatement4() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
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
				public int foo2(Integer x) {
					if (x == 2) {
						return 4;
					}
					if (x == 3) {
						return 6;
					}
					if (x == 8) {
						return 16;
					}
					if (x == 10) {
						return 18;
					}
					return 7;
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int startIndex= str1.indexOf("if");
		int endIndex= str1.indexOf("return 7;");

		IInvocationContext ctx= getCorrectionContext(cu, startIndex, endIndex + 9 - startIndex);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
				public int foo2(Integer x) {
					if (x != null) {
						return switch (x) {
							case 2 -> 4;
							case 3 -> 6;
							case 8 -> 16;
							case 10 -> 18;
							default -> 7;
						};
					} else {
						return 7;
					}
				}
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}
}

