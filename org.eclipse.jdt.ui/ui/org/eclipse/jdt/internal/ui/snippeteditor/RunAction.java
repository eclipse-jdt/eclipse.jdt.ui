package org.eclipse.jdt.internal.ui.snippeteditor;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.internal.ui.JavaPluginImages;


/**
 * Runs a snippet
 *
 */
public class RunAction extends SnippetAction implements IUpdate {
	
	public RunAction(JavaSnippetEditor editor, String label) {
		super(editor, label);
		setDescription("Run the selected code");
		setToolTipText("Run the selected code");
		setImageDescriptor(JavaPluginImages.DESC_TOOL_RUNSNIPPET);
	}
	
	/**
	 * The user has invoked this action.
	 */
	public void run() {
		fEditor.evalSelection(JavaSnippetEditor.RESULT_RUN);
	} 

	public void snippetStateChanged(JavaSnippetEditor editor) {
		setEnabled(editor != null && !editor.isEvaluating());
	}
	
	public void update() {
		ITextSelection selection= (ITextSelection)fEditor.getSelectionProvider().getSelection();
		setEnabled(selection.getLength() > 0);
	}

}
