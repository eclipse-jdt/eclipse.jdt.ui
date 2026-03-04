/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.IClasspathEntry;
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
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class SuppressWarningsQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.DO_NOT_INSERT);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE, DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_NO_SPLIT));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD, JavaCore.ERROR);


		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fJProject1, cpe);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testMissingSuppressWarningsAnnotation() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNCLOSED_CLOSEABLE, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_POTENTIALLY_UNCLOSED_CLOSEABLE, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS, JavaCore.ENABLED);
		JavaCore.setOptions(options);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String str= """
			package test1;

			import java.io.Closeable;
			import java.io.IOException;

			class A implements Closeable {
				@Override
				public void close() throws IOException {
				}
			}
			class B {
				public static Closeable getClosable() {
					return new A();
				}
			}
			public class C {
				public void foo() {
					B.getClosable();
					B.getClosable();
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;

			import java.io.Closeable;
			import java.io.IOException;

			class A implements Closeable {
				@Override
				public void close() throws IOException {
				}
			}
			class B {
				public static Closeable getClosable() {
					return new A();
				}
			}
			public class C {
				@SuppressWarnings("resource")
			    public void foo() {
					B.getClosable();
					B.getClosable();
				}
			}
			""";
		assertEqualString(preview, str1);
	}

}
