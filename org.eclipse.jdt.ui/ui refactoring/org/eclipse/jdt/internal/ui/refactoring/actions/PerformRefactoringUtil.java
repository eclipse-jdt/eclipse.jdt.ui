package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IUndoManager;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.PerformChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.refactoring.changes.ChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

//XXX code duplicated from RefactoringWizard
class PerformRefactoringUtil {

	//no instances
	private PerformRefactoringUtil(){
	}

	public static boolean performRefactoring(PerformChangeOperation op, Refactoring refactoring) {
		ChangeContext context= new ChangeContext(new ChangeExceptionHandler());
		boolean success= false;
		IUndoManager undoManager= Refactoring.getUndoManager();
		try{
			op.setChangeContext(context);
			undoManager.aboutToPerformRefactoring();
			new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell()).run(false, false, op);
			if (op.changeExecuted()) {
				if (! op.getChange().isUndoable()){
					success= false;
				} else { 
					undoManager.addUndo(refactoring.getName(), op.getChange().getUndoChange());
					success= true;
				}	
			}
		} catch (InvocationTargetException e) {
			Throwable t= e.getTargetException();
			if (t instanceof ChangeAbortException) {
				success= handleChangeAbortException(context, (ChangeAbortException)t);
				return true;
			} else {
				handleUnexpectedException(e);
			}	
			return false;
		} catch (InterruptedException e) {
			return false;
		} finally {
			context.clearPerformedChanges();
			undoManager.refactoringPerformed(success);
		}
		
		return true;
	}
	
	private static boolean handleChangeAbortException(final ChangeContext context, ChangeAbortException exception) {
		if (!context.getTryToUndo())
			return false; // Return false since we handle an unexpected exception and we don't have any
						  // idea in which state the workbench is.
			
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor pm) throws CoreException, InvocationTargetException {
				ChangeContext undoContext= new ChangeContext(new AbortChangeExceptionHandler());
				try {
					IChange[] changes= context.getPerformedChanges();
					pm.beginTask(RefactoringMessages.getString("RefactoringWizard.undoing"), changes.length); //$NON-NLS-1$
					IProgressMonitor sub= new NullProgressMonitor();
					for (int i= changes.length - 1; i >= 0; i--) {
						IChange change= changes[i];
						pm.subTask(change.getName());
						change.getUndoChange().perform(undoContext, sub);
						pm.worked(1);
					}
				} catch (ChangeAbortException e) {
					throw new InvocationTargetException(e.getThrowable());
				} finally {
					pm.done();
				} 
			}
		};
		
		try {
			new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell()).run(false, false, op);
		} catch (InvocationTargetException e) {
			handleUnexpectedException(e);
			return false;
		} catch (InterruptedException e) {
			// not possible. Operation not cancelable.
		}
		
		return true;
	}
	
	private static void handleUnexpectedException(InvocationTargetException e) {
		ExceptionHandler.handle(e, RefactoringMessages.getString("RefactoringWizard.refactoring"), RefactoringMessages.getString("RefactoringWizard.unexpected_exception_1")); //$NON-NLS-2$ //$NON-NLS-1$
	}
}
