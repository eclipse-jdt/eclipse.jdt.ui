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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceProcessors;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.MoveProcessor;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;

public class JavaMoveProcessor extends MoveProcessor implements IQualifiedNameUpdating {

	private IReorgQueries fReorgQueries;
	private IMovePolicy fMovePolicy;
	private boolean fWasCanceled;

	private static final String IDENTIFIER= "org.eclipse.jdt.ui.MoveProcessor"; //$NON-NLS-1$
	
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

	public static JavaMoveProcessor create(IResource[] resources, IJavaElement[] javaElements, CodeGenerationSettings settings) throws JavaModelException{
		IMovePolicy movePolicy= ReorgPolicyFactory.createMovePolicy(resources, javaElements, settings);
		if (! isAvailable(movePolicy))
			return null;
		return new JavaMoveProcessor(movePolicy);
	}

	private JavaMoveProcessor(IMovePolicy movePolicy) {
		fMovePolicy= movePolicy;
	}
	
	private static boolean isAvailable(IMovePolicy movePolicy) throws JavaModelException{
		return movePolicy.canEnable();
	}
	
	//---- MoveProcessor ------------------------------------------------------

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
		return isAvailable(fMovePolicy);
	}
	
	public MoveParticipant[] loadElementParticipants() throws CoreException {
		Object[] elements= getElements();
		String[] natures= getAffectedProjectNatures();
		List result= new ArrayList();
		for (int i= 0; i < elements.length; i++) {
			result.addAll(Arrays.asList(ParticipantManager.getMoveParticipants(this, elements[i], natures, getSharedParticipants())));
		}
		return (MoveParticipant[])result.toArray(new MoveParticipant[result.size()]);
	}
	
	public RefactoringParticipant[] loadDerivedParticipants() throws CoreException {
		IJavaElement[] elements= fMovePolicy.getJavaElements();
		String[] natures= getAffectedProjectNatures();
		ResourceModifications rm= new ResourceModifications();
		rm.setMoveArguments(getArguments());
		List derivedElements= new ArrayList();
		for (int i= 0; i < elements.length; i++) {
			IJavaElement element= elements[i];
			if (element instanceof ICompilationUnit ) {
				ICompilationUnit unit= (ICompilationUnit)element;
				IResource resource= element.getResource();
				if (resource != null) {
					rm.addMove(resource);
				}
				derivedElements.addAll(Arrays.asList(unit.getTypes()));
			} else if (element instanceof IPackageFragmentRoot) {
				IResource resource= element.getResource();
				if (resource != null)
					rm.addMove(resource);
			} else if (element instanceof IPackageFragment) {
				IPackageFragment pack= (IPackageFragment)element;
				IContainer container= (IContainer)pack.getResource();
				if (container == null)
					continue;
				IJavaElement destination= fMovePolicy.getJavaElementDestination();
				if (destination.getResource() == null)
					continue;
				IPath path= destination.getResource().getFullPath();
				path= path.append(pack.getElementName().replace('.', '/'));
				IResource[] members= container.members();
				int files= 0;
				for (int m= 0; m < members.length; m++) {
					IResource member= members[m];
					if (member instanceof IFile) {
						files++;
						IFile file= (IFile)member;
						if ("class".equals(file.getFileExtension()) && file.isDerived()) //$NON-NLS-1$
							continue;
						rm.addMove(member);
					}
				}
				IFolder target= ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
				if (!target.exists()) {
					rm.addCreate(target);
				}
				rm.setMoveArguments(new MoveArguments(target, getUpdateReferences()));
				if (files == members.length) {
					rm.addDelete(container);
				}
			}
		}
		List result= new ArrayList();
		for (Iterator iter= derivedElements.iterator(); iter.hasNext();) {
			result.addAll(Arrays.asList(ParticipantManager.getMoveParticipants(this, iter.next(), natures, getSharedParticipants())));
		}
		result.addAll(Arrays.asList(rm.getParticipants(this, natures, getSharedParticipants())));
		return (RefactoringParticipant[])result.toArray(new RefactoringParticipant[result.size()]);
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

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
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

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
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

	public String getProcessorName() {
		return RefactoringCoreMessages.getString("MoveRefactoring.0"); //$NON-NLS-1$
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
}