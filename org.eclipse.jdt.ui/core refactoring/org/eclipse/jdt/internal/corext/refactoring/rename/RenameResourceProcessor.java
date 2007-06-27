/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameResourceDescriptor;

import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.code.ScriptableRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceProcessors;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ICommentProvider;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Resources;

public class RenameResourceProcessor extends RenameProcessor implements IScriptableRefactoring, ICommentProvider, INameUpdating {

	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameResourceProcessor"; //$NON-NLS-1$

	private String fComment;

	private String fNewElementName;

	private RenameModifications fRenameModifications;

	private IResource fResource;

	/**
	 * Creates a new rename resource processor.
	 * 
	 * @param resource
	 *            the resource, or <code>null</code> if invoked by scripting
	 */
	public RenameResourceProcessor(IResource resource) {
		fResource= resource;
		if (resource != null) {
			setNewElementName(resource.getName());
		}
	}

	public boolean canEnableComment() {
		return true;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			fRenameModifications= new RenameModifications();
			fRenameModifications.rename(fResource, new RenameArguments(getNewElementName(), getUpdateReferences()));

			ResourceChangeChecker checker= (ResourceChangeChecker) context.getChecker(ResourceChangeChecker.class);
			IResourceChangeDescriptionFactory deltaFactory= checker.getDeltaFactory();
			fRenameModifications.buildDelta(deltaFactory);

			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		return RefactoringStatus.create(Resources.checkInSync(fResource));
	}

	public RefactoringStatus checkNewElementName(String newName) throws JavaModelException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		IContainer c= fResource.getParent();
		if (c == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameResourceRefactoring_Internal_Error);

		if (c.findMember(newName) != null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameResourceRefactoring_alread_exists);

		if (!c.getFullPath().isValidSegment(newName))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameResourceRefactoring_invalidName);

		RefactoringStatus result= RefactoringStatus.create(c.getWorkspace().validateName(newName, fResource.getType()));
		if (!result.hasFatalError())
			result.merge(RefactoringStatus.create(c.getWorkspace().validatePath(createNewPath(newName), fResource.getType())));
		return result;
	}

	public Change createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			String project= null;
			if (fResource.getType() != IResource.PROJECT)
				project= fResource.getProject().getName();
			final String header= Messages.format(RefactoringCoreMessages.RenameResourceChange_descriptor_description, new String[] { fResource.getFullPath().toString(), getNewElementName()});
			final String description= Messages.format(RefactoringCoreMessages.RenameResourceChange_descriptor_description_short, fResource.getName());
			final String comment= new JDTRefactoringDescriptorComment(project, this, header).asString();
			final int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE | RefactoringDescriptor.BREAKING_CHANGE;
			final RenameResourceDescriptor descriptor= new RenameResourceDescriptor();
			descriptor.setProject(project);
			descriptor.setDescription(description);
			descriptor.setComment(comment);
			descriptor.setFlags(flags);
			descriptor.setResource(fResource);
			descriptor.setNewName(getNewElementName());
			return new DynamicValidationStateChange(new RenameResourceChange(descriptor, fResource, getNewElementName(), comment));
		} finally {
			pm.done();
		}
	}

	private String createNewPath(String newName) {
		return fResource.getFullPath().removeLastSegments(1).append(newName).toString();
	}

	public String[] getAffectedProjectNatures() throws CoreException {
		return ResourceProcessors.computeAffectedNatures(fResource);
	}

	public String getComment() {
		return fComment;
	}

	public String getCurrentElementName() {
		return fResource.getName();
	}

	public Object[] getElements() {
		return new Object[] { fResource};
	}

	public String getIdentifier() {
		return IDENTIFIER;
	}

	public Object getNewElement() {
		return ResourcesPlugin.getWorkspace().getRoot().findMember(createNewPath(getNewElementName()));
	}

	public String getNewElementName() {
		return fNewElementName;
	}

	public String getProcessorName() {
		return RefactoringCoreMessages.RenameResourceProcessor_name;
	}

	public boolean getUpdateReferences() {
		return true;
	}

	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String handle= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
			if (handle != null) {
				fResource= JavaRefactoringDescriptorUtil.handleToResource(extended.getProject(), handle);
				if (fResource == null || !fResource.exists())
					return ScriptableRefactoring.createInputFatalStatus(fResource, getRefactoring().getName(), IJavaRefactorings.RENAME_RESOURCE);
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				setNewElementName(name);
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	public boolean isApplicable() throws JavaModelException {
		return RefactoringAvailabilityTester.isRenameAvailable(fResource);
	}

	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants shared) throws CoreException {
		return fRenameModifications.loadParticipants(status, this, getAffectedProjectNatures(), shared);
	}

	public void setComment(final String comment) {
		fComment= comment;
	}

	public void setNewElementName(String newName) {
		Assert.isNotNull(newName);
		fNewElementName= newName;
	}
}