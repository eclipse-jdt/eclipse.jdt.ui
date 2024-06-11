/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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
import java.util.Hashtable;

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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java9ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ModifierCorrectionsQuickFixTest9 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java9ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.DO_NOT_INSERT);


		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testAddSafeVarargs1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.List;
			public class E {
			    private <T> List<T> asList(T ... a) {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			import java.util.List;
			public class E {
			    @SafeVarargs
			    private <T> List<T> asList(T ... a) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAddSafeVarargs2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.List;
			public interface E {
			    private <T> List<T> asList(T ... a) {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			import java.util.List;
			public interface E {
			    @SafeVarargs
			    private <T> List<T> asList(T ... a) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	// Bug 530600 - [9][quick fix] should handle new variants of IProblem.OverridingDeprecatedMethod
	// - since
	@Test
	public void testMethodOverrideDeprecated1() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    @Deprecated(since="3")
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     */
			    public void foo() {
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			public class E {
			    @Deprecated(since="3")
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    public void foo() {
			    }
			}
			
			""";

		expected[1]= """
			package pack;
			public class E {
			    @Deprecated(since="3")
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     */
			    @SuppressWarnings("deprecation")
			    public void foo() {
			    }
			}
			
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	// Bug 530600 - [9][quick fix] should handle new variants of IProblem.OverridingDeprecatedMethod
	// - forRemoval
	@Test
	public void testMethodOverrideDeprecated2() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    @Deprecated(forRemoval=true)
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     */
			    public void foo() {
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			public class E {
			    @Deprecated(forRemoval=true)
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    public void foo() {
			    }
			}
			
			""";

		expected[1]= """
			package pack;
			public class E {
			    @Deprecated(forRemoval=true)
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     */
			    @SuppressWarnings("removal")
			    public void foo() {
			    }
			}
			
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	// Bug 530600 - [9][quick fix] should handle new variants of IProblem.OverridingDeprecatedMethod
	// - since and forRemoval
	@Test
	public void testMethodOverrideDeprecated3() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    @Deprecated(since="4.2",forRemoval=true)
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     */
			    public void foo() {
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			public class E {
			    @Deprecated(since="4.2",forRemoval=true)
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     * @deprecated
			     */
			    @Deprecated
			    public void foo() {
			    }
			}
			
			""";

		expected[1]= """
			package pack;
			public class E {
			    @Deprecated(since="4.2",forRemoval=true)
			    public void foo() {
			    }
			}   \s
			
			class F extends E {
			    /**
			     */
			    @SuppressWarnings("removal")
			    public void foo() {
			    }
			}
			
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

}
