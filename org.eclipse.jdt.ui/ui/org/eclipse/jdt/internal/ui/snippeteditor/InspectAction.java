/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.snippeteditor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Inspects a snippet
 *
 */
public class InspectAction extends SnippetAction implements IUpdate {

	public InspectAction(JavaSnippetEditor editor) {
		super(editor);
		setText(SnippetMessages.getString("InspectAction.label")); //$NON-NLS-1$
		setToolTipText(SnippetMessages.getString("InspectAction.tooltip")); //$NON-NLS-1$
		setDescription(SnippetMessages.getString("InspectAction.description")); //$NON-NLS-1$
		setImageDescriptor(JavaPluginImages.DESC_TOOL_INSPSNIPPET);
	}
	
	/**
	 * The user has invoked this action.
	 */
	public void run() {
		fEditor.evalSelection(JavaSnippetEditor.RESULT_INSPECT);
	} 
}
