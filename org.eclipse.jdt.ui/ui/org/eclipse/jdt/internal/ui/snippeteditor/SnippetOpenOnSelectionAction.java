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

import org.eclipse.jdt.internal.ui.actions.OpenJavaElementAction;


/**
 * This action opens a java editor on the element represented by text selection of
 * the code snippet.
 */
public class SnippetOpenOnSelectionAction extends OpenJavaElementAction {
	
	private JavaSnippetEditor fEditor;
	private String fDialogTitle;
	private String fDialogMessage;
	
	public SnippetOpenOnSelectionAction(JavaSnippetEditor editor) {
		super();
		
		fEditor= editor;
		
		setText("&Open on Selection@F3");
		setDescription("Open an editor on the selected element");
		setToolTipText("Open an editor on the selected element");
		setDialogTitle("Open on Selection");
		setDialogMessage("Select or enter the element to open");
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
		
		Shell w= getShell();
		
		try {
			IJavaElement[] result= fEditor.codeResolve();
			if (result != null && result.length > 0) {
				ISourceReference chosen= selectSourceReference(
										filterResolveResults(result),
										w, 
										fDialogTitle, 
										fDialogMessage
										);
				if (chosen != null) {
					open(chosen);
					return;
				}
			}
		} catch (JavaModelException x) {
		} catch (PartInitException x) {
		}
		
		w.getDisplay().beep();		
	}
	
	protected Shell getShell() {
		return fEditor.getSite().getWorkbenchWindow().getShell();
	}					
	
}