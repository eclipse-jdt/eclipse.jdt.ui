/*******************************************************************************
 * Copyright (c) 2016, 2020 GK Software AG and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

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
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Tests against projects with 1.8 compliance and "old" declaration null annotations.
 * Those tests are made to run on Java Spider 1.8 .
 */
public class NullAnnotationsQuickFixTest1d8Mix extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java1d8ProjectTestSetup();

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
			String version= "[1.1.0,2.0.0)"; // tests run at 1.8, but still use "old" null annotations
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

	// ==== Problem:	unchecked conversion, type elided lambda arg
	// ==== Fixes:		change downstream method parameter to @Nullable
	//					add @SW("null")

	@Test
	public void testBug473068_elided() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("testNullAnnotations", false, null);
		String str= """
			package testNullAnnotations;
			
			import org.eclipse.jdt.annotation.NonNull;
			
			interface Consumer<T> {
			    void accept(T t);
			}
			public class Snippet {
			\t
				public void select(final double min, final double max) {
				    doStuff(0, 1, min, max, (data) -> updateSelectionData(data));
				}
			\t
				private void doStuff(int a, int b, final double min, final double max, Consumer<Object> postAction) {
			
				}
			    private void updateSelectionData(final @NonNull Object data) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package testNullAnnotations;
			
			import org.eclipse.jdt.annotation.NonNull;
			import org.eclipse.jdt.annotation.Nullable;
			
			interface Consumer<T> {
			    void accept(T t);
			}
			public class Snippet {
			\t
				public void select(final double min, final double max) {
				    doStuff(0, 1, min, max, (data) -> updateSelectionData(data));
				}
			\t
				private void doStuff(int a, int b, final double min, final double max, Consumer<Object> postAction) {
			
				}
			    private void updateSelectionData(final @Nullable Object data) {
			    }
			}
			""";
		assertEqualString(preview, str1);

		proposal= (CUCorrectionProposal) proposals.get(1);
		preview= getPreviewContent(proposal);

		String str2= """
			package testNullAnnotations;
			
			import org.eclipse.jdt.annotation.NonNull;
			
			interface Consumer<T> {
			    void accept(T t);
			}
			public class Snippet {
			\t
				@SuppressWarnings("null")
			    public void select(final double min, final double max) {
				    doStuff(0, 1, min, max, (data) -> updateSelectionData(data));
				}
			\t
				private void doStuff(int a, int b, final double min, final double max, Consumer<Object> postAction) {
			
				}
			    private void updateSelectionData(final @NonNull Object data) {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	// ==== Problem:	unchecked conversion, explicitly typed lambda arg
	// ==== Fixes:		annotate lambda arg (@NonNull)
	//					change downstream method parameter to @Nullable
	//					add @SW("null")

	@Test
	public void testBug473068_explicit_type() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("testNullAnnotations", false, null);
		String str= """
			package testNullAnnotations;
			
			import org.eclipse.jdt.annotation.NonNull;
			
			interface Consumer<T> {
			    void accept(T t);
			}
			public class Snippet {
			\t
				public void select(final double min, final double max) {
				    doStuff(0, 1, min, max, (Object data) -> updateSelectionData(data));
				}
			\t
				private void doStuff(int a, int b, final double min, final double max, Consumer<Object> postAction) {
			
				}
			    private void updateSelectionData(final @NonNull Object data) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package testNullAnnotations;
			
			import org.eclipse.jdt.annotation.NonNull;
			
			interface Consumer<T> {
			    void accept(T t);
			}
			public class Snippet {
			\t
				public void select(final double min, final double max) {
				    doStuff(0, 1, min, max, (@NonNull Object data) -> updateSelectionData(data));
				}
			\t
				private void doStuff(int a, int b, final double min, final double max, Consumer<Object> postAction) {
			
				}
			    private void updateSelectionData(final @NonNull Object data) {
			    }
			}
			""";
		assertEqualString(preview, str1);

		proposal= (CUCorrectionProposal) proposals.get(1);
		preview= getPreviewContent(proposal);

		String str2= """
			package testNullAnnotations;
			
			import org.eclipse.jdt.annotation.NonNull;
			import org.eclipse.jdt.annotation.Nullable;
			
			interface Consumer<T> {
			    void accept(T t);
			}
			public class Snippet {
			\t
				public void select(final double min, final double max) {
				    doStuff(0, 1, min, max, (Object data) -> updateSelectionData(data));
				}
			\t
				private void doStuff(int a, int b, final double min, final double max, Consumer<Object> postAction) {
			
				}
			    private void updateSelectionData(final @Nullable Object data) {
			    }
			}
			""";
		assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal) proposals.get(2);
		preview= getPreviewContent(proposal);

		String str3= """
			package testNullAnnotations;
			
			import org.eclipse.jdt.annotation.NonNull;
			
			interface Consumer<T> {
			    void accept(T t);
			}
			public class Snippet {
			\t
				@SuppressWarnings("null")
			    public void select(final double min, final double max) {
				    doStuff(0, 1, min, max, (Object data) -> updateSelectionData(data));
				}
			\t
				private void doStuff(int a, int b, final double min, final double max, Consumer<Object> postAction) {
			
				}
			    private void updateSelectionData(final @NonNull Object data) {
			    }
			}
			""";
		assertEqualString(preview, str3);
	}
}
