/*******************************************************************************
 * Copyright (c) 2017, 2022 GK Software AG and others.
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;

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
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class NullAnnotationsQuickFixTest1d8 extends QuickFixTest {

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
		if (this.ANNOTATION_JAR_PATH == null) {
			String version= "[2.0.0,3.0.0)"; // tests run at 1.8, need the "new" null annotations
			Bundle[] bundles= Platform.getBundles("org.eclipse.jdt.annotation", version);
			File bundleFile= FileLocator.getBundleFileLocation(bundles[0]).get();
			if (bundleFile.isDirectory())
				this.ANNOTATION_JAR_PATH= bundleFile.getPath() + "/bin";
			else
				this.ANNOTATION_JAR_PATH= bundleFile.getPath();
		}
		JavaProjectHelper.addLibrary(fJProject1, new Path(ANNOTATION_JAR_PATH));

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	/*
	 * Problem: package default nonnull conflicts with inherited nullable (via generic substitution) (implicit vs. implicit)
	 * Location: method return
	 * Fixes:
	 * - change local to nullable (equal)
	 * - change local to nonnull  (covariant return)
	 * - change super to nonnull  (equal)
	 */
	@Test
	public void testBug499716_a() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.ENABLED);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("interface Type<@Nullable K> {\n");
		buf.append("	@NonNullByDefault(DefaultLocation.RETURN_TYPE)\n"); // no effect
		buf.append("	K get();\n");
		buf.append("\n");
		buf.append("	class U implements Type<@Nullable String> {\n");
		buf.append("		@Override\n");
		buf.append("		public String get() { // <-- error \"The default '@NonNull' conflicts...\"\n");
		buf.append("			return \"\";\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'get(..)' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("interface Type<@Nullable K> {\n");
		buf.append("	@NonNullByDefault(DefaultLocation.RETURN_TYPE)\n");
		buf.append("	K get();\n");
		buf.append("\n");
		buf.append("	class U implements Type<@Nullable String> {\n");
		buf.append("		@Override\n");
		buf.append("		public @Nullable String get() { // <-- error \"The default '@NonNull' conflicts...\"\n");
		buf.append("			return \"\";\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'get(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("interface Type<@Nullable K> {\n");
		buf.append("	@NonNullByDefault(DefaultLocation.RETURN_TYPE)\n");
		buf.append("	K get();\n");
		buf.append("\n");
		buf.append("	class U implements Type<@Nullable String> {\n");
		buf.append("		@Override\n");
		buf.append("		public @NonNull String get() { // <-- error \"The default '@NonNull' conflicts...\"\n");
		buf.append("			return \"\";\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(2);

		assertEqualString(proposal.getDisplayString(), "Change return type of overridden 'get(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("interface Type<@Nullable K> {\n");
		buf.append("	@NonNullByDefault(DefaultLocation.RETURN_TYPE)\n");
		buf.append("    @NonNull\n");
		buf.append("	K get();\n");
		buf.append("\n");
		buf.append("	class U implements Type<@Nullable String> {\n");
		buf.append("		@Override\n");
		buf.append("		public String get() { // <-- error \"The default '@NonNull' conflicts...\"\n");
		buf.append("			return \"\";\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	/*
	 * Problem: package default nonnull conflicts with inherited nullable (via generic substitution) (implicit vs. implicit)
	 * Location: parameter
	 * Fixes:
	 * - change local to nullable
	 * - change super to nonnull
	 * No covariant parameter!
	 */
	@Test
	public void testBug499716_b() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.ENABLED);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("interface Type<@Nullable K> {\n");
		buf.append("	void set(int i, K arg);\n");
		buf.append("\n");
		buf.append("	class U implements Type<@Nullable String> {\n");
		buf.append("		@Override\n");
		buf.append("		public void set(int i, String arg) {\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'arg' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("interface Type<@Nullable K> {\n");
		buf.append("	void set(int i, K arg);\n");
		buf.append("\n");
		buf.append("	class U implements Type<@Nullable String> {\n");
		buf.append("		@Override\n");
		buf.append("		public void set(int i, @Nullable String arg) {\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change parameter in overridden 'set(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("interface Type<@Nullable K> {\n");
		buf.append("	void set(int i, @NonNull K arg);\n");
		buf.append("\n");
		buf.append("	class U implements Type<@Nullable String> {\n");
		buf.append("		@Override\n");
		buf.append("		public void set(int i, String arg) {\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	/*
	 * Problem: explicit nullable conflicts with inherited nonnull (from type default) (illegal override)
	 * Location: return type
	 * Fixes:
	 * - change local to nonnull
	 * - change super to nullable
	 * No contravariant return!
	 */
	@Test
	public void testBug499716_c() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.ENABLED);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault(DefaultLocation.RETURN_TYPE)\n");
		buf.append("interface Type {\n");
		buf.append("	String get();\n");
		buf.append("\n");
		buf.append("	class U implements Type {\n");
		buf.append("		@Override\n");
		buf.append("		public @Nullable String get() {\n");
		buf.append("			return \"\";\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'get(..)' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault(DefaultLocation.RETURN_TYPE)\n");
		buf.append("interface Type {\n");
		buf.append("	String get();\n");
		buf.append("\n");
		buf.append("	class U implements Type {\n");
		buf.append("		@Override\n");
		buf.append("		public String get() {\n");
		buf.append("			return \"\";\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change return type of overridden 'get(..)' to '@Nullable'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault(DefaultLocation.RETURN_TYPE)\n");
		buf.append("interface Type {\n");
		buf.append("	@Nullable\n");
		buf.append("    String get();\n");
		buf.append("\n");
		buf.append("	class U implements Type {\n");
		buf.append("		@Override\n");
		buf.append("		public @Nullable String get() {\n");
		buf.append("			return \"\";\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	/*
	 * Problem: package default nonnull conflicts with declared nullable in super (illegal override)
	 * Location: method parameter
	 * Fixes:
	 * - change local to nullable
	 * - change super to implicit nonnull (by removing to make default apply)
	 * No covariant parameter!
	 */
	@Test
	public void testBug499716_d() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("interface Type {\n");
		buf.append("	void set(@Nullable String s);\n");
		buf.append("\n");
		buf.append("	class U implements Type {\n");
		buf.append("		@Override\n");
		buf.append("		public void set(String t) {\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 't' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("interface Type {\n");
		buf.append("	void set(@Nullable String s);\n");
		buf.append("\n");
		buf.append("	class U implements Type {\n");
		buf.append("		@Override\n");
		buf.append("		public void set(@Nullable String t) {\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change parameter in overridden 'set(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("interface Type {\n");
		buf.append("	void set(String s);\n");
		buf.append("\n");
		buf.append("	class U implements Type {\n");
		buf.append("		@Override\n");
		buf.append("		public void set(String t) {\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}


	/*
	 * Test that no redundant null annotations are created.
	 * Variation 1: @NonNullByDefault applies everywhere, type is non-null
	 */
	@Test
	public void test443146a() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test {\n");
		buf.append("	abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Create field 'x'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test {\n");
		buf.append("	private Map<? extends Map<String, @Nullable Integer>, String[][]> x;\n");
		buf.append("\n");
		buf.append("    abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Create parameter 'x'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test {\n");
		buf.append("	abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g(Map<? extends Map<String, @Nullable Integer>, String[][]> x) {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(2);

		assertEqualString(proposal.getDisplayString(), "Create local variable 'x'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test {\n");
		buf.append("	abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		Map<? extends Map<String, @Nullable Integer>, String[][]> x = f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	/*
	 * Test that no redundant null annotations are created.
	 * Variation 2: @NonNullByDefault applies everywhere
	 * Note, that there is no @Nullable generated for the 'local variable' case.
	 */
	@Test
	public void test443146b() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test {\n");
		buf.append("	abstract @Nullable Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Create field 'x'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test {\n");
		buf.append("	private @Nullable Map<? extends Map<String, @Nullable Integer>, String[][]> x;\n");
		buf.append("\n");
		buf.append("    abstract @Nullable Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Create parameter 'x'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test {\n");
		buf.append("	abstract @Nullable Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g(@Nullable Map<? extends Map<String, @Nullable Integer>, String[][]> x) {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(2);

		assertEqualString(proposal.getDisplayString(), "Create local variable 'x'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test {\n");
		buf.append("	abstract @Nullable Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		Map<? extends Map<String, @Nullable Integer>, String[][]> x = f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	/*
	 * Test that no redundant null annotations are created.
	 * Variation 3: @NonNullByDefault doesn't apply at the target locations (so annotations ARE expected, but not for the local variable)
	 */
	@Test
	public void test443146c() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("abstract class Test {\n");
		buf.append("	@NonNullByDefault\n");
		buf.append("	abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Create field 'x'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("abstract class Test {\n");
		buf.append("	private @NonNull Map<? extends @NonNull Map<@NonNull String, @Nullable Integer>, String @NonNull [][]> x;\n");
		buf.append("\n");
		buf.append("    @NonNullByDefault\n");
		buf.append("	abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Create parameter 'x'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("abstract class Test {\n");
		buf.append("	@NonNullByDefault\n");
		buf.append("	abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g(@NonNull Map<? extends @NonNull Map<@NonNull String, @Nullable Integer>, String @NonNull [][]> x) {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(2);

		assertEqualString(proposal.getDisplayString(), "Create local variable 'x'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault({})\n");
		buf.append("abstract class Test {\n");
		buf.append("	@NonNullByDefault\n");
		buf.append("	abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		Map<? extends @NonNull Map<@NonNull String, @Nullable Integer>, String @NonNull [][]> x = f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	/*
	 * Test that no null annotations are created in inapplicable location, here: cast.
	 */
	@Test
	public void test443146d() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test {\n");
		buf.append("	@NonNull Map<@NonNull String, @Nullable Integer> f(Object o) {\n");
		buf.append("		return o;\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Add cast to 'Map<String, Integer>'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test {\n");
		buf.append("	@NonNull Map<@NonNull String, @Nullable Integer> f(Object o) {\n");
		buf.append("		return (Map<@NonNull String, @Nullable Integer>) o;\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	/*
	 * Variation: @NonNullByDefault applies everywhere, type is a type variable
	 */
	@Test
	public void test443146e() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test<T> {\n");
		buf.append("	abstract @NonNull T f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Create field 'x'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test<T> {\n");
		buf.append("	private @NonNull T x;\n");
		buf.append("\n");
		buf.append("    abstract @NonNull T f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Create parameter 'x'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test<T> {\n");
		buf.append("	abstract @NonNull T f();\n");
		buf.append("\n");
		buf.append("	public void g(@NonNull T x) {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(2);

		assertEqualString(proposal.getDisplayString(), "Create local variable 'x'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test<T> {\n");
		buf.append("	abstract @NonNull T f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		@NonNull\n");
		buf.append("        T x = f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	/*
	 * Variation: @NonNullByDefault applies everywhere, type contains explicit @NonNull on wildcard and type variable
	 */
	@Test
	public void test443146f() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test<T> {\n");
		buf.append("	abstract Map<Map<@NonNull ?, Integer>, @NonNull T> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Create field 'x'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test<T> {\n");
		buf.append("	private Map<Map<@NonNull ?, Integer>, @NonNull T> x;\n");
		buf.append("\n");
		buf.append("    abstract Map<Map<@NonNull ?, Integer>, @NonNull T> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Create parameter 'x'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test<T> {\n");
		buf.append("	abstract Map<Map<@NonNull ?, Integer>, @NonNull T> f();\n");
		buf.append("\n");
		buf.append("	public void g(Map<Map<@NonNull ?, Integer>, @NonNull T> x) {\n");
		buf.append("		x=f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

		proposal= (CUCorrectionProposal)proposals.get(2);

		assertEqualString(proposal.getDisplayString(), "Create local variable 'x'");

		preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("\n");
		buf.append("abstract class Test<T> {\n");
		buf.append("	abstract Map<Map<@NonNull ?, Integer>, @NonNull T> f();\n");
		buf.append("\n");
		buf.append("	public void g() {\n");
		buf.append("		Map<Map<@NonNull ?, Integer>, @NonNull T> x = f();\n");
		buf.append("	}\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	@Test
	public void testBug513682() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class Test {\n");
		buf.append("    void foo(Object o) {\n");
		buf.append("      if(o != null) {\n");
		buf.append("          o.hashCode();\n");
		buf.append("      }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu=pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'o' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class Test {\n");
		buf.append("    void foo(@Nullable Object o) {\n");
		buf.append("      if(o != null) {\n");
		buf.append("          o.hashCode();\n");
		buf.append("      }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	@Test
	public void testBug513209a() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("   public void SomeMethod(\n");
		buf.append("      String[] a)\n");
		buf.append("   {\n");
		buf.append("\n");
		buf.append("   }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class B extends A {\n");
		buf.append("   @Override\n");
		buf.append("   public void SomeMethod(\n");
		buf.append("      String[] a)\n");
		buf.append("   {\n");
		buf.append("\n");
		buf.append("   }\n");
		buf.append("}\n");
		ICompilationUnit cu=pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'a' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class B extends A {\n");
		buf.append("   @Override\n");
		buf.append("   public void SomeMethod(\n");
		buf.append("      String @Nullable [] a)\n");
		buf.append("   {\n");
		buf.append("\n");
		buf.append("   }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	@Test
	public void testBug513209b() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("   public void SomeMethod(\n");
		buf.append("      int[][] a)\n");
		buf.append("   {\n");
		buf.append("\n");
		buf.append("   }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class B extends A {\n");
		buf.append("   @Override\n");
		buf.append("   public void SomeMethod(\n");
		buf.append("      int[][] a)\n");
		buf.append("   {\n");
		buf.append("\n");
		buf.append("   }\n");
		buf.append("}\n");
		ICompilationUnit cu=pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'a' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class B extends A {\n");
		buf.append("   @Override\n");
		buf.append("   public void SomeMethod(\n");
		buf.append("      int @Nullable [][] a)\n");
		buf.append("   {\n");
		buf.append("\n");
		buf.append("   }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	@Test
	public void testBug513209c() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("   public void SomeMethod(\n");
		buf.append("      String[] a)\n");
		buf.append("   {\n");
		buf.append("\n");
		buf.append("   }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class B extends A {\n");
		buf.append("   @Override\n");
		buf.append("   public void SomeMethod(\n");
		buf.append("      String @NonNull [] a)\n");
		buf.append("   {\n");
		buf.append("\n");
		buf.append("   }\n");
		buf.append("}\n");
		ICompilationUnit cu=pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 'a' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("public class B extends A {\n");
		buf.append("   @Override\n");
		buf.append("   public void SomeMethod(\n");
		buf.append("      String @Nullable [] a)\n");
		buf.append("   {\n");
		buf.append("\n");
		buf.append("   }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	@Test
	public void testBug513209d() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class A {\n");
		buf.append("   public String[][][] SomeMethod()\n");
		buf.append("   {\n");
		buf.append("		return null;\n");
		buf.append("   }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		buf = new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class B extends A {\n");
		buf.append("   @Override\n");
		buf.append("   public String[][][] SomeMethod()\n");
		buf.append("   {\n");
		buf.append("		return new String[0][][];\n");
		buf.append("   }\n");
		buf.append("}\n");
		ICompilationUnit cu=pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change return type of 'SomeMethod(..)' to '@NonNull'");

		String preview= getPreviewContent(proposal);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNull;\n");
		buf.append("\n");
		buf.append("public class B extends A {\n");
		buf.append("   @Override\n");
		buf.append("   public String @NonNull [][][] SomeMethod()\n");
		buf.append("   {\n");
		buf.append("		return new String[0][][];\n");
		buf.append("   }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	@Test
	public void testBug562891() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class A {\n");
		buf.append("    private @Nullable String foo;\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("A.java", buf.toString(), false, null);
		AssistContext context= new AssistContext(cu, buf.toString().indexOf("foo"), 0);
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import org.eclipse.jdt.annotation.*;\n");
		buf.append("@NonNullByDefault\n");
		buf.append("public class A {\n");
		buf.append("    private @Nullable String foo;\n");
		buf.append("\n");
		buf.append("    public @Nullable String getFoo() {\n");
		buf.append("        return foo;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public void setFoo(@Nullable String foo) {\n");
		buf.append("        this.foo = foo;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertExpectedExistInProposals(proposals, new String[] {buf.toString()});
	}
	@Test
	public void testBug525424() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		try {
			Hashtable<String, String> myOptions= new Hashtable<>(options);
			myOptions.put(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, "my.Nullable");
			myOptions.put(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, "my.NonNull");
			myOptions.put(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME, "my.NonNullByDefault");
			JavaCore.setOptions(myOptions);

			IPackageFragment my= fSourceFolder.createPackageFragment("my", false, null);
			StringBuilder buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("import java.lang.annotation.ElementType;\n");
			buf.append("import java.lang.annotation.Target;\n");
			buf.append("\n");
			buf.append("@Target(ElementType.TYPE_USE)\n");
			buf.append("public @interface Nullable {\n");
			buf.append("}\n");
			my.createCompilationUnit("Nullable.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("import java.lang.annotation.ElementType;\n");
			buf.append("import java.lang.annotation.Target;\n");
			buf.append("\n");
			buf.append("@Target(ElementType.TYPE_USE)\n");
			buf.append("public @interface NonNull {\n");
			buf.append("}\n");
			my.createCompilationUnit("NonNull.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("public enum DefaultLocation {\n");
			buf.append("	PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT\n");
			buf.append("}\n");
			buf.append("");
			my.createCompilationUnit("DefaultLocation.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("import static my.DefaultLocation.*;\n");
			buf.append("\n");
			buf.append("public @interface NonNullByDefault {\n");
			buf.append("	DefaultLocation[] value() default { PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT };\n");
			buf.append("}\n");
			buf.append("");
			my.createCompilationUnit("NonNullByDefault.java", buf.toString(), false, null);

			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("public class A {\n");
			buf.append("   public void SomeMethod(\n");
			buf.append("      String[] a)\n");
			buf.append("   {\n");
			buf.append("\n");
			buf.append("   }\n");
			buf.append("}\n");
			pack1.createCompilationUnit("A.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("import my.*;\n");
			buf.append("@NonNullByDefault\n");
			buf.append("public class B extends A {\n");
			buf.append("   @Override\n");
			buf.append("   public void SomeMethod(\n");
			buf.append("      String[] a)\n");
			buf.append("   {\n");
			buf.append("\n");
			buf.append("   }\n");
			buf.append("}\n");
			ICompilationUnit cu= pack1.createCompilationUnit("B.java", buf.toString(), false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 3);
			CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

			assertEqualString(proposal.getDisplayString(), "Change parameter 'a' to '@Nullable'");

			String preview= getPreviewContent(proposal);

			buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("import my.*;\n");
			buf.append("@NonNullByDefault\n");
			buf.append("public class B extends A {\n");
			buf.append("   @Override\n");
			buf.append("   public void SomeMethod(\n");
			buf.append("      String @Nullable [] a)\n");
			buf.append("   {\n");
			buf.append("\n");
			buf.append("   }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		} finally {
			JavaCore.setOptions(options);
		}
	}
	public void runBug531511Test(boolean useTypeAnnotations, String defaultNullnessAnnotations, boolean expectReturnAnnotation, boolean expectParamAnnotation) throws Exception {
		Map<String, String> options= fJProject1.getOptions(false);
		try {
			Hashtable<String, String> myOptions= new Hashtable<>(options);
			myOptions.put(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, "my.Nullable");
			myOptions.put(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, "my.NonNull");
			myOptions.put(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME, "my.NonNullByDefault");
			myOptions.put(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_SECONDARY_NAMES, "my.NNApi,my.NNFields,my.NNParams,my.NNReturn,my.NNBDBoolean,my.NNBDUnconfigurable");
			myOptions.put(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.DISABLED);
			fJProject1.setOptions(myOptions);

			IPackageFragment my= fSourceFolder.createPackageFragment("my", false, null);
			StringBuilder buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			if(useTypeAnnotations) {
				buf.append("import java.lang.annotation.ElementType;\n");
				buf.append("import java.lang.annotation.Target;\n");
				buf.append("@Target({ElementType.TYPE_USE})\n");
			}
			buf.append("public @interface Nullable {\n");
			buf.append("}\n");
			my.createCompilationUnit("Nullable.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("\n");
			if(useTypeAnnotations) {
				buf.append("import java.lang.annotation.ElementType;\n");
				buf.append("import java.lang.annotation.Target;\n");
				buf.append("@Target({ElementType.TYPE_USE})\n");
			}
			buf.append("public @interface NonNull {\n");
			buf.append("}\n");
			my.createCompilationUnit("NonNull.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("public enum DefaultLocation {\n");
			buf.append("	PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT\n");
			buf.append("}\n");
			buf.append("");
			my.createCompilationUnit("DefaultLocation.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("public @interface TypeQualifierDefault {\n");
			buf.append("	java.lang.annotation.ElementType[] value();\n");
			buf.append("}\n");
			buf.append("");
			my.createCompilationUnit("TypeQualifierDefault.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("import static my.DefaultLocation.*;\n");
			buf.append("\n");
			buf.append("public @interface NonNullByDefault {\n");
			buf.append("	DefaultLocation[] value() default { PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT };\n");
			buf.append("}\n");
			buf.append("");
			my.createCompilationUnit("NonNullByDefault.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("import java.lang.annotation.ElementType;\n");
			buf.append("\n");
			buf.append("@TypeQualifierDefault({ElementType.METHOD,ElementType.PARAMETER})\n");
			buf.append("public @interface NNApi {\n");
			buf.append("}\n");
			buf.append("");
			my.createCompilationUnit("NNApi.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("import java.lang.annotation.ElementType;\n");
			buf.append("\n");
			buf.append("@TypeQualifierDefault({ElementType.PARAMETER})\n");
			buf.append("public @interface NNParams {\n");
			buf.append("}\n");
			buf.append("");
			my.createCompilationUnit("NNParams.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("import java.lang.annotation.ElementType;\n");
			buf.append("\n");
			buf.append("@TypeQualifierDefault({ElementType.METHOD})\n");
			buf.append("public @interface NNReturn {\n");
			buf.append("}\n");
			buf.append("");
			my.createCompilationUnit("NNReturn.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("import java.lang.annotation.ElementType;\n");
			buf.append("\n");
			buf.append("@TypeQualifierDefault(ElementType.FIELD)\n");
			buf.append("public @interface NNFields {\n");
			buf.append("}\n");
			buf.append("");
			my.createCompilationUnit("NNFields.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("public @interface NNBDBoolean {\n");
			buf.append("boolean value() default true;\n");
			buf.append("}\n");
			buf.append("");
			my.createCompilationUnit("NNBDBoolean.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package my;\n");
			buf.append("\n");
			buf.append("public @interface NNBDUnconfigurable {\n");
			buf.append("}\n");
			buf.append("");
			my.createCompilationUnit("NNBDUnconfigurable.java", buf.toString(), false, null);


			IPackageFragment api= fSourceFolder.createPackageFragment("api", false, null);
			buf= new StringBuilder();
			buf.append("package api;\n");
			buf.append("import my.*;\n");
			buf.append("public interface I {\n");
			buf.append("   @NonNull public Object someMethod(@NonNull Object p);\n");
			buf.append("}\n");
			api.createCompilationUnit("I.java", buf.toString(), false, null);

			IPackageFragment test= fSourceFolder.createPackageFragment("test", false, null);
			buf= new StringBuilder();
			buf.append("@my.NonNullByDefault(my.DefaultLocation.TYPE_BOUND)\n"); // create irrelevant package default, so no tested combination will be redunant
			buf.append("package test;\n");
			test.createCompilationUnit("package-info.java", buf.toString(), false, null);

			// ensure the support classes are problem-free
			fJProject1.getProject().getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
			IMarker[] markers= fJProject1.getResource().findMarkers(null, true, IResource.DEPTH_INFINITE);
			assertEquals(0, markers.length);

			// actual test begins here
			buf= new StringBuilder();
			buf.append("package test;\n");
			buf.append("import my.*;\n");
			buf.append(defaultNullnessAnnotations+ "\n");
			buf.append("public class A implements api.I {\n");
			buf.append("}\n");
			ICompilationUnit cu= test.createCompilationUnit("A.java", buf.toString(), false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
			assertNumberOfProposals(proposals, 2);

			buf= new StringBuilder();
			buf.append("package test;\n");
			buf.append("import my.*;\n");
			buf.append(defaultNullnessAnnotations+ "\n");
			buf.append("public class A implements api.I {\n");
			buf.append("\n");
			buf.append("    @Override\n");
			if (useTypeAnnotations) {
				buf.append("    public " + (expectReturnAnnotation ? "@NonNull " : "")
						+ "Object someMethod(" + (expectParamAnnotation ? "@NonNull " : "")
						+ "Object p) {\n");
			} else {
				if (expectReturnAnnotation) {
					buf.append("    @NonNull\n");
				}
				buf.append("    public Object someMethod(" + (expectParamAnnotation ? "@NonNull " : "")
						+ "Object p) {\n");
			}
			buf.append("        return null;\n");
			buf.append("    }\n");
			buf.append("}\n");
			buf.append("");
			assertProposalPreviewEquals(buf.toString(), "Add unimplemented methods", proposals);
		} finally {
			fJProject1.setOptions(options);
		}
	}

	@Test
	public void testBug531511_none_type() throws Exception {
		runBug531511Test(true, "", true, true);
	}
	@Test
	public void testBug531511_none_decl() throws Exception {
		runBug531511Test(false, "", true, true);
	}
	@Test
	public void testBug531511_combined_multi_first_type() throws Exception {
		runBug531511Test(true, "@NNApi @NNFields", false, false);
	}
	@Test
	public void testBug531511_combined_multi_first_decl() throws Exception {
		runBug531511Test(false, "@NNApi @NNFields", false, false);
	}
	@Test
	public void testBug531511_combined_multi_second_type() throws Exception {
		runBug531511Test(true, "@NNApi @NNFields", false, false);
	}
	@Test
	public void testBug531511_combined_multi_second_decl() throws Exception {
		runBug531511Test(false, "@NNApi @NNFields", false, false);
	}
	@Test
	public void testBug531511_param_multi_first_type() throws Exception {
		runBug531511Test(true, "@NNParams @NNFields", true, false);
	}
	@Test
	public void testBug531511_param_multi_first_decl() throws Exception {
		runBug531511Test(false, "@NNParams @NNFields", true, false);
	}
	@Test
	public void testBug531511_param_multi_second_type() throws Exception {
		runBug531511Test(true, "@NNFields @NNParams", true, false);
	}
	@Test
	public void testBug531511_param_multi_second_decl() throws Exception {
		runBug531511Test(false, "@NNFields @NNParams", true, false);
	}
	@Test
	public void testBug531511_return_multi_first_type() throws Exception {
		runBug531511Test(true, "@NNReturn @NNFields", false, true);
	}
	@Test
	public void testBug531511_return_multi_first_decl() throws Exception {
		runBug531511Test(false, "@NNReturn @NNFields", false, true);
	}
	@Test
	public void testBug531511_return_multi_second_type() throws Exception {
		runBug531511Test(true, "@NNFields @NNReturn", false, true);
	}
	@Test
	public void testBug531511_return_multi_second_decl() throws Exception {
		runBug531511Test(false, "@NNFields @NNReturn", false, true);
	}
	@Test
	public void testBug531511_boolean_default_type() throws Exception {
		runBug531511Test(true, "@NNBDBoolean", false, false);
	}
	@Test
	public void testBug531511_boolean_default_decl() throws Exception {
		runBug531511Test(false, "@NNBDBoolean", false, false);
	}
	@Test
	public void testBug531511_boolean_true_type() throws Exception {
		runBug531511Test(true, "@NNBDBoolean(true)", false, false);
	}
	@Test
	public void testBug531511_boolean_true_decl() throws Exception {
		runBug531511Test(false, "@NNBDBoolean(true)", false, false);
	}
	@Test
	public void testBug531511_boolean_false_type() throws Exception {
		runBug531511Test(true, "@NNBDBoolean(false)", true, true);
	}
	@Test
	public void testBug531511_boolean_false_decl() throws Exception {
		runBug531511Test(false, "@NNBDBoolean(false)", true, true);
	}
	@Test
	public void testBug531511_unconfigurable_type() throws Exception {
		runBug531511Test(true, "@NNBDUnconfigurable", false, false);
	}
	@Test
	public void testBug531511_unconfigurable_decl() throws Exception {
		runBug531511Test(false, "@NNBDUnconfigurable", false, false);
	}
}
