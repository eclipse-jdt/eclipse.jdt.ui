/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.changes;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

/**
 * An implementation of <code>IChangeExceptionHandler</code> which pops up a dialog
 * box asking the user if the refactoring is to be aborted without further actions or
 * if the refactoring engine should try to undo all successfully executed changes.
 */
public class ChangeExceptionHandler implements IChangeExceptionHandler {
	
	private Shell fParent;
	
	public ChangeExceptionHandler(Shell parent) {
		Assert.isNotNull(parent);
		fParent= parent;
	}
	
	public void handle(ChangeContext context, IChange change, Exception e) {
		JavaPlugin.log(e);
		
		final MessageDialog dialog= new MessageDialog(fParent,
			RefactoringMessages.getString("ChangeExceptionHandler.refactoring"), null, //$NON-NLS-1$
			RefactoringMessages.getFormattedString("ChangeExceptionHandler.unexpected_exception", new String[] {change.getName(), e.getMessage()}), //$NON-NLS-1$
			MessageDialog.ERROR, new String[] { RefactoringMessages.getString("ChangeExceptionHandler.undo"), RefactoringMessages.getString("ChangeExceptionHandler.abort")}, 1); //$NON-NLS-2$ //$NON-NLS-1$
		final int[] result= new int[1];	
		Runnable runnable= new Runnable() {
			public void run() {
				result[0]= dialog.open();
			}
		};
		fParent.getDisplay().syncExec(runnable);
		switch(result[0]) {
			case 0:
				context.setTryToUndo();
				// Fall through
			case 1:
				throw new ChangeAbortException(e);
		}
	}
}
