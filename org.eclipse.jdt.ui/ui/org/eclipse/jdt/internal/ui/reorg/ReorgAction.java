/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.CreateChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.PerformChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringAction;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Base class for actions related to reorganizing resources
 */
abstract class ReorgAction extends RefactoringAction {
	
	public ReorgAction(String name, StructuredSelectionProvider p) {
		super(name, p);
	}
	
	public ReorgAction(String name, ISelectionProvider p) {
		super(name, p);
	}
	
	protected boolean hasOnlyProjects(){
		return (! getStructuredSelection().isEmpty() && getStructuredSelection().size() == getSelectedProjects().size());
	}
	
	List getSelectedProjects() {
		List result= new ArrayList(getStructuredSelection().size());
		for(Iterator iter= getStructuredSelection().iterator(); iter.hasNext(); ) {
			Object element= iter.next();
			if (element instanceof IJavaProject) {
				try {
					result.add(((IJavaProject)element).getUnderlyingResource());
				} catch (JavaModelException e) {
					if (!e.isDoesNotExist()) {
						//do not show error dialogs in a loop
						JavaPlugin.log(e);
					}
				}
			}
		}
		return result;
	}	
	
	static boolean canActivate(Refactoring ref){
		try {
			return ref.checkActivation(new NullProgressMonitor()).isOK();
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("ReorgAction.reorganize"), RefactoringMessages.getString("ReorgAction.exception")); //$NON-NLS-2$ //$NON-NLS-1$
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
			Throwable target= e.getTargetException();
			if (target instanceof CoreException)
				handler.getStatus().merge(((CoreException) target).getStatus());
			JavaPlugin.log(e);	
			//fall thru
		} catch (InterruptedException e) {
			//fall thru
		}
		return handler.getStatus();
	}		
}