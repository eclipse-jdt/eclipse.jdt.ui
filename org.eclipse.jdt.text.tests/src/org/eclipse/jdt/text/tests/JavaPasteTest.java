/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class JavaPasteTest {

	private static final int SRC_START_LINE= 5;

	private AbstractTextEditor fEditor;

	private IJavaProject fJavaProject;
	private IJavaProject fJavaProject2;

	@Test
	public void testTabsToSpaces() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2546
		fJavaProject= JavaProjectHelper.createJavaProject("P", "bin");

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		IPackageFragment pack= root.createPackageFragment("testA.testB", true, null);

		ICompilationUnit cu= pack.getCompilationUnit("A.java");
		IType type= cu.createType("public class A {\n}\n", null, true, null);
		type.createMethod("public void a() {\n\tint a = 3;\n}\n", null, true, null);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_SMART_PASTE, true);
		store.setValue(PreferenceConstants.EDITOR_IMPORTS_ON_PASTE, true);
		fJavaProject.setOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		fJavaProject.setOption(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");

		fJavaProject2= JavaProjectHelper.createJavaProject("Q", "bin");

		IPackageFragmentRoot root2= JavaProjectHelper.addSourceContainer(fJavaProject2, "src");
		IPackageFragment pack2= root2.createPackageFragment("testA", true, null);

		ICompilationUnit cu2= pack2.getCompilationUnit("B.java");
		IType type2= cu2.createType("public class B {\n}\n", null, true, null);
		type2.createMethod("public void a() {\n    String x = \"\";\n}\n", null, true, null);
		fJavaProject2.setOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		fJavaProject2.setOption(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");

		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("/Q/src/testA/B.java"));
		fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(file, true);
		IDocument document= EditorTestHelper.getDocument(fEditor);
		copyToClipboard(SRC_START_LINE);
		String buffer= document.get();
		String find= "String x = ";
		int index= buffer.indexOf(find);
		performPaste(index + find.length() + 1); // copy after opening quote of string
		// verify tabs were not changed to spaces when pasted in a string
		buffer= document.get();
		String expectedString= "String x = \"\t\tint a = 3;\";";
		String s= buffer.substring(index, index + expectedString.length());
		assertEquals(expectedString, s);
		int newlineIndex= buffer.substring(index).indexOf("\n");
		int nextLineIndex= index + newlineIndex + 1;
		performPaste(nextLineIndex); // copy after statement
		// verify tabs were changed to spaces in second paste
		buffer= document.get();
		String expectedString2= "        int a = 3;";
		String s2= buffer.substring(nextLineIndex, nextLineIndex + expectedString2.length());
		assertEquals(expectedString2, s2);
		EditorTestHelper.closeAllEditors();
		store.setToDefault(PreferenceConstants.EDITOR_SMART_PASTE);
		store.setToDefault(PreferenceConstants.EDITOR_IMPORTS_ON_PASTE);
		if (fJavaProject != null)
			JavaProjectHelper.delete(fJavaProject);

		if (fJavaProject2 != null)
			JavaProjectHelper.delete(fJavaProject2);
	}

	private void copyToClipboard(int startLine) throws Exception {
		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("/P/src/testA/testB/A.java"));
		ITextEditor editor= (ITextEditor) EditorTestHelper.openInEditor(file, true);
		IDocument document= EditorTestHelper.getDocument(editor);
		int offset= document.getLineOffset(startLine);
		editor.selectAndReveal(offset, document.getLineOffset(startLine + 1) - offset - 1);
		runAction(editor.getAction(ITextEditorActionConstants.COPY));
		EditorTestHelper.closeEditor(editor);
	}

	private void performPaste(int destOffset) throws Exception {
		IDocument document= EditorTestHelper.getDocument(fEditor);
		IAction paste= fEditor.getAction(ITextEditorActionConstants.PASTE);
		dirty(document);
		fEditor.selectAndReveal(destOffset, 0);
		EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(fEditor), 0, 10000, 100);
		runAction(paste);
	}

	private void dirty(IDocument document) throws BadLocationException {
		document.replace(0, 0, " ");
		document.replace(0, 1, "");
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}
