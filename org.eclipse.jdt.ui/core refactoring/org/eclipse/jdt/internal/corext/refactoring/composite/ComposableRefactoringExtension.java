/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.composite;

import org.eclipse.ltk.core.refactoring.Refactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Default implementation of
 * {@link org.eclipse.jdt.internal.corext.refactoring.composite.IComposableRefactoring}.
 * <p>
 * This class can be used for as implementation in delegate methods of
 * refactorings which implement
 * {@link org.eclipse.jdt.internal.corext.refactoring.composite.IComposableRefactoring}.
 * </p>
 * 
 * @since 3.2
 */
public final class ComposableRefactoringExtension {

	/** The composable refactoring */
	private final Refactoring fComposableRefactoring;

	/** The composite refactoring */
	private CompositeRefactoring fCompositeRefactoring;

	/**
	 * Creates a new composable refactoring extension.
	 * 
	 * @param refactoring
	 *            the refactoring to extend
	 */
	public ComposableRefactoringExtension(final Refactoring refactoring) {
		Assert.isNotNull(refactoring);

		fComposableRefactoring= refactoring;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.composite.IComposableRefactoring#getGlobalWorkingCopyOwner()
	 */
	public final WorkingCopyOwner getGlobalWorkingCopyOwner() {

		if (fCompositeRefactoring != null)
			return fCompositeRefactoring.getGlobalWorkingCopyOwner();

		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.composite.IComposableRefactoring#getLocalWorkingCopyOwner()
	 */
	public final WorkingCopyOwner getLocalWorkingCopyOwner() {

		if (fCompositeRefactoring != null)
			return fCompositeRefactoring.getLocalWorkingCopyOwner(fComposableRefactoring);

		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.composite.IComposableRefactoring#getWorkingCopy(org.eclipse.jdt.core.ICompilationUnit)
	 */
	public final ICompilationUnit getWorkingCopy(final ICompilationUnit original) throws JavaModelException {

		if (fCompositeRefactoring != null)
			return fCompositeRefactoring.getWorkingCopy(original);

		return original;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.composite.IComposableRefactoring#setCompositeRefactoring(org.eclipse.jdt.internal.corext.refactoring.composite.CompositeRefactoring)
	 */
	public final void setCompositeRefactoring(final CompositeRefactoring refactoring) {
		Assert.isTrue(fCompositeRefactoring == null, "The refactoring can only be set once"); //$NON-NLS-1$
		Assert.isNotNull(refactoring);

		fCompositeRefactoring= refactoring;
	}
}
