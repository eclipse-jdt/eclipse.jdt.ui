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

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class DeleteFolderChange extends AbstractDeleteChange {
	
	private IPath fPath;
	
	public DeleteFolderChange(IFolder folder){
		this(getFolderPath(folder));
	}
	
	public DeleteFolderChange(IPath path){
		fPath= path;
	}
	
	public static IPath getFolderPath(IFolder folder){
		return folder.getFullPath().removeFirstSegments(ResourcesPlugin.getWorkspace().getRoot().getFullPath().segmentCount());
	}
	
	public static IFolder getFolder(IPath path){
		return ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
	}

	public String getName() {
		return RefactoringCoreMessages.getFormattedString("DeleteFolderChange.0", fPath.lastSegment()); //$NON-NLS-1$
	}
	
	public Object getModifiedElement() {
		return getFolder(fPath);
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return super.isValid(pm, false, false);
	}

	protected void doDelete(IProgressMonitor pm) throws CoreException{
		IFolder folder= getFolder(fPath);
		Assert.isTrue(folder.exists());
		pm.beginTask("", 2); //$NON-NLS-1$
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
		folder.delete(false, true, new SubProgressMonitor(pm, 1));
		pm.done();
	}
}

