/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.undo;

import java.lang.reflect.InvocationTargetException;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.actions.JavaUIAction;import org.eclipse.jdt.internal.ui.refactoring.RefactoringResources;import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class UndoManagerAction extends JavaUIAction {

	public UndoManagerAction(String prefix) {
		super(RefactoringResources.getResourceBundle(), prefix);
	}
	
	public abstract IRunnableWithProgress createOperation(ChangeContext context);
	
	public void run() {
		ChangeContext context= new ChangeContext(new AbortChangeExceptionHandler());
		IRunnableWithProgress op= createOperation(context);
		try {
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell());
			// Don't execute in separate thread since it updates the UI.
			dialog.run(false, false, op);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, "Error", "Internal Error in Undo Manager");
		} catch (InterruptedException e) {
			// Opertation isn't cancelable.
		}
	}
}