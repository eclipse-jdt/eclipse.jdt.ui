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

import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;

/**
 * Refactoring to extract an interface from a type.
 */
public final class ExtractInterfaceRefactoring extends ProcessorBasedRefactoring {

	/**
	 * Creates a new extract interface refactoring.
	 * 
	 * @param type the type to extract an interface from
	 * @param settings the code generation settings to apply
	 * @return the created refactoring
	 * @throws JavaModelException if the the refactoring could not be tested for availability
	 */
	public static ExtractInterfaceRefactoring create(final IType type, final CodeGenerationSettings settings) throws JavaModelException {
		Assert.isNotNull(type);
		Assert.isNotNull(settings);
		Assert.isTrue(type.exists() && !type.isAnnotation() && !type.isAnonymous() && !type.isBinary() && !type.isReadOnly());
		return new ExtractInterfaceRefactoring(new ExtractInterfaceProcessor(type, settings));
	}

	/** The processor to use */
	private final ExtractInterfaceProcessor fProcessor;

	/**
	 * Creates a new extract interface refactoring.
	 * 
	 * @param processor the processor to use
	 */
	private ExtractInterfaceRefactoring(final ExtractInterfaceProcessor processor) {
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
}
