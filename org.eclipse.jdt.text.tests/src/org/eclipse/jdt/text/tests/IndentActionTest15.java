/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;

import java.util.ListResourceBundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestName;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;
import org.eclipse.jdt.text.tests.performance.ResourceTestHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.ui.actions.IndentAction;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 *
 * @since 3.2
 */
public class IndentActionTest15 {
	@Rule
	public TestName tn= new TestName();

	private static final String PROJECT= "IndentTests";

	private final static class IndentTestSetup extends ExternalResource {
		private IJavaProject fJavaProject;

		@Override
		protected void before() throws Exception {
			fJavaProject= EditorTestHelper.createJavaProject15(PROJECT, "testResources/indentation15");
			fJavaProject.setOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
			fJavaProject.setOption(DefaultCodeFormatterConstants.FORMATTER_INDENT_EMPTY_LINES, DefaultCodeFormatterConstants.FALSE);
			fJavaProject.setOption(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN, DefaultCodeFormatterConstants.TRUE);
			fJavaProject.setOption(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, DefaultCodeFormatterConstants.TRUE);
		}

		@Override
		protected void after () {
			if (fJavaProject != null)
				try {
					JavaProjectHelper.delete(fJavaProject);
				} catch (CoreException e) {
					e.printStackTrace();
				}
		}

		public IJavaProject getProject() {
			IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT);
			return JavaCore.create(project);
		}
	}

	private static final class EmptyBundle extends ListResourceBundle {
		@Override
		protected Object[][] getContents() {
			return new Object[0][];
		}
	}

	@Rule
	public IndentTestSetup indentTestSetup=new IndentTestSetup();

	private JavaEditor fEditor;
	private SourceViewer fSourceViewer;
	private IDocument fDocument;

	@Before
	public void setUp() throws Exception {
		String filename= createFileName("Before");
		fEditor= (JavaEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(filename), true);
		fSourceViewer= EditorTestHelper.getSourceViewer(fEditor);
		fDocument= fSourceViewer.getDocument();
	}

	@After
	public void tearDown() throws Exception {
		EditorTestHelper.closeEditor(fEditor);
		fEditor= null;
		fSourceViewer= null;
	}

	private void assertIndentResult() throws Exception {
		String afterFile= createFileName("Modified");
		String expected= ResourceTestHelper.read(afterFile).toString();

		new IndentAction(new EmptyBundle(), "prefix", fEditor, false).run();

		assertEquals(expected, fDocument.get());
	}

	private String createFileName(String qualifier) {
		String name= tn.getMethodName();
		name= name.substring(4, 5).toLowerCase() + name.substring(5);
		return "/" + PROJECT + "/src/" + name + "/" + qualifier + ".java";
	}

	private void selectAll() {
		fSourceViewer.setSelectedRange(0, fDocument.getLength());
	}


	@Test
	public void testIssue30_1() throws Exception {
		IJavaProject project= indentTestSetup.getProject();
		String value= project.getOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, true);
		project.setOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, Integer.toString(DefaultCodeFormatterConstants.INDENT_BY_ONE));
		try {
			selectAll();
			assertIndentResult();
		} finally {
			project.setOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, value);
		}
	}

	@Test
	public void testIssue30_2() throws Exception {
		IJavaProject project= indentTestSetup.getProject();
		String value= project.getOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, true);
		project.setOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, Integer.toString(DefaultCodeFormatterConstants.INDENT_DEFAULT));
		try {
			selectAll();
			assertIndentResult();
		} finally {
			project.setOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, value);
		}
	}

	@Test
	public void testIssue30_3() throws Exception {
		IJavaProject project= indentTestSetup.getProject();
		String value= project.getOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, true);
		project.setOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, Integer.toString(DefaultCodeFormatterConstants.INDENT_ON_COLUMN));
		try {
			selectAll();
			assertIndentResult();
		} finally {
			project.setOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, value);
		}
	}

	@Test
	public void testIssue30_4() throws Exception {
		IJavaProject project= indentTestSetup.getProject();
		String value= project.getOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, true);
		project.setOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, Integer.toString(DefaultCodeFormatterConstants.INDENT_PRESERVE));
		try {
			selectAll();
			assertIndentResult();
		} finally {
			project.setOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, value);
		}
	}
}
