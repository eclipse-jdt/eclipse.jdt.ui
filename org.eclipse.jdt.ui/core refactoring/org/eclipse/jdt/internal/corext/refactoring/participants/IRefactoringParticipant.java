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
package org.eclipse.jdt.internal.corext.refactoring.participants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;


public interface IRefactoringParticipant {
	
	public void initialize(IRefactoringProcessor processor, Object element) throws CoreException;
	
	public boolean isAvailable() throws CoreException;
	
	public boolean operatesOn(Object element);
	
	public RefactoringStatus checkActivation() throws CoreException;
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException;
	
	public Change createChange(IProgressMonitor pm) throws CoreException;
}
