/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
import org.eclipse.jdt.ui.tests.core.rules.Java1d7ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

/**
 * Those tests should run on Java Dolphin 1.7 .
 */
public class AdvancedQuickAssistTest1d7 extends QuickFixTest {
	@Rule
    public ProjectTestSetup projectSetup= new Java1d7ProjectTestSetup();

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
	public void testConvertSwitchToIf() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(String s) {
			        switch (s) {
			        case "abc":
			            System.out.println();
			            break;
			        case "xyz":
			            System.out.println();
			            break;
			        default:
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

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
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

		String expected2= """
			package test1;
			public class E {
			    public void foo(String s) {
			        if (s.equals("abc")) {
			            System.out.println();
			        } else if (s.equals("xyz")) {
			            System.out.println();
			        } else {
			            System.out.println();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testConvertIfToSwitch1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(String s) {
			        if (s.equals("abc")) {
			            System.out.println();
			        } else if (s.equals("xyz")) {
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

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(String s) {
			        switch (s) {
			            case "abc" :
			                System.out.println();
			                break;
			            case "xyz" :
			                System.out.println();
			                break;
			            default :
			                System.out.println();
			                break;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertIfToSwitch2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(String s) {
			        if ("abc" == s) {
			            System.out.println();
			        } else if ("xyz" == s) {
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
	public void testConvertIfToSwitch3() throws Exception {
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

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(String s) {
			        switch (s) {
			            case "abc" :
			                System.out.println();
			                break;
			            case "xyz" :
			                System.out.println();
			                break;
			            default :
			                System.out.println();
			                break;
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo(String s) {
			        if (s == null) {
			            System.out.println();
			        } else {
			            switch (s) {
			                case "abc" :
			                    System.out.println();
			                    break;
			                case "xyz" :
			                    System.out.println();
			                    break;
			                default :
			                    System.out.println();
			                    break;
			            }
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testConvertIfToSwitch4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(String s) {
			        if (s.equals("abc")) {
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

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(String s) {
			        switch (s) {
			            case "abc" :
			                System.out.println();
			                break;
			            case "xyz" :
			                System.out.println();
			                break;
			            default :
			                System.out.println();
			                break;
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo(String s) {
			        if (s == null) {
			            System.out.println();
			        } else {
			            switch (s) {
			                case "abc" :
			                    System.out.println();
			                    break;
			                case "xyz" :
			                    System.out.println();
			                    break;
			                default :
			                    System.out.println();
			                    break;
			            }
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testConvertIfToSwitch5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(String s) {
			        if ("abc".equals(s)) {
			            System.out.println();
			        } else if ("xyz".equals(s)) {
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

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo(String s) {
			        switch (s) {
			            case "abc" :
			                System.out.println();
			                break;
			            case "xyz" :
			                System.out.println();
			                break;
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo(String s) {
			        if (s == null) {
			        } else {
			            switch (s) {
			                case "abc" :
			                    System.out.println();
			                    break;
			                case "xyz" :
			                    System.out.println();
			                    break;
			            }
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
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
			    List<String> foo(int a) {
			        return a > 0 ? new ArrayList<>() : new ArrayList<>();
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
			    List<String> foo(int a) {
			        if (a > 0)
			            return new ArrayList<>();
			        else
			            return new ArrayList<>();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testReplaceReturnIfWithCondition3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public List<String> foo(int a) {
			        if (a > 0) {
			            return new ArrayList<>();
			        } else {
			            return new ArrayList<>();
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
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    public List<String> foo(int a) {
			        return a > 0 ? new ArrayList<String>() : new ArrayList<String>();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testReplaceReturnIfWithCondition4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collections;
			import java.util.Map;
			public class E {
			    public Map<String, java.io.IOException> foo(int a) {
			        if (a > 0) {
			            return Collections.emptyMap();
			        } else {
			            return Collections.singletonMap("none", null);
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
			import java.io.IOException;
			import java.util.Collections;
			import java.util.Map;
			public class E {
			    public Map<String, java.io.IOException> foo(int a) {
			        return a > 0 ? Collections.<String, IOException>emptyMap() : Collections.<String, IOException>singletonMap("none", null);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testConvertIfToSwitch() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static boolean isOdd(String number) {
			        if (number.equals("one") || number.equals("three") || number.equals("five") || number.equals("nine")) {
			            return true;
			        } else {
			            return false;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("if");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public static boolean isOdd(String number) {
			        switch (number) {
			            case "one" :
			            case "three" :
			            case "five" :
			            case "nine" :
			                return true;
			            default :
			                return false;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

}
