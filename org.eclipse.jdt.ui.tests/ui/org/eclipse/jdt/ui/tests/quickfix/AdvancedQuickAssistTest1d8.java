/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class AdvancedQuickAssistTest1d8 extends QuickFixTest {
	@Rule
    public ProjectTestSetup projectSetup= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testConvertToIfReturn1() throws Exception {
		// 'if' in lambda body - positive cases
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			@FunctionalInterface
			interface A {
			    void run(int n);
			}
			
			@FunctionalInterface
			interface B {
			    A foo(int x);
			}
			
			public class Test {
			    A fi0 = (n1) -> {
			        if (n1 == 0) {
			            System.out.println(n1);
			            return;
			        }
			    };
			   \s
			    int fun1(int a, int b) {
			        A fi2 = (n2) -> {
			            if (a == b) {
			                System.out.println(n2);
			                return;
			            }
			        };
			        return a + b;
			    }
			
			    A fun2(int a1, int b1) {
			        return (n) -> {
			            if (a1 == b1) {
			                System.out.println(n);
			                return;
			            }
			        };
			    }
			
			    int fun3(int a2, int b2) {
			        B fi3 = (x) -> (n) -> {
			            if (a2 == b2) {
			                System.out.println(a2);
			                return;
			            }
			        };
			        return a2 + b2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str1, false, null);

		String str= "if (n1 == 0)";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			@FunctionalInterface
			interface A {
			    void run(int n);
			}
			
			@FunctionalInterface
			interface B {
			    A foo(int x);
			}
			
			public class Test {
			    A fi0 = (n1) -> {
			        if (n1 != 0)
			            return;
			        System.out.println(n1);
			    };
			   \s
			    int fun1(int a, int b) {
			        A fi2 = (n2) -> {
			            if (a == b) {
			                System.out.println(n2);
			                return;
			            }
			        };
			        return a + b;
			    }
			
			    A fun2(int a1, int b1) {
			        return (n) -> {
			            if (a1 == b1) {
			                System.out.println(n);
			                return;
			            }
			        };
			    }
			
			    int fun3(int a2, int b2) {
			        B fi3 = (x) -> (n) -> {
			            if (a2 == b2) {
			                System.out.println(a2);
			                return;
			            }
			        };
			        return a2 + b2;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		str= "if (a == b)";
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			@FunctionalInterface
			interface A {
			    void run(int n);
			}
			
			@FunctionalInterface
			interface B {
			    A foo(int x);
			}
			
			public class Test {
			    A fi0 = (n1) -> {
			        if (n1 == 0) {
			            System.out.println(n1);
			            return;
			        }
			    };
			   \s
			    int fun1(int a, int b) {
			        A fi2 = (n2) -> {
			            if (a != b)
			                return;
			            System.out.println(n2);
			        };
			        return a + b;
			    }
			
			    A fun2(int a1, int b1) {
			        return (n) -> {
			            if (a1 == b1) {
			                System.out.println(n);
			                return;
			            }
			        };
			    }
			
			    int fun3(int a2, int b2) {
			        B fi3 = (x) -> (n) -> {
			            if (a2 == b2) {
			                System.out.println(a2);
			                return;
			            }
			        };
			        return a2 + b2;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });


		str= "if (a1 == b1)";
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			@FunctionalInterface
			interface A {
			    void run(int n);
			}
			
			@FunctionalInterface
			interface B {
			    A foo(int x);
			}
			
			public class Test {
			    A fi0 = (n1) -> {
			        if (n1 == 0) {
			            System.out.println(n1);
			            return;
			        }
			    };
			   \s
			    int fun1(int a, int b) {
			        A fi2 = (n2) -> {
			            if (a == b) {
			                System.out.println(n2);
			                return;
			            }
			        };
			        return a + b;
			    }
			
			    A fun2(int a1, int b1) {
			        return (n) -> {
			            if (a1 != b1)
			                return;
			            System.out.println(n);
			        };
			    }
			
			    int fun3(int a2, int b2) {
			        B fi3 = (x) -> (n) -> {
			            if (a2 == b2) {
			                System.out.println(a2);
			                return;
			            }
			        };
			        return a2 + b2;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });


		str= "if (a2 == b2)";
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		expected1= """
			package test1;
			@FunctionalInterface
			interface A {
			    void run(int n);
			}
			
			@FunctionalInterface
			interface B {
			    A foo(int x);
			}
			
			public class Test {
			    A fi0 = (n1) -> {
			        if (n1 == 0) {
			            System.out.println(n1);
			            return;
			        }
			    };
			   \s
			    int fun1(int a, int b) {
			        A fi2 = (n2) -> {
			            if (a == b) {
			                System.out.println(n2);
			                return;
			            }
			        };
			        return a + b;
			    }
			
			    A fun2(int a1, int b1) {
			        return (n) -> {
			            if (a1 == b1) {
			                System.out.println(n);
			                return;
			            }
			        };
			    }
			
			    int fun3(int a2, int b2) {
			        B fi3 = (x) -> (n) -> {
			            if (a2 != b2)
			                return;
			            System.out.println(a2);
			        };
			        return a2 + b2;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToIfReturn2() throws Exception {
		// 'if' in lambda body - negative cases
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			@FunctionalInterface
			interface A {
			    void run(int n);
			}
			
			@FunctionalInterface
			interface B {
			    A foo(int x);
			}
			
			public class Test {
			    int f1(int a2, int b2) {
			        B fi3 = (x) -> {
			            if (x != 100) {
			                return (n) -> System.out.println(n + x);
			            }
			        };
			        return a2 + b2;
			    }
			   \s
			    void f2(int a1, int b1) {
			        A a= (n) -> {
			            if (a1 == b1) {
			                System.out.println(n);
			                return;
			            }
			            bar();
			        };
			    }
			
			    private void bar() {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str1, false, null);

		String str= "if (x != 100)"; // #foo does not return void
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);

		str= "if (a1 == b1)"; // not the last executable statement in lambda body
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);
	}
}
