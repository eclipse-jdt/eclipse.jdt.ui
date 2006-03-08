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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.osgi.util.NLS;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.IInitializableRefactoringComponent;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * Refactoring to extract a supertype from a class.
 * 
 * @since 3.2
 */
public final class ExtractSupertypeRefactoring extends ProcessorBasedRefactoring implements IInitializableRefactoringComponent {

	/** The refactoring processor to use */
	private final ExtractSupertypeProcessor fProcessor;

	/**
	 * Creates a new extract supertype refactoring.
	 * 
	 * @param processor
	 *            the extract supertype refactoring processor to use
	 */
	public ExtractSupertypeRefactoring(final ExtractSupertypeProcessor processor) {
		super(processor);
		fProcessor= processor;
	}

	/**
	 * Returns the extract supertype processor.
	 * 
	 * @return the extract supertype processor.
	 */
	public ExtractSupertypeProcessor getExtractSupertypeProcessor() {
		return (ExtractSupertypeProcessor) getProcessor();
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringProcessor getProcessor() {
		return fProcessor;
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
		return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.ProcessorBasedRefactoring_error_unsupported_initialization, getProcessor().getIdentifier()));
	}
}