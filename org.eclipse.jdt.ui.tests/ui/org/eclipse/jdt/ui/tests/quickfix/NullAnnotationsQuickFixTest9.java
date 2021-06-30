/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.ClasspathAttribute;

import org.eclipse.jdt.ui.tests.core.rules.Java9ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

public class NullAnnotationsQuickFixTest9 extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new Java9ProjectTestSetup();

	private IJavaProject fJProject1;

	private IJavaProject fJProject2;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws CoreException {
		fJProject2= JavaProjectHelper.createJavaProject("annots", "bin");
		JavaProjectHelper.set9CompilerOptions(fJProject2);
		JavaProjectHelper.addRTJar9(fJProject2);

		IPackageFragmentRoot java9Src= JavaProjectHelper.addSourceContainer(fJProject2, "src");
		IPackageFragment def= java9Src.createPackageFragment("", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("module annots {\n");
		buf.append("     exports annots; \n");
		buf.append("}\n");
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment annots= java9Src.createPackageFragment("annots", false, null);
		buf= new StringBuilder();
		buf.append("package annots;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("public @interface Nullable {\n");
		buf.append("}\n");
		annots.createCompilationUnit("Nullable.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package annots;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.ElementType;\n");
		buf.append("import java.lang.annotation.Target;\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE)\n");
		buf.append("public @interface NonNull {\n");
		buf.append("}\n");
		annots.createCompilationUnit("NonNull.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package annots;\n");
		buf.append("\n");
		buf.append("public enum DefaultLocation {\n");
		buf.append("	PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT\n");
		buf.append("}\n");
		buf.append("");
		annots.createCompilationUnit("DefaultLocation.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package annots;\n");
		buf.append("\n");
		buf.append("import static annots.DefaultLocation.*;\n");
		buf.append("\n");
		buf.append("public @interface NonNullByDefault {\n");
		buf.append("	DefaultLocation[] value() default { PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT };\n");
		buf.append("}\n");
		buf.append("");
		annots.createCompilationUnit("NonNullByDefault.java", buf.toString(), false, null);

		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.set9CompilerOptions(fJProject1);
		JavaProjectHelper.addRTJar9(fJProject1);

		Map<String, String> options= fJProject1.getOptions(false);
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, "annots.Nullable");
		options.put(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, "annots.NonNull");
		options.put(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME, "annots.NonNullByDefault");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		fJProject1.setOptions(options);

		IClasspathAttribute[] attributes= { new ClasspathAttribute(IClasspathAttribute.MODULE, "true") };
		IClasspathEntry cpe= JavaCore.newProjectEntry(fJProject2.getProject().getFullPath(), null, false, attributes, false);
		JavaProjectHelper.addToClasspath(fJProject1, cpe);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}
		if (fJProject2 != null) {
			JavaProjectHelper.delete(fJProject2);
		}
	}

	@Test
	public void testBug530580a() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("@annots.NonNullByDefault module test {\n");
		buf.append(" requires annots;");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import annots.*;\n");
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
		buf.append("import annots.*;\n");
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
		buf.append("import annots.*;\n");
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
		buf.append("import annots.*;\n");
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
	@Test
	public void testBug530580b() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("@annots.NonNullByDefault module test {\n");
		buf.append(" requires annots;");
		buf.append("}\n");
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import annots.*;\n");
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
		buf.append("import annots.*;\n");
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
		buf.append("import annots.*;\n");
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
	}}
