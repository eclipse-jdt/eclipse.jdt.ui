/*******************************************************************************
 * Copyright (c) 2017 GK Software AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

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

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import junit.framework.Test;
import junit.framework.TestSuite;

public class NullAnnotationsQuickFixTest18 extends QuickFixTest {

	private static final Class<NullAnnotationsQuickFixTest18> THIS= NullAnnotationsQuickFixTest18.class;
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private String ANNOTATION_JAR_PATH;

	public NullAnnotationsQuickFixTest18(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java18ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
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

		fJProject1= Java18ProjectTestSetup.getProject();

		if (this.ANNOTATION_JAR_PATH == null) {
			String version= "[2.0.0,3.0.0)"; // tests run at 1.8, need the "new" null annotations
			Bundle[] bundles= Platform.getBundles("org.eclipse.jdt.annotation", version);
			File bundleFile= FileLocator.getBundleFile(bundles[0]);
			if (bundleFile.isDirectory())
				this.ANNOTATION_JAR_PATH= bundleFile.getPath() + "/bin";
			else
				this.ANNOTATION_JAR_PATH= bundleFile.getPath();
		}
		JavaProjectHelper.addLibrary(fJProject1, new Path(ANNOTATION_JAR_PATH));

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	/*
	 * Problem: package default nonnull conflicts with inherited nullable (via generic substitution) (implicit vs. implicit)
	 * Location: method return
	 * Fixes:
	 * - change local to nullable (equal)
	 * - change local to nonnull  (covariant return)
	 * - change super to nonnull  (equal)
	 */
	public void testBug499716_a() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.ENABLED);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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
	public void testBug499716_b() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.ENABLED);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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
	public void testBug499716_c() throws Exception {
		fJProject1.setOption(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS, JavaCore.ENABLED);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuffer buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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
	public void testBug499716_d() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);
		
		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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
	public void test443146a() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);
		
		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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
	public void test443146b() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);
		
		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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
	public void test443146c() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);
		
		buf= new StringBuffer();
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
	
		buf= new StringBuffer();
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
	
		buf= new StringBuffer();
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
	
		buf= new StringBuffer();
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
	public void test443146d() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
	
		buf= new StringBuffer();
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
	public void test443146e() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);
		
		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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
	public void test443146f() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("@org.eclipse.jdt.annotation.NonNullByDefault\n");
		buf.append("package test1;\n");
		pack1.createCompilationUnit("package-info.java", buf.toString(), true, null);
		
		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

		buf= new StringBuffer();
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
	public void testBug513682() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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

		buf= new StringBuffer();
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
}
