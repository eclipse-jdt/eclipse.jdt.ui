/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.refactoring.descriptors;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

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
 * Clients must first set the basic refactoring descriptor attributes such as
 * the project name, the description, the comment and the flags before setting
 * any other attributes.
 * </p>
 * <p>
 * Note: this class is not intended to be instantiated by clients.
 * </p>
 * 
 * @since 3.3
 */
public final class RenameResourceDescriptor extends JavaRefactoringDescriptor {

	/**
	 * Creates a new refactoring descriptor.
	 */
	public RenameResourceDescriptor() {
		super(IJavaRefactorings.RENAME_RESOURCE);
	}

	/**
	 * Sets the new name to rename the resource to.
	 * 
	 * @param name
	 *            the non-empty new name to set
	 */
	public void setNewName(final String name) {
		Assert.isNotNull(name);
		Assert.isLegal(!"".equals(name), "Name must not be empty"); //$NON-NLS-1$//$NON-NLS-2$
		fArguments.put(JavaRefactoringDescriptor.ATTRIBUTE_NAME, name);
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
	public void setResource(final IResource resource) {
		Assert.isNotNull(resource);
		final String project= getProject();
		if (resource.getType() == IResource.PROJECT && project != null)
			throw new IllegalArgumentException("Project name must be null"); //$NON-NLS-1$
		fArguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, resourceToHandle(project, resource));
	}
}