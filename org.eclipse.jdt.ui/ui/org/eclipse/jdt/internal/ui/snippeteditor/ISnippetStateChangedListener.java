/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.snippeteditor;


/**
 * A listener interested in snippet evaluation state changes.
 */
public interface ISnippetStateChangedListener {
	
	/**
	 * Informs about the changed snippet evaluation state
	 */
	void snippetStateChanged(JavaSnippetEditor editor);
}