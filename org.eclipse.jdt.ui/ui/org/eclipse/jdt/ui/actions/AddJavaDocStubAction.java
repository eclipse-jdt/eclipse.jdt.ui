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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;

import org.eclipse.jdt.internal.corext.codemanipulation.AddJavaDocStubOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * Create Java Doc Stubs for selected members
 * Always forces the he field to be in an open editor. The result is unsaved,
 * so the user can decide if he wnats to accept the changes.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class AddJavaDocStubAction extends SelectionDispatchAction {

	/**
	 * Creates a new <code>AddJavaDocStubAction</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddJavaDocStubAction(UnifiedSite site) {
		super(site);
		setText(ActionMessages.getString("AddJavaDocStubAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("AddJavaDocStubAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddJavaDocStubAction.tooltip")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(getSelectedMembers(selection) != null);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void run(IStructuredSelection selection) {
		IMember[] members= getSelectedMembers(selection);
		if (members == null || members.length == 0) {
			return;
		}
		
		try {
			ICompilationUnit cu= members[0].getCompilationUnit();
			// open the editor, forces the creation of a working copy
			IEditorPart editor= EditorUtility.openInEditor(cu);
			
			ICompilationUnit workingCopyCU;
			IMember[] workingCopyMembers;
			if (cu.isWorkingCopy()) {
				workingCopyCU= cu;
				workingCopyMembers= members;
			} else {
				// get the corresponding elements from the working copy
				workingCopyCU= EditorUtility.getWorkingCopy(cu);
				if (workingCopyCU == null) {
					showError(ActionMessages.getString("AddJavaDocStubsAction.error.noWorkingCopy")); //$NON-NLS-1$
					return;
				}
				workingCopyMembers= new IMember[members.length];
				for (int i= 0; i < members.length; i++) {
					IMember member= members[i];
					IMember workingCopyMember= (IMember) JavaModelUtil.findMemberInCompilationUnit(workingCopyCU, member);
					if (workingCopyMember == null) {
						showError(ActionMessages.getFormattedString("AddJavaDocStubsAction.error.memberNotExisting", member.getElementName())); //$NON-NLS-1$
						return;
					}
					workingCopyMembers[i]= workingCopyMember;
				}
			}
			
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();

			AddJavaDocStubOperation op= new AddJavaDocStubOperation(workingCopyMembers, settings);
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());
			dialog.run(false, true, new WorkbenchRunnableAdapter(op));
					
			EditorUtility.revealInEditor(editor, members[0]);	
		} catch (InvocationTargetException e) {
			JavaPlugin.log(e);
			showError(ActionMessages.getString("AddJavaDocStubsAction.error.actionFailed")); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// operation cancelled
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			showError(ActionMessages.getString("AddJavaDocStubsAction.error.actionFailed")); //$NON-NLS-1$
			return;
		}
	}
	
	private void showError(String message) {
		String title= ActionMessages.getString("AddJavaDocStubsAction.error.dialogTitle"); //$NON-NLS-1$
		MessageDialog.openError(getShell(), title, message);
	}
	
	private IMember[] getSelectedMembers(IStructuredSelection selection) {
		List elements= selection.toList();
		int nElements= elements.size();
		if (nElements > 0) {
			IMember[] res= new IMember[nElements];
			ICompilationUnit cu= null;
			for (int i= 0; i < nElements; i++) {
				Object curr= elements.get(i);
				if (curr instanceof IMethod) {
					IMethod member= (IMethod)curr; // limit to methods
					
					if (i == 0) {
						cu= member.getCompilationUnit();
						if (cu == null) {
							return null;
						}
					} else if (!cu.equals(member.getCompilationUnit())) {
						return null;
					}						
					res[i]= member;
				} else {
					return null;
				}
			}
			return res;
		}
		return null;
	}	
}