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

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.ICompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ICopyQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.IReorgPolicy.ICopyPolicy;

public final class CopyRefactoring2 extends Refactoring{

	private ICopyQueries fCopyQueries;
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
	
	public void setQueries(ICopyQueries copyQueries){
		Assert.isNotNull(copyQueries);
		fCopyQueries= copyQueries;
	}

	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try {
			return new RefactoringStatus();
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
		Assert.isNotNull(fCopyQueries);
		return fCopyPolicy.checkInput(pm);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		Assert.isNotNull(fCopyQueries);
		Assert.isTrue(fCopyPolicy.getJavaElementDestination() == null || fCopyPolicy.getResourceDestination() == null);
		Assert.isTrue(fCopyPolicy.getJavaElementDestination() != null || fCopyPolicy.getResourceDestination() != null);		
		try {
			CompositeChange resultComposite= new CompositeChange(){
				public boolean isUndoable(){
					return false; 
				}
			};
			IChange change= fCopyPolicy.createChange(pm, fCopyQueries);
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
