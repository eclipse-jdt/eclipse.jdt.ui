/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

/**
 * Those tests should run on Java Dolphin 1.7 .
 */
public class AssistQuickFixTest1d7 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java1d7ProjectTestSetup();

	private static final String REMOVE_CATCH_CLAUSE= CorrectionMessages.QuickAssistProcessor_removecatchclause_description;
	private static final String REPLACE_CATCH_CLAUSE_WITH_THROWS= CorrectionMessages.QuickAssistProcessor_catchclausetothrows_description;
	private static final String REMOVE_SURROUNDING_TRY_BLOCK= CorrectionMessages.QuickAssistProcessor_unwrap_trystatement;
	private static final String CONVERT_TO_A_SINGLE_MULTI_CATCH_BLOCK= CorrectionMessages.QuickAssistProcessor_convert_to_single_multicatch_block;
	private static final String CONVERT_TO_SEPARATE_CATCH_BLOCKS= CorrectionMessages.QuickAssistProcessor_convert_to_multiple_singletype_catch_blocks;

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
	public void testConvertToMultiCatch1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException ex) {
			            ex.printStackTrace();
			        } catch (NullPointerException ex) {
			            ex.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException | NullPointerException ex) {
			            ex.printStackTrace();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToMultiCatch2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException e) {
			            e.printStackTrace();
			        } catch (NullPointerException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException | NullPointerException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });

	}

	@Test
	public void testConvertToMultiCatch3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException | NullPointerException e) {
			            e.printStackTrace();
			        } catch (RuntimeException e) {
			            e.printStackTrace();
			        }
			        // a comment at the end
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException | NullPointerException | RuntimeException e) {
			            e.printStackTrace();
			        }
			        // a comment at the end
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testConvertToMultiCatch4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException e) {
			            e.printStackTrace();
			        } catch (NullPointerException e) {
			           \s
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, CONVERT_TO_A_SINGLE_MULTI_CATCH_BLOCK);
	}

	@Test
	public void testConvertToMultiCatch5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, CONVERT_TO_A_SINGLE_MULTI_CATCH_BLOCK);
	}

	@Test
	public void testConvertToMultiCatch6() throws Exception {
		//Quick assist should not be offered in 1.5 mode
		JavaProjectHelper.set15CompilerOptions(fJProject1);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class E {
				    void foo() {
				        try {
				            System.out.println("foo");
				        } catch (IllegalArgumentException e) {
				            e.printStackTrace();
				        } catch (NullPointerException e) {
				            e.printStackTrace();
				        }
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			int offset= str.indexOf("catch");
			AssistContext context= getCorrectionContext(cu, offset, 0);
			assertNoErrors(context);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertProposalDoesNotExist(proposals, CONVERT_TO_A_SINGLE_MULTI_CATCH_BLOCK);
		} finally {
			JavaProjectHelper.set17CompilerOptions(fJProject1);
		}
	}

	@Test
	public void testUnrollMultiCatch1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException | NullPointerException ex) {
			            ex.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException ex) {
			            ex.printStackTrace();
			        } catch (NullPointerException ex) {
			            ex.printStackTrace();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUnrollMultiCatch2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException | NullPointerException e) {
			            e.printStackTrace();
			        } catch (RuntimeException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException e) {
			            e.printStackTrace();
			        } catch (NullPointerException e) {
			            e.printStackTrace();
			        } catch (RuntimeException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUnrollMultiCatch3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException e) {
			            e.printStackTrace();
			        } catch (NullPointerException | ClassCastException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch (NullPointerException");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException e) {
			            e.printStackTrace();
			        } catch (NullPointerException e) {
			            e.printStackTrace();
			        } catch (ClassCastException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUnrollMultiCatch4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException e) {
			            e.printStackTrace();
			        } catch (NullPointerException | ClassCastException e) {
			            e.printStackTrace();
			        } catch (ArrayIndexOutOfBoundsException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch (NullPointerException");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (IllegalArgumentException e) {
			            e.printStackTrace();
			        } catch (NullPointerException e) {
			            e.printStackTrace();
			        } catch (ClassCastException e) {
			            e.printStackTrace();
			        } catch (ArrayIndexOutOfBoundsException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testUnrollMultiCatch5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (NullPointerException ex) {
			            ex.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, CONVERT_TO_SEPARATE_CATCH_BLOCKS);
	}

	@Test
	public void testUnrollMultiCatch6() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=350285#c12
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException | IllegalAccessException
			                | IllegalArgumentException | InvocationTargetException
			                | NoSuchMethodException | SecurityException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		assertNoErrors(context);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException e) {
			            e.printStackTrace();
			        } catch (IllegalAccessException e) {
			            e.printStackTrace();
			        } catch (IllegalArgumentException e) {
			            e.printStackTrace();
			        } catch (InvocationTargetException e) {
			            e.printStackTrace();
			        } catch (NoSuchMethodException e) {
			            e.printStackTrace();
			        } catch (SecurityException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testReplaceMultiCatchClauseWithThrows1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        try {
			            goo();
			        } catch (IllegalArgumentException | NullPointerException e) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("catch");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() throws IllegalArgumentException, NullPointerException {
			        goo();
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        goo();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testReplaceMultiCatchClauseWithThrows2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        try {
			            System.out.println("foo");
			        } catch (Outer<String>.Inner | NullPointerException ex) {
			            ex.printStackTrace();
			        }
			    }
			}
			class Outer<E> {
			    class Inner extends IllegalArgumentException { }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("Inner");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertProposalDoesNotExist(proposals, REMOVE_CATCH_CLAUSE);
		assertProposalDoesNotExist(proposals, REPLACE_CATCH_CLAUSE_WITH_THROWS);
	}

	@Test
	public void testReplaceMultiCatchClauseWithThrows3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        try {
			            goo();
			        } catch (IllegalArgumentException | NullPointerException e) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("IllegalArgumentException");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        try {
			            goo();
			        } catch (NullPointerException e) {
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() throws IllegalArgumentException {
			        try {
			            goo();
			        } catch (NullPointerException e) {
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        try {
			            goo();
			        } catch (NullPointerException e) {
			        } catch (IllegalArgumentException e) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testReplaceMultiCatchClauseWithThrows4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException | IllegalAccessException
			                | IllegalArgumentException | InvocationTargetException
			                | NoSuchMethodException | SecurityException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("IllegalArgumentException");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException | IllegalAccessException
			                | InvocationTargetException
			                | NoSuchMethodException | SecurityException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() throws IllegalArgumentException {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException | IllegalAccessException
			                | InvocationTargetException
			                | NoSuchMethodException | SecurityException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException | IllegalAccessException
			                | InvocationTargetException
			                | NoSuchMethodException | SecurityException e) {
			            e.printStackTrace();
			        } catch (IllegalArgumentException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testPickoutTypeFromMulticatch1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException | IllegalAccessException
			                | IllegalArgumentException | InvocationTargetException
			                | NoSuchMethodException | SecurityException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String string= "IllegalArgumentException | InvocationTargetException";
		int offset= str.indexOf(string);
		int length= string.length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        String.class.getConstructor().newInstance();
			    }
			}
			""";

		String expected2= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
			        String.class.getConstructor().newInstance();
			    }
			}
			""";

		String expected3= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException | IllegalAccessException
			                | NoSuchMethodException | SecurityException e) {
			            e.printStackTrace();
			        } catch (IllegalArgumentException | InvocationTargetException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		String expected4= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException e) {
			            e.printStackTrace();
			        } catch (IllegalAccessException e) {
			            e.printStackTrace();
			        } catch (IllegalArgumentException e) {
			            e.printStackTrace();
			        } catch (InvocationTargetException e) {
			            e.printStackTrace();
			        } catch (NoSuchMethodException e) {
			            e.printStackTrace();
			        } catch (SecurityException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testPickoutTypeFromMulticatch2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException | IllegalAccessException
			                | IllegalArgumentException | InvocationTargetException
			                | java.lang.NoSuchMethodException | SecurityException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String string= "MethodException";
		int offset= str.indexOf(string);
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException | IllegalAccessException
			                | IllegalArgumentException | InvocationTargetException
			                | SecurityException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() throws java.lang.NoSuchMethodException {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException | IllegalAccessException
			                | IllegalArgumentException | InvocationTargetException
			                | SecurityException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			import java.lang.reflect.InvocationTargetException;
			public class E {
			    public void foo() {
			        try {
			            String.class.getConstructor().newInstance();
			        } catch (InstantiationException | IllegalAccessException
			                | IllegalArgumentException | InvocationTargetException
			                | SecurityException e) {
			            e.printStackTrace();
			        } catch (java.lang.NoSuchMethodException e) {
			            e.printStackTrace();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testSplitDeclaration1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    void foo() throws Exception {
			        try (FileReader reader = new FileReader("file")) {
			            int ch;
			            while ((ch = reader.read()) != -1) {
			                System.out.println(ch);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "reader";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		assertProposalDoesNotExist(proposals, "Split variable declaration");
	}

	@Test
	public void testUnwrapTryStatement() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import java.io.FileReader;
			public class E {
			    void foo() throws Exception {
			        try (FileReader reader1 = new FileReader("file")) {
			            int ch;
			            while ((ch = reader1.read()) != -1) {
			                System.out.println(ch);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "try";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, REMOVE_SURROUNDING_TRY_BLOCK);
	}

	@Test
	public void testExtractLocalInTryWithResource1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import java.io.BufferedReader;
			import java.io.FileReader;
			import java.io.Reader;
			public class E {
			    void foo() throws Exception {
			        try (Reader s = new BufferedReader(new FileReader("c.d"));
			                Reader r = new BufferedReader(new FileReader("a.b"))) {
			            r.read();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "new FileReader(\"a.b\")";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		String expected= """
			package test1;
			import java.io.BufferedReader;
			import java.io.FileReader;
			import java.io.Reader;
			public class E {
			    void foo() throws Exception {
			        try (Reader s = new BufferedReader(new FileReader("c.d"));
			                FileReader fileReader = new FileReader("a.b");
			                Reader r = new BufferedReader(fileReader)) {
			            r.read();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testExtractLocalInTryWithResource2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import java.io.BufferedReader;
			import java.io.FileReader;
			import java.io.Reader;
			public class E {
			    void foo() throws Exception {
			        try (Reader s = new BufferedReader(new FileReader("c.d"));
			                Reader r = new BufferedReader(new FileReader("a.b"))) {
			            r.read();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "\"a.b\"";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		String expected= """
			package test1;
			import java.io.BufferedReader;
			import java.io.FileReader;
			import java.io.Reader;
			public class E {
			    void foo() throws Exception {
			        String string = "a.b";
			        try (Reader s = new BufferedReader(new FileReader("c.d"));
			                Reader r = new BufferedReader(new FileReader(string))) {
			            r.read();
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected });
	}

	@Test
	public void testInferDiamondArguments() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import java.util.HashMap;
			import java.util.Map;
			public class E {
			    public void foo() {
			        Map<String, ? extends Number> m = new HashMap<>(12);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "<>";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import java.util.HashMap;
			import java.util.Map;
			public class E {
			    public void foo() {
			        Map<String, ? extends Number> m = new HashMap<String, Number>(12);
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

}
