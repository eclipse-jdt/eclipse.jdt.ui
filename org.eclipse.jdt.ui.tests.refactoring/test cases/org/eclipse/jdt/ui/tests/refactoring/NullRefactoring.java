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
package org.eclipse.jdt.ui.tests.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.NullChange;

/**
 * useful for tests
 */
public class NullRefactoring extends Refactoring {
	
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1);
		pm.worked(1);
		pm.done();
		return new NullChange("NullRefactoring");
	}


	public RefactoringStatus checkActivation(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1);
		pm.worked(1);
		pm.done();
		return new RefactoringStatus();
	}

	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1);
		pm.worked(1);
		pm.done();
		return new RefactoringStatus();
	}
	
	public String getName() {
		return "Null Refactoring";
	}
}
