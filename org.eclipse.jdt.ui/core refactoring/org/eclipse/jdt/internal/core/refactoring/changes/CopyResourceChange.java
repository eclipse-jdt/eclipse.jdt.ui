package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

class CopyResourceChange extends ResourceReorgChange {
	
	CopyResourceChange(IResource res, IContainer dest, String newName){
		super(res, dest, newName);
	}
	
	/**
	 * @see ResourceReorgChange#doPerform(IPath, IProgressMonitor)
	 */
	protected void doPerform(IPath path, IProgressMonitor pm) throws JavaModelException{
		try{
			getResource().copy(path, true, pm);
		}catch(CoreException e){
			throw new JavaModelException(e);
		}	
	}
	
	/**
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		if (!isActive())
			return new NullChange();
		
		IResource copied= getNewResource();
		if (copied instanceof IFile)
			return new DeleteFileChange((IFile)copied);
		
		if (copied instanceof IFolder)
			return new DeleteFolderChange((IFolder)copied);		
		
		Assert.isTrue(false, "not expected to get here");	
		return null;
	}
	
	public boolean isUndoable(){
		return false;
	}

	/**
	 * @see IChange#getName()
	 */
	public String getName() {
		return "Copy resource";
	}
}

