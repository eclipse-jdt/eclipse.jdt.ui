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
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;

import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class PerformRefactoringUtil {

	//no instances
	private PerformRefactoringUtil() {
	}

	public static boolean performRefactoring(PerformChangeOperation op, Refactoring refactoring, IRunnableContext execContext, Shell parent) {
		op.setUndoManager(Refactoring.getUndoManager(), refactoring.getName());
		try{
			execContext.run(false, false, new WorkbenchRunnableAdapter(op));
		} catch (InvocationTargetException e) {
			Throwable inner= e.getTargetException();
			if (op.getChangeExecutionFailed()) {
				org.eclipse.ltk.internal.ui.refactoring.ChangeExceptionHandler handler=
					new org.eclipse.ltk.internal.ui.refactoring.ChangeExceptionHandler(parent, refactoring);
				if (inner instanceof RuntimeException) {
					handler.handle(op.getChange(), (RuntimeException)inner);
					return false;
				} else if (inner instanceof CoreException) {
					handler.handle(op.getChange(), (CoreException)inner);
					return false;
				}
			}
			ExceptionHandler.handle(e, parent, 
				RefactoringMessages.getString("RefactoringWizard.refactoring"), //$NON-NLS-1$
				RefactoringMessages.getString("RefactoringWizard.unexpected_exception_1")); //$NON-NLS-1$
			return false;
		} catch (InterruptedException e) {
			return false;
		} 
		return true;
	}	
}
