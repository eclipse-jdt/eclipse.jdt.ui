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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

import org.eclipse.jdt.core.ICompilationUnit;

/**
 * Compilation unit change with a refactoring descriptor.
 * 
 * @since 3.2
 */
public final class CompilationUnitDescriptorChange extends CompilationUnitChange {

	/** The refactoring descriptor */
	private final RefactoringDescriptor fDescriptor;

	/**
	 * Creates a new compilation unit descriptor change.
	 * 
	 * @param descriptor
	 *            the refactoring descriptor
	 * @param name
	 *            the name
	 * @param unit
	 *            the compilation unit
	 */
	public CompilationUnitDescriptorChange(final RefactoringDescriptor descriptor, final String name, final ICompilationUnit unit) {
		super(name, unit);
		Assert.isNotNull(descriptor);
		fDescriptor= descriptor;
	}

	/**
	 * {@inheritDoc}
	 */
	public ChangeDescriptor getDescriptor() {
		return new RefactoringChangeDescriptor(fDescriptor);
	}
}
