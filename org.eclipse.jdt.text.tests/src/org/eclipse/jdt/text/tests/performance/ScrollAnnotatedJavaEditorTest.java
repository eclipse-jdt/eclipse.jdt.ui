/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class ScrollAnnotatedJavaEditorTest extends ScrollEditorTest {
	
	private static final Class THIS= ScrollAnnotatedJavaEditorTest.class;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected String getEditor() {
		return EditorTestHelper.COMPILATION_UNIT_EDITOR_ID;
	}

	protected void setUp(AbstractTextEditor editor) throws Exception {
		super.setUp(editor);
		createAnnotations(editor);
	}
	
	public void testScrollJavaEditorLineWise1() throws Exception {
		measure(LINE_WISE_NO_CARET_MOVE);
	}

	public void testScrollJavaEditorPageWise() throws Exception {
		measure(PAGE_WISE);
	}

	public void testScrollJavaEditorLineWiseMoveCaret1() throws Exception {
		measure(LINE_WISE);
	}
	
	public void testScrollJavaEditorLineWiseSelect1() throws Exception {
		measure(LINE_WISE_SELECT);
	}

	private void createAnnotations(IEditorPart editor) throws BadLocationException, JavaModelException {
		// produce a lot of annotations: rename all declarations
		ITextViewerExtension extension= null;
		JavaEditor javaEditor= (JavaEditor) editor;
		ISourceViewer viewer= javaEditor.getViewer();
		if (viewer instanceof ITextViewerExtension) {
			extension= (ITextViewerExtension) viewer;
			extension.getRewriteTarget().beginCompoundChange();
		}
		try {
			IDocument document= EditorTestHelper.getDocument((ITextEditor) editor);
			ICompilationUnit unit= JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(javaEditor.getEditorInput());
			IType[] allTypes= unit.getAllTypes();
			for (int i= 0; i < allTypes.length; i++) {
				IType type= allTypes[i];
				renameMemberDecls(document, type.getMethods());
				renameMemberDecls(document, type.getFields());
			}
		} finally {
			if (extension != null)
				extension.getRewriteTarget().endCompoundChange();
		}
		
	}

	private void renameMemberDecls(IDocument document, IMember[] members) throws JavaModelException, BadLocationException {
		for (int j= 0; j < members.length; j++) {
			IMember member= members[j];
			ISourceRange range= member.getNameRange();
			if (range != null)
				document.replace(range.getOffset(), 2, "XX");
		}
	}
}
