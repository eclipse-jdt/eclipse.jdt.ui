/*******************************************************************************
 * Copyright (c) 2012, 2020 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *     IBM Corporation - bug fixes
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.IEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring.MultiFixTarget;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMarkerResolutionGenerator;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMarkerResolutionGenerator.CorrectionMarkerResolution;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreatePackageInfoWithDefaultNullnessProposal;

public class NullAnnotationsQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private String ANNOTATION_JAR_PATH;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_HASHCODE_METHOD, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_NULL_SPECIFICATION_VIOLATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fJProject1= projectSetup.getProject();

		if (ANNOTATION_JAR_PATH == null) {
			String version= "[1.1.0,2.0.0)"; // tests run at 1.5, need the "old" null annotations
			Bundle[] bundles= Platform.getBundles("org.eclipse.jdt.annotation", version);
			File bundleFile= FileLocator.getBundleFileLocation(bundles[0]).get();
			if (bundleFile.isDirectory())
				ANNOTATION_JAR_PATH= bundleFile.getPath() + "/bin";
			else
				ANNOTATION_JAR_PATH= bundleFile.getPath();
		}
		JavaProjectHelper.addLibrary(fJProject1, new Path(ANNOTATION_JAR_PATH));

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	// ==== Problem:	dereferencing a @Nullable field
	// ==== Fix:		extract field access to a fresh local variable and add a null-check

	// basic case
	@Test
	public void testExtractNullableField1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String f;
			    public void foo() {
			        System.out.println(f.toUpperCase());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String f;
			    public void foo() {
			        final String f2 = f;
			        if (f2 != null) {
			            System.out.println(f2.toUpperCase());
			        } else {
			            // TODO handle null value
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// statement is not element of a block - need to create a new block - local name f2 already in use
	@Test
	public void testExtractNullableField2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String f;
			    public void foo(boolean b) {
			        @SuppressWarnings("unused") boolean f2 = false;
			        if (b)
			          System.out.println(f.toUpperCase());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String f;
			    public void foo(boolean b) {
			        @SuppressWarnings("unused") boolean f2 = false;
			        if (b) {
			            final String f3 = f;
			            if (f3 != null) {
			                System.out.println(f3.toUpperCase());
			            } else {
			                // TODO handle null value
			            }
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// field name is part of a qualified field reference - inside a return statement (type: int)
	@Test
	public void testExtractNullableField3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable E other;
			    int f;
			    public int foo(E that) {
			        return that.other.f;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable E other;
			    int f;
			    public int foo(E that) {
			        final E other2 = that.other;
			        if (other2 != null) {
			            return other2.f;
			        } else {
			            // TODO handle null value
			            return 0;
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// field name is part of a this-qualified field reference - inside a return statement (type: String)
	@Test
	public void testExtractNullableField4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable E other;
			    @Nullable String f;
			    public String foo() {
			        return this.other.f;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable E other;
			    @Nullable String f;
			    public String foo() {
			        final E other2 = this.other;
			        if (other2 != null) {
			            return other2.f;
			        } else {
			            // TODO handle null value
			            return null;
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// field referenced inside the rhs of an assignment-as-expression
	@Test
	public void testExtractNullableField5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable E other;
			    @Nullable String f;
			    public void foo() {
			        String lo;
			        if ((lo = this.other.f) != null)
			            System.out.println(lo);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable E other;
			    @Nullable String f;
			    public void foo() {
			        String lo;
			        final E other2 = this.other;
			        if (other2 != null) {
			            if ((lo = other2.f) != null)
			                System.out.println(lo);
			        } else {
			            // TODO handle null value
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// reference to field of array type - dereferenced by f[0] and f.length
	@Test
	public void testExtractNullableField6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String[] f1;
			    @Nullable String[] f2;
			    public void foo() {
			        System.out.println(f1[0]);
			        System.out.println(f2.length);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, 0); // get correction for first of two problems
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String[] f1;
			    @Nullable String[] f2;
			    public void foo() {
			        final String[] f12 = f1;
			        if (f12 != null) {
			            System.out.println(f12[0]);
			        } else {
			            // TODO handle null value
			        }
			        System.out.println(f2.length);
			    }
			}
			""";
		assertEqualString(preview, str1);

		proposals= collectCorrections(cu, astRoot, 2, 1); // get correction for second of two problems
		assertNumberOfProposals(proposals, 3);
		proposal= (CUCorrectionProposal) proposals.get(0);
		preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String[] f1;
			    @Nullable String[] f2;
			    public void foo() {
			        System.out.println(f1[0]);
			        final String[] f22 = f2;
			        if (f22 != null) {
			            System.out.println(f22.length);
			        } else {
			            // TODO handle null value
			        }
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	// field has a generic type
	@Test
	public void testExtractNullableField7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			import java.util.List;
			public class E {
			    @Nullable List<String> f;
			    public void foo() {
			        System.out.println(f.size());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			import java.util.List;
			public class E {
			    @Nullable List<String> f;
			    public void foo() {
			        final List<String> f2 = f;
			        if (f2 != null) {
			            System.out.println(f2.size());
			        } else {
			            // TODO handle null value
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// occurrences inside a class initializer
	@Test
	public void testExtractNullableField8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable Exception e;
			    {
			        e.printStackTrace();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable Exception e;
			    {
			        final Exception e2 = e;
			        if (e2 != null) {
			            e2.printStackTrace();
			        } else {
			            // TODO handle null value
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// field reference inside a local variable initialization - ensure correct scoping of this local
	@Test
	public void testExtractNullableField9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String f;
			    public String foo() {
			        String upper = f.toUpperCase();
			        System.out.println(upper);
			        return upper;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String f;
			    public String foo() {
			        final String f2 = f;
			        if (f2 != null) {
			            String upper = f2.toUpperCase();
			            System.out.println(upper);
			            return upper;
			        } else {
			            // TODO handle null value
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// ==== Problem:	using a @Nullable or un-annotated field in assignment/return context expecting @NonNull
	// ==== Fix:		extract field access to a fresh local variable and add a null-check

	// return situation, field reference is this.f
	@Test
	public void testExtractPotentiallyNullField1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String f;
			    public @NonNull String foo() {
			        return this.f;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		// primary proposal: Extract to checked local variable
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String f;
			    public @NonNull String foo() {
			        final String f2 = this.f;
			        if (f2 != null) {
			            return f2;
			        } else {
			            // TODO handle null value
			            return null;
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);


		// secondary proposal: Change return type of 'foo(..)' to '@Nullable'
		proposal= (CUCorrectionProposal) proposals.get(1);
		preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String f;
			    public @Nullable String foo() {
			        return this.f;
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	// message send argument situation, field reference is local.f
	@Test
	public void testExtractPotentiallyNullField2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String f;
			    public void foo() {
			        E local = this;
			        bar(local.f);
			    }
			    public void bar(@NonNull String s) { }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable String f;
			    public void foo() {
			        E local = this;
			        final String f2 = local.f;
			        if (f2 != null) {
			            bar(f2);
			        } else {
			            // TODO handle null value
			        }
			    }
			    public void bar(@NonNull String s) { }
			}
			""";
		assertEqualString(preview, str1);
	}


	// @Nullable argument is used where @NonNull is required -> change to @NonNull
	@Test
	public void testChangeParameter1a() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@Nullable Exception e1) {
			        @NonNull Exception e = new Exception();
			        e = e1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'e1' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@NonNull Exception e1) {
			        @NonNull Exception e = new Exception();
			        e = e1;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// unspec'ed argument is used where @NonNull is required -> change to @NonNull
	@Test
	public void testChangeParameter1b() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(Exception e1) {
			        @NonNull Exception e = new Exception();
			        e = e1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3); // other is add @SW - TODO: check when this is offered
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'e1' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@NonNull Exception e1) {
			        @NonNull Exception e = new Exception();
			        e = e1;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testChangeParameter1c() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNull Object foo(@Nullable Object o) {
			        return o;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNull Object foo(@NonNull Object o) {
			        return o;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testChangeParameter1d() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNull Object foo(Object o) {
			        return o;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNull Object foo(@NonNull Object o) {
			        return o;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// don't propose to change argument if mismatch is in an assignment to the argument
	@Test
	public void testChangeParameter2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@NonNull Exception e1) {
			        e1 = null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
	}

	// Attempt to override a @Nullable argument with a @NonNull argument
	// -> change to @Nullable
	// -> change overridden to @NonNull
	@Test
	public void testChangeParameter3a() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@Nullable Exception e1) {
			    }
			}
			""";
		pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E2 extends E {
			    void foo(@NonNull Exception e1) {
			        e1.printStackTrace();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'e1' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E2 extends E {
			    void foo(@Nullable Exception e1) {
			        e1.printStackTrace();
			    }
			}
			""";
		assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change parameter in overridden 'foo(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@NonNull Exception e1) {
			    }
			}
			""";
		assertEqualString(preview, str3);
	}

	// Attempt to override a @Nullable argument with an unspec'ed argument
	// -> change to @Nullable
	@Test
	public void testChangeParameter3b() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@Nullable Exception e1) {
			    }
			}
			""";
		pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class E2 extends E {
			    void foo(Exception e1) {
			        e1.printStackTrace();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'e1' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			
			import org.eclipse.jdt.annotation.Nullable;
			
			public class E2 extends E {
			    void foo(@Nullable Exception e1) {
			        e1.printStackTrace();
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	// Attempt to override a @NonNull argument with an unspec'ed argument
	// -> change to @NonNull
	@Test
	public void testChangeParameter3c() throws Exception {
		// quickfix only offered with this warning enabled, but no need to say, because default is already "warning"
//		this.fJProject1.setOption(JavaCore.COMPILER_PB_NONNULL_PARAMETER_ANNOTATION_DROPPED, JavaCore.WARNING);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@NonNull Exception e1) {
			    }
			}
			""";
		pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class E2 extends E {
			    void foo(Exception e1) {
			        e1.printStackTrace();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4); // one real change plus two @SuppressWarnings proposals
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'e1' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			
			import org.eclipse.jdt.annotation.NonNull;
			
			public class E2 extends E {
			    void foo(@NonNull Exception e1) {
			        e1.printStackTrace();
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	// http://bugs.eclipse.org/400668 - [quick fix] The fix change parameter type to @Nonnull generated a null change
	// don't confuse changing arguments of current method and target method
	// -> split into two proposals
	@Test
	public void testChangeParameter4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@NonNull Object o) {
			        // nop
			    }
			}
			""";
		pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E2 {
			    void test(E e, @Nullable Object in) {
			        e.foo(in);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'in' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E2 {
			    void test(E e, @NonNull Object in) {
			        e.foo(in);
			    }
			}
			""";
		assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change parameter of 'foo(..)' to '@Nullable'");

		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@Nullable Object o) {
			        // nop
			    }
			}
			""";
		assertEqualString(preview, str3);
	}

	// http://bugs.eclipse.org/400668 - [quick fix] The fix change parameter type to @Nonnull generated a null change
	// don't confuse changing arguments of current method and target method
	// -> split into two proposals
	// variant with un-annotated parameter
	@Test
	public void testChangeParameter4a() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@NonNull Object o) {
			        // nop
			    }
			}
			""";
		pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class E2 {
			    void test(E e, Object in) {
			        e.foo(in);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4); // third (uninteresting) is "add @SW"
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'in' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			
			import org.eclipse.jdt.annotation.NonNull;
			
			public class E2 {
			    void test(E e, @NonNull Object in) {
			        e.foo(in);
			    }
			}
			""";
		assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change parameter of 'foo(..)' to '@Nullable'");

		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@Nullable Object o) {
			        // nop
			    }
			}
			""";
		assertEqualString(preview, str3);
	}

	// Bug 405086 - [quick fix] don't propose null annotations when those are disabled
	@Test
	public void testChangeParameter5() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.DISABLED);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class E {
				    void foo(Object o) {
				        if (o == null) return;
				        if (o != null) System.out.print(o.toString());
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 2); // only "add @SW"
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		}
	}

	// Bug 405086 - [quick fix] don't propose null annotations when those are disabled
	// don't propose a parameter change if there was no parameter annotation being the cause for the warning
	@Test
	public void testChangeParameter6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo(Object o) {
			        if (o == null) return;
			        if (o != null) System.out.print(o.toString());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2); // only "add @SW"
	}

	// Bug 405086 - [quick fix] don't propose null annotations when those are disabled
	// positive case (redundant check)
	@Test
	public void testChangeParameter7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@NonNull Object o) {
			        if (o != null) System.out.print(o.toString());
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3); // ignore 2nd ("add @SW")

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(@Nullable Object o) {
			        if (o != null) System.out.print(o.toString());
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// Bug 405086 - [quick fix] don't propose null annotations when those are disabled
	// positive case 2 (check always false)
	@Test
	public void testChangeParameter8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo(@org.eclipse.jdt.annotation.NonNull Object o) {
			        if (o == null) System.out.print("NOK");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3); // ignore 2nd ("add @SW")

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			
			import org.eclipse.jdt.annotation.Nullable;
			
			public class E {
			    void foo(@Nullable Object o) {
			        if (o == null) System.out.print("NOK");
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	// http://bugs.eclipse.org/395555 - [quickfix] Update null annotation quick fixes for bug 388281
	// conflict between inherited and default nullness
	@Test
	public void testChangeParameter9() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.ENABLED);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				import org.eclipse.jdt.annotation.*;
				public class E {
				    void foo(@Nullable Object o) {
				        // nop
				    }
				}
				""";
			pack1.createCompilationUnit("E.java", str, false, null);

			String str1= """
				package test1;
				import org.eclipse.jdt.annotation.*;
				@NonNullByDefault
				public class E2 extends E {
				    void foo(Object o) {
				        System.out.print("E2");
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 3);

			CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

			assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@Nullable'");

			String preview= getPreviewContent(proposal);

			String str2= """
				package test1;
				import org.eclipse.jdt.annotation.*;
				@NonNullByDefault
				public class E2 extends E {
				    void foo(@Nullable Object o) {
				        System.out.print("E2");
				    }
				}
				""";
			assertEqualString(preview, str2);

			proposal= (CUCorrectionProposal)proposals.get(1);

			assertEqualString(proposal.getDisplayString(), "Change parameter in overridden 'foo(..)' to '@NonNull'");

			preview= getPreviewContent(proposal);

			String str3= """
				package test1;
				import org.eclipse.jdt.annotation.*;
				public class E {
				    void foo(@NonNull Object o) {
				        // nop
				    }
				}
				""";
			assertEqualString(preview, str3);
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.DISABLED);
		}
	}

	// returning @Nullable value from @NonNull method -> change to @Nullable return
	@Test
	public void testChangeReturn1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNull Object foo() {
			        @Nullable Object o = null;
			        return o;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable Object foo() {
			        @Nullable Object o = null;
			        return o;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testChangeReturn2a() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNull Object foo() {
			        return new Object();
			    }
			}
			""";
		pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E2 extends E {
			    @Nullable Object foo() {
			        return new Object();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E2 extends E {
			    @NonNull Object foo() {
			        return new Object();
			    }
			}
			""";
		assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change return type of overridden 'foo(..)' to '@Nullable'");

		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @Nullable Object foo() {
			        return new Object();
			    }
			}
			""";
		assertEqualString(preview, str3);
	}

	@Test
	public void testChangeReturn2b() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNull Object foo() {
			        return new Object();
			    }
			}
			""";
		pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			public class E2 extends E {
			    Object foo() {
			        return new Object();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			
			import org.eclipse.jdt.annotation.NonNull;
			
			public class E2 extends E {
			    @NonNull
			    Object foo() {
			        return new Object();
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	// https://bugs.eclipse.org/395555 - [quickfix] Update null annotation quick fixes for bug 388281
	// conflict between nullness inherited from different parents
	@Test
	public void testChangeReturn3() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.ENABLED);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				import org.eclipse.jdt.annotation.*;
				public class E {
				    @NonNull Object foo() {
				        // nop
				    }
				}
				""";
			pack1.createCompilationUnit("E.java", str, false, null);

			String str1= """
				package test1;
				import org.eclipse.jdt.annotation.*;
				public interface IE {
				    @Nullable Object foo();
				}
				""";
			pack1.createCompilationUnit("IE.java", str1, false, null);

			String str2= """
				package test1;
				public class E2 extends E implements IE {
				    public Object foo() {
				        return this;
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str2, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 3);

			CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

			assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@Nullable'");

			String preview= getPreviewContent(proposal);

			String str3= """
				package test1;
				
				import org.eclipse.jdt.annotation.Nullable;
				
				public class E2 extends E implements IE {
				    public @Nullable Object foo() {
				        return this;
				    }
				}
				""";
			assertEqualString(preview, str3);

			proposal= (CUCorrectionProposal)proposals.get(1);

			assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@NonNull'");

			preview= getPreviewContent(proposal);

			String str4= """
				package test1;
				
				import org.eclipse.jdt.annotation.NonNull;
				
				public class E2 extends E implements IE {
				    public @NonNull Object foo() {
				        return this;
				    }
				}
				""";
			assertEqualString(preview, str4);
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.DISABLED);
		}
	}

	// https://bugs.eclipse.org/378724 - Null annotations are extremely hard to use in an existing project
	// see comment 12
	// remove @Nullable without adding redundant @NonNull (due to @NonNullByDefault)
	@Test
	public void testChangeReturn4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    @Nullable Object bar() {
			        return new Object();
			    }
			}
			""";
		pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E2 {
			    @NonNull Object foo(E e) {
			        return e.bar();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E2 {
			    @Nullable Object foo(E e) {
			        return e.bar();
			    }
			}
			""";
		assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'bar(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    Object bar() {
			        return new Object();
			    }
			}
			""";
		assertEqualString(preview, str3);
	}

	// https://bugs.eclipse.org/378724 - Null annotations are extremely hard to use in an existing project
	// see comment 12
	// remove @Nullable without adding redundant @NonNull (due to @NonNullByDefault)
	// variant: package-level default
	@Test
	public void testChangeReturn5() throws Exception {
		String suppressOptionalErrors= fJProject1.getOption(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, true);
		try {
			fJProject1.setOption(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, JavaCore.ENABLED);
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

			String str= """
				@org.eclipse.jdt.annotation.NonNullByDefault
				package test1;
				""";
			pack1.createCompilationUnit("package-info.java", str, false, null);

			String str1= """
				package test1;
				import org.eclipse.jdt.annotation.*;
				public class E {
				    @Nullable Object bar() {
				        return new Object();
				    }
				}
				""";
			pack1.createCompilationUnit("E.java", str1, false, null);

			String str2= """
				package test1;
				public class E2 {
				    public Object foo(E e) {
				        return e.bar();
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str2, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 4); // includes "add @SW"

			CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

			assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@Nullable'");

			String preview= getPreviewContent(proposal);

			String str3= """
				package test1;
				
				import org.eclipse.jdt.annotation.Nullable;
				
				public class E2 {
				    public @Nullable Object foo(E e) {
				        return e.bar();
				    }
				}
				""";
			assertEqualString(preview, str3);

			proposal= (CUCorrectionProposal)proposals.get(1);

			assertEqualString(proposal.getDisplayString(), "Change return type of 'bar(..)' to '@NonNull'");

			preview= getPreviewContent(proposal);

			String str4= """
				package test1;
				import org.eclipse.jdt.annotation.*;
				public class E {
				    Object bar() {
				        return new Object();
				    }
				}
				""";
			assertEqualString(preview, str4);
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, suppressOptionalErrors);
		}
	}

	// https://bugs.eclipse.org/378724 - Null annotations are extremely hard to use in an existing project
	// see comment 12
	// remove @Nullable without adding redundant @NonNull (due to @NonNullByDefault)
	// variant: cancelled default
	@Test
	public void testChangeReturn6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			@org.eclipse.jdt.annotation.NonNullByDefault
			package test1;
			""";
		pack1.createCompilationUnit("package-info.java", str, false, null);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault(false)\n"); // <- HERE
		buf.append("public class E {\n");
		buf.append("    @Nullable Object bar() {\n");
		buf.append("        return new Object();\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		String str1= """
			package test1;
			public class E2 {
			    public Object foo(E e) {
			        return e.bar();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'foo(..)' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			
			import org.eclipse.jdt.annotation.Nullable;
			
			public class E2 {
			    public @Nullable Object foo(E e) {
			        return e.bar();
			    }
			}
			""";
		assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'bar(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault(false)
			public class E {
			    @NonNull Object bar() {
			        return new Object();
			    }
			}
			""";
		assertEqualString(preview, str3);
	}

	@Test
	public void testRemoveRedundantAnnotation1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    void foo(@NonNull Object o) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    void foo(Object o) {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testRemoveRedundantAnnotation2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    @NonNull
			    Object foo(Object o) {
			        return new Object();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    Object foo(Object o) {
			        return new Object();
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testRemoveRedundantAnnotation3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    @NonNull
			    public Object foo(Object o) {
			        return new Object();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    public Object foo(Object o) {
			        return new Object();
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testRemoveRedundantAnnotation4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    @NonNullByDefault
			    void foo(Object o) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    void foo(Object o) {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testRemoveRedundantAnnotation5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    @NonNullByDefault
			    class E1 {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			    class E1 {
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testRemoveRedundantAnnotation6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNullByDefault
			    void foo(Object o) {
			        @NonNullByDefault
			        class E1 {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNullByDefault
			    void foo(Object o) {
			        class E1 {
			        }
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testRemoveRedundantAnnotation7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			@org.eclipse.jdt.annotation.NonNullByDefault
			package test1;
			""";
		pack1.createCompilationUnit("package-info.java", str, false, null);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			@NonNullByDefault
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testRemoveRedundantAnnotation8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			@org.eclipse.jdt.annotation.NonNullByDefault
			package test1;
			""";
		pack1.createCompilationUnit("package-info.java", str, false, null);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    @NonNullByDefault
			    void foo(Object o) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(Object o) {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}
	@Test
	public void testAddNonNull() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public <T extends Number> double foo(boolean b) {
			        Number n=Integer.valueOf(1);
			        if(b) {
			          n = null;
			        };
			        return n.doubleValue();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			
			import org.eclipse.jdt.annotation.NonNull;
			
			public class E {
			    public <T extends Number> double foo(boolean b) {
			        @NonNull
			        Number n=Integer.valueOf(1);
			        if(b) {
			          n = null;
			        };
			        return n.doubleValue();
			    }
			}
			""";
		assertEqualString(preview, str1);
	}
	// Attempt to override an unspec'ed argument with a @NonNull argument
	// -> change to @Nullable
	// -> change overridden to @NonNull
	// Specific for this test: arg name is different in overridden method.
	@Test
	public void testBug506108() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(Exception e, Exception e1, Exception e2) {
			    }
			}
			""";
		pack1.createCompilationUnit("E.java", str, false, null);

		String str1= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E2 extends E {
			    void foo(Exception e1, @NonNull Exception e2, Exception e) {
			        e2.printStackTrace();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E2.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'e2' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E2 extends E {
			    void foo(Exception e1, @Nullable Exception e2, Exception e) {
			        e2.printStackTrace();
			    }
			}
			""";
		assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change parameter in overridden 'foo(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			import org.eclipse.jdt.annotation.*;
			public class E {
			    void foo(Exception e, @NonNull Exception e1, Exception e2) {
			    }
			}
			""";
		assertEqualString(preview, str3);
	}
	@Test
	public void testBug525428a() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.ERROR);
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("package-info.java", str, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 2);
			CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

			assertEqualString(proposal.getDisplayString(), "Add '@NonNullByDefault' to the package declaration");

			String preview= getPreviewContent(proposal);

			String str1= """
				@org.eclipse.jdt.annotation.NonNullByDefault
				package test1;
				""";
			assertEqualStringIgnoreDelim(preview, str1);
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.IGNORE);
		}
	}
	@Test
	public void testBug525428b() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.ERROR);
		String typecomment= StubUtility.getCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, null).getPattern();
		String filecomment= StubUtility.getCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, null).getPattern();

		try {
			StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "/**\n * Type\n */", null);
			StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, "/**\n * File\n */", null);
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class E {
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
			cu.getJavaProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
			IMarker[] markers= pack1.getResource().findMarkers(null, true, IResource.DEPTH_INFINITE);
			assertEquals(1, markers.length);
			ArrayList<IJavaCompletionProposal> proposals= new ArrayList<>();
			IInvocationContext context= new AssistContext(cu,  0, 0);
			IEditorInput input= EditorUtility.getEditorInput(cu);
			IProblemLocation location= CorrectionMarkerResolutionGenerator.findProblemLocation(input, markers[0]);
			IStatus status= JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { location }, proposals);
			assertStatusOk(status);

			assertNumberOfProposals(proposals, 2);
			IJavaCompletionProposal proposal= proposals.get(0);

			assertEqualString(proposal.getDisplayString(), "Add package-info.java with '@NonNullByDefault'");
			proposal.apply(null);

			cu.getJavaProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);

			assertEquals(0, pack1.getResource().findMarkers(null, true, IResource.DEPTH_INFINITE).length);

			ICompilationUnit packageInfoCU= pack1.getCompilationUnit("package-info.java");
			String str1= """
				/**
				 * File
				 */
				/**
				 * Type
				 */
				@org.eclipse.jdt.annotation.NonNullByDefault
				package test1;
				""";
			assertEqualStringIgnoreDelim(packageInfoCU.getSource(), str1);
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.IGNORE);
			StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, typecomment, null);
			StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, filecomment, null);
		}
	}
	// marker for same project in two source folders in same project: package-info.java must be created in first one (when comparing paths)
	@Test
	public void testBug525428c() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.ERROR);
		String typecomment= StubUtility.getCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, null).getPattern();
		String filecomment= StubUtility.getCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, null).getPattern();

		try {
			StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "/**\n * Type\n */", null);
			StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, "/**\n * File\n */", null);
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class E {
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			IPackageFragmentRoot testSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src-tests");
			IPackageFragment pack2= testSourceFolder.createPackageFragment("test1", false, null);

			cu.getJavaProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);

			String str1= """
				package test1;
				public class ETest {
				}
				""";
			ICompilationUnit cu2= pack2.createCompilationUnit("ETest.java", str1, false, null);

			// after the incremental, there is also a problem marker on the package in the src-tests directory
			cu2.getJavaProject().getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);

			IMarker[] markers= pack1.getResource().findMarkers(null, true, IResource.DEPTH_INFINITE);
			assertEquals(1, markers.length);

			IMarker[] markers2= pack2.getResource().findMarkers(null, true, IResource.DEPTH_INFINITE);
			assertEquals(1, markers2.length);


			ArrayList<IJavaCompletionProposal> proposals= new ArrayList<>();
			IInvocationContext context= new AssistContext(cu, 0, 0);
			IEditorInput input= EditorUtility.getEditorInput(cu);
			IProblemLocation location= CorrectionMarkerResolutionGenerator.findProblemLocation(input, markers[0]);
			IStatus status= JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { location }, proposals);
			assertStatusOk(status);

			IInvocationContext context2= new AssistContext(cu, 0, 0);
			IEditorInput input2= EditorUtility.getEditorInput(cu);
			IProblemLocation location2= CorrectionMarkerResolutionGenerator.findProblemLocation(input2, markers2[0]);
			status= JavaCorrectionProcessor.collectCorrections(context2, new IProblemLocation[] { location2 }, proposals);
			assertStatusOk(status);

			assertNumberOfProposals(proposals, 4);
			IJavaCompletionProposal proposal= proposals.get(0);
			IJavaCompletionProposal proposal2= proposals.get(2);

			assertEqualString(proposal.getDisplayString(), "Add package-info.java with '@NonNullByDefault'");
			assertEqualString(proposal2.getDisplayString(), "Add package-info.java with '@NonNullByDefault'");

			MultiFixTarget[] problems= CorrectionMarkerResolution.getCleanUpTargets(new IMarker[] { markers[0], markers2[0] });

			((CreatePackageInfoWithDefaultNullnessProposal) proposal).resolve(problems, new NullProgressMonitor());

			cu.getJavaProject().getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
			assertEquals(0, cu.getJavaProject().getProject().findMarkers(null, true, IResource.DEPTH_INFINITE).length);

			ICompilationUnit packageInfoCU= pack1.getCompilationUnit("package-info.java");
			ICompilationUnit packageInfoCU2= pack2.getCompilationUnit("package-info.java");
			assertTrue("a package-info.java should have been created in src", packageInfoCU.exists());
			assertFalse("no package-info.java should have been created in src-tests", packageInfoCU2.exists());

			String str2= """
				/**
				 * File
				 */
				/**
				 * Type
				 */
				@org.eclipse.jdt.annotation.NonNullByDefault
				package test1;
				""";
			assertEqualStringIgnoreDelim(packageInfoCU.getSource(), str2);
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.IGNORE);
			StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, typecomment, null);
			StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, filecomment, null);
		}
	}
	// marker for same project in two dependent projects: package-info.java must be created in the one that cannot see the other
	@Test
	public void testBug525428d() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.ERROR);
		String typecomment= StubUtility.getCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, null).getPattern();
		String filecomment= StubUtility.getCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, null).getPattern();
		IJavaProject proj2=null;

		try {
			StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "/**\n * Type\n */", null);
			StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, "/**\n * File\n */", null);

			proj2= JavaProjectHelper.createJavaProject("OtherProject", "bin");
			proj2.setRawClasspath(projectSetup.getDefaultClasspath(), null);
			TestOptions.initializeProjectOptions(proj2);
			JavaProjectHelper.addLibrary(proj2, new Path(ANNOTATION_JAR_PATH));
			JavaProjectHelper.addRequiredProject(proj2, fJProject1);
			IPackageFragmentRoot sourceFolder2= JavaProjectHelper.addSourceContainer(proj2, "src");
			proj2.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.ERROR);

			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class E1 {
				}
				""";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", str, false, null);

			IPackageFragment pack2= sourceFolder2.createPackageFragment("test1", false, null);

			String str1= """
				package test1;
				public class E2 {
				}
				""";
			ICompilationUnit cu2= pack2.createCompilationUnit("E2.java", str1, false, null);

			cu2.getJavaProject().getProject().getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);

			IMarker[] markers= pack1.getResource().findMarkers(null, true, IResource.DEPTH_INFINITE);
			assertEquals(1, markers.length);

			IMarker[] markers2= pack2.getResource().findMarkers(null, true, IResource.DEPTH_INFINITE);
			assertEquals(1, markers2.length);


			ArrayList<IJavaCompletionProposal> proposals= new ArrayList<>();
			IInvocationContext context= new AssistContext(cu1, 0, 0);
			IEditorInput input= EditorUtility.getEditorInput(cu1);
			IProblemLocation location= CorrectionMarkerResolutionGenerator.findProblemLocation(input, markers[0]);
			IStatus status= JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { location }, proposals);
			assertStatusOk(status);

			IInvocationContext context2= new AssistContext(cu2, 0, 0);
			IEditorInput input2= EditorUtility.getEditorInput(cu2);
			IProblemLocation location2= CorrectionMarkerResolutionGenerator.findProblemLocation(input2, markers2[0]);
			status= JavaCorrectionProcessor.collectCorrections(context2, new IProblemLocation[] { location2 }, proposals);
			assertStatusOk(status);

			assertNumberOfProposals(proposals, 4);
			IJavaCompletionProposal proposal= proposals.get(0);
			IJavaCompletionProposal proposal2= proposals.get(2);

			assertEqualString(proposal.getDisplayString(), "Add package-info.java with '@NonNullByDefault'");
			assertEqualString(proposal2.getDisplayString(), "Add package-info.java with '@NonNullByDefault'");

			MultiFixTarget[] problems= CorrectionMarkerResolution.getCleanUpTargets(new IMarker[] { markers[0], markers2[0] });

			((CreatePackageInfoWithDefaultNullnessProposal) proposal).resolve(problems, new NullProgressMonitor());

			cu1.getJavaProject().getProject().getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);

			assertEquals(0, cu1.getJavaProject().getProject().findMarkers(null, true, IResource.DEPTH_INFINITE).length);
			assertEquals(0, cu2.getJavaProject().getProject().findMarkers(null, true, IResource.DEPTH_INFINITE).length);

			ICompilationUnit packageInfoCU= pack1.getCompilationUnit("package-info.java");
			ICompilationUnit packageInfoCU2= pack2.getCompilationUnit("package-info.java");
			assertTrue("a package-info.java should have been created in fJProject1", packageInfoCU.exists());
			assertFalse("no package-info.java should have been created in proj2", packageInfoCU2.exists());

			String str2= """
				/**
				 * File
				 */
				/**
				 * Type
				 */
				@org.eclipse.jdt.annotation.NonNullByDefault
				package test1;
				""";
			assertEqualStringIgnoreDelim(packageInfoCU.getSource(), str2);
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.IGNORE);
			StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, typecomment, null);
			StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, filecomment, null);
			if (proj2 != null) {
				JavaProjectHelper.delete(proj2);
			}
		}
	}
	// marker for same project in two independent projects: package-info.java must be created in both
	@Test
	public void testBug525428e() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.ERROR);
		String typecomment= StubUtility.getCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, null).getPattern();
		String filecomment= StubUtility.getCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, null).getPattern();
		IJavaProject proj2=null;

		try {
			StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "/**\n * Type\n */", null);
			StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, "/**\n * File\n */", null);

			proj2= JavaProjectHelper.createJavaProject("OtherProject", "bin");
			proj2.setRawClasspath(projectSetup.getDefaultClasspath(), null);
			TestOptions.initializeProjectOptions(proj2);
			JavaProjectHelper.addLibrary(proj2, new Path(ANNOTATION_JAR_PATH));
			IPackageFragmentRoot sourceFolder2= JavaProjectHelper.addSourceContainer(proj2, "src");
			proj2.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.ERROR);

			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class E1 {
				}
				""";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", str, false, null);

			IPackageFragment pack2= sourceFolder2.createPackageFragment("test1", false, null);

			String str1= """
				package test1;
				public class E2 {
				}
				""";
			ICompilationUnit cu2= pack2.createCompilationUnit("E2.java", str1, false, null);

			cu2.getJavaProject().getProject().getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);

			IMarker[] markers= pack1.getResource().findMarkers(null, true, IResource.DEPTH_INFINITE);
			assertEquals(1, markers.length);

			IMarker[] markers2= pack2.getResource().findMarkers(null, true, IResource.DEPTH_INFINITE);
			assertEquals(1, markers2.length);


			ArrayList<IJavaCompletionProposal> proposals= new ArrayList<>();
			IInvocationContext context= new AssistContext(cu1, 0, 0);
			IEditorInput input= EditorUtility.getEditorInput(cu1);
			IProblemLocation location= CorrectionMarkerResolutionGenerator.findProblemLocation(input, markers[0]);
			IStatus status= JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { location }, proposals);
			assertStatusOk(status);

			IInvocationContext context2= new AssistContext(cu2, 0, 0);
			IEditorInput input2= EditorUtility.getEditorInput(cu2);
			IProblemLocation location2= CorrectionMarkerResolutionGenerator.findProblemLocation(input2, markers2[0]);
			status= JavaCorrectionProcessor.collectCorrections(context2, new IProblemLocation[] { location2 }, proposals);
			assertStatusOk(status);

			assertNumberOfProposals(proposals, 4);
			IJavaCompletionProposal proposal= proposals.get(0);
			IJavaCompletionProposal proposal2= proposals.get(2);

			assertEqualString(proposal.getDisplayString(), "Add package-info.java with '@NonNullByDefault'");
			assertEqualString(proposal2.getDisplayString(), "Add package-info.java with '@NonNullByDefault'");

			MultiFixTarget[] problems= CorrectionMarkerResolution.getCleanUpTargets(new IMarker[] { markers[0], markers2[0] });

			((CreatePackageInfoWithDefaultNullnessProposal) proposal).resolve(problems, new NullProgressMonitor());

			cu1.getJavaProject().getProject().getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);

			assertEquals(0, cu1.getJavaProject().getProject().findMarkers(null, true, IResource.DEPTH_INFINITE).length);
			assertEquals(0, cu2.getJavaProject().getProject().findMarkers(null, true, IResource.DEPTH_INFINITE).length);

			ICompilationUnit packageInfoCU= pack1.getCompilationUnit("package-info.java");
			ICompilationUnit packageInfoCU2= pack2.getCompilationUnit("package-info.java");
			assertTrue("a package-info.java should have been created in fJProject1", packageInfoCU.exists());
			assertTrue("a package-info.java should have been created in proj2", packageInfoCU2.exists());

			String str2= """
				/**
				 * File
				 */
				/**
				 * Type
				 */
				@org.eclipse.jdt.annotation.NonNullByDefault
				package test1;
				""";
			assertEqualStringIgnoreDelim(packageInfoCU.getSource(), str2);
		} finally {
			fJProject1.setOption(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, JavaCore.IGNORE);
			StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, typecomment, null);
			StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, filecomment, null);
			if (proj2 != null) {
				JavaProjectHelper.delete(proj2);
			}
		}
	}
	@Test
	public void testGH1294() throws Exception {
		IPackageFragment my= fSourceFolder.createPackageFragment("my", false, null);
		ICompilationUnit cu= my.createCompilationUnit("Test.java",
				"""
				package my;
				import org.eclipse.jdt.annotation.*;
				interface IInputValidator {
					public String isValid(String newText);
				}
				public class Test {
					public static IInputValidator getRefNameInputValidator(
							final Object repo, final String refPrefix,
							final boolean errorOnEmptyName) {
						return new IInputValidator() {
							public String isValid(String newText) {
								String validationStatus = validateNewRefName(newText, this,
										refPrefix, errorOnEmptyName);
								return validationStatus;
							}
						};
					}
					@NonNull
					public static String validateNewRefName(String refNameInput,
							@NonNull Object repo, @NonNull String refPrefix,
							final boolean errorOnEmptyName) {
						return "";
					}
				}
				""",
				false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 5);
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'refPrefix' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		assertEqualString(preview,
				"""
				package my;
				import org.eclipse.jdt.annotation.*;
				interface IInputValidator {
					public String isValid(String newText);
				}
				public class Test {
					public static IInputValidator getRefNameInputValidator(
							final Object repo, final @NonNull String refPrefix,
							final boolean errorOnEmptyName) {
						return new IInputValidator() {
							public String isValid(String newText) {
								String validationStatus = validateNewRefName(newText, this,
										refPrefix, errorOnEmptyName);
								return validationStatus;
							}
						};
					}
					@NonNull
					public static String validateNewRefName(String refNameInput,
							@NonNull Object repo, @NonNull String refPrefix,
							final boolean errorOnEmptyName) {
						return "";
					}
				}
				""");
	}
}
