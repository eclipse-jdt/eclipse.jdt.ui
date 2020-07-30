/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.core.refactoring;

import org.eclipse.osgi.util.TextProcessor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.edits.UndoEdit;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.UndoTextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationMessages;
import org.eclipse.jdt.internal.core.manipulation.Messages;

/* package */ class UndoCompilationUnitChange extends UndoTextFileChange {

	private ICompilationUnit fCUnit;

	public UndoCompilationUnitChange(String name, ICompilationUnit unit, UndoEdit undo, ContentStamp stampToRestore, int saveMode) throws CoreException {
		super(name, getFile(unit), undo, stampToRestore, saveMode);
		fCUnit= unit;
	}

	private static IFile getFile(ICompilationUnit cunit) throws CoreException {
		IFile file= (IFile)cunit.getResource();
		if (file == null) {
			String message= Messages.format(JavaManipulationMessages.UndoCompilationUnitChange_no_file, TextProcessor.process(cunit.getElementName()));
			throw new CoreException(new Status(IStatus.ERROR, JavaManipulation.ID_PLUGIN, message));
		}
		return file;
	}

	@Override
	public Object getModifiedElement() {
		return fCUnit;
	}

	@Override
	protected Change createUndoChange(UndoEdit edit, ContentStamp stampToRestore) throws CoreException {
		return new UndoCompilationUnitChange(getName(), fCUnit, edit, stampToRestore, getSaveMode());
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		SubMonitor subMonitor= SubMonitor.convert(pm, 2);
		fCUnit.becomeWorkingCopy(subMonitor.split(1));
		try {
			return super.perform(subMonitor.split(1));
		} finally {
			fCUnit.discardWorkingCopy();
		}
	}
}
