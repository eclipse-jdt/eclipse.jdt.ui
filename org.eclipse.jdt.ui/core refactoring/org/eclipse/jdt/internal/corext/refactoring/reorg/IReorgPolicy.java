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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdatingRefactoring;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

interface IReorgPolicy extends IReorgEnablementPolicy, IQualifiedNameUpdatingRefactoring {
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, IReorgQueries reorgQueries) throws JavaModelException;
	public RefactoringStatus setDestination(IResource resource) throws JavaModelException;
	public RefactoringStatus setDestination(IJavaElement javaElement) throws JavaModelException;
	
	public boolean canChildrenBeDestinations(IResource resource);
	public boolean canChildrenBeDestinations(IJavaElement javaElement);
	public boolean canElementBeDestination(IResource resource);
	public boolean canElementBeDestination(IJavaElement javaElement);
	
	public IResource[] getResources();
	public IJavaElement[] getJavaElements();
	
	public IResource getResourceDestination();
	public IJavaElement getJavaElementDestination();
	
	public boolean hasAllInputSet();

	public boolean canUpdateReferences();
	public void setUpdateReferences(boolean update);
	public boolean getUpdateReferences();
	public boolean canUpdateQualifiedNames();
	
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) throws CoreException;
	
	static interface ICopyPolicy extends IReorgPolicy{
		public Change createChange(IProgressMonitor pm, INewNameQueries copyQueries) throws JavaModelException;
	}
	static interface IMovePolicy extends IReorgPolicy{
		public Change createChange(IProgressMonitor pm) throws JavaModelException;
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException;
	}
}
