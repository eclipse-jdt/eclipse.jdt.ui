/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.codemanipulation.AddGetterSetterOperation;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

/**
 * Create Getter and Setter for a selected field
 * Always forces the he field to be in an open editor. The result is unsaved,
 * so the user can decide if he wnats to accept the changes
 */
public class AddGetterSetterAction extends Action {

	private ISelectionProvider fSelectionProvider;

	public AddGetterSetterAction(ISelectionProvider selProvider) {
		super(JavaUIMessages.getString("AddGetterSetterAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("AddGetterSetterAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("AddGetterSetterAction.tooltip")); //$NON-NLS-1$
		fSelectionProvider= selProvider;
		
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.GETTERSETTER_ACTION });
	}

				
	public void run() {
		IField[] fields= getSelectedFields();
		if (fields == null) {
			return;
		}
		
		try {
			ICompilationUnit cu= fields[0].getCompilationUnit();
			// open the editor, forces the creation of a working copy
			IEditorPart editor= EditorUtility.openInEditor(cu);
			
			ICompilationUnit workingCopyCU;
			IField[] workingCopyFields;
			if (cu.isWorkingCopy()) {
				workingCopyCU= cu;
				workingCopyFields= fields;
			} else {
				workingCopyCU= EditorUtility.getWorkingCopy(cu);
				if (workingCopyCU == null) {
					showError(JavaUIMessages.getString("AddGetterSetterAction.error.actionFailed")); //$NON-NLS-1$
					return;
				}
				workingCopyFields= new IField[fields.length];
				for (int i= 0; i < fields.length; i++) {
					IField field= fields[i];
					IField workingCopyField= (IField) JavaModelUtility.findMemberInCompilationUnit(workingCopyCU, field);
					if (workingCopyField == null) {
						showError(JavaUIMessages.getFormattedString("AddGetterSetterAction.error.fieldNotExisting", field.getElementName())); //$NON-NLS-1$
						return;
					}
					workingCopyFields[i]= workingCopyField;
				}
			}
		
			AddGetterSetterOperation op= new AddGetterSetterOperation(workingCopyFields);
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell());
			dialog.run(false, true, op);
			
			if (op.getOperationInfo() != null) {
				String title= JavaUIMessages.getString("AddGetterSetterAction.info.title"); //$NON-NLS-1$
				String message= JavaUIMessages.getString("AddGetterSetterAction.info.message"); //$NON-NLS-1$
				ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), title, message, op.getOperationInfo());
			}
		
			IMethod[] createdMethods= op.getCreatedAccessors();
			if (createdMethods.length > 0) {
				EditorUtility.revealInEditor(editor, createdMethods[0]);
			}		
		} catch (InvocationTargetException e) {
			JavaPlugin.log(e);
			showError(JavaUIMessages.getString("AddGetterSetterAction.error.actionFailed")); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// operation cancelled
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			showError(JavaUIMessages.getString("AddGetterSetterAction.error.actionFailed")); //$NON-NLS-1$
			return;
		}
	}
				
	
	
	private void showError(String message) {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		String title= JavaUIMessages.getString("AddGetterSetterAction.error.dialogTitle"); //$NON-NLS-1$
		MessageDialog.openError(shell, title, message);
	}
	
	private IField[] getSelectedFields() {
		ISelection sel= fSelectionProvider.getSelection();
		if (sel instanceof IStructuredSelection) {
			List elements= ((IStructuredSelection)sel).toList();
			int nElements= elements.size();
			if (nElements > 0) {
				IField[] res= new IField[nElements];
				ICompilationUnit cu= null;
				for (int i= 0; i < nElements; i++) {
					Object curr= elements.get(i);
					if (curr instanceof IField) {
						IField fld= (IField)curr;
						
						if (i == 0) {
							cu= fld.getCompilationUnit();
							if (cu == null) {
								return null;
							}
						} else if (!cu.equals(fld.getCompilationUnit())) {
							return null;
						}
						try {
							if (fld.getDeclaringType().isInterface()) {
								return null;
							}
						} catch (JavaModelException e) {
							JavaPlugin.log(e);
							return null;
						}
						
						res[i]= fld;
					} else {
						return null;
					}
				}
				return res;
			}
		}
		return null;
	}
	
	
	public boolean canActionBeAdded() {
		return getSelectedFields() != null;
	}

}