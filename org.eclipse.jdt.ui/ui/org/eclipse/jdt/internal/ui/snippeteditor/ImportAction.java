package org.eclipse.jdt.internal.ui.snippeteditor;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;

/**
 * Imports a package for a snippet evaluation
 *
 */
public class ImportAction extends SnippetAction {

	public ImportAction(JavaSnippetEditor editor, String label) {
		super(editor, label);
		setDescription("Evaluate the import statements");
	}
	
	public void run() {
		//choosePackage(context.getShell());
		//fEvalViewer.evalImports();
	} 
	
	public void snippetStateChanged(JavaSnippetEditor editor) {
		setEnabled(!editor.isEvaluating());
	}
	
	private IPackageFragment choosePackage(Shell shell) {
		IJavaElement[] packages= null;
		try {
			IJavaProject p= fEditor.getJavaProject();
			packages= p.getPackageFragments();
		} catch (JavaModelException e) {
		}
		if (packages == null) {
			packages= new IJavaElement[0];
		}
		
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), false, true);
		dialog.setTitle("Import");
		dialog.setMessage("Select packages to import");
		dialog.open(packages);
		Object[] res= dialog.getSelectedElements();
		if (res != null && res.length > 0) {
			return (IPackageFragment)res[0];
		} else {
			return null;
		}
	}

}
