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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.ide.undo.ResourceDescription;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

public class DeleteFolderChange extends AbstractDeleteChange {
	
	private final IPath fPath;
	
	public DeleteFolderChange(IFolder folder, boolean isExecuteChange) {
		this(getFolderPath(folder), isExecuteChange);
	}
	
	public DeleteFolderChange(IPath path, boolean isExecuteChange) {
		fPath= path;
		
		// no need to do additional checking since the dialog
		// already prompts the user if there are dirty
		// or read only files in the folder. The change is
		// currently not used as a undo/redo change
		if (isExecuteChange) {
			setValidationMethod(VALIDATE_DEFAULT);
		} else {
			setValidationMethod(VALIDATE_NOT_READ_ONLY | VALIDATE_NOT_DIRTY);
		}
	}
	
	public static IPath getFolderPath(IFolder folder){
		return folder.getFullPath().removeFirstSegments(ResourcesPlugin.getWorkspace().getRoot().getFullPath().segmentCount());
	}
	
	public static IFolder getFolder(IPath path){
		return ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.DeleteFolderChange_0, fPath.lastSegment()); 
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.JDTChange#getModifiedResource()
	 */
	protected IResource getModifiedResource() {
		return getFolder(fPath);
	}

	protected Change doDelete(IProgressMonitor pm) throws CoreException{
		IFolder folder= getFolder(fPath);
		Assert.isTrue(folder.exists());
		pm.beginTask("", 3); //$NON-NLS-1$
		folder.accept(new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if (resource instanceof IFile) {
					// progress is covered outside.
					saveFileIfNeeded((IFile)resource, new NullProgressMonitor());
				}
				return true;
			}
		}, IResource.DEPTH_INFINITE, false);
		pm.worked(1);
		
		ResourceDescription resourceDescription = ResourceDescription.fromResource(folder);
		folder.delete(false, true, new SubProgressMonitor(pm, 1));
		resourceDescription.recordStateFromHistory(folder, new SubProgressMonitor(pm, 1));
		pm.done();
		
		return new UndoDeleteResourceChange(resourceDescription);
	}
}

