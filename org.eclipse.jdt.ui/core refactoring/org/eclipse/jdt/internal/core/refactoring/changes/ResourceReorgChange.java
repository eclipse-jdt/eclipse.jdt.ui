package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;

abstract class ResourceReorgChange extends Change {
	
	private IPath fResourcePath;
	private boolean fIsFile;
	
	private IPath fDestinationPath;
	private boolean fIsDestinationProject;
	private String fNewName;
	
	private IResource fNewResource;
	
	ResourceReorgChange(IResource res, IContainer dest, String newName){
		Assert.isTrue(res instanceof IFile || res instanceof IFolder);
		fIsFile= (res instanceof IFile);
		fResourcePath= ReorgUtils.getResourcePath(res);
	
		Assert.isTrue(dest instanceof IProject || dest instanceof IFolder);
		fIsDestinationProject= (dest instanceof IProject);
		fDestinationPath= ReorgUtils.getResourcePath(dest);
		fNewName= newName;
	}
	
	protected abstract void doPerform(IPath path, IProgressMonitor pm) throws JavaModelException;
	
	/**
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public final void perform(ChangeContext context, IProgressMonitor pm)	throws JavaModelException, ChangeAbortException {
		try{
			pm.beginTask(getName(), 1);
			if (!isActive())
				return;
			IResource destResource= getDestination();
			IResource res= getResource();
			String oldName= res.getName();
			IPath path= destResource.getFullPath();
			if (fNewName == null)
				path= path.append(oldName);
			else 
				path= path.append(fNewName);
			doPerform(path, pm);	
			fNewResource= destResource.getWorkspace().getRoot().getFile(path);
		} catch (CoreException e){
			throw new JavaModelException(e);			
		} finally {
			pm.done();
		}
	}
		/**
	 * @see IChange#getCorrespondingJavaElement()
	 */
	public IJavaElement getCorrespondingJavaElement() {
		return JavaCore.create(getResource());
	}

	private IFile getFile(){
		return ReorgUtils.getFile(fResourcePath);
	}
	
	private IFolder getFolder(){
		return ReorgUtils.getFolder(fResourcePath);
	}
	
	protected IResource getResource(){
		if (fIsFile)
			return getFile();
		else
			return getFolder();
	}
	
	private IContainer getDestination(){
		if (fIsDestinationProject)
			return ReorgUtils.getProject(fDestinationPath);
		else
			return ReorgUtils.getFolder(fDestinationPath);	
	}
	
	protected IResource getNewResource(){
		return fNewResource;
	}
}

