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
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class MoveRefactoring extends Refactoring implements IQualifiedNameUpdatingRefactoring{

	private IReorgQueries fReorgQueries;
	private IMovePolicy fMovePolicy;
	private boolean fWasCanceled;

	public static boolean isAvailable(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		if (javaElements != null) {
			for (int i= 0; i < javaElements.length; i++) {
				IJavaElement element= javaElements[i];
				if ((element instanceof IType) && ((IType)element).isLocal())
					return false;
				if ((element instanceof IPackageDeclaration))
					return false;
			}
		}
		return isAvailable(ReorgPolicyFactory.createMovePolicy(resources, javaElements, settings));
	}

	public static MoveRefactoring create(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		IMovePolicy movePolicy= ReorgPolicyFactory.createMovePolicy(resources, javaElements, settings);
		if (! isAvailable(movePolicy))
			return null;
		return new MoveRefactoring(movePolicy);
	}

	private MoveRefactoring(IMovePolicy movePolicy) {
		fMovePolicy= movePolicy;
	}
	
	private static boolean isAvailable(IMovePolicy copyPolicy) throws JavaModelException{
		return copyPolicy.canEnable();
	}

	public boolean wasCanceled() {
		return fWasCanceled;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			RefactoringStatus result= new RefactoringStatus();
			result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotNulls(fMovePolicy.getResources()))));
			IResource[] javaResources= ReorgUtils.getResources(fMovePolicy.getJavaElements());
			result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotNulls(javaResources))));
			return result;
		} finally {
			pm.done();
		}
	}

	public Object getCommonParentForInputElements(){
		return new ParentChecker(fMovePolicy.getResources(), fMovePolicy.getJavaElements()).getCommonParent();
	}
	
	public IJavaElement[] getJavaElements() {
		return fMovePolicy.getJavaElements();
	}
	
	public IResource[] getResources() {
		return fMovePolicy.getResources();
	}

	public RefactoringStatus setDestination(IJavaElement destination) throws JavaModelException{
		return fMovePolicy.setDestination(destination);
	}

	public RefactoringStatus setDestination(IResource destination) throws JavaModelException{
		return fMovePolicy.setDestination(destination);
	}

	public void setReorgQueries(IReorgQueries queries){
		Assert.isNotNull(queries);
		fReorgQueries= queries;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		try{
			Assert.isNotNull(fReorgQueries);
			fWasCanceled= false;
			return fMovePolicy.checkInput(pm, fReorgQueries);
		} catch (OperationCanceledException e) {
			fWasCanceled= true;
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		Assert.isTrue(fMovePolicy.getJavaElementDestination() == null || fMovePolicy.getResourceDestination() == null);
		Assert.isTrue(fMovePolicy.getJavaElementDestination() != null || fMovePolicy.getResourceDestination() != null);		
		try {
			final ValidationStateChange result= new ValidationStateChange(){
				public Change perform(IProgressMonitor pm) throws CoreException {
					super.perform(pm);
					return null;
				}
			};
			Change change= fMovePolicy.createChange(pm);
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

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("MoveRefactoring.0"); //$NON-NLS-1$
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

	public boolean hasAllInputSet() {
		return fMovePolicy.hasAllInputSet();
	}
	public boolean hasDestinationSet() {
		return fMovePolicy.getJavaElementDestination() != null || fMovePolicy.getResourceDestination() != null;
	}
}