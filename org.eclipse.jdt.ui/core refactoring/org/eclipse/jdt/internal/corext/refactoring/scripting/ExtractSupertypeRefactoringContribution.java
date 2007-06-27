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

import org.eclipse.jdt.core.refactoring.descriptors.ExtractSuperclassDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringContribution;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractSupertypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractSupertypeRefactoring;

/**
 * Refactoring contribution for the extract supertype refactoring.
 * 
 * @since 3.2
 */
public final class ExtractSupertypeRefactoringContribution extends JavaRefactoringContribution {

	/**
	 * {@inheritDoc}
	 */
	public final Refactoring createRefactoring(JavaRefactoringDescriptor descriptor, RefactoringStatus status) throws CoreException {
		ExtractSupertypeRefactoring refactoring= new ExtractSupertypeRefactoring(new ExtractSupertypeProcessor(null, null));
		status.merge(refactoring.initialize(new JavaRefactoringArguments(descriptor.getProject(), retrieveArgumentMap(descriptor))));
		return refactoring;
	}

	public RefactoringDescriptor createDescriptor() {
		return new ExtractSuperclassDescriptor();
	}
	
	public RefactoringDescriptor createDescriptor(String id, String project, String description, String comment, Map arguments, int flags) {
		return new ExtractSuperclassDescriptor(project, description, comment, arguments, flags);
	}
}
