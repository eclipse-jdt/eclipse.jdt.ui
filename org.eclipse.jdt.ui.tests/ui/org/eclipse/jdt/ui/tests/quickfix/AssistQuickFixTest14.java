/********************,***********************************************************
 * Copyright (c) 2020 2026 IBM Corporation and others.
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
 *     Red Hat Inc. - modified to test Java 14 quickfixes
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

import org.eclipse.jdt.ui.tests.core.rules.Java14ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

public class AssistQuickFixTest14 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java14ProjectTestSetup(true);

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
	public void testConvertToSwitchExpression1() throws Exception {
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
				public int foo(Day day) {
					// return variable
					int i;
					switch (day) {
						case SATURDAY:
						case SUNDAY: i = 5; break;
						case MONDAY:
						case TUESDAY, WEDNESDAY: i = 7; break;
						case THURSDAY:
						case FRIDAY: i = 14; break;
						default :
							i = 22;
							break;
					}
					return i;
				}
			}

			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("\t\tswitch (day) {");
		IInvocationContext ctx= getCorrectionContext(cu, index, 16);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
				public int foo(Day day) {
					// return variable
					int i = switch (day) {
						case SATURDAY, SUNDAY -> 5;
						case MONDAY, TUESDAY, WEDNESDAY -> 7;
						case THURSDAY, FRIDAY -> 14;
						default -> 22;
					};
					return i;
				}
			}

			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testConvertToSwitchExpression2() throws Exception {
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
				public int foo(Day day) {
					// return variable
					int i;
					int j = 4;
					// logic comment
					switch (day) {
						case SATURDAY:
						case SUNDAY: i = 5; break;
						case MONDAY:
						case TUESDAY:
						case WEDNESDAY: System.out.println("here"); i = 7; break;
						case THURSDAY:
						case FRIDAY: i = 14; break;
						default: i = 22; break;
					}
					return i;
				}
			}

			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("switch");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
				public int foo(Day day) {
					// return variable
					int i;
					int j = 4;
					// logic comment
					i = switch (day) {
						case SATURDAY, SUNDAY -> 5;
						case MONDAY, TUESDAY, WEDNESDAY -> {
							System.out.println("here");
							yield 7;
						}
						case THURSDAY, FRIDAY -> 14;
						default -> 22;
					};
					return i;
				}
			}

			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testConvertToSwitchExpression3() throws Exception {
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
				static int i;
				static {
					// var comment
					int j = 4;
					// logic comment
					switch (j) {
						case 0:
						case 1: i = 5; break;
						case 2:
						case 3:
						case 4:
			                System.out.println("here"); // comment 1
			                // comment 2
			                i = 7; // comment 3
			                break;
						case 5:
						case 6: i = 14; break;
						default: i = 22; break;
					}
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("switch");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
				static int i;
				static {
					// var comment
					int j = 4;
					// logic comment
					i = switch (j) {
						case 0, 1 -> 5;
						case 2, 3, 4 -> {
							System.out.println("here"); // comment 1
							// comment 2
							yield 7; // comment 3
						}
						case 5, 6 -> 14;
						default -> 22;
					};
				}
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testConvertToSwitchExpression4() throws Exception {
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
				public int foo(int j, int k) {
					// var comment
					int i;
					// logic comment
					switch (j) {
						case 0:
						case 1: i = k > 7 ? 5 : 6; break;
						case 2:
						case 3:
						case 4: System.out.println("here"); i = 7; break;
						case 5:
						case 6: i = 14; break;
						default: i = 22; break;
					}
					return i;
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("switch");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
				public int foo(int j, int k) {
					// var comment
					int i = switch (j) {
						case 0, 1 -> k > 7 ? 5 : 6;
						case 2, 3, 4 -> {
							System.out.println("here");
							yield 7;
						}
						case 5, 6 -> 14;
						default -> 22;
					};
					return i;
				}
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testConvertToSwitchExpression5() throws Exception {
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
			    public int foo(int i) {
			        switch (i) {
			        case 0: // comment
			            return 0;
			        default:
			            return 1;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("switch");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public int foo(int i) {
			        return switch (i) {
						case 0: // comment
							yield 0;
						default:
							yield 1;
					};
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testNoConvertToSwitchExpression1() throws Exception {
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
				static int i;
				static {
					// var comment
					int j = 4;
					// logic comment
					switch (j) {
						case 0: break; // no statements
						case 1: i = 5; break;
						case 2:
						case 3:
						case 4: System.out.println("here"); i = 7; break;
						case 5:
						case 6: i = 14; break;
						default: i = 22; break;
					}
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("switch");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, FixMessages.SwitchExpressionsFix_convert_to_switch_expression);

	}

	@Test
	public void testNoConvertToSwitchExpression2() throws Exception {
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
				public int foo(int k) {
					int i;
					// var comment
					int j = 4;
					// logic comment
					switch (j) {
						case 0: System.out.println("here"); // fall-through with statements
						case 1: i = 5; break;
						case 2:
						case 3:
						case 4: System.out.println("here"); i = 7; break;
						case 5:
						case 6: i = 14; break;
						default: i = 22; break;
					}
					return i;
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("switch");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, FixMessages.SwitchExpressionsFix_convert_to_switch_expression);

	}

	@Test
	public void testConvertToSwitchExpression6() throws Exception {
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
				public int foo(int k) {
					int i;
					// var comment
					int j = 4;
					// logic comment
					switch (j) {
						case 0:
						case 1: i = 5; return i; // return statement
						case 2:
						case 3:
						case 4: System.out.println("here"); i = 7; break;
						case 5:
						case 6: i = 14; break;
						default: i = 22; break;
					}
					return i;
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("switch");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
				public int foo(int k) {
					int i;
					// var comment
					int j = 4;
					// logic comment
					switch (j) {
						case 0, 1 -> {
							i = 5;
							return i; // return statement
						}
						case 2, 3, 4 -> {
							System.out.println("here");
							i = 7;
						}
						case 5, 6 -> i = 14;
						default -> i = 22;
					}
					return i;
				}
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });

	}

	@Test
	public void testConvertToSwitchExpression7() throws Exception {
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
				public int foo(int k) {
					int i;
					// var comment
					int j = 4;
					// logic comment
					switch (j) {
						case 0:
						case 1: i = 5; j = 5; break; // last statement not common assignment
						case 2:
						case 3:
						case 4: System.out.println("here"); i = 7; break;
						case 5:
						case 6: i = 14; break;
						default: i = 22; break;
					}
					return i;
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("switch");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
				public int foo(int k) {
					int i;
					// var comment
					int j = 4;
					// logic comment
					switch (j) {
						case 0, 1 -> {
							i = 5;
							j = 5;
						}
						case 2, 3, 4 -> {
							System.out.println("here");
							i = 7;
						}
						case 5, 6 -> i = 14;
						default -> i = 22;
					}
					return i;
				}
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });

	}

	@Test
	public void testConvertToSwitchExpression8() throws Exception {
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
				    public int foo(int j) {
				        // return value
				        int i = 0;
				        switch (j) {
				            case 1:
				                i = 8; // value 8
				                break; // can't refactor with no assignment to i
				            case 2:
				                i = 7; // value 7
					            break;
				            default:
				                return -1; // invalid
				        }
				        return i;
				    }
				}
				""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("switch");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		assertNoErrors(ctx);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
				package test;

				public class Cls {
				    public int foo(int j) {
				        // return value
				        int i = 0;
				        switch (j) {
							case 1 -> i = 8; // value 8
							case 2 -> i = 7; // value 7
							default -> {
								return -1; // invalid
							}
						}
				        return i;
				    }
				}
				""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });

	}

	@Test
	public void testNoConvertToSwitchExpression3() throws Exception {
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
				public int foo(int k) {
					int i;
					// var comment
					int j = 4;
					// logic comment
					switch (j) {
						case 0:
						case 1: i = 5; break;
						case 2:
						case 3:
						case 4: System.out.println("here"); i = 7; break;
						case 5:
						case 6: i = 14; break;
						// no default
					}
					return i;
				}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("switch");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, FixMessages.SwitchExpressionsFix_convert_to_switch_expression);
	}

	@Test
	public void testNoConvertToSwitchExpression4() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2728
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
			    public void f(int i) {
				    int j;
			        switch (i) {
				        case 0 -> j = 3;
				        default -> throw new AssertionError();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", str1, false, null);

		int index= str1.indexOf("switch");
		IInvocationContext ctx= getCorrectionContext(cu, index, 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(ctx, false);
		assertProposalDoesNotExist(proposals, FixMessages.SwitchExpressionsFix_convert_to_switch_expression);
	}

}

