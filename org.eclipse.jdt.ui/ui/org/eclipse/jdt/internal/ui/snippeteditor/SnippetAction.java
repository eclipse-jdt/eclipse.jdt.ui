/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.snippeteditor;

import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jface.action.Action;import org.eclipse.jface.text.ITextSelection;

/**
 * Base class for evaluation state dependent actions and
 * supports to retarget the action to a different viewer.
 */
public abstract class SnippetAction extends Action implements ISnippetStateChangedListener {
	
	public static final String LABEL = "label";
	public static final String TOOLTIP = "tooltip";
	public static final String DESCRIPTION = "description";
	
	protected JavaSnippetEditor fEditor;
	
	public SnippetAction(JavaSnippetEditor editor, String resourcePrefix) {
		super(JavaPlugin.getResourceString(resourcePrefix + LABEL));
		setEditor(editor);
		setToolTipText(JavaPlugin.getResourceString(resourcePrefix + TOOLTIP));
		setDescription(JavaPlugin.getResourceString(resourcePrefix + DESCRIPTION));
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
	
	public void snippetStateChanged(JavaSnippetEditor editor) {
		if (editor != null && !editor.isEvaluating()) {
			update();
		} else {
			setEnabled(false);
		}
		
	}
	
	/**
	 * Common update method for subclasses
	 * that implement <code>IUpdate</code>
	 */
	public void update() {
		ITextSelection selection= (ITextSelection)fEditor.getSelectionProvider().getSelection();
		String text= selection.getText();
		if (text != null) {
			text= text.trim();
			setEnabled(textHasContent(text));
			return;
		} 
		setEnabled(false);
	}
	
	protected boolean textHasContent(String text) {
		int length= text.length();
		if (length > 0) {
			for (int i= 0; i < length; i++) {
				if (Character.isLetterOrDigit(text.charAt(i))) {
					return true;
				}
			}
		}
		return false;
	}
}
