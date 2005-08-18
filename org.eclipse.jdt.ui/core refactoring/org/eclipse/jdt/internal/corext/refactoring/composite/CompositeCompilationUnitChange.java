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

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.CompositeTextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;

/**
 * Composite compilation unit change for composite refactorings.
 * 
 * @since 3.2
 */
public final class CompositeCompilationUnitChange extends CompositeTextFileChange {

	/** The compilation unit */
	private final ICompilationUnit fUnit;

	/**
	 * Creates a new composite compilation unit change.
	 * 
	 * @param name
	 *            the name of the change
	 * @param unit
	 *            the compilation unit
	 */
	public CompositeCompilationUnitChange(final String name, final ICompilationUnit unit) {
		super(name, (IFile) unit.getResource());

		fUnit= unit;

		setTextType("java"); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Change#getAdapter(java.lang.Class)
	 */
	public final Object getAdapter(final Class adapter) {

		if (ICompilationUnit.class.equals(adapter))
			return fUnit;

		return super.getAdapter(adapter);
	}

	/**
	 * Returns the compilation unit.
	 * 
	 * @return the compilation unit
	 */
	public final ICompilationUnit getCompilationUnit() {
		return fUnit;
	}
}
