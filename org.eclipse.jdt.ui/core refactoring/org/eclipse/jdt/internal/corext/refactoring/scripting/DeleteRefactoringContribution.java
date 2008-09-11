/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.scripting;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.DeleteRefactoring;

import org.eclipse.jdt.core.refactoring.descriptors.DeleteDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringContribution;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaDeleteProcessor;

/**
 * Refactoring contribution for the delete refactoring.
 *
 * @since 3.2
 */
public final class DeleteRefactoringContribution extends JavaRefactoringContribution {

	/**
	 * {@inheritDoc}
	 */
	public final Refactoring createRefactoring(JavaRefactoringDescriptor descriptor, RefactoringStatus status) throws CoreException {
		JavaRefactoringArguments arguments= new JavaRefactoringArguments(descriptor.getProject(), retrieveArgumentMap(descriptor));
		JavaDeleteProcessor processor= new JavaDeleteProcessor(arguments, status);
		return new DeleteRefactoring(processor);
	}

	public RefactoringDescriptor createDescriptor() {
		return new DeleteDescriptor();
	}

	public RefactoringDescriptor createDescriptor(String id, String project, String description, String comment, Map arguments, int flags) {
		return new DeleteDescriptor(project, description, comment, arguments, flags);
	}
}
