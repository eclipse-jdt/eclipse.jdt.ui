/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.text.edits.UndoEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.ltk.core.refactoring.Change;

/* package */ class UndoCompilationUnitChange extends UndoTextFileChange {
	
	private ICompilationUnit fCUnit;

	public UndoCompilationUnitChange(String name, ICompilationUnit unit, UndoEdit undo, int saveMode) throws CoreException {
		super(name, getFile(unit), undo, saveMode);
		fCUnit= unit;
	}

	private static IFile getFile(ICompilationUnit cunit) throws CoreException {
		return (IFile) cunit.getResource();
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected Change createUndoChange(UndoEdit edit) throws CoreException {
		return new UndoCompilationUnitChange(getName(), fCUnit, edit, getSaveMode());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Change perform(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		fCUnit.becomeWorkingCopy(null, new SubProgressMonitor(pm,1));
		try {
			return super.perform(new SubProgressMonitor(pm,1));
		} finally {
			fCUnit.discardWorkingCopy();
		}
	}
}
