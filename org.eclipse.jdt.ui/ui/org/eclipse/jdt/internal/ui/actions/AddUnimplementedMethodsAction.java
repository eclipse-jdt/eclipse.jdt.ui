/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;


/**
 * Evaluates unimplemented methods of a type.
 * Always forces the the type to be in an open editor. The result is unsaved,
 * so the user can decide if he wants to accept the changes.
 */
public class AddUnimplementedMethodsAction extends Action {

	private ISelectionProvider fSelectionProvider;

	public AddUnimplementedMethodsAction(ISelectionProvider selProvider) {
		super(JavaUIMessages.getString("AddUnimplementedMethodsAction.lebel")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("AddUnimplementedMethodsAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("AddUnimplementedMethodsAction.tooltip")); //$NON-NLS-1$
		fSelectionProvider= selProvider;
		
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.ADD_UNIMPLEMENTED_METHODS_ACTION });
		
	}

	public void run() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		try {
			IType type= getSelectedType();
			if (type == null) {
				return;
			}		
			// open an editor and work on a working copy
			IEditorPart editor= EditorUtility.openInEditor(type);
			type= EditorUtility.getWorkingCopy(type);
			
			if (type == null) {
				showSimpleDialog(shell, JavaUIMessages.getString("AddUnimplementedMethodsAction.dialogTitle"), JavaUIMessages.getString("AddUnimplementedMethodsAction.type_removed_in_editor")); //$NON-NLS-2$ //$NON-NLS-1$
				return;
			}
			
			AddUnimplementedMethodsOperation op= new AddUnimplementedMethodsOperation(type, false);
			try {
				ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
				dialog.run(false, true, op);
				IMethod[] res= op.getCreatedMethods();
				if (res == null || res.length == 0) {
					showSimpleDialog(shell, JavaUIMessages.getString("AddUnimplementedMethodsAction.DialogTitle"), JavaUIMessages.getString("AddUnimplementedMethodsAction.nothing_found")); //$NON-NLS-2$ //$NON-NLS-1$
				} else if (editor != null) {
					EditorUtility.revealInEditor(editor, res[0]);
				}
			} catch (InvocationTargetException e) {
				MessageDialog.openError(shell, JavaUIMessages.getString("AddUnimplementedMethodsAction.actionFailed"), e.getTargetException().getMessage()); //$NON-NLS-1$
			} catch (InterruptedException e) {
				// Do nothing. Operation has been canceled by user.
			}
		} catch (JavaModelException e) {
			ErrorDialog.openError(shell, JavaUIMessages.getString("AddUnimplementedMethodsAction.actionFailed"), null, e.getStatus()); //$NON-NLS-1$
		} catch (PartInitException e) {
			MessageDialog.openError(shell, JavaUIMessages.getString("AddUnimplementedMethodsAction.actionFailed"), e.getMessage()); //$NON-NLS-1$
		}			
	}
	
	private void showSimpleDialog(Shell shell, String title, String message) {
		MessageDialog dialog= new MessageDialog(shell, title, null, message, SWT.ICON_INFORMATION,
	 		new String[] { JavaUIMessages.getString("AddUnimplementedMethodsAction.ok") }, 0); //$NON-NLS-1$
	 	
	 	dialog.open();
	}	
	
	private IType getSelectedType() throws JavaModelException {
		ISelection sel= fSelectionProvider.getSelection();
		if (sel instanceof IStructuredSelection) {
			Object[] vec= ((IStructuredSelection)sel).toArray();
			if (vec.length == 1 && (vec[0] instanceof IType)) {
				IType type= (IType)vec[0];
				if (type.getCompilationUnit() != null && type.isClass()) {
					return type;
				}
			}
		}
		return null;
	}
	
	public boolean canActionBeAdded() {
		try {
			return getSelectedType() != null;
		} catch (JavaModelException e) {
			// do not handle here
		}
		return false;
	}	

}