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
package org.eclipse.jdt.core.refactoring.descriptors;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

/**
 * Refactoring descriptor for the use supertype refactoring.
 * <p>
 * An instance of this refactoring descriptor may be obtained by calling
 * {@link RefactoringContribution#createDescriptor()} on a refactoring
 * contribution requested by invoking
 * {@link RefactoringCore#getRefactoringContribution(String)} with the
 * appropriate refactoring id.
 * </p>
 * <p>
 * Clients must first set the basic refactoring descriptor attributes such as
 * the project name, the description, the comment and the flags before setting
 * any other attributes.
 * </p>
 * <p>
 * Note: this class is not intended to be instantiated by clients.
 * </p>
 * 
 * @since 3.3
 */
public final class UseSupertypeDescriptor extends JavaRefactoringDescriptor {

	/** The instanceof attribute */
	private static final String ATTRIBUTE_INSTANCEOF= "instanceof"; //$NON-NLS-1$

	/**
	 * Creates a new refactoring descriptor.
	 */
	public UseSupertypeDescriptor() {
		super(IJavaRefactorings.USE_SUPER_TYPE);
	}

	/**
	 * Determines whether 'instanceof' statements are considered as candidates
	 * to replace the subtype occurrence by one of its supertypes.
	 * 
	 * @param replace
	 *            <code>true</code> to replace subtype occurrences in
	 *            'instanceof' statements, <code>false</code> otherwise
	 */
	public void setReplaceInstanceof(final boolean replace) {
		fArguments.put(ATTRIBUTE_INSTANCEOF, Boolean.valueOf(replace).toString());
	}

	/**
	 * Sets the subtype of the refactoring.
	 * <p>
	 * Occurrences of the subtype are replaced by the supertype set by
	 * {@link #setSupertype(IType)} where possible.
	 * </p>
	 * 
	 * @param type
	 *            the subtype to set
	 */
	public void setSubtype(final IType type) {
		Assert.isNotNull(type);
		fArguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, elementToHandle(getProject(), type));
	}

	/**
	 * Sets the supertype of the refactoring.
	 * <p>
	 * Occurrences of the subtype set by {@link #setSubtype(IType)} are replaced
	 * by the supertype where possible.
	 * </p>
	 * 
	 * @param type
	 *            the supertype to set
	 */
	public void setSupertype(final IType type) {
		Assert.isNotNull(type);
		fArguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1, elementToHandle(getProject(), type));
	}
}