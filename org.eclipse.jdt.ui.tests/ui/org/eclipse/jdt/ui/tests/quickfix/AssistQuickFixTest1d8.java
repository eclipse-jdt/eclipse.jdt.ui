/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
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
 *     Jerome Cambon <jerome.cambon@oracle.com> - [1.8][clean up][quick assist] Convert lambda to anonymous must qualify references to 'this'/'super' - https://bugs.eclipse.org/430573
 *     Jeremie Bresson <dev@jmini.fr> - Bug 439912: [1.8][quick assist] Add quick assists to add and remove parentheses around single lambda parameter - https://bugs.eclipse.org/439912
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.NullTestUtils;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class AssistQuickFixTest1d8 extends QuickFixTest {
	@Rule
    public ProjectTestSetup projectSetup = new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

//	public static Test setUpTest() {
//		return new Java1d8ProjectTestSetup() {
//			@Override
//			protected void setUp() throws Exception {
//				JavaProjectHelper.PERFORM_DUMMY_SEARCH++;
//				super.setUp();
//			}
//			@Override
//			protected void tearDown() throws Exception {
//				super.tearDown();
//				JavaProjectHelper.PERFORM_DUMMY_SEARCH--;
//			}
//		};
//	}

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);

		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testAssignParamToField1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface I {
			    default void foo(int x) {
			        System.out.println(x);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", str, false, null);

		int offset= str.indexOf("x");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testAssignParamToField2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface I {
			    static void bar(int x) {
			        System.out.println(x);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", str, false, null);

		int offset= str.indexOf("x");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testConvertToLambda1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    void method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(new I() {
			            public void method() {
			                System.out.println();
			                System.out.println();
			            }
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I {
			    void method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(() -> {
			            System.out.println();
			            System.out.println();
			        });
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    void method(int a, int b);
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(new I() {
			            public void method(int a, int b) {
			                System.out.println(a+b);
			                System.out.println(a+b);
			            }
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I {
			    void method(int a, int b);
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar((a, b) -> {
			            System.out.println(a+b);
			            System.out.println(a+b);
			        });
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    void method();
			    boolean equals(Object obj);
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(new I() {
			            public void method() {
			                System.out.println();
			            }
			            public boolean equals(Object obj) {
			                return false;
			            }
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	@Test
	public void testConvertToLambda4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    void method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(new I() {
			            int count=0;
			            public void method() {
			                System.out.println();
			                count++;
			            }
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	@Test
	public void testConvertToLambda5() throws Exception {
		//Quick assist should not be offered in 1.7 mode
		JavaProjectHelper.set17CompilerOptions(fJProject1);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		try {
			String str= """
				package test1;
				interface I {
				    void method();
				}
				public class E {
				    void bar(I i) {
				    }
				    void foo() {
				        bar(new I() {
				            public void method() {
				                System.out.println();
				            }
				        });
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			int offset= str.indexOf("I()");
			AssistContext context= getCorrectionContext(cu, offset, 0);
			assertNoErrors(context);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 1);
			assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
		} finally {
			JavaProjectHelper.set18CompilerOptions(fJProject1);
		}
	}

	@Test
	public void testConvertToLambda6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    boolean equals(Object obj);
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(new I() {
			            public boolean equals(Object obj) {
			                return false;
			            }
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	@Test
	public void testConvertToLambda7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			abstract class C {
			    abstract void method();
			}
			public class E {
			    void bar(C c) {
			    }
			    void foo() {
			        bar(new C() {
			            public void method() {
			                System.out.println();
			            }
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("C()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	@Test
	public void testConvertToLambda8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    void method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(new I() {
			            public void method() {
			                System.out.println();
			            }
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I {
			    void method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(() -> System.out.println());
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    int method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(new I() {
			            public int method() {
			                return 1;
			            }
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I {
			    int method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(() -> 1);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    int foo(String s);
			}
			
			interface J {
			    Integer foo(String s);
			}
			
			public class X {
			    static void goo(I i) { }
			
			    static void goo(J j) { }
			
			    public static void main(String[] args) {
			        goo(new I() {
			            @Override
			            public int foo(String s) {
			                return 0;
			            }
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", str, false, null);

		int offset= str.indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I {
			    int foo(String s);
			}
			
			interface J {
			    Integer foo(String s);
			}
			
			public class X {
			    static void goo(I i) { }
			
			    static void goo(J j) { }
			
			    public static void main(String[] args) {
			        goo((I) s -> 0);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    int foo(String s);
			}
			
			interface J {
			    Integer foo(String s);
			}
			
			public class X extends Y {
			    static void goo(I i) { }
			    public static void main(String[] args) {
			        goo(new I() {
			            @Override
			            public int foo(String s) {
			                return 0;
			            }
			        });
			    }
			}
			
			class Y {
			    private static void goo(J j) { }   \s
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", str, false, null);

		int offset= str.indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I {
			    int foo(String s);
			}
			
			interface J {
			    Integer foo(String s);
			}
			
			public class X extends Y {
			    static void goo(I i) { }
			    public static void main(String[] args) {
			        goo(s -> 0);
			    }
			}
			
			class Y {
			    private static void goo(J j) { }   \s
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    int foo(String s);
			}
			
			interface J {
			    Integer foo(String s);
			}
			
			public class X extends Y {
			    static void goo(I i) { }
			    public static void main(String[] args) {
			        goo(new I() {
			            @Override
			            public int foo(String s) {
			                return 0;
			            }
			        });
			    }
			}
			
			class Y {
			    static void goo(J j) { }   \s
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", str, false, null);

		int offset= str.indexOf("I()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I {
			    int foo(String s);
			}
			
			interface J {
			    Integer foo(String s);
			}
			
			public class X extends Y {
			    static void goo(I i) { }
			    public static void main(String[] args) {
			        goo((I) s -> 0);
			    }
			}
			
			class Y {
			    static void goo(J j) { }   \s
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface J {
			    <M> J run(M x);
			}
			
			class Test {
			    J j = new J() {
			        @Override
			        public <M> J run(M x) {
			            return null;
			        }
			    };   \s
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str, false, null);

		int offset= str.indexOf("J()"); // generic lambda not allowed
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, FixMessages.LambdaExpressionsFix_convert_to_lambda_expression);
	}

	@Test
	public void testConvertToLambda14() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			@FunctionalInterface
			interface FI {
			    int foo(int x, int y, int z);
			}
			
			class C {
			    int i;
			    private void test(int x) {
			        int y;
			        FI fi = new FI() {
			            @Override
			            public int foo(int x/*km*/, int i /*inches*/, int y/*yards*/) {
			                return x + i + y;
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		int offset= str.indexOf("FI()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			@FunctionalInterface
			interface FI {
			    int foo(int x, int y, int z);
			}
			
			class C {
			    int i;
			    private void test(int x) {
			        int y;
			        FI fi = (x1/*km*/, i /*inches*/, y1/*yards*/) -> x1 + i + y1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			@FunctionalInterface
			interface FI {
			    int foo(int x, int y, int z);
			}
			
			class C {
			    int i;
			    private void test(int x, int y, int z) {
			        FI fi = new FI() {
			            @Override
			            public int foo(int a, int b, int z) {
			                int x= 0, y=0;\s
			                return x + y + z;
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		int offset= str.indexOf("FI()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			@FunctionalInterface
			interface FI {
			    int foo(int x, int y, int z);
			}
			
			class C {
			    int i;
			    private void test(int x, int y, int z) {
			        FI fi = (a, b, z1) -> {
			            int x1= 0, y1=0;\s
			            return x1 + y1 + z1;
			        };
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda16() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface FI {
			    void foo();
			}
			
			class C1 {
			    void fun1() {
			        int c = 0; // [1]
			        FI test = new FI() {
			            @Override
			            public void foo() {
			                for (int c = 0; c < 10;) { /* [2] */ }
			                for (int c = 0; c < 20;) { /* [3] */ }
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", str, false, null);

		int offset= str.indexOf("FI()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface FI {
			    void foo();
			}
			
			class C1 {
			    void fun1() {
			        int c = 0; // [1]
			        FI test = () -> {
			            for (int c1 = 0; c1 < 10;) { /* [2] */ }
			            for (int c2 = 0; c2 < 20;) { /* [3] */ }
			        };
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda17() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface X {
			    void foo();
			}
			
			public class CX {
			    private void fun(int a) {
			        X x= new X() {
			            @Override
			            public void foo() {
			                int a;\s
			                int a1;
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("CX.java", str, false, null);

		int offset= str.indexOf("X()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface X {
			    void foo();
			}
			
			public class CX {
			    private void fun(int a) {
			        X x= () -> {
			            int a2;\s
			            int a1;
			        };
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda18() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface FIOther {
			    void run(int x);
			}
			
			public class TestOther {
			    void init() {
			        String x;
			        m(x1 -> {
			            FIOther fi = new FIOther() {
			                @Override
			                public void run(int x1) {\s
			                    return;
			                }
			            };
			        });
			    }
			
			    void m(FIOther fi) {
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("TestOther.java", str, false, null);

		int offset= str.indexOf("FIOther()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface FIOther {
			    void run(int x);
			}
			
			public class TestOther {
			    void init() {
			        String x;
			        m(x1 -> {
			            FIOther fi = x11 -> {\s
			                return;
			            };
			        });
			    }
			
			    void m(FIOther fi) {
			    };
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda19() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class C1 {
			    Runnable r1 = new/*[1]*/ Runnable() {
			        @Override @A @Deprecated
			        public void run() {
			        }
			    };
			    Runnable r2 = new/*[2]*/ Runnable() {
			        @Override @Deprecated
			        public void run() {
			        }
			    };
			}
			@interface A {}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", str, false, null);

		int offset= str.indexOf("new/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected= """
			package test;
			public class C1 {
			    Runnable r1 = () -> {
			    };
			    Runnable r2 = new/*[2]*/ Runnable() {
			        @Override @Deprecated
			        public void run() {
			        }
			    };
			}
			@interface A {}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		offset= str.indexOf("new/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test;
			public class C1 {
			    Runnable r1 = new/*[1]*/ Runnable() {
			        @Override @A @Deprecated
			        public void run() {
			        }
			    };
			    Runnable r2 = () -> {
			    };
			}
			@interface A {}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda20() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class C1 {
			    FI fi= new  FI() {
			        @Override
			        public void foo(@A String... strs) {
			        }
			    };
			}
			interface FI {
			    void foo(String... strs);
			}
			@interface A {}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", str, false, null);

		int offset= str.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected= """
			package test;
			public class C1 {
			    FI fi= (@A String... strs) -> {
			    };
			}
			interface FI {
			    void foo(String... strs);
			}
			@interface A {}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda21() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			public class C1 {
			    FI fi= new  FI() {
			        @Override
			        public void foo(@A String... strs) {
			        }
			    };
			}
			interface FI {
			    void foo(String... strs);
			}
			@Target(ElementType.TYPE_USE)
			@interface A {}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", str, false, null);

		int offset= str.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected= """
			package test;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			public class C1 {
			    FI fi= (@A String... strs) -> {
			    };
			}
			interface FI {
			    void foo(String... strs);
			}
			@Target(ElementType.TYPE_USE)
			@interface A {}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda22() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.io.IOException;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.ArrayList;
			public class C1 {
			    FI fi1 = new/*[1]*/ FI() {
			        @Override
			        public void foo(java.util.@T ArrayList<IOException> x) {
			        }
			    };
			    FI fi2 = new/*[2]*/ FI() {
			        @Override
			        public void foo(ArrayList<@T(val1=0, val2=8) IOException> x) {
			        }
			    };
			    FI fi3 = new/*[3]*/ FI() {
			        @Override
			        public void foo(ArrayList<@T(val1=0) IOException> x) {
			        }
			    };
			}
			interface FI {
			    void foo(ArrayList<IOException> x);
			}
			@Target(ElementType.TYPE_USE)
			@interface T {
			    int val1() default 1;
			    int val2() default -1;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", str, false, null);

		int offset= str.indexOf("new/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected= """
			package test;
			import java.io.IOException;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.ArrayList;
			public class C1 {
			    FI fi1 = (java.util.@T ArrayList<IOException> x) -> {
			    };
			    FI fi2 = new/*[2]*/ FI() {
			        @Override
			        public void foo(ArrayList<@T(val1=0, val2=8) IOException> x) {
			        }
			    };
			    FI fi3 = new/*[3]*/ FI() {
			        @Override
			        public void foo(ArrayList<@T(val1=0) IOException> x) {
			        }
			    };
			}
			interface FI {
			    void foo(ArrayList<IOException> x);
			}
			@Target(ElementType.TYPE_USE)
			@interface T {
			    int val1() default 1;
			    int val2() default -1;
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		offset= str.indexOf("new/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test;
			import java.io.IOException;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.ArrayList;
			public class C1 {
			    FI fi1 = new/*[1]*/ FI() {
			        @Override
			        public void foo(java.util.@T ArrayList<IOException> x) {
			        }
			    };
			    FI fi2 = (ArrayList<@T(val1=0, val2=8) IOException> x) -> {
			    };
			    FI fi3 = new/*[3]*/ FI() {
			        @Override
			        public void foo(ArrayList<@T(val1=0) IOException> x) {
			        }
			    };
			}
			interface FI {
			    void foo(ArrayList<IOException> x);
			}
			@Target(ElementType.TYPE_USE)
			@interface T {
			    int val1() default 1;
			    int val2() default -1;
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		offset= str.indexOf("new/*[3]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test;
			import java.io.IOException;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			import java.util.ArrayList;
			public class C1 {
			    FI fi1 = new/*[1]*/ FI() {
			        @Override
			        public void foo(java.util.@T ArrayList<IOException> x) {
			        }
			    };
			    FI fi2 = new/*[2]*/ FI() {
			        @Override
			        public void foo(ArrayList<@T(val1=0, val2=8) IOException> x) {
			        }
			    };
			    FI fi3 = (ArrayList<@T(val1=0) IOException> x) -> {
			    };
			}
			interface FI {
			    void foo(ArrayList<IOException> x);
			}
			@Target(ElementType.TYPE_USE)
			@interface T {
			    int val1() default 1;
			    int val2() default -1;
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda23() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			public class C1 {
			    FI1 fi1 = new/*[1]*/ FI1() {
			        @Override
			        public void foo(String @T [] x[]) {
			        }
			    };
			    FI1 fi2 = new/*[2]*/ FI1() {
			        @Override
			        public void foo(String [] x @T[]) {
			        }
			    };
			}
			interface FI1 {
			    void foo(String[] x []);
			}
			@Target(ElementType.TYPE_USE)
			@interface T {}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", str, false, null);

		int offset= str.indexOf("new/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected= """
			package test;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			public class C1 {
			    FI1 fi1 = (String @T [] x[]) -> {
			    };
			    FI1 fi2 = new/*[2]*/ FI1() {
			        @Override
			        public void foo(String [] x @T[]) {
			        }
			    };
			}
			interface FI1 {
			    void foo(String[] x []);
			}
			@Target(ElementType.TYPE_USE)
			@interface T {}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		offset= str.indexOf("new/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			public class C1 {
			    FI1 fi1 = new/*[1]*/ FI1() {
			        @Override
			        public void foo(String @T [] x[]) {
			        }
			    };
			    FI1 fi2 = (String [] x @T[]) -> {
			    };
			}
			interface FI1 {
			    void foo(String[] x []);
			}
			@Target(ElementType.TYPE_USE)
			@interface T {}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda24() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			public class C1 {
			    FI1 fi1 = new FI1() {
			        @Override
			        public void foo(int i, String[] @T... x) {
			        }
			    };
			}
			interface FI1 {
			    void foo(int i, String[] ... x);
			}
			@Target(ElementType.TYPE_USE)
			@interface T {}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", str, false, null);

		int offset= str.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected= """
			package test;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			public class C1 {
			    FI1 fi1 = (int i, String[] @T... x) -> {
			    };
			}
			interface FI1 {
			    void foo(int i, String[] ... x);
			}
			@Target(ElementType.TYPE_USE)
			@interface T {}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertToLambda25() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class C1 {
			    Runnable run = new Runnable() {
			        @Override
			        public synchronized void run() {
			        }
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", str, false, null);

		int offset= str.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testConvertToLambda26() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class C1 {
			    Runnable run = new Runnable() {
			        @Override
			        public strictfp void run() {
			        }
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", str, false, null);

		int offset= str.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testConvertToLambda27() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			interface FI {
			    int e= 0;
			    void run(int x);
			}
			
			class Test {
			    {
			        FI fi = new FI() {
			            @Override
			            public void run(int e) {
			                FI fi = new FI() {
			                    @Override
			                    public void run(int e) { // [1]
			                        return;
			                    }
			                };
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str, false, null);

		int offset= str.indexOf("run(int e) { // [1]");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			
			interface FI {
			    int e= 0;
			    void run(int x);
			}
			
			class Test {
			    {
			        FI fi = new FI() {
			            @Override
			            public void run(int e) {
			                FI fi = e1 -> { // [1]
			                    return;
			                };
			            }
			        };
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambda28() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String buf=
		"""
			package test;
			public class C1 {
			    private final String s;
			    Runnable run = new Runnable() {
			        @Override
			        public void run() {
			           for (int i=0; i < s.length(); ++i) {
			               int j = i;
			           }
			        }
			    };
			    public C1() {
			        s = "abc";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf, false, null);

		int offset= buf.toString().indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testConvertToLambda29() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String buf= """
			package test;
			public class C1 {
			    private final String s = "ABC";
			    Runnable run = new Runnable() {
			        @Override
			        public void run() {
			           for (int i=0; i < s.length(); ++i) {
			               int j = i;
			           }
			        }
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf, false, null);

		int offset= buf.toString().indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test;
			public class C1 {
			    private final String s = "ABC";
			    Runnable run = () -> {
			       for (int i=0; i < s.length(); ++i) {
			           int j = i;
			       }
			    };
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}
	@Test
	public void testConvertToLambda30() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String buf= """
			package test;
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
		ICompilationUnit cu= pack1.createCompilationUnit("C1.java", buf, false, null);

		int offset= buf.toString().indexOf("new IOverwriteQuery");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test;
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
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToLambdaAmbiguousOverridden() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			import java.util.function.Predicate;
			
			public class Test {
			    void foo(ArrayList<String> list) {
			        list.removeIf(new Predicate<String>() {
			            @Override
			            public boolean test(String t) {
			                return t.isEmpty();
			            }
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str, false, null);

		int offset= str.indexOf("public boolean test(");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			
			public class Test {
			    void foo(ArrayList<String> list) {
			        list.removeIf(String::isEmpty);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    void method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(() -> {
			            System.out.println();
			            System.out.println();
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I {
			    void method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(new I() {
			            @Override
			            public void method() {
			                System.out.println();
			                System.out.println();
			            }
			        });
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    void method(int a, int b);
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar((int a, int b) -> {
			            System.out.println(a+b);
			            System.out.println(a+b);
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I {
			    void method(int a, int b);
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(new I() {
			            @Override
			            public void method(int a, int b) {
			                System.out.println(a+b);
			                System.out.println(a+b);
			            }
			        });
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    void method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(() -> System.out.println());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I {
			    void method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(new I() {
			            @Override
			            public void method() {
			                System.out.println();
			            }
			        });
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I {
			    int method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(() -> 1);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I {
			    int method();
			}
			public class E {
			    void bar(I i) {
			    }
			    void foo() {
			        bar(new I() {
			            @Override
			            public int method() {
			                return 1;
			            }
			        });
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface FX {
			    default int defaultMethod(String x) {
			        return -1;
			    }
			
			    int foo(int x);
			}
			
			class TestX {
			    FX fxx = x -> {
			        return (new FX() {
			            @Override
			            public int foo(int x) {
			                return 0;
			            }
			        }).defaultMethod("a");
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("TestX.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface FX {
			    default int defaultMethod(String x) {
			        return -1;
			    }
			
			    int foo(int x);
			}
			
			class TestX {
			    FX fxx = new FX() {
			        @Override
			        public int foo(int x) {
			            return (new FX() {
			                @Override
			                public int foo(int x) {
			                    return 0;
			                }
			            }).defaultMethod("a");
			        }
			    };
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.UnaryOperator;
			
			public class Snippet {
			    UnaryOperator<String> fi3 = x -> {
			        return x.toString();
			    };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.function.UnaryOperator;
			
			public class Snippet {
			    UnaryOperator<String> fi3 = new UnaryOperator<String>() {
			        @Override
			        public String apply(String x) {
			            return x.toString();
			        }
			    };
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	// Bug 427694: [1.8][compiler] Functional interface not identified correctly
	public void _testConvertToAnonymousClassCreation7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface I { Object m(Class c); }
			interface J<S> { S m(Class<?> c); }
			interface K<T> { T m(Class<?> c); }
			interface Functional<S,T> extends I, J<S>, K<T> {}
			
			class C {
			    Functional<?, ?> fun= (c) -> { return null;};
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			interface I { Object m(Class c); }
			interface J<S> { S m(Class<?> c); }
			interface K<T> { T m(Class<?> c); }
			interface Functional<S,T> extends I, J<S>, K<T> {}
			
			class C {
			    Functional<?, ?> fun= new Functional<Object, Object>() {
			        @Override
			        public Object m(Class c) {
			            return null;
			        }
			    };
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.IntSupplier;
			public class B extends C {
			    private int var;
			    B() {
			        IntSupplier i = () -> {
			            int j = this.var;
			            super.o();
			            int k = super.varC;
			            return this.m();
			        };
			    }
			
			    public int m() {
			        return 7;
			    }
			}
			
			class C {
			    int varC;
			    public void o() {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.function.IntSupplier;
			public class B extends C {
			    private int var;
			    B() {
			        IntSupplier i = new IntSupplier() {
			            @Override
			            public int getAsInt() {
			                int j = B.this.var;
			                B.super.o();
			                int k = B.super.varC;
			                return B.this.m();
			            }
			        };
			    }
			
			    public int m() {
			        return 7;
			    }
			}
			
			class C {
			    int varC;
			    public void o() {}
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreation9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.IntSupplier;
			public class D {
			    D() {
			        F<Object> f = new F<Object>() {
			            int x= 10;
			            @Override
			            public void run() {
			                IntSupplier i = () -> {
			                    class CX {
			                        int n=10;
			                        {
			                            this.n= 0;
			                        }
			                    }
			                    D.this.n();
			                    return this.x;
			                };
			            }
			        };
			    }
			    public int n() {
			        return 7;
			    }
			
			    @FunctionalInterface
			    public interface F<T> {
			        void run();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("D.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.function.IntSupplier;
			
			import test1.D.F;
			public class D {
			    D() {
			        F<Object> f = new F<Object>() {
			            int x= 10;
			            @Override
			            public void run() {
			                IntSupplier i = new IntSupplier() {
			                    @Override
			                    public int getAsInt() {
			                        class CX {
			                            int n=10;
			                            {
			                                this.n= 0;
			                            }
			                        }
			                        D.this.n();
			                        return F.this.x;
			                    }
			                };
			            }
			        };
			    }
			    public int n() {
			        return 7;
			    }
			
			    @FunctionalInterface
			    public interface F<T> {
			        void run();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToAnonymousClassCreationWithParameterName() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.IntFunction;
			public class E {
			    IntFunction<String> toString= (int i) -> Integer.toString(i);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.function.IntFunction;
			public class E {
			    IntFunction<String> toString= new IntFunction<String>() {
			        @Override
			        public String apply(int i) {
			            return Integer.toString(i);
			        }
			    };
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToBlock1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI1 fi1a= x -> -1;
			}
			
			@FunctionalInterface
			interface FI1 {
			    int foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			class E {
			    FI1 fi1a= x -> {
			        return -1;
			    };
			}
			
			@FunctionalInterface
			interface FI1 {
			    int foo(int x);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToBlock2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI1 fi1b= x -> m1();
			    int m1(){ return 0; }
			}
			
			@FunctionalInterface
			interface FI1 {
			    int foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			class E {
			    FI1 fi1b= x -> {
			        return m1();
			    };
			    int m1(){ return 0; }
			}
			
			@FunctionalInterface
			interface FI1 {
			    int foo(int x);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToBlock3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI2 fi2b= x -> m1();
			    int m1() { return 0; }
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			class E {
			    FI2 fi2b= x -> {
			        m1();
			    };
			    int m1() { return 0; }
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToBlock4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI2 fi2a= x -> System.out.println();
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			class E {
			    FI2 fi2a= x -> {
			        System.out.println();
			    };
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI1 fi1= x -> {
			        return x=0;
			    };
			}
			
			@FunctionalInterface
			interface FI1 {
			    int foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			class E {
			    FI1 fi1= x -> x=0;
			}
			
			@FunctionalInterface
			interface FI1 {
			    int foo(int x);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI2 fi2= x -> {
			        new Runnable() {
			            public void run() {
			            }
			        };
			    };
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			class E {
			    FI2 fi2= x -> new Runnable() {
			        public void run() {
			        }
			    };
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI2 fi2= x -> { m1(); };
			    int m1(){ return 0; }
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			class E {
			    FI2 fi2= x -> m1();
			    int m1(){ return 0; }
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI2 fi2= x -> {
			        super.toString();
			    };
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			class E {
			    FI2 fi2= x -> super.toString();
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI2 fi2= x -> {
			        --x;
			    };
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			class E {
			    FI2 fi2= x -> --x;
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testChangeLambdaBodyToExpression6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI2 fi2z= x -> { };
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_change_lambda_body_to_expression);
	}

	@Test
	public void testChangeLambdaBodyToExpression7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI2 fi2c = x ->    {
			        return;
			    };
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_change_lambda_body_to_expression);
	}

	@Test
	public void testChangeLambdaBodyToExpression8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
			    FI2 fi2c = x ->    {
			        int n= 0;
			    };
			}
			
			@FunctionalInterface
			interface FI2 {
			    void foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_change_lambda_body_to_expression);
	}

	@Test
	public void testExtractLambdaBodyToMethod1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
				public void foo1(int a) {
					FI2 k = (e) -> {
						int x = e + 3;
						if (x > 3) {
							return a;
						}
						return x;
					};
					k.foo(3);
				}
			}
			
			@FunctionalInterface
			interface FI2 {
			    int foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("+");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_extractmethod_description);
		String expected1= """
			package test1;
			class E {
				public void foo1(int a) {
					FI2 k = (e) -> extracted(a, e);
					k.foo(3);
				}
			
			    private int extracted(int a, int e) {
			        int x = e + 3;
			        if (x > 3) {
			        	return a;
			        }
			        return x;
			    }
			}
			
			@FunctionalInterface
			interface FI2 {
			    int foo(int x);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testExtractLambdaBodyToMethod2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
				public void foo1(int a) {
					FI2 k = (e) -> {
						int x = e + 3;
						if (x > 3) {
							return a;
						}
						return x;
					};
					k.foo(3);
				}
			}
			
			@FunctionalInterface
			interface FI2 {
			    int foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("e + 3");
		AssistContext context= getCorrectionContext(cu, offset, 5);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			class E {
				public void foo1(int a) {
					FI2 k = (e) -> extracted(a, e);
					k.foo(3);
				}
			
			    private int extracted(int a, int e) {
			        int x = e + 3;
			        if (x > 3) {
			        	return a;
			        }
			        return x;
			    }
			}
			
			@FunctionalInterface
			interface FI2 {
			    int foo(int x);
			}
			""";

		String expected2= """
			package test1;
			class E {
				public void foo1(int a) {
					FI2 k = (e) -> {
						int x = extracted(e);
						if (x > 3) {
							return a;
						}
						return x;
					};
					k.foo(3);
				}
			
			    private int extracted(int e) {
			        return e + 3;
			    }
			}
			
			@FunctionalInterface
			interface FI2 {
			    int foo(int x);
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testExtractLambdaBodyToMethod3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			class E {
				public void foo1(int a) {
					FI2 k = (e) -> {
						int x = e + 3;
			            System.out.println("help");
						if (x > 3) {
							return a;
						}
						return x;
					};
					k.foo(3);
				}
			}
			
			@FunctionalInterface
			interface FI2 {
			    int foo(int x);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("elp");
		AssistContext context= getCorrectionContext(cu, offset, 5);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			class E {
				public void foo1(int a) {
					FI2 k = (e) -> extracted(a, e);
					k.foo(3);
				}
			
			    private int extracted(int a, int e) {
			        int x = e + 3;
			        System.out.println("help");
			        if (x > 3) {
			        	return a;
			        }
			        return x;
			    }
			}
			
			@FunctionalInterface
			interface FI2 {
			    int foo(int x);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testBug433754() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			class E {
			    private void foo() {
			        for (String str : new String[1]) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("str");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			
			class E {
			    private void foo() {
			        String[] strings = new String[1];
			        for (int i = 0; i < strings.length; i++) {
			            String str = strings[i];
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testAddInferredLambdaParamTypes1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E1 {
			    FI fi= (i, s) -> {};
			}
			interface FI {
			    void foo(Integer i, String[]... s);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    FI fi= (Integer i, String[][] s) -> {};\n"); // no varargs
		buf.append("}\n");
		buf.append("interface FI {\n");
		buf.append("    void foo(Integer i, String[]... s);\n");
		buf.append("}\n");
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= buf.toString();

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testAddInferredLambdaParamTypes2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Function;
			public class E2<T> {
			    Function<T, Object> fi = t -> null;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.function.Function;
			public class E2<T> {
			    Function<T, Object> fi = (T t) -> null;
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testAddInferredLambdaParamTypes3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.IntFunction;
			import java.util.function.Supplier;
			public class E3 {
			    Supplier<String> s = () -> "";
			    IntFunction<Object> ifn= (int i) -> null;\s
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E3.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_add_inferred_lambda_parameter_types);

		offset= str.indexOf("-> null");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_add_inferred_lambda_parameter_types);
	}

	@Test
	public void testAddInferredLambdaParamTypes4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			public class E4 {
			    FI fi= (i, s) -> {};
			}
			interface FI {
			    void foo(@A Integer i, @A String @B [] s @C []);
			}
			@Target(ElementType.TYPE_USE)
			@interface A {}
			@Target(ElementType.TYPE_USE)
			@interface B {}
			@Target(ElementType.TYPE_USE)
			@interface C {}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E4.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			public class E4 {
			    FI fi= (@A Integer i, @A String @C [] @B [] s) -> {};
			}
			interface FI {
			    void foo(@A Integer i, @A String @B [] s @C []);
			}
			@Target(ElementType.TYPE_USE)
			@interface A {}
			@Target(ElementType.TYPE_USE)
			@interface B {}
			@Target(ElementType.TYPE_USE)
			@interface C {}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E1 {
			    Baz bm = E1::test;
			    static <X> void test(X x) {}
			}
			interface Foo<T, N extends Number> {
			    void m(N arg2);
			    void m(T arg1);
			}
			interface Baz extends Foo<Integer, Integer> {}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		int offset= str.indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E1 {
			    Baz bm = arg2 -> test(arg2);
			    static <X> void test(X x) {}
			}
			interface Foo<T, N extends Number> {
			    void m(N arg2);
			    void m(T arg1);
			}
			interface Baz extends Foo<Integer, Integer> {}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			interface J<R> {
			    <T> R run(T t);
			}
			public class E2 {
			    J<String> j1 = E2::<Object>test;   \s
			   \s
			    static <T> String test(T t) {
			        return "";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str, false, null);

		int offset= str.indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_lambda_expression);
	}

	@Test
	public void testConvertMethodReferenceToLambda3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.HashSet;
			import java.util.function.*;
			class E3<T> {
			    IntFunction<int[][][]> ma = int[][][]::/*[1]*/new;
			    Supplier<MyHashSet<Integer>> mb = MyHashSet::/*[2]*/new;
			    Function<T, MyHashSet<Number>> mc = MyHashSet<Number>::/*[3]*/<T>new;
			    Function<String, MyHashSet<Integer>> md = MyHashSet::/*[4]*/new;
			}
			class MyHashSet<T> extends HashSet<T> {
			    public MyHashSet() {}
			    public <A> MyHashSet(A a) {}
			    public MyHashSet(String i) {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E3.java", str, false, null);

		// [1]
		int offset= str.indexOf("::/*[1]*/") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			import java.util.HashSet;
			import java.util.function.*;
			class E3<T> {
			    IntFunction<int[][][]> ma = arg0 -> new int[arg0][][];
			    Supplier<MyHashSet<Integer>> mb = MyHashSet::/*[2]*/new;
			    Function<T, MyHashSet<Number>> mc = MyHashSet<Number>::/*[3]*/<T>new;
			    Function<String, MyHashSet<Integer>> md = MyHashSet::/*[4]*/new;
			}
			class MyHashSet<T> extends HashSet<T> {
			    public MyHashSet() {}
			    public <A> MyHashSet(A a) {}
			    public MyHashSet(String i) {}
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [2]
		offset= str.indexOf("::/*[2]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		expected= """
			package test1;
			import java.util.HashSet;
			import java.util.function.*;
			class E3<T> {
			    IntFunction<int[][][]> ma = int[][][]::/*[1]*/new;
			    Supplier<MyHashSet<Integer>> mb = () -> new MyHashSet<>();
			    Function<T, MyHashSet<Number>> mc = MyHashSet<Number>::/*[3]*/<T>new;
			    Function<String, MyHashSet<Integer>> md = MyHashSet::/*[4]*/new;
			}
			class MyHashSet<T> extends HashSet<T> {
			    public MyHashSet() {}
			    public <A> MyHashSet(A a) {}
			    public MyHashSet(String i) {}
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [3]
		offset= str.indexOf("::/*[3]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test1;
			import java.util.HashSet;
			import java.util.function.*;
			class E3<T> {
			    IntFunction<int[][][]> ma = int[][][]::/*[1]*/new;
			    Supplier<MyHashSet<Integer>> mb = MyHashSet::/*[2]*/new;
			    Function<T, MyHashSet<Number>> mc = arg0 -> new <T>MyHashSet<Number>(arg0);
			    Function<String, MyHashSet<Integer>> md = MyHashSet::/*[4]*/new;
			}
			class MyHashSet<T> extends HashSet<T> {
			    public MyHashSet() {}
			    public <A> MyHashSet(A a) {}
			    public MyHashSet(String i) {}
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [4]
		offset= str.indexOf("::/*[4]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		expected= """
			package test1;
			import java.util.HashSet;
			import java.util.function.*;
			class E3<T> {
			    IntFunction<int[][][]> ma = int[][][]::/*[1]*/new;
			    Supplier<MyHashSet<Integer>> mb = MyHashSet::/*[2]*/new;
			    Function<T, MyHashSet<Number>> mc = MyHashSet<Number>::/*[3]*/<T>new;
			    Function<String, MyHashSet<Integer>> md = arg0 -> new MyHashSet<>(arg0);
			}
			class MyHashSet<T> extends HashSet<T> {
			    public MyHashSet() {}
			    public <A> MyHashSet(A a) {}
			    public MyHashSet(String i) {}
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertMethodReferenceToLambda4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Function;
			public class E4 {
			    Function<String, String> p1 = E4::<Float>staticMethod;
			    static <F> String staticMethod(String s) {
			        return "s";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E4.java", str, false, null);

		int offset= str.indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.function.Function;
			public class E4 {
			    Function<String, String> p1 = arg0 -> E4.<Float>staticMethod(arg0);
			    static <F> String staticMethod(String s) {
			        return "s";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.*;
			public class E5<T> {
			    <F> String method1() {
			        return "a";
			    }
			    <F> String method1(E5<T> e) {
			        return "b";
			    }
			}
			class Sub extends E5<Integer> {
			    Supplier<String> s1 = super::/*[1]*/method1;
			    Supplier<String> s2 = Sub.super::/*[2]*/<Float>method1;
			    Function<E5<Integer>, String> s3 = super::/*[3]*/<Float>method1;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E5.java", str, false, null);

		// [1]
		int offset= str.indexOf("::/*[1]*/") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			import java.util.function.*;
			public class E5<T> {
			    <F> String method1() {
			        return "a";
			    }
			    <F> String method1(E5<T> e) {
			        return "b";
			    }
			}
			class Sub extends E5<Integer> {
			    Supplier<String> s1 = () -> super./*[1]*/method1();
			    Supplier<String> s2 = Sub.super::/*[2]*/<Float>method1;
			    Function<E5<Integer>, String> s3 = super::/*[3]*/<Float>method1;
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [2]
		offset= str.indexOf("::/*[2]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test1;
			import java.util.function.*;
			public class E5<T> {
			    <F> String method1() {
			        return "a";
			    }
			    <F> String method1(E5<T> e) {
			        return "b";
			    }
			}
			class Sub extends E5<Integer> {
			    Supplier<String> s1 = super::/*[1]*/method1;
			    Supplier<String> s2 = () -> Sub.super.<Float>method1();
			    Function<E5<Integer>, String> s3 = super::/*[3]*/<Float>method1;
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [3]
		offset= str.indexOf("::/*[3]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test1;
			import java.util.function.*;
			public class E5<T> {
			    <F> String method1() {
			        return "a";
			    }
			    <F> String method1(E5<T> e) {
			        return "b";
			    }
			}
			class Sub extends E5<Integer> {
			    Supplier<String> s1 = super::/*[1]*/method1;
			    Supplier<String> s2 = Sub.super::/*[2]*/<Float>method1;
			    Function<E5<Integer>, String> s3 = arg0 -> super.<Float>method1(arg0);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertMethodReferenceToLambda6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.*;
			public class E6<T> {
			    <F> String method1() {
			        return "a";
			    }
			    <F> String method1(E6<T> e) {
			        return "b";
			    }
			    Supplier<String> v1 = new E6<Integer>()::/*[1]*/method1;
			    Supplier<String> v2 = this::/*[2]*/<Float>method1;
			    Function<E6<Integer>, String> v3 = new E6<Integer>()::/*[3]*/<Float>method1;
			    T1[] ts = new T1[5];
			    BiFunction<Integer, Integer, Integer> m6 = ts[1]::/*[4]*/bar;
			    int[] is = new int[5];
			    Supplier<int[]> m10 = is::/*[5]*/clone;
			}
			class T1 {
			    int bar(int i, int j) { return i + j; }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E6.java", str, false, null);

		// [1]
		int offset= str.indexOf("::/*[1]*/") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			import java.util.function.*;
			public class E6<T> {
			    <F> String method1() {
			        return "a";
			    }
			    <F> String method1(E6<T> e) {
			        return "b";
			    }
			    Supplier<String> v1 = () -> new E6<Integer>()./*[1]*/method1();
			    Supplier<String> v2 = this::/*[2]*/<Float>method1;
			    Function<E6<Integer>, String> v3 = new E6<Integer>()::/*[3]*/<Float>method1;
			    T1[] ts = new T1[5];
			    BiFunction<Integer, Integer, Integer> m6 = ts[1]::/*[4]*/bar;
			    int[] is = new int[5];
			    Supplier<int[]> m10 = is::/*[5]*/clone;
			}
			class T1 {
			    int bar(int i, int j) { return i + j; }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [2]
		offset= str.indexOf("::/*[2]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test1;
			import java.util.function.*;
			public class E6<T> {
			    <F> String method1() {
			        return "a";
			    }
			    <F> String method1(E6<T> e) {
			        return "b";
			    }
			    Supplier<String> v1 = new E6<Integer>()::/*[1]*/method1;
			    Supplier<String> v2 = () -> this.<Float>method1();
			    Function<E6<Integer>, String> v3 = new E6<Integer>()::/*[3]*/<Float>method1;
			    T1[] ts = new T1[5];
			    BiFunction<Integer, Integer, Integer> m6 = ts[1]::/*[4]*/bar;
			    int[] is = new int[5];
			    Supplier<int[]> m10 = is::/*[5]*/clone;
			}
			class T1 {
			    int bar(int i, int j) { return i + j; }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [3]
		offset= str.indexOf("::/*[3]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test1;
			import java.util.function.*;
			public class E6<T> {
			    <F> String method1() {
			        return "a";
			    }
			    <F> String method1(E6<T> e) {
			        return "b";
			    }
			    Supplier<String> v1 = new E6<Integer>()::/*[1]*/method1;
			    Supplier<String> v2 = this::/*[2]*/<Float>method1;
			    Function<E6<Integer>, String> v3 = arg0 -> new E6<Integer>().<Float>method1(arg0);
			    T1[] ts = new T1[5];
			    BiFunction<Integer, Integer, Integer> m6 = ts[1]::/*[4]*/bar;
			    int[] is = new int[5];
			    Supplier<int[]> m10 = is::/*[5]*/clone;
			}
			class T1 {
			    int bar(int i, int j) { return i + j; }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [4]
		offset= str.indexOf("::/*[4]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test1;
			import java.util.function.*;
			public class E6<T> {
			    <F> String method1() {
			        return "a";
			    }
			    <F> String method1(E6<T> e) {
			        return "b";
			    }
			    Supplier<String> v1 = new E6<Integer>()::/*[1]*/method1;
			    Supplier<String> v2 = this::/*[2]*/<Float>method1;
			    Function<E6<Integer>, String> v3 = new E6<Integer>()::/*[3]*/<Float>method1;
			    T1[] ts = new T1[5];
			    BiFunction<Integer, Integer, Integer> m6 = (arg0, arg1) -> ts[1]./*[4]*/bar(arg0, arg1);
			    int[] is = new int[5];
			    Supplier<int[]> m10 = is::/*[5]*/clone;
			}
			class T1 {
			    int bar(int i, int j) { return i + j; }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [5]
		offset= str.indexOf("::/*[5]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test1;
			import java.util.function.*;
			public class E6<T> {
			    <F> String method1() {
			        return "a";
			    }
			    <F> String method1(E6<T> e) {
			        return "b";
			    }
			    Supplier<String> v1 = new E6<Integer>()::/*[1]*/method1;
			    Supplier<String> v2 = this::/*[2]*/<Float>method1;
			    Function<E6<Integer>, String> v3 = new E6<Integer>()::/*[3]*/<Float>method1;
			    T1[] ts = new T1[5];
			    BiFunction<Integer, Integer, Integer> m6 = ts[1]::/*[4]*/bar;
			    int[] is = new int[5];
			    Supplier<int[]> m10 = () -> is./*[5]*/clone();
			}
			class T1 {
			    int bar(int i, int j) { return i + j; }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertMethodReferenceToLambda7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.*;
			public class E7<T> {
			    <F> String method1() {
			        return "a";
			    }
			    Function<E7<Integer>, String> v1 = E7<Integer>::/*[1]*/<Float>method1;
			    Function<int[], int[]> v2 = int[]::/*[2]*/clone;
			    BiFunction<int[], int[], Boolean> v3 = int[]::/*[3]*/equals;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E7.java", str, false, null);

		// [1]
		int offset= str.indexOf("::/*[1]*/") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			import java.util.function.*;
			public class E7<T> {
			    <F> String method1() {
			        return "a";
			    }
			    Function<E7<Integer>, String> v1 = arg0 -> arg0.<Float>method1();
			    Function<int[], int[]> v2 = int[]::/*[2]*/clone;
			    BiFunction<int[], int[], Boolean> v3 = int[]::/*[3]*/equals;
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [2]
		offset= str.indexOf("::/*[2]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		expected= """
			package test1;
			import java.util.function.*;
			public class E7<T> {
			    <F> String method1() {
			        return "a";
			    }
			    Function<E7<Integer>, String> v1 = E7<Integer>::/*[1]*/<Float>method1;
			    Function<int[], int[]> v2 = arg0 -> arg0./*[2]*/clone();
			    BiFunction<int[], int[], Boolean> v3 = int[]::/*[3]*/equals;
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [3]
		offset= str.indexOf("::/*[3]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		expected= """
			package test1;
			import java.util.function.*;
			public class E7<T> {
			    <F> String method1() {
			        return "a";
			    }
			    Function<E7<Integer>, String> v1 = E7<Integer>::/*[1]*/<Float>method1;
			    Function<int[], int[]> v2 = int[]::/*[2]*/clone;
			    BiFunction<int[], int[], Boolean> v3 = (arg0, arg1) -> arg0./*[3]*/equals(arg1);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertMethodReferenceToLambda8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.*;
			import java.util.function.Supplier;
			public class E8 {
			    List<String> list = new ArrayList<>();
			    Supplier<Iterator<String>> mr = (list.size() == 5 ? list.subList(0, 3) : list)::iterator;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E8.java", str, false, null);

		int offset= str.indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.*;
			import java.util.function.Supplier;
			public class E8 {
			    List<String> list = new ArrayList<>();
			    Supplier<Iterator<String>> mr = () -> (list.size() == 5 ? list.subList(0, 3) : list).iterator();
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E9 {
			    private void test(int t) {
			        FI t1= E9::bar;
			    }
			    private static void bar(int x, int y, int z) {}
			}
			interface FI {
			    void foo(int t, int t1, int t2);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E9.java", str, false, null);

		int offset= str.indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E9 {
			    private void test(int t) {
			        FI t1= (t3, t11, t2) -> bar(t3, t11, t2);
			    }
			    private static void bar(int x, int y, int z) {}
			}
			interface FI {
			    void foo(int t, int t1, int t2);
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.lang.annotation.*;
			import java.util.function.*;
			
			@Target(ElementType.TYPE_USE)
			@interface Great {}
			
			public class E10 {
			    LongSupplier foo() {
			        return @Great System::currentTimeMillis;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E10.java", str, false, null);

		int offset= str.indexOf("::") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.lang.annotation.*;
			import java.util.function.*;
			
			@Target(ElementType.TYPE_USE)
			@interface Great {}
			
			public class E10 {
			    LongSupplier foo() {
			        return () -> System.currentTimeMillis();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertMethodReferenceToLambda11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test01", false, null);
		String str= """
			package test01;
			import java.util.function.Supplier;
			public class E10 extends Sup {
			    {
			        Supplier<String> v1 = this::/*[1]*/method1;
			        Supplier<String> v2 = this::/*[2]*/<Number>method1;
			
			        Supplier<String> n1 = E10::/*[3]*/method2;
			        Supplier<String> n2 = E10::/*[4]*/method2a;
			        Supplier<String> n3 = E10::/*[5]*/method3;
			        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;
			
			        Supplier<String> a1 = E10a::/*[7]*/method4;
			    }
			
			    <T> String method1() {
			        return "1";
			    }
			    static String method2() {
			        return "2";
			    }
			    static <T> String method2a() {
			        return "2a";
			    }
			    static String method4() {
			        return "4";
			    }
			}
			
			class Sup {
			    static String method3() {
			        return "3";
			    }
			}
			
			class E10a {
			    static String method4() {
			        return "4";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E10.java", str, false, null);

		// [1]
		int offset= str.indexOf("::/*[1]*/") + 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected= """
			package test01;
			import java.util.function.Supplier;
			public class E10 extends Sup {
			    {
			        Supplier<String> v1 = () -> /*[1]*/method1();
			        Supplier<String> v2 = this::/*[2]*/<Number>method1;
			
			        Supplier<String> n1 = E10::/*[3]*/method2;
			        Supplier<String> n2 = E10::/*[4]*/method2a;
			        Supplier<String> n3 = E10::/*[5]*/method3;
			        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;
			
			        Supplier<String> a1 = E10a::/*[7]*/method4;
			    }
			
			    <T> String method1() {
			        return "1";
			    }
			    static String method2() {
			        return "2";
			    }
			    static <T> String method2a() {
			        return "2a";
			    }
			    static String method4() {
			        return "4";
			    }
			}
			
			class Sup {
			    static String method3() {
			        return "3";
			    }
			}
			
			class E10a {
			    static String method4() {
			        return "4";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [2]
		offset= str.indexOf("::/*[2]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		expected= """
			package test01;
			import java.util.function.Supplier;
			public class E10 extends Sup {
			    {
			        Supplier<String> v1 = this::/*[1]*/method1;
			        Supplier<String> v2 = () -> this.<Number>method1();
			
			        Supplier<String> n1 = E10::/*[3]*/method2;
			        Supplier<String> n2 = E10::/*[4]*/method2a;
			        Supplier<String> n3 = E10::/*[5]*/method3;
			        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;
			
			        Supplier<String> a1 = E10a::/*[7]*/method4;
			    }
			
			    <T> String method1() {
			        return "1";
			    }
			    static String method2() {
			        return "2";
			    }
			    static <T> String method2a() {
			        return "2a";
			    }
			    static String method4() {
			        return "4";
			    }
			}
			
			class Sup {
			    static String method3() {
			        return "3";
			    }
			}
			
			class E10a {
			    static String method4() {
			        return "4";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [3]
		offset= str.indexOf("::/*[3]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		expected= """
			package test01;
			import java.util.function.Supplier;
			public class E10 extends Sup {
			    {
			        Supplier<String> v1 = this::/*[1]*/method1;
			        Supplier<String> v2 = this::/*[2]*/<Number>method1;
			
			        Supplier<String> n1 = () -> /*[3]*/method2();
			        Supplier<String> n2 = E10::/*[4]*/method2a;
			        Supplier<String> n3 = E10::/*[5]*/method3;
			        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;
			
			        Supplier<String> a1 = E10a::/*[7]*/method4;
			    }
			
			    <T> String method1() {
			        return "1";
			    }
			    static String method2() {
			        return "2";
			    }
			    static <T> String method2a() {
			        return "2a";
			    }
			    static String method4() {
			        return "4";
			    }
			}
			
			class Sup {
			    static String method3() {
			        return "3";
			    }
			}
			
			class E10a {
			    static String method4() {
			        return "4";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [4]
		offset= str.indexOf("::/*[4]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		expected= """
			package test01;
			import java.util.function.Supplier;
			public class E10 extends Sup {
			    {
			        Supplier<String> v1 = this::/*[1]*/method1;
			        Supplier<String> v2 = this::/*[2]*/<Number>method1;
			
			        Supplier<String> n1 = E10::/*[3]*/method2;
			        Supplier<String> n2 = () -> /*[4]*/method2a();
			        Supplier<String> n3 = E10::/*[5]*/method3;
			        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;
			
			        Supplier<String> a1 = E10a::/*[7]*/method4;
			    }
			
			    <T> String method1() {
			        return "1";
			    }
			    static String method2() {
			        return "2";
			    }
			    static <T> String method2a() {
			        return "2a";
			    }
			    static String method4() {
			        return "4";
			    }
			}
			
			class Sup {
			    static String method3() {
			        return "3";
			    }
			}
			
			class E10a {
			    static String method4() {
			        return "4";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [5]
		offset= str.indexOf("::/*[5]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		expected= """
			package test01;
			import java.util.function.Supplier;
			public class E10 extends Sup {
			    {
			        Supplier<String> v1 = this::/*[1]*/method1;
			        Supplier<String> v2 = this::/*[2]*/<Number>method1;
			
			        Supplier<String> n1 = E10::/*[3]*/method2;
			        Supplier<String> n2 = E10::/*[4]*/method2a;
			        Supplier<String> n3 = () -> /*[5]*/method3();
			        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;
			
			        Supplier<String> a1 = E10a::/*[7]*/method4;
			    }
			
			    <T> String method1() {
			        return "1";
			    }
			    static String method2() {
			        return "2";
			    }
			    static <T> String method2a() {
			        return "2a";
			    }
			    static String method4() {
			        return "4";
			    }
			}
			
			class Sup {
			    static String method3() {
			        return "3";
			    }
			}
			
			class E10a {
			    static String method4() {
			        return "4";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [6]
		offset= str.indexOf("::/*[6]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		expected= """
			package test01;
			import java.util.function.Supplier;
			public class E10 extends Sup {
			    {
			        Supplier<String> v1 = this::/*[1]*/method1;
			        Supplier<String> v2 = this::/*[2]*/<Number>method1;
			
			        Supplier<String> n1 = E10::/*[3]*/method2;
			        Supplier<String> n2 = E10::/*[4]*/method2a;
			        Supplier<String> n3 = E10::/*[5]*/method3;
			        Supplier<String> n4 = () -> E10.<Number>method2a();
			
			        Supplier<String> a1 = E10a::/*[7]*/method4;
			    }
			
			    <T> String method1() {
			        return "1";
			    }
			    static String method2() {
			        return "2";
			    }
			    static <T> String method2a() {
			        return "2a";
			    }
			    static String method4() {
			        return "4";
			    }
			}
			
			class Sup {
			    static String method3() {
			        return "3";
			    }
			}
			
			class E10a {
			    static String method4() {
			        return "4";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

		// [7]
		offset= str.indexOf("::/*[7]*/") + 1;
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		expected= """
			package test01;
			import java.util.function.Supplier;
			public class E10 extends Sup {
			    {
			        Supplier<String> v1 = this::/*[1]*/method1;
			        Supplier<String> v2 = this::/*[2]*/<Number>method1;
			
			        Supplier<String> n1 = E10::/*[3]*/method2;
			        Supplier<String> n2 = E10::/*[4]*/method2a;
			        Supplier<String> n3 = E10::/*[5]*/method3;
			        Supplier<String> n4 = E10::/*[6]*/<Number>method2a;
			
			        Supplier<String> a1 = () -> E10a./*[7]*/method4();
			    }
			
			    <T> String method1() {
			        return "1";
			    }
			    static String method2() {
			        return "2";
			    }
			    static <T> String method2a() {
			        return "2a";
			    }
			    static String method4() {
			        return "4";
			    }
			}
			
			class Sup {
			    static String method3() {
			        return "3";
			    }
			}
			
			class E10a {
			    static String method4() {
			        return "4";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertLambdaToMethodReference1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.BiFunction;
			import java.util.function.Consumer;
			import java.util.function.Function;
			import java.util.function.IntFunction;
			import java.util.function.Supplier;
			public class E1 extends E {
			    Supplier<String> a1= () ->/*[1]*/ {
			        String s = "";
			        return s;
			    };
			    Consumer<String> a2= s ->/*[2]*/ {
			        return;
			    };
			
			    Supplier<E1.In> a3= () ->/*[3]*/ (new E1()).new In();
			    Supplier<E1> a4= () ->/*[4]*/ new E1() {
			        void test() {
			            System.out.println("hey");
			        }
			    };
			    Function<String, Integer> a5= s ->/*[5]*/ Integer.valueOf(s+1);
			
			    BiFunction<Integer, Integer, int[][][]> a6 = (a, b) ->/*[6]*/ new int[a][b][];
			    IntFunction<Integer[][][]> a61 = value ->/*[61]*/ new Integer[][][] {{{7, 8}}};
			    Function<Integer, int[]> a7 = t ->/*[7]*/ new int[100];
			
			    BiFunction<Character, Integer, String> a8 = (c, i) ->/*[8]*/ super.method1();
			    BiFunction<Character, Integer, String> a9 = (c, i) ->/*[9]*/ method1();
			   \s
			    class In {}
			}
			class E {
			    String method1() {
			        return "a";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		int offset= str.indexOf("->/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[3]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[4]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[5]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[6]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[61]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[7]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[8]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[9]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);
	}

	@Test
	public void testConvertLambdaToMethodReference2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Function;
			public class E2<T> {
			    public <A> E2(A a) {}
			    public E2(String s) {}
			   \s
			    Function<T, E2<Integer>> a1 = t ->/*[1]*/ (new <T>E2<Integer>(t));
			    Function<String, E2<Integer>> a2 = t ->/*[2]*/ new E2<>(t);
			   \s
			    Function<Integer, Float[]> a3 = t ->/*[3]*/ new Float[t];
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str, false, null);

		int offset= str.indexOf("->/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			import java.util.function.Function;
			public class E2<T> {
			    public <A> E2(A a) {}
			    public E2(String s) {}
			   \s
			    Function<T, E2<Integer>> a1 = E2<Integer>::<T>new;
			    Function<String, E2<Integer>> a2 = t ->/*[2]*/ new E2<>(t);
			   \s
			    Function<Integer, Float[]> a3 = t ->/*[3]*/ new Float[t];
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("->/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.Function;
			public class E2<T> {
			    public <A> E2(A a) {}
			    public E2(String s) {}
			   \s
			    Function<T, E2<Integer>> a1 = t ->/*[1]*/ (new <T>E2<Integer>(t));
			    Function<String, E2<Integer>> a2 = E2::new;
			   \s
			    Function<Integer, Float[]> a3 = t ->/*[3]*/ new Float[t];
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("->/*[3]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.Function;
			public class E2<T> {
			    public <A> E2(A a) {}
			    public E2(String s) {}
			   \s
			    Function<T, E2<Integer>> a1 = t ->/*[1]*/ (new <T>E2<Integer>(t));
			    Function<String, E2<Integer>> a2 = t ->/*[2]*/ new E2<>(t);
			   \s
			    Function<Integer, Float[]> a3 = Float[]::new;
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertLambdaToMethodReference3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Function;
			public class E3<T> extends SuperE3<Number> {
			    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);
			    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);
			    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);
			
			    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);
			    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);
			
			    Function<Integer, String> b1 = t -> /*[6]*/method1(t);
			    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);
			    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);
			
			    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();
			    Function<E3, String> p2 = t -> /*[10]*/t.method2();
			
			    <V> String method2() { return "m2";    }
			}
			class SuperE3<S> {
			    String method1(int i) { return "m1"; }
			    static <V> String staticMethod1(int i) { return "s"; }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E3.java", str, false, null);

		int offset= str.indexOf("-> /*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			import java.util.function.Function;
			public class E3<T> extends SuperE3<Number> {
			    Function<Integer, String> a1 = super::method1;
			    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);
			    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);
			
			    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);
			    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);
			
			    Function<Integer, String> b1 = t -> /*[6]*/method1(t);
			    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);
			    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);
			
			    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();
			    Function<E3, String> p2 = t -> /*[10]*/t.method2();
			
			    <V> String method2() { return "m2";    }
			}
			class SuperE3<S> {
			    String method1(int i) { return "m1"; }
			    static <V> String staticMethod1(int i) { return "s"; }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("-> /*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.Function;
			public class E3<T> extends SuperE3<Number> {
			    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);
			    Function<Integer, String> a2 = E3.super::method1;
			    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);
			
			    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);
			    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);
			
			    Function<Integer, String> b1 = t -> /*[6]*/method1(t);
			    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);
			    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);
			
			    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();
			    Function<E3, String> p2 = t -> /*[10]*/t.method2();
			
			    <V> String method2() { return "m2";    }
			}
			class SuperE3<S> {
			    String method1(int i) { return "m1"; }
			    static <V> String staticMethod1(int i) { return "s"; }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("-> /*[3]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.Function;
			public class E3<T> extends SuperE3<Number> {
			    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);
			    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);
			    Function<Integer, String> a3 = SuperE3::<Float>staticMethod1;
			
			    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);
			    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);
			
			    Function<Integer, String> b1 = t -> /*[6]*/method1(t);
			    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);
			    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);
			
			    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();
			    Function<E3, String> p2 = t -> /*[10]*/t.method2();
			
			    <V> String method2() { return "m2";    }
			}
			class SuperE3<S> {
			    String method1(int i) { return "m1"; }
			    static <V> String staticMethod1(int i) { return "s"; }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("-> /*[4]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.Function;
			public class E3<T> extends SuperE3<Number> {
			    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);
			    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);
			    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);
			
			    Function<Integer, String> s1 = E3::staticMethod1;
			    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);
			
			    Function<Integer, String> b1 = t -> /*[6]*/method1(t);
			    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);
			    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);
			
			    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();
			    Function<E3, String> p2 = t -> /*[10]*/t.method2();
			
			    <V> String method2() { return "m2";    }
			}
			class SuperE3<S> {
			    String method1(int i) { return "m1"; }
			    static <V> String staticMethod1(int i) { return "s"; }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("-> /*[5]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.Function;
			public class E3<T> extends SuperE3<Number> {
			    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);
			    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);
			    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);
			
			    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);
			    Function<Integer, String> s2 = E3::staticMethod1;
			
			    Function<Integer, String> b1 = t -> /*[6]*/method1(t);
			    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);
			    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);
			
			    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();
			    Function<E3, String> p2 = t -> /*[10]*/t.method2();
			
			    <V> String method2() { return "m2";    }
			}
			class SuperE3<S> {
			    String method1(int i) { return "m1"; }
			    static <V> String staticMethod1(int i) { return "s"; }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("-> /*[6]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.Function;
			public class E3<T> extends SuperE3<Number> {
			    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);
			    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);
			    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);
			
			    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);
			    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);
			
			    Function<Integer, String> b1 = this::method1;
			    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);
			    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);
			
			    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();
			    Function<E3, String> p2 = t -> /*[10]*/t.method2();
			
			    <V> String method2() { return "m2";    }
			}
			class SuperE3<S> {
			    String method1(int i) { return "m1"; }
			    static <V> String staticMethod1(int i) { return "s"; }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("-> /*[7]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.Function;
			public class E3<T> extends SuperE3<Number> {
			    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);
			    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);
			    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);
			
			    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);
			    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);
			
			    Function<Integer, String> b1 = t -> /*[6]*/method1(t);
			    Function<Integer, String> b2 = this::method1;
			    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);
			
			    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();
			    Function<E3, String> p2 = t -> /*[10]*/t.method2();
			
			    <V> String method2() { return "m2";    }
			}
			class SuperE3<S> {
			    String method1(int i) { return "m1"; }
			    static <V> String staticMethod1(int i) { return "s"; }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("-> /*[8]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.Function;
			public class E3<T> extends SuperE3<Number> {
			    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);
			    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);
			    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);
			
			    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);
			    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);
			
			    Function<Integer, String> b1 = t -> /*[6]*/method1(t);
			    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);
			    Function<Integer, String> b3 = (new SuperE3<String>())::method1;
			
			    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();
			    Function<E3, String> p2 = t -> /*[10]*/t.method2();
			
			    <V> String method2() { return "m2";    }
			}
			class SuperE3<S> {
			    String method1(int i) { return "m1"; }
			    static <V> String staticMethod1(int i) { return "s"; }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("-> /*[9]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.Function;
			public class E3<T> extends SuperE3<Number> {
			    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);
			    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);
			    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);
			
			    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);
			    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);
			
			    Function<Integer, String> b1 = t -> /*[6]*/method1(t);
			    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);
			    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);
			
			    Function<E3<Integer>, String> p1 = E3<Integer>::<Float>method2;
			    Function<E3, String> p2 = t -> /*[10]*/t.method2();
			
			    <V> String method2() { return "m2";    }
			}
			class SuperE3<S> {
			    String method1(int i) { return "m1"; }
			    static <V> String staticMethod1(int i) { return "s"; }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		String match10= "-> /*[10]*/t.method2";
		offset= str.indexOf(match10) + match10.length();
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.Function;
			public class E3<T> extends SuperE3<Number> {
			    Function<Integer, String> a1 = t -> /*[1]*/super.method1(t);
			    Function<Integer, String> a2 = t -> /*[2]*/E3.super.method1(t);
			    Function<Integer, String> a3 = t -> /*[3]*/super.<Float>staticMethod1(t);
			
			    Function<Integer, String> s1 = t -> /*[4]*/(new E3()).staticMethod1(t);
			    Function<Integer, String> s2 = t -> /*[5]*/staticMethod1(t);
			
			    Function<Integer, String> b1 = t -> /*[6]*/method1(t);
			    Function<Integer, String> b2 = t -> /*[7]*/this.method1(t);
			    Function<Integer, String> b3 = t -> /*[8]*/(new SuperE3<String>()).method1(t);
			
			    Function<E3<Integer>, String> p1 = t -> /*[9]*/t.<Float>method2();
			    Function<E3, String> p2 = E3::method2;
			
			    <V> String method2() { return "m2";    }
			}
			class SuperE3<S> {
			    String method1(int i) { return "m1"; }
			    static <V> String staticMethod1(int i) { return "s"; }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertLambdaToMethodReference4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.BiFunction;
			import java.util.function.Function;
			public class E4 {
			    static String staticMethod1() {    return "s"; }
			    Function<E4, String> s1 = t ->/*[1]*/ staticMethod1();
			   \s
			    int myVal= 0;
			    String method1(int i) { return "m1"; }
			    BiFunction<Float, Integer, String> p1 = (t, u) ->/*[2]*/ method1(u);
			    BiFunction<SubE4, Integer, String> p2 = (t, u) ->/*[3]*/ method1(u);
			   \s
			    BiFunction<SubE4, Integer, String> p3 = (t, u) ->/*[4]*/ t.method1(u);
			    BiFunction<E4, Integer, String> p4 = (t, u) ->/*[5]*/ t.method1(u);
			   \s
			    Function<int[], int[]> a1 = t ->/*[6]*/ t.clone();
			}
			class SubE4 extends E4 {}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E4.java", str, false, null);

		int offset= str.indexOf("->/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[3]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_method_reference);

		offset= str.indexOf("->/*[4]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			import java.util.function.BiFunction;
			import java.util.function.Function;
			public class E4 {
			    static String staticMethod1() {    return "s"; }
			    Function<E4, String> s1 = t ->/*[1]*/ staticMethod1();
			   \s
			    int myVal= 0;
			    String method1(int i) { return "m1"; }
			    BiFunction<Float, Integer, String> p1 = (t, u) ->/*[2]*/ method1(u);
			    BiFunction<SubE4, Integer, String> p2 = (t, u) ->/*[3]*/ method1(u);
			   \s
			    BiFunction<SubE4, Integer, String> p3 = SubE4::method1;
			    BiFunction<E4, Integer, String> p4 = (t, u) ->/*[5]*/ t.method1(u);
			   \s
			    Function<int[], int[]> a1 = t ->/*[6]*/ t.clone();
			}
			class SubE4 extends E4 {}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("->/*[5]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.BiFunction;
			import java.util.function.Function;
			public class E4 {
			    static String staticMethod1() {    return "s"; }
			    Function<E4, String> s1 = t ->/*[1]*/ staticMethod1();
			   \s
			    int myVal= 0;
			    String method1(int i) { return "m1"; }
			    BiFunction<Float, Integer, String> p1 = (t, u) ->/*[2]*/ method1(u);
			    BiFunction<SubE4, Integer, String> p2 = (t, u) ->/*[3]*/ method1(u);
			   \s
			    BiFunction<SubE4, Integer, String> p3 = (t, u) ->/*[4]*/ t.method1(u);
			    BiFunction<E4, Integer, String> p4 = E4::method1;
			   \s
			    Function<int[], int[]> a1 = t ->/*[6]*/ t.clone();
			}
			class SubE4 extends E4 {}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("->/*[6]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			import java.util.function.BiFunction;
			import java.util.function.Function;
			public class E4 {
			    static String staticMethod1() {    return "s"; }
			    Function<E4, String> s1 = t ->/*[1]*/ staticMethod1();
			   \s
			    int myVal= 0;
			    String method1(int i) { return "m1"; }
			    BiFunction<Float, Integer, String> p1 = (t, u) ->/*[2]*/ method1(u);
			    BiFunction<SubE4, Integer, String> p2 = (t, u) ->/*[3]*/ method1(u);
			   \s
			    BiFunction<SubE4, Integer, String> p3 = (t, u) ->/*[4]*/ t.method1(u);
			    BiFunction<E4, Integer, String> p4 = (t, u) ->/*[5]*/ t.method1(u);
			   \s
			    Function<int[], int[]> a1 = int[]::clone;
			}
			class SubE4 extends E4 {}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertLambdaToMethodReference5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface I {
			    public default void i1() {
			    }
			    void i2();
			   \s
			    class E5 implements I {
			        Thread o1 = new Thread(() ->/*[1]*/ i1());
			        Thread o2 = new Thread(() ->/*[2]*/ i2());
			        public void i2() {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", str, false, null);


		int offset= str.indexOf("->/*[1]*/");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			public interface I {
			    public default void i1() {
			    }
			    void i2();
			   \s
			    class E5 implements I {
			        Thread o1 = new Thread(this::i1);
			        Thread o2 = new Thread(() ->/*[2]*/ i2());
			        public void i2() {
			        }
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		offset= str.indexOf("->/*[2]*/");
		context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			public interface I {
			    public default void i1() {
			    }
			    void i2();
			   \s
			    class E5 implements I {
			        Thread o1 = new Thread(() ->/*[1]*/ i1());
			        Thread o2 = new Thread(this::i2);
			        public void i2() {
			        }
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertLambdaToMethodReference6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E6 {
			
			    private interface I6 {
			        public boolean isCorrect(Object z);
			    }
			
			    public boolean foo() {
			        I6 x = z -> z instanceof String;
			        return x.isCorrect(this);
			    }
			
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E6.java", str, false, null);

		int offset= str.indexOf("z ->");
		AssistContext context= getCorrectionContext(cu, offset, 4);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			public class E6 {
			
			    private interface I6 {
			        public boolean isCorrect(Object z);
			    }
			
			    public boolean foo() {
			        I6 x = String.class::isInstance;
			        return x.isCorrect(this);
			    }
			
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testIssue1047_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Supplier;
			
			public class E {
			    void func( String ... args) {
			    }
			
			    private void called( Supplier<Object> r ) {
			    }
			
			    void called( Runnable r ) {
			    }
			
			    void test() {
			        called(() -> func());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);


		int offset= str.indexOf("func()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			import java.util.function.Supplier;
			
			public class E {
			    void func( String ... args) {
			    }
			
			    private void called( Supplier<Object> r ) {
			    }
			
			    void called( Runnable r ) {
			    }
			
			    void test() {
			        called((Runnable) this::func);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testIssue1047_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Supplier;
			
			public class E1 {
			    private void called( Supplier<Object> r ) {
			    }
			
			}
			""";
		pack1.createCompilationUnit("E1.java", str, false, null);

		String str1= """
			package test1;
			
			public class E extends E1 {
			    void func( String ... args) {
			    }
			
			    void called( Runnable r ) {
			    }
			
			    void test() {
			        called(() -> func());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		int offset= str1.indexOf("func()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			
			public class E extends E1 {
			    void func( String ... args) {
			    }
			
			    void called( Runnable r ) {
			    }
			
			    void test() {
			        called((Runnable) this::func);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testIssue1047_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Supplier;
			
			public class E1 {
			    void func( String ... args) {
			    }
			    private void called( Supplier<Object> r ) {
			    }
			
			}
			""";
		pack1.createCompilationUnit("E1.java", str, false, null);

		String str1= """
			package test1;
			
			public class E extends E1 {
			
			    void called( Runnable r ) {
			    }
			
			    void test() {
			        called(() -> super.func());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		int offset= str1.indexOf("func()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			
			public class E extends E1 {
			
			    void called( Runnable r ) {
			    }
			
			    void test() {
			        called((Runnable) super::func);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testIssue1047_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Supplier;
			
			public class E1 {
			    public static void func( String ... args) {
			    }
			    private void called( Supplier<Object> r ) {
			    }
			
			}
			""";
		pack1.createCompilationUnit("E1.java", str, false, null);

		String str1= """
			package test1;
			
			public class E extends E1 {
			
			    void called( Runnable r ) {
			    }
			
			    void test() {
			        called(() -> E1.func());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		int offset= str1.indexOf("func()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			
			public class E extends E1 {
			
			    void called( Runnable r ) {
			    }
			
			    void test() {
			        called((Runnable) E1::func);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testFixParenthesesInLambdaExpressionAdd() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Consumer;
			public class MyClass {
			    public void foo() {
			        Consumer<Integer> c = id -> {System.out.println(id);};
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("MyClass.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 7);
		assertCorrectLabels(proposals);
		String expected= """
			package test1;
			import java.util.function.Consumer;
			public class MyClass {
			    public void foo() {
			        Consumer<Integer> c = (id) -> {System.out.println(id);};
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testFixParenthesesInLambdaExpressionRemove() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Consumer;
			public class MyClass {
			    public void foo() {
			        Consumer<Integer> c = (id) -> {System.out.println(id);};
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("MyClass.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 7);
		assertCorrectLabels(proposals);
		String expected= """
			package test1;
			import java.util.function.Consumer;
			public class MyClass {
			    public void foo() {
			        Consumer<Integer> c = id -> {System.out.println(id);};
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testFixParenthesesInLambdaExpressionCannotRemove1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class MyClass {
			    public void foo() {
			        Runnable r = () -> System.out.println("Hello world!");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("MyClass.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_removeParenthesesInLambda);
	}

	@Test
	public void testFixParenthesesInLambdaExpressionCannotRemove2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Comparator;
			public class MyClass {
			    public void foo() {
			        Comparator<String> c = (String s1, String s2) -> {return s1.length() - s2.length();};
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("MyClass.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_removeParenthesesInLambda);
	}

	@Test
	public void testFixParenthesesInLambdaExpressionCannotRemove3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.function.Consumer;
			public class MyClass {
			    public void foo() {
			        Consumer<Integer> c = (Integer id) -> {System.out.println(id);};
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("MyClass.java", str, false, null);

		int offset= str.indexOf("->");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_removeParenthesesInLambda);
	}

	@Test
	public void testBug514203_wildCard() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			import java.util.Comparator;
			import java.util.List;
			
			public class Lambda1 {
				Comparator<List<?>> c = (l1, l2) -> Integer.compare(l1.size(), l2.size());
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Lambda1.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("->"), 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);


		String str1= """
			package test1;
			
			import java.util.Comparator;
			import java.util.List;
			
			public class Lambda1 {
				Comparator<List<?>> c = (List<?> l1, List<?> l2) -> Integer.compare(l1.size(), l2.size());
			}
			""";
		assertProposalPreviewEquals(str1, "Add inferred lambda parameter types", proposals);

		String str2= """
			package test1;
			
			import java.util.Comparator;
			import java.util.List;
			
			public class Lambda1 {
				Comparator<List<?>> c = new Comparator<List<?>>() {
			        @Override
			        public int compare(List<?> l1, List<?> l2) {
			            return Integer.compare(l1.size(), l2.size());
			        }
			    };
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to anonymous class creation", proposals);

	}

	@Test
	public void testBug514203_capture1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			@org.eclipse.jdt.annotation.NonNullByDefault
			package test1;
			""";

		pack1.createCompilationUnit("package-info.java", str, false, null);

		String str1= """
			package test1;
			
			public class Lambda2 {
				interface Sink<T> {
					void receive(T t);
				}
			
				interface Source<U> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<? extends Number> source) {
					source.sendTo(a -> a.doubleValue());
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Lambda2.java", str1, false, null);

		AssistContext context= getCorrectionContext(cu, str1.indexOf("->"), 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		String str2= """
			package test1;
			
			public class Lambda2 {
				interface Sink<T> {
					void receive(T t);
				}
			
				interface Source<U> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<? extends Number> source) {
					source.sendTo(Number::doubleValue);
				}
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to method reference", proposals);


		String str3= """
			package test1;
			
			public class Lambda2 {
				interface Sink<T> {
					void receive(T t);
				}
			
				interface Source<U> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<? extends Number> source) {
					source.sendTo((Number a) -> a.doubleValue());
				}
			}
			""";
		assertProposalPreviewEquals(str3, "Add inferred lambda parameter types", proposals);

		String str4= """
			package test1;
			
			public class Lambda2 {
				interface Sink<T> {
					void receive(T t);
				}
			
				interface Source<U> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<? extends Number> source) {
					source.sendTo(new Sink<Number>() {
			            @Override
			            public void receive(Number a) {
			                a.doubleValue();
			            }
			        });
				}
			}
			""";
		assertProposalPreviewEquals(str4, "Convert to anonymous class creation", proposals);

	}

	@Test
	public void testBug514203_capture2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			@org.eclipse.jdt.annotation.NonNullByDefault
			package test1;
			""";

		pack1.createCompilationUnit("package-info.java", str, false, null);

		String str1= """
			package test1;
			
			import java.math.BigDecimal;
			
			public class Lambda3 {
				interface Sink<T extends Number> {
					void receive(T t);
				}
			
				interface Source<U extends BigDecimal> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<?> source) {
					source.sendTo(a -> a.scale());
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Lambda3.java", str1, false, null);

		AssistContext context= getCorrectionContext(cu, str1.indexOf("->"), 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		String str2= """
			package test1;
			
			import java.math.BigDecimal;
			
			public class Lambda3 {
				interface Sink<T extends Number> {
					void receive(T t);
				}
			
				interface Source<U extends BigDecimal> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<?> source) {
					source.sendTo(BigDecimal::scale);
				}
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to method reference", proposals);


		String str3= """
			package test1;
			
			import java.math.BigDecimal;
			
			public class Lambda3 {
				interface Sink<T extends Number> {
					void receive(T t);
				}
			
				interface Source<U extends BigDecimal> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<?> source) {
					source.sendTo((BigDecimal a) -> a.scale());
				}
			}
			""";
		assertProposalPreviewEquals(str3, "Add inferred lambda parameter types", proposals);

		String str4= """
			package test1;
			
			import java.math.BigDecimal;
			
			public class Lambda3 {
				interface Sink<T extends Number> {
					void receive(T t);
				}
			
				interface Source<U extends BigDecimal> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<?> source) {
					source.sendTo(new Sink<BigDecimal>() {
			            @Override
			            public void receive(BigDecimal a) {
			                a.scale();
			            }
			        });
				}
			}
			""";
		assertProposalPreviewEquals(str4, "Convert to anonymous class creation", proposals);

	}

	@Test
	public void testBug514203_capture3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			@org.eclipse.jdt.annotation.NonNullByDefault
			package test1;
			""";

		pack1.createCompilationUnit("package-info.java", str, false, null);

		String str1= """
			package test1;
			
			import java.util.ArrayList;
			import java.util.List;
			
			public class Lambda4 {
				interface Sink<T extends List<Number>> {
					void receive(T t);
				}
			
				interface Source<U extends ArrayList<Number>> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<?> source) {
					source.sendTo(a -> a.size());
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Lambda4.java", str1, false, null);

		AssistContext context= getCorrectionContext(cu, str1.indexOf("->"), 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		String str2= """
			package test1;
			
			import java.util.ArrayList;
			import java.util.List;
			
			public class Lambda4 {
				interface Sink<T extends List<Number>> {
					void receive(T t);
				}
			
				interface Source<U extends ArrayList<Number>> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<?> source) {
					source.sendTo(ArrayList<Number>::size);
				}
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to method reference", proposals);


		String str3= """
			package test1;
			
			import java.util.ArrayList;
			import java.util.List;
			
			public class Lambda4 {
				interface Sink<T extends List<Number>> {
					void receive(T t);
				}
			
				interface Source<U extends ArrayList<Number>> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<?> source) {
					source.sendTo((ArrayList<Number> a) -> a.size());
				}
			}
			""";
		assertProposalPreviewEquals(str3, "Add inferred lambda parameter types", proposals);

		String str4= """
			package test1;
			
			import java.util.ArrayList;
			import java.util.List;
			
			public class Lambda4 {
				interface Sink<T extends List<Number>> {
					void receive(T t);
				}
			
				interface Source<U extends ArrayList<Number>> {
					void sendTo(Sink<? super U> c);
				}
			
				void f(Source<?> source) {
					source.sendTo(new Sink<ArrayList<Number>>() {
			            @Override
			            public void receive(ArrayList<Number> a) {
			                a.size();
			            }
			        });
				}
			}
			""";
		assertProposalPreviewEquals(str4, "Convert to anonymous class creation", proposals);

	}

	@Test
	public void testBug514203_lambdaNN() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		JavaProjectHelper.addLibrary(fJProject1, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS })
			package test1;
			
			import static org.eclipse.jdt.annotation.DefaultLocation.*;
			import org.eclipse.jdt.annotation.NonNullByDefault;
			""";

		// --- Set up @NonNullByDefault for the package, including ARRAY_CONTENTS --

		pack1.createCompilationUnit("package-info.java", str, false, null);

		// --- Classes that are only referenced --

		String str1= """
			package test1;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			@Target(ElementType.TYPE_USE)
			@interface X {}
			""";
		pack1.createCompilationUnit("X.java", str1, false, null);

		String str2= """
			package test1;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			@Target(ElementType.TYPE_USE)
			@interface Y {}
			""";
		pack1.createCompilationUnit("Y.java", str2, false, null);

		String str3= """
			package test1;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			@Target(ElementType.TYPE_USE)
			@interface Z {}
			""";
		pack1.createCompilationUnit("Z.java", str3, false, null);

		String str4= """
			package test1;
			
			class Ref<A> {}
			""";
		pack1.createCompilationUnit("Ref.java", str4, false, null);

		String str5= """
			package test1;
			
			interface SAM<A> {
				void f(A[] a);
			}
			""";
		pack1.createCompilationUnit("SAM.java", str5, false, null);

		String str6= """
			package test1;
			
			public class Test {
				static int nn(Object o) {
					return 0;
				}
			}
			""";
		pack1.createCompilationUnit("Test.java", str6, false, null);

		// --- Classes in which the quick assists are checked (without and with NonNullByDefault in effect at the target location) ---

		String str7= """
			package test1;
			
			import org.eclipse.jdt.annotation.*;
			
			public class LambdaNN1 {
				void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {
					@NonNullByDefault({})
					SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = a0 -> Test.nn(a0);
					sam0.f(data);
				}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("LambdaNN1.java", str7, false, null);

		AssistContext context1= getCorrectionContext(cu1, str7.indexOf("->"), 0);
		assertNoErrors(context1);
		List<IJavaCompletionProposal> proposals1= collectAssists(context1, false);

		String str8= """
			package test1;
			
			import org.eclipse.jdt.annotation.*;
			
			public class LambdaNN2 {
				void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {
					SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = a0 -> Test.nn(a0);
					sam0.f(data);
				}
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("LambdaNN2.java", str8, false, null);

		AssistContext context2= getCorrectionContext(cu2, str8.indexOf("->"), 0);
		assertNoErrors(context2);

		List<IJavaCompletionProposal> proposals2= collectAssists(context2, false);

		// --- Convert to method reference without and with NNBD ---

		String str9= """
			package test1;
			
			import org.eclipse.jdt.annotation.*;
			
			public class LambdaNN1 {
				void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {
					@NonNullByDefault({})
					SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = Test::nn;
					sam0.f(data);
				}
			}
			""";
		assertProposalPreviewEquals(str9, "Convert to method reference", proposals1);


		String str10= """
			package test1;
			
			import org.eclipse.jdt.annotation.*;
			
			public class LambdaNN2 {
				void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {
					SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = Test::nn;
					sam0.f(data);
				}
			}
			""";
		assertProposalPreviewEquals(str10, "Convert to method reference", proposals2);

		// --- Add inferred lambda parameter types without and with NNBD ---

		String str11= """
			package test1;
			
			import org.eclipse.jdt.annotation.*;
			
			public class LambdaNN1 {
				void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {
					@NonNullByDefault({})
					SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = (@NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>> @NonNull [] a0) -> Test.nn(a0);
					sam0.f(data);
				}
			}
			""";
		assertProposalPreviewEquals(str11, "Add inferred lambda parameter types", proposals1);

		String str12= """
			package test1;
			
			import org.eclipse.jdt.annotation.*;
			
			public class LambdaNN2 {
				void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {
					SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = (Ref<? extends @Y Ref<@X @Nullable String @Y [] @Z []>>[] a0) -> Test.nn(a0);
					sam0.f(data);
				}
			}
			""";
		assertProposalPreviewEquals(str12, "Add inferred lambda parameter types", proposals2);

		// --- Convert to anonymous class creation without and with NNBD --
		String str13= """
			package test1;
			
			import org.eclipse.jdt.annotation.*;
			
			public class LambdaNN1 {
				void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {
					@NonNullByDefault({})
					SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = new SAM<@NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>>() {
			            @Override
			            public void f(
			                    @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>> @NonNull [] a0) {
			                Test.nn(a0);
			            }
			        };
					sam0.f(data);
				}
			}
			""";
		assertProposalPreviewEquals(str13, "Convert to anonymous class creation", proposals1);

		String str14= """
			package test1;
			
			import org.eclipse.jdt.annotation.*;
			
			public class LambdaNN2 {
				void g(Ref<? extends Ref<@X @Nullable String @Y [] @Z []>>[] data) {
					SAM<? super @NonNull Ref<? extends @NonNull @Y Ref<@X @Nullable String @Y @NonNull [] @Z @NonNull []>>> sam0 = new SAM<Ref<? extends @Y Ref<@X @Nullable String @Y [] @Z []>>>() {
			            @Override
			            public void f(
			                    Ref<? extends @Y Ref<@X @Nullable String @Y [] @Z []>>[] a0) {
			                Test.nn(a0);
			            }
			        };
					sam0.f(data);
				}
			}
			""";
		assertProposalPreviewEquals(str14, "Convert to anonymous class creation", proposals2);
	}
	@Test
	public void testBug514203_annotatedParametrizedType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public class Example {
				@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
				public @interface X {}
			
				interface SAM<T> {
					T f(T t);
				}
			
				@X
				SAM<String> c = a -> a;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Example.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("->"), 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		String str1= """
			package test1;
			
			public class Example {
				@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
				public @interface X {}
			
				interface SAM<T> {
					T f(T t);
				}
			
				@X
				SAM<String> c = new @X SAM<String>() {
			        @Override
			        public String f(String a) {
			            return a;
			        }
			    };
			}
			""";
		assertProposalPreviewEquals(str1, "Convert to anonymous class creation", proposals);
	}

	@Test
	public void testNoRedundantNonNullInConvertArrayForLoop() throws Exception {
		NullTestUtils.prepareNullTypeAnnotations(fSourceFolder);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				import annots.*;
				public class A {
				    public void foo(@NonNull String[] array) {
						for (int i = 0; i < array.length; i++){
							System.out.println(array[i]);
						}
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

	        AssistContext context= getCorrectionContext(cu, str.indexOf("for"), 0);
			ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 3);

			String str1= """
				package test1;
				import annots.*;
				public class A {
				    public void foo(@NonNull String[] array) {
						for (String element : array) {
							System.out.println(element);
						}
				    }
				}
				""";
			assertProposalPreviewEquals(str1, "Convert to enhanced 'for' loop", proposals);
		} finally {
			NullTestUtils.disableAnnotationBasedNullAnalysis(fSourceFolder);
		}
	}
	@Test
	public void testNoRedundantNonNullInConvertIterableForLoop() throws Exception {
		NullTestUtils.prepareNullTypeAnnotations(fSourceFolder);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				import java.util.Iterator;
				import annots.*;
				@NonNullByDefault
				public class A {
				    public void foo(Iterable<String> x) {
						for (Iterator<String> iterator = x.iterator(); iterator.hasNext();){
							System.out.println(iterator.next());
						}
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

	        AssistContext context= getCorrectionContext(cu, str.indexOf("for"), 0);
			ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertCorrectLabels(proposals);
			assertNumberOfProposals(proposals, 3);

			String str1= """
				package test1;
				import annots.*;
				@NonNullByDefault
				public class A {
				    public void foo(Iterable<String> x) {
						for (String string : x) {
							System.out.println(string);
						}
				    }
				}
				""";
			assertProposalPreviewEquals(str1, "Convert to enhanced 'for' loop", proposals);
		} finally {
			NullTestUtils.disableAnnotationBasedNullAnalysis(fSourceFolder);
		}
	}

	@Test
	public void testSurroundWithTryWithResource_01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			import java.io.IOException;
			import java.io.InputStream;
			import java.net.Socket;
			
			public class E {
			    public void foo() throws IOException {
			        /*1*/Socket s = new Socket(), s2 = new Socket();
			        /*2*/InputStream is = s.getInputStream();
			        /*3*/int i = 0;
			        System.out.println(s.getInetAddress().toString());
			        System.out.println(is.markSupported());/*0*/
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		String strEnd= "/*0*/";

		String str1= "/*1*/";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str.indexOf(strEnd) - str.indexOf(str1));
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package p;
			
			import java.io.IOException;
			import java.io.InputStream;
			import java.net.Socket;
			
			public class E {
			    public void foo() throws IOException {
			        try (/*1*/Socket s = new Socket();
			                Socket s2 = new Socket();
			                /*2*/InputStream is = s.getInputStream()) {
			            /*3*/int i = 0;
			            System.out.println(s.getInetAddress().toString());
			            System.out.println(is.markSupported());/*0*/
			        }
			    }
			}
			
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });

		str1= "/*2*/";
		context= getCorrectionContext(cu, str.indexOf(str1), str.indexOf(strEnd) - str.indexOf(str1));
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package p;
			
			import java.io.IOException;
			import java.io.InputStream;
			import java.net.Socket;
			
			public class E {
			    public void foo() throws IOException {
			        /*1*/Socket s = new Socket(), s2 = new Socket();
			        try (/*2*/InputStream is = s.getInputStream()) {
			            /*3*/int i = 0;
			            System.out.println(s.getInetAddress().toString());
			            System.out.println(is.markSupported());/*0*/
			        }
			    }
			}
			
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview2 }, new String[] { expected2 });

		str1= "/*3*/";
		context= getCorrectionContext(cu, str.indexOf(str1), str.indexOf(strEnd) - str.indexOf(str1));
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
	}
	@Test
	public void testSurroundWithTryWithResource_02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.InputStream;
			import java.net.Socket;
			
			public class E {
			    public void foo() throws FileNotFoundException {
			        /*1*/Socket s = new Socket(), s2 = new Socket();
			        /*2*/InputStream is = s.getInputStream();
			        /*3*/FileInputStream f = new FileInputStream("a.b");
			        /*4*/int i = 0;
			        System.out.println(s.getInetAddress().toString());
			        System.out.println(is.markSupported());/*0*/
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		String strEnd= "/*0*/";

		String str1= "/*1*/";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str.indexOf(strEnd) - str.indexOf(str1));
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package p;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			import java.io.InputStream;
			import java.net.Socket;
			
			public class E {
			    public void foo() throws FileNotFoundException {
			        try (/*1*/Socket s = new Socket();
			                Socket s2 = new Socket();
			                /*2*/InputStream is = s.getInputStream();
			                /*3*/FileInputStream f = new FileInputStream("a.b")) {
			            /*4*/int i = 0;
			            System.out.println(s.getInetAddress().toString());
			            System.out.println(is.markSupported());/*0*/
			        } catch (FileNotFoundException e) {
			            throw e;
			        } catch (IOException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
			        }
			    }
			}
			
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });

		str1= "/*2*/";
		context= getCorrectionContext(cu, str.indexOf(str1), str.indexOf(strEnd) - str.indexOf(str1));
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package p;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			import java.io.InputStream;
			import java.net.Socket;
			
			public class E {
			    public void foo() throws FileNotFoundException {
			        /*1*/Socket s = new Socket(), s2 = new Socket();
			        try (/*2*/InputStream is = s.getInputStream();
			                /*3*/FileInputStream f = new FileInputStream("a.b")) {
			            /*4*/int i = 0;
			            System.out.println(s.getInetAddress().toString());
			            System.out.println(is.markSupported());/*0*/
			        } catch (FileNotFoundException e) {
			            throw e;
			        } catch (IOException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
			        }
			    }
			}
			
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview2 }, new String[] { expected2 });

		str1= "/*4*/";
		context= getCorrectionContext(cu, str.indexOf(str1), str.indexOf(strEnd) - str.indexOf(str1));
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
	}
	@Test
	public void testSurroundWithTryWithResource_03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.InputStream;
			import java.net.Socket;
			
			public class E {
			    public void foo() {
			        try {
			            /*1*/Socket s = new Socket(), s2 = new Socket();
			            /*2*/InputStream is = s.getInputStream();
			            /*3*/FileInputStream f = new FileInputStream("a.b");
			            /*4*/int i = 0;
			            System.out.println(s.getInetAddress().toString());
			            System.out.println(is.markSupported());/*0*/
			        } catch (FileNotFoundException e) {
			        }
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		String strEnd= "/*0*/";

		String str1= "/*1*/";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str.indexOf(strEnd) - str.indexOf(str1));
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package p;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			import java.io.InputStream;
			import java.net.Socket;
			
			public class E {
			    public void foo() {
			        try (/*1*/Socket s = new Socket();
			                Socket s2 = new Socket();
			                /*2*/InputStream is = s.getInputStream();
			                /*3*/FileInputStream f = new FileInputStream("a.b")) {
			            /*4*/int i = 0;
			            System.out.println(s.getInetAddress().toString());
			            System.out.println(is.markSupported());/*0*/
			        } catch (FileNotFoundException e) {
			        } catch (IOException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
			        }
			    }
			}
			
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
		assertCorrectLabels(proposals);
	}
	@Test
	public void testSurroundWithTryWithResource_04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.InputStream;
			import java.net.Socket;
			
			public class E {
			    public void foo() throws FileNotFoundException {
			        try {
			            /*1*/Socket s = new Socket(), s2 = new Socket();
			            /*2*/InputStream is = s.getInputStream();
			            /*3*/FileInputStream f = new FileInputStream("a.b");
			            /*4*/int i = 0;
			            System.out.println(s.getInetAddress().toString());
			            System.out.println(is.markSupported());/*0*/
			        } catch (FileNotFoundException e) {
			        }
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		String strEnd= "/*0*/";

		String str1= "/*1*/";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str.indexOf(strEnd) - str.indexOf(str1));
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package p;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			import java.io.InputStream;
			import java.net.Socket;
			
			public class E {
			    public void foo() throws FileNotFoundException {
			        try (/*1*/Socket s = new Socket();
			                Socket s2 = new Socket();
			                /*2*/InputStream is = s.getInputStream();
			                /*3*/FileInputStream f = new FileInputStream("a.b")) {
			            /*4*/int i = 0;
			            System.out.println(s.getInetAddress().toString());
			            System.out.println(is.markSupported());/*0*/
			        } catch (FileNotFoundException e) {
			        } catch (IOException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
			        }
			    }
			}
			
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
		assertCorrectLabels(proposals);
	}

	@Test
	public void testSurroundWithTryWithResource_05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			import java.io.File;
			import java.util.stream.Stream;
			
			public class X {
				public static void main(String[] args) throws Exception {
					try {
						try {
							Stream<File> stream = Stream.of(new File(""));
							System.out.println(stream);
						} catch (Exception e) {
						}
					} catch (Exception e) {
					}
				}
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", str, false, null);

		int cursor = str.indexOf("Stream<File>");
		AssistContext context= getCorrectionContext(cu, cursor + 1, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) findProposalByName("Surround with try-with-resources", proposals);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package p;
			
			import java.io.File;
			import java.util.stream.Stream;
			
			public class X {
				public static void main(String[] args) throws Exception {
					try {
						try (Stream<File> stream = Stream.of(new File(""))) {
							System.out.println(stream);
						} catch (Exception e) {
						}
					} catch (Exception e) {
					}
				}
			}""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
		assertCorrectLabels(proposals);
	}


	@Test
	public void testWrapInOptional_01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.Optional;
			public class E {
				Optional<Integer> a = 1;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		CompilationUnit compilationUnit= getASTRoot(cu);
		IProblem[] problems= compilationUnit.getProblems();
		assertNumberOfProblems(1, problems);

		List<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);

		assertNumberOfProposals(proposals, 3);

		String str1= """
			package p;
			import java.util.Optional;
			public class E {
				Optional<Integer> a = Optional.of(1);
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });

		String str2= """
			package p;
			import java.util.Optional;
			public class E {
				Optional<Integer> a = Optional.empty();
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str2 });
	}
	@Test
	public void testWrapInOptional_02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.Optional;
			public class E {
				Optional<Object> foo(int x) {
					return bar();
				}
				Object bar() {
					return null;
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		CompilationUnit compilationUnit= getASTRoot(cu);
		IProblem[] problems= compilationUnit.getProblems();
		assertNumberOfProblems(1, problems);

		List<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);

		assertNumberOfProposals(proposals, 6);

		String str1= """
			package p;
			import java.util.Optional;
			public class E {
				Optional<Object> foo(int x) {
					return Optional.of(bar());
				}
				Object bar() {
					return null;
				}
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });

		String str2= """
			package p;
			import java.util.Optional;
			public class E {
				Optional<Object> foo(int x) {
					return Optional.ofNullable(bar());
				}
				Object bar() {
					return null;
				}
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str2 });

		String str3= """
			package p;
			import java.util.Optional;
			public class E {
				Optional<Object> foo(int x) {
					return Optional.empty();
				}
				Object bar() {
					return null;
				}
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str3 });
	}
	@Test
	public void testWrapInOptional_03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.Optional;
			public class E <T> {
				Optional<T> a = 1;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		CompilationUnit compilationUnit= getASTRoot(cu);
		IProblem[] problems= compilationUnit.getProblems();
		assertNumberOfProblems(1, problems);

		List<IJavaCompletionProposal> proposals= collectCorrections(cu, problems[0], null);

		assertNumberOfProposals(proposals, 2);

		String str1= """
			package p;
			import java.util.Optional;
			public class E <T> {
				Optional<T> a = Optional.empty();
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testAssignInTryWithResources_01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			import java.io.FileInputStream;
			import java.io.IOException;
			
			class E {
			    void f() throws IOException {
			        new FileInputStream("f");
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);

		int offset= src.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		String expected=
				"""
			package test1;
			
			import java.io.FileInputStream;
			import java.io.IOException;
			
			class E {
			    void f() throws IOException {
			        try (FileInputStream fileInputStream = new FileInputStream("f")) {
			           \s
			        };
			    }
			}""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testAssignInTryWithResources_02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			
			class E {
			    void f() throws FileNotFoundException {
			        new FileInputStream("f");
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);

		int offset= src.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		String expected=
				"""
			package test1;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			
			class E {
			    void f() throws FileNotFoundException {
			        try (FileInputStream fileInputStream = new FileInputStream("f")) {
			           \s
			        } catch (FileNotFoundException e) {
			            throw e;
			        } catch (IOException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
			        };
			    }
			}""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testAssignInTryWithResources_03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			
			class E {
			    void f() {
			        try {
			            new FileInputStream("f");
			        } catch (FileNotFoundException e) {
			            // some action
			        }
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);

		int offset= src.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		String expected=
				"""
			package test1;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			
			class E {
			    void f() {
			        try (FileInputStream fileInputStream = new FileInputStream("f")) {
			        } catch (FileNotFoundException e) {
			            // some action
			        } catch (IOException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
			        }
			    }
			}""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testAssignInTryWithResources_04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			
			class E {
			    void f() throws FileNotFoundException {
			        try {
			            String s = "a.b";
			            new FileInputStream(s);
			        } catch (FileNotFoundException e) {
			            // some action
			        }
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);

		int offset= src.indexOf("new");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String expected=
				"""
			package test1;
			
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			
			class E {
			    void f() throws FileNotFoundException {
			        try {
			            String s = "a.b";
			            try (FileInputStream fileInputStream = new FileInputStream(s)) {
			               \s
			            } catch (FileNotFoundException e) {
			                throw e;
			            } catch (IOException e) {
			                // TODO Auto-generated catch block
			                e.printStackTrace();
			            };
			        } catch (FileNotFoundException e) {
			            // some action
			        }
			    }
			}""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testSplitTryWithResources1() throws Exception { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=530208
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			import java.io.BufferedReader;
			import java.io.FileNotFoundException;
			import java.io.FileReader;
			import java.io.IOException;
			import java.io.Reader;
			
			class E {
			    public void foo() {
			        try (Reader s = new BufferedReader(new FileReader("c.d"));
			                Reader r = new BufferedReader(new FileReader("a.b"));
			                Reader t = new BufferedReader(new FileReader("e.f"))) {
			            r.read();
			            System.out.println("abc");
			        } catch (FileNotFoundException e) {
			            e.printStackTrace();
			        } catch (IOException e) {
			            e.printStackTrace();
			        }
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);

		int offset= src.indexOf("\"a.b\"");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		String expected=
				"""
			package test1;
			
			import java.io.BufferedReader;
			import java.io.FileNotFoundException;
			import java.io.FileReader;
			import java.io.IOException;
			import java.io.Reader;
			
			class E {
			    public void foo() {
			        try (Reader s = new BufferedReader(new FileReader("c.d"))) {
			            try (Reader r = new BufferedReader(new FileReader("a.b"));
			                    Reader t = new BufferedReader(new FileReader("e.f"))) {
			                r.read();
			                System.out.println("abc");
			            }\s
			        } catch (FileNotFoundException e) {
			            e.printStackTrace();
			        } catch (IOException e) {
			            e.printStackTrace();
			        }
			    }
			}""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testSplitTryWithResources2() throws Exception { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=530208
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			import java.io.BufferedReader;
			import java.io.FileNotFoundException;
			import java.io.FileReader;
			import java.io.IOException;
			import java.io.Reader;
			
			class E {
			    public void foo() {
			        try (Reader s = new BufferedReader(new FileReader("c.d"));
			                Reader r = new BufferedReader(new FileReader("a.b"));
			                Reader t = new BufferedReader(new FileReader("e.f"))) {
			            r.read();
			            System.out.println("abc");
			        } catch (FileNotFoundException e) {
			            e.printStackTrace();
			        } catch (IOException e) {
			            e.printStackTrace();
			        }
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);

		int offset= src.indexOf("\"e.f\"");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		String expected=
				"""
			package test1;
			
			import java.io.BufferedReader;
			import java.io.FileNotFoundException;
			import java.io.FileReader;
			import java.io.IOException;
			import java.io.Reader;
			
			class E {
			    public void foo() {
			        try (Reader s = new BufferedReader(new FileReader("c.d"));
			                Reader r = new BufferedReader(new FileReader("a.b"))) {
			            try (Reader t = new BufferedReader(new FileReader("e.f"))) {
			                r.read();
			                System.out.println("abc");
			            }\s
			        } catch (FileNotFoundException e) {
			            e.printStackTrace();
			        } catch (IOException e) {
			            e.printStackTrace();
			        }
			    }
			}""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testInlineDeprecated_1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			class E {
			    private class E1 {
			        public int foo(int a, int b) {
			            return a + b;
			        }
			        /**
			         * @deprecated use {@link #foo(int, int)} instead
			         * @param x - x
			         * @param y - y
			         * @param z - z
			         */
			        @Deprecated
			        public int foo(int x, int y, int z) {
			            int k = 2*y + 3*z;
			            return foo(x, k);
			        }
			    }
			    public int callfoo(int a, int b, int c) {
			        E1 e1= new E1();
			        return e1.foo(a, b, c);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);
		int offset= src.indexOf("e1.foo");
		AssistContext context= getCorrectionContext(cu, offset + 3, 3);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);


		String expected=
				"""
			package test1;
			
			class E {
			    private class E1 {
			        public int foo(int a, int b) {
			            return a + b;
			        }
			        /**
			         * @deprecated use {@link #foo(int, int)} instead
			         * @param x - x
			         * @param y - y
			         * @param z - z
			         */
			        @Deprecated
			        public int foo(int x, int y, int z) {
			            int k = 2*y + 3*z;
			            return foo(x, k);
			        }
			    }
			    public int callfoo(int a, int b, int c) {
			        E1 e1= new E1();
			        int k = 2*b + 3*c;
			        return e1.foo(a, k);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected });

	}

	@Test
	public void testInlineDeprecated_2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			class E {
			    private class E1 {
			        public int foo(int a, int b) {
			            return a + b;
			        }
			        /**
			         * @deprecated use {@link #foo(int, int)} instead
			         * @param x - x
			         * @param y - y
			         * @param z - z
			         */
			        @Deprecated
			        public int foo(int x, int y, int z) {
			            int k = 2*y + 3*z;
			            return k;
			        }
			    }
			    public int callfoo(int a, int b, int c) {
			        E1 e1= new E1();
			        return e1.foo(a, b, c);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);
		int offset= src.indexOf("e1.foo");
		AssistContext context= getCorrectionContext(cu, offset + 3, 3);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testInlineDeprecated_3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			class E {
			    private class E1 {
			        private int v = 5;
			        public int foo(int a, int b) {
			            return a + b;
			        }
			        /**
			         * @deprecated use {@link #foo(int, int)} instead
			         * @param x - x
			         * @param y - y
			         * @param z - z
			         */
			        @Deprecated
			        public int foo(int x, int y, int z) {
			            int k = 2*y + 3*z + v;
			            return foo(x, k);
			        }
			    }
			    public int callfoo(int a, int b, int c) {
			        E1 e1= new E1();
			        return e1.foo(a, b, c);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", src, false, null);
		int offset= src.indexOf("e1.foo");
		AssistContext context= getCorrectionContext(cu, offset + 3, 3);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);


		String expected=
				"""
			package test1;
			
			class E {
			    private class E1 {
			        private int v = 5;
			        public int foo(int a, int b) {
			            return a + b;
			        }
			        /**
			         * @deprecated use {@link #foo(int, int)} instead
			         * @param x - x
			         * @param y - y
			         * @param z - z
			         */
			        @Deprecated
			        public int foo(int x, int y, int z) {
			            int k = 2*y + 3*z + v;
			            return foo(x, k);
			        }
			    }
			    public int callfoo(int a, int b, int c) {
			        E1 e1= new E1();
			        int k = 2*b + 3*c + e1.v;
			        return e1.foo(a, k);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testInlineDeprecated_4() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			public class E1 {
			    private int v = 5;
			    public int foo(int a, int b) {
			        return a + b;
			    }
			    /**
			     * @deprecated use {@link #foo(int, int)} instead
			     * @param x - x
			     * @param y - y
			     * @param z - z
			     */
			    @Deprecated
			    public int foo(int x, int y, int z) {
			        int k = 2*y + 3*z + v;
			        return foo(x, k);
			    }
			}
			""";
		pack1.createCompilationUnit("E1.java", src, false, null);
		String src1=
				"""
			package test1;
			
			class E {
			    public int callfoo(int a, int b, int c) {
			        E1 e1= new E1();
			        return e1.foo(a, b, c);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", src1, false, null);
		int offset= src1.indexOf("e1.foo");
		AssistContext context= getCorrectionContext(cu1, offset + 3, 3);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testInlineDeprecated_5() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			public class E1 {
			    public int v = 5;
			    public int foo(int a, int b) {
			        return a + b;
			    }
			    /**
			     * @deprecated use {@link #foo(int, int)} instead
			     * @param x - x
			     * @param y - y
			     * @param z - z
			     */
			    @Deprecated
			    public int foo(int x, int y, int z) {
			        int k = 2*y + 3*z + v;
			        return foo(x, k);
			    }
			}
			""";
		pack1.createCompilationUnit("E1.java", src, false, null);
		String src1=
				"""
			package test1;
			
			class E {
			    public int callfoo(int a, int b, int c) {
			        E1 e1= new E1();
			        return e1.foo(a, b, c);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", src1, false, null);
		int offset= src1.indexOf("e1.foo");
		AssistContext context= getCorrectionContext(cu1, offset + 3, 3);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 1);

		String expected=
				"""
			package test1;
			
			class E {
			    public int callfoo(int a, int b, int c) {
			        E1 e1= new E1();
			        int k = 2*b + 3*c + e1.v;
			        return e1.foo(a, k);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testInlineDeprecated_6() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String src=
				"""
			package test1;
			
			public class E1 {
			    public int v = 5;
			    public int foo(int a, int b) {
			        return a + b;
			    }
			    @Deprecated
			    public int foo(int x, int y, int z) {
			        int k = 2*y + 3*z + v;
			        return foo(x, k);
			    }
			}
			""";
		pack1.createCompilationUnit("E1.java", src, false, null);
		String src1=
				"""
			package test1;
			
			class E {
			    public int callfoo(int a, int b, int c) {
			        E1 e1= new E1();
			        return e1.foo(a, b, c);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", src1, false, null);
		int offset= src1.indexOf("e1.foo");
		AssistContext context= getCorrectionContext(cu1, offset + 3, 3);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 0);
	}

}

