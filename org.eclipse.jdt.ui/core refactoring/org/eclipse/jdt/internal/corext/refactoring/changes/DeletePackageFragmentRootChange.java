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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.undo.ResourceDescription;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

public class DeletePackageFragmentRootChange extends AbstractDeleteChange {
	
	private final String fHandle;
	private final boolean fIsExecuteChange;
	private final IPackageFragmentRootManipulationQuery fUpdateClasspathQuery;

	public DeletePackageFragmentRootChange(IPackageFragmentRoot root, boolean isExecuteChange, 
			IPackageFragmentRootManipulationQuery updateClasspathQuery) {
		Assert.isNotNull(root);
		Assert.isTrue(! root.isExternal());
		fHandle= root.getHandleIdentifier();
		fIsExecuteChange= isExecuteChange;
		fUpdateClasspathQuery= updateClasspathQuery;
	}

	public String getName() {
		String[] keys= {getRoot().getElementName()};
		return Messages.format(RefactoringCoreMessages.DeletePackageFragmentRootChange_delete, keys); 
	}

	public Object getModifiedElement() {
		return getRoot();
	}
	
	private IPackageFragmentRoot getRoot(){
		return (IPackageFragmentRoot)JavaCore.create(fHandle);
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		if (fIsExecuteChange) {
			// don't check for read-only resources since we already
			// prompt the user via a dialog to confirm deletion of
			// read only resource. The change is currently not used
			// as 
			return super.isValid(pm, DIRTY);
		} else {
			return super.isValid(pm, READ_ONLY | DIRTY);
		}
	}

	protected Change doDelete(IProgressMonitor pm) throws CoreException {
		if (! confirmDeleteIfReferenced())
			return new NullChange();
		int resourceUpdateFlags= IResource.KEEP_HISTORY;
		int jCoreUpdateFlags= IPackageFragmentRoot.ORIGINATING_PROJECT_CLASSPATH | IPackageFragmentRoot.OTHER_REFERRING_PROJECTS_CLASSPATH;
		
		pm.beginTask("", 2); //$NON-NLS-1$
		IPackageFragmentRoot root= getRoot();
		IResource rootResource= root.getResource();
		ResourceDescription resourceDescription = ResourceDescription.fromResource(rootResource);
		root.delete(resourceUpdateFlags, jCoreUpdateFlags, new SubProgressMonitor(pm, 1));
		resourceDescription.recordStateFromHistory(rootResource, new SubProgressMonitor(pm, 1));
		
		pm.done();
		switch (rootResource.getType()) {
			//TODO: undo (and redo) all changed .classpath files
			case IResource.FILE:
				return new UndoDeleteFileChange(resourceDescription);
			case IResource.FOLDER:
				return new UndoDeleteFolderChange(resourceDescription);
			default:
				return null;
		}
	}

	private boolean confirmDeleteIfReferenced() throws JavaModelException {
		if (! getRoot().isArchive()) //for source folders, you don't ask, just do it
			return true;
		if (fUpdateClasspathQuery == null)
			return true;
		IJavaProject[] referencingProjects= JavaElementUtil.getReferencingProjects(getRoot());
		if (referencingProjects.length == 0)
			return true;
		return fUpdateClasspathQuery.confirmManipulation(getRoot(), referencingProjects);
	}
}
