package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

public class DeleteFolderChange extends DeleteChange {
	
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

	/**
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Delete folder " + fPath.lastSegment();
	}
	
	/**
	 * @see IChange#getCorrespondingJavaElement()
	 */
	public IJavaElement getCorrespondingJavaElement() {
		return JavaCore.create(getFolder(fPath));
	}

	/**
	 * @see DeleteChange#doDelete(IProgressMonitor)
	 */
	protected void doDelete(IProgressMonitor pm) throws CoreException{
		IFolder folder= getFolder(fPath);
		Assert.isNotNull(folder);
		Assert.isTrue(folder.exists());
		folder.delete(true, false, new SubProgressMonitor(pm, 1));
	}
}

