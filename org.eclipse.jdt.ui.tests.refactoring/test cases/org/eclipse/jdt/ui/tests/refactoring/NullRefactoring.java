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

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;



/**
 * useful for tests
 */
public class NullRefactoring implements IRefactoring {
	
	public IChange createChange(IProgressMonitor pm){
		pm.beginTask("", 1);
		pm.worked(1);
		pm.done();
		return new NullChange("NullRefactoring");
	}

	public RefactoringStatus checkPreconditions(IProgressMonitor pm){
		pm.beginTask("", 1);
		pm.worked(1);
		pm.done();
		return new RefactoringStatus();
	}
	
	public String getName(){
		return "Null Refactoring";
	}
}
