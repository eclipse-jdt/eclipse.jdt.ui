/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

public final class RenameCompilationUnitChange extends AbstractJavaElementRenameChange {

	public RenameCompilationUnitChange(RefactoringDescriptor descriptor, ICompilationUnit unit, String newName, String comment) {
		this(descriptor, unit.getResource().getFullPath(), unit.getElementName(), newName, comment, IResource.NULL_STAMP);
		Assert.isTrue(!unit.isReadOnly(), "compilation unit must not be read-only"); //$NON-NLS-1$
	}

	private RenameCompilationUnitChange(RefactoringDescriptor descriptor, IPath resourcePath, String oldName, String newName, String comment, long stampToRestore) {
		super(descriptor, resourcePath, oldName, newName, comment, stampToRestore);
	}

	protected IPath createNewPath() {
		final IPath path= getResourcePath();
		if (path.getFileExtension() != null)
			return path.removeFileExtension().removeLastSegments(1).append(getNewName());
		else
			return path.removeLastSegments(1).append(getNewName());
	}

	protected Change createUndoChange(long stampToRestore) throws JavaModelException {
		return new RenameCompilationUnitChange(null, createNewPath(), getNewName(), getOldName(), getComment(), stampToRestore);
	}

	protected void doRename(IProgressMonitor pm) throws CoreException {
		ICompilationUnit cu= (ICompilationUnit) getModifiedElement();
		if (cu != null)
			cu.rename(getNewName(), false, pm);
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.RenameCompilationUnitChange_name, new String[] { getOldName(), getNewName()});
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return super.isValid(pm, READ_ONLY | SAVE_IF_DIRTY);
	}
}
