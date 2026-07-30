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
package org.eclipse.jdt.ui.tests.core.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java23ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class GetTypeCommentMarkdownTest {

	private static final String MARKDOWN_TYPECOMMENT_TEMPLATE= "/// ${type_name}" + "\n/// ${tags}";
	@Rule
	public ProjectTestSetup pts= new Java23ProjectTestSetup(false);

	private IJavaProject fJavaProject;
	private IPackageFragmentRoot fSourceFolder;
	private IPackageFragment fPackageP;

	@Before
	public void setUp() throws CoreException {
		fJavaProject= pts.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackageP= fSourceFolder.createPackageFragment("p", true, null);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_USE_MARKDOWN, true);
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, true);

		StubUtility.setCodeTemplate(CodeTemplateContextType.MARKDOWNTYPECOMMENT_ID, MARKDOWN_TYPECOMMENT_TEMPLATE, null);
	}

	@After
	public void tearDown() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_USE_MARKDOWN, false);
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		fJavaProject= null;
		fPackageP= null;
	}

	@Test
	public void testNoTagsStripsTrailingMarkdownLine() throws Exception {
		ICompilationUnit cu= fPackageP.createCompilationUnit("A.java",
				"package p;\n\npublic class A {\n}\n", true, null);

		String comment= StubUtility.getTypeComment(cu, "p.A", new String[0], new String[0], "\n");

		assertEquals("/// A", comment);
	}

	@Test
	public void testTypeParameterTagsNotStripped() throws Exception {
		ICompilationUnit cu= fPackageP.createCompilationUnit("B.java",
				"package p;\n\npublic class B<T> {\n}\n", true, null);

		String comment= StubUtility.getTypeComment(cu, "p.B", new String[] { "T" }, new String[0], "\n");

		assertTrue("expected type-name line", comment.startsWith("/// B\n"));
		assertTrue("expected @param <T> tag to be preserved", comment.contains("@param <T>"));
	}

	@Test
	public void testRecordComponentParamsNotStripped() throws Exception {
		ICompilationUnit cu= fPackageP.createCompilationUnit("R.java",
				"package p;\n\npublic record R(String x, int y) {\n}\n", true, null);

		String comment= StubUtility.getTypeComment(cu, "p.R", new String[0], new String[] { "x", "y" }, "\n");

		assertTrue("expected type-name line", comment.startsWith("/// R\n"));
		assertTrue("missing @param tag for record component x", comment.contains("@param x"));
		assertTrue("missing @param tag for record component y", comment.contains("@param y"));
	}
}