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

/**
 * Refactoring to replace type occurrences by a super type where possible.
 * 
 * @since 3.1
 */
public final class UseSuperTypeRefactoring extends ProcessorBasedRefactoring {

	/**
	 * Creates a new use super type refactoring.
	 * 
	 * @param subType the type to replace its occurrences
	 * @return the created refactoring
	 * @throws JavaModelException if the the refactoring could not be tested for availability
	 */
	public static UseSuperTypeRefactoring create(final IType subType) throws JavaModelException {
		Assert.isNotNull(subType);
		Assert.isTrue(subType.exists() && !subType.isAnonymous() && !subType.isAnnotation());
		return new UseSuperTypeRefactoring(new UseSuperTypeProcessor(subType));
	}

	/**
	 * Creates a new use super type refactoring.
	 * 
	 * @param subType the type to replace its occurrences
	 * @param superType the type as replacement
	 * @return the created refactoring
	 * @throws JavaModelException if the the refactoring could not be tested for availability
	 */
	public static UseSuperTypeRefactoring create(final IType subType, final IType superType) throws JavaModelException {
		Assert.isNotNull(subType);
		Assert.isNotNull(superType);
		Assert.isTrue(subType.exists() && !subType.isAnonymous() && !subType.isAnnotation());
		Assert.isTrue(superType.exists() && !superType.isAnonymous() && !superType.isAnnotation() && !superType.isEnum());
		return new UseSuperTypeRefactoring(new UseSuperTypeProcessor(subType, superType));
	}

	/** The processor to use */
	private final UseSuperTypeProcessor fProcessor;

	/**
	 * @param processor
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
}
