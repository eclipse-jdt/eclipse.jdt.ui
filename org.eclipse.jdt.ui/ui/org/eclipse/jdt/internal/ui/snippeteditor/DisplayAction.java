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

	public DisplayAction(JavaSnippetEditor editor) {
		super(editor);
		setImageDescriptor(JavaPluginImages.DESC_TOOL_DISPLAYSNIPPET);

		setText(SnippetMessages.getString("DisplayAction.label")); //$NON-NLS-1$
		setToolTipText(SnippetMessages.getString("DisplayAction.tooltip")); //$NON-NLS-1$
		setDescription(SnippetMessages.getString("DisplayAction.description")); //$NON-NLS-1$
	}
	
	public void run() {
		fEditor.evalSelection(JavaSnippetEditor.RESULT_DISPLAY);
	} 
}
