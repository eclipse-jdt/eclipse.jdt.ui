/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.snippeteditor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.ui.texteditor.IUpdate;

/**
 * Inspects a snippet
 *
 */
public class InspectAction extends SnippetAction implements IUpdate {

	public static final String PREFIX = "SnippetEditor.InspectAction.";
	
	public InspectAction(JavaSnippetEditor editor) {
		super(editor, PREFIX);
		setImageDescriptor(JavaPluginImages.DESC_TOOL_INSPSNIPPET);
	}
	
	/**
	 * The user has invoked this action.
	 */
	public void run() {
		fEditor.evalSelection(JavaSnippetEditor.RESULT_INSPECT);
	} 
}
