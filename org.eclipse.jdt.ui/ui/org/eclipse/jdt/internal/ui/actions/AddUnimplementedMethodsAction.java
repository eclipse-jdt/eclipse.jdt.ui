/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;


/**
 * Evaluates unimplemented methods of a type.
 * Will open an editor for the type. Changes are unsaved.
 */
public class AddUnimplementedMethodsAction extends Action {

	private ISelectionProvider fSelectionProvider;

	public AddUnimplementedMethodsAction(ISelectionProvider selProvider) {
		super(JavaUIMessages.getString("AddUnimplementedMethodsAction.label")); //$NON-NLS-1$
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
			type= (IType)EditorUtility.getWorkingCopy(type);
			
			if (type == null) {
				MessageDialog.openError(shell, JavaUIMessages.getString("AddUnimplementedMethodsAction.error.title"), JavaUIMessages.getString("AddUnimplementedMethodsAction.error.type_removed_in_editor")); //$NON-NLS-2$ //$NON-NLS-1$
				return;
			}
			
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			AddUnimplementedMethodsOperation op= new AddUnimplementedMethodsOperation(type, settings, false);
			try {
				ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
				dialog.run(false, true, new WorkbenchRunnableAdapter(op));
				IMethod[] res= op.getCreatedMethods();
				if (res == null || res.length == 0) {
					MessageDialog.openInformation(shell, JavaUIMessages.getString("AddUnimplementedMethodsAction.error.title"), JavaUIMessages.getString("AddUnimplementedMethodsAction.error.nothing_found")); //$NON-NLS-2$ //$NON-NLS-1$
				} else if (editor != null) {
					EditorUtility.revealInEditor(editor, res[0]);
				}
			} catch (InvocationTargetException e) {
				JavaPlugin.log(e);
				MessageDialog.openError(shell, JavaUIMessages.getString("AddUnimplementedMethodsAction.error.title"), e.getTargetException().getMessage()); //$NON-NLS-1$
			} catch (InterruptedException e) {
				// Do nothing. Operation has been canceled by user.
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
			ErrorDialog.openError(shell, JavaUIMessages.getString("AddUnimplementedMethodsAction.error.title"), null, e.getStatus()); //$NON-NLS-1$
		}			
	}
		
	private IType getSelectedType() throws JavaModelException {
		ISelection sel= fSelectionProvider.getSelection();
		if (sel instanceof IStructuredSelection) {
			Object[] elements= ((IStructuredSelection)sel).toArray();
			if (elements.length == 1 && (elements[0] instanceof IType)) {
				IType type= (IType) elements[0];
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
			JavaPlugin.log(e.getStatus());
		}
		return false;
	}	

}