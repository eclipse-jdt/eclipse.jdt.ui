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

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.jdt.internal.corext.util.Resources;

public final class CopyRefactoring extends Refactoring{

	private INewNameQueries fNewNameQueries;
	private IReorgQueries fReorgQueries;
	private ICopyPolicy fCopyPolicy;
	
	public static boolean isAvailable(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		return isAvailable(ReorgPolicyFactory.createCopyPolicy(resources, javaElements, settings));
	}
	
	public static CopyRefactoring create(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		ICopyPolicy copyPolicy= ReorgPolicyFactory.createCopyPolicy(resources, javaElements, settings);
		if (! isAvailable(copyPolicy))
			return null;
		return new CopyRefactoring(copyPolicy);
	}

	private static boolean isAvailable(ICopyPolicy copyPolicy) throws JavaModelException{
		return copyPolicy.canEnable();
	}
		
	private CopyRefactoring(ICopyPolicy copyPolicy) {
		fCopyPolicy= copyPolicy;
	}
	
	public void setNewNameQueries(INewNameQueries newNameQueries){
		Assert.isNotNull(newNameQueries);
		fNewNameQueries= newNameQueries;
	}

	public void setReorgQueries(IReorgQueries queries){
		Assert.isNotNull(queries);
		fReorgQueries= queries;
	}

	public RefactoringStatus checkActivation(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotNulls(fCopyPolicy.getResources()))));
		IResource[] javaResources= ReorgUtils.getResources(fCopyPolicy.getJavaElements());
		result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotNulls(javaResources))));
		return result;
	}

	public Object getCommonParentForInputElements(){
		return new ParentChecker(fCopyPolicy.getResources(), fCopyPolicy.getJavaElements()).getCommonParent();
	}
	
	public IJavaElement[] getJavaElements() {
		return fCopyPolicy.getJavaElements();
	}

	public IResource[] getResources() {
		return fCopyPolicy.getResources();
	}

	public RefactoringStatus setDestination(IJavaElement destination) throws JavaModelException{
		return fCopyPolicy.setDestination(destination);
	}

	public RefactoringStatus setDestination(IResource destination) throws JavaModelException{
		return fCopyPolicy.setDestination(destination);
	}

	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		Assert.isNotNull(fNewNameQueries, "Missing new name queries"); //$NON-NLS-1$
		Assert.isNotNull(fReorgQueries, "Missing reorg queries"); //$NON-NLS-1$
		return fCopyPolicy.checkInput(pm, fReorgQueries);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws CoreException {
		Assert.isNotNull(fNewNameQueries);
		Assert.isTrue(fCopyPolicy.getJavaElementDestination() == null || fCopyPolicy.getResourceDestination() == null);
		Assert.isTrue(fCopyPolicy.getJavaElementDestination() != null || fCopyPolicy.getResourceDestination() != null);		
		try {
			final ValidationStateChange result= new ValidationStateChange() {
				public boolean isUndoable(){
					return false; 
				}
			};
			IChange change= fCopyPolicy.createChange(pm, fNewNameQueries);
			if (change instanceof CompositeChange){
				CompositeChange subComposite= (CompositeChange)change;
				result.merge(subComposite);
			} else{
				result.add(change);
			}
			return result;		
		} finally {
			pm.done();
		}
	}

	public String getName() {
		return RefactoringCoreMessages.getString("CopyRefactoring.0"); //$NON-NLS-1$
	}
}
