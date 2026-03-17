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
package org.eclipse.jdt.ui.tests.editor;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

import junit.framework.TestCase;

public class MarkdownTypingTest extends TestCase {
	private IJavaProject javaProject;
	private CompilationUnitEditor editor;

	@Override
	protected void setUp() throws Exception {
		javaProject= JavaProjectHelper.createJavaProject("MarkdownTypingTests", "bin");
		JavaProjectHelper.addSourceContainer(javaProject, "src");
	}

	@Override
	protected void tearDown() throws Exception {
		if (editor != null) {
	        editor.getSite().getPage().closeEditor(editor, false);
	        editor = null;
	    }

		if (javaProject != null) {
			JavaProjectHelper.delete(javaProject);
		}
	}

	private ICompilationUnit createCompilationUnit() throws Exception {
		IPackageFragment fragment= javaProject.findPackageFragment(
				javaProject.getProject().getFullPath().append("src"));

		String content= """
					/// markdown comment
					public class Test {}
				""";

		ICompilationUnit cu= fragment.createCompilationUnit(
				"Test.java", content, true, new NullProgressMonitor());

		javaProject.getProject().build(
				IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		return cu;
	}

	private CompilationUnitEditor openEditor(ICompilationUnit cu) throws Exception {
		IEditorPart part= EditorUtility.openInEditor(cu);
		assertNotNull(part);
		return (CompilationUnitEditor) part;
	}

	private void typeCharacter(CompilationUnitEditor editr, char c) throws Exception {
		IDocument doc = editr.getDocumentProvider().getDocument(editr.getEditorInput());

		var viewer = editr.getViewer();
		var textWidget = viewer.getTextWidget();

		int offset = doc.get().indexOf("markdown comment") + "markdown comment".length();
		textWidget.setCaretOffset(offset);

		org.eclipse.swt.widgets.Event event = new org.eclipse.swt.widgets.Event();
		event.character = c;
		event.doit = true;

		textWidget.notifyListeners(org.eclipse.swt.SWT.KeyDown, event);
	}

	public void testSingleQuoteTypingInMarkdown() throws Exception {
		ICompilationUnit cu= createCompilationUnit();
		editor = openEditor(cu);

		typeCharacter(editor, '\'');

		IDocument doc= editor.getDocumentProvider().getDocument(editor.getEditorInput());

		String expected= """
					/// markdown comment'
					public class Test {}
				""";
		assertEquals(expected, doc.get());
	}

	public void testDoubleQuoteTypingInMarkdown() throws Exception {
		ICompilationUnit cu= createCompilationUnit();
		editor = openEditor(cu);

		typeCharacter(editor, '"');

		IDocument doc= editor.getDocumentProvider().getDocument(editor.getEditorInput());

		String expected= """
					/// markdown comment"
					public class Test {}
				""";
		assertEquals(expected, doc.get());
	}

	public void testOpeningParenthesisTypingInMarkdown() throws Exception {
		ICompilationUnit cu= createCompilationUnit();
		editor = openEditor(cu);

		typeCharacter(editor, '(');

		IDocument doc= editor.getDocumentProvider().getDocument(editor.getEditorInput());

		String expected = """
					/// markdown comment(
					public class Test {}
				""";
		assertEquals(expected, doc.get());
	}

	public void testOpeningSquareBracketTypingInMarkdown() throws Exception {
		ICompilationUnit cu= createCompilationUnit();
		editor = openEditor(cu);

		typeCharacter(editor, '[');

		IDocument doc= editor.getDocumentProvider().getDocument(editor.getEditorInput());

		String expected = """
					/// markdown comment[
					public class Test {}
				""";
		assertEquals(expected, doc.get());
	}
}
