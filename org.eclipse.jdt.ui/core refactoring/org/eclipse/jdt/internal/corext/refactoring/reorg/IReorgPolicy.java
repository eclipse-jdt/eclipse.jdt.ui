/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IScriptableRefactoring;

public interface IReorgPolicy extends IReferenceUpdating, IQualifiedNameUpdating, IScriptableRefactoring {

	public ChangeDescriptor getDescriptor();

	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor, CheckConditionsContext context, IReorgQueries queries) throws CoreException;
	public RefactoringStatus setDestination(IResource resource) throws JavaModelException;
	public RefactoringStatus setDestination(IJavaElement javaElement) throws JavaModelException;
	
	public boolean canEnable() throws JavaModelException;
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
	public boolean canUpdateQualifiedNames();
	
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) throws CoreException;
	
	public String getPolicyId();

	public static interface ICopyPolicy extends IReorgPolicy{
		public Change createChange(IProgressMonitor monitor, INewNameQueries queries) throws JavaModelException;
		public ReorgExecutionLog getReorgExecutionLog();
	}
	public static interface IMovePolicy extends IReorgPolicy{
		public Change createChange(IProgressMonitor monitor) throws JavaModelException;
		public Change postCreateChange(Change[] participantChanges, IProgressMonitor monitor) throws CoreException;
		public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries);
		public boolean isTextualMove();
		public CreateTargetExecutionLog getCreateTargetExecutionLog();
	}
}
