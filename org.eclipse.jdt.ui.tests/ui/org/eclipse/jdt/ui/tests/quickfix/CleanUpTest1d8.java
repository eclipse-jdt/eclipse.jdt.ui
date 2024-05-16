/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class CleanUpTest1d8 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java1d8ProjectTestSetup();

	@Override
	public void setUp() throws Exception {
		super.setUp();
		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "", null);
	}

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testConvertToLambda01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    void foo(){
			        // Keep this comment
			        Runnable r = new Runnable() {
			            @Override
			            public void run() {
			                System.out.println("do something");
			            }
			        };
			    };
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test;
			public class E {
			    void foo(){
			        // Keep this comment
			        Runnable r = () -> System.out.println("do something");
			    };
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original },
				new HashSet<>(Arrays.asList(FixMessages.LambdaExpressionsFix_convert_to_anonymous_class_creation)));
	}

	@Test
	public void testConvertToLambda02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    void foo(){
			        Runnable r1 = new Runnable() {
			            @Override
			            public void run() {
			                System.out.println("do something");
			            }
			        };
			        Runnable r2 = new Runnable() {
			            @Override
			            public void run() {
			                System.out.println("do one thing");
			                System.out.println("do another thing");
			            }
			        };
			    };
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test;
			public class E {
			    void foo(){
			        Runnable r1 = () -> System.out.println("do something");
			        Runnable r2 = () -> {
			            System.out.println("do one thing");
			            System.out.println("do another thing");
			        };
			    };
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression)));

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original },
				new HashSet<>(Arrays.asList(FixMessages.LambdaExpressionsFix_convert_to_anonymous_class_creation)));
	}

	@Test
	public void testConvertToLambda03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test1;
			import java.util.function.Supplier;
			class E {
			    Supplier<Supplier<String>> s= new Supplier<Supplier<String>>() {
			        @Override
			        public Supplier<String> get() {
			            return new Supplier<String>() {
			                @Override
			                public String get() {
			                    return "a";
			                }
			            };
			        }
			    };
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test1;
			import java.util.function.Supplier;
			class E {
			    Supplier<Supplier<String>> s= () -> () -> "a";
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
	}

	@Test
	public void testConvertToLambda04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    private class K {
			        public void routine(int i) {
			        }
			    }
			    private interface J {
			        public void routine(K k, int i //
			    }
			    public void foo2() {
			    }
			    public void foo() {
			        Runnable r = new Runnable() {
			            @Override
			            public void run() {
			                foo2();
			            }
			        };
			        J c = new J() {
			            @Override
			            public void routine(K k, int i) {
			                k.routine(i);
			            }
			        };
			    }
			}
			"""; //
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);
		disable(CleanUpConstants.ALSO_SIMPLIFY_LAMBDA);

		sample= """
			package test1;
			public class E {
			    private class K {
			        public void routine(int i) {
			        }
			    }
			    private interface J {
			        public void routine(K k, int i //
			    }
			    public void foo2() {
			    }
			    public void foo() {
			        Runnable r = () -> foo2();
			        J c = (k, i) -> k.routine(i);
			    }
			}
			"""; //
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
	}

	@Test
	public void testConvertToLambda05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    private class K {
			        public void routine(int i) {
			        }
			    }
			    private interface J {
			        public void routine(K k, int i //
			    }
			    public void foo2() {
			    }
			    public void foo() {
			        Runnable r = new Runnable() {
			            @Override
			            public void run() {
			                foo2();
			            }
			        };
			        J c = new J() {
			            @Override
			            public void routine(K k, int i) {
			                k.routine(i);
			            }
			        };
			    }
			}
			"""; //
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.ALSO_SIMPLIFY_LAMBDA);

		sample= """
			package test1;
			public class E {
			    private class K {
			        public void routine(int i) {
			        }
			    }
			    private interface J {
			        public void routine(K k, int i //
			    }
			    public void foo2() {
			    }
			    public void foo() {
			        Runnable r = this::foo2;
			        J c = K::routine;
			    }
			}
			"""; //
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToLambda06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    private class K {
			        public void routine(int i) {
			        }
			    }
			    private interface J {
			        public void routine(K k, int i //
			    }
			    public void foo2() {
			    }
			    public void foo() {
			        Runnable r = new Runnable() {
			            @Override
			            public void run() {
			                foo2();
			            }
			        };
			        J c = new J() {
			            @Override
			            public void routine(K k, int i) {
			                k.routine(i);
			            }
			        };
			    }
			}
			"""; //
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);
		disable(CleanUpConstants.ALSO_SIMPLIFY_LAMBDA);
		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		sample= """
			package test1;
			public class E {
			    private class K {
			        public void routine(int i) {
			        }
			    }
			    private interface J {
			        public void routine(K k, int i //
			    }
			    public void foo2() {
			    }
			    public void foo() {
			        Runnable r = this::foo2;
			        J c = K::routine;
			    }
			}
			"""; //
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToLambda07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E {
			    private interface Blah {
			        public boolean isCorrect(Object z //
			    }
			    public boolean foo() {
			        Blah x = new Blah() {
			            @Override
			            public boolean isCorrect(Object z) {
			                return z instanceof String;
			            }
			        }; // comment 1
			        return x.isCorrect(this //
			    }
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.ALSO_SIMPLIFY_LAMBDA);

		sample= """
			package test1;
			public class E {
			    private interface Blah {
			        public boolean isCorrect(Object z //
			    }
			    public boolean foo() {
			        Blah x = String.class::isInstance; // comment 1
			        return x.isCorrect(this //
			    }
			}
			"""; //
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToLambda08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class C1 {
			    interface IOverwriteQuery {
			        String ALL = "ALL";
			
			        String queryOverwrite(String pathString);
			    }
			
			    class ImportOperation {
			        public ImportOperation(IOverwriteQuery query) {
			        }
			    }
			
			    public C1() {
			        ImportOperation io = new ImportOperation(new IOverwriteQuery() {
			
			            @Override
			            public String queryOverwrite(String pathString) {
			                return ALL;
			            }
			
			        });
			    }
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("C1.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test1;
			public class C1 {
			    interface IOverwriteQuery {
			        String ALL = "ALL";
			
			        String queryOverwrite(String pathString);
			    }
			
			    class ImportOperation {
			        public ImportOperation(IOverwriteQuery query) {
			        }
			    }
			
			    public C1() {
			        ImportOperation io = new ImportOperation(pathString -> IOverwriteQuery.ALL);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToLambdaWithConstant() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			public class E {
			    @FunctionalInterface
			    interface FI1 extends Runnable {
			        int CONSTANT_VALUE = 123;
			    }
			
			    void foo() {
			        Runnable r = new FI1() {
			            @Override
			            public void run() {
			                System.out.println(CONSTANT_VALUE);
			            }
			        };
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test;
			
			public class E {
			    @FunctionalInterface
			    interface FI1 extends Runnable {
			        int CONSTANT_VALUE = 123;
			    }
			
			    void foo() {
			        Runnable r = () -> System.out.println(FI1.CONSTANT_VALUE);
			    };
			}
			""";
		String expected= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.LambdaExpressionsFix_convert_to_lambda_expression)));
	}

	@Test
	public void testConvertToLambdaNestedWithImports() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.concurrent.Callable;
			import java.util.concurrent.Executors;
			public class E {
			    void foo() {
			        new Thread(new Runnable() {
			            @Override
			            public void run() {
			                Executors.newSingleThreadExecutor().submit(new Callable<String>() {
			                    @Override
			                    public String call() throws Exception {
			                        return "hi";
			                    }
			                });
			            }
			        });
			    }
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test;
			import java.util.concurrent.Executors;
			public class E {
			    void foo() {
			        new Thread(() -> Executors.newSingleThreadExecutor().submit(() -> "hi"));
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=434507#c5
	@Test
	public void testConvertToLambdaAmbiguous01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			interface ISuper {
			    void foo(FI1 fi1);
			}
			
			interface ISub extends ISuper {
			    void foo(FI2 fi2);
			}
			
			@FunctionalInterface
			interface FI1 {
			    void abc();
			}
			
			@FunctionalInterface
			interface FI2 {
			    void xyz();
			}
			
			class Test1 {
			    private void test1() {
			        f1().foo(new FI1() {
			            @Override
			            public void abc() {
			                System.out.println();
			            }
			        });
			
			    }
			   \s
			    private ISub f1() {
			        return null;
			    }
			}
			
			abstract class Test2 implements ISub {
			    private void test2() {
			        foo(new FI1() {
			            @Override
			            public void abc() {
			                System.out.println();
			            }
			        });
			    }
			}
			
			class Test3 {
			    void foo(FI1 fi1) {}
			    void foo(FI2 fi2) {}
			    private void test3() {
			        foo(new FI1() {
			            @Override
			            public void abc() {
			                System.out.println();
			            }
			        });
			    }
			}
			
			class Outer {
			    class Test4 {
			        {
			            bar(0, new FI1() {
			                @Override
			                public void abc() {
			                }
			            });
			        }
			    }
			    void bar(int i, FI1 fi1) {}
			    void bar(int s, FI2 fi2) {}
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test;
			
			interface ISuper {
			    void foo(FI1 fi1);
			}
			
			interface ISub extends ISuper {
			    void foo(FI2 fi2);
			}
			
			@FunctionalInterface
			interface FI1 {
			    void abc();
			}
			
			@FunctionalInterface
			interface FI2 {
			    void xyz();
			}
			
			class Test1 {
			    private void test1() {
			        f1().foo((FI1) () -> System.out.println());
			
			    }
			   \s
			    private ISub f1() {
			        return null;
			    }
			}
			
			abstract class Test2 implements ISub {
			    private void test2() {
			        foo((FI1) () -> System.out.println());
			    }
			}
			
			class Test3 {
			    void foo(FI1 fi1) {}
			    void foo(FI2 fi2) {}
			    private void test3() {
			        foo((FI1) () -> System.out.println());
			    }
			}
			
			class Outer {
			    class Test4 {
			        {
			            bar(0, (FI1) () -> {
			            });
			        }
			    }
			    void bar(int i, FI1 fi1) {}
			    void bar(int s, FI2 fi2) {}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=434507#c5
	@Test
	public void testConvertToLambdaAmbiguous02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			@FunctionalInterface
			interface FI1 {
			    void abc();
			}
			
			@FunctionalInterface
			interface FI2 {
			    void xyz();
			}
			
			class Outer {
			    void outer(FI1 fi1) {}
			}
			class OuterSub extends Outer {
			    OuterSub() {
			        super.outer(new FI1() {
			            @Override
			            public void abc() {
			                System.out.println();
			            }
			        });
			    }
			    class Test1 {
			        private void test1() {
			            OuterSub.super.outer(new FI1() {
			                @Override
			                public void abc() {
			                    System.out.println();
			                }
			            });
			            OuterSub.this.outer(new FI1() {
			                @Override
			                public void abc() {
			                    System.out.println();
			                }
			            });
			            outer(new FI1() {
			                @Override
			                public void abc() {
			                    System.out.println();
			                }
			            });
			        }
			    }
			    @Override
			    void outer(FI1 fi1) {}
			    void outer(FI2 fi2) {}
			}
			
			class OuterSub2 extends OuterSub {
			    OuterSub2() {
			        super.outer(new FI1() {
			            @Override
			            public void abc() {
			                System.out.println();
			            }
			        });
			    }
			    class Test2 {
			        private void test2() {
			            OuterSub2.super.outer(new FI1() {
			                @Override
			                public void abc() {
			                    System.out.println();
			                }
			            });
			            OuterSub2.this.outer(new FI1() {
			                @Override
			                public void abc() {
			                    System.out.println();
			                }
			            });
			            outer(new FI1() {
			                @Override
			                public void abc() {
			                    System.out.println();
			                }
			            });
			        }
			    }
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test;
			
			@FunctionalInterface
			interface FI1 {
			    void abc();
			}
			
			@FunctionalInterface
			interface FI2 {
			    void xyz();
			}
			
			class Outer {
			    void outer(FI1 fi1) {}
			}
			class OuterSub extends Outer {
			    OuterSub() {
			        super.outer(() -> System.out.println());
			    }
			    class Test1 {
			        private void test1() {
			            OuterSub.super.outer(() -> System.out.println());
			            OuterSub.this.outer((FI1) () -> System.out.println());
			            outer((FI1) () -> System.out.println());
			        }
			    }
			    @Override
			    void outer(FI1 fi1) {}
			    void outer(FI2 fi2) {}
			}
			
			class OuterSub2 extends OuterSub {
			    OuterSub2() {
			        super.outer((FI1) () -> System.out.println());
			    }
			    class Test2 {
			        private void test2() {
			            OuterSub2.super.outer((FI1) () -> System.out.println());
			            OuterSub2.this.outer((FI1) () -> System.out.println());
			            outer((FI1) () -> System.out.println());
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=434507#c2
	@Test
	public void testConvertToLambdaAmbiguous03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public interface E {
			    default void m() {
			        bar(0, new FI() {
			            @Override
			            public int foo(int x) {
			                return x++;
			            }
			        });
			        baz(0, new ZI() {
			            @Override
			            public int zoo() {
			                return 1;
			            }
			        });
			    }
			
			    void bar(int i, FI fi);
			    void bar(int i, FV fv);
			
			    void baz(int i, ZI zi);
			    void baz(int i, ZV zv);
			}
			
			@FunctionalInterface interface FI { int  foo(int a); }
			@FunctionalInterface interface FV { void foo(int a); }
			
			@FunctionalInterface interface ZI { int  zoo(); }
			@FunctionalInterface interface ZV { void zoo(); }
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test;
			public interface E {
			    default void m() {
			        bar(0, (FI) x -> x++);
			        baz(0, () -> 1);
			    }
			
			    void bar(int i, FI fi);
			    void bar(int i, FV fv);
			
			    void baz(int i, ZI zi);
			    void baz(int i, ZV zv);
			}
			
			@FunctionalInterface interface FI { int  foo(int a); }
			@FunctionalInterface interface FV { void foo(int a); }
			
			@FunctionalInterface interface ZI { int  zoo(); }
			@FunctionalInterface interface ZV { void zoo(); }
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_LAMBDA);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
	}

	@Test
	public void testConvertToLambdaConflictingNames() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test1;
			
			interface FI {
			    void run(int x);
			}
			
			public class Test {
			    {
			        int e;
			        FI fi = new FI() {
			            @Override
			            public void run(int e) {
			                class C1 {
			                    void init1() {
			                        m(new FI() {
			                            @Override
			                            public void run(int e) {
			                                FI fi = new FI() {
			                                    @Override
			                                    public void run(int e) {
			                                        FI fi = new FI() {
			                                            @Override
			                                            public void run(int e) {
			                                                return;
			                                            }
			                                        };
			                                    }
			                                };
			                            }
			                        });
			                    }
			
			                    void init2() {
			                        m(new FI() {
			                            @Override
			                            public void run(int e) {
			                                new FI() {
			                                    @Override
			                                    public void run(int e3) {
			                                        FI fi = new FI() {
			                                            @Override
			                                            public void run(int e) {
			                                                return;
			                                            }
			                                        };
			                                    }
			                                };
			                            }
			                        });
			                    }
			                }
			            }
			        };
			    }
			
			    void m(FI fi) {
			    };
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test1;
			
			interface FI {
			    void run(int x);
			}
			
			public class Test {
			    {
			        int e;
			        FI fi = e4 -> {
			            class C1 {
			                void init1() {
			                    m(e3 -> {
			                        FI fi2 = e2 -> {
			                            FI fi1 = e1 -> {
			                                return;
			                            };
			                        };
			                    });
			                }
			
			                void init2() {
			                    m(e2 -> new FI() {
			                        @Override
			                        public void run(int e3) {
			                            FI fi = e1 -> {
			                                return;
			                            };
			                        }
			                    });
			                }
			            }
			        };
			    }
			
			    void m(FI fi) {
			    };
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToLambdaNoRenameLocals() throws CoreException {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String original= """
			package test;
			
			interface FI {
			    void doIt(String p);
			}
			public class C1 {
			    public void foo() {
			        FI fi= new FI() {
			            @Override
			            public void doIt(String e) {
			                if (e != null) {
			                    int i= 0;
			                    System.out.println(i);
			                } else {
			                    int i= 0;
			                    System.out.println(i);
			                }
			            }
			        };
			    }
			}
			""";

		String fixed= """
			package test;
			
			interface FI {
			    void doIt(String p);
			}
			public class C1 {
			    public void foo() {
			        FI fi= e -> {
			            if (e != null) {
			                int i= 0;
			                System.out.println(i);
			            } else {
			                int i= 0;
			                System.out.println(i);
			            }
			        };
			    }
			}
			""";

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);
		ICompilationUnit cu= pack.createCompilationUnit("C1.java", original, false, null);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { fixed }, null);
	}

	@Test
	public void testConvertToLambdaWithRenameLocals() throws CoreException {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String original= """
			package test1;
			interface FI {
			    void doIt(String p);
			}
			public class C1 {
			    public void foo() {
			        int i= 33;
			        FI fi = new FI() {
			            @Override
			            public void doIt(String e) {
			                FI fi = new FI() {
			                    @Override
			                    public void doIt(String e) {
			                        int i1= 37;
			                        if (e != null) {
			                            int i = 0;
			                            System.out.println(i);
			                        } else {
			                            int i = 0;
			                            System.out.println(i);
			                        }
			                    }
			                };
			            }
			        };
			    }
			}
			""";


		String fixed= """
			package test1;
			interface FI {
			    void doIt(String p);
			}
			public class C1 {
			    public void foo() {
			        int i= 33;
			        FI fi = e -> {
			            FI fi1 = e1 -> {
			                int i1= 37;
			                if (e1 != null) {
			                    int i2 = 0;
			                    System.out.println(i2);
			                } else {
			                    int i3 = 0;
			                    System.out.println(i3);
			                }
			            };
			        };
			    }
			}
			""";

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);
		ICompilationUnit cu= pack.createCompilationUnit("C1.java", original, false, null);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { fixed }, null);

	}

	@Test
	public void testConvertToLambdaWithMethodAnnotations() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class C1 {
			    Runnable r1 = new Runnable() {
			        @Override @A @Deprecated
			        public void run() {
			        }
			    };
			    Runnable r2 = new Runnable() {
			        @Override @Deprecated
			        public void run() {
			        }
			    };
			}
			@interface A {}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("C1.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test;
			public class C1 {
			    Runnable r1 = new Runnable() {
			        @Override @A @Deprecated
			        public void run() {
			        }
			    };
			    Runnable r2 = () -> {
			    };
			}
			@interface A {}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToAnonymousWithWildcards() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class E {
			    void foo(Integer[] ints){
			        Arrays.sort(ints, (i1, i2) -> i1 - i2);
			        Comparator<?> cw = (w1, w2) -> 0;
			        Comparator cr = (r1, r2) -> 0;
			        Comparator<? extends Number> ce = (n1, n2) -> -0;
			    };
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		sample= """
			package test;
			import java.util.*;
			public class E {
			    void foo(Integer[] ints){
			        Arrays.sort(ints, new Comparator<Integer>() {
			            @Override
			            public int compare(Integer i1, Integer i2) {
			                return i1 - i2;
			            }
			        });
			        Comparator<?> cw = new Comparator<Object>() {
			            @Override
			            public int compare(Object w1, Object w2) {
			                return 0;
			            }
			        };
			        Comparator cr = new Comparator() {
			            @Override
			            public int compare(Object r1, Object r2) {
			                return 0;
			            }
			        };
			        Comparator<? extends Number> ce = new Comparator<Number>() {
			            @Override
			            public int compare(Number n1, Number n2) {
			                return -0;
			            }
			        };
			    };
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
	}

	@Test
	public void testConvertToAnonymousWithWildcards1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			interface I<M> {
			    M run(M x);
			}
			
			class Test {
			    I<?> li = s -> null;
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		sample= """
			package test;
			
			interface I<M> {
			    M run(M x);
			}
			
			class Test {
			    I<?> li = new I<Object>() {
			        @Override
			        public Object run(Object s) {
			            return null;
			        }
			    };
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
	}

	@Test
	public void testConvertToAnonymousWithJoinedSAM() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=428526#c1 and #c6
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			interface Foo<T, N extends Number> {
			    void m(T t);
			    void m(N n);
			}
			interface Baz extends Foo<Integer, Integer> {}
			class Test {
			    Baz baz = x -> { return; };
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);

		sample= """
			package test;
			
			interface Foo<T, N extends Number> {
			    void m(T t);
			    void m(N n);
			}
			interface Baz extends Foo<Integer, Integer> {}
			class Test {
			    Baz baz = new Baz() {
			        @Override
			        public void m(Integer x) { return; }
			    };
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		disable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
	}

	@Test
	public void testConvertToLambdaWithNonFunctionalTargetType() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=468457
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			public class Snippet {
			    void test(Interface context) {
			        context.set("bar", new Runnable() {
			            @Override
			            public void run() {}
			        });
			       \s
			    }   \s
			}
			
			interface Interface {
			    public void set(String name, Object value);
			}
			""";
		String original= sample;
		ICompilationUnit cu1= pack1.createCompilationUnit("Snippet.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		sample= """
			package test;
			
			public class Snippet {
			    void test(Interface context) {
			        context.set("bar", (Runnable) () -> {});
			       \s
			    }   \s
			}
			
			interface Interface {
			    public void set(String name, Object value);
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);

		enable(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION);
		disable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { original }, null);
	}

	@Test
	public void testConvertToLambdaWithSynchronizedOrStrictfp() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class C1 {
			    Runnable run1 = new Runnable() {
			        @Override
			        public synchronized void run() {
			        }
			    };
			    Runnable run2 = new Runnable() {
			        @Override
			        public strictfp void run() {
			        }
			    };
			}
			""";
		String original= sample;
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", original, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=560018
	@Test
	public void testConvertToLambdaInFieldInitializerWithFinalFieldReference() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			public class C1 {
			    final String s;
			
			    Runnable run1 = new Runnable() {
			        @Override
			        public void run() {
			            System.out.println(s);
			        }
			    };
			
			    public C1() {
			        s = "abc";
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=560018
	@Test
	public void testConvertToLambdaInFieldInitializerWithFinalFieldReference2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class C1 {
			    final String s = "abc";
			    Runnable run1 = new Runnable() {
			        @Override
			        public void run() {
			            System.out.println(s);
			        }
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		String expected1= """
			package test;
			public class C1 {
			    final String s = "abc";
			    Runnable run1 = () -> System.out.println(s);
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected1 }, null);
	}

	@Test
	public void testConvertToLambdaAndQualifyNextField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			public class C1 {
			    static final String previousField = "abc";
			
			    Runnable run1 = new Runnable() {
			        @Override
			        public void run() {
			            System.out.println(previousField + instanceField + classField + getString());
			        }
			    };
			
			    static final String classField = "abc";
			    final String instanceField = "abc";
			    public String getString() {
			        return "";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		String expected= """
			package test;
			
			public class C1 {
			    static final String previousField = "abc";
			
			    Runnable run1 = () -> System.out.println(previousField + this.instanceField + C1.classField + getString());
			
			    static final String classField = "abc";
			    final String instanceField = "abc";
			    public String getString() {
			        return "";
			    }
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, null);
	}

	@Test
	public void testConvertToLambdaWithQualifiedField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			public class C1 {
			    static final String previousField = "abc";
			
			    Runnable run1 = new Runnable() {
			        @Override
			        public void run() {
			            System.out.println(C1.previousField + C1.this.instanceField + C1.classField + C1.this.getString());
			        }
			    };
			
			    static final String classField = "def";
			    final String instanceField = "abc";
			    public String getString() {
			        return "";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		String expected= """
			package test;
			
			public class C1 {
			    static final String previousField = "abc";
			
			    Runnable run1 = () -> System.out.println(C1.previousField + this.instanceField + C1.classField + this.getString());
			
			    static final String classField = "def";
			    final String instanceField = "abc";
			    public String getString() {
			        return "";
			    }
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, null);
	}

	@Test
	public void testSimplifyLambdaExpression() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			import static java.util.Calendar.getInstance;
			import static java.util.Calendar.getAvailableLocales;
			
			import java.time.Instant;
			import java.util.ArrayList;
			import java.util.Calendar;
			import java.util.Date;
			import java.util.Locale;
			import java.util.Vector;
			import java.util.function.BiFunction;
			import java.util.function.Function;
			import java.util.function.Supplier;
			
			public class E extends Date {
			    public String changeableText = "foo";
			
			    public Function<String, String> removeParentheses() {
			        return (someString) -> someString.trim().toLowerCase();
			    }
			
			    public Function<String, String> removeReturnAndBrackets() {
			        return someString -> {return someString.trim().toLowerCase();};
			    }
			
			    public Function<String, String> removeReturnAndBracketsWithParentheses() {
			        return (someString) -> {return someString.trim().toLowerCase() + "bar";};
			    }
			
			    public Supplier<ArrayList<String>> useCreationReference() {
			        return () -> { return new ArrayList<>(); };
			    }
			
			    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameter() {
			        return (capacity) -> new ArrayList<>(capacity);
			    }
			
			    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameterAndType() {
			        // TODO this can be refactored like useCreationReferenceWithParameter
			        return (Integer capacity) -> new ArrayList<>(capacity);
			    }
			
			    public BiFunction<Integer, Integer, Vector<String>> useCreationReferenceWithParameters() {
			        return (initialCapacity, capacityIncrement) -> new Vector<>(initialCapacity, capacityIncrement);
			    }
			
			    public Function<Date, Long> useMethodReference() {
			        return date -> date.getTime();
			    }
			
			    public BiFunction<Date, Date, Integer> useMethodReferenceWithParameter() {
			        return (date, anotherDate) -> date.compareTo(anotherDate);
			    }
			
			    public static Function<String, Long> useTypeReference() {
			        return (numberInText) -> { return Long.getLong(numberInText); };
			    }
			
			    public static Function<Locale, Calendar> useTypeReferenceOnImportedMethod() {
			        return locale -> Calendar.getInstance(locale);
			    }
			
			    public static Supplier<Locale[]> useTypeReferenceAsSupplier() {
			        return () -> Calendar.getAvailableLocales();
			    }
			
			    public Function<String, Integer> useExpressionMethodReferenceOnLiteral() {
			        return textToSearch -> "AutoRefactor".indexOf(textToSearch);
			    }
			
			    public Function<Date, Integer> useThisMethodReference() {
			        return anotherDate -> compareTo(anotherDate);
			    }
			
			    public Function<Date, Integer> useThisMethodReferenceAddThis() {
			        return anotherDate -> this.compareTo(anotherDate);
			    }
			
			    public Function<Date, Integer> useSuperMethodReference() {
			        return anotherDate -> super.compareTo(anotherDate);
			    }
			
			    public static Integer dummy(String arg) {
			        return 0;
			    }
			
			    public static Function<String, Integer> useTypeReferenceQualifyingLocalType() {
			        return numberInText -> E.dummy(numberInText);
			    }
			
			    public static Function<String, Integer> useTypeReferenceFullyQualifyingLocalType() {
			        return numberInText -> test1.E.dummy(numberInText);
			    }
			
			    public static Function<String, Integer> useTypeReferenceOnLocalType() {
			        return numberInText -> dummy(numberInText);
			    }
			
			    public static Function<Instant, java.sql.Date> useTypeReferenceQualifyingInheritedType() {
			        return instant -> java.sql.Date.from(instant);
			    }
			}
			""";

		String expected= """
			package test1;
			
			import static java.util.Calendar.getInstance;
			import static java.util.Calendar.getAvailableLocales;
			
			import java.time.Instant;
			import java.util.ArrayList;
			import java.util.Calendar;
			import java.util.Date;
			import java.util.Locale;
			import java.util.Vector;
			import java.util.function.BiFunction;
			import java.util.function.Function;
			import java.util.function.Supplier;
			
			public class E extends Date {
			    public String changeableText = "foo";
			
			    public Function<String, String> removeParentheses() {
			        return someString -> someString.trim().toLowerCase();
			    }
			
			    public Function<String, String> removeReturnAndBrackets() {
			        return someString -> someString.trim().toLowerCase();
			    }
			
			    public Function<String, String> removeReturnAndBracketsWithParentheses() {
			        return someString -> (someString.trim().toLowerCase() + "bar");
			    }
			
			    public Supplier<ArrayList<String>> useCreationReference() {
			        return ArrayList::new;
			    }
			
			    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameter() {
			        return ArrayList::new;
			    }
			
			    public Function<Integer, ArrayList<String>> useCreationReferenceWithParameterAndType() {
			        // TODO this can be refactored like useCreationReferenceWithParameter
			        return (Integer capacity) -> new ArrayList<>(capacity);
			    }
			
			    public BiFunction<Integer, Integer, Vector<String>> useCreationReferenceWithParameters() {
			        return Vector::new;
			    }
			
			    public Function<Date, Long> useMethodReference() {
			        return Date::getTime;
			    }
			
			    public BiFunction<Date, Date, Integer> useMethodReferenceWithParameter() {
			        return Date::compareTo;
			    }
			
			    public static Function<String, Long> useTypeReference() {
			        return Long::getLong;
			    }
			
			    public static Function<Locale, Calendar> useTypeReferenceOnImportedMethod() {
			        return Calendar::getInstance;
			    }
			
			    public static Supplier<Locale[]> useTypeReferenceAsSupplier() {
			        return Calendar::getAvailableLocales;
			    }
			
			    public Function<String, Integer> useExpressionMethodReferenceOnLiteral() {
			        return "AutoRefactor"::indexOf;
			    }
			
			    public Function<Date, Integer> useThisMethodReference() {
			        return this::compareTo;
			    }
			
			    public Function<Date, Integer> useThisMethodReferenceAddThis() {
			        return this::compareTo;
			    }
			
			    public Function<Date, Integer> useSuperMethodReference() {
			        return super::compareTo;
			    }
			
			    public static Integer dummy(String arg) {
			        return 0;
			    }
			
			    public static Function<String, Integer> useTypeReferenceQualifyingLocalType() {
			        return E::dummy;
			    }
			
			    public static Function<String, Integer> useTypeReferenceFullyQualifyingLocalType() {
			        return E::dummy;
			    }
			
			    public static Function<String, Integer> useTypeReferenceOnLocalType() {
			        return E::dummy;
			    }
			
			    public static Function<Instant, java.sql.Date> useTypeReferenceQualifyingInheritedType() {
			        return java.sql.Date::from;
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description)));
	}

	@Test
	public void testDoNotSimplifyLambdaExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.ArrayList;
			import java.util.Date;
			import java.util.Vector;
			import java.util.function.BiFunction;
			import java.util.function.Function;
			import java.util.function.Supplier;
			
			public class E extends Date {
			    public String changeableText = "foo";
			
			    public Supplier<Date> doNotRefactorWithAnonymousBody() {
			        return () -> new Date() {
			            @Override
			            public String toString() {
			                return "foo";
			            }
			        };
			    }
			
			    public Function<String, String> doNotRemoveParenthesesWithSingleVariableDeclaration() {
			        return (String someString) -> someString.trim().toLowerCase();
			    }
			
			    public BiFunction<String, String, Integer> doNotRemoveParenthesesWithTwoParameters() {
			        return (someString, anotherString) -> someString.trim().compareTo(anotherString.trim());
			    }
			
			    public Supplier<Boolean> doNotRemoveParenthesesWithNoParameter() {
			        return () -> {System.out.println("foo");return true;};
			    }
			
			    public Function<String, String> doNotRemoveReturnWithSeveralStatements() {
			        return someString -> {String trimmed = someString.trim();
			        return trimmed.toLowerCase();};
			    }
			
			    public Function<Integer, ArrayList<String>> doNotRefactorWithExpressions() {
			        return capacity -> new ArrayList<>(capacity + 1);
			    }
			
			    public BiFunction<Integer, Integer, Vector<String>> doNotRefactorShuffledParams() {
			        return (initialCapacity, capacityIncrement) -> new Vector<>(capacityIncrement, initialCapacity);
			    }
			
			    public Function<String, Integer> doNotUseExpressionMethodReferenceOnVariable() {
			        return textToSearch -> this.changeableText.indexOf(textToSearch);
			    }
			
			    public class InnerClass {
			        public Function<Date, Integer> doNotUseThisMethodReferenceOnTopLevelClassMethod() {
			            return anotherDate -> compareTo(anotherDate);
			        }
			    }
			
			    public Function<Integer, String> doNotUseConflictingMethodReference() {
			        return numberToPrint -> numberToPrint.toString();
			    }
			
			    public Function<Integer, String> doNotUseConflictingStaticMethodReference() {
			        return numberToPrint -> Integer.toString(numberToPrint);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testIssue1047_1() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			import java.util.function.Supplier;
			
			public class E {
			
			    void func( String ... args) {
			
			    }
			    void called( Runnable r ) {
			
			    }
			    void called( Supplier<Object> r ) {
			
			    }
			    void test() {
			        called(() -> func());
			    }
			}
			"""; //

		String expected= """
			import java.util.function.Supplier;
			
			public class E {
			
			    void func( String ... args) {
			
			    }
			    void called( Runnable r ) {
			
			    }
			    void called( Supplier<Object> r ) {
			
			    }
			    void test() {
			        called((Runnable) this::func);
			    }
			}
			"""; //
		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description)));
	}

	@Test
	public void testIssue1047_2() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given1= """
			import java.util.function.Supplier;
			
			public class E1 {
			
			    void called( Supplier<Object> r ) {
			
			    }
			}
			"""; //
		ICompilationUnit cu1= pack.createCompilationUnit("E1.java", given1, false, null);

		String given= """
			import java.util.function.Supplier;
			
			public class E extends E1 {
			
			    void func( String ... args) {
			
			    }
			    void called( Runnable r ) {
			
			    }
			    void test() {
			        called(() -> func());
			    }
			}
			"""; //

		String expected= """
			import java.util.function.Supplier;
			
			public class E extends E1 {
			
			    void func( String ... args) {
			
			    }
			    void called( Runnable r ) {
			
			    }
			    void test() {
			        called((Runnable) this::func);
			    }
			}
			"""; //
		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu, cu1 }, new String[] { expected, given1 },
				new HashSet<>(Arrays.asList(MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description)));
	}

	@Test
	public void testIssue1047_3() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given1= """
			import java.util.function.Supplier;
			
			public class E1 {
			
			    void func( String ... args) {
			
			    }
			    void called( Supplier<Object> r ) {
			
			    }
			}
			"""; //
		ICompilationUnit cu1= pack.createCompilationUnit("E1.java", given1, false, null);

		String given= """
			import java.util.function.Supplier;
			
			public class E extends E1 {
			
			    void called( Runnable r ) {
			
			    }
			    void test() {
			        called(() -> super.func());
			    }
			}
			"""; //

		String expected= """
			import java.util.function.Supplier;
			
			public class E extends E1 {
			
			    void called( Runnable r ) {
			
			    }
			    void test() {
			        called((Runnable) super::func);
			    }
			}
			"""; //
		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu, cu1 }, new String[] { expected, given1 },
				new HashSet<>(Arrays.asList(MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description)));
	}

	@Test
	public void testIssue1047_4() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given1= """
			import java.util.function.Supplier;
			
			public class E1 {
			
			    static void func( String ... args) {
			
			    }
			    void called( Supplier<Object> r ) {
			
			    }
			}
			"""; //
		ICompilationUnit cu1= pack.createCompilationUnit("E1.java", given1, false, null);

		String given= """
			import java.util.function.Supplier;
			
			public class E extends E1 {
			
			    void called( Runnable r ) {
			
			    }
			    void test() {
			        called(() -> E1.func());
			    }
			}
			"""; //

		String expected= """
			import java.util.function.Supplier;
			
			public class E extends E1 {
			
			    void called( Runnable r ) {
			
			    }
			    void test() {
			        called((Runnable) E1::func);
			    }
			}
			"""; //
		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu, cu1 }, new String[] { expected, given1 },
				new HashSet<>(Arrays.asList(MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description)));
	}

	@Test
	public void testBug579393() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			import java.util.stream.Stream;
			
			public class E {
			    public static void main(String[] args) {
			        new A() {
			        };
			        get();
			        System.out.println("done");
			    }
			
			    public static A get(B<?>... sources) {
			        return Stream.of(sources)
			                .map(B::getT)
			                .filter(x -> x.exists_testOpen())
			                .findFirst()
			                .orElse(null);
			    }
			
			    public interface B<T extends A> extends A {
			        T getT();
			    }
			
			    public interface A {
			        default boolean exists_testOpen() {
			            return true;
			        }
			    }
			}
			""";

		String expected= """
			import java.util.stream.Stream;
			
			public class E {
			    public static void main(String[] args) {
			        new A() {
			        };
			        get();
			        System.out.println("done");
			    }
			
			    public static A get(B<?>... sources) {
			        return Stream.of(sources)
			                .map(B::getT)
			                .filter(A::exists_testOpen)
			                .findFirst()
			                .orElse(null);
			    }
			
			    public interface B<T extends A> extends A {
			        T getT();
			    }
			
			    public interface A {
			        default boolean exists_testOpen() {
			            return true;
			        }
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.LambdaExpressionAndMethodRefCleanUp_description)));
	}

	@Test
	public void testConvertToLambdaWithRecursion() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			import java.util.function.Function;
			
			public class C1 {
			
			    public interface I1 {
			        public int add(int a);
			    }
			
			    I1 k = new I1() {
			        @Override
			        public int add(int a) {
			            if (a == 2) {
			                return add(3);
			            }
			            return a + 7;
			        }
			    };
			
			    public static I1 j = new I1() {
			        @Override
			        public int add(int a) {
			            if (a == 2) {
			                return add(4);
			            }
			            return a + 8;
			        }
			    };
			}
			"""; //
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		String expected= """
			package test;
			
			import java.util.function.Function;
			
			public class C1 {
			
			    public interface I1 {
			        public int add(int a);
			    }
			
			    I1 k = a -> {
			        if (a == 2) {
			            return this.k.add(3);
			        }
			        return a + 7;
			    };
			
			    public static I1 j = a -> {
			        if (a == 2) {
			            return C1.j.add(4);
			        }
			        return a + 8;
			    };
			}
			"""; //
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotConvertLocalRecursiveClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.function.Function;
			
			public class C2 {
			
			    public interface I1 {
			        public int add(int a);
			    }
			
			    public int foo() {
			        I1 doNotConvert = new I1() {
			            @Override
			            public int add(int a) {
			                if (a == 2) {
			                    return add(5);
			                }
			                return a + 9;
			            }
			        };
			        return doNotConvert.add(9);
			    }
			}
			"""; //
		ICompilationUnit cu= pack1.createCompilationUnit("C2.java", sample, false, null);

		enable(CleanUpConstants.SIMPLIFY_LAMBDA_EXPRESSION_AND_METHOD_REF);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotConvertGenericInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class C2 {
			
			    public interface IInteractionContext {
			    }
			
			    public interface IAdaptable {
			        public <T> T getAdapter(Class<T> adapter);
			    }
			
			    @SuppressWarnings("unchecked")
			    public IAdaptable asAdaptable(final IInteractionContext result) {
			        return new IAdaptable() {
			            public Object getAdapter(Class adapter) {
			                if (adapter == IInteractionContext.class) {
			                    return result;
			                }
			                return null;
			            }
			        };
			    }
			}
			"""; //
		ICompilationUnit cu= pack1.createCompilationUnit("C2.java", sample, false, null);

		enable(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES);
		enable(CleanUpConstants.USE_LAMBDA);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testComparingOnCriteria() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			import java.io.File;
			import java.util.Collections;
			import java.util.Comparator;
			import java.util.Date;
			import java.util.List;
			import java.util.Locale;
			import java.util.TreeSet;
			import java.util.Map.Entry;
			import java.util.stream.Stream;
			
			public class E {
			    private Comparator<Date> refactorField = new Comparator<Date>() {
			        @Override
			        public int compare(Date o1, Date o2) {
			            return o1.toString().compareTo(o2.toString());
			        }
			    };
			
			    public List<Date> useMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                return o1.toString().compareTo(o2.toString());
			            }
			
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useReversedMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                return o2.toString().compareTo(o1.toString());
			            }
			
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useNegatedMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                return -o1.toString().compareTo(o2.toString());
			            }
			
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<File> useTypedLambdaExpression(List<File> listToSort) {
			        // Keep this comment
			        Comparator<File> comparator = new Comparator<File>() {
			
			            @Override
			            public int compare(File f1, File f2) {
			                return f1.separator.compareTo(f2.separator);
			            }
			
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<File> useUntypedLambdaExpression(List<File> listToSort) {
			        // Keep this comment
			        Comparator comparator = new Comparator<File>() {
			
			            @Override
			            public int compare(File f1, File f2) {
			                return f1.separator.compareTo(f2.separator);
			            }
			
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<File> useReversedLambdaExpression(List<File> listToSort) {
			        // Keep this comment
			        Comparator<File> comparator = new Comparator<File>() {
			
			            @Override
			            public int compare(File f1, File f2) {
			                return f2.separator.compareTo(f1.separator);
			            }
			
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = (o1, o2) -> o1.toString().compareTo(o2.toString());
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByReversedMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = (o1, o2) -> o2.toString().compareTo(o1.toString());
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByNegatedMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = (o1, o2) -> -o1.toString().compareTo(o2.toString());
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<File> replaceLambdaByTypedLambdaExpression(List<File> listToSort) {
			        // Keep this comment
			        Comparator<File> comparator = (f1, f2) -> f1.separator.compareTo(f2.separator);
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<File> replaceLambdaByReversedLambdaExpression(List<File> listToSort) {
			        // Keep this comment
			        Comparator<File> comparator = (f1, f2) -> f2.separator.compareTo(f1.separator);
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaUsingRightType(List<Date> initialPackagesToDelete) {
			        // Keep this comment
			        Collections.sort(initialPackagesToDelete, (Date one, Date two) -> one.toString().compareTo(two.toString()));
			
			        return initialPackagesToDelete;
			    }
			
			    public List<Date> useMethodRefNullFirst(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                if (o1 != null) {
			                    if (o2 != null) {
			                        return o1.toString().compareTo(o2.toString());
			                    }
			
			                    return 1;
			                } else if (o2 != null) {
			                    return -1;
			                } else {
			                    return 0;
			                }
			            }
			
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useMethodRefNullLast(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                if (o1 != null) {
			                    if (null != o2) {
			                        return o1.toString().compareTo(o2.toString());
			                    } else {
			                        return -10;
			                    }
			                } else {
			                    return (o2 == null) ? 0 : 20;
			                }
			            }
			
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useReversedMethodRefNullFirst(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                if (o1 != null)
			                    if (null == o2)
			                        return 123;
			                     else
			                        return o2.toString().compareTo(o1.toString());
			
			                return (o2 == null) ? 0 : -123;
			            }
			
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useReversedMethodRefNullLast(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                if (o1 != null) {
			                    if (null == o2) {
			                        return -10;
			                    } else {
			                        return Long.compare(o2.getTime(), o1.getTime());
			                    }
			                }
			
			                return (o2 == null) ? 0 : 20;
			            }
			
			        });
			
			        return listToSort;
			    }
			
			    public List<Date> useMethodRefWithNegation(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = new Comparator<Date>() {
			            @Override
			            public int compare(Date o1, Date o2) {
			                if (!(o1 != null)) {
			                    if (o2 != null) {
			                        return -1;
			                    } else {
			                        return 0;
			                    }
			                } else {
			                    if (o2 != null) {
			                        return -o1.toString().compareTo(o2.toString());
			                    }
			
			                    return 1;
			                }
			            }
			        };
			        listToSort.sort(comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useMethodRefUnorderedCondition(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                if (o2 != null) {
			                    if (o1 != null) {
			                        return o1.toString().compareTo(o2.toString());
			                    }
			
			                    return -1;
			                } else if (o1 != null) {
			                    return 1;
			                } else {
			                    return 0;
			                }
			            }
			
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByMethodRefNullFirst(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = (o1, o2) -> {
			            if (o1 != null) {
			                if (o2 != null) {
			                    return o1.toString().compareTo(o2.toString());
			                }
			
			                return 1;
			            } else if (o2 != null) {
			                return -1;
			            } else {
			                return 0;
			            }
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByMethodRefNullLast(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = (o1, o2) -> {
			            if (o1 != null) {
			                if (null != o2) {
			                    return o1.toString().compareTo(o2.toString());
			                } else {
			                    return -10;
			                }
			            } else {
			                return (o2 == null) ? 0 : 20;
			            }
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByReversedMethodRefNullFirst(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = (o1, o2) -> {
			            if (o1 != null)
			                if (null == o2)
			                    return 123;
			                 else
			                    return o2.toString().compareTo(o1.toString());
			
			            return (o2 == null) ? 0 : -123;
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByReversedMethodRefNullLast(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator= (o1, o2) -> {
			            if (o1 != null) {
			                if (null == o2) {
			                    return -10;
			                } else {
			                    return Long.compare(o2.getTime(), o1.getTime());
			                }
			            }
			
			            return (o2 == null) ? 0 : 20;
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByMethodRefWithNegation(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = (o1, o2) -> {
			            if (!(o1 != null)) {
			                if (o2 != null) {
			                    return -1;
			                } else {
			                    return 0;
			                }
			            } else {
			                if (o2 != null) {
			                    return -o1.toString().compareTo(o2.toString());
			                }
			
			                return 1;
			            }
			        };
			        listToSort.sort(comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByMethodRefUnorderedCondition(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = (o1, o2) -> {
			            if (o2 != null) {
			                if (o1 != null) {
			                    return o1.toString().compareTo(o2.toString());
			                }
			
			                return -1;
			            } else if (o1 != null) {
			                return 1;
			            } else {
			                return 0;
			            }
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    void wildcardMethod() {
			        Stream.<Entry<String, String>>of().sorted((entry1, entry2) -> entry1.getKey().compareTo(entry2.getKey()));
			    }
			
			    public class FooBar {
			        public String value;
			    }
			
			    private final TreeSet<FooBar> foo = new TreeSet<>((a,b) -> b.value.compareTo(a.value));
			
			}
			""";

		String expected= """
			package test1;
			
			import java.io.File;
			import java.util.Collections;
			import java.util.Comparator;
			import java.util.Date;
			import java.util.List;
			import java.util.Locale;
			import java.util.TreeSet;
			import java.util.Map.Entry;
			import java.util.stream.Stream;
			
			public class E {
			    private Comparator<Date> refactorField = Comparator.comparing(Date::toString);
			
			    public List<Date> useMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.comparing(Date::toString);
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useReversedMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.comparing(Date::toString).reversed();
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useNegatedMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.comparing(Date::toString).reversed();
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<File> useTypedLambdaExpression(List<File> listToSort) {
			        // Keep this comment
			        Comparator<File> comparator = Comparator.comparing(f1 -> f1.separator);
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<File> useUntypedLambdaExpression(List<File> listToSort) {
			        // Keep this comment
			        Comparator comparator = Comparator.comparing((File f1) -> f1.separator);
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<File> useReversedLambdaExpression(List<File> listToSort) {
			        // Keep this comment
			        Comparator<File> comparator = Comparator.comparing((File f1) -> f1.separator).reversed();
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.comparing(Date::toString);
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByReversedMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.comparing(Date::toString).reversed();
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByNegatedMethodRef(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.comparing(Date::toString).reversed();
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<File> replaceLambdaByTypedLambdaExpression(List<File> listToSort) {
			        // Keep this comment
			        Comparator<File> comparator = Comparator.comparing(f1 -> f1.separator);
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<File> replaceLambdaByReversedLambdaExpression(List<File> listToSort) {
			        // Keep this comment
			        Comparator<File> comparator = Comparator.comparing((File f1) -> f1.separator).reversed();
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaUsingRightType(List<Date> initialPackagesToDelete) {
			        // Keep this comment
			        Collections.sort(initialPackagesToDelete, Comparator.comparing(Date::toString));
			
			        return initialPackagesToDelete;
			    }
			
			    public List<Date> useMethodRefNullFirst(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString));
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useMethodRefNullLast(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.nullsLast(Comparator.comparing(Date::toString));
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useReversedMethodRefNullFirst(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString).reversed());
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useReversedMethodRefNullLast(List<Date> listToSort) {
			        // Keep this comment
			        Collections.sort(listToSort, Comparator.nullsLast(Comparator.comparing(Date::getTime)));
			
			        return listToSort;
			    }
			
			    public List<Date> useMethodRefWithNegation(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString).reversed());
			        listToSort.sort(comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> useMethodRefUnorderedCondition(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString));
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByMethodRefNullFirst(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString));
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByMethodRefNullLast(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.nullsLast(Comparator.comparing(Date::toString));
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByReversedMethodRefNullFirst(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString).reversed());
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByReversedMethodRefNullLast(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator= Comparator.nullsLast(Comparator.comparing(Date::getTime));
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByMethodRefWithNegation(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString).reversed());
			        listToSort.sort(comparator);
			
			        return listToSort;
			    }
			
			    public List<Date> replaceLambdaByMethodRefUnorderedCondition(List<Date> listToSort) {
			        // Keep this comment
			        Comparator<Date> comparator = Comparator.nullsFirst(Comparator.comparing(Date::toString));
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    void wildcardMethod() {
			        Stream.<Entry<String, String>>of().sorted(Comparator.comparing(Entry<String, String>::getKey));
			    }
			
			    public class FooBar {
			        public String value;
			    }
			
			    private final TreeSet<FooBar> foo = new TreeSet<>(Comparator.comparing((FooBar a) -> a.value).reversed());
			
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.COMPARING_ON_CRITERIA);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.ComparingOnCriteriaCleanUp_description)));
	}

	@Test
	public void testDoNotUseComparingOnCriteria() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.Collections;
			import java.util.Comparator;
			import java.util.Date;
			import java.util.List;
			import java.util.Locale;
			
			public class E {
			    public List<Date> doNotUseMethodRefWithWeirdBehavior(List<Date> listToSort) {
			        Comparator<Date> comparator = new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                if (o1 != null) {
			                    if (o2 != null) {
			                        return o1.toString().compareTo(o2.toString());
			                    } else {
			                        return 1;
			                    }
			                } else if (o2 != null) {
			                    return -1;
			                } else {
			                    return 100;
			                }
			            }
			
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<String> doNotUseMethodRef(List<String> listToSort) {
			        Comparator<String> comparator = new Comparator<String>() {
			            @Override
			            public int compare(String o1, String o2) {
			                return o1.toLowerCase(Locale.ENGLISH).compareTo(o2.toLowerCase(Locale.ENGLISH));
			            }
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public Comparator<Date> doNotRefactorComparisonWithoutCompareToMethod(List<Date> listToSort) {
			        Comparator<Date> comparator = new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                return (int) (o1.getTime() - o2.getTime());
			            }
			        };
			
			        return comparator;
			    }
			
			    public int compareTo(E anc) {
			        return 0;
			    }
			
			    public E getNewInstance() {
			        return new E();
			    }
			
			    private Comparator<E> doNotRefactorNotComparableObjects = new Comparator<E>() {
			        @Override
			        public int compare(E o1, E o2) {
			            return o1.getNewInstance().compareTo(o2.getNewInstance());
			        }
			    };
			
			    public Comparator<Date> doNotRemoveSecondaryMethod(List<Date> listToSort) {
			        Comparator<Date> comparator = new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                return o1.toString().compareTo(o2.toString());
			            }
			
			            @Override
			            public String toString() {
			                return "Compare formatted dates";
			            }
			        };
			
			        return comparator;
			    }
			
			    public List<Date> doNotReplaceLambdaByUseMethodRefWithWeirdBehavior(List<Date> listToSort) {
			        Comparator<Date> comparator = (o1, o2) -> {
			            if (o1 != null) {
			                if (o2 != null) {
			                    return o1.toString().compareTo(o2.toString());
			                } else {
			                    return 1;
			                }
			            } else if (o2 != null) {
			                return -1;
			            } else {
			                return 100;
			            }
			        };
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public List<String> doNotReplaceLambdaByUseMethodRef(List<String> listToSort) {
			        Comparator<String> comparator = (o1, o2) -> o1.toLowerCase(Locale.ENGLISH).compareTo(o2.toLowerCase(Locale.ENGLISH));
			        Collections.sort(listToSort, comparator);
			
			        return listToSort;
			    }
			
			    public Comparator<Date> doNotReplaceLambdaByRefactorComparisonWithoutCompareToMethod(List<Date> listToSort) {
			        Comparator<Date> comparator = (o1, o2) -> (int) (o1.getTime() - o2.getTime());
			
			        return comparator;
			    }
			
			    public Comparator<Date> doNotReplaceLambdaByRemoveSecondaryMethod(List<Date> listToSort) {
			        Comparator<Date> comparator = new Comparator<Date>() {
			
			            @Override
			            public int compare(Date o1, Date o2) {
			                return o1.toString().compareTo(o2.toString());
			            }
			
			            @Override
			            public String toString() {
			                return "Compare formatted dates";
			            }
			        };
			
			        return comparator;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.COMPARING_ON_CRITERIA);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testJoin() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;
			
			public class E {
			    public String refactorConcatenation(String[] texts) {
			        // Keep this comment
			        boolean isFirst = true;
			        // Keep this comment too
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment also
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorReassignment(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (String text : texts) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation = concatenation.append(", ");
			            }
			            concatenation = concatenation.append(text);
			        }
			
			        return concatenation.toString();
			    }
			
			    public Runnable refactorFinalConcatenation(String[] names) {
			        boolean isFirst = true;
			        final StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < names.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(names[i]);
			        }
			
			        Runnable supplier= new Runnable() {
			            @Override
			            public void run() {
			                System.out.println(concatenation.toString());
			            }
			        };
			        return supplier;
			    }
			
			    public String refactorConcatenationWithChar(String[] titles) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (String title : titles) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(',');
			            }
			            concatenation.append(title);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithCharVariable(String[] titles, char delimiter) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (String title : titles) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(delimiter);
			            }
			            concatenation.append(title);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithCharacterWrapper(String[] titles, Character delimiter) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (String title : titles) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(delimiter);
			            }
			            concatenation.append(title);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithEscapedChar(String[] titles) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (String title : titles) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append('\\n');
			            }
			            concatenation.append(title);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithInt(String[] titles) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (String title : titles) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(123);
			            }
			            concatenation.append(title);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithHardCodedDelimiter(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation = concatenation.append(" " + 1);
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithBuilderFirst(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			        boolean isFirst = true;
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithStringBuffer(String[] texts) {
			        boolean isFirst = true;
			        StringBuffer concatenation = new StringBuffer();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithBooleanObject(String[] texts) {
			        Boolean isFirst = Boolean.TRUE;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = Boolean.FALSE;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation = concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithNegatedBoolean(String[] texts) {
			        Boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (!isFirst) {
			                concatenation.append(", ");
			            } else {
			                isFirst = false;
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithReversedBoolean(String[] texts) {
			        boolean isVisited = Boolean.FALSE;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (isVisited) {
			                concatenation.append(", ");
			            } else {
			                isVisited = Boolean.TRUE;
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithLotsOfMethods(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        System.out.println(concatenation.charAt(0));
			        System.out.println(concatenation.chars());
			        System.out.println(concatenation.codePoints());
			        System.out.println(concatenation.indexOf("foo", 0));
			        System.out.println(concatenation.lastIndexOf("foo"));
			        System.out.println(concatenation.lastIndexOf("foo", 0));
			        System.out.println(concatenation.length());
			        System.out.println(concatenation.subSequence(0, 0));
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationOnForeach(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			        boolean isFirst = true;
			
			        // Keep this comment
			        for (String text : texts) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(text);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithConditionOnIndex(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (i > 0) {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithInequalityOnIndex(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (i != 0) {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithReversedConditionOnIndex(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (0 < i) {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithGreaterOrEqualsOnIndex(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (i >= 1) {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithDelimiterAtTheEnd(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            concatenation.append(texts[i]);
			            if (i < texts.length - 1) {
			                concatenation.append(", ");
			            }
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithMirroredCondition(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            concatenation.append(texts[i]);
			            if (texts.length - 1 > i) {
			                concatenation.append(", ");
			            }
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithNotEqualsCondition(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            concatenation.append(texts[i]);
			            if (i < texts.length - 1) {
			                concatenation.append(", ");
			            }
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationWithLessOrEqualsCondition(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            concatenation.append(texts[i]);
			            if (i <= texts.length - 2) {
			                concatenation.append(", ");
			            }
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationTestingLength(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (concatenation.length() > 0) {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationTestingNotEmpty(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (concatenation.length() != 0) {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationTestingGreaterOrEqualsOne(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (concatenation.length() >= 1) {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationTestingLengthMirrored(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (0 < concatenation.length()) {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationTestingNotEmptyMirrored(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (0 != concatenation.length()) {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConcatenationTestingGreaterOrEqualsOneMirrored(String[] texts) {
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (int i = 0; i < texts.length; i++) {
			            if (1 <= concatenation.length()) {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorConstantBooleanShift(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (String text : texts) {
			            if (!isFirst) {
			                concatenation.append(", ");
			            }
			            isFirst = false;
			            concatenation.append(text);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorWithBooleanShiftAtTheEnd(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (String text : texts) {
			            if (!isFirst) {
			                concatenation.append(", ");
			            }
			            concatenation.append(text);
			            isFirst = false;
			        }
			
			        return concatenation.toString();
			    }
			
			    public String refactorWithReversedBooleanShift(String[] texts) {
			        boolean isNotFirst = false;
			        StringBuilder concatenation = new StringBuilder();
			
			        // Keep this comment
			        for (String text : texts) {
			            if (isNotFirst) {
			                concatenation.append(", ");
			            }
			            concatenation.append(text);
			            isNotFirst = true;
			        }
			
			        return concatenation.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.JOIN);

		String output= """
			package test1;
			
			public class E {
			    public String refactorConcatenation(String[] texts) {
			        // Keep this comment
			       \s
			        // Keep this comment too
			       \s
			
			        // Keep this comment also
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorReassignment(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public Runnable refactorFinalConcatenation(String[] names) {
			       \s
			
			        // Keep this comment
			        final String concatenation = String.join(", ", names);
			
			        Runnable supplier= new Runnable() {
			            @Override
			            public void run() {
			                System.out.println(concatenation);
			            }
			        };
			        return supplier;
			    }
			
			    public String refactorConcatenationWithChar(String[] titles) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(",", titles);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithCharVariable(String[] titles, char delimiter) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(String.valueOf(delimiter), titles);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithCharacterWrapper(String[] titles, Character delimiter) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(String.valueOf(delimiter), titles);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithEscapedChar(String[] titles) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join("\\n", titles);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithInt(String[] titles) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(String.valueOf(123), titles);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithHardCodedDelimiter(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(" " + 1, texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithBuilderFirst(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithStringBuffer(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithBooleanObject(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithNegatedBoolean(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithReversedBoolean(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithLotsOfMethods(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        System.out.println(concatenation.charAt(0));
			        System.out.println(concatenation.chars());
			        System.out.println(concatenation.codePoints());
			        System.out.println(concatenation.indexOf("foo", 0));
			        System.out.println(concatenation.lastIndexOf("foo"));
			        System.out.println(concatenation.lastIndexOf("foo", 0));
			        System.out.println(concatenation.length());
			        System.out.println(concatenation.subSequence(0, 0));
			        return concatenation;
			    }
			
			    public String refactorConcatenationOnForeach(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithConditionOnIndex(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithInequalityOnIndex(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithReversedConditionOnIndex(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithGreaterOrEqualsOnIndex(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithDelimiterAtTheEnd(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithMirroredCondition(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithNotEqualsCondition(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationWithLessOrEqualsCondition(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationTestingLength(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationTestingNotEmpty(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationTestingGreaterOrEqualsOne(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationTestingLengthMirrored(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationTestingNotEmptyMirrored(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConcatenationTestingGreaterOrEqualsOneMirrored(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorConstantBooleanShift(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorWithBooleanShiftAtTheEnd(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			
			    public String refactorWithReversedBooleanShift(String[] texts) {
			       \s
			
			        // Keep this comment
			        String concatenation = String.join(", ", texts);
			
			        return concatenation;
			    }
			}
			""";

		assertNotEquals("The class must be changed", input, output);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.JoinCleanup_description)));
	}

	@Test
	public void testDoNotJoin() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public boolean doNotRefactorUsedBoolean(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        System.out.println(concatenation.toString());
			        return isFirst;
			    }
			
			    public String doNotRefactorUnhandledMethod(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        System.out.println(concatenation.codePointAt(0));
			        System.out.println(concatenation.codePointBefore(0));
			        System.out.println(concatenation.codePointCount(0, 0));
			        concatenation.getChars(0, 0, new char[0], 0);
			        System.out.println(concatenation.indexOf("foo"));
			        System.out.println(concatenation.offsetByCodePoints(0, 0));
			        System.out.println(concatenation.substring(0));
			        System.out.println(concatenation.substring(0, 0));
			        System.out.println(concatenation.capacity());
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorPartialConcatenation(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 1; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorUnfinishedConcatenation(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length - 1; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorReversedConcatenation(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = texts.length - 1; i >= 0; i--) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithOppositeBoolean(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 1; i < texts.length; i++) {
			            if (isFirst) {
			                concatenation.append(", ");
			            } else {
			                isFirst = false;
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorOnObjects(Object[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithOtherAppending(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        concatenation.append("foo");
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithInitialization(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder("foo");
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithWrongIndex(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[0]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithWrongBoolean(String[] texts, boolean isSecond) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isSecond) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithWrongBoolean(String[] texts) {
			        boolean isSecond = false;
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isSecond = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithWrongArray(String[] texts, String[] names) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(names[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithWrongBuilder(String[] texts, StringBuilder otherBuilder) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            otherBuilder.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithAnotherBuilder(String[] texts, StringBuilder otherBuilder) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                otherBuilder.append(", ");
			            }
			            concatenation.append(texts[i]);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithAdditionalStatement(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i]);
			            System.out.println("Hi!");
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithWrongMethod(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (int i = 0; i < texts.length; i++) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(texts[i], 0, 2);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWrongVariable(String[] texts, String test) {
			        StringBuilder concatenation = new StringBuilder();
			        boolean isFirst = true;
			
			        for (String text : texts) {
			            if (isFirst) {
			                isFirst = false;
			            } else {
			                concatenation.append(", ");
			            }
			            concatenation.append(test);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithBooleanShiftFirst(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (String text : texts) {
			            isFirst = false;
			            if (!isFirst) {
			                concatenation.append(", ");
			            }
			            concatenation.append(text);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithAppendingFirst(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (String text : texts) {
			            concatenation.append(text);
			            if (!isFirst) {
			                concatenation.append(", ");
			            }
			            isFirst = false;
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithConditionAtTheEnd(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (String text : texts) {
			            concatenation.append(text);
			            isFirst = false;
			            if (!isFirst) {
			                concatenation.append(", ");
			            }
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWithNonsense(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (String text : texts) {
			            isFirst = false;
			            concatenation.append(text);
			            if (!isFirst) {
			                concatenation.append(", ");
			            }
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorUnshiftedBoolean(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (String text : texts) {
			            if (!isFirst) {
			                concatenation.append(", ");
			            }
			            isFirst = true;
			            concatenation.append(text);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWrongCondition(String[] texts) {
			        boolean isFirst = true;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (String text : texts) {
			            if (isFirst) {
			                concatenation.append(", ");
			            }
			            isFirst = false;
			            concatenation.append(text);
			        }
			
			        return concatenation.toString();
			    }
			
			    public String doNotRefactorWrongInit(String[] texts) {
			        boolean isFirst = false;
			        StringBuilder concatenation = new StringBuilder();
			
			        for (String text : texts) {
			            if (!isFirst) {
			                concatenation.append(", ");
			            }
			            isFirst = false;
			            concatenation.append(text);
			        }
			
			        return concatenation.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.JOIN);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testStringBufferToStringBuilderLocalsOnly() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample0= """
			package test1;
			
			public class SuperClass {
			    public StringBuffer field0;
			    public void method0(StringBuffer parm) {
			        System.out.println(parm.toString());
			    }
			}""";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= """
			package test1;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    StringBuffer field1;
			    StringBuffer field2;
			    public void changeLambda(StringBuffer parm) {
			        Runnable r = () -> {
			            StringBuffer a = new StringBuffer();
			            field1 = field2;
			            super.field0 = parm;
			            super.method0(parm);
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
			    StringBuffer field1;
			    StringBuffer field2;
			    public void changeLambda(StringBuffer parm) {
			        Runnable r = () -> {
			            StringBuilder a = new StringBuilder();
			            field1 = field2;
			            super.field0 = parm;
			            super.method0(parm);
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
			    public StringBuffer field0;
			    public void method0(StringBuffer parm) {
			        System.out.println(parm.toString());
			    }
			}""";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= """
			package test1;
			
			import java.io.StringWriter;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    StringBuffer field1;
			    StringBuffer field2;
			    public void doNotChangeLambdaWithFieldAssignment() {
			        Runnable r = () -> {
			            StringBuffer a = new StringBuffer();
			            a = field1;
			            a.append("abc");
			        };
			    }
			    public void doNotChangeLambdaWithParmAssignment(StringBuffer parm) {
			        Runnable r = () -> {
			            StringBuffer a = new StringBuffer();
			            a = parm;
			            a.append("abc");
			        };
			    }
			    public void doNotChangeLambdaWithSuperFieldAssignment(StringBuffer parm) {
			        Runnable r = () -> {
			            StringBuffer a = new StringBuffer();
			            a = super.field0;
			            a.append("abc");
			        };
			    }
			    public void doNotChangeLambdaWithMethodCall(StringBuffer parm) {
			        Runnable r = () -> {
			            StringBuffer a = new StringBuffer();
			            doNotChangeLambdaWithSuperFieldAssignment(a);
			            a.append("abc");
			        };
			    }
			    public void doNotChangeLambdaWithSuperMethodCall(StringBuffer parm) {
			        Runnable r = () -> {
			            StringBuffer a = new StringBuffer();
			            super.method0(a);
			            a.append("abc");
			        };
			    }
			    public void doNotChangeConstructorCall() {
			        StringBuffer a = new StringBuffer();
			        new Helper(a);
			    }
			    private class Helper {
			    	   public Helper(StringBuffer b) {
				           System.out.println(b.toString());\s
			   	   }
			    }
			    public void doNotChangeIfBufferIsAssigned() {
			        StringWriter stringWriter = new StringWriter();
				       StringBuffer buffer = stringWriter.getBuffer();\
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
			    public StringBuffer field0;
			    public void method0(StringBuffer parm) {
			        System.out.println(parm.toString());
			    }
			}""";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= """
			package test1;
			import java.io.StringWriter;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    StringBuffer field1;
			    StringBuffer field2;
			    public void changeLambdaWithFieldAssignment() {
			        Runnable r = () -> {
			            StringBuffer a = new StringBuffer();
			            a = field1;
			            a.append("abc");
			        };
			    }
			    public void changeLambdaWithParmAssignment(StringBuffer parm) {
			        Runnable r = () -> {
			            StringBuffer a = new StringBuffer();
			            a = parm;
			            a.append("abc");
			        };
			    }
			    public void changeLambdaWithSuperFieldAssignment(StringBuffer parm) {
			        Runnable r = () -> {
			            StringBuffer a = new StringBuffer();
			            a = super.field0;
			            a.append("abc");
			        };
			    }
			    public void changeLambdaWithMethodCall(StringBuffer parm) {
			        Runnable r = () -> {
			            StringBuffer a = new StringBuffer();
			            changeLambdaWithSuperFieldAssignment(a);
			            a.append("abc");
			        };
			    }
			    public void changeLambdaWithSuperMethodCall(StringBuffer parm) {
			        Runnable r = () -> {
			            StringBuffer a = new StringBuffer();
			            super.method0(a);
			            a.append("abc");
			        };
			    }
			    public void changeStringWriterInLambda(StringBuffer parm) {
			        Runnable r = () -> {
			            StringWriter a = new StringWriter();
			            StringBuffer k = a.getBuffer().append("abc");
			        };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		disable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		String expected0= """
			package test1;
			
			public class SuperClass {
			    public StringBuilder field0;
			    public void method0(StringBuilder parm) {
			        System.out.println(parm.toString());
			    }
			}""";

		String expected1= """
			package test1;
			import java.io.StringWriter;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    StringBuilder field1;
			    StringBuilder field2;
			    public void changeLambdaWithFieldAssignment() {
			        Runnable r = () -> {
			            StringBuilder a = new StringBuilder();
			            a = field1;
			            a.append("abc");
			        };
			    }
			    public void changeLambdaWithParmAssignment(StringBuilder parm) {
			        Runnable r = () -> {
			            StringBuilder a = new StringBuilder();
			            a = parm;
			            a.append("abc");
			        };
			    }
			    public void changeLambdaWithSuperFieldAssignment(StringBuilder parm) {
			        Runnable r = () -> {
			            StringBuilder a = new StringBuilder();
			            a = super.field0;
			            a.append("abc");
			        };
			    }
			    public void changeLambdaWithMethodCall(StringBuilder parm) {
			        Runnable r = () -> {
			            StringBuilder a = new StringBuilder();
			            changeLambdaWithSuperFieldAssignment(a);
			            a.append("abc");
			        };
			    }
			    public void changeLambdaWithSuperMethodCall(StringBuilder parm) {
			        Runnable r = () -> {
			            StringBuilder a = new StringBuilder();
			            super.method0(a);
			            a.append("abc");
			        };
			    }
			    public void changeStringWriterInLambda(StringBuilder parm) {
			        Runnable r = () -> {
			            StringWriter a = new StringWriter();
			            StringBuilder k = new StringBuilder(a.getBuffer().toString()).append("abc");
			        };
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu0, cu1 }, new String[] { expected0, expected1 },
				new HashSet<>(Arrays.asList(MultiFixMessages.StringBufferToStringBuilderCleanUp_description)));
	}

	@Test
	public void testWhile() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        Collections.reverse(strings);
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            // OK
			            System.err.println(s);
			        }
			        System.out.println();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        Collections.reverse(strings);
			        for (String s : strings) {
			            System.out.println(s);
			            // OK
			            System.err.println(s);
			        }
			        System.out.println();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileNested() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings,List<String> strings2) {
			        Collections.reverse(strings);
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            Iterator it2 = strings2.iterator();
			            while (it2.hasNext()) {
			                String s2 = (String) it2.next();
			                System.out.println(s2);
			            }
			            // OK
			            System.err.println(s);
			        }
			        System.out.println();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings,List<String> strings2) {
			        Collections.reverse(strings);
			        for (String s : strings) {
			            for (String s2 : strings2) {
			                System.out.println(s2);
			            }
			            // OK
			            System.err.println(s);
			        }
			        System.out.println();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileNested2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.*;
			public class Test {
			    void m(List<String> strings,List<String> strings2) {
			        Collections.reverse(strings);
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            Iterator it2 = strings2.iterator();
			            while (it2.hasNext()) {
			                String s2 = (String) it2.next();
			                System.out.println(s2);
			            }
			            // OK
			            System.out.println(it.next());
			        }
			        System.out.println();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.*;
			public class Test {
			    void m(List<String> strings,List<String> strings2) {
			        Collections.reverse(strings);
			        for (String string : strings) {
			            for (String s2 : strings2) {
			                System.out.println(s2);
			            }
			            // OK
			            System.out.println(string);
			        }
			        System.out.println();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileNested3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.*;
			public class Test {
			    void m(List<String> strings,List<String> strings2) {
			        Collections.reverse(strings);
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            it.next();
			            Iterator it2 = strings2.iterator();
			            while (it2.hasNext()) {
			                String s2 = (String) it2.next();
			                System.out.println(s2);
			            }
			        }
			        System.out.println();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.*;
			public class Test {
			    void m(List<String> strings,List<String> strings2) {
			        Collections.reverse(strings);
			        for (String string : strings) {
			            for (String s2 : strings2) {
			                System.out.println(s2);
			            }
			        }
			        System.out.println();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileNested4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.*;
			public class Test {
			    void m(List<String> strings,List<String> strings2) {
			        Collections.reverse(strings);
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            it.next();
			            Iterator it2 = strings2.iterator();
			            while (it2.hasNext()) {
			                String s2 = (String) it2.next();
			                System.out.println(s2);
			            }
			        }
			        System.out.println();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED);

		sample= """
			package test1;
			import java.util.*;
			public class Test {
			    void m(List<String> strings,List<String> strings2) {
			        Collections.reverse(strings);
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            it.next();
			            for (String s2 : strings2) {
			                System.out.println(s2);
			            }
			        }
			        System.out.println();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileNested5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.*;
			public class Test {
			    void m(List<String> strings,List<String> strings2) {
			        Collections.reverse(strings);
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String)it.next();
			            Iterator it2 = strings2.iterator();
			            while (it2.hasNext()) {
			                String s2 = (String) it2.next();
			                System.out.println(s2);
			            }
			            // end line comment
			        }
			        System.out.println();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED);

		sample= """
			package test1;
			import java.util.*;
			public class Test {
			    void m(List<String> strings,List<String> strings2) {
			        Collections.reverse(strings);
			        for (String s : strings) {
			            for (String s2 : strings2) {
			                System.out.println(s2);
			            }
			            // end line comment
			        }
			        System.out.println();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileGenericSubtype() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<ArrayList<String>> lists) {
			        Iterator it = lists.iterator();
			        while (it.hasNext()) {
			            List<String> list = (List<String>) it.next();
			            System.out.println(list);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<ArrayList<String>> lists) {
			        for (List<String> list : lists) {
			            System.out.println(list);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	/**
	 * https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/109
	 */
	@Test
	public void testWhileIssue109_EntrySet() throws CoreException {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			import java.util.Map.Entry;
			public class Test {
					void m() {
						Map<String, Object> map = Map.of("Hello", new Object());
						Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
						while (iterator.hasNext()) {
							Entry<String, Object> entry = iterator.next();
							System.out.println(entry);
						}
					}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		String expected= """
			package test;
			import java.util.*;
			import java.util.Map.Entry;
			public class Test {
					void m() {
						Map<String, Object> map = Map.of("Hello", new Object());
						for (Entry<String, Object> entry : map.entrySet()) {
							System.out.println(entry);
						}
					}
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	/**
	 * https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/109
	 */
	@Test
	public void testWhileIssue109_EntrySet_2() throws CoreException {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			import java.util.Map.Entry;
			public class Test {
					void m(Map<List<String>, Object> map) {
						Iterator<Entry<List<String>, Object>> iterator = map.entrySet().iterator();
						while (iterator.hasNext()) {
							Entry<List<String>, Object> entry = iterator.next();
							System.out.println(entry);
						}
					}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		String expected= """
			package test;
			import java.util.*;
			import java.util.Map.Entry;
			public class Test {
					void m(Map<List<String>, Object> map) {
						for (Entry<List<String>, Object> entry : map.entrySet()) {
							System.out.println(entry);
						}
					}
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	/**
	 * https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/109
	 */
	@Test
	public void testWhileIssue109_EntrySet_3() throws CoreException {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			import java.util.Map.Entry;
			public class Test {
					void m(Map<List<Date>[], Date[]> map) {
			         Iterator<Entry<List<Date>[], Date[]>> iterator = map.entrySet().iterator();
			         while (iterator.hasNext()) {
			             Entry<List<Date>[], Date[]> entry = iterator.next();
			             System.out.println(entry);
			         }
					}
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		String expected= """
			package test;
			import java.util.*;
			import java.util.Map.Entry;
			public class Test {
					void m(Map<List<Date>[], Date[]> map) {
			         for (Entry<List<Date>[], Date[]> entry : map.entrySet()) {
			             System.out.println(entry);
			         }
					}
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	/**
	 * https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/120
	 */
	@Test
	public void testWhileIssue120_CollectionTypeResolution() throws CoreException {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    private static <K, V> List<V> m(Map<K, List<V>> map) {
			        List<V> results = new ArrayList<>();
			        Iterator<List<V>> iterator = map.values().iterator();
			        while (iterator.hasNext()) {
			            results.addAll(iterator.next());
			        }
			        return results;
			    }\
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		String expected= """
			package test;
			import java.util.*;
			public class Test {
			    private static <K, V> List<V> m(Map<K, List<V>> map) {
			        List<V> results = new ArrayList<>();
			        for (List<V> element : map.values()) {
			            results.addAll(element);
			        }
			        return results;
			    }\
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	/**
	 * https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/190
	 */
	@Test
	public void testWhileIssue190_MultipleWhileLoops() throws CoreException {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        Iterator<String> it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			        Iterator<String> it2 = strings.iterator();
			        while (it2.hasNext()) {
			            String s = (String) it2.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		String expected= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        for (String s : strings) {
			            System.out.println(s);
			            System.err.println(s);
			        }
			        for (String s : strings) {
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	/**
	 * https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/798
	 *
	 * @throws CoreException on failure
	 */
	@Test
	public void testWhileIssue798() throws CoreException {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.HashSet;
			import java.util.Iterator;
			
			public class Test {
			   \s
			    public class Element {
			    }
			
			    public class ElementOccurrenceResult {
			    }
			
			    public void foo(Element element, HashSet<ElementOccurrenceResult> hashSet) {
			        Iterator<ElementOccurrenceResult> minIterator= hashSet.iterator();
			        while (minIterator.hasNext()) {
			            reportProblem(element, minIterator.next(), null);
			        }
			    }
			
			    private void reportProblem(Element element, ElementOccurrenceResult next, Object object) {}
			
			}
			"""; //
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		String expected= """
			package test;
			import java.util.HashSet;
			
			public class Test {
			   \s
			    public class Element {
			    }
			
			    public class ElementOccurrenceResult {
			    }
			
			    public void foo(Element element, HashSet<ElementOccurrenceResult> hashSet) {
			        for (ElementOccurrenceResult element2 : hashSet) {
			            reportProblem(element, element2, null);
			        }
			    }
			
			    private void reportProblem(Element element, ElementOccurrenceResult next, Object object) {}
			
			}
			"""; //
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	/**
	 * https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/963
	 *
	 * @throws CoreException on failure
	 */
	@Test
	public void testWhileIssue963() throws CoreException {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.io.File;
			import java.util.Iterator;
			import java.util.List;
			
			public class Test {
			
			    public int foo(String x, List<? extends File> files) {
			        Iterator<? extends File> iter= files.iterator();
			        while(iter.hasNext()){
			            dumpIMethod((String)iter.next().getAbsolutePath());
			        }
			        if (x.length() == 8) {
			            int count = 0;
			            for (Iterator<? extends File> iterator = files.iterator(); iterator.hasNext(); ) {
			                iterator.next();
			                count++;
			            }
			            return count;
			        }
			        return 0;
			    }
			
			    public static void dumpIMethodList(List<?> l){
			        Iterator<?> iter= l.iterator();
			        while(iter.hasNext()){
			            dumpIMethod((String)iter.next());
			        }
			        for (Iterator<?> i = l.iterator(); i.hasNext();) {
			            dumpIMethod((String)i.next());
			        }
			    }
			
			    private static void dumpIMethod(String next) {
			        System.out.println(next);
			    }
			}
			"""; //

		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		String expected= """
			package test;
			import java.io.File;
			import java.util.Iterator;
			import java.util.List;
			
			public class Test {
			
			    public int foo(String x, List<? extends File> files) {
			        for (File file : files) {
			            dumpIMethod((String)file.getAbsolutePath());
			        }
			        if (x.length() == 8) {
			            int count = 0;
			            for (File file : files) {
			                count++;
			            }
			            return count;
			        }
			        return 0;
			    }
			
			    public static void dumpIMethodList(List<?> l){
			        for (Object element : l) {
			            dumpIMethod((String)element);
			        }
			        for (Object name : l) {
			            dumpIMethod((String)name);
			        }
			    }
			
			    private static void dumpIMethod(String next) {
			        System.out.println(next);
			    }
			}
			"""; //
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileSelf() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test extends ArrayList<String> {
			    void m() {
			        Iterator it = iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test;
			import java.util.*;
			public class Test extends ArrayList<String> {
			    void m() {
			        for (String s : this) {
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileIteratorAssigned() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test extends ArrayList<String> {
			    void m(ArrayList<String> strings) {
			        Iterator it;
			        it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test;
			import java.util.*;
			public class Test extends ArrayList<String> {
			    void m(ArrayList<String> strings) {
			        for (String s : strings) {
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileNoSelf() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m() {
			        Iterator it = factory().iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			    private ArrayList<String> factory() {
			        return new ArrayList<String>();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m() {
			        for (String s : factory()) {
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			    private ArrayList<String> factory() {
			        return new ArrayList<String>();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileWithNonRawSuperclass() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(MyList strings) {
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			    static class MyList extends ArrayList<String> {}
			}
			""";
		ICompilationUnit cu1= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(MyList strings) {
			        for (String s : strings) {
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			    static class MyList extends ArrayList<String> {}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileWithRawIterator() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        for (String s : strings) {
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testWhileSubtype() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<PropertyResourceBundle> bundles) {
			        Iterator it = bundles.iterator();
			        while (it.hasNext()) {
			            ResourceBundle bundle = (ResourceBundle) it.next();
			            System.out.println(bundle);
			            System.err.println(bundle);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<PropertyResourceBundle> bundles) {
			        for (ResourceBundle bundle : bundles) {
			            System.out.println(bundle);
			            System.err.println(bundle);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description)));
	}

	@Test
	public void testDoNotWhileBigChangeNeeded() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> nodes) {
			        Iterator<String> fragments= null;
			        if (nodes != null) {
			        		fragments= nodes.iterator();
			        }
			        if (fragments != null) {
			        		while (fragments.hasNext()) {
			        			System.out.println(fragments.next());
			         	}
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotWhileUsedSpecially() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            if (s.isEmpty()) {
			                it.remove();
			            } else {
			                System.out.println(s);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Ignore("""
		Either check exactly the data type (eg CopyOnWriteArrayList allows modifications)\
		 or stay away from refactoring when deletions/additions happen.\
		btw simple for loop to enhanced for loop should do the same.""")
	@Test
	public void testDoNotConcurrentModificationException() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    List<String> strings=new ArrayList<>();
			    void m(List<String> strings) {
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			        }
			    }
			    void outside(int x) {
			        strings.remove(x);
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotWhileUsedSpecially2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        Iterator it = strings.iterator();
			        String startvalue = (String) it.next();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotWhileWithIndirectIterator() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m() {
			        Iterator it = getIterator();
			        String startvalue = (String) it.next();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			        }
			    }
			    List<String> strings= new ArrayList<String>();
			    public Iterator<String> getIterator() {
			        return strings.iterator();
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotWhileWithIndirectIterator2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    public static class MyIterator implements Iterator<String> {
			        List<String> strings= new ArrayList<>();
			        Iterator<String> iterator;
			        public MyIterator() {
			           iterator= strings.iterator();
			        }
			        @Override
			        public boolean hasNext() {
			            return iterator.hasNext();
			        }
			        @Override
			        public String next() {
			            return iterator.next();
			        }
			        @Override
			        public void remove() {
			           iterator.remove();
			        }
			    }
			    void m() {
			        Iterator it = new MyIterator();
			        String startvalue = (String) it.next();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			        }
			    }
			    List<String> strings= new ArrayList<>();
			    public Iterator<String> getIterator() {
			        return strings.iterator();
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotWhileWithDoubleNext() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            String s2 = (String) it.next();
			            System.out.println(s + s2);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotWhileRaw() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List strings) {
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotWhileWrongType() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<java.net.URL> strings) {
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotWhileIssue373() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(it);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotWhileIssue190_1() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        Iterator<String> it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			        it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });

	}

	@Test
	public void testDoNotWhileIssue190_2() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<String> strings) {
			        Iterator<String> it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });

	}

	@Test
	public void testDoNotWhileNotIterable() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(MyList strings) {
			        Iterator it = strings.iterator();
			        while (it.hasNext()) {
			            String s = (String) it.next();
			            System.out.println(s);
			            System.err.println(s);
			        }
			    }
			    interface MyList {
			        Iterator<String> iterator();
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotWhileNotSubtype() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.*;
			public class Test {
			    void m(List<ResourceBundle> bundles) {
			        Iterator it = bundles.iterator();
			        while (it.hasNext()) {
			            PropertyResourceBundle bundle = (PropertyResourceBundle) it.next();
			            System.out.println(bundle);
			            System.err.println(bundle);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testDoNotAddFinalForFieldUsedBeforeInitialized() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/769
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			import java.util.ArrayList;
			import java.util.List;
			
			public class E {
			   \s
			    public interface I1 {
			        public void run();
			    }
			    private class E1 {
			        public void foo2(I1 k) {}
			    }
			    private E1 fField;
			    private List<String> fList;
			   \s
			    public E() {
			        fField = new E1();
			        fField.foo2(() -> {
			            fList.clear();
			        });
			        fList = new ArrayList<>();
			    }
			}
			"""; //
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= """
			package test;
			import java.util.ArrayList;
			import java.util.List;
			
			public class E {
			   \s
			    public interface I1 {
			        public void run();
			    }
			    private class E1 {
			        public void foo2(final I1 k) {}
			    }
			    private final E1 fField;
			    private List<String> fList;
			   \s
			    public E() {
			        fField = new E1();
			        fField.foo2(() -> {
			            fList.clear();
			        });
			        fList = new ArrayList<>();
			    }
			}
			"""; //
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1},
				new HashSet<>(Arrays.asList(FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description)));
	}

	@Test
	public void testDoNotAddFinalForFieldUsedInLambdaFieldInitializer() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/769
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E {
			    interface I {
			        void run( //
			    }
			    private String f;
			    private String g;
			    I x = () -> {
			        g.concat("abc");
			    };
			    public E() {
			        this.f= "abc";
			        this.g= "def";
			    }
			    public void foo() {
			        x.run( //
			        System.out.println(f //
			    }
			
			}
			
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);

		sample= """
			package test;
			public class E {
			    interface I {
			        void run( //
			    }
			    private final String f;
			    private String g;
			    I x = () -> {
			        g.concat("abc");
			    };
			    public E() {
			        this.f= "abc";
			        this.g= "def";
			    }
			    public void foo() {
			        x.run( //
			        System.out.println(f //
			    }
			
			}
			
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1}, new String[] {expected1},
				new HashSet<>(Arrays.asList(FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description)));
	}

	@Test
	public void testDeprecatedCleanup1() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/722
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E1 {
			
			    String blah = "blah";
			   \s
			    class Blah {
			        public static String blah2 = "blah2";
			    }
			
			    public int foo(String a, String b) {
			        System.out.println(a + b);
			        return a.length() + b.length();
			    }
			
			    /**
			     * @deprecated use {@link #foo(String, String)}
			     * @param a
			     * @param b
			     * @param c
			     * @return int
			     */
			    @Deprecated
			    public int foo(String a, String b, Object c) {
			        String k = a.toLowerCase() + Blah.blah2;
			        return foo(k, b);
			    }
			
			}
			"""; //
		pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test;
			public class E {
			
			    public static void depfunc(String a, String b, Object c) {
			        E1 d = new E1();
			        int k1= 8;
			        int y = d.foo(a, b, c);
			        System.out.println(y);
			        E1 e = new E1();
			        int z = e.foo(a, b, c);
			        System.out.println(z);
			        int v = e.foo(a, b, c);
			        System.out.println(v);
			    }
			
			    public static void depfunc2(String x, String y, Object z) {
			        E1 k = new E1();
			        k.foo(x, y, z);
			        { E1 e = new E1();
			        e.foo(x, y, z); }
			    }
			
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E.java", sample, false, null);
		enable(CleanUpConstants.REPLACE_DEPRECATED_CALLS);

		sample= """
			package test;
			public class E {
			
			    public static void depfunc(String a, String b, Object c) {
			        E1 d = new E1();
			        int k1= 8;
			        String k1_1 = a.toLowerCase() + Blah.blah2;
			        int y = d.foo(k1_1, b);
			        System.out.println(y);
			        E1 e = new E1();
			        String k2 = a.toLowerCase() + Blah.blah2;
			        int z = e.foo(k2, b);
			        System.out.println(z);
			        String k = a.toLowerCase() + Blah.blah2;
			        int v = e.foo(k, b);
			        System.out.println(v);
			    }
			
			    public static void depfunc2(String x, String y, Object z) {
			        E1 k = new E1();
			        String k1_1 = x.toLowerCase() + Blah.blah2;
			        k.foo(k1_1, y);
			        { E1 e = new E1();
			        String k1 = x.toLowerCase() + Blah.blah2;
			        e.foo(k1, y); }
			    }
			
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu2}, new String[] {expected1},
				new HashSet<>(Arrays.asList(FixMessages.InlineDeprecatedMethod_msg)));
	}

	@Test
	public void testDeprecatedCleanup2() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/722
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample1= """
			package test;
			public class E1 {
			
			    String blah = "blah";
			   \s
			    public class Blah {
			        public static String blah2 = "blah2";
			    }
			
			    public int foo(String a, String b) {
			        System.out.println(a + b);
			        return a.length() + b.length();
			    }
			
			    /**
			     * @deprecated use {@link #foo(String, String)}
			     * @param a
			     * @param b
			     * @param c
			     * @return int
			     */
			    @Deprecated
			    public int foo(String a, String b, Object c) {
			        String k = a.toLowerCase() + Blah.blah2;
			        return foo(k, b);
			    }
			
			}
			"""; //
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample1, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", true, null);
		String sample= """
			package test2;
			import test.E1;
			public class E {
			
			    public static void depfunc(String a, String b, Object c) {
			        E1 d = new E1();
			        int k1= 8;
			        int y = d.foo(a, b, c);
			        System.out.println(y);
			        E1 e = new E1();
			        int z = e.foo(a, b, c);
			        System.out.println(z);
			        int v = e.foo(a, b, c);
			        System.out.println(v);
			    }
			
			    public static void depfunc2(String x, String y, Object z) {
			        E1 k = new E1();
			        k.foo(x, y, z);
			        { E1 e = new E1();
			        e.foo(x, y, z); }
			    }
			
			}
			""";
		ICompilationUnit cu2= pack2.createCompilationUnit("E.java", sample, false, null);
		enable(CleanUpConstants.REPLACE_DEPRECATED_CALLS);

		sample= """
			package test2;
			import test.E1;
			import test.E1.Blah;
			public class E {
			
			    public static void depfunc(String a, String b, Object c) {
			        E1 d = new E1();
			        int k1= 8;
			        String k1_1 = a.toLowerCase() + Blah.blah2;
			        int y = d.foo(k1_1, b);
			        System.out.println(y);
			        E1 e = new E1();
			        String k2 = a.toLowerCase() + Blah.blah2;
			        int z = e.foo(k2, b);
			        System.out.println(z);
			        String k = a.toLowerCase() + Blah.blah2;
			        int v = e.foo(k, b);
			        System.out.println(v);
			    }
			
			    public static void depfunc2(String x, String y, Object z) {
			        E1 k = new E1();
			        String k1_1 = x.toLowerCase() + Blah.blah2;
			        k.foo(k1_1, y);
			        { E1 e = new E1();
			        String k1 = x.toLowerCase() + Blah.blah2;
			        e.foo(k1, y); }
			    }
			
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu1, cu2}, new String[] {sample1, expected1},
				new HashSet<>(Arrays.asList(FixMessages.InlineDeprecatedMethod_msg)));
	}

	@Test
	public void testDoNotDoDeprecatedCleanup1() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/722
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			public class E1 {
			
			    String blah = "blah";
			   \s
			    private static class Blah {
			        public static String blah2 = "blah2";
			    }
			
			    public int foo(String a, String b) {
			        System.out.println(a + b);
			        return a.length() + b.length();
			    }
			
			    /**
			     * @deprecated use {@link #foo(String, String)}
			     * @param a
			     * @param b
			     * @param c
			     * @return int
			     */
			    @Deprecated
			    public int foo(String a, String b, Object c) {
			        String k = a.toLowerCase() + Blah.blah2;
			        return foo(k, b);
			    }
			
			}
			"""; //
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test;
			public class E {
			
			    public static void depfunc(String a, String b, Object c) {
			        E1 d = new E1();
			        int k1= 8;
			        int y = d.foo(a, b, c);
			        System.out.println(y);
			        E1 e = new E1();
			        int z = e.foo(a, b, c);
			        System.out.println(z);
			        int v = e.foo(a, b, c);
			        System.out.println(v);
			    }
			
			    public static void depfunc2(String x, String y, Object z) {
			        E1 k = new E1();
			        k.foo(x, y, z);
			        { E1 e = new E1();
			        e.foo(x, y, z); }
			    }
			
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E.java", sample, false, null);
		enable(CleanUpConstants.REPLACE_DEPRECATED_CALLS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1, cu2 });
	}

	@Test
	public void testDoNotDoDeprecatedCleanup2() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/722
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample1= """
			package test;
			public class E1 {
			
			    String blah = "blah";
			   \s
			    protected static class Blah {
			        public static String blah2 = "blah2";
			    }
			
			    public int foo(String a, String b) {
			        System.out.println(a + b);
			        return a.length() + b.length();
			    }
			
			    /**
			     * @deprecated use {@link #foo(String, String)}
			     * @param a
			     * @param b
			     * @param c
			     * @return int
			     */
			    @Deprecated
			    public int foo(String a, String b, Object c) {
			        String k = a.toLowerCase() + Blah.blah2;
			        return foo(k, b);
			    }
			
			}
			"""; //
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample1, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", true, null);
		String sample= """
			package test2;
			import test.E1;
			public class E {
			
			    public static void depfunc(String a, String b, Object c) {
			        E1 d = new E1();
			        int k1= 8;
			        int y = d.foo(a, b, c);
			        System.out.println(y);
			        E1 e = new E1();
			        int z = e.foo(a, b, c);
			        System.out.println(z);
			        int v = e.foo(a, b, c);
			        System.out.println(v);
			    }
			
			    public static void depfunc2(String x, String y, Object z) {
			        E1 k = new E1();
			        k.foo(x, y, z);
			        { E1 e = new E1();
			        e.foo(x, y, z); }
			    }
			
			}
			""";
		ICompilationUnit cu2= pack2.createCompilationUnit("E.java", sample, false, null);
		enable(CleanUpConstants.REPLACE_DEPRECATED_CALLS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1, cu2 });
	}

	@Test
	public void testDoNotDoDeprecatedCleanup3() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/722
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample1= """
			package test;
			public class E1 {
			
			    String blah = "blah";
			   \s
			    static class Blah {
			        public static String blah2 = "blah2";
			    }
			
			    public int foo(String a, String b) {
			        System.out.println(a + b);
			        return a.length() + b.length();
			    }
			
			    /**
			     * @deprecated use {@link #foo(String, String)}
			     * @param a
			     * @param b
			     * @param c
			     * @return int
			     */
			    @Deprecated
			    public int foo(String a, String b, Object c) {
			        String k = a.toLowerCase() + Blah.blah2;
			        return foo(k, b);
			    }
			
			}
			"""; //
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample1, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", true, null);
		String sample= """
			package test2;
			import test.E1;
			public class E {
			
			    public static void depfunc(String a, String b, Object c) {
			        E1 d = new E1();
			        int k1= 8;
			        int y = d.foo(a, b, c);
			        System.out.println(y);
			        E1 e = new E1();
			        int z = e.foo(a, b, c);
			        System.out.println(z);
			        int v = e.foo(a, b, c);
			        System.out.println(v);
			    }
			
			    public static void depfunc2(String x, String y, Object z) {
			        E1 k = new E1();
			        k.foo(x, y, z);
			        { E1 e = new E1();
			        e.foo(x, y, z); }
			    }
			
			}
			""";
		ICompilationUnit cu2= pack2.createCompilationUnit("E.java", sample, false, null);
		enable(CleanUpConstants.REPLACE_DEPRECATED_CALLS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1, cu2 });
	}

	@Test
	public void testDoNotDoDeprecatedCleanup4() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/722
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample1= """
			package test;
			public class E1 {
			
			    String blah = "blah";
			   \s
			    static class Blah {
			        public static String blah2 = "blah2";
			    }
			
			    public int foo(String a, String b) {
			        System.out.println(a + b);
			        return a.length() + b.length();
			    }
			
			    /**
			     * @deprecated use {@link #foo(String, String)}
			     * @param a
			     * @param b
			     * @param c
			     * @return int
			     */
			    @Deprecated
			    public int foo(String a, String b, Object c) {
			        String k = a.toLowerCase() + this.blah;
			        return foo(k, b);
			    }
			
			}
			"""; //
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample1, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", true, null);
		String sample= """
			package test2;
			import test.E1;
			public class E {
			
			    public static void depfunc(String a, String b, Object c) {
			        E1 d = new E1();
			        int k1= 8;
			        int y = d.foo(a, b, c);
			        System.out.println(y);
			        E1 e = new E1();
			        int z = e.foo(a, b, c);
			        System.out.println(z);
			        int v = e.foo(a, b, c);
			        System.out.println(v);
			    }
			
			    public static void depfunc2(String x, String y, Object z) {
			        E1 k = new E1();
			        k.foo(x, y, z);
			        { E1 e = new E1();
			        e.foo(x, y, z); }
			    }
			
			}
			""";
		ICompilationUnit cu2= pack2.createCompilationUnit("E.java", sample, false, null);
		enable(CleanUpConstants.REPLACE_DEPRECATED_CALLS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1, cu2 });
	}

	@Test
	public void testRemoveThisIssue1211() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1211
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String sample= """
			package test1;
			
			public class A {
			    public interface PropertyChangeListener {
			        void propertyChange(Object evt);
			
			    }
			
			    private final PropertyChangeListener listener = evt -> {
			        this.clientCache.get();
			    };
			
			    public void x() {
			        PropertyChangeListener listener = evt -> {
			            this.clientCache.get();
			        };
			        listener.propertyChange(listener);
			    }
			    interface Cache<V> {
			        V get();
			    }
			
			    final Cache<String> clientCache = new Cache<>() {
			        @Override
			        public String get() {
			            listener.propertyChange(null);
			            return "";
			        }
			    };
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		sample= """
			package test1;
			
			public class A {
			    public interface PropertyChangeListener {
			        void propertyChange(Object evt);
			
			    }
			
			    private final PropertyChangeListener listener = evt -> {
			        this.clientCache.get();
			    };
			
			    public void x() {
			        PropertyChangeListener listener = evt -> {
			            clientCache.get();
			        };
			        listener.propertyChange(listener);
			    }
			    interface Cache<V> {
			        V get();
			    }
			
			    final Cache<String> clientCache = new Cache<>() {
			        @Override
			        public String get() {
			            listener.propertyChange(null);
			            return "";
			        }
			    };
			}
			""";
		String expected1= sample;
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
    }

}
