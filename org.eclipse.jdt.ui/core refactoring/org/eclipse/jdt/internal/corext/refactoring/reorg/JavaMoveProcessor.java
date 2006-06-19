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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceProcessors;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ICommentProvider;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.jdt.internal.corext.util.Resources;

public final class JavaMoveProcessor extends MoveProcessor implements IScriptableRefactoring, ICommentProvider, IQualifiedNameUpdating, IReorgDestinationValidator {

	private IReorgQueries fReorgQueries;
	private IMovePolicy fMovePolicy;
	private ICreateTargetQueries fCreateTargetQueries;
	private boolean fWasCanceled;
	private String fComment;

	public static final String ID_MOVE= "org.eclipse.jdt.ui.move"; //$NON-NLS-1$
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.MoveProcessor"; //$NON-NLS-1$
	
	public JavaMoveProcessor(IMovePolicy policy) {
		fMovePolicy= policy;
	}
	
	protected Object getDestination() {
		IJavaElement je= fMovePolicy.getJavaElementDestination();
		if (je != null)
			return je;
		return fMovePolicy.getResourceDestination();
	}
	
	public Object[] getElements() {
		List result= new ArrayList();
		result.addAll(Arrays.asList(fMovePolicy.getJavaElements()));
		result.addAll(Arrays.asList(fMovePolicy.getResources()));
		return result.toArray();
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}

	public boolean isApplicable() throws CoreException {
		return fMovePolicy.canEnable();
	}
	
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants shared) throws CoreException {
		return fMovePolicy.loadParticipants(status, this, getAffectedProjectNatures(), shared);
	}

	private String[] getAffectedProjectNatures() throws CoreException {
		String[] jNatures= JavaProcessors.computeAffectedNaturs(fMovePolicy.getJavaElements());
		String[] rNatures= ResourceProcessors.computeAffectedNatures(fMovePolicy.getResources());
		Set result= new HashSet();
		result.addAll(Arrays.asList(jNatures));
		result.addAll(Arrays.asList(rNatures));
		return (String[])result.toArray(new String[result.size()]);
	}

	public boolean wasCanceled() {
		return fWasCanceled;
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
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
	
	public boolean canChildrenBeDestinations(IJavaElement javaElement) {
		return fMovePolicy.canChildrenBeDestinations(javaElement);
	}
	public boolean canChildrenBeDestinations(IResource resource) {
		return fMovePolicy.canChildrenBeDestinations(resource);
	}
	public boolean canElementBeDestination(IJavaElement javaElement) {
		return fMovePolicy.canElementBeDestination(javaElement);
	}
	public boolean canElementBeDestination(IResource resource) {
		return fMovePolicy.canElementBeDestination(resource);
	}
	
	public void setReorgQueries(IReorgQueries queries){
		Assert.isNotNull(queries);
		fReorgQueries= queries;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		try{
			Assert.isNotNull(fReorgQueries);
			fWasCanceled= false;
			return fMovePolicy.checkFinalConditions(pm, context, fReorgQueries);
		} catch (OperationCanceledException e) {
			fWasCanceled= true;
			throw e;
		}
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		Assert.isTrue(fMovePolicy.getJavaElementDestination() == null || fMovePolicy.getResourceDestination() == null);
		Assert.isTrue(fMovePolicy.getJavaElementDestination() != null || fMovePolicy.getResourceDestination() != null);		
		try {
			final DynamicValidationStateChange result= new DynamicValidationStateChange(RefactoringCoreMessages.JavaMoveProcessor_change_name) { 
				public Change perform(IProgressMonitor pm2) throws CoreException {
					Change change= super.perform(pm2);
					Change[] changes= getChildren();
					for (int index= 0; index < changes.length; index++) {
						if (!(changes[index] instanceof TextEditBasedChange))
							return null;
					}
					return change;
				}
// TODO: enable for scripting
//				public ChangeDescriptor getDescriptor() {
//					return fMovePolicy.getDescriptor();
//				}
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
	
	public Change postCreateChange(Change[] participantChanges, IProgressMonitor pm) throws CoreException {
		return fMovePolicy.postCreateChange(participantChanges, pm);
	}

	public String getProcessorName() {
		return RefactoringCoreMessages.MoveRefactoring_0; 
	}
	
	public boolean canUpdateReferences(){
		return fMovePolicy.canUpdateReferences();
	}
	
	public void setUpdateReferences(boolean update){
		fMovePolicy.setUpdateReferences(update);
	}
	
	public boolean getUpdateReferences() {
		if (!canUpdateReferences())
			return false;
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
	
	public void setCreateTargetQueries(ICreateTargetQueries queries){
		Assert.isNotNull(queries);
		fCreateTargetQueries= queries;
	}
	/**
	 * @return the create target queries, or <code>null</code> if creating new targets is not supported
	 */
	public ICreateTargetQuery getCreateTargetQuery() {
		return fMovePolicy.getCreateTargetQuery(fCreateTargetQueries);
	}
	public boolean isTextualMove() {
		return fMovePolicy.isTextualMove();
	}

	public RefactoringStatus initialize(RefactoringArguments arguments) {
		setReorgQueries(new NullReorgQueries());
		final RefactoringStatus status= new RefactoringStatus();
		fMovePolicy= ReorgPolicyFactory.createMovePolicy(status, arguments);
		if (fMovePolicy != null && !status.hasFatalError()) {
			status.merge(fMovePolicy.initialize(arguments));
			setCreateTargetQueries(new LoggedCreateTargetQueries());
		}
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canEnableComment() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getComment() {
		return fComment;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setComment(String comment) {
		fComment= comment;
	}
}
