package org.eclipse.jdt.internal.ui.snippeteditor;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.ui.texteditor.IUpdate;


/**
 * Runs a snippet
 *
 */
public class RunAction extends SnippetAction implements IUpdate {

	public static final String PREFIX = "SnippetEditor.RunAction.";
	
	public RunAction(JavaSnippetEditor editor) {
		super(editor, PREFIX);
		setImageDescriptor(JavaPluginImages.DESC_TOOL_RUNSNIPPET);
	}
	
	/**
	 * The user has invoked this action.
	 */
	public void run() {
		fEditor.evalSelection(JavaSnippetEditor.RESULT_RUN);
	} 
}
