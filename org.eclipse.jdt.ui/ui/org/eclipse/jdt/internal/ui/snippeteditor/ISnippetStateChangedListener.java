package org.eclipse.jdt.internal.ui.snippeteditor;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */



/**
 * A listener interested in snippet evaluation state changes.
 */
public interface ISnippetStateChangedListener {
	
	/**
	 * Informs about the changed snippet evaluation state
	 */
	void snippetStateChanged(JavaSnippetEditor editor);
}