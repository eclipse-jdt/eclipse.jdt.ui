package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.swt.SWT;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.codemanipulation.OrganizeImportsOperation;



public class OrganizeImportsAction extends Action {
	
	
	private ITextEditor fEditor;
	
	
	public OrganizeImportsAction(ITextEditor editor) {
		
		super(JavaEditorMessages.getString("OrganizeImports.label")); //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("OrganizeImports.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("OrganizeImports.description")); //$NON-NLS-1$
		
		setContentEditor(editor);
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.ORGANIZE_IMPORTS_ACTION });					
	}
	
	public OrganizeImportsAction() {
		this(null);
	}
	
	public void setContentEditor(ITextEditor editor) {
		fEditor= editor;
		setEnabled(editor != null && fEditor.isEditable());
	}
	
	/**
	 * @see IAction#actionPerformed
	 */
	public void run() {
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit cu= manager.getWorkingCopy(fEditor.getEditorInput());
		if (cu != null) {
			OrganizeImportsOperation op= new OrganizeImportsOperation(cu, false);
			try {
				ProgressMonitorDialog dialog= new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell());
				dialog.run(false, true, op);
				if (op.hasCompilationErrors()) {
					showCompilationErrorDialog();			
				}
				return;
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), JavaEditorMessages.getString("OrganizeImports.error.title1"), e.getTargetException().getMessage()); //$NON-NLS-1$
			} catch (InterruptedException e) {
			}
		}		
	}
	
	private void showCompilationErrorDialog() {
		MessageDialog dialog= new MessageDialog(JavaPlugin.getActiveWorkbenchShell(), JavaEditorMessages.getString("OrganizeImports.error.title2"), null, JavaEditorMessages.getString("OrganizeImports.error.message2"), SWT.ICON_INFORMATION, new String[] { JavaEditorMessages.getString("OrganizeImports.ok_label") }, 0); //$NON-NLS-3$ //$NON-NLS-1$ //$NON-NLS-2$
	 	dialog.open();
	}
	
	public static boolean canActionBeAdded(ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			List elements= ((IStructuredSelection)selection).toList();
			if (elements.size() == 1) {
				return (elements.get(0) instanceof IImportContainer);
			}
		}
		return false;
	}
		
}