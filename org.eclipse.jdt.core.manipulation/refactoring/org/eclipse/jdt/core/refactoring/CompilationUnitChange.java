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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 477789
 *******************************************************************************/
package org.eclipse.jdt.core.refactoring;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.edits.UndoEdit;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.ContentStamp;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;

/**
 * A {@link TextFileChange} that operates on an {@link ICompilationUnit} in the workspace.
 *
 * @since 1.3
 */
public class CompilationUnitChange extends TextFileChange {

	private final ICompilationUnit fCUnit;

	/** The (optional) refactoring descriptor */
	private ChangeDescriptor fDescriptor;

	/**
	 * Creates a new <code>CompilationUnitChange</code>.
	 *
	 * @param name the change's name, mainly used to render the change in the UI
	 * @param cunit the compilation unit this change works on
	 */
	public CompilationUnitChange(String name, ICompilationUnit cunit) {
		super(name, getFile(cunit));
		Assert.isNotNull(cunit);
		fCUnit= cunit;
		setTextType("java"); //$NON-NLS-1$
	}

	private static IFile getFile(ICompilationUnit cunit) {
		return (IFile) cunit.getResource();
	}

	@Override
	public Object getModifiedElement(){
		return fCUnit;
	}

	/**
	 * Returns the compilation unit this change works on.
	 *
	 * @return the compilation unit this change works on
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCUnit;
	}

	@Override
	protected IDocument acquireDocument(IProgressMonitor pm) throws CoreException {
		SubMonitor subMonitor= SubMonitor.convert(pm, 2);
		fCUnit.becomeWorkingCopy(subMonitor.split(1));
		Assert.isTrue(fCUnit.isWorkingCopy(), !fCUnit.isWorkingCopy()?fCUnit.toString():""); //$NON-NLS-1$
		return super.acquireDocument(subMonitor.split(1));
	}

	@Override
	protected void releaseDocument(IDocument document, IProgressMonitor pm) throws CoreException {
		boolean isModified= isDocumentModified();
		SubMonitor subMonitor= SubMonitor.convert(pm, 2);
		super.releaseDocument(document, subMonitor.split(1));
		try {
			fCUnit.discardWorkingCopy();
		} finally {
			if (isModified && !isDocumentAcquired()) {
				if (fCUnit.isWorkingCopy())
					fCUnit.reconcile(
							ICompilationUnit.NO_AST,
							false /* don't force problem detection */,
							null /* use primary owner */,
							subMonitor.split(1, SubMonitor.SUPPRESS_BEGINTASK | SubMonitor.SUPPRESS_ISCANCELED));

				else
					fCUnit.makeConsistent(subMonitor.split(1, SubMonitor.SUPPRESS_BEGINTASK | SubMonitor.SUPPRESS_ISCANCELED));
			}
		}
	}

	@Override
	protected Change createUndoChange(UndoEdit edit, ContentStamp stampToRestore) {
		try {
			return new UndoCompilationUnitChange(getName(), fCUnit, edit, stampToRestore, getSaveMode());
		} catch (CoreException e) {
			JavaManipulationPlugin.log(e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (ICompilationUnit.class.equals(adapter))
			return (T) fCUnit;
		return super.getAdapter(adapter);
	}

	/**
	 * Sets the refactoring descriptor for this change.
	 *
	 * @param descriptor the descriptor to set, or <code>null</code> to set no descriptor
	 */
	public void setDescriptor(ChangeDescriptor descriptor) {
		fDescriptor= descriptor;
	}

	@Override
	public ChangeDescriptor getDescriptor() {
		return fDescriptor;
	}
}

