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

import org.eclipse.ltk.core.refactoring.IInitializableRefactoringComponent;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;

/**
 * Interface for refactorings which can be composed as a composite refactoring.
 * 
 * @since 3.2
 */
public interface IComposableRefactoring extends IInitializableRefactoringComponent {

	/**
	 * Returns the global working copy owner to use.
	 * <p>
	 * If the refactoring is not part of a composite refactoring, this method
	 * always returns <code>null</code>.
	 * </p>
	 * 
	 * @return the global working copy owner, or <code>null</code>
	 */
	public WorkingCopyOwner getGlobalWorkingCopyOwner();

	/**
	 * Returns the local working copy owner to use.
	 * <p>
	 * If the refactoring is not part of a composite refactoring, this method
	 * always returns <code>null</code>.
	 * </p>
	 * 
	 * @return the local working copy owner, or <code>null</code>
	 */
	public WorkingCopyOwner getLocalWorkingCopyOwner();

	/**
	 * Returns the working copy to use instead of the original compilation unit.
	 * <p>
	 * If the refactoring is not part of a composite refactoring, this method
	 * always returns the original compilation unit.
	 * </p>
	 * 
	 * @param original
	 *            the original compilation unit
	 * @return the working copy, or the original compilation unit
	 * @throws JavaModelException
	 *             if the working copy could not be acquired
	 */
	public ICompilationUnit getWorkingCopy(ICompilationUnit original) throws JavaModelException;

	/**
	 * Sets the composite refactoring. This method is automatically called by
	 * the refactoring framework and should not be invoked by any clients.
	 * <p>
	 * This method is called exactly once for a composable refactoring.
	 * </p>
	 * 
	 * @param refactoring
	 *            the refactoring to set
	 */
	public void setCompositeRefactoring(CompositeRefactoring refactoring);
}
