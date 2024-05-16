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
		String str= """
			module annots {
			     exports annots;\s
			}
			""";
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment annots= java9Src.createPackageFragment("annots", false, null);
		String str1= """
			package annots;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			@Target(ElementType.TYPE_USE)
			public @interface Nullable {
			}
			""";
		annots.createCompilationUnit("Nullable.java", str1, false, null);

		String str2= """
			package annots;
			
			import java.lang.annotation.ElementType;
			import java.lang.annotation.Target;
			
			@Target(ElementType.TYPE_USE)
			public @interface NonNull {
			}
			""";
		annots.createCompilationUnit("NonNull.java", str2, false, null);

		String str3= """
			package annots;
			
			public enum DefaultLocation {
				PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT
			}
			""";
		annots.createCompilationUnit("DefaultLocation.java", str3, false, null);

		String str4= """
			package annots;
			
			import static annots.DefaultLocation.*;
			
			public @interface NonNullByDefault {
				DefaultLocation[] value() default { PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT };
			}
			""";
		annots.createCompilationUnit("NonNullByDefault.java", str4, false, null);

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
		String str= """
			@annots.NonNullByDefault module test {
			 requires annots;\
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str1= """
			package test1;
			import java.util.Map;
			import annots.*;
			
			abstract class Test {
				abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();
			
				public void g() {
					x=f();
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Create field 'x'");

		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import java.util.Map;
			import annots.*;
			
			abstract class Test {
				private Map<? extends Map<String, @Nullable Integer>, String[][]> x;
			
			    abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();
			
				public void g() {
					x=f();
				}
			}
			""";
		assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Create parameter 'x'");

		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			import java.util.Map;
			import annots.*;
			
			abstract class Test {
				abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();
			
				public void g(Map<? extends Map<String, @Nullable Integer>, String[][]> x) {
					x=f();
				}
			}
			""";
		assertEqualString(preview, str3);

		proposal= (CUCorrectionProposal)proposals.get(2);

		assertEqualString(proposal.getDisplayString(), "Create local variable 'x'");

		preview= getPreviewContent(proposal);

		String str4= """
			package test1;
			import java.util.Map;
			import annots.*;
			
			abstract class Test {
				abstract Map<? extends Map<String, @Nullable Integer>, String[][]> f();
			
				public void g() {
					Map<? extends Map<String, @Nullable Integer>, String[][]> x = f();
				}
			}
			""";
		assertEqualString(preview, str4);
	}
	@Test
	public void testBug530580b() throws Exception {
		String str= """
			@annots.NonNullByDefault module test {
			 requires annots;\
			}
			""";
		IPackageFragment def= fSourceFolder.createPackageFragment("", false, null);
		def.createCompilationUnit("module-info.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str1= """
			package test1;
			import annots.*;
			
			interface Type {
				void set(@Nullable String s);
			
				class U implements Type {
					@Override
					public void set(String t) {
					}
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);

		assertEqualString(proposal.getDisplayString(), "Change parameter 't' to '@Nullable'");

		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			import annots.*;
			
			interface Type {
				void set(@Nullable String s);
			
				class U implements Type {
					@Override
					public void set(@Nullable String t) {
					}
				}
			}
			""";
		assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal)proposals.get(1);

		assertEqualString(proposal.getDisplayString(), "Change parameter in overridden 'set(..)' to '@NonNull'");

		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			import annots.*;
			
			interface Type {
				void set(String s);
			
				class U implements Type {
					@Override
					public void set(String t) {
					}
				}
			}
			""";
		assertEqualString(preview, str3);
	}}
