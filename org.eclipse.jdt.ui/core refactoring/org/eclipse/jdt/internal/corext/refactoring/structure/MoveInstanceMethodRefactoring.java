/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * Refactoring to move an instance method to another class.
 */
public final class MoveInstanceMethodRefactoring extends MoveRefactoring {

	/**
	 * Creates a new move instance method refactoring.
	 * 
	 * @param method
	 *        the method to move
	 * @param settings
	 *        the code generation settings to apply
	 * @return the created refactoring
	 * @throws JavaModelException
	 *         if the the refactoring could not be tested for availability
	 */
	public static MoveInstanceMethodRefactoring create(final IMethod method, final CodeGenerationSettings settings) throws JavaModelException {
		Assert.isNotNull(method);
		Assert.isNotNull(settings);
		Assert.isTrue(method.exists() && !method.isConstructor() && !method.isBinary() && !method.isReadOnly());
		return new MoveInstanceMethodRefactoring(new MoveInstanceMethodProcessor(method, settings));
	}

	/**
	 * Creates a new move instance method refactoring.
	 * 
	 * @param processor
	 *        the processor to use
	 */
	private MoveInstanceMethodRefactoring(final MoveInstanceMethodProcessor processor) {
		super(processor);
	}

	/**
	 * Returns the move instance method processor
	 * 
	 * @return the move processor
	 */
	public final MoveInstanceMethodProcessor getMoveMethodProcessor() {
		return (MoveInstanceMethodProcessor) getMoveProcessor();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public final String getName() {
		return RefactoringCoreMessages.MoveInstanceMethodRefactoring_name; 
	}
}
