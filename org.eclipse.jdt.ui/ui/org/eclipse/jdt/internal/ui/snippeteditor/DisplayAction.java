/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.snippeteditor;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Displays the result of a snippet
 *
 */
public class DisplayAction extends SnippetAction implements IUpdate {

	public static final String PREFIX = "SnippetEditor.DisplayAction.";

	public DisplayAction(JavaSnippetEditor editor) {
		super(editor, PREFIX);
		setImageDescriptor(JavaPluginImages.DESC_TOOL_DISPLAYSNIPPET);
	}
	
	public void run() {
		fEditor.evalSelection(JavaSnippetEditor.RESULT_DISPLAY);
	} 
}
