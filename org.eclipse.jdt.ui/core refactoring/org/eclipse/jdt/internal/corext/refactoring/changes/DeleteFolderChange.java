/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;

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

	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("DeleteFolderChange.Delete_folder") + fPath.lastSegment(); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return getFolder(fPath);
	}

	/* non java-doc
	 * @see DeleteChange#doDelete(IProgressMonitor)
	 */
	protected void doDelete(ChangeContext context, IProgressMonitor pm) throws CoreException{
		IFolder folder= getFolder(fPath);
		Assert.isTrue(folder.exists());
		folder.delete(false, true, new SubProgressMonitor(pm, 1));
	}
}

