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

import org.eclipse.core.resources.IProject;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;


public abstract class RefactoringProcessor extends PlatformObject {
	
	public abstract void initialize(Object[] elements) throws CoreException;
	
	public abstract boolean isAvailable() throws CoreException;
	
	public abstract String getProcessorName();
	
	public abstract int getStyle();
	
	public abstract IProject[] getAffectedProjects() throws CoreException;
	
	public abstract Object[] getElements();
	
	public abstract RefactoringStatus checkActivation() throws CoreException;
	
	public abstract RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException;
	
	public abstract Change createChange(IProgressMonitor pm) throws CoreException;	
}
