/*******************************************************************************
 * Copyright (c) 2026 Luise Ravnskjaer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse contributors - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.action.IAction;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Tests that imports on paste are only added when inserting into code, not into strings or text
 * blocks (https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1716) or comments
 * (https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/3275).
 */
class JavaPasteWithImportsTest {

	private static final String COPY_SOURCE= """
			package p;
			import other.Foo;
			public class A {
				void a() {
					Foo f= null;
				}
			}
			""";

	private static final String COPY_SOURCE_REFERENCED_TYPE= """
			package other;
			public class Foo {}
			""";

	private static final String PASTE_TARGET= """
			package p;
			public class B {
				String string= "string literal";
				String textBlock= \"""
						text block
						\""";
				// line comment
				void line() {
				}
				/* block comment */
				void block() {
				}
				/** javadoc comment */
				void javadoc() {
				}
				/// markdown comment
				void markdown() {
				}
			}
			""";

	private static final String CODE_TO_COPY= "Foo f= null;";

	private static final String CODE_TO_COPY_IMPORT= "import other.Foo;";

	private IJavaProject project;

	@BeforeEach
	void setUp() throws Exception {
		project= JavaProjectHelper.createJavaProject("P", "bin");
		JavaProjectHelper.addRTJar_17(project, false);
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(project, "src");
		root.createPackageFragment("other", true, null).createCompilationUnit("Foo.java", COPY_SOURCE_REFERENCED_TYPE, true, null);
		IPackageFragment pack= root.createPackageFragment("p", true, null);
		pack.createCompilationUnit("A.java", COPY_SOURCE, true, null);
		pack.createCompilationUnit("B.java", PASTE_TARGET, true, null);
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_IMPORTS_ON_PASTE, true);
	}

	@AfterEach
	void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
		JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.EDITOR_IMPORTS_ON_PASTE);
		if (project != null) {
			JavaProjectHelper.delete(project);
		}
	}

	@ParameterizedTest(name= "method after {0} comment")
	@ValueSource(strings= { "line", "block", "javadoc", "markdown" })
	void testImportsAddedWhenPastingIntoCode(String kind) throws Exception {
		AbstractTextEditor sourceEditor= openFileInEditor("/P/src/p/A.java");
		copyToClipboard(sourceEditor, CODE_TO_COPY);

		AbstractTextEditor targetEditor= openFileInEditor("/P/src/p/B.java");
		pasteAfter(targetEditor, "void " + kind + "() {");

		IDocument document= EditorTestHelper.getDocument(targetEditor);
		assertTrue(document.get().contains(CODE_TO_COPY), document.get());
		assertTrue(document.get().contains(CODE_TO_COPY_IMPORT), document.get());
	}

	@ParameterizedTest(name= "{0}")
	@ValueSource(strings= { "string literal", "text block" })
	void testNoImportsAddedWhenPastingIntoString(String kind) throws Exception {
		AbstractTextEditor sourceEditor= openFileInEditor("/P/src/p/A.java");
		copyToClipboard(sourceEditor, CODE_TO_COPY);

		AbstractTextEditor targetEditor= openFileInEditor("/P/src/p/B.java");
		pasteAfter(targetEditor, kind);

		IDocument document= EditorTestHelper.getDocument(targetEditor);
		assertTrue(document.get().contains(CODE_TO_COPY), document.get());
		assertFalse(document.get().contains(CODE_TO_COPY_IMPORT), document.get());
	}

	@ParameterizedTest(name= "{0} comment")
	@ValueSource(strings= { "line", "block", "javadoc", "markdown" })
	void testNoImportsAddedWhenPastingIntoComment(String kind) throws Exception {
		AbstractTextEditor sourceEditor= openFileInEditor("/P/src/p/A.java");
		copyToClipboard(sourceEditor, CODE_TO_COPY);

		AbstractTextEditor targetEditor= openFileInEditor("/P/src/p/B.java");
		pasteAfter(targetEditor, kind + " comment");

		IDocument document= EditorTestHelper.getDocument(targetEditor);
		assertTrue(document.get().contains(CODE_TO_COPY), document.get());
		assertFalse(document.get().contains(CODE_TO_COPY_IMPORT), document.get());
	}

	private static AbstractTextEditor openFileInEditor(String path) throws Exception {
		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
		AbstractTextEditor editor= (AbstractTextEditor) EditorTestHelper.openInEditor(file, true);
		assertTrue(EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(editor), 0, 10000, 100));
		return editor;
	}

	private static void copyToClipboard(AbstractTextEditor editor, String text) {
		editor.selectAndReveal(offsetOf(editor, text), text.length());
		runAction(editor.getAction(ITextEditorActionConstants.COPY));
	}

	private static void pasteAfter(AbstractTextEditor editor, String text) {
		int offset= offsetOf(editor, text) + text.length();
		editor.selectAndReveal(offset, 0);
		runAction(editor.getAction(ITextEditorActionConstants.PASTE));
	}

	private static int offsetOf(AbstractTextEditor editor, String text) {
		String content= EditorTestHelper.getDocument(editor).get();
		int offset= content.indexOf(text);
		assertTrue(offset >= 0 && offset == content.lastIndexOf(text), "expected exactly one occurrence of: " + text);
		return offset;
	}

	private static void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}
