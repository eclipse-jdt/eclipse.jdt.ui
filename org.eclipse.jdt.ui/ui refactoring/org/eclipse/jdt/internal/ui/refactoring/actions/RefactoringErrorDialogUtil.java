/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusContentProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusEntryLabelProvider;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

public class RefactoringErrorDialogUtil {
	
	private RefactoringErrorDialogUtil() {
		// no instance.
	}
	
	public static Object open(String dialogTitle, RefactoringStatus status, Shell parentShell) {
		RefactoringStatusEntry[] entries= status.getEntries();
		if (entries.length == 1) {
			RefactoringStatusEntry entry= entries[0];
			String message= status.getMessageMatchingSeverity(RefactoringStatus.FATAL);
			
			if (entry.getCode() != RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD
				&& entry.getCode() != RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE) {
				MessageDialog.openInformation(parentShell, dialogTitle, message);
				return null;
			}			
			message= message + RefactoringMessages.getString("RefactoringErrorDialogUtil.okToPerformQuestion"); //$NON-NLS-1$
			if (MessageDialog.openQuestion(parentShell, dialogTitle, message))
				return entry.getData();
			return null;
		} else {
			openListDialog(dialogTitle, status, parentShell);	
			return null;
		}
	}
	
	private static void openListDialog(String dialogTitle, RefactoringStatus status, Shell parentShell) {
		ListDialog dialog= new ListDialog(parentShell);
		dialog.setInput(status);
		dialog.setTitle(dialogTitle);
		dialog.setMessage(RefactoringMessages.getString("RefactoringErrorDialogUtil.cannot_perform")); //$NON-NLS-1$
		dialog.setContentProvider(new RefactoringStatusContentProvider());
		dialog.setLabelProvider(new RefactoringStatusEntryLabelProvider());
		dialog.open();	
	}
}
