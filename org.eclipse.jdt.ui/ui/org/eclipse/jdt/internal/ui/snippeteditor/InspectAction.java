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
 * Inspects a snippet
 *
 */
public class InspectAction extends SnippetAction implements IUpdate {
	
	public InspectAction(JavaSnippetEditor editor, String label) {
		super(editor, label);
		setDescription("Inspect the result of evaluating the selected code");
		setToolTipText("Inspect the selected code");
		setImageDescriptor(JavaPluginImages.DESC_TOOL_INSPSNIPPET);
	}
	
	/**
	 * The user has invoked this action.
	 */
	public void run() {
		fEditor.evalSelection(JavaSnippetEditor.RESULT_INSPECT);
	} 

	public void snippetStateChanged(JavaSnippetEditor editor) {
		setEnabled(editor != null && !editor.isEvaluating());
	}
	
	public void update() {
		ITextSelection selection= (ITextSelection)fEditor.getSelectionProvider().getSelection();
		setEnabled(selection.getLength() > 0);
	}

}
