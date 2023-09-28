/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.MultiStateTextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;

/**
 * Multi state compilation unit change for composite refactorings.
 *
 * @since 3.2
 */
public final class MultiStateCompilationUnitChange extends MultiStateTextFileChange {

	/** The compilation unit */
	private final ICompilationUnit fUnit;

	/**
	 * Creates a new multi state compilation unit change.
	 *
	 * @param name
	 *            the name of the change
	 * @param unit
	 *            the compilation unit
	 */
	public MultiStateCompilationUnitChange(final String name, final ICompilationUnit unit) {
		super(name, (IFile) unit.getResource());

		fUnit= unit;

		setTextType("java"); //$NON-NLS-1$
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (ICompilationUnit.class.equals(adapter))
			return (T) fUnit;

		return super.getAdapter(adapter);
	}

	/**
	 * Returns the compilation unit.
	 *
	 * @return the compilation unit
	 */
	public ICompilationUnit getCompilationUnit() {
		return fUnit;
	}

	@Override
	public String getName() {
		return Messages.format(RefactoringCoreMessages.MultiStateCompilationUnitChange_name_pattern, new String[] { BasicElementLabels.getFileName(fUnit), BasicElementLabels.getPathLabel(fUnit.getParent().getPath(), false) });
	}

}
