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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Description
 */
public class ExtractConstantDescriptor extends RefactoringDescriptor {

	/**
	 * @param id
	 * @param project
	 * @param description
	 * @param comment
	 * @param flags
	 */
	protected ExtractConstantDescriptor(String id, String project, String description, String comment, int flags) {
		super(id, project, description, comment, flags);
	}

	/**
	 * {@inheritDoc}
	 */
	public Refactoring createRefactoring(RefactoringStatus status) throws CoreException {
		return null;
	}

}
