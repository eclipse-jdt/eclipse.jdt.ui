/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.tests.core.rules.Java14ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

public class QuickFixTest14 extends QuickFixTest {
	@Rule
	public ProjectTestSetup projectSetup= new Java14ProjectTestSetup(true);

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	public static final String MODULE_INFO_FILE= "module-info.java";

	@Test
	public void testAddDefaultCaseSwitchStatement1() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			public class Cls {
			    public static void foo(Day day) {
			        switch (day) {
			        case SATURDAY, SUNDAY -> System.out.println("Weekend");
			        case MONDAY, TUESDAY, WEDNESDAY -> System.out.println("Weekday");
			        }
			    }
			}
			
			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public static void foo(Day day) {
			        switch (day) {
			        case SATURDAY, SUNDAY -> System.out.println("Weekend");
			        case MONDAY, TUESDAY, WEDNESDAY -> System.out.println("Weekday");
						default -> throw new IllegalArgumentException("Unexpected value: " + day);
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
	public void testAddDefaultCaseSwitchStatement2() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			public class Cls {
			    public static void foo(Day day) {
			        switch (day) {
			        case SATURDAY, SUNDAY: System.out.println("Weekend");
			        case MONDAY, TUESDAY, WEDNESDAY: System.out.println("Weekday");
			        }
			    }
			}
			
			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public static void foo(Day day) {
			        switch (day) {
			        case SATURDAY, SUNDAY: System.out.println("Weekend");
			        case MONDAY, TUESDAY, WEDNESDAY: System.out.println("Weekday");
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
	public void testAddDefaultCaseSwitchStatement3() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			public class Cls {
			    public static void foo(Day day) {
			        switch (day) {
			        }
			    }
			}
			
			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 7);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(1);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public static void foo(Day day) {
			        switch (day) {
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
	public void testAddMissingCaseSwitchStatement1() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			public class Cls {
			    public void bar1(Day day) {
			        switch (day) {
			            case MONDAY, FRIDAY -> System.out.println(Day.SUNDAY);
			            case TUESDAY                -> System.out.println(7);
			            case THURSDAY, SATURDAY     -> System.out.println(8);
			        }
			    }
			}
			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public void bar1(Day day) {
			        switch (day) {
			            case MONDAY, FRIDAY -> System.out.println(Day.SUNDAY);
			            case TUESDAY                -> System.out.println(7);
			            case THURSDAY, SATURDAY     -> System.out.println(8);
						case SUNDAY -> throw new UnsupportedOperationException("Unimplemented case: " + day);
						case WEDNESDAY -> throw new UnsupportedOperationException("Unimplemented case: " + day);
						default -> throw new IllegalArgumentException("Unexpected value: " + day);
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
	public void testAddDefaultCaseSwitchExpression1() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			public class Cls {
			    public static void bar3(int input) {
			        int num = switch (input) {
			        case 60, 600 -> 6;
			        case 70 -> 7;
			        case 80 -> 8;
			        case 90, 900 -> {
			            yield 9;
			        }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public static void bar3(int input) {
			        int num = switch (input) {
			        case 60, 600 -> 6;
			        case 70 -> 7;
			        case 80 -> 8;
			        case 90, 900 -> {
			            yield 9;
			        }
						default -> throw new IllegalArgumentException("Unexpected value: " + input);
			        };
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testAddDefaultCaseSwitchExpression2() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			public class Cls {
			    public static void bar4(int input) {
			        int num = switch (input) {
			        case 60, 600:
			            yield 6;
			        case 70:
			            yield 7;
			        case 80:
			            yield 8;
			        case 90, 900:
			            yield 9;
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public static void bar4(int input) {
			        int num = switch (input) {
			        case 60, 600:
			            yield 6;
			        case 70:
			            yield 7;
			        case 80:
			            yield 8;
			        case 90, 900:
			            yield 9;
						default :
							throw new IllegalArgumentException(
									"Unexpected value: " + input);
			        };
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testAddDefaultCaseSwitchExpression3() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			public class Cls {
			    public static void bar4(int input) {
			        int num = switch (input) {
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, 1, null);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public static void bar4(int input) {
			        int num = switch (input) {
						default :
							throw new IllegalArgumentException(
									"Unexpected value: " + input);
			        };
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testAddMissingCaseSwitchExpression() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			public class Cls {
			    public void bar1(Day day) {
			        int len = switch (day) {
			        case MONDAY, FRIDAY:
			            yield 6;
			        case TUESDAY:
			            yield 7;
			        case THURSDAY, SATURDAY:
			            yield 8;
			        };
			    }
			}
			enum Day {
			    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
			    public void bar1(Day day) {
			        int len = switch (day) {
			        case MONDAY, FRIDAY:
			            yield 6;
			        case TUESDAY:
			            yield 7;
			        case THURSDAY, SATURDAY:
			            yield 8;
						case SUNDAY :
							throw new UnsupportedOperationException(
									"Unimplemented case: " + day);
						case WEDNESDAY :
							throw new UnsupportedOperationException(
									"Unimplemented case: " + day);
						default :
							throw new IllegalArgumentException(
									"Unexpected value: " + day);
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
	public void testReplaceIncorrectReturnInSwitchExpressionWithYieldStatement() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.setRawClasspath(projectSetup.getDefaultClasspath(), null);
		JavaProjectHelper.set14CompilerOptions(fJProject1, false);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit(MODULE_INFO_FILE, MODULE_INFO_FILE_CONTENT, false, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String test= """
			package test;
			public class Cls {
				public static int process(int i) {
					var t = switch (i) {
						case 0 -> {
							return 99;
						}
						default ->100;
					};
					return t;
				}
			
				public static void main(String[] args) {
					System.out.println(process(1));
					System.out.println(process(0));
				}
			}""";
		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test;
			public class Cls {
				public static int process(int i) {
					var t = switch (i) {
						case 0 -> {
							yield 99;
						}
						default ->100;
					};
					return t;
				}
			
				public static void main(String[] args) {
					System.out.println(process(1));
					System.out.println(process(0));
				}
			}""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}

	@Test
	public void testReplaceIncorrectReturnInSwitchExpressionWithYieldStatement2() throws Exception {
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
		String test= """
			package test;
			public class Cls {
			
				public static int process(int i) {
					var t = switch (i) {
				        case 0:
				             return 1; // Error - Quick Fix works only if the return statement is surrounded in curly braces
				        default:
			                     yield 100;
					};
			       System.out.println(t);
					return t;
				}
				public static void main(String[] args) {
					process(1);
					process(0);
					process(2);
				}
			}""";

		ICompilationUnit cu= pack.createCompilationUnit("Cls.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		String expected= """
			package test;
			public class Cls {
			
				public static int process(int i) {
					var t = switch (i) {
				        case 0:
				             yield 1;
				        default:
			                     yield 100;
					};
			       System.out.println(t);
					return t;
				}
				public static void main(String[] args) {
					process(1);
					process(0);
					process(2);
				}
			}""";

		assertEqualStringsIgnoreOrder(new String[] { preview }, new String[] { expected });
	}
}
