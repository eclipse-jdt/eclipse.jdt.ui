/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IUIConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;


/**
 * Evaluates unimplemented methods of a type.
 * Always forces the the type to be in an open editor. The result is unsaved,
 * so the user can decide if he wants to accept the changes.
 */
public class AddUnimplementedMethodsAction extends JavaUIAction {

	private static final String ACTION_PREFIX= "AddUnimplementedMethodsAction.";
	
	private static final String NOTHINGADDED_PREFIX= ACTION_PREFIX + "NothingAddedDialog.";
	private static final String NOTINWORKINGCOPY_PREFIX= ACTION_PREFIX + "NotInWorkingCopyDialog.";
		
	private ISelectionProvider fSelectionProvider;

	public AddUnimplementedMethodsAction(ISelectionProvider selProvider) {
		super(JavaPlugin.getResourceBundle(), ACTION_PREFIX);
		fSelectionProvider= selProvider;
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
				showSimpleDialog(shell, NOTINWORKINGCOPY_PREFIX);
				return;
			}
			
			AddUnimplementedMethodsOperation op= new AddUnimplementedMethodsOperation(type, false);
			try {
				ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
				dialog.run(false, true, op);
				IMethod[] res= op.getCreatedMethods();
				if (res == null || res.length == 0) {
					showSimpleDialog(shell, NOTHINGADDED_PREFIX);
				} else if (editor != null) {
					EditorUtility.revealInEditor(editor, res[0]);
				}
			} catch (InvocationTargetException e) {
				MessageDialog.openError(shell, "AddUnimplementedMethodsAction failed", e.getTargetException().getMessage());
			} catch (InterruptedException e) {
				// Do nothing. Operation has been canceled by user.
			}
		} catch (JavaModelException e) {
			ErrorDialog.openError(shell, "AddUnimplementedMethodsAction failed", null, e.getStatus());
		} catch (PartInitException e) {
			MessageDialog.openError(shell, "AddMethodStubAction failed", e.getMessage());
		}			
	}
	
	private void showSimpleDialog(Shell shell, String resourcePrefix) {
		JavaPlugin plugin= JavaPlugin.getDefault();
		String okLabel= plugin.getResourceString(IUIConstants.KEY_OK);
		String title= plugin.getResourceString(resourcePrefix + "title");	
		String message= plugin.getResourceString(resourcePrefix + "message");
		
		MessageDialog dialog= new MessageDialog(shell, title, null, message, SWT.ICON_INFORMATION,
	 		new String[] { okLabel }, 0);
	 	
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