/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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

import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringContribution;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.UseSupertypeDescriptor;

import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.structure.UseSuperTypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.UseSuperTypeRefactoring;

/**
 * Refactoring contribution for the use supertype refactoring.
 * 
 * @since 3.2
 */
public final class UseSupertypeRefactoringContribution extends JavaRefactoringContribution {

	/**
	 * {@inheritDoc}
	 */
	public final Refactoring createRefactoring(final JavaRefactoringDescriptor descriptor, RefactoringStatus status) throws CoreException {
		UseSuperTypeRefactoring refactoring= new UseSuperTypeRefactoring(new UseSuperTypeProcessor(null, null));
		status.merge(refactoring.initialize(new JavaRefactoringArguments(descriptor.getProject(), retrieveArgumentMap(descriptor))));
		return refactoring;
	}

	public RefactoringDescriptor createDescriptor() {
		return new UseSupertypeDescriptor();
	}

	public RefactoringDescriptor createDescriptor(String id, String project, String description, String comment, Map arguments, int flags) {
		return new UseSupertypeDescriptor(project, description, comment, arguments, flags);
	}
}
