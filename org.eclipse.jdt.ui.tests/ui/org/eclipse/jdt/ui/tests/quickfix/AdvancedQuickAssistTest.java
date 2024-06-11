/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Konstantin Scheglov (scheglov_ke@nlmk.ru) - initial API and implementation
 *          (reports 71244 & 74746: New Quick Assist's [quick assist])
 *   Benjamin Muskalla (buskalla@innoopract.com) - 104021: [quick fix] Introduce
 *   		new local with casted type applied more than once
 *   Billy Huang <billyhuang31@gmail.com> - [quick assist] concatenate/merge string literals - https://bugs.eclipse.org/77632
 *   Robert Roth <robert.roth.off@gmail.com> - [templates] 2 new code templates: finally & lock - https://bugs.eclipse.org/184222
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Preferences;

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
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.QuickTemplateProcessor;

public class AdvancedQuickAssistTest extends QuickFixTest {
	@Rule
    public ProjectTestSetup projectSetup= new ProjectTestSetup();

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

		Preferences corePrefs= JavaPlugin.getJavaCorePluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testSplitIfCondition1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b) {
			        if (a && (b == 0)) {
			            b= 9;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("&&");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b) {
			        if (a) {
			            if (b == 0) {
			                b= 9;
			            }
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testSplitIfCondition2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a && (b == 0) && c) {
			            b= 9;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("&& (");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a) {
			            if (b == 0 && c) {
			                b= 9;
			            }
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testSplitIfCondition3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a && (b == 0) && c) {
			            b= 9;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("&& c");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a && (b == 0)) {
			            if (c) {
			                b= 9;
			            }
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testSplitIfElseCondition() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b) {
			        if (a && (b == 0)) {
			            b= 9;
			        } else {
			            b= 2;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("&&");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		ArrayList<String> previews= new ArrayList<>();
		ArrayList<String> expecteds= new ArrayList<>();

		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(boolean a, int b) {\n");
		buf.append("        if (a) {\n");
		buf.append("            if (b == 0) {\n");
		buf.append("                b= 9;\n");
		buf.append("            } else {\n");
		buf.append("                b= 2;\n");
		buf.append("            }\n");
		buf.append("        } else {\n");
		buf.append("            b= 2;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		addPreviewAndExpected(proposals, buf, expecteds, previews);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(boolean a, int b) {\n");
		buf.append("        if ((b == 0) && a) {\n");
		buf.append("            b= 9;\n");
		buf.append("        } else {\n");
		buf.append("            b= 2;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		addPreviewAndExpected(proposals, buf, expecteds, previews);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(boolean a, int b) {\n");
		buf.append("        boolean c = a && (b == 0);\n");
		buf.append("        if (c) {\n");
		buf.append("            b= 9;\n");
		buf.append("        } else {\n");
		buf.append("            b= 2;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		addPreviewAndExpected(proposals, buf, expecteds, previews);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(boolean a, int b) {\n");
		buf.append("        boolean c = a && (b == 0);\n");
		buf.append("        if (c) {\n");
		buf.append("            b= 9;\n");
		buf.append("        } else {\n");
		buf.append("            b= 2;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		addPreviewAndExpected(proposals, buf, expecteds, previews);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(boolean a, int b) {\n");
		buf.append("        if ((a && (b == 0))) {\n");
		buf.append("            b= 9;\n");
		buf.append("        } else {\n");
		buf.append("            b= 2;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		addPreviewAndExpected(proposals, buf, expecteds, previews);

		assertEqualStringsIgnoreOrder(previews, expecteds);
	}

	@Test
	public void testJoinAndIfStatements1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a && (b == 0)) {
			            if (c) {
			                b= 9;
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (a");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a && (b == 0) && c) {
			            b= 9;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testJoinAndIfStatements2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a && (b == 0))
			            if (c) {
			                b= 9;
			            }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (a");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a && (b == 0) && c) {
			            b= 9;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testJoinAndIfStatements3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a && (b == 0)) {
			            if (c) {
			                b= 9;
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (c");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a && (b == 0) && c) {
			            b= 9;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testJoinAndIfStatements4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a && (b == 0))
			            if (c) {
			                b= 9;
			            }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (c");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a && (b == 0) && c) {
			            b= 9;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testJoinAndIfStatementsBug335173() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object a, int x) {
			        if (a instanceof String) {
			            if (x > 2) {
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (a");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object a, int x) {
			        if (a instanceof String && x > 2) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testJoinOrIfStatements1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a)
			            return;
			        if (b == 5)
			            return;
			        b= 9;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("if (a");
		int offset2= str.lastIndexOf("b= 9;");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		for (Iterator<IJavaCompletionProposal> I= proposals.iterator(); I.hasNext();) {
			Object o= I.next();
			if (!(o instanceof CUCorrectionProposal))
				I.remove();
		}

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a || b == 5)
			            return;
			        b= 9;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testJoinOrIfStatementsBug335173() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object a, int x) {
			        if (a instanceof String)
			            return;
			        if (x > 2)
			            return;
			        x= 9;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("if (a");
		int offset2= str.lastIndexOf("x= 9;");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		for (Iterator<IJavaCompletionProposal> I= proposals.iterator(); I.hasNext();) {
			Object o= I.next();
			if (!(o instanceof CUCorrectionProposal))
				I.remove();
		}

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object a, int x) {
			        if (a instanceof String || x > 2)
			            return;
			        x= 9;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testSplitOrCondition1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a || b == 5)
			            return;
			        b= 9;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("||");
		AssistContext context= getCorrectionContext(cu, offset, 0);

		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a)
			            return;
			        else if (b == 5)
			            return;
			        b= 9;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testSplitOrCondition2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a || b == 5)
			            return;
			        else {
			            b= 8;
			        }
			        b= 9;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("||");
		AssistContext context= getCorrectionContext(cu, offset, 0);

		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, boolean c) {
			        if (a)
			            return;
			        else if (b == 5)
			            return;
			        else {
			            b= 8;
			        }
			        b= 9;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testIfReturnIntoIfElseAtEndOfVoidMethod1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b) {
			        if (a) {
			            b= 9;
			            return;
			        }
			        b= 0;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b) {
			        if (a) {
			            b= 9;
			        } else {
			            b= 0;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testInverseIfContinueIntoIfThenInLoops1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.*;
			public class E {
			    public void foo(boolean a, ArrayList list) {
			        for (Iterator I = list.iterator(); I.hasNext();) {
			            if (a) {
			                b= 9;
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.*;
			public class E {
			    public void foo(boolean a, ArrayList list) {
			        for (Iterator I = list.iterator(); I.hasNext();) {
			            if (!a)
			                continue;
			            b= 9;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testInverseIfIntoContinueInLoops1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.*;
			public class E {
			    public void foo(boolean a, ArrayList list) {
			        for (Iterator I = list.iterator(); I.hasNext();) {
			            if (!a)
			                continue;
			            b= 9;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.*;
			public class E {
			    public void foo(boolean a, ArrayList list) {
			        for (Iterator I = list.iterator(); I.hasNext();) {
			            if (a) {
			                b= 9;
			            }
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testRemoveExtraParentheses1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, Object o) {
			        if (a && (b == 0) && (o instanceof Integer) && (a || b)) {
			            b= 9;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("if (");
		int offset2= str.indexOf(") {", offset1);
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, Object o) {
			        if (a && b == 0 && o instanceof Integer && (a || b)) {
			            b= 9;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testRemoveExtraParentheses2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public int foo() {
			        return (9+ 8);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "(9+ 8)";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int foo() {
			        return 9+ 8;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testAddParanoidalParentheses1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, int c, Object o) {
			        if (a && b == 0 && b + c > 3 && o instanceof Integer) {
			            b= 9;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("if (");
		int offset2= str.indexOf(") {", offset1);
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b, int c, Object o) {
			        if (a && (b == 0) && ((b + c) > 3) && (o instanceof Integer)) {
			            b= 9;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testAddParenthesesForExpression1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object obj) {
			        if (obj instanceof String) {
			            String string = (String) obj;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("(String) obj"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object obj) {
			        if (obj instanceof String) {
			            String string = ((String) obj);
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAddParenthesesForExpression2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object obj) {
			        if (obj instanceof String) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("instanceof"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object obj) {
			        if ((obj instanceof String)) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAddParenthesesForExpression3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, int b, int c) {
			        if (a + b == 0 && b + c > 3) {
			            b= 9;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("=="), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, int b, int c) {
			        if ((a + b == 0) && b + c > 3) {
			            b= 9;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAddParenthesesForExpression4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, int b, int c) {
			        if (a + b == 0 && b + c > 3) {
			            b= 9;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("+"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, int b, int c) {
			        if ((a + b) == 0 && b + c > 3) {
			            b= 9;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAddParenthesesForExpression5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, int b, int c) {
			        int d = a > 10 ? b : c;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("?"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, int b, int c) {
			        int d = (a > 10 ? b : c);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAddParenthesesForExpression6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, int b, int c) {
			        if (a > 3 && b > 5) {
			            a= 3;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("a > 3"), "a > 3".length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 7);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, int b, int c) {
			        if ((a > 3) && b > 5) {
			            a= 3;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAddParenthesesForExpression7() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=338675
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, int b, int c) {
			        if (a > 3 && b > 5) {
			            a= 3;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("a >"), "a >".length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, "Put '>' expression in parentheses");

	}

	@Test
	public void testAddParenthesesForExpression8() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=338675
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, int b, int c) {
			        if (a > 3 && b > 5) {
			            a= 3;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("a >"), 1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, "Put '>' expression in parentheses");

	}

	@Test
	public void testInverseIfCondition1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, int b) {
			        if (a && (b == 0)) {
			            return;
			        } else {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, int b) {
			        if (!a || (b != 0)) {
			        } else {
			            return;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testInverseIfCondition2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b, boolean c) {
			        if (a || b && c) {
			            return;
			        } else {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b, boolean c) {
			        if (!a && (!b || !c)) {
			        } else {
			            return;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testInverseIfCondition3() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=75109
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if (a)
			            if (b) //inverse
			                return 1;
			            else
			                return 2;
			        return 17;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (b");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if (a)
			            if (!b)
			                return 2;
			            else
			                return 1;
			        return 17;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testInverseIfCondition4() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=74580
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b, boolean c) {
			        if (a) {
			            one();
			        } else if (b) {
			            two();
			        } else {
			            three();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (a");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b, boolean c) {
			        if (!a) {
			            if (b) {
			                two();
			            } else {
			                three();
			            }
			        } else {
			            one();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testInverseIfCondition5() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=74580
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        if (i == 1)
			            one();
			        else if (i == 2)
			            two();
			        else
			            three();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (i == 1");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int i) {
			        if (i != 1) {
			            if (i == 2)
			                two();
			            else
			                three();
			        } else
			            one();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testInverseIfCondition_bug119251() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=119251
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    private boolean a() { return false; }
			    private void foo(int i) {}
			    public void b() {
			        if (!a() && !a() && !a() && !a())
			            foo(1);
			        else
			            foo(2);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test;
			public class E {
			    private boolean a() { return false; }
			    private void foo(int i) {}
			    public void b() {
			        if (a() || a() || a() || a())
			            foo(2);
			        else
			            foo(1);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testInverseIfCondition6() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=119251
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    private boolean a() { return false; }
			    private void foo(int i) {}
			    public void b() {
			        if (!a() && !a() || !a() && !a())
			            foo(1);
			        else
			            foo(2);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test;
			public class E {
			    private boolean a() { return false; }
			    private void foo(int i) {}
			    public void b() {
			        if ((a() || a()) && (a() || a()))
			            foo(2);
			        else
			            foo(1);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testInverseIfConditionUnboxing() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=297645
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Boolean b) {
			        if (b) {
			            System.out.println("######");
			        } else {
			            System.out.println("-");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test;
			public class E {
			    public void foo(Boolean b) {
			        if (!b) {
			            System.out.println("-");
			        } else {
			            System.out.println("######");
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testInverseIfConditionEquals() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if (a == (b && a))
			            return 1;
			        else
			            return 2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if (a != (b && a))
			            return 2;
			        else
			            return 1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testInverseIfCondition_bug117960() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=117960
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if (a || b ? a : b) {
			            System.out.println();
			        } else {
			            return;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if (a || b ? !a : !b) {
			            return;
			        } else {
			            System.out.println();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testInverseIfCondition_bug388074() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=388074
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if (!(a || b) || c) {
			            return 0;
			        } else {
			            return 1;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (!");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if ((a || b) && !c) {
			            return 1;
			        } else {
			            return 0;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testInverseConditionalStatement1() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=74746
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int foo(boolean a) {
			        return a ? 4 : 5;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("?");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int foo(boolean a) {
			        return !a ? 5 : 4;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testInverseConditionalStatement2() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=74746
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int foo(int a) {
			        return a + 6 == 9 ? 4 : 5;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("?");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int foo(int a) {
			        return a + 6 != 9 ? 5 : 4;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testInnerAndOuterIfConditions1() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=74746
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int foo(int a, Object b) {
			        if (a == 8) {
			            if (b instanceof String) {
			                return 0;
			            }
			        }
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (a");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int foo(int a, Object b) {
			        if (b instanceof String) {
			            if (a == 8) {
			                return 0;
			            }
			        }
			        return 1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testInnerAndOuterIfConditions2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int foo(int a, Object b) {
			        if (a == 8)
			            if (b instanceof String) {
			                return 0;
			            }
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (a");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int foo(int a, Object b) {
			        if (b instanceof String)
			            if (a == 8) {
			                return 0;
			            }
			        return 1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testInnerAndOuterIfConditions3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int foo(int a, Object b) {
			        if (a == 8) {
			            if (b instanceof String) {
			                return 0;
			            }
			        }
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (b");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int foo(int a, Object b) {
			        if (b instanceof String) {
			            if (a == 8) {
			                return 0;
			            }
			        }
			        return 1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testInnerAndOuterIfConditions4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int foo(int a, Object b) {
			        if (a == 8)
			            if (b instanceof String) {
			                return 0;
			            }
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if (b");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int foo(int a, Object b) {
			        if (b instanceof String)
			            if (a == 8) {
			                return 0;
			            }
			        return 1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testExchangeOperands1() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=74746
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int a, Object b) {
			        return a == b.hashCode();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("==");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(int a, Object b) {
			        return b.hashCode() == a;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testExchangeOperands2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return (0 == (a & b));
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("==");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return ((a & b) == 0);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testExchangeOperands3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int n = (2 + 3) * (4 + 5);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("*");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        int n = (4 + 5) * (2 + 3);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testExchangeOperands4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return (a < b);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("<");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return (b > a);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testExchangeOperands5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return (a <= b);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("<=");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return (b >= a);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testExchangeOperands6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return (a > b);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf(">");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return (b < a);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testExchangeOperands7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return (a >= b);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf(">=");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return (b <= a);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testExchangeOperandsBug332019_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return b != 0 != (a == b);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!= (");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return (a == b) != (b != 0);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testExchangeOperandsBug332019_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return b > 0 != (a == b);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!=");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return (a == b) != b > 0;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testExchangeOperandsBug332019_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return b == 0 == true == false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("== false");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return false == (b == 0 == true);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testExchangeOperandsBug332019_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return b + 1 != a - 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!=");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(int a, int b) {
			        return a - 1 != b + 1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAssignAndCast1() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=75066
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("instanceof");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			            String string = (String) b;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testAssignAndCast2() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=75066
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        while (b instanceof String)
			            return;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("instanceof");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        while (b instanceof String) {
			            String string = (String) b;
			            return;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testAssignAndCastBug_104021() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			            String string = "";
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("instanceof");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			            String string2 = (String) b;
			            String string = "";
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testAssignAndCastBug129336_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (!(b instanceof String)) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("instanceof");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (!(b instanceof String)) {
			        }
			        String string = (String) b;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testAssignAndCast129336_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        while (!(b instanceof String))
			            return;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("instanceof");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        while (!(b instanceof String))
			            return;
			        String string = (String) b;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testAssignAndCastBug129336_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (!(b instanceof String)) {
			        } else {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("instanceof");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (!(b instanceof String)) {
			        } else {
			            String string = (String) b;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testAssignAndCastBug331195_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("}") - 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			            String string = (String) b;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAssignAndCastBug331195_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        while (b instanceof String) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("}") - 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        while (b instanceof String) {
			            String string = (String) b;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAssignAndCastBug331195_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("b instanceof");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			            String string = (String) b;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAssignAndCastBug331195_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("String)");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			            String string = (String) b;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAssignAndCastBug331195_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String && a > 10) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("}") - 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String && a > 10) {
			            String string = (String) b;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAssignAndCastBug331195_6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			            int x=10;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("int x") - 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			            String string = (String) b;
			            int x=10;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testAssignAndCastBug331195_7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String)\s
			            return;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("return") - 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        if (b instanceof String) {
			            String string = (String) b;
			            return;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}
@Test
	public void testAssignAndCastBug331195_8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        while (b instanceof String)\s
			            System.out.println(b);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("System") - 1;
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, Object b) {
			        while (b instanceof String) {
			            String string = (String) b;
			            System.out.println(b);
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

@Test
	public void testReplaceReturnConditionWithIf1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public String foo(Object b) {
			        return (b == null) ? null : b.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("?");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public String foo(Object b) {
			        if (b == null)
			            return null;
			        else
			            return b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

@Test
	public void testReplaceReturnConditionWithIf2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public String foo(Object b) {
			        return (b == null) ? null : b.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("return");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public String foo(Object b) {
			        if (b == null)
			            return null;
			        else
			            return b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testReplaceReturnConditionWithIf3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public String foo(Object b) {
			        return (b == null) ? null : b.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int startOffset= str.indexOf("return");
		int endOffset= str.indexOf("    }");
		AssistContext context= getCorrectionContext(cu, startOffset, endOffset - startOffset - 1);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public String foo(Object b) {
			        if (b == null)
			            return null;
			        else
			            return b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testReplaceReturnConditionWithIf4() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=112443
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collections;
			import java.util.List;
			public class E {
			    List<String> foo(List<String> list) {
			        return list != null ? list : Collections.emptyList();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("?");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Collections;
			import java.util.List;
			public class E {
			    List<String> foo(List<String> list) {
			        if (list != null)
			            return list;
			        else
			            return Collections.emptyList();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testReplaceAssignConditionWithIf1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res= (b == null) ? null : b.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("?");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        if (b == null)
			            res = null;
			        else
			            res = b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testReplaceAssignConditionWithIf2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        res= (b == null) ? null : b.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("?");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        if (b == null)
			            res = null;
			        else
			            res = b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testReplaceAssignConditionWithIf3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        int i = 42;
			        i += ( b ) ? 1 : 2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("?");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        int i = 42;
			        if (b)
			            i += 1;
			        else
			            i += 2;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testReplaceAssignConditionWithIf4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res= (b == null) ? null : b.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("res");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        if (b == null)
			            res = null;
			        else
			            res = b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testReplaceAssignConditionWithIf5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res= (b == null) ? null : b.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("Object res");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        if (b == null)
			            res = null;
			        else
			            res = b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testReplaceAssignConditionWithIf6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res= (b == null) ? null : b.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int startOffset= str.indexOf("Object res");
		int endOffset= str.indexOf("    }");
		AssistContext context= getCorrectionContext(cu, startOffset, endOffset - startOffset - 1);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        if (b == null)
			            res = null;
			        else
			            res = b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testReplaceAssignConditionWithIf7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        res= (b == null) ? null : b.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("res=");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        if (b == null)
			            res = null;
			        else
			            res = b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testReplaceAssignConditionWithIf8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        res= (b == null) ? null : b.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int startOffset= str.indexOf("res=");
		int endOffset= str.indexOf("    }");
		AssistContext context= getCorrectionContext(cu, startOffset, endOffset - startOffset - 1);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        if (b == null)
			            res = null;
			        else
			            res = b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}
	@Test
	public void testReplaceConditionalWithIf9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int f1() {
			        boolean b = true;
			        return (b) ? 1 : 2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int startOffset= str.indexOf("?");
		AssistContext context= getCorrectionContext(cu, startOffset, 1);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int f1() {
			        boolean b = true;
			        if (b)
			            return 1;
			        else
			            return 2;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}
	@Test
	public void testReplaceConditionalWithIf10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int f1() {
			        boolean b = true;
			        int r = ((b)) ? ((1)) : ((2));
			        return r;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int startOffset= str.indexOf("?");
		AssistContext context= getCorrectionContext(cu, startOffset, 1);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int f1() {
			        boolean b = true;
			        int r;
			        if (b)
			            r = 1;
			        else
			            r = 2;
			        return r;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}
	public void testReplaceConditionalWithIf11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int f1() {
			        boolean b = true;
			        int r = 2;
			        r += (((b ? 1 : 2)));
			        return r;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int startOffset= str.indexOf("?");
		AssistContext context= getCorrectionContext(cu, startOffset, 1);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int f1() {
			        boolean b = true;
			        int r = 2;
			        if (b)
			            r += 1;
			        else
			            r += 2;
			        return r;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}
	public void testReplaceConditionalWithIf12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int f1() {
			        boolean b = true;
			        return (((b) ? (1) : (2)));
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int startOffset= str.indexOf("?");
		AssistContext context= getCorrectionContext(cu, startOffset, 1);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int f1() {
			        boolean b = true;
			        if (b)
			            return 1;
			        else
			            return 2;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}
	@Test
	public void testReplaceReturnIfWithCondition() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public String foo(Object b) {
			        if (b == null) {
			            return null;
			        } else {
			            return b.toString();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public String foo(Object b) {
			        return b == null ? null : b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testReplaceReturnIfWithCondition2() throws Exception {
		try {
			JavaProjectHelper.set14CompilerOptions(fJProject1);

			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class E {
				    public Number foo(Integer integer) {
				        if (integer != null) {
				            return integer;
				        } else {
				            return Double.valueOf(Double.MAX_VALUE);
				        }
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			int offset= str.indexOf("if");
			AssistContext context= getCorrectionContext(cu, offset, 0);
			assertNoErrors(context);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertCorrectLabels(proposals);

			String expected1= """
				package test1;
				public class E {
				    public Number foo(Integer integer) {
				        return integer != null ? integer : (Number) Double.valueOf(Double.MAX_VALUE);
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, new String[] {expected1});
		} finally {
			JavaProjectHelper.set15CompilerOptions(fJProject1);
		}
	}

	@Test
	public void testReplaceReturnIfWithCondition3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public String foo(Object b) {
			        if (b == null) {
			            return null;
			        }
			        return b.toString();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public String foo(Object b) {
			        return b == null ? null : b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}


	@Test
	public void testReplaceAssignIfWithCondition1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        if (b == null) {
			            res = null;
			        } else {
			            res = b.toString();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object b) {
			        Object res;
			        res = b == null ? null : b.toString();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testReplaceAssignIfWithCondition2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        int res= 0;
			        if (b) {
			            res -= 2;
			        } else {
			            res -= 3;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        int res= 0;
			        res -= b ? 2 : 3;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}



	@Test
	public void testInverseVariable1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean foo(boolean b) {
			        boolean var= false;
			        boolean d= var && b;
			        return d;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("var");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public boolean foo(boolean b) {
			        boolean notVar= true;
			        boolean d= !notVar && b;
			        return d;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testInverseVariable2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        boolean var= b && !b;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("var");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        boolean notVar= !b || b;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testInverseVariable2b() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        boolean var= b & !b;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("var");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        boolean notVar= !b | b;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testInverseVariable3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        boolean var= true;
			        b= var && !var;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("var");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        boolean notVar= false;
			        b= !notVar && notVar;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}


	@Test
	public void testInverseVariable4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        boolean var= false;
			        var |= b;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("var");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        boolean notVar= true;
			        notVar &= !b;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testInverseVariableBug117960() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        boolean var= a || b ? a : b;
			        var |= b;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("var");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        boolean notVar= a || b ? !a : !b;
			        notVar &= !b;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testInverseCondition1() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=334876
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object a, Object b) {
			        if (a == null ^ b == null) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("a ==");
		int length= "a == null ^ b == null".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object a, Object b) {
			        if (!(a == null ^ b == null)) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testInverseCondition2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        if (!(a instanceof String)) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!");
		int length= "!(a instanceof String)".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        if (a instanceof String) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testInverseCondition3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        while (!(a instanceof String)) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!");
		int length= "!(a instanceof String)".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        while (a instanceof String) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testInverseCondition4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        for (int i = 0; !(a instanceof String); i++) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!");
		int length= "!(a instanceof String)".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        for (int i = 0; a instanceof String; i++) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testInverseCondition5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        do {
			        } while (!(a instanceof String));
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!");
		int length= "!(a instanceof String)".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        do {
			        } while (a instanceof String);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testInverseCondition6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        assert !(a instanceof String);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!");
		int length= "!(a instanceof String)".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        assert a instanceof String;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testPushNegationDown1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i, int j, int k) {
			        boolean b= (i > 1) || !(j < 2 || k < 3);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!(");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int i, int j, int k) {
			        boolean b= (i > 1) || j >= 2 && k >= 3;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testPushNegationDown2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i, int j, int k) {
			        boolean b= (i > 1) && !(j < 2 && k < 3);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!(");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int i, int j, int k) {
			        boolean b= (i > 1) && (j >= 2 || k >= 3);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testPushNegationDown3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i, int j, int k) {
			        boolean b= (i > 1) || !(j < 2 || k < 3);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("(j < 2");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int i, int j, int k) {
			        boolean b= (i > 1) || j >= 2 && k >= 3;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testPushNegationDownBug335778_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        if (!(b)) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!(");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, "Push negation down");
	}

	@Test
	public void testPushNegationDownBug335778_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        if (!(a instanceof String)) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!(");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, "Push negation down");
	}

	@Test
	public void testPushNegationDownBug117960() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if (!(a || b ? !a : !b)) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!(");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if (a || b ? a : b) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testPullNegationUp() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i, int j, int k, int m, int n) {
			        boolean b = i > 1 || j >= 2 && k >= 3 || m > 4 || n > 5;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("j >= 2");
		int offset2= str.indexOf(" || m > 4");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int i, int j, int k, int m, int n) {
			        boolean b = i > 1 || !(j < 2 || k < 3) || m > 4 || n > 5;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testPullNegationUpBug335778_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean b) {
			        if (!b) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!b");
		int length= "!b".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, "Pull negation up");
	}

	@Test
	public void testPullNegationUpBug335778_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(Object a) {
			        if (!(a instanceof String)) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("!(");
		int length= "!(a instanceof String)".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, "Pull negation up");
	}

	@Test
	public void testPullNegationUpBug117960() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if (a || b ? a : b) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("a || b");
		int length= "a || b ? a : b".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(boolean a, boolean b) {
			        if (!(a || b ? !a : !b)) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testJoinIfListInIfElseIf() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a, int b) {
			        if (a == 1)
			            System.out.println(1);
			        if (a == 2)
			            if (b > 0)
			                System.out.println(2);
			        if (a == 3)
			            if (b > 0)
			                System.out.println(3);
			            else
			                System.out.println(-3);
			        if (a == 4)
			            System.out.println(4);
			        int stop;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("if (a == 1)");
		int offset2= str.indexOf("int stop;");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a, int b) {
			        if (a == 1)
			            System.out.println(1);
			        else if (a == 2) {
			            if (b > 0)
			                System.out.println(2);
			        } else if (a == 3)
			            if (b > 0)
			                System.out.println(3);
			            else
			                System.out.println(-3); else if (a == 4)
			                System.out.println(4);
			        int stop;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}


	@Test
	public void testConvertSwitchToIf() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a) {
			        switch (a) {
			            case 1:
			                {
			                    System.out.println(1);
			                    break;
			                }
			            case 2:
			            case 3:
			                System.out.println(2);
			                break;
			            case 4:
			                System.out.println(4);
			                return;
			            default:
			                System.out.println(-1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("switch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a) {
			        if (a == 1) {
			            System.out.println(1);
			        } else if (a == 2 || a == 3) {
			            System.out.println(2);
			        } else if (a == 4) {
			            System.out.println(4);
			            return;
			        } else {
			            System.out.println(-1);
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testConvertSwitchToIf2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class A {
			    public enum TimeUnit {
			        SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS
			    }
			    public static int getPower(TimeUnit unit) {
			        switch (unit) {
			        case SECONDS:
			                return 0;
			        case MILLISECONDS:
			                return -3;
			        case MICROSECONDS:
			                return -6;
			        case NANOSECONDS:
			                return -9;
			        default:
			                throw new InternalError();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		int offset= str.indexOf("switch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package pack;
			
			public class A {
			    public enum TimeUnit {
			        SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS
			    }
			    public static int getPower(TimeUnit unit) {
			        if (unit == TimeUnit.SECONDS) {
			            return 0;
			        } else if (unit == TimeUnit.MILLISECONDS) {
			            return -3;
			        } else if (unit == TimeUnit.MICROSECONDS) {
			            return -6;
			        } else if (unit == TimeUnit.NANOSECONDS) {
			            return -9;
			        } else {
			            throw new InternalError();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testConvertSwitchToIf3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class A {
			    final static int SECONDS=1, MILLISECONDS=2, MICROSECONDS=4,NANOSECONDS=8;
			    public static int getPower(int unit) {
			        switch (unit) {
			        case SECONDS:
			                return 0;
			        case MILLISECONDS:
			                return -3;
			        case MICROSECONDS:
			                return -6;
			        case NANOSECONDS:
			                return -9;
			        default:
			                throw new InternalError();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		int offset= str.indexOf("switch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package pack;
			
			public class A {
			    final static int SECONDS=1, MILLISECONDS=2, MICROSECONDS=4,NANOSECONDS=8;
			    public static int getPower(int unit) {
			        if (unit == SECONDS) {
			            return 0;
			        } else if (unit == MILLISECONDS) {
			            return -3;
			        } else if (unit == MICROSECONDS) {
			            return -6;
			        } else if (unit == NANOSECONDS) {
			            return -9;
			        } else {
			            throw new InternalError();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testConvertSwitchToIfBug252104_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foobar() {
			        switch (getFoo() ? getBar() : getBar()) {
			        case 1:
			            System.out.println();
			            break;
			        case 2:
			            System.out.println();
			            break;
			        }
			    }
			    private int getBar() {
			        return 0;
			    }
			    private boolean getFoo() {
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("switch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foobar() {
			        int i = getFoo() ? getBar() : getBar();
			        if (i == 1) {
			            System.out.println();
			        } else if (i == 2) {
			            System.out.println();
			        }
			    }
			    private int getBar() {
			        return 0;
			    }
			    private boolean getFoo() {
			        return false;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testConvertSwitchToIfBug252104_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int x, int y) {
			        switch (x + y) {
			        case 1:
			            System.out.println();
			            break;
			        case 2:
			            System.out.println();
			            break;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("switch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int x, int y) {
			        int i = x + y;
			        if (i == 1) {
			            System.out.println();
			        } else if (i == 2) {
			            System.out.println();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testConvertSwitchToIfBug252040_1() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=252040
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        switch (getSomethingWithSideEffects()) {
			        case 1:
			            System.out.println();
			            break;
			        case 2:
			            System.out.println();
			            break;
			        }
			    }
			    private int getSomethingWithSideEffects() {
			        System.out.println("side effect");
			        return 2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("switch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        int somethingWithSideEffects = getSomethingWithSideEffects();
			        if (somethingWithSideEffects == 1) {
			            System.out.println();
			        } else if (somethingWithSideEffects == 2) {
			            System.out.println();
			        }
			    }
			    private int getSomethingWithSideEffects() {
			        System.out.println("side effect");
			        return 2;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testConvertSwitchToIfBug252040_2() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=252040
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            switch (getSomethingWithSideEffects()) {
			            case 1:
			                System.out.println();
			                break;
			            case 2:
			                System.out.println();
			                break;
			            }
			    }
			    private int getSomethingWithSideEffects() {
			        System.out.println("side effect");
			        return 2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("switch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            int somethingWithSideEffects = getSomethingWithSideEffects();
			            if (somethingWithSideEffects == 1) {
			                System.out.println();
			            } else if (somethingWithSideEffects == 2) {
			                System.out.println();
			            }
			        }
			    }
			    private int getSomethingWithSideEffects() {
			        System.out.println("side effect");
			        return 2;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testConvertSwitchToIfBug352422() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a) {
			        switch (a) {
			            case 1:
			                System.out.println(1);
			                break;
			            case 2:
			            case 3:
			                System.out.println(2);
			                break;
			            case 4:
			            case 5:
			            default:
			                System.out.println(-1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("switch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a) {
			        if (a == 1) {
			            System.out.println(1);
			        } else if (a == 2 || a == 3) {
			            System.out.println(2);
			        } else if (a == 4 || a == 5 || true) {
			            System.out.println(-1);
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testConvertSwitchToIfBug352422_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a) {
			        switch (a) {
			            case 1:
			                System.out.println(1);
			                break;
			            case 2:
			            case 3:
			                System.out.println(2);
			                break;
			            case 4:
			            default:
			            case 5:
			                System.out.println(-1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("switch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testConvertIfToSwitch() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int a) {
			        if (a == 1) {
			            System.out.println(1);
			        } else if (a == 2 || a == 3 || a == 4 || a == 5) {
			            System.out.println(2);
			        } else if (a == 6) {
			            System.out.println(4);
			        } else {
			            System.out.println(-1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int a) {
			        switch (a) {
			            case 1 :
			                System.out.println(1);
			                break;
			            case 2 :
			            case 3 :
			            case 4 :
			            case 5 :
			                System.out.println(2);
			                break;
			            case 6 :
			                System.out.println(4);
			                break;
			            default :
			                System.out.println(-1);
			                break;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertIfToSwitchWithEagerOr() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        if (a == 1) {
			            System.out.println(1);
			        } else if (a == 2 | a == 3 | a == 4 | a == 5) {
			            System.out.println(2);
			        } else if (a == 6) {
			            System.out.println(4);
			        } else {
			            System.out.println(-1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", given, false, null);

		int offset= given.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        switch (a) {
			            case 1 :
			                System.out.println(1);
			                break;
			            case 2 :
			            case 3 :
			            case 4 :
			            case 5 :
			                System.out.println(2);
			                break;
			            case 6 :
			                System.out.println(4);
			                break;
			            default :
			                System.out.println(-1);
			                break;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertIfToSwitchWithXOr() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        if (a == 1) {
			            System.out.println(1);
			        } else if (a == 2 ^ a == 3 ^ a == 4 ^ a == 5) {
			            System.out.println(2);
			        } else if (a == 6) {
			            System.out.println(4);
			        } else {
			            System.out.println(-1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", given, false, null);

		int offset= given.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        switch (a) {
			            case 1 :
			                System.out.println(1);
			                break;
			            case 2 :
			            case 3 :
			            case 4 :
			            case 5 :
			                System.out.println(2);
			                break;
			            case 6 :
			                System.out.println(4);
			                break;
			            default :
			                System.out.println(-1);
			                break;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertIfToSwitchWithOperation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        if (a == 1) {
			            System.out.println(1);
			        } else if (a == 2 || a == 3 || a == 4 || a == 2 + 3) {
			            System.out.println(1 + 1);
			        } else if (a == 6) {
			            System.out.println(4);
			        } else {
			            System.out.println(-1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", given, false, null);

		int offset= given.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        switch (a) {
			            case 1 :
			                System.out.println(1);
			                break;
			            case 2 :
			            case 3 :
			            case 4 :
			            case 2 + 3 :
			                System.out.println(1 + 1);
			                break;
			            case 6 :
			                System.out.println(4);
			                break;
			            default :
			                System.out.println(-1);
			                break;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertIfToSwitchWithComments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        if (a == 1) {
			            System.out.println(1);
			        } else if (a == 2 || a == 3 || a == 4 || a == 2 + /* Addition */ 3) {
			            System.out.println(1 + /* Addition */ 1);
			        } else if (a == 6) {
			            System.out.println(4);
			        } else {
			            System.out.println(-1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", given, false, null);

		int offset= given.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        switch (a) {
			            case 1 :
			                System.out.println(1);
			                break;
			            case 2 :
			            case 3 :
			            case 4 :
			            case 2 + /* Addition */ 3 :
			                System.out.println(1 + /* Addition */ 1);
			                break;
			            case 6 :
			                System.out.println(4);
			                break;
			            default :
			                System.out.println(-1);
			                break;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertIfToSwitchDoNotConvertStringUnderJava1d7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(String s) {
			        if ("abc".equals(s)) {
			            System.out.println();
			        } else if ("xyz".equals(s)) {
			            System.out.println();
			        } else {
			            System.out.println();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertIfElseToSwitch);
	}

	@Test
	public void testConvertIfToSwitchDoNotConvertAnnoyingBreak() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        while (a-- > 0) {
			            if (a == 1) {
			                System.out.println(1);
			                break;
			            } else if (a == 2 || a == 3 || a == 4 || a == 5) {
			                System.out.println(2);
			            } else if (a == 6) {
			                System.out.println(4);
			            } else {
			                System.out.println(-1);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", given, false, null);

		int offset= given.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertIfElseToSwitch);
	}

	@Test
	public void testConvertIfToSwitchOnEnumWithEqual() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class A {
			    public enum TimeUnit {
			        SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS
			    }
			    public static int getPower(TimeUnit unit) {
			        if (unit == TimeUnit.SECONDS) {
			            return 0;
			        } else if (unit == TimeUnit.MILLISECONDS) {
			            return -3;
			        } else if (unit == TimeUnit.MICROSECONDS) {
			            return -6;
			        } else if (unit == TimeUnit.NANOSECONDS) {
			            return -9;
			        } else {
			            throw new InternalError();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package pack;
			
			public class A {
			    public enum TimeUnit {
			        SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS
			    }
			    public static int getPower(TimeUnit unit) {
			        switch (unit) {
			            case SECONDS :
			                return 0;
			            case MILLISECONDS :
			                return -3;
			            case MICROSECONDS :
			                return -6;
			            case NANOSECONDS :
			                return -9;
			            default :
			                throw new InternalError();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertIfToSwitchOnEnumWithMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class A {
			    public enum TimeUnit {
			        SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS
			    }
			    public static int getPower(TimeUnit unit) {
			        if (unit.equals(TimeUnit.SECONDS)) {
			            return 0;
			        } else if (unit.equals(TimeUnit.MILLISECONDS)) {
			            return -3;
			        } else if (unit.equals(TimeUnit.MICROSECONDS)) {
			            return -6;
			        } else if (unit.equals(TimeUnit.NANOSECONDS)) {
			            return -9;
			        } else {
			            throw new InternalError();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package pack;
			
			public class A {
			    public enum TimeUnit {
			        SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS
			    }
			    public static int getPower(TimeUnit unit) {
			        switch (unit) {
			            case SECONDS :
			                return 0;
			            case MILLISECONDS :
			                return -3;
			            case MICROSECONDS :
			                return -6;
			            case NANOSECONDS :
			                return -9;
			            default :
			                throw new InternalError();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertIfToSwitchUsingConstants() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class A {
			    final static int SECONDS=1, MILLISECONDS=2, MICROSECONDS=4,NANOSECONDS=8;
			    public static int getPower(int unit) {
			        if (unit == SECONDS) {
			            return 0;
			        } else if (unit == MILLISECONDS) {
			            return -3;
			        } else if (unit == MICROSECONDS) {
			            return -6;
			        } else if (unit == NANOSECONDS) {
			            return -9;
			        } else {
			            throw new InternalError();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package pack;
			
			public class A {
			    final static int SECONDS=1, MILLISECONDS=2, MICROSECONDS=4,NANOSECONDS=8;
			    public static int getPower(int unit) {
			        switch (unit) {
			            case SECONDS :
			                return 0;
			            case MILLISECONDS :
			                return -3;
			            case MICROSECONDS :
			                return -6;
			            case NANOSECONDS :
			                return -9;
			            default :
			                throw new InternalError();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertIfToSwitchWithoutJumpStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int a= 10;
			    public void foo() {
			        if (this.a == 1) {
			            System.out.println(1);
			        } else if (this.a == 2 || this.a == 3 || this.a == 4) {
			            System.out.println(2);
			        } else if (this.a == 5) {
			            System.out.println(4);
			        } else {
			            System.out.println(-1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    private int a= 10;
			    public void foo() {
			        switch (this.a) {
			            case 1 :
			                System.out.println(1);
			                break;
			            case 2 :
			            case 3 :
			            case 4 :
			                System.out.println(2);
			                break;
			            case 5 :
			                System.out.println(4);
			                break;
			            default :
			                System.out.println(-1);
			                break;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertIfToSwitchWithContinueStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        while (a-- > 0) {
			            if (a == 1) {
			                System.out.println(1);
			                continue;
			            } else if (a == 2 || a == 3 || a == 4 || a == 5) {
			                System.out.println(2);
			            } else if (a == 6) {
			                System.out.println(4);
			            } else {
			                System.out.println(-1);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", given, false, null);

		int offset= given.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        while (a-- > 0) {
			            switch (a) {
			                case 1 :
			                    System.out.println(1);
			                    continue;
			                case 2 :
			                case 3 :
			                case 4 :
			                case 5 :
			                    System.out.println(2);
			                    break;
			                case 6 :
			                    System.out.println(4);
			                    break;
			                default :
			                    System.out.println(-1);
			                    break;
			            }
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertIfToSwitchWithLabeledBreak() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        loop: while (a-- > 0) {
			            if (a == 1) {
			                System.out.println(1);
			                break loop;
			            } else if (a == 2 || a == 3 || a == 4 || a == 5) {
			                System.out.println(2);
			            } else if (a == 6) {
			                System.out.println(4);
			            } else {
			                System.out.println(-1);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", given, false, null);

		int offset= given.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			
			public class E {
			    public void foo(int a) {
			        loop: while (a-- > 0) {
			            switch (a) {
			                case 1 :
			                    System.out.println(1);
			                    break loop;
			                case 2 :
			                case 3 :
			                case 4 :
			                case 5 :
			                    System.out.println(2);
			                    break;
			                case 6 :
			                    System.out.println(4);
			                    break;
			                default :
			                    System.out.println(-1);
			                    break;
			            }
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testConvertIfToSwitchWithMethodAsDiscriminant() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int a= 10;
			    public int getA() {
			        return a;
			    }
			    public void foo() {
			        if (getA() == 1) {
			            System.out.println(1);
			        } else if (getA() == 2) {
			            System.out.println(2);
			        } else if (getA() == 3) {
			            System.out.println(3);
			        } else {
			            System.out.println(-1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    private int a= 10;
			    public int getA() {
			        return a;
			    }
			    public void foo() {
			        switch (getA()) {
			            case 1 :
			                System.out.println(1);
			                break;
			            case 2 :
			                System.out.println(2);
			                break;
			            case 3 :
			                System.out.println(3);
			                break;
			            default :
			                System.out.println(-1);
			                break;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertIfToSwitchDoNotFixOnDifferentDiscriminant() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int a= 10;
			    public int getA() {
			        return a;
			    }
			    public void foo() {
			        if (getA() == 1) {
			            System.out.println(1);
			        } else if (this.a == 2) {
			            System.out.println(2);
			        } else if (getA() == 3) {
			            System.out.println(3);
			        } else {
			            System.out.println(-1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertIfElseToSwitch);
	}

	@Test
	public void testConvertIfToSwitchOnEnum() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			
			public class A {
			    public enum TimeUnit {
			        SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS
			    }
			    public static int getPower(TimeUnit unit) {
			        if (TimeUnit.SECONDS.equals(unit)) {
			            return 0;
			        } else if (TimeUnit.MILLISECONDS.equals(unit)) {
			            return -3;
			        } else if (TimeUnit.MICROSECONDS.equals(unit)) {
			            return -6;
			        } else if (TimeUnit.NANOSECONDS.equals(unit)) {
			            return -9;
			        } else {
			            throw new InternalError();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String expected1= """
			package pack;
			
			public class A {
			    public enum TimeUnit {
			        SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS
			    }
			    public static int getPower(TimeUnit unit) {
			        switch (unit) {
			            case SECONDS :
			                return 0;
			            case MILLISECONDS :
			                return -3;
			            case MICROSECONDS :
			                return -6;
			            case NANOSECONDS :
			                return -9;
			            default :
			                throw new InternalError();
			        }
			    }
			}
			""";

		String expected2= """
			package pack;
			
			public class A {
			    public enum TimeUnit {
			        SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS
			    }
			    public static int getPower(TimeUnit unit) {
			        if (unit == null) {
			            throw new InternalError();
			        } else {
			            switch (unit) {
			                case SECONDS :
			                    return 0;
			                case MILLISECONDS :
			                    return -3;
			                case MICROSECONDS :
			                    return -6;
			                case NANOSECONDS :
			                    return -9;
			                default :
			                    throw new InternalError();
			            }
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testConvertIfToSwitchBug392847() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=392847
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(String[] args) {
			        int n = 42;
			        if (n == args.length)
			            System.out.println();
			        else {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertIfElseToSwitch);
	}

	@Test
	public void testConvertIfToSwitchBug393147() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        if (equals("a")) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertIfElseToSwitch);
	}

	@Test
	public void testConvertIfToSwitchBug393147_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        if (this.equals("a")) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertIfElseToSwitch);
	}

	@Test
	public void testSurroundWithTemplate01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E1 {
			    public void foo() {
			        System.out.println(1);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String selection= "System.out.println(1);";
		int offset= str.indexOf(selection);

		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= Arrays.asList(new QuickTemplateProcessor().getAssists(context, null));

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 9);

		String[] expected= new String[9];

		expected[0]= """
			package test1;
			public class E1 {
			    public void foo() {
			        do {
			            System.out.println(1);
			        } while (condition);
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E1 {
			    public void foo() {
			        for (int i = 0; i < array.length; i++) {
			            System.out.println(1);
			        }
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class E1 {
			    public void foo() {
			        if (condition) {
			            System.out.println(1);
			        }
			    }
			}
			""";

		expected[3]= """
			package test1;
			public class E1 {
			    public void foo() {
			        new Runnable() {
			            public void run() {
			                System.out.println(1);
			            }
			        }
			    }
			}
			""";

		expected[4]= """
			package test1;
			public class E1 {
			    public void foo() {
			        synchronized (mutex) {
			            System.out.println(1);
			        }
			    }
			}
			""";

		expected[5]= """
			package test1;
			public class E1 {
			    public void foo() {
			        try {
			            System.out.println(1);
			        } catch (Exception e) {
			            // TODO: handle exception
			        }
			    }
			}
			""";

		expected[6]= """
			package test1;
			public class E1 {
			    public void foo() {
			        while (condition) {
			            System.out.println(1);
			        }
			    }
			}
			""";

		expected[7]= """
			package test1;
			public class E1 {
			    public void foo() {
			        try {
			            System.out.println(1);
			        } finally {
			            // TODO: handle finally clause
			        }
			    }
			}
			""";

		expected[8]= """
			package test1;
			public class E1 {
			    public void foo() {
			        lock.lock();
			        try {
			            System.out.println(1);
			        } finally {
			            lock.unlock();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSurroundWithTemplate02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String selection= "System.out.println(i);";
		int offset= str.indexOf(selection);

		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= Arrays.asList(new QuickTemplateProcessor().getAssists(context, null));

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 9);

		String[] expected= new String[9];

		expected[0]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        do {
			            System.out.println(i);
			        } while (condition);
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        for (int j = 0; j < array.length; j++) {
			            System.out.println(i);
			        }
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        if (condition) {
			            System.out.println(i);
			        }
			    }
			}
			""";

		expected[3]= """
			package test1;
			public class E1 {
			    public void foo() {
			        final int i= 10;
			        new Runnable() {
			            public void run() {
			                System.out.println(i);
			            }
			        }
			    }
			}
			""";

		expected[4]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        synchronized (mutex) {
			            System.out.println(i);
			        }
			    }
			}
			""";

		expected[5]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        try {
			            System.out.println(i);
			        } catch (Exception e) {
			            // TODO: handle exception
			        }
			    }
			}
			""";

		expected[6]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        while (condition) {
			            System.out.println(i);
			        }
			    }
			}
			""";

		expected[7]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        try {
			            System.out.println(i);
			        } finally {
			            // TODO: handle finally clause
			        }
			    }
			}
			""";

		expected[8]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        lock.lock();
			        try {
			            System.out.println(i);
			        } finally {
			            lock.unlock();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSurroundWithTemplate03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        System.out.println(i);
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String selection= "int i= 10;\n        System.out.println(i);";
		int offset= str.indexOf(selection);

		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= Arrays.asList(new QuickTemplateProcessor().getAssists(context, null));

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 9);

		String[] expected= new String[9];

		expected[0]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i;
			        do {
			            i = 10;
			            System.out.println(i);
			        } while (condition);
			        System.out.println(i);
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i;
			        for (int j = 0; j < array.length; j++) {
			            i = 10;
			            System.out.println(i);
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i;
			        if (condition) {
			            i = 10;
			            System.out.println(i);
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[3]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i;
			        new Runnable() {
			            public void run() {
			                i = 10;
			                System.out.println(i);
			            }
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[4]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i;
			        synchronized (mutex) {
			            i = 10;
			            System.out.println(i);
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[5]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i;
			        try {
			            i = 10;
			            System.out.println(i);
			        } catch (Exception e) {
			            // TODO: handle exception
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[6]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i;
			        while (condition) {
			            i = 10;
			            System.out.println(i);
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[7]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i;
			        try {
			            i = 10;
			            System.out.println(i);
			        } finally {
			            // TODO: handle finally clause
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[8]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i;
			        lock.lock();
			        try {
			            i = 10;
			            System.out.println(i);
			        } finally {
			            lock.unlock();
			        }
			        System.out.println(i);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testSurroundWithTemplate04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        System.out.println(i);
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		String selection= "System.out.println(i);";
		int offset= str.indexOf(selection);

		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= Arrays.asList(new QuickTemplateProcessor().getAssists(context, null));

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 9);

		String[] expected= new String[9];

		expected[0]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        do {
			            System.out.println(i);
			        } while (condition);
			        System.out.println(i);
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        for (int j = 0; j < array.length; j++) {
			            System.out.println(i);
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        if (condition) {
			            System.out.println(i);
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[3]= """
			package test1;
			public class E1 {
			    public void foo() {
			        final int i= 10;
			        new Runnable() {
			            public void run() {
			                System.out.println(i);
			            }
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[4]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        synchronized (mutex) {
			            System.out.println(i);
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[5]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        try {
			            System.out.println(i);
			        } catch (Exception e) {
			            // TODO: handle exception
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[6]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        while (condition) {
			            System.out.println(i);
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[7]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        try {
			            System.out.println(i);
			        } finally {
			            // TODO: handle finally clause
			        }
			        System.out.println(i);
			    }
			}
			""";

		expected[8]= """
			package test1;
			public class E1 {
			    public void foo() {
			        int i= 10;
			        lock.lock();
			        try {
			            System.out.println(i);
			        } finally {
			            lock.unlock();
			        }
			        System.out.println(i);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testPickOutStringProposals1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "Hello World";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("llo");
		int length= "llo".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "He" + "llo" + " World";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

}

	@Test
	public void testPickOutStringProposals2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "Hello World";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("Hel");
		int length= "Hel".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "Hel" + "lo World";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

	}

	@Test
	public void testPickOutStringProposals3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "Hello World";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("World");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, "Pick out selected part of String");

	}

	@Test
	public void testPickOutStringProposals4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "Hello World";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("Hello");
		int length= "Hello World".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, "Pick out selected part of String");

	}

	@Test
	public void testCombineStringsProposals1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "Hello" + " World";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("\"Hello\"");
		int length= "\"Hello\" + \"World\"".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "Hello World";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

	}

	@Test
	public void testCombineStringsProposals2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "Hello" + " " + "World";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("\"Hello\"");
		int length= "\"Hello\" + \" \" + \"World\"".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "Hello World";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

	}

	@Test
	public void testCombineStringsProposals3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "Hello World";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("\"Hello World\"");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_combineSelectedStrings);

	}

	@Test
	public void testCombineStringsProposals4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        System.out.println("Hello" + " " + "World");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("\"Hello\"");
		int length= "\"Hello\" + \" \" + \"World\"".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected= """
			package test1;
			public class E {
			    public void foo() {
			        System.out.println("Hello World");
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });

	}

	@Test
	public void testCombineStringsProposals5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "Hello" + "World" + 2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("\"Hello\" + \"World\"");
		int length= "\"Hello\" + \"World\"".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_combineSelectedStrings);

	}

	@Test
	public void testConvertToIfReturn1() throws Exception {
		// positive cases
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo1() {
			        if (a) {
			            System.out.println("1");
			            System.out.println("11");
			        }
			    }
			
			    public void foo2() {
			        bar();
			        if (b) {
			            System.out.println("2");
			            System.out.println("22");
			        }
			    }
			
			    public void foo3() {
			        if (c) {
			            if (d) {
			                System.out.println("3");
			                System.out.println("33");
			        	}
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "if (a)";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		String expected1= """
			package test1;
			public class E {
			    public void foo1() {
			        if (!a)
			            return;
			        System.out.println("1");
			        System.out.println("11");
			    }
			
			    public void foo2() {
			        bar();
			        if (b) {
			            System.out.println("2");
			            System.out.println("22");
			        }
			    }
			
			    public void foo3() {
			        if (c) {
			            if (d) {
			                System.out.println("3");
			                System.out.println("33");
			        	}
			        }
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });

		str= "if (b)";
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		String expected2= """
			package test1;
			public class E {
			    public void foo1() {
			        if (a) {
			            System.out.println("1");
			            System.out.println("11");
			        }
			    }
			
			    public void foo2() {
			        bar();
			        if (!b)
			            return;
			        System.out.println("2");
			        System.out.println("22");
			    }
			
			    public void foo3() {
			        if (c) {
			            if (d) {
			                System.out.println("3");
			                System.out.println("33");
			        	}
			        }
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected2 });

		str= "if (d)";
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		String expected3= """
			package test1;
			public class E {
			    public void foo1() {
			        if (a) {
			            System.out.println("1");
			            System.out.println("11");
			        }
			    }
			
			    public void foo2() {
			        bar();
			        if (b) {
			            System.out.println("2");
			            System.out.println("22");
			        }
			    }
			
			    public void foo3() {
			        if (c) {
			            if (!d)
			                return;
			            System.out.println("3");
			            System.out.println("33");
			        }
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected3 });
	}

	@Test
	public void testConvertToIfReturn2() throws Exception {
		// negative cases
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo1() {
			        if (true) {
			            System.out.println("1");
			            System.out.println("2");
			        }
			        bar();\
			    }
			
			    public void foo2() {
			        if (a)\s
			            if (b) {
			                System.out.println("1");
			                System.out.println("2");
			        	}
			    }
			
			    public void foo3() {
			        if (c) {
			            return;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "if (true)"; // not the last executable statement in the method
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);

		str= "if (b)"; // not present in a block
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);

		str= "if (c)"; // no other statement in 'then' part other than 'return'
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);
	}

	@Test
	public void testConvertToIfReturn3() throws Exception {
		// 'if' should be in a 'method' returning 'void'
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    static {
			        if (a) {
			            System.out.println("1");
			        }
			    }
			    public String foo1() {
			        if (b) {
			            System.out.println("1");
			            return "foo"
			        }
			    }
			
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "if (a)"; // not in a method
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);

		str= "if (b)"; // method does not return 'void'
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);
	}

	@Test
	public void testConvertToIfReturn4() throws Exception {
		// 'if' should not be in a loop
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo1() {
			        for (int i; i < 3; i++) {
			            if (a) {
			                System.out.println("1");
			        	}
			        }
			    }
			
			    public void foo2() {
			        List<String> strs= new ArrayList<String>;
			        for (String s : strs) {
			            if (b) {
			                System.out.println("2");
			        	}
			        }
			    }
			
			    public void foo3() {
			        do {
			            if (c) {
			                System.out.println("3");
			        	}
			        } while (true)
			    }
			
			    public void foo4() {
			        while (true) {
			            if (d) {
			                System.out.println("4");
			        	}
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "if (a)";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);

		str= "if (b)";
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);

		str= "if (c)";
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);

		str= "if (d)";
		context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.AdvancedQuickAssistProcessor_convertToIfReturn);
	}

	@Test
	public void testConvertParamsToFields() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String buf= """
			package test;
			
			package test16_3;
			public class App {
				private String s;
			
				public App(String s, String s3, String s2) {
				}
			}""";

		ICompilationUnit cu= pack1.createCompilationUnit("App.java", buf, false, null);

		String str= "String s3";
		AssistContext context= getCorrectionContext(cu, buf.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertProposalExists(proposals, CorrectionMessages.AssignToVariableAssistProposal_assignallparamstofields_description);
		String expected= """
			package test;
			
			package test16_3;
			public class App {
				private String s;
			    private String s4;
			    private String s3;
			    private String s2;
			
				public App(String s, String s3, String s2) {
			        s4 = s;
			        this.s3 = s3;
			        this.s2 = s2;
				}
			}""";

		assertProposalPreviewEquals(expected, CorrectionMessages.AssignToVariableAssistProposal_assignallparamstofields_description, proposals);
	}
}
