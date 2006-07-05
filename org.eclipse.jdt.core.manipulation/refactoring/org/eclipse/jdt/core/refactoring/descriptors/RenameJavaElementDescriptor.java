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

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.IJavaElement;

/**
 * Refactoring descriptor for the rename java element refactoring.
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
public final class RenameJavaElementDescriptor extends JavaRefactoringDescriptor {

	/**
	 * Creates a new refactoring descriptor.
	 * 
	 * @param id
	 *            the unique id of the refactoring
	 */
	public RenameJavaElementDescriptor(final String id) {
		super(id);
	}

	/**
	 * Sets the Java element to be renamed.
	 * <p>
	 * Note: If the Java element to be renamed is of type
	 * {@link IJavaElement#JAVA_PROJECT}, clients are required to to set the
	 * project name to <code>null</code>.
	 * </p>
	 * 
	 * @param element
	 *            the Java element to be renamed
	 */
	public void setJavaElement(final IJavaElement element) {
		Assert.isNotNull(element);
		final String project= getProject();
		if (element.getElementType() == IJavaElement.JAVA_PROJECT && project != null)
			throw new IllegalArgumentException("Project name must be null"); //$NON-NLS-1$
		fArguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, elementToHandle(project, element));
	}

	/**
	 * Sets the new name to rename the Java element to.
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
	 * Note: If the Java element to be renamed is of type
	 * {@link IJavaElement#JAVA_PROJECT}, clients are required to to set the
	 * project name to <code>null</code>.
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
}