/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Those tests are made to run on Java >= 1.8 .
 */

public class LambdaQuickFixTest1d8 extends QuickFixTest {

	    @Rule
	    public ProjectTestSetup projectSetup = new Java1d8ProjectTestSetup();

	    private IJavaProject fJProject1;
	    private IPackageFragmentRoot fSourceFolder;

	    @Before
	    public void setUp() throws Exception {
	        Hashtable<String, String> options = TestOptions.getDefaultOptions();
	        options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
	        options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
	        options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "99");

	        // enable unused-variable warning (for test #3)
	        options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.WARNING);

	        JavaCore.setOptions(options);
	        JavaPlugin.getDefault().getPreferenceStore().setValue(
	                PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

	        fJProject1 = projectSetup.getProject();
	        fSourceFolder = JavaProjectHelper.addSourceContainer(fJProject1, "src");
	    }



	    // ==========================================================================================
	    // 1. UNHANDLED EXCEPTION INSIDE LAMBDA → Quick-fix exists (Surround with try/catch)
	    // ==========================================================================================
	    @Test
	    public void testUnhandledExceptionInLambda() throws Exception {

	        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

	        String source = """
	                package test;
	                public class X {
	                    public void foo() {
	                        Runnable r = () -> Thread.sleep(10);
	                    }
	                }
	                """;

	        ICompilationUnit cu = pack.createCompilationUnit("X.java", source, false, null);
	        CompilationUnit astRoot = getASTRoot(cu);

	        ArrayList<IJavaCompletionProposal> proposals = collectCorrections(cu, astRoot);

	        // this quick-fix ALWAYS exists
	        assertProposalContains(proposals, "Surround with try");
	    }

	    // ==========================================================================================
	    // 2. MISSING RETURN IN LAMBDA BLOCK → Quick fix exists ("Add return statement")
	    // ==========================================================================================
	    @Test
	    public void testMissingReturnInLambda() throws Exception {

	        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

	        String source = """
	                package test;
	                import java.util.concurrent.Callable;
	                public class X {
	                    Callable<Integer> c = () -> { }; // invalid: missing return
	                }
	                """;

	        ICompilationUnit cu = pack.createCompilationUnit("X.java", source, false, null);
	        CompilationUnit astRoot = getASTRoot(cu);

	        ArrayList<IJavaCompletionProposal> proposals = collectCorrections(cu, astRoot);

	        assertProposalContains(proposals, "Add return statement");
	    }

	    // ==========================================================================================
	    // 3. UNUSED VARIABLE INSIDE METHOD (NOT LAMBDA) → Quick fix exists
	    // ==========================================================================================
	    @Test
	    public void testUnusedVariableQuickFix() throws Exception {

	        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

	        String source = """
	                package test;
	                import java.util.function.Supplier;
	                public class X {
	                    public void foo() {
	                        Supplier<Integer> s = () -> 10; // 's' is unused
	                    }
	                }
	                """;

	        ICompilationUnit cu = pack.createCompilationUnit("X.java", source, false, null);
	        CompilationUnit astRoot = getASTRoot(cu);

	        ArrayList<IJavaCompletionProposal> proposals = collectCorrections(cu, astRoot);

	        // JDT *does* provide unused-variable fixes in tests
	        assertProposalContains(proposals, "Remove 's'");
	    }

	    // ==========================================================================================
	    // 4. UNHANDLED EXCEPTION IN EXPRESSION LAMBDA → Quick fix exists (Surround with try/catch)
	    // ==========================================================================================
	    @Test
	    public void testUnhandledExceptionInExpressionLambda() throws Exception {

	        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

	        String source = """
	                package test;
	                public class X {
	                    public void foo() {
	                        Runnable r = () -> Thread.sleep(5);
	                    }
	                }
	                """;

	        ICompilationUnit cu = pack.createCompilationUnit("X.java", source, false, null);
	        CompilationUnit astRoot = getASTRoot(cu);

	        ArrayList<IJavaCompletionProposal> proposals = collectCorrections(cu, astRoot);

	        assertProposalContains(proposals, "Surround with try");
	    }

	    // ==========================================================================================
	    // 5. MISSING RETURN IN LAMBDA BLOCK → Quick fix exists ("Add return statement")
	    // ==========================================================================================
	    @Test
	    public void testMissingReturnInConditionalLambda() throws Exception {

	        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

	        String source = """
	                package test;
	                import java.util.concurrent.Callable;
	                public class X {
	                    Callable<Integer> c = () -> {
	                        if (System.currentTimeMillis() > 0) {
	                            return 1;
	                        }
	                    };
	                }
	                """;

	        ICompilationUnit cu = pack.createCompilationUnit("X.java", source, false, null);
	        CompilationUnit astRoot = getASTRoot(cu);

	        ArrayList<IJavaCompletionProposal> proposals = collectCorrections(cu, astRoot);

	        assertProposalContains(proposals, "Add return statement");
	    }

	    // ==========================================================================================
	    // 6. MISSING RETURN IN LAMBDA BLOCK → Quick fix exists ("Add return statement")
	    // ==========================================================================================
	    @Test
	    public void testMissingReturnInMultipleBranches() throws Exception {

	        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

	        String source = """
	                package test;
	                import java.util.concurrent.Callable;
	                public class X {
	                    Callable<Integer> c = () -> {
	                        if (System.currentTimeMillis() > 0) {
	                            return 1;
	                        } else if (System.currentTimeMillis() < 0) {
	                            return 2;
	                        }
	                    };
	                }
	                """;

	        ICompilationUnit cu = pack.createCompilationUnit("X.java", source, false, null);
	        CompilationUnit astRoot = getASTRoot(cu);

	        ArrayList<IJavaCompletionProposal> proposals = collectCorrections(cu, astRoot);

	        assertProposalContains(proposals, "Add return statement");
	    }

	    // ==========================================================================================
	    // 7. MISSING RETURN IN LAMBDA BLOCK → Quick fix exists ("Add return statement")
	    // ==========================================================================================
	    @Test
	    public void testMissingReturnAfterLoop() throws Exception {

	        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

	        String source = """
	                package test;
	                import java.util.concurrent.Callable;
	                public class X {
	                    Callable<Integer> c = () -> {
	                        for (int i = 0; i < 10; i++) {
	                            if (i == 5) return i;
	                        }
	                    };
	                }
	                """;

	        ICompilationUnit cu = pack.createCompilationUnit("X.java", source, false, null);
	        CompilationUnit astRoot = getASTRoot(cu);

	        ArrayList<IJavaCompletionProposal> proposals = collectCorrections(cu, astRoot);

	        assertProposalContains(proposals, "Add return statement");
	    }

	    // ==========================================================================================
	    // 8. MISSING RETURN IN LAMBDA BLOCK → Quick fix exists ("Add return statement")
	    // ==========================================================================================
	    @Test
	    public void testMissingReturnInTryCatch() throws Exception {

	        IPackageFragment pack = fSourceFolder.createPackageFragment("test", false, null);

	        String source = """
	                package test;
	                import java.util.concurrent.Callable;
	                public class X {
	                    Callable<Integer> c = () -> {
	                        try {
	                            return Integer.parseInt("123");
	                        } catch (Exception e) {
	                        }
	                    };
	                }
	                """;

	        ICompilationUnit cu = pack.createCompilationUnit("X.java", source, false, null);
	        CompilationUnit astRoot = getASTRoot(cu);

	        ArrayList<IJavaCompletionProposal> proposals = collectCorrections(cu, astRoot);

	        assertProposalContains(proposals, "Add return statement");
	    }


}

