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

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class ScrollAnnotatedJavaEditorTest extends ScrollEditorTest {

	private static final String FILE= "org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private static final int N_OF_RUNS= 4;

	private IFile fFile;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		createAnnotations();

		EditorTestHelper.calmDown(1000, 20000, 100);
	}
	
	private void createAnnotations() throws BadLocationException, JavaModelException {
		// produce a lot of annotations: rename all declarations
		ITextViewerExtension ext= null;
		if (fEditor instanceof JavaEditor) {
			JavaEditor je= (JavaEditor) fEditor;
			ISourceViewer viewer= je.getViewer();
			if (viewer instanceof ITextViewerExtension) {
				ext= (ITextViewerExtension) viewer;
				ext.getRewriteTarget().beginCompoundChange();
			}
		}
		
		try {

			IDocument document= EditorTestHelper.getDocument((ITextEditor) fEditor);
			ICompilationUnit unit= JavaCore.createCompilationUnitFrom(getFile());
			IType[] allTypes= unit.getAllTypes();
			for (int i= 0; i < allTypes.length; i++) {
				IType type= allTypes[i];
				
				renameMemberDecls(document, type.getMethods());
				renameMemberDecls(document, type.getFields());
			}
		} finally {
			if (ext != null)
				ext.getRewriteTarget().endCompoundChange();
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

	protected void tearDown() throws Exception {
		EditorTestHelper.revertEditor((ITextEditor) fEditor, true);
		super.tearDown();
	}
	
	public void testScrollJavaEditorLineWise() {
		setScrollingMode(LINE_WISE_NO_CARET_MOVE);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollJavaEditorPageWise() {
		setScrollingMode(PAGE_WISE);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollJavaEditorLineWisePreloaded() {
		setScrollingMode(LINE_WISE_NO_CARET_MOVE);
		setPreloadEvents(true);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollJavaEditorPageWisePreloaded() {
		setScrollingMode(PAGE_WISE);
		setPreloadEvents(true);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollJavaEditorLineWiseMoveCaret() {
		setScrollingMode(LINE_WISE);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollJavaEditorLineWiseMoveCaretPreloaded() {
		setScrollingMode(LINE_WISE);
		setPreloadEvents(true);
		measureScrolling(N_OF_RUNS);
	}

	public void testScrollTextEditorLineWiseSelect1() {
		setScrollingMode(LINE_WISE_SELECT);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}
	
	public void testScrollTextEditorLineWiseSelect2() {
		setScrollingMode(LINE_WISE_SELECT);
		setPreloadEvents(false);
		measureScrolling(N_OF_RUNS);
	}

	protected IFile getFile() {
		if (fFile == null)
			fFile= ResourceTestHelper.findFile(FILE);
		return fFile;
	}
}
