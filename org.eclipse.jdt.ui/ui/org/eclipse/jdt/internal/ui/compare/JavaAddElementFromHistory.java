/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IEditorInput;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.compare.*;


public class JavaAddElementFromHistory extends JavaHistoryAction {
	
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.AddFromHistoryAction";
	
	private JavaEditor fEditor;
		
	public JavaAddElementFromHistory(JavaEditor editor, ISelectionProvider sp) {
		super(sp, BUNDLE_NAME);
		fEditor= editor;
	}
			
	/**
	 * @see Action#run
	 */
	public final void run() {
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		
		ISelection selection= fSelectionProvider.getSelection();
		
		ICompilationUnit cu= null;
		IParent parent= null;
		
		if (selection.isEmpty()) {
			if (fEditor != null) {
				IEditorInput editorInput= fEditor.getEditorInput();
				IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
				cu= manager.getWorkingCopy(editorInput);
				parent= cu;
			}
			
		} else {
			IMember input= getEditionElement(selection);
			if (input == null) {
				// shouldn't happen because Action should not be enabled in the first place
				MessageDialog.openError(shell, fTitle, "No editions for selection");
				return;
			}
			cu= input.getCompilationUnit();
			
			if (input instanceof IParent) {
				parent= (IParent)input;
			} else {
				IJavaElement parentElement= input.getParent();
				if (parentElement instanceof IParent)
					parent= (IParent)parentElement; 
			}
		}
		
		// extract CU from selection
		if (cu.isWorkingCopy())
			cu= (ICompilationUnit) cu.getOriginalElement();

		// find underlying file
		IFile file= null;
		try {
			file= (IFile) cu.getUnderlyingResource();
		} catch (JavaModelException ex) {
		}
		if (file == null) {
			MessageDialog.openError(shell, fTitle, "Can't find underlying file");
			return;
		}
		
		// get available editions
		IFileState[] states= null;
		try {
			states= file.getHistory(null);
		} catch (CoreException ex) {
		}	
		if (states == null || states.length <= 0) {
			MessageDialog.openInformation(shell, fTitle, "No editions available");
			return;
		}
		
		ITypedElement target= new ResourceNode(file);
		ITypedElement[] editions= new ITypedElement[states.length];
		for (int i= 0; i < states.length; i++)
			editions[i]= new HistoryItem(target, states[i]);
		
		EditionSelectionDialog esd= new EditionSelectionDialog(shell, fBundle);
		//esd.setAddMode(true);
		esd.selectEdition(target, editions, parent);
	}
	
	protected void updateLabel(ISelection selection) {
		setText("Add from Local History...");
		setEnabled(true);
	}
}
