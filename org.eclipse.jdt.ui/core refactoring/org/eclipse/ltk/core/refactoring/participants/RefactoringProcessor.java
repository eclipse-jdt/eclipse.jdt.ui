/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.ltk.core.refactoring.participants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public abstract class RefactoringProcessor extends PlatformObject {
	
	public abstract Object[] getElements();
	
	public abstract String getIdentifier();
	
	public abstract String getProcessorName();
	
	public abstract int getStyle();
	
	public abstract boolean isApplicable() throws CoreException;
	
	public abstract RefactoringStatus checkInitialConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException;
	
	public abstract RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException;
	
	public abstract Change createChange(IProgressMonitor pm) throws CoreException;	
}
