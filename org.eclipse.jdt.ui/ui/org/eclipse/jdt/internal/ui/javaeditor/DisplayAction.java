package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import java.util.ResourceBundle;import org.eclipse.debug.core.DebugException;import org.eclipse.debug.core.model.IValue;import org.eclipse.jdt.debug.core.IJavaEvaluationResult;import org.eclipse.jdt.debug.core.IJavaValue;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jface.text.BadLocationException;import org.eclipse.jface.text.IDocument;import org.eclipse.swt.widgets.Display;

/**
 * Displays the result of an evaluation in the java editor
 */

public class DisplayAction extends EvaluateAction {

	public DisplayAction(ResourceBundle bundle, String prefix, JavaEditor editor) {
		super(bundle, prefix, editor);
	}
	
	public void evaluationComplete(final IJavaEvaluationResult res) {
		final IJavaValue value= res.getValue();
		if (value != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					insertResult(value);
				}
			});
		}
	}
	void insertResult(IJavaValue result) {
		StringBuffer resultString= new StringBuffer();
		try {
			String sig= result.getSignature();
			if ("V".equals(sig)) {
				resultString.append(' ');
				resultString.append(getErrorResourceString("noreturn"));
			} else {
				if (sig != null) {
					resultString.append(" (");
					resultString.append(result.getReferenceTypeName());
					resultString.append(") ");
				} else {
					resultString.append(' ');
				}  
				resultString.append(result.evaluateToString());
			}
		} catch(DebugException e) {
			ExceptionHandler.handle(e, JavaPlugin.getActiveWorkbenchShell(), getErrorResourceString("errordialogtitle"), "");
		}
			
		int start= fSelection.getOffset();
		int end= start + fSelection.getLength();
		IDocument document= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());

		try {
			document.replace(end, 0, resultString.toString());
		} catch (BadLocationException e) {
		}
	}
}
