/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.CreateChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.PerformChangeOperation;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Base class for actions related to reorganizing resources
 */
public abstract class ReorgAction extends SelectionProviderAction {
	
	public ReorgAction(ISelectionProvider p, String name) {
		super(p, name);
	}
	
	/**
	 * Hook to update the action's enable state before it is added to the context
	 * menu. This default implementation does nothing.
	 */
	public void update() {
	}
	
	/**
	 *Set self's enablement based upon the currently selected resources
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canExecute(selection.toList()));
	}
	abstract boolean canExecute(List selection);

	boolean canActivate(Refactoring ref){
		try {
			return ref.checkActivation(new NullProgressMonitor()).isOK();
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, "Exception", "Unexpected exception. See log for details.");
			return false;
		}	
	}
	
	static MultiStatus perform(Refactoring ref) throws JavaModelException{	
		PerformChangeOperation op= new PerformChangeOperation(new CreateChangeOperation(ref, CreateChangeOperation.CHECK_NONE));
		ReorgExceptionHandler handler= new ReorgExceptionHandler();
		op.setChangeContext(new ChangeContext(handler));		
		try {
			//cannot fork - must run in the ui thread
			new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell()).run(false, true, op);
		} catch (InvocationTargetException e) {
			Throwable t= e.getTargetException();
			if (t instanceof CoreException)
				handler.getStatus().merge(((CoreException) t).getStatus());
			JavaPlugin.log(t);	
			if (t instanceof Error)
				throw (Error)t;
			if (t instanceof RuntimeException)
				throw (RuntimeException)t;
			//fall thru
		} catch (InterruptedException e) {
			//fall thru
		}
		return handler.getStatus();
	}	
}