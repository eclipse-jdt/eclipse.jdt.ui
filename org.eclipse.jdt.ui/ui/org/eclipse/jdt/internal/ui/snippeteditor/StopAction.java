package org.eclipse.jdt.internal.ui.snippeteditor;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Stops the VM used to run a snippet.
 *
 */
public class StopAction extends SnippetAction {

	public static final String PREFIX = "SnippetEditor.StopAction.";
	
	public StopAction(JavaSnippetEditor editor) {
		super(editor, PREFIX);
		setImageDescriptor(JavaPluginImages.DESC_TOOL_TERMSNIPPET);
	}
	
	/**
	 * The user has invoked this action.
	 */
	public void run() {
		fEditor.shutDownVM();
	}
	
	public void snippetStateChanged(JavaSnippetEditor editor) {
		setEnabled(editor != null && editor.isVMLaunched());
	}
}
