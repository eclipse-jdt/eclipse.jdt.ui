/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.AddGetterSetterOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.IRequestQuery;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.CodeGenerationPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * Create Getter and Setter for selected fields.
 * Will open the parent compilation unit in the editor.
 * The result is unsaved, so the user can decide if the
 * changes are acceptable.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 */
public class AddGetterSetterAction extends SelectionDispatchAction {

	/**
	 * Creates a new <code>AddGetterSetterAction</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddGetterSetterAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("AddGetterSetterAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("AddGetterSetterAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddGetterSetterAction.tooltip")); //$NON-NLS-1$
		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.GETTERSETTER_ACTION);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		try {
			IMember[] members= getSelectedFields(selection);
			setEnabled(members != null && members.length > 0 && JavaModelUtil.isEditable(members[0].getCompilationUnit()));
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			setEnabled(false);
		}
	}
		
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void run(IStructuredSelection selection) {
		IField[] fields= getSelectedFields(selection);
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
					showError(ActionMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
					return;
				}
				workingCopyFields= new IField[fields.length];
				for (int i= 0; i < fields.length; i++) {
					IField field= fields[i];
					IField workingCopyField= (IField) JavaModelUtil.findMemberInCompilationUnit(workingCopyCU, field);
					if (workingCopyField == null) {
						showError(ActionMessages.getFormattedString("AddGetterSetterAction.error.fieldNotExisting", field.getElementName())); //$NON-NLS-1$
						return;
					}
					workingCopyFields[i]= workingCopyField;
				}
			}
			IRequestQuery skipSetterForFinalQuery= skipSetterForFinalQuery();
			IRequestQuery skipReplaceQuery= skipReplaceQuery();
			String[] prefixes= CodeGenerationPreferencePage.getGetterStetterPrefixes();
			String[] suffixes= CodeGenerationPreferencePage.getGetterStetterSuffixes();
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		
			AddGetterSetterOperation op= new AddGetterSetterOperation(workingCopyFields, prefixes, suffixes, settings, skipSetterForFinalQuery, skipReplaceQuery);
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());
			dialog.run(false, true, new WorkbenchRunnableAdapter(op));
		
			IMethod[] createdMethods= op.getCreatedAccessors();
			if (createdMethods.length > 0) {
				EditorUtility.revealInEditor(editor, createdMethods[0]);
			}		
		} catch (InvocationTargetException e) {
			JavaPlugin.log(e);
			showError(ActionMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			showError(ActionMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
			return;
		} catch (InterruptedException e) {
			// operation cancelled
		}
		
	}
	
	private IRequestQuery skipSetterForFinalQuery() {
		return new IRequestQuery() {
			public int doQuery(IMember field) {
				int[] returnCodes= {IRequestQuery.YES, IRequestQuery.NO, IRequestQuery.YES_ALL, IRequestQuery.CANCEL};
				String[] options= {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.CANCEL_LABEL};
				String fieldName= JavaElementLabels.getElementLabel(field, 0);
				String formattedMessage= ActionMessages.getFormattedString("AddGetterSetterAction.SkipSetterForFinalDialog.message", fieldName); //$NON-NLS-1$
				return showQueryDialog(formattedMessage, options, returnCodes);	
			}
		};
	}
	
	private IRequestQuery skipReplaceQuery() {
		return new IRequestQuery() {
			public int doQuery(IMember method) {
				int[] returnCodes= {IRequestQuery.YES, IRequestQuery.NO, IRequestQuery.YES_ALL, IRequestQuery.CANCEL};
				String skipLabel= ActionMessages.getString("AddGetterSetterAction.SkipExistingDialog.skip.label"); //$NON-NLS-1$
				String replaceLabel= ActionMessages.getString("AddGetterSetterAction.SkipExistingDialog.replace.label"); //$NON-NLS-1$
				String skipAllLabel= ActionMessages.getString("AddGetterSetterAction.SkipExistingDialog.skipAll.label"); //$NON-NLS-1$
				String[] options= { skipLabel, replaceLabel, skipAllLabel, IDialogConstants.CANCEL_LABEL};
				String methodName= JavaElementLabels.getElementLabel(method, JavaElementLabels.M_PARAMETER_TYPES);
				String formattedMessage= ActionMessages.getFormattedString("AddGetterSetterAction.SkipExistingDialog.message", methodName); //$NON-NLS-1$
				return showQueryDialog(formattedMessage, options, returnCodes);		
			}
		};
	}
	
	
	private int showQueryDialog(final String message, final String[] buttonLabels, int[] returnCodes) {
		final Shell shell= getShell();
		if (shell == null) {
			JavaPlugin.logErrorMessage("AddGetterSetterAction.showQueryDialog: No active shell found"); //$NON-NLS-1$
			return IRequestQuery.CANCEL;
		}		
		final int[] result= { MessageDialog.CANCEL };
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				String title= ActionMessages.getString("AddGetterSetterAction.QueryDialog.title"); //$NON-NLS-1$
				MessageDialog dialog= new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, buttonLabels, 0);
				result[0]= dialog.open();				
			}
		});
		int returnVal= result[0];
		return returnVal < 0 ? IRequestQuery.CANCEL : returnCodes[returnVal];
	}	

	private void showError(String message) {
		MessageDialog.openError(getShell(), getDialogTitle(), message);
	}
	
	/*
	 * Returns fields in the selection or <code>null</code> if the selection is 
	 * empty or not valid.
	 */
	private IField[] getSelectedFields(IStructuredSelection selection) {
		List elements= selection.toList();
		int nElements= elements.size();
		if (nElements > 0) {
			IField[] res= new IField[nElements];
			ICompilationUnit cu= null;
			for (int i= 0; i < nElements; i++) {
				Object curr= elements.get(i);
				if (curr instanceof IField) {
					IField fld= (IField)curr;
					
					if (i == 0) {
						// remember the cu of the first element
						cu= fld.getCompilationUnit();
						if (cu == null) {
							return null;
						}
					} else if (!cu.equals(fld.getCompilationUnit())) {
						// all fields must be in the same CU
						return null;
					}
					try {
						if (fld.getDeclaringType().isInterface()) {
							// no setters/getters for interfaces
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
		return null;
	}	
	
	private String getDialogTitle() {
		return ActionMessages.getString("AddGetterSetterAction.error.title"); //$NON-NLS-1$
	}	
}