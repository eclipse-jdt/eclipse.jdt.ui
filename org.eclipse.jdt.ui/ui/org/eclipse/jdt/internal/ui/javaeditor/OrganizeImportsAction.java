package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.swt.SWT;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.IUIConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.JavaUIAction;
import org.eclipse.jdt.internal.ui.codemanipulation.OrganizeImportsOperation;

public class OrganizeImportsAction extends JavaUIAction {
	
	private static final String ACTION_PREFIX= "OrganizeImportsAction.";
	private static final String PREFIX_DILAOG= ACTION_PREFIX + "compilationerrordialog.";
		
	private ITextEditor fEditor;
	
	public OrganizeImportsAction(ITextEditor editor) {
		super(JavaPlugin.getResourceBundle(), ACTION_PREFIX);
		setContentEditor(editor);
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
				MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), "AddGetterSetterAction failed", e.getTargetException().getMessage());
			} catch (InterruptedException e) {
			}
		}		
	}
	
	private void showCompilationErrorDialog() {
		String okLabel= JavaPlugin.getResourceString(IUIConstants.KEY_OK);
		String message= JavaPlugin.getResourceString(PREFIX_DILAOG + "message");
		String title= JavaPlugin.getResourceString(PREFIX_DILAOG + "title");
		MessageDialog dialog= new MessageDialog(JavaPlugin.getActiveWorkbenchShell(), title, null, message, SWT.ICON_INFORMATION, new String[] { okLabel }, 0);
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