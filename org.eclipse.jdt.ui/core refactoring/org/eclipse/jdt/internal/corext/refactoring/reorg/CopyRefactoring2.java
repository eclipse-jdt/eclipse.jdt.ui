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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.ICompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.jdt.internal.corext.util.Resources;

public final class CopyRefactoring2 extends Refactoring{

	private INewNameQueries fNewNameQueries;
	private IReorgQueries fReorgQueries;
	private ICopyPolicy fCopyPolicy;
	
	public static boolean isAvailable(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		return isAvailable(ReorgPolicyFactory.createCopyPolicy(resources, javaElements, settings));
	}
	
	public static CopyRefactoring2 create(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		ICopyPolicy copyPolicy= ReorgPolicyFactory.createCopyPolicy(resources, javaElements, settings);
		if (! isAvailable(copyPolicy))
			return null;
		return new CopyRefactoring2(copyPolicy);
	}

	private static boolean isAvailable(ICopyPolicy copyPolicy) throws JavaModelException{
		return copyPolicy.canEnable();
	}
		
	private CopyRefactoring2(ICopyPolicy copyPolicy) {
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

	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try {
			RefactoringStatus result= new RefactoringStatus();
			result.merge(RefactoringStatus.create(Resources.checkInSync(fCopyPolicy.getResources())));
			IResource[] javaResources= ReorgUtils2.getResources(fCopyPolicy.getJavaElements());
			result.merge(RefactoringStatus.create(Resources.checkInSync(javaResources)));
			return result;
		} finally {
			pm.done();
		}
	}

	public Object getCommonParentForInputElements(){
		return new ParentChecker(fCopyPolicy.getResources(), fCopyPolicy.getJavaElements()).getCommonParent();
	}
	
	public RefactoringStatus setDestination(IJavaElement destination) throws JavaModelException{
		return fCopyPolicy.setDestination(destination);
	}

	public RefactoringStatus setDestination(IResource destination) throws JavaModelException{
		return fCopyPolicy.setDestination(destination);
	}

	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		Assert.isNotNull(fNewNameQueries, "Missing new name queries");
		Assert.isNotNull(fReorgQueries, "Missing reorg queries");
		return fCopyPolicy.checkInput(pm, fReorgQueries);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		Assert.isNotNull(fNewNameQueries);
		Assert.isTrue(fCopyPolicy.getJavaElementDestination() == null || fCopyPolicy.getResourceDestination() == null);
		Assert.isTrue(fCopyPolicy.getJavaElementDestination() != null || fCopyPolicy.getResourceDestination() != null);		
		try {
			CompositeChange resultComposite= new CompositeChange(){
				public boolean isUndoable(){
					return false; 
				}
			};
			IChange change= fCopyPolicy.createChange(pm, fNewNameQueries);
			if (change instanceof ICompositeChange){
				ICompositeChange subComposite= (ICompositeChange)change;
				resultComposite.addAll(subComposite.getChildren());
			} else{
				resultComposite.add(change);
			}
			return resultComposite;		
		} finally {
			pm.done();
		}
	}

	public String getName() {
		return "Copy";
	}
}
