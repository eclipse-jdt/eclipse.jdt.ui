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
package org.eclipse.jdt.internal.corext.refactoring.reorg2;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ICopyQueries;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdatingRefactoring;

interface IReorgPolicy extends IReorgEnablementPolicy, IQualifiedNameUpdatingRefactoring {
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException;
	public RefactoringStatus setDestination(IResource resource) throws JavaModelException;
	public RefactoringStatus setDestination(IJavaElement javaElement) throws JavaModelException;
	public IResource[] getResources();
	public IJavaElement[] getJavaElements();
	public IResource getResourceDestination();
	public IJavaElement getJavaElementDestination();

	public boolean canUpdateReferences();
	public void setUpdateReferences(boolean update);
	public boolean getUpdateReferences();
	public boolean canUpdateQualifiedNames();
	
	static interface ICopyPolicy extends IReorgPolicy{
		public IChange createChange(IProgressMonitor pm, ICopyQueries copyQueries) throws JavaModelException;
	}
	static interface IMovePolicy extends IReorgPolicy{
		public IChange createChange(IProgressMonitor pm) throws JavaModelException;
	}
}
