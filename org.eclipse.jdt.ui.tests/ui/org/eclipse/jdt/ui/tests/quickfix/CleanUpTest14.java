/*******************************************************************************
 * Copyright (c) 2020, 2023 Red Hat Inc. and others.
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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java14ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 14.
 */
public class CleanUpTest14 extends CleanUpTestCase {

	@Rule
	public ProjectTestSetup projectSetup= new Java14ProjectTestSetup(false);

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testConvertToSwitchExpressionMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public int foo(int j) {
			        // return value
			        int i;
			        // logic comment
			        switch (j) {
			            case 1:
			            case 2:
			                System.out.println("here"); // comment 1
			                // comment 2
			                i = 7; // comment 3
			            break;
			            case 3: throw new RuntimeException(); // throw comment
			            default:
			                i = 8; // value 8
			        }
			        return i;
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			
			public class E {
			    public int foo(int j) {
			        // return value
			        int i = switch (j) {
			            case 1, 2 -> {
			                System.out.println("here"); // comment 1
			                // comment 2
			                yield 7; // comment 3
			            }
			            case 3 -> throw new RuntimeException(); // throw comment
			            default -> 8; // value 8
			        };
			        return i;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    private int i;
			    public void foo(int j) {
			        // logic comment
			        switch (j) {
			            case 1:
			            case 2:
			                System.out.println("here");
			                // comment 1
			                i = 7; // comment 2
			            break;
			            default:
			                i = 8; // value 8
			            break;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			
			public class E {
			    private int i;
			    public void foo(int j) {
			        // logic comment
			        i = switch (j) {
			            case 1, 2 -> {
			                System.out.println("here");
			                // comment 1
			                yield 7; // comment 2
			            }
			            default -> 8; // value 8
			        };
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionStaticInitializer() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Random;
			public class E {
			    private static int i;
			    static {
			        Random rand= new Random();
			        int j = rand.nextInt(10);
			        // logic comment
			        switch (j) {
			            case 1:
			            case 2:
			                System.out.println("here");
			                // comment 2
			                i = 7; // comment 3
			            break;
			            default:
			                i = 8; // value 8
			            break;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			import java.util.Random;
			public class E {
			    private static int i;
			    static {
			        Random rand= new Random();
			        int j = rand.nextInt(10);
			        // logic comment
			        i = switch (j) {
			            case 1, 2 -> {
			                System.out.println("here");
			                // comment 2
			                yield 7; // comment 3
			            }
			            default -> 8; // value 8
			        };
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionEnumsNoDefault() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public enum Day {
			        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			    }
			    public int foo(Day day) {
			        // return value
			        int i = 0;
			        // logic comment
			        switch (day) {
			            case SATURDAY:
			            case SUNDAY:
			                i = 5;
			            break;
			            case MONDAY:
			            case TUESDAY:
			            case WEDNESDAY:
			                i = 7;
			            break;
			            case THURSDAY:
			            case FRIDAY:
			                i = 14;
			            break;
			        }
			        return i;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			
			public class E {
			    public enum Day {
			        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
			    }
			    public int foo(Day day) {
			        // return value
			        int i = 0;
			        // logic comment
			        i = switch (day) {
			            case SATURDAY, SUNDAY -> 5;
			            case MONDAY, TUESDAY, WEDNESDAY -> 7;
			            case THURSDAY, FRIDAY -> 14;
			        };
			        return i;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionBug574824() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public void foo(String[] args) {
			        // comment 1
			        final File file;
			        switch (args[1]) {
			            case "foo":
			                file = new File("foo.txt");
			                break;
			            case "bar":
			                file = new File("bar.txt");
			                break;
			            default:
			                file = new File("foobar.txt");
			        }
			        System.err.println(file);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public void foo(String[] args) {
			        // comment 1
			        final File file = switch (args[1]) {
			            case "foo" -> new File("foo.txt");
			            case "bar" -> new File("bar.txt");
			            default -> new File("foobar.txt");
			        };
			        System.err.println(file);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionBug578130() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public void foo(String[] args) throws Exception {
			        boolean isWhiteSpace;
			        switch (args[0].charAt(0)) {
			            case 10: /* \\ u000a: LINE FEED */
			            case 12: /* \\ u000c: FORM FEED */
			            case 13: /* \\ u000d: CARRIAGE RETURN */
			            case 32: /* \\ u0020: SPACE */
			            case 9: /* \\ u0009: HORIZONTAL TABULATION */
			                isWhiteSpace = true; /* comment x */
			                break;
			            case 0:
			            	   throw new Exception("invalid char"); //$NON-NLS-1$
			            case 95:
			            {
			                System.out.println("here"); //$NON-NLS-1$
			            	   isWhiteSpace = false;
			            }
			            break;
			            default:
			                isWhiteSpace = false;
			        }
			        System.out.println(isWhiteSpace);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public void foo(String[] args) throws Exception {
			        boolean isWhiteSpace = switch (args[0].charAt(0)) {
			            case 10: /* \\ u000a: LINE FEED */
			            case 12: /* \\ u000c: FORM FEED */
			            case 13: /* \\ u000d: CARRIAGE RETURN */
			            case 32: /* \\ u0020: SPACE */
			            case 9: /* \\ u0009: HORIZONTAL TABULATION */
			                yield true; /* comment x */
			            case 0:
			                throw new Exception("invalid char"); //$NON-NLS-1$
			            case 95: {
			                System.out.println("here"); //$NON-NLS-1$
			                yield false;
			            }
			            default:
			                yield false;
			        };
			        System.out.println(isWhiteSpace);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionBug578129_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public void foo(String[] args) throws Exception {
			        boolean isWhiteSpace;
			        switch (args[0].charAt(0)) {
			            case 10:
			            case 12:
			            case 13:
			            case 32:
			            case 9:
			                isWhiteSpace = true; /* comment x */
			                break;
			            case 0:
			            	   throw new Exception("invalid char"); //$NON-NLS-1$
			            case 95:
			            default:
			                isWhiteSpace = false;
			        }
			        System.out.println(isWhiteSpace);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public void foo(String[] args) throws Exception {
			        boolean isWhiteSpace = switch (args[0].charAt(0)) {
			            case 10, 12, 13, 32, 9 -> true; /* comment x */
			            case 0 -> throw new Exception("invalid char"); //$NON-NLS-1$
			            case 95 -> false;
			            default -> false;
			        };
			        System.out.println(isWhiteSpace);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionBug578129_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public void foo(String[] args) throws Exception {
			        boolean isWhiteSpace;
			        switch (args[0].charAt(0)) {
			            case 10:
			            case 12:
			            case 13:
			            case 32:
			            case 9:
			                // comment 1
			                isWhiteSpace = true; /* comment x */
			                break;
			            case 0:
			            	   throw new Exception("invalid char"); //$NON-NLS-1$
			            case 95:
			            default: {
			                System.out.println("non-whitespace");
			                isWhiteSpace = false;
			            }
			        }
			        System.out.println(isWhiteSpace);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public void foo(String[] args) throws Exception {
			        boolean isWhiteSpace = switch (args[0].charAt(0)) {
			            case 10, 12, 13, 32, 9 -> /* comment 1 */ true; /* comment x */
			            case 0 -> throw new Exception("invalid char"); //$NON-NLS-1$
			            case 95 -> {
			                System.out.println("non-whitespace");
			                yield false;
			            }
			            default -> {
			                System.out.println("non-whitespace");
			                yield false;
			            }
			        };
			        System.out.println(isWhiteSpace);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToReturnSwitchExpressionIssue104_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public enum InnerEnum {
			        A, B, C, D;
			    }
			    public int foo(InnerEnum k) {
			        switch (k) {
			            case A:
			            case B:
			                /* comment 1 */
			                return 6; /* abc */
			            case C: {
			                System.out.println("x"); //$NON-NLS-1$
			                /* comment 2 */
			                return 8; /* def */
			            }
			            case D:
			                // comment 3
			                return 9;
			            default:
			                throw new NullPointerException();
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public enum InnerEnum {
			        A, B, C, D;
			    }
			    public int foo(InnerEnum k) {
			        return switch (k) {
			            case A, B -> /* comment 1 */ 6; /* abc */
			            case C -> {
			                System.out.println("x"); //$NON-NLS-1$
			                /* comment 2 */
			                yield 8; /* def */
			            }
			            case D -> /* comment 3 */ 9;
			            default -> throw new NullPointerException();
			        };
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToReturnSwitchExpressionIssue104_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public enum InnerEnum {
			        A, B, C, D;
			    }
			    public int foo(InnerEnum k) {
			        switch (k) {
			            case A:
			            case B:
			                /* comment 1 */
			                return 6; /* abc */
			            case C:
			                System.out.println("x"); //$NON-NLS-1$
			                /* comment 2 */
			                return 8; /* def */
			            case D:
			                return 9;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public enum InnerEnum {
			        A, B, C, D;
			    }
			    public int foo(InnerEnum k) {
			        return switch (k) {
			            case A, B -> /* comment 1 */ 6; /* abc */
			            case C -> {
			                System.out.println("x"); //$NON-NLS-1$
			                /* comment 2 */
			                yield 8; /* def */
			            }
			            case D -> 9;
			        };
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionIssue380() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    public void bar() {
			    }
			
			    public int foo(int i) {
			        switch (i) {
			        case 0:
			            return 0;
			        default:
			            bar(); //
			            throw new AssertionError();
			        }
			    }
			}
			"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			public class E {
			    public void bar() {
			    }
			
			    public int foo(int i) {
			        return switch (i) {
			            case 0 -> 0;
			            default -> {
			                bar(); //
			                throw new AssertionError();
			            }
			        };
			    }
			}
			"""; //
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToSwitchExpressionIssue388() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    public void bar() {
			    }
			
			    public int foo(int i) {
			        switch (i) {
			        case 0: // comment
			            return 0;
			        default:
			            return 1;
			        }
			    }
			}
			"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		sample= """
			package test1;
			public class E {
			    public void bar() {
			    }
			
			    public int foo(int i) {
			        return switch (i) {
			            case 0: // comment
			                yield 0;
			            default:
			                yield 1;
			        };
			    }
			}
			"""; //
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testDoNotConvertToReturnSwitchExpressionIssue104_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public enum InnerEnum {
			        A, B, C, D;
			    }
			    public int foo(InnerEnum k) {
			        switch (k) {
			            case A:
			                System.out.println("a");
			            case B:
			                /* comment 1 */
			                return 6; /* abc */
			            case C: {
			                System.out.println("x"); //$NON-NLS-1$
			                /* comment 2 */
			                return 8; /* def */
			            }
			            case D:
			                return 9;
			            default:
			                throw new NullPointerException();
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToReturnSwitchExpressionIssue104_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.io.File;
			
			public class E {
			    public enum InnerEnum {
			        A, B, C, D;
			    }
			    public int foo(InnerEnum k, int x) {
			        switch (k) {
			            case A:
			                System.out.println("a");
			            case B:
			                /* comment 1 */
			                if (x > 3)
			                    return 6; /* abc */
			                else
			                    return 10;
			            case C: {
			                System.out.println("x"); //$NON-NLS-1$
			                /* comment 2 */
			                return 8; /* def */
			            }
			            case D:
			                return 9;
			            default:
			                throw new NullPointerException();
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionNoBreak() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public int foo(int j) {
			        // return value
			        int i;
			        switch (j) {
			            case 1:
			                i = 8; // can't refactor with no break
			            case 2:
			                i = 7; // value 7
			            break;
			            default:
			                i = 8; // value 8
			            break;
			        }
			        return i;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionNoStatements() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public int foo(int j) {
			        // return value
			        int i = 0;
			        switch (j) {
			            case 1:
			                break; // can't refactor with no statements
			            case 2:
			                i = 7; // value 7
			            break;
			            default:
			                i = 8; // value 8
			            break;
			        }
			        return i;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionNoAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public int foo(int j) {
			        // return value
			        int i = 0;
			        switch (j) {
			            case 1:
			                System.out.println("here");
			                break; // can't refactor with no assignment to i
			            case 2:
			                i = 7; // value 7
			            break;
			            default:
			                i = 8; // value 8
			            break;
			        }
			        return i;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionNoLastAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public int foo(int j) {
			        // return value
			        int i = 0;
			        switch (j) {
			            case 1:
			                i = 6; // assignment not last statement
			                System.out.println("here");
			                break;
			            case 2:
			                i = 7; // value 7
			            break;
			            default:
			                i = 8; // value 8
			            break;
			        }
			        return i;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionIfElse() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public int foo(int j, int k) {
			        // return value
			        int i;
			        switch (j) {
			            case 1:
			                if (k < 4) { // we don't delve into control statements
			                    i = 6;
			                } else {
			                    i = 9;
			                }
			                break;
			            case 2:
			                i = 7; // value 7
			            break;
			            default:
			                i = 8; // value 8
			            break;
			        }
			        return i;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionWithTry() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public int foo(int j, int k) {
			        // return value
			        int i;
			        switch (j) {
			            case 1:
			                try { // we don't delve into try statements
			                    i = 6;
			                } finally {
			                    i = 9;
			                }
			                break;
			            case 2:
			                i = 7; // value 7
			            break;
			            default:
			                i = 8; // value 8
			            break;
			        }
			        return i;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionReturn() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public int foo(int j) {
			        // return value
			        int i;
			        switch (j) {
			            case 1:
			                return 6; // we don't support return
			            case 2:
			                i = 7; // value 7
			            break;
			            default:
			                i = 8; // value 8
			            break;
			        }
			        return i;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionBug578128() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public static void main(String[] args) {
			        boolean rulesOK = true;
			        switch (args[0].charAt(0)) {
			            case '+':
			                args[0] = "+";
			                break;
			            case '~':
			                args[0] = "~";
			                break;
			            case '-':
			                args[0] = "-";
			                break;
			            case '?':
			                args[0] = "?";
			                break;
			            default:
			                rulesOK = false;
			        }
			        System.out.println(rulesOK);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotConvertToSwitchExpressionIssue381() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public void f(int i) {
			        switch (i) {
			        case 0:
			            return;
			        default:
			            throw new AssertionError();
			        }
			    }
			}
			"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

}
