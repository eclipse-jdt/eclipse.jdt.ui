/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.corext.codemanipulation.AddGetterSetterOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.IRequestQuery;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.CodeGenerationPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * Create Getter and Setter for selected fields.
 * Will open the parent compilation unit in the editor.
 * The result is unsaved, so the user can decide if the
 * changes are acceptable.
 */
public class AddGetterSetterAction extends Action {

	private ISelectionProvider fSelectionProvider;

	public AddGetterSetterAction(ISelectionProvider selProvider) {
		super(JavaUIMessages.getString("AddGetterSetterAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("AddGetterSetterAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("AddGetterSetterAction.tooltip")); //$NON-NLS-1$
		fSelectionProvider= selProvider;
		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.GETTERSETTER_ACTION);
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
					showError(JavaUIMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
					return;
				}
				workingCopyFields= new IField[fields.length];
				for (int i= 0; i < fields.length; i++) {
					IField field= fields[i];
					IField workingCopyField= (IField) JavaModelUtil.findMemberInCompilationUnit(workingCopyCU, field);
					if (workingCopyField == null) {
						showError(JavaUIMessages.getFormattedString("AddGetterSetterAction.error.fieldNotExisting", field.getElementName())); //$NON-NLS-1$
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
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell());
			dialog.run(false, true, new WorkbenchRunnableAdapter(op));
		
			IMethod[] createdMethods= op.getCreatedAccessors();
			if (createdMethods.length > 0) {
				EditorUtility.revealInEditor(editor, createdMethods[0]);
			}		
		} catch (InvocationTargetException e) {
			JavaPlugin.log(e);
			showError(JavaUIMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			showError(JavaUIMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
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
				String formattedMessage= JavaUIMessages.getFormattedString("AddGetterSetterAction.SkipSetterForFinalDialog.message", fieldName); //$NON-NLS-1$
				return showQueryDialog(formattedMessage, options, returnCodes);	
			}
		};
	}
	
	private IRequestQuery skipReplaceQuery() {
		return new IRequestQuery() {
			public int doQuery(IMember method) {
				int[] returnCodes= {IRequestQuery.YES, IRequestQuery.NO, IRequestQuery.YES_ALL, IRequestQuery.CANCEL};
				String skipLabel= JavaUIMessages.getString("AddGetterSetterAction.SkipExistingDialog.skip.label"); //$NON-NLS-1$
				String replaceLabel= JavaUIMessages.getString("AddGetterSetterAction.SkipExistingDialog.replace.label"); //$NON-NLS-1$
				String skipAllLabel= JavaUIMessages.getString("AddGetterSetterAction.SkipExistingDialog.skipAll.label"); //$NON-NLS-1$
				String[] options= { skipLabel, replaceLabel, skipAllLabel, IDialogConstants.CANCEL_LABEL};
				String methodName= JavaElementLabels.getElementLabel(method, JavaElementLabels.M_PARAMETER_TYPES);
				String formattedMessage= JavaUIMessages.getFormattedString("AddGetterSetterAction.SkipExistingDialog.message", methodName); //$NON-NLS-1$
				return showQueryDialog(formattedMessage, options, returnCodes);		
			}
		};
	}
	
	
	private int showQueryDialog(final String message, final String[] buttonLabels, int[] returnCodes) {
		final Shell shell= JavaPlugin.getActiveWorkbenchShell();
		if (shell == null) {
			JavaPlugin.logErrorMessage("AddGetterSetterAction.showQueryDialog: No active shell found"); //$NON-NLS-1$
			return IRequestQuery.CANCEL;
		}		
		final int[] result= { MessageDialog.CANCEL };
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				String title= JavaUIMessages.getString("AddGetterSetterAction.QueryDialog.title"); //$NON-NLS-1$
				MessageDialog dialog= new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, buttonLabels, 0);
				result[0]= dialog.open();				
			}
		});
		int returnVal= result[0];
		return returnVal < 0 ? IRequestQuery.CANCEL : returnCodes[returnVal];
	}	
				
	
	
	private void showError(String message) {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		String title= JavaUIMessages.getString("AddGetterSetterAction.error.title"); //$NON-NLS-1$
		MessageDialog.openError(shell, title, message);
	}
	
	/*
	 * Returns fields in the selection or <code>null</code> if the selection is 
	 * empty or not valid.
	 */
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
		}
		return null;
	}
	
	/**
	 * Tests if the acion can be run using the current selection.
	 */
	public boolean canActionBeAdded() {
		return getSelectedFields() != null;
	}

}