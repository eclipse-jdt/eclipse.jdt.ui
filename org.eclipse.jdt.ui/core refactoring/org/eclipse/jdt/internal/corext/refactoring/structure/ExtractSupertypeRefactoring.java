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

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * Refactoring to extract a supertype from a class.
 * 
 * @since 3.2
 */
public final class ExtractSupertypeRefactoring extends PullUpRefactoring {

	/**
	 * Creates a new extract supertype refactoring.
	 * 
	 * @param processor
	 *            the extract supertype refactoring processor to use
	 */
	public ExtractSupertypeRefactoring(final ExtractSupertypeProcessor processor) {
		super(processor);
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
	public String getName() {
		return RefactoringCoreMessages.ExtractSupertypeProcessor_extract_supertype;
	}
}