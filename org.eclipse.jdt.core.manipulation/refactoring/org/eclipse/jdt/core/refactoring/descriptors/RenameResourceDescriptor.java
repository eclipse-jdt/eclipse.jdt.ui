/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.refactoring.descriptors;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

import org.eclipse.jdt.internal.core.refactoring.descriptors.DescriptorMessages;
import org.eclipse.jdt.internal.core.refactoring.descriptors.JavaRefactoringDescriptorUtil;

/**
 * Refactoring descriptor for the rename resource refactoring.
 * <p>
 * An instance of this refactoring descriptor may be obtained by calling
 * {@link RefactoringContribution#createDescriptor()} on a refactoring
 * contribution requested by invoking
 * {@link RefactoringCore#getRefactoringContribution(String)} with the
 * appropriate refactoring id.
 * </p>
 * <p>
 * Note: this class is not intended to be instantiated by clients.
 * </p>
 *
 * @since 1.1
 *
 * @deprecated since 1.2, use {@link org.eclipse.ltk.core.refactoring.resource.RenameResourceDescriptor} from
 * <code>org.eclipse.ltk.core.refactoring</code> instead.
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@Deprecated
public final class RenameResourceDescriptor extends JavaRefactoringDescriptor {

	/** The name attribute */
	private String fName= null;

	/** The resource path attribute (full path) */
	private IPath fResourcePath= null;

	/**
	 * Creates a new refactoring descriptor.
	 */
	@Deprecated
	public RenameResourceDescriptor() {
		super(IJavaRefactorings.RENAME_RESOURCE);
	}

	/**
	 * Creates a new refactoring descriptor.
	 *
	 * @param project
	 *            the non-empty name of the project associated with this
	 *            refactoring, or <code>null</code> for a workspace
	 *            refactoring
	 * @param description
	 *            a non-empty human-readable description of the particular
	 *            refactoring instance
	 * @param comment
	 *            the human-readable comment of the particular refactoring
	 *            instance, or <code>null</code> for no comment
	 * @param arguments
	 * 			  a map of arguments that will be persisted and describes
	 * 			  all settings for this refactoring
	 * @param flags
	 *            the flags of the refactoring descriptor
	 *
	 * @throws IllegalArgumentException if the argument map contains invalid keys/values
	 *
	 * @since 1.2
	 */
	@Deprecated
	public RenameResourceDescriptor(String project, String description, String comment, Map<String, String> arguments, int flags) {
		super(IJavaRefactorings.RENAME_RESOURCE, project, description, comment, arguments, flags);
		fResourcePath= JavaRefactoringDescriptorUtil.getResourcePath(arguments, ATTRIBUTE_INPUT, project);
		fName= JavaRefactoringDescriptorUtil.getString(arguments, ATTRIBUTE_NAME);
	}

	@Deprecated
	@Override
	protected void populateArgumentMap() {
		super.populateArgumentMap();
		JavaRefactoringDescriptorUtil.setResourcePath(fArguments, ATTRIBUTE_INPUT, getProject(), fResourcePath);
		JavaRefactoringDescriptorUtil.setString(fArguments, ATTRIBUTE_NAME, fName);
	}

	/**
	 * Sets the new name to rename the resource to.
	 *
	 * @param name
	 *            the non-empty new name to set
	 */
	@Deprecated
	public void setNewName(final String name) {
		Assert.isNotNull(name);
		Assert.isLegal(!"".equals(name), "Name must not be empty"); //$NON-NLS-1$//$NON-NLS-2$
		fName= name;
	}

	/**
	 * Returns the new name to rename the resource to.
	 *
	 * @return
	 *            the new name to rename the resource to
	 *
	 * @since 1.2
	 */
	@Deprecated
	public String getNewName() {
		return fName;
	}

	/**
	 * Sets the project name of this refactoring.
	 * <p>
	 * Note: If the resource to be renamed is of type {@link IResource#PROJECT},
	 * clients are required to to set the project name to <code>null</code>.
	 * </p>
	 * <p>
	 * The default is to associate the refactoring with the workspace.
	 * </p>
	 *
	 * @param project
	 *            the non-empty project name to set, or <code>null</code> for
	 *            the workspace
	 *
	 * @see #getProject()
	 */
	@Deprecated
	@Override
	public void setProject(final String project) {
		super.setProject(project);
	}

	/**
	 * Sets the resource to be renamed.
	 * <p>
	 * Note: If the resource to be renamed is of type {@link IResource#PROJECT},
	 * clients are required to to set the project name to <code>null</code>.
	 * </p>
	 *
	 * @param resource
	 *            the resource to be renamed
	 */
	@Deprecated
	public void setResource(final IResource resource) {
		Assert.isNotNull(resource);
		fResourcePath= resource.getFullPath();
	}


	/**
	 * Returns the path of the resource to rename.
	 *
	 * @return
	 *          the path of the resource to rename
	 *
	 * @since 1.2
	 */
	@Deprecated
	public IPath getResourcePath() {
		return fResourcePath;
	}

	@Deprecated
	@Override
	public RefactoringStatus validateDescriptor() {
		RefactoringStatus status= super.validateDescriptor();
		if (fResourcePath == null)
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameResourceDescriptor_no_resource));
		if (fName == null || "".equals(fName)) //$NON-NLS-1$
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameResourceDescriptor_no_new_name));
		if (fResourcePath.segmentCount() == 1 && getProject() != null)
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameResourceDescriptor_project_constraint));
		return status;
	}
}