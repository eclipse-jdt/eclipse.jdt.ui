package org.eclipse.jdt.internal.ui.snippeteditor;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.jface.action.Action;

/**
 * Base class for evaluation state dependent actions and
 * supports to retarget the action to a different viewer.
 */
public abstract class SnippetAction extends Action implements ISnippetStateChangedListener {
	protected JavaSnippetEditor fEditor;
	
	public SnippetAction(JavaSnippetEditor editor, String label) {
		super(label);
		setEditor(editor);
	}
		
	public void setEditor(JavaSnippetEditor editor) {
		if (fEditor != null)
			fEditor.removeSnippetStateChangedListener(this);
		fEditor= editor;
		if (fEditor != null) {
			fEditor.addSnippetStateChangedListener(this);
			snippetStateChanged(fEditor);
		}
	} 
}
