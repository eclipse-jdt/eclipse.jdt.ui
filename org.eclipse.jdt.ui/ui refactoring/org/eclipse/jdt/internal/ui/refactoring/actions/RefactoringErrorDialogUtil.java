/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry;
import org.eclipse.jdt.internal.corext.refactoring.util.DebugUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusContentProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusEntryLabelProvider;

public class RefactoringErrorDialogUtil {
	
	private RefactoringErrorDialogUtil() {
		// no instance.
	}
	
	public static Object open(String dialogTitle, RefactoringStatus status) {
		if (status.getEntries().size() == 1) {
			RefactoringStatusEntry entry= (RefactoringStatusEntry)status.getEntries().get(0);
			String message= status.getFirstMessage(RefactoringStatus.FATAL);
			
			if (   entry.getCode() != RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD
				&& entry.getCode() != RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE){
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), dialogTitle, message);
				return null;
			}			
			message= message + RefactoringMessages.getString("RefactoringErrorDialogUtil.okToPerformQuestion"); //$NON-NLS-1$
			if (MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), dialogTitle, message))
				return entry.getData();
			return null;
		} else {
			openListDialog(dialogTitle, status);	
			return null;
		}
	}
	
	private static void openListDialog(String dialogTitle, RefactoringStatus status) {
		ListDialog dialog= new ListDialog(JavaPlugin.getActiveWorkbenchShell());
		dialog.setInput(status);
		dialog.setTitle(dialogTitle);
		dialog.setMessage(RefactoringMessages.getString("RefactoringErrorDialogUtil.cannot_perform")); //$NON-NLS-1$
		dialog.setContentProvider(new RefactoringStatusContentProvider());
		dialog.setLabelProvider(new RefactoringStatusEntryLabelProvider());
		dialog.open();	
	}
}
