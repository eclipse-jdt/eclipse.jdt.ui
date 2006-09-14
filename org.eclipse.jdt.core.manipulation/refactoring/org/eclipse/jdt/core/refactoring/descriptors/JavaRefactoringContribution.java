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

import java.util.Map;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

/**
 * Partial implementation of a Java refactoring contribution.
 * <p>
 * Note: this class is not intended to be extended outside the refactoring
 * framework.
 * </p>
 * 
 * @since 3.3
 */
public abstract class JavaRefactoringContribution extends RefactoringContribution {

	/**
	 * {@inheritDoc}
	 */
	public final Map retrieveArgumentMap(final RefactoringDescriptor descriptor) {
		Assert.isNotNull(descriptor);
		if (descriptor instanceof JavaRefactoringDescriptor)
			return ((JavaRefactoringDescriptor) descriptor).getArguments();
		return super.retrieveArgumentMap(descriptor);
	}
}