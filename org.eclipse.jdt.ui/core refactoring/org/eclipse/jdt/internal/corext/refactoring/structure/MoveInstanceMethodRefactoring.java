/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

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
	 * Is this refactoring available for the specified method?
	 * 
	 * @param method
	 *        the method to test
	 * @return <code>true</code> if this refactoring is available, <code>false</code> otherwise
	 * @throws JavaModelException
	 *         if the method could not be tested
	 */
	public static boolean isAvailable(final IMethod method) throws JavaModelException {
		Assert.isNotNull(method);
		return method.exists() && !method.isConstructor() && !method.isBinary() && !method.getDeclaringType().isLocal() && !method.getDeclaringType().isAnnotation() && !method.isReadOnly() && !JdtFlags.isStatic(method);
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
		return RefactoringCoreMessages.getString("MoveInstanceMethodRefactoring.name"); //$NON-NLS-1$
	}
}
