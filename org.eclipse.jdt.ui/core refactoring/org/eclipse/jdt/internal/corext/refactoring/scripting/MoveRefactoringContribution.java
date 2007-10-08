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
package org.eclipse.jdt.internal.corext.refactoring.scripting;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.refactoring.descriptors.MoveDescriptor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringContribution;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.JavaMoveRefactoring;

/**
 * Refactoring contribution for the move refactoring.
 * 
 * @since 3.2
 */
public final class MoveRefactoringContribution extends JDTRefactoringContribution {

	/**
	 * {@inheritDoc}
	 */
	public RefactoringDescriptor createDescriptor() {
		return new MoveDescriptor();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public final Refactoring createRefactoring(final RefactoringDescriptor descriptor) throws CoreException {
		return new JavaMoveRefactoring(new JavaMoveProcessor(null));
	}
}
