/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Benjamin Muskalla <b.muskalla@gmx.net> - [surround with] "Surround With runnable" crash - https://bugs.eclipse.org/bugs/show_bug.cgi?id=238226
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.text.templates.TemplatePersistenceData;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

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

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.QuickTemplateProcessor;

public class SurroundWithTemplateTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

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

	private static List<IJavaCompletionProposal> getRunnableProposal(AssistContext context) throws CoreException {

		String str= """
			Runnable runnable = new Runnable() {
			    public void run() {
			        ${line_selection}
			    }
			};""";
		TemplateStore templateStore= JavaPlugin.getDefault().getTemplateStore();
		for (TemplatePersistenceData t : templateStore.getTemplateData(false)) {
			templateStore.delete(t);
		}
		TemplatePersistenceData surroundWithRunnableTemplate= new TemplatePersistenceData(new Template("runnable", "surround with runnable", "java", str, false), true);
		templateStore.add(surroundWithRunnableTemplate);

		IJavaCompletionProposal[] templateProposals= (new QuickTemplateProcessor()).getAssists(context, null);
		if (templateProposals == null || templateProposals.length != 1)
			return Collections.EMPTY_LIST;

		return Arrays.asList(templateProposals);
	}

	@Test
	public void testSurroundWithRunnable1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        System.out.println(1);
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(1);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(1);
			            }
			        };
			    }
			}
			
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int i = 10;
			        final int j = 10;
			        System.out.println(i);
			        System.out.println(j);
			        int k = 10;
			        k++;
			        System.out.println(k);
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(i);
			        System.out.println(j);
			        int k = 10;
			        k++;
			        System.out.println(k);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        final int i = 10;
			        final int j = 10;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(i);
			                System.out.println(j);
			                int k = 10;
			                k++;
			                System.out.println(k);
			            }
			        };
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int i = 10;
			        int k = 10;
			        k++;
			        int h = 10;
			        int j = 10;
			        j++;
			        System.out.println(k);
			        System.out.println(j);
			        System.out.println(i);
			        System.out.println(h);
			        i++;
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        j++;
			        System.out.println(k);
			        System.out.println(j);
			        System.out.println(i);
			        System.out.println(h);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        final int i = 10;
			        final int k = 10;
			        k++;
			        final int h = 10;
			        final int j = 10;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                j++;
			                System.out.println(k);
			                System.out.println(j);
			                System.out.println(i);
			                System.out.println(h);
			            }
			        };
			        i++;
			    }
			}
			
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable4() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int j = 10;
			        while (j > 0) {
			            System.out.println(j);
			            j--;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			            System.out.println(j);
			            j--;
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        final int j = 10;
			        while (j > 0) {
			            Runnable runnable = new Runnable() {
			                public void run() {
			                    System.out.println(j);
			                    j--;
			                }
			            };
			        }
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable5() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int i = 10;
			        System.out.println(i);
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        int i = 10;
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        int i;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                i = 10;
			            }
			        };
			        System.out.println(i);
			    }
			}
			
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable6() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        /***/ int i = 10;
			        System.out.println(i);
			        System.out.println(i);
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        /***/ int i = 10;
			        System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        /***/ int i;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                i = 10;
			                System.out.println(i);
			            }
			        };
			        System.out.println(i);
			    }
			}
			
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable7() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        /***/ int i = 10;
			        System.out.println(i);
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        /***/ final int i = 10;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(i);
			            }
			        };
			    }
			}
			
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable8() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        //TextTextText
			       \s
			        //TextTextText
			        //
			        //TextTextText
			        /***/ int i = 10;
			        System.out.println(i);
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			int i = 10;
			        System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        //TextTextText
			       \s
			        Runnable runnable = new Runnable() {
			            public void run() {
			                //TextTextText
			                //
			                //TextTextText
			                /***/
			                int i = 10;
			                System.out.println(i);
			            }
			        };
			    }
			}
			
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable9() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        /***/ int i = 10;
			        System.out.println(i);
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			int i = 10;
			        System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        /***/ int i;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                i = 10;
			                System.out.println(i);
			            }
			        };
			        System.out.println(i);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable10() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int i = 10;
			        int j;
			        System.out.println(10);
			        j = 10;
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        int i = 10;
			        int j;
			        System.out.println(10);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        int j;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                int i = 10;
			                System.out.println(10);
			            }
			        };
			        j = 10;
			    }
			}
			
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable11() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int i;
			        System.out.println(i);
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        int i;
			        System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        final int i;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(i);
			            }
			        };
			        System.out.println(i);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable12() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i, String s) {
			        System.out.println(i);
			        System.out.println(s);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(i);
			        System.out.println(s);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo(final int i, final String s) {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(i);
			                System.out.println(s);
			            }
			        };
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable13() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i, String s) {
			        i = 10;
			        System.out.println(i);
			        System.out.println(s);
			        s = "";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(i);
			        System.out.println(s);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo(final int i, final String s) {
			        i = 10;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(i);
			                System.out.println(s);
			            }
			        };
			        s = "";
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable14() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int j,i = 10;
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        int j;
			        final int i = 10;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(i);
			            }
			        };
			        System.out.println(j);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable15() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int j,i = 10;
			        System.out.println(i);
			        j = 10;
			        j++;
			        System.out.println(j);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        int j;
			        final int i = 10;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(i);
			            }
			        };
			        j = 10;
			        j++;
			        System.out.println(j);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable16() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int j, i = 10;
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        int j, i = 10;
			        System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        int j;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                int i = 10;
			                System.out.println(i);
			            }
			        };
			        System.out.println(j);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable17() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int i = 10, j = i;
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        final int i = 10;
			        int j = i;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(i);
			            }
			        };
			        System.out.println(j);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable18() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int i = 10, j = i;
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(i);
			        System.out.println(j);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        final int i = 10, j = i;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(i);
			                System.out.println(j);
			            }
			        };
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable19() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int i = 10, k = i, j = k;
			        System.out.println(k);
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(k);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        int i = 10;
			        final int k = i;
			        int j = k;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(k);
			            }
			        };
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable20() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int i = 10, j = 10;
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        int i = 10, j = 10;
			        System.out.println(i);
			        System.out.println(j);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                int i = 10, j = 10;
			                System.out.println(i);
			                System.out.println(j);
			            }
			        };
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable21() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            int j = 10;
			            System.out.println(j);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			            int j = 10;
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            int j;
			            Runnable runnable = new Runnable() {
			                public void run() {
			                    j = 10;
			                }
			            };
			            System.out.println(j);
			        }
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable22() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            int j = 10;
			            while (j == 10) {
			                System.out.println(j);
			                j--;
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			            while (j == 10) {
			                System.out.println(j);
			                j--;
			            }
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            final int j = 10;
			            Runnable runnable = new Runnable() {
			                public void run() {
			                    while (j == 10) {
			                        System.out.println(j);
			                        j--;
			                    }
			                }
			            };
			        }
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable23() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    int i= 9;
			    {
			        /***/ int k = 10;
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        /***/ int k = 10;
			        System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    int i= 9;
			    {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                /***/
			                int k = 10;
			                System.out.println(i);
			            }
			        };
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable24() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int k = 0, v = 0;
			        {
			            System.out.println(v);
			            System.out.println(k);
			        }
			        k++;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			            System.out.println(v);
			            System.out.println(k);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        final int k = 0, v = 0;
			        {
			            Runnable runnable = new Runnable() {
			                public void run() {
			                    System.out.println(v);
			                    System.out.println(k);
			                }
			            };
			        }
			        k++;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}
	/*
	@Test
	public void testSurroundWithRunnable25() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int y = 1;\n");
		buf.append("        switch (y) {\n");
		buf.append("        case 1:\n");
		buf.append("            int e4 = 9, e5 = 0;\n");
		buf.append("            System.out.println(e4);\n");
		buf.append("            e5++;\n");
		buf.append("        default:\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuffer selection= new StringBuffer();
		selection.append("            System.out.println(e4);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int y = 1;\n");
		expected1.append("        switch (y) {\n");
		expected1.append("        case 1:\n");
		expected1.append("            final int e4 = 9;\n");
		expected1.append("                int e5 = 0;\n");
		expected1.append("            Runnable runnable = new Runnable() {\n");
		expected1.append("                    public void run() {\n");
		expected1.append("                        System.out.println(e4);\n");
		expected1.append("                    }\n");
		expected1.append("                };\n");
		expected1.append("                e5++;\n");
		expected1.append("        default:\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}

	@Test
	public void testSurroundWithRunnable26() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int y = 1;\n");
		buf.append("        switch (y) {\n");
		buf.append("        case 1:\n");
		buf.append("            int e4 = 9, e5 = 0;\n");
		buf.append("            System.out.println(e4);\n");
		buf.append("            e5++;\n");
		buf.append("        default:\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		StringBuffer selection= new StringBuffer();
		selection.append("            int e4 = 9, e5 = 0;\n");
		selection.append("            System.out.println(e4);\n");

		AssistContext context= getCorrectionContext(cu, buf.toString().indexOf(selection.toString()), selection.toString().length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 9);
		assertCorrectLabels(proposals);

		StringBuffer expected1= new StringBuffer();
		expected1.append("package test1;\n");
		expected1.append("public class E {\n");
		expected1.append("    public void foo() {\n");
		expected1.append("        int y = 1;\n");
		expected1.append("        switch (y) {\n");
		expected1.append("        case 1:\n");
		expected1.append("            int e5;\n");
		expected1.append("                Runnable runnable = new Runnable() {\n");
		expected1.append("                    public void run() {\n");
		expected1.append("                        int e4 = 9;\n");
		expected1.append("                        e5 = 0;\n");
		expected1.append("                        System.out.println(e4);\n");
		expected1.append("                    }\n");
		expected1.append("                };\n");
		expected1.append("                e5++;\n");
		expected1.append("        default:\n");
		expected1.append("        }\n");
		expected1.append("    }\n");
		expected1.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {expected1.toString()});
	}*/

	@Test
	public void testSurroundWithRunnable27() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        String s = "", c = "";
			        System.out.println(s);
			        c = "";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(s);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        final String s = "";
			        String c = "";
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(s);
			            }
			        };
			        c = "";
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable28() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        int i = 10, j, k, v;
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(v);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        int i = 10, j, k, v;
			        System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        int j;
			        int v;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                int i = 10;
			                int k;
			                System.out.println(i);
			            }
			        };
			        System.out.println(j);
			        System.out.println(v);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable29() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        @SuppressWarnings("nls") String s= "", k = "";
			        System.out.println(s);
			        System.out.println(k);
			        k="";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        @SuppressWarnings("nls") String s= "", k = "";
			        System.out.println(s);
			        System.out.println(k);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        @SuppressWarnings("nls")
			        String k;
			        Runnable runnable = new Runnable() {
			            public void run() {
			                @SuppressWarnings("nls")
			                String s = "";
			                k = "";
			                System.out.println(s);
			                System.out.println(k);
			            }
			        };
			        k="";
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnable30() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    public void foo() {
			        if (true)\s
			            System.out.println(1);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			            System.out.println(1);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test;
			public class E {
			    public void foo() {
			        if (true) {
			            Runnable runnable = new Runnable() {
			                public void run() {
			                    System.out.println(1);
			                }
			            };
			        }
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnableBug133560() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        for (int i = 0; i < 10; i++) {
			            System.out.println(i);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			            System.out.println(i);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        for (final int i = 0; i < 10; i++) {
			            Runnable runnable = new Runnable() {
			                public void run() {
			                    System.out.println(i);
			                }
			            };
			        }
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnableBug233278() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			  {
			    final int x = 0, y = 1;
			    new Object() {
			      void method() {
			        if (x == y)
			          return;
			        toString();
			      }
			    };
			  }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        if (x == y)
			          return;
			        toString();
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			  {
			    final int x = 0, y = 1;
			    new Object() {
			      void method() {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                if (x == y)
			                    return;
			                toString();
			            }
			        };
			      }
			    };
			  }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithRunnableBug138323() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E<I> {
			    public void foo() {
			        System.out.println(this);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			        System.out.println(this);
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E<I> {
			    public void foo() {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                System.out.println(E.this);
			            }
			        };
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

	@Test
	public void testSurroundWithBug162549() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void m() {
			        if (true) {
			            System.out.println("T");
			        } // else {
			        // System.out.println("F");
			        // }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			if (true) {
			            System.out.println("T");
			        } // else {
			        // System.out.println("F");
			        // }
			""";
		AssistContext context= getCorrectionContext(cu, str.indexOf(str1), str1.length());
		List<IJavaCompletionProposal> proposals= getRunnableProposal(context);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			public class E {
			    void m() {
			        Runnable runnable = new Runnable() {
			            public void run() {
			                if (true) {
			                    System.out.println("T");
			                } // else {
			                  // System.out.println("F");
			                  // }
			            }
			        };
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str2});
	}

}
