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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.codemanipulation.AddJavaDocStubOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * Create Java Doc Stubs for selected members
 * Always forces the he field to be in an open editor. The result is unsaved,
 * so the user can decide if he wnats to accept the changes
 * 
 * @deprecated Use action from package org.eclipse.jdt.ui.actions
 */
public class AddJavaDocStubAction extends Action {

	private ISelectionProvider fSelectionProvider;

	public AddJavaDocStubAction(ISelectionProvider selProvider) {
		super(JavaUIMessages.getString("AddJavaDocStubAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("AddJavaDocStubAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("AddJavaDocStubAction.tooltip")); //$NON-NLS-1$
		fSelectionProvider= selProvider;
	}

	public void run() {
		IMember[] members= getSelectedMembers();
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
					showError(JavaUIMessages.getString("AddJavaDocStubsAction.error.noWorkingCopy")); //$NON-NLS-1$
					return;
				}
				workingCopyMembers= new IMember[members.length];
				for (int i= 0; i < members.length; i++) {
					IMember member= members[i];
					IMember workingCopyMember= (IMember) JavaModelUtil.findMemberInCompilationUnit(workingCopyCU, member);
					if (workingCopyMember == null) {
						showError(JavaUIMessages.getFormattedString("AddJavaDocStubsAction.error.memberNotExisting", member.getElementName())); //$NON-NLS-1$
						return;
					}
					workingCopyMembers[i]= workingCopyMember;
				}
			}
			
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();

			AddJavaDocStubOperation op= new AddJavaDocStubOperation(workingCopyMembers, settings);
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell());
			dialog.run(false, true, new WorkbenchRunnableAdapter(op));
					
			EditorUtility.revealInEditor(editor, members[0]);	
		} catch (InvocationTargetException e) {
			JavaPlugin.log(e);
			showError(JavaUIMessages.getString("AddJavaDocStubsAction.error.actionFailed")); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// operation cancelled
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			showError(JavaUIMessages.getString("AddJavaDocStubsAction.error.actionFailed")); //$NON-NLS-1$
			return;
		}
	}
	
	private void showError(String message) {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		String title= JavaUIMessages.getString("AddJavaDocStubsAction.error.dialogTitle"); //$NON-NLS-1$
		MessageDialog.openError(shell, title, message);
	}
	
	private IMember[] getSelectedMembers() {
		ISelection sel= fSelectionProvider.getSelection();
		if (sel instanceof IStructuredSelection) {
			List elements= ((IStructuredSelection)sel).toList();
			int nElements= elements.size();
			if (nElements > 0) {
				IMember[] res= new IMember[nElements];
				ICompilationUnit cu= null;
				for (int i= 0; i < nElements; i++) {
					Object curr= elements.get(i);
					if (curr instanceof IMethod || curr instanceof IType) {
						IMember member= (IMember)curr; // limit to methods & types
						
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
		}
		return null;
	}
	
	public boolean canActionBeAdded() {
		return getSelectedMembers() != null;
	}

}