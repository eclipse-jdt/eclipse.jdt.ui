/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.refactoring.descriptors.ExtractClassDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringContribution;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractClassRefactoring;

public class ExtractClassContribution extends JavaRefactoringContribution {

	public ExtractClassContribution() {
	}

	public RefactoringDescriptor createDescriptor(String id, String project, String description, String comment, Map arguments, int flags) throws IllegalArgumentException {
		return new ExtractClassDescriptor(project, description, comment, arguments, flags);
	}
	
	public RefactoringDescriptor createDescriptor() {
		return new ExtractClassDescriptor();
	}

	public Refactoring createRefactoring(JavaRefactoringDescriptor descriptor, RefactoringStatus status) throws CoreException {
		if (!(descriptor instanceof ExtractClassDescriptor)) {
			status.addFatalError(RefactoringCoreMessages.ExtractClassContribution_error_unknown_descriptor);
			return null;
		}
		return new ExtractClassRefactoring((ExtractClassDescriptor) descriptor);
	}

}
