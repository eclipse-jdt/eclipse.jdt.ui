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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
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
	 * Constant describing the jar migration flag (value: <code>65536</code>).
	 * <p>
	 * Clients should set this flag to indicate that the refactoring can be
	 * stored to a JAR file in order to be accessible to the Migrate JAR File
	 * refactoring, regardless whether there is a source attachment to the JAR
	 * file or not. If this flag is set, <code>JAR_REFACTORING</code> should
	 * be set as well.
	 * </p>
	 * 
	 * @see #JAR_REFACTORING
	 */
	public static final int JAR_MIGRATION= 1 << 16;

	/**
	 * Constant describing the jar refactoring flag (value: <code>524288</code>).
	 * <p>
	 * Clients should set this flag to indicate that the refactoring in
	 * principle can be performed on binary elements originating from a JAR
	 * file. Refactorings which are able to run on binary elements, but require
	 * a correctly configured source attachment to work must set the
	 * <code>JAR_SOURCE_ATTACHMENT</code> flag as well.
	 * </p>
	 * 
	 * @see #JAR_SOURCE_ATTACHMENT
	 */
	public static final int JAR_REFACTORING= 1 << 19;

	/**
	 * Constant describing the jar source attachment flag (value:
	 * <code>262144</code>).
	 * <p>
	 * Clients should set this flag to indicate that the refactoring can be
	 * performed on binary elements originating from a JAR file if and only if
	 * it has a correctly configured source attachment.
	 * </p>
	 * 
	 * @see #JAR_REFACTORING
	 */
	public static final int JAR_SOURCE_ATTACHMENT= 1 << 18;

	/** The argument map */
	final Map fArguments= new HashMap();

	/**
	 * Creates a new java refactoring descriptor.
	 * 
	 * @param id
	 *            the unique id of the refactoring
	 */
	JavaRefactoringDescriptor(final String id) {
		super(id, null, DescriptorMessages.JavaRefactoringDescriptor_not_available, null, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
	}

	/**
	 * {@inheritDoc}
	 */
	public final Refactoring createRefactoring(final RefactoringStatus status) throws CoreException {
		Refactoring refactoring= null;
		final String id= getID();
		final RefactoringContribution contribution= RefactoringCore.getRefactoringContribution(id);
		if (contribution != null) {

			final RefactoringDescriptor descriptor= contribution.createDescriptor(id, getProject(), getDescription(), getComment(), fArguments, getFlags());
			if (descriptor != null) {
				refactoring= descriptor.createRefactoring(status);
			} else
				JavaManipulationPlugin.log(new Status(IStatus.ERROR, JavaManipulationPlugin.getPluginId(), 0, MessageFormat.format(DescriptorMessages.JavaRefactoringDescriptor_no_resulting_descriptor, new Object[] { id}), null));
		}
		return refactoring;
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
	 * 
	 * @see #JAR_MIGRATION
	 * @see #JAR_REFACTORING
	 * @see #JAR_SOURCE_ATTACHMENT
	 */
	public final void setFlags(final int flags) {
		super.setFlags(flags);
	}

	/**
	 * Sets the project name of this refactoring.
	 * <p>
	 * The default is to associate the refactoring with the workspace.
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