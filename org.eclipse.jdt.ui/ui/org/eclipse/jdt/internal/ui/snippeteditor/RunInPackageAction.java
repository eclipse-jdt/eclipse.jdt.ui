package org.eclipse.jdt.internal.ui.snippeteditor;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Runs a snippet in the context of a package
 *
 */
public class RunInPackageAction extends SnippetAction {

	public static final String PREFIX = "SnippetEditor.RunInPackageAction.";
	
	public RunInPackageAction(JavaSnippetEditor editor) {
		super(editor, PREFIX);
		setImageDescriptor(JavaPluginImages.DESC_TOOL_PACKSNIPPET);
	}
	
	/**
	 * The user has invoked this action.
	 */
	public void run() {
		Shell s= fEditor.getSite().getShell();
		IPackageFragment result= choosePackage(s);
		if (result != null) {
			fEditor.setPackage(result.getElementName());
		}
	} 
	
	private IPackageFragment choosePackage(Shell shell) {
		try {
			IJavaProject p= fEditor.getJavaProject();
			//fix for 1G472LK: ITPJUI:WIN2000 - Package selection dialog must qualify package names regarding source folders
			SelectionDialog dialog= JavaUI.createPackageDialog(shell, p, IJavaElementSearchConstants.CONSIDER_BINARIES);
			dialog.setTitle("Run");
			dialog.setMessage("Choose the package in which code is run");
			String pkg= fEditor.getPackage();
			if (pkg != null) {
				dialog.setInitialSelections(new String[] {pkg});
			}
			dialog.open();		
			Object[] res= dialog.getResult();
			if (res != null && res.length > 0) 
				return (IPackageFragment)res[0];
		} catch (JavaModelException e) {
		}
		return null;
	}
	
	public void snippetStateChanged(JavaSnippetEditor editor) {
		setEnabled(editor != null && !editor.isEvaluating());
	}
}
