/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Open a test in the Java editor and reveal a given line
 */
public class OpenEditorAtLineAction extends OpenEditorAction {
	
	private int fLineNumber;
	
	/**
	 * Constructor for OpenEditorAtLineAction.
	 */
	public OpenEditorAtLineAction(TestRunnerViewPart testRunner, String className, int line) {
		super(testRunner, className);
		WorkbenchHelp.setHelp(this, IJUnitHelpContextIds.OPENEDITORATLINE_ACTION);
		fLineNumber= line;
	}
		
	protected void reveal(ITextEditor textEditor) {
		if (fLineNumber >= 0) {
			try {
				IDocument document= textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
				textEditor.selectAndReveal(document.getLineOffset(fLineNumber-1), document.getLineLength(fLineNumber-1));
			} catch (BadLocationException x) {
				// marker refers to invalid text position -> do nothing
			}
		}
	}
	
	protected IJavaElement findElement(IJavaProject project, String className) throws JavaModelException {
		return project.findType(className);
	}

	public boolean isEnabled() {
		try {
			return getLaunchedProject().findType(getClassName()) != null;
		} catch (JavaModelException e) {
		}
		return false;
	}
}
