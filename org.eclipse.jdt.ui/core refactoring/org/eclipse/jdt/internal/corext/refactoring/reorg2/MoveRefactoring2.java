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
import org.eclipse.jdt.internal.corext.refactoring.reorg2.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdatingRefactoring;

public class MoveRefactoring2 extends Refactoring implements IQualifiedNameUpdatingRefactoring{

	private IMovePolicy fMovePolicy;

	public static boolean isAvailable(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		return isAvailable(ReorgPolicyFactory.createMovePolicy(resources, javaElements, settings));
	}

	public static MoveRefactoring2 create(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		IMovePolicy movePolicy= ReorgPolicyFactory.createMovePolicy(resources, javaElements, settings);
		if (! isAvailable(movePolicy))
			return null;
		return new MoveRefactoring2(movePolicy);
	}

	private MoveRefactoring2(IMovePolicy movePolicy) {
		fMovePolicy= movePolicy;
	}
	
	private static boolean isAvailable(IMovePolicy copyPolicy) throws JavaModelException{
		return copyPolicy.canEnable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try {
			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}

	public Object getCommonParentForInputElements(){
		return new ParentChecker(fMovePolicy.getResources(), fMovePolicy.getJavaElements()).getCommonParent();
	}
	
	public RefactoringStatus setDestination(IJavaElement destination) throws JavaModelException{
		return fMovePolicy.setDestination(destination);
	}

	public RefactoringStatus setDestination(IResource destination) throws JavaModelException{
		return fMovePolicy.setDestination(destination);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		return fMovePolicy.checkInput(pm);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		Assert.isTrue(fMovePolicy.getJavaElementDestination() == null || fMovePolicy.getResourceDestination() == null);
		Assert.isTrue(fMovePolicy.getJavaElementDestination() != null || fMovePolicy.getResourceDestination() != null);		
		try {
			CompositeChange resultComposite= new CompositeChange(){
				public boolean isUndoable(){
					return false; 
				}
			};
			IChange change= fMovePolicy.createChange(pm);
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

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return "Move";
	}
	
	public boolean canUpdateReferences(){
		return fMovePolicy.canUpdateReferences();
	}
	
	public void setUpdateReferences(boolean update){
		fMovePolicy.setUpdateReferences(update);
	}
	
	public boolean getUpdateReferences() {
		return fMovePolicy.getUpdateReferences();
	}
	
	public boolean canEnableQualifiedNameUpdating() {
		return fMovePolicy.canEnableQualifiedNameUpdating();
	}
	
	public boolean canUpdateQualifiedNames() {
		return fMovePolicy.canUpdateQualifiedNames();
	}
	
	public String getFilePatterns() {
		return fMovePolicy.getFilePatterns();
	}
	
	public boolean getUpdateQualifiedNames() {
		return fMovePolicy.getUpdateQualifiedNames();
	}
	
	public void setFilePatterns(String patterns) {
		fMovePolicy.setFilePatterns(patterns);
	}
	
	public void setUpdateQualifiedNames(boolean update) {
		fMovePolicy.setUpdateQualifiedNames(update);
	}
}
