package org.eclipse.jdt.internal.ui.snippeteditor;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

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
	protected ResourceBundle fBundle;
	protected String fPrefix;
	protected JavaSnippetEditor fEditor;
		
	public SnippetOpenOnSelectionAction(JavaSnippetEditor editor, ResourceBundle bundle, String prefix) {
		super(bundle, prefix);
		fBundle= bundle;
		fPrefix= prefix;
		fEditor= editor;
	}

	public SnippetOpenOnSelectionAction(ResourceBundle bundle, String prefix) {
		this(null, bundle, prefix);
	}
	
	public void setContentEditor(JavaSnippetEditor contentEditor) {
		fEditor= contentEditor;
	}
	
	protected String getResourceString(String key) {
		return fBundle.getString(fPrefix + key);
	}
		
	public void run() {
		
		Shell w= getShell();
		
		try {
			IJavaElement[] result= fEditor.codeResolve();
			if (result != null && result.length > 0) {
				ISourceReference chosen= selectSourceReference(
										filterResolveResults(result),
										w, 
										getResourceString("title"), 
										getResourceString("message")
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