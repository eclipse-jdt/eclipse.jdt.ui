package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.debug.core.model.IValue;

import org.eclipse.jdt.debug.core.IJavaEvaluationResult;

/**
 * Displays the result of an evaluation in the java editor
 */

public class DisplayAction extends EvaluateAction {

	public DisplayAction(ResourceBundle bundle, String prefix, JavaEditor editor) {
		super(bundle, prefix, editor);
	}
	
	public void evaluationComplete(final IJavaEvaluationResult res) {
		final IValue value= res.getValue();
		if (value != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					insertResult(res);
				}
			});
		}
	}
	void insertResult(IJavaEvaluationResult result) {
		IValue value = result.getValue();
		String resultString = null;
		if (value == null)
			resultString= "(No explicit return value)";
		else
			resultString= " (" + value.getReferenceTypeName() + ") " + value.getValueString();
		int start= fSelection.getOffset();
		int end= start + fSelection.getLength();
		IDocument document= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());

		try {
			document.replace(end, 0, resultString);
		} catch (BadLocationException e) {
		}
		//fEditor.getSelectionProvider().setSelection(new TextSelection(document, end, resultString.length()));
	}
}
