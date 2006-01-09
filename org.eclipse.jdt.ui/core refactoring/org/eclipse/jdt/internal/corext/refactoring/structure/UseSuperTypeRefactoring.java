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

import org.eclipse.ltk.core.refactoring.IInitializableRefactoringComponent;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.osgi.util.NLS;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * Refactoring to replace type occurrences by a super type where possible.
 * 
 * @since 3.1
 */
public final class UseSuperTypeRefactoring extends ProcessorBasedRefactoring implements IInitializableRefactoringComponent {

	/** The processor to use */
	private final UseSuperTypeProcessor fProcessor;

	/**
	 * Creates a new use super type refactoring.
	 * 
	 * @param processor
	 *            the processor to use
	 */
	public UseSuperTypeRefactoring(final UseSuperTypeProcessor processor) {
		super(processor);

		fProcessor= processor;
	}
	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring#getProcessor()
	 */
	public final RefactoringProcessor getProcessor() {
		return fProcessor;
	}

	/**
	 * Returns the use super type processor.
	 * 
	 * @return the refactoring processor
	 */
	public final UseSuperTypeProcessor getUseSuperTypeProcessor() {
		return (UseSuperTypeProcessor) getProcessor();
	}

	/**
	 * {@inheritDoc}
	 */
	public final RefactoringStatus initialize(final RefactoringArguments arguments) {
		Assert.isNotNull(arguments);
		final RefactoringProcessor processor= getProcessor();
		if (processor instanceof IInitializableRefactoringComponent) {
			return ((IInitializableRefactoringComponent) processor).initialize(arguments);
		}
		return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.ProcessorBasedRefactoring_error_unsupported_initialization, UseSuperTypeProcessor.ID_USE_SUPERTYPE));
	}
}
