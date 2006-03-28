/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

/**
 * Partial implementation of a java refactoring contribution.
 * 
 * @since 3.2
 */
public abstract class JavaRefactoringContribution extends RefactoringContribution {

	/**
	 * {@inheritDoc}
	 */
	public final RefactoringDescriptor createDescriptor(final String id, final String project, final String description, final String comment, final Map arguments, final int flags) {
		return new JavaRefactoringDescriptor(this, id, project, description, comment, arguments, flags);
	}

	/**
	 * Creates the a new refactoring instance.
	 * 
	 * @param descriptor
	 *            the refactoring descriptor
	 * @return the refactoring, or <code>null</code>
	 * @throws CoreException
	 *             if an error occurs while creating the refactoring
	 */
	public abstract Refactoring createRefactoring(RefactoringDescriptor descriptor) throws CoreException;

	/**
	 * {@inheritDoc}
	 */
	public final Map retrieveArgumentMap(final RefactoringDescriptor descriptor) {
		Assert.isNotNull(descriptor);
		if (descriptor instanceof JavaRefactoringDescriptor)
			return ((JavaRefactoringDescriptor) descriptor).getArguments();
		return super.retrieveArgumentMap(descriptor);
	}
}
