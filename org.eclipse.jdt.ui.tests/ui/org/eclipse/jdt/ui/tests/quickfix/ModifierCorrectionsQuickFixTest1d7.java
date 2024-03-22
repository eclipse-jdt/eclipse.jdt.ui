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
import org.eclipse.jdt.ui.tests.core.rules.Java1d7ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

/**
 * Those tests should run on Java Dolphin 1.7 .
 */
public class ModifierCorrectionsQuickFixTest1d7 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup= new Java1d7ProjectTestSetup();

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
			    public static <T> List<T> asList(T ... a) {
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
			    public static <T> List<T> asList(T ... a) {
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
			public class E {
			    public final <T> List<T> asList(T ... a) {
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
			    public final <T> List<T> asList(T ... a) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAddSafeVarargs3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.List;
			public class E {
			    @Deprecated
			    public static <T> List<T> asList(T ... a) {
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
			    @Deprecated
			    public static <T> List<T> asList(T ... a) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAddSafeVarargs4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			public class E {
			    public <T> E(T ... a) {
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
			public class E {
			    @SafeVarargs
			    public <T> E(T ... a) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAddSafeVarargs5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.List;
			public class E {
			    public <T> List<T> asList(T ... a) {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		assertProposalDoesNotExist(proposals, CorrectionMessages.VarargsWarningsSubProcessor_add_safevarargs_label);
	}

	@Test
	public void testAddSafeVarargsToDeclaration1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.List;
			public class E {
			    void foo() {
			        Y.asList(Y.asList("Hello", " World"));
			    }
			}
			class Y {
			    public static <T> List<T> asList(T... a) {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			import java.util.List;
			public class E {
			    void foo() {
			        Y.asList(Y.asList("Hello", " World"));
			    }
			}
			class Y {
			    @SafeVarargs
			    public static <T> List<T> asList(T... a) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAddSafeVarargsToDeclaration2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.List;
			public class E {
			    void foo() {
			        Y.asList(Y.asList("Hello", " World"));
			    }
			}
			class Y {
			    @Deprecated
			    public static <T> List<T> asList(T... a) {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			import java.util.List;
			public class E {
			    void foo() {
			        Y.asList(Y.asList("Hello", " World"));
			    }
			}
			class Y {
			    @SafeVarargs
			    @Deprecated
			    public static <T> List<T> asList(T... a) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAddSafeVarargsToDeclaration3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			public class E {
			    void foo() {
			        Y.asList(Y.asList("Hello", " World"));
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package p;
			import java.util.List;
			class Y {
			    public static <T> List<T> asList(T... a) {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Y.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			import java.util.List;
			class Y {
			    @SafeVarargs
			    public static <T> List<T> asList(T... a) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAddSafeVarargsToDeclaration4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.List;
			public class E {
			    void foo() {
			        new Y(Y.asList("Hello", " World"));
			    }
			}
			class Y {
			    @SafeVarargs
			    public static <T> List<T> asList(T... a) {
			        return null;
			    }
			    public <T> Y(T ... a) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			import java.util.List;
			public class E {
			    void foo() {
			        new Y(Y.asList("Hello", " World"));
			    }
			}
			class Y {
			    @SafeVarargs
			    public static <T> List<T> asList(T... a) {
			        return null;
			    }
			    @SafeVarargs
			    public <T> Y(T ... a) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAddSafeVarargsToDeclaration5() throws Exception {
		JavaProjectHelper.set15CompilerOptions(fJProject1);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
			String str= """
				package p;
				import java.util.List;
				public class E {
				    void foo() {
				        Y.asList(Y.asList("Hello", " World"));
				    }
				}
				class Y {
				    public static <T> List<T> asList(T... a) {
				        return null;
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 2);

			assertProposalDoesNotExist(proposals, "Add @SafeVarargs to 'asList(..)'");
		} finally {
			JavaProjectHelper.set17CompilerOptions(fJProject1);
		}
	}

	@Test
	public void testRemoveSafeVarargs1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.List;
			public class E {
			    @SafeVarargs
			    public static <T> List<T> asList() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			import java.util.List;
			public class E {
			    public static <T> List<T> asList() {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testRemoveSafeVarargs2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.List;
			public class E {
			    @SafeVarargs
			    public <T> List<T> asList2(T... a) {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			import java.util.List;
			public class E {
			    public <T> List<T> asList2(T... a) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testRemoveSafeVarargs3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			import java.util.List;
			public class E {
			    @SafeVarargs
			    @Deprecated
			    public <T> List<T> asList2(T... a) {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			import java.util.List;
			public class E {
			    @Deprecated
			    public <T> List<T> asList2(T... a) {
			        return null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

}
