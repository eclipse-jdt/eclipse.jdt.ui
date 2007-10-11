/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

public final class RenameSourceFolderChange extends AbstractJavaElementRenameChange {

	private static RefactoringStatus checkIfModifiable(IPackageFragmentRoot root) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		if (root == null) {
			result.addFatalError(RefactoringCoreMessages.DynamicValidationStateChange_workspace_changed);
			return result;
		}
		if (!root.exists()) {
			result.addFatalError(Messages.format(RefactoringCoreMessages.Change_does_not_exist, root.getElementName()));
			return result;
		}
		
		
		if (result.hasFatalError())
			return result;

		if (root.isArchive()) {
			result.addFatalError(Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_rename_archive, root.getElementName()));
			return result;
		}

		if (root.isExternal()) {
			result.addFatalError(Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_rename_external, root.getElementName()));
			return result;
		}

		IResource correspondingResource= root.getCorrespondingResource();
		if (correspondingResource == null || correspondingResource.exists()) {
			result.addFatalError(Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_error_underlying_resource_not_existing, root.getElementName()));
			return result;
		}

		if (correspondingResource.isLinked()) {
			result.addFatalError(Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_rename_linked, root.getElementName()));
			return result;
		}

		return result;
	}

	public RenameSourceFolderChange(IPackageFragmentRoot sourceFolder, String newName) {
		this(sourceFolder.getPath(), sourceFolder.getElementName(), newName, IResource.NULL_STAMP);
		Assert.isTrue(!sourceFolder.isReadOnly(), "should not be read only"); //$NON-NLS-1$
		Assert.isTrue(!sourceFolder.isArchive(), "should not be an archive"); //$NON-NLS-1$
		setValidationMethod(VALIDATE_NOT_DIRTY);
	}

	private RenameSourceFolderChange(IPath resourcePath, String oldName, String newName, long stampToRestore) {
		super(resourcePath, oldName, newName, stampToRestore);
	}

	protected IPath createNewPath() {
		return getResourcePath().removeLastSegments(1).append(getNewName());
	}

	protected Change createUndoChange(long stampToRestore) {
		return new RenameSourceFolderChange(createNewPath(), getNewName(), getOldName(), stampToRestore);
	}

	protected void doRename(IProgressMonitor pm) throws CoreException {
		IPackageFragmentRoot sourceFolder= getSourceFolder();
		if (sourceFolder != null)
			sourceFolder.move(getNewPath(), getCoreMoveFlags(), getJavaModelUpdateFlags(), null, pm);
	}

	private int getCoreMoveFlags() {
		if (getResource().isLinked())
			return IResource.SHALLOW;
		else
			return IResource.NONE;
	}

	private int getJavaModelUpdateFlags() {
		return IPackageFragmentRoot.DESTINATION_PROJECT_CLASSPATH | IPackageFragmentRoot.ORIGINATING_PROJECT_CLASSPATH | IPackageFragmentRoot.OTHER_REFERRING_PROJECTS_CLASSPATH | IPackageFragmentRoot.REPLACE;
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_rename, new String[] { getOldName(), getNewName()});
	}

	private IPath getNewPath() {
		return getResource().getFullPath().removeLastSegments(1).append(getNewName());
	}

	private IPackageFragmentRoot getSourceFolder() {
		return (IPackageFragmentRoot) getModifiedElement();
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= super.isValid(pm);
		if (result.hasFatalError())
			return result;
		
		IPackageFragmentRoot sourceFolder= getSourceFolder();
		result.merge(checkIfModifiable(sourceFolder));

		return result;
	}
}
