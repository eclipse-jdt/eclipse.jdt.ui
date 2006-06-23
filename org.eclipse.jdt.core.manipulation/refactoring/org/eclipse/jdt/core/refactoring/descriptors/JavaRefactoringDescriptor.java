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

import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.jdt.internal.core.refactoring.descriptors.DescriptorMessages;

/**
 * Partial implementation of a java refactoring descriptor.
 * <p>
 * This class provides features common to all Java refactorings.
 * </p>
 * <p>
 * Note: this class is not intended to be extended outside the refactoring
 * framework.
 * </p>
 * 
 * @since 3.3
 */
public abstract class JavaRefactoringDescriptor extends RefactoringDescriptor {

	/**
	 * Creates a new java refactoring descriptor.
	 * 
	 * @param id
	 *            the unique id of the refactoring
	 */
	protected JavaRefactoringDescriptor(final String id) {
		super(id, null, DescriptorMessages.JavaRefactoringDescriptor_not_available, null, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
	}

	/**
	 * Sets the details comment of this refactoring.
	 * <p>
	 * This information is used in the user interface to show additional details
	 * about the performed refactoring. The default is to use no details
	 * comment.
	 * </p>
	 * 
	 * @param comment
	 *            the details comment to set, or <code>null</code> to set no
	 *            details comment
	 * 
	 * @see #getComment()
	 */
	public final void setComment(final String comment) {
		super.setComment(comment);
	}

	/**
	 * Sets the description of this refactoring.
	 * <p>
	 * This information is used to label a refactoring in the user interface.
	 * The default is an unspecified, but legal description.
	 * </p>
	 * 
	 * @param description
	 *            the non-empty description of the refactoring to set
	 * 
	 * @see #getDescription()
	 */
	public final void setDescription(final String description) {
		super.setDescription(description);
	}

	/**
	 * Sets the flags of this refactoring.
	 * <p>
	 * The default is
	 * <code>RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE</code>,
	 * unless overridden by a concrete subclass. Clients may use refactoring
	 * flags to indicate special capabilities of Java refactorings.
	 * </p>
	 * 
	 * @param flags
	 *            the flags to set, or <code>RefactoringDescriptor.NONE</code>
	 *            to clear the flags
	 * 
	 * @see #getFlags()
	 * 
	 * @see RefactoringDescriptor#NONE
	 * @see RefactoringDescriptor#STRUCTURAL_CHANGE
	 * @see RefactoringDescriptor#BREAKING_CHANGE
	 * @see RefactoringDescriptor#MULTI_CHANGE
	 */
	public final void setFlags(final int flags) {
		super.setFlags(flags);
	}

	/**
	 * Sets the project name of this refactoring.
	 * <p>
	 * The default is to associated the refactoring with the workspace.
	 * Subclasses should call this method with the project name associated with
	 * the refactoring's input elements, if available.
	 * </p>
	 * 
	 * @param project
	 *            the non-empty project name to set, or <code>null</code> for
	 *            the workspace
	 * 
	 * @see #getProject()
	 */
	public final void setProject(final String project) {
		super.setProject(project);
	}
}