/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
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
import org.eclipse.jdt.ui.tests.core.rules.Java10ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Those tests are made to run on Java 10..
 */
public class LocalCorrectionsQuickFixTest10 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup= new Java10ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_HASHCODE_METHOD, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_TYPE_ARGUMENTS, JavaCore.WARNING);

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
    public void testTypeParametersToRawTypeReferenceIssue765() throws Exception {
            Hashtable<String, String> options= JavaCore.getOptions();
            options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
            options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
            JavaCore.setOptions(options);

            IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

            String str= """
				package test1;
				import java.util.List;
				
				class E1 {
				    void f() {
				        var list = T.forClass(List.class);
				        doSomethingWith(list.get()); // (1)
				    }
				    @SuppressWarnings("unused")
				    void doSomethingWith(List<Object> list) {}
				    static class T<A> {
				        static <O> T<O> forClass(@SuppressWarnings("unused") Class<O> clazz) { return null; }
				        A get() { return null; }
				    }
				}
				""";
            ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

            CompilationUnit astRoot= getASTRoot(cu);
            ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
    		assertNumberOfProposals(proposals, 2);

    		assertProposalDoesNotExist(proposals, "Add type arguments to 'var'");
    		assertProposalDoesNotExist(proposals, "Infer Generic Type Arguments...");
    		assertProposalExists(proposals, "Configure problem severity");
    		assertProposalExists(proposals, "Add @SuppressWarnings 'unchecked' to 'f()'");
    }

	@Test
    public void testTypeParametersToRawTypeReferenceIssue774() throws Exception {
            Hashtable<String, String> options= JavaCore.getOptions();
            options.put(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE, JavaCore.WARNING);
            options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.WARNING);
            JavaCore.setOptions(options);

            IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

            String str= """
				package test1;
				import java.util.List;
				import java.util.ArrayList;
				
				class E1 {
				    void f() {
				        var list = new ArrayList(); // (1)
				        doSomethingWith(list);      // (2)
				    }
				    void doSomethingWith(List<String> list) {}
				}
				""";
            ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

            CompilationUnit astRoot= getASTRoot(cu);
            ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
    		assertNumberOfProposals(proposals, 4);

    		assertProposalDoesNotExist(proposals, "Add type arguments to 'var'");
    		assertProposalDoesNotExist(proposals, "Infer Generic Type Arguments...");
    		assertProposalExists(proposals, "Configure problem severity");
    		assertProposalExists(proposals, "Add @SuppressWarnings 'rawtypes' to 'f()'");
    }

	@Test
	public void testMapOfToOfEntries() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Map;
			public class A {
			    Map<String, String> myMap = Map.of("k1", "v1",
			        "k2", "v2",
			        "k3", "v3",
			        "k4", "v4",
			        "k5", "v5",
			        "k6", "v6",
			        "k7", "v7",
			        "k8", "v8",
			        "k9", "v9",
			        "k10", "v10",
			        "k11", "v11");
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String[] expected= new String[2];

		expected[0]= """
				package test1;
				import java.util.Map;
				public class A {
				    Map<String, String> myMap = Map.of();
				}
				""";

		expected[1] = """
				package test1;
				import java.util.Map;
				public class A {
				    Map<String, String> myMap = Map.ofEntries(Map.entry("k1", "v1"),
				        Map.entry("k2", "v2"),
				        Map.entry("k3", "v3"),
				        Map.entry("k4", "v4"),
				        Map.entry("k5", "v5"),
				        Map.entry("k6", "v6"),
				        Map.entry("k7", "v7"),
				        Map.entry("k8", "v8"),
				        Map.entry("k9", "v9"),
				        Map.entry("k10", "v10"),
				        Map.entry("k11", "v11"));
				}
				""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMapOfToOfEntriesWithMethodCall() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
				package test1;
				import java.util.Map;
				public class A {
				    Map<String, String> myMap = Map.of(id("k1"), "v1",
				        "k2", id("v2"),
				        "k3", "v3",
				        "k4", "v4",
				        "k5", "v5",
				        "k6", "v6",
				        "k7", "v7",
				        "k8", "v8",
				        "k9", "v9",
				        "k10", "v10",
				        "k11", "v11");
				    static String id(String s) {
				        return s;
				    }
				}
				""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String[] expected= new String[2];

		expected[0]= """
				package test1;
				import java.util.Map;
				public class A {
				    Map<String, String> myMap = Map.of();
				    static String id(String s) {
				        return s;
				    }
				}
				""";

		expected[1] = """
				package test1;
				import java.util.Map;
				public class A {
				    Map<String, String> myMap = Map.ofEntries(Map.entry(id("k1"), "v1"),
				        Map.entry("k2", id("v2")),
				        Map.entry("k3", "v3"),
				        Map.entry("k4", "v4"),
				        Map.entry("k5", "v5"),
				        Map.entry("k6", "v6"),
				        Map.entry("k7", "v7"),
				        Map.entry("k8", "v8"),
				        Map.entry("k9", "v9"),
				        Map.entry("k10", "v10"),
				        Map.entry("k11", "v11"));
				    static String id(String s) {
				        return s;
				    }
				}
				""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMapOfToOfEntriesSkippedWithOddNumberOfParameters() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
				package test1;
				import java.util.Map;
				public class A {
				    Map<String, String> myMap = Map.of("k1", "v1",
				        "k2", "v2",
				        "k3", "v3",
				        "k4", "v4",
				        "k5", "v5",
				        "k6", "v6",
				        "k7", "v7",
				        "k8", "v8",
				        "k9", "v9",
				        "k10", "v10",
				        "k11", "v11",
				        "k12");
				}
				""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String[] expected= new String[1];

		expected[0]= """
				package test1;
				import java.util.Map;
				public class A {
				    Map<String, String> myMap = Map.of();
				}
				""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMapOfToOfEntriesSkippedWithWrongMethodCall() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
				package test1;
				import java.util.Map;
				public class A {
				    Map<String, String> myMap = Map.entry("k1", "v1",
				        "k2", "v2",
				        "k3", "v3",
				        "k4", "v4",
				        "k5", "v5",
				        "k6", "v6",
				        "k7", "v7",
				        "k8", "v8",
				        "k9", "v9",
				        "k10", "v10",
				        "k11", "v11");
				}
				""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 0);
	}

}
