/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusContentProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusEntryLabelProvider;

public class RefactoringErrorDialogUtil {
	
	private RefactoringErrorDialogUtil() {
		// no instance.
	}
	
	public static void open(String dialogTitle, RefactoringStatus status) {
		if (status.getEntries().size() == 1) {
			String message= "This operation cannot be performed:\n\n" + status.getFirstMessage(RefactoringStatus.FATAL);
			MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), dialogTitle, message);
		} else {
			openListDialog(dialogTitle, status);	
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
