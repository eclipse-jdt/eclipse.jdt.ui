/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.changes;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jdt.core.refactoring.ChangeAbortException;import org.eclipse.jdt.core.refactoring.ChangeContext;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.IChangeExceptionHandler;import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * An implementation of <code>IChangeExceptionHandler</code> which pops up a dialog
 * box asking the user if the refactoring is to be aborted without further actions or
 * if the refactoring engine should try to undo all successfully executed changes.
 */
public class ChangeExceptionHandler implements IChangeExceptionHandler {
	
	public void handle(ChangeContext context, IChange change, Exception e) {
		JavaPlugin.log(e);
		
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		final MessageDialog dialog= new MessageDialog(parent, "Refactoring", null,
			"An unexpected exception has been caught while processing the Refactoring. Its message is:\n\n" +
			e.getMessage() + "\n\nPress \"Undo\" to undo all executed changes\nPress \"Abort\" to abort the refactoring",
			MessageDialog.ERROR, new String[] {"Undo", "Abort"}, 1);
		final int[] result= new int[1];	
		Runnable runnable= new Runnable() {
			public void run() {
				result[0]= dialog.open();
			}
		};
		parent.getDisplay().syncExec(runnable);
		switch(result[0]) {
			case 0:
				context.setTryToUndo();
				// Fall through
			case 1:
				throw new ChangeAbortException(e);
		}
	}
}
