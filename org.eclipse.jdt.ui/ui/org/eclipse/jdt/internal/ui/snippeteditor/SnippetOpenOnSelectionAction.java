/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.snippeteditor;

import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.actions.AbstractOpenJavaElementAction;


/**
 * This action opens a java editor on the element represented by text selection of
 * the code snippet.
 */
public class SnippetOpenOnSelectionAction extends AbstractOpenJavaElementAction {
	
	protected JavaSnippetEditor fEditor;
	private String fDialogTitle;
	private String fDialogMessage;
	
	public SnippetOpenOnSelectionAction(JavaSnippetEditor editor) {
		super();
		
		fEditor= editor;
		setResources();
	}
	
	protected void setResources() {
		setText(SnippetMessages.getString("SnippetOpenOnSelectionAction.label")); //$NON-NLS-1$
		setDescription(SnippetMessages.getString("SnippetOpenOnSelectionAction.tooltip")); //$NON-NLS-1$
		setToolTipText(SnippetMessages.getString("SnippetOpenOnSelectionAction.description")); //$NON-NLS-1$
		setDialogTitle(SnippetMessages.getString("SnippetOpenOnSelectionDialog.title")); //$NON-NLS-1$
		setDialogMessage(SnippetMessages.getString("SnippetOpenOnSelectionDialog.message")); //$NON-NLS-1$
	}
		
	public SnippetOpenOnSelectionAction() {
		this(null);
	}
	
	protected void setDialogTitle(String title) {
		fDialogTitle= title;
	}
	
	protected void setDialogMessage(String message) {
		fDialogMessage= message;
	}
	
	public void setContentEditor(JavaSnippetEditor contentEditor) {
		fEditor= contentEditor;
	}
			
	public void run() {
		
		try {
			IJavaElement[] result= fEditor.codeResolve();
			if (result != null && result.length > 0) {
				IJavaElement chosen= selectJavaElement(filterResolveResults(result), getShell(),  fDialogTitle, fDialogMessage);
				if (chosen != null) {
					open(chosen);
					return;
				}
			}
		} catch (JavaModelException x) {
		} catch (PartInitException x) {
		}
		
		getShell().getDisplay().beep();		
	}
	
	protected Shell getShell() {
		return fEditor.getSite().getWorkbenchWindow().getShell();
	}					
	
}