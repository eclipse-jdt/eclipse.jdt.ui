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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

/**
 * Operation that, when run, check proceconditions of an <code>Refactoring</code> passed 
 * on creation.
 */
public class CheckConditionsOperation implements IWorkspaceRunnable {
	private Refactoring fRefactoring;
	private int fStyle;
	private RefactoringStatus fStatus;
	
	public final static int NONE=			0;
	public final static int ACTIVATION=    	1 << 1;
	public final static int INPUT=	   		1 << 2;
	public final static int PRECONDITIONS= 	ACTIVATION | INPUT;
	final static int LAST=                 	1 << 3;
	
	/**
	 * Creates a new <code>CheckConditionsOperation</code>.
	 * 
	 * @param refactoring the refactoring. Parameter must not be <code>null</code>.
	 * @param style style to define which conditions to check.
	 */
	public CheckConditionsOperation(Refactoring refactoring, int style) {
		Assert.isNotNull(refactoring);
		fRefactoring= refactoring;
		fStyle= style;
		Assert.isTrue(checkStyle(fStyle));
	}

	/*
	 * (Non-Javadoc)
	 * Method defined int IRunnableWithProgress
	 */
	public void run(IProgressMonitor pm) throws CoreException {
		try {
			fStatus= null;
			if ((fStyle & PRECONDITIONS) == PRECONDITIONS)
				fStatus= fRefactoring.checkPreconditions(pm);
			else if ((fStyle & ACTIVATION) == ACTIVATION)
				fStatus= fRefactoring.checkActivation(pm);
			else if ((fStyle & INPUT) == INPUT)
				fStatus= fRefactoring.checkInput(pm);
		} finally {
			pm.done();
		}
	}

	/**
	 * Returns the outcome of the operation or <code>null</code> if an exception 
	 * has occured when performing the operation.
	 * 
	 * @return the <code>RefactoringStatus</code> returned from 
	 *  <code>IRefactoring.checkPreconditions</code>.
	 */
	public RefactoringStatus getStatus() {
		return fStatus;
	}
	
	private boolean checkStyle(int style) {
		return style < LAST;
	}

}
