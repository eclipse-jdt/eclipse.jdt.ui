/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IScriptableRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

/**
 * Refactoring to extract an interface from a type.
 */
public final class ExtractInterfaceRefactoring extends ProcessorBasedRefactoring implements IScriptableRefactoring {

	/** The processor to use */
	private final SuperTypeRefactoringProcessor fProcessor;

	/**
	 * Creates a new extract interface refactoring.
	 * 
	 * @param processor
	 *            the processor to use
	 */
	public ExtractInterfaceRefactoring(final SuperTypeRefactoringProcessor processor) {
		super(processor);

		fProcessor= processor;
	}

	/**
	 * Returns the extract interface processor.
	 * 
	 * @return the refactoring processor
	 */
	public final ExtractInterfaceProcessor getExtractInterfaceProcessor() {
		return (ExtractInterfaceProcessor) getProcessor();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring#getProcessor()
	 */
	public final RefactoringProcessor getProcessor() {
		return fProcessor;
	}

	/**
	 * {@inheritDoc}
	 */
	public final RefactoringStatus initialize(final RefactoringArguments arguments) {
		Assert.isNotNull(arguments);
		final RefactoringProcessor processor= getProcessor();
		if (processor instanceof IScriptableRefactoring) {
			return ((IScriptableRefactoring) processor).initialize(arguments);
		}
		return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.ProcessorBasedRefactoring_error_unsupported_initialization, IJavaRefactorings.EXTRACT_INTERFACE));
	}
}
