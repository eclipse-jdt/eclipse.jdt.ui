/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.undo;

import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.Iterator;import java.util.List;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.dialogs.IDialogConstants;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.core.resources.IFile;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.MultiStatus;import org.eclipse.core.runtime.Status;import org.eclipse.ui.IEditorInput;import org.eclipse.ui.IEditorPart;import org.eclipse.ui.IFileEditorInput;import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatusEntry;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.actions.JavaUIAction;import org.eclipse.jdt.internal.ui.refactoring.RefactoringResources;import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class UndoManagerAction extends JavaUIAction {

	private RefactoringStatus fPreflightStatus;

	public UndoManagerAction(String prefix) {
		super(RefactoringResources.getResourceBundle(), prefix);
	}
	
	public abstract IRunnableWithProgress createOperation(ChangeContext context);
	
	protected abstract String getName();
	
	public void run() {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		ChangeContext context= new ChangeContext(new AbortChangeExceptionHandler(), getUnsavedFiles());
		IRunnableWithProgress op= createOperation(context);
		try {
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(parent);
			// Don't execute in separate thread since it updates the UI.
			dialog.run(false, false, op);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, "Error", "Internal Error in Undo Manager");
		} catch (InterruptedException e) {
			// Opertation isn't cancelable.
		} finally {
			context.clearPerformedChanges();
		}
		
		if (fPreflightStatus.hasError()) {
			String name= getName();
			MultiStatus status = createMultiStatus(name);
			String message= name + " cannot be executed.";
			ErrorDialog error= new ErrorDialog(parent, name, message, status, IStatus.ERROR) {
				public void create() {
					super.create();
					buttonPressed(IDialogConstants.DETAILS_ID);
				}
			};
			error.open();
		}
	}

	protected MultiStatus createMultiStatus(String name) {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		MultiStatus status= new MultiStatus(
			JavaPlugin.getPluginId(), 
			IStatus.ERROR,
			"There are unsaved files affected by the " + name + ". " + name + " can only proceed if you revert the files to their original content.",
			null);
		String id= JavaPlugin.getPluginId();
		for (Iterator iter= fPreflightStatus.getEntries().iterator(); iter.hasNext(); ) {
			RefactoringStatusEntry entry= (RefactoringStatusEntry)iter.next();
			status.merge(new Status(
				IStatus.ERROR,
				id,
				IStatus.ERROR,
				entry.getMessage(),
				null));
		}
		return status;
	}
	
	protected void setPreflightStatus(RefactoringStatus status) {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		fPreflightStatus= status;
	}
	
	protected IFile[] getUnsavedFiles() {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		IEditorPart[] parts= JavaPlugin.getDirtyEditors();
		List result= new ArrayList(parts.length);
		for (int i= 0; i < parts.length; i++) {
			IEditorInput input= parts[i].getEditorInput();
			if (input instanceof IFileEditorInput) {
				result.add(((IFileEditorInput)input).getFile());
			}
		}
		return (IFile[])result.toArray(new IFile[result.size()]);
	}	
}