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
 * Displays the result of a snippet
 *
 */
public class DisplayAction extends SnippetAction implements IUpdate {

	public DisplayAction(JavaSnippetEditor editor, String label) {
		super(editor, label);
		setToolTipText("Display");
		setDescription("Display the result of running the selected code");
		setImageDescriptor(JavaPluginImages.DESC_TOOL_DISPLAYSNIPPET);
	}
	
	public void run() {
		fEditor.evalSelection(JavaSnippetEditor.RESULT_DISPLAY);
	} 
	
	public void snippetStateChanged(JavaSnippetEditor editor) {
		setEnabled(editor != null && !editor.isEvaluating());
	}

	public void update() {
		ITextSelection selection= (ITextSelection)fEditor.getSelectionProvider().getSelection();
		setEnabled(selection.getLength() > 0);
	}
}
