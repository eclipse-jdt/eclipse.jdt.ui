/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;

abstract class ResourceReorgChange extends Change {
	
	private IPath fResourcePath;
	private boolean fIsFile;
	private IPath fDestinationPath;
	private boolean fIsDestinationProject;
	private INewNameQuery fNewNameQuery;
	
	ResourceReorgChange(IResource res, IContainer dest, INewNameQuery nameQuery){
		Assert.isTrue(res instanceof IFile || res instanceof IFolder);
		fIsFile= (res instanceof IFile);
		fResourcePath= Utils.getResourcePath(res);
	
		Assert.isTrue(dest instanceof IProject || dest instanceof IFolder);
		fIsDestinationProject= (dest instanceof IProject);
		fDestinationPath= Utils.getResourcePath(dest);
		fNewNameQuery= nameQuery;
	}
	
	protected abstract void doPerform(IPath path, IProgressMonitor pm) throws JavaModelException;
	
	/* non java-doc
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public final void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		try{
			pm.beginTask(getName(), 2);
			if (!isActive())
				return;
			
			String newName= getNewResourceName();
			deleteIfAlreadyExists(new SubProgressMonitor(pm, 1), newName);
				
			doPerform(getDestination().getFullPath().append(newName), new SubProgressMonitor(pm, 1));	
		} catch (CoreException e){
			throw new JavaModelException(e);			
		} finally {
			pm.done();
		}
	}

	private void deleteIfAlreadyExists(IProgressMonitor pm, String newName) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		IResource current= getDestination().findMember(newName);
		if (current == null)
			return;
		if (! current.exists())
			return;
		if (current instanceof IFile)
			((IFile)current).delete(false, true, new SubProgressMonitor(pm, 1));
		else if (current instanceof IFolder)
			((IFolder)current).delete(false, true, new SubProgressMonitor(pm, 1));
		else 
			Assert.isTrue(false, RefactoringCoreMessages.getString("ResourceReorgChange.assert"));	 //$NON-NLS-1$
	}
	
	private String getNewResourceName(){
		if (fNewNameQuery == null)
			return getResource().getName();
		String name= fNewNameQuery.getNewName();
		if (name == null)
			return getResource().getName();
		return name;
	}
	
	/* non java-doc
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return getResource();
	}

	private IFile getFile(){
		return Utils.getFile(fResourcePath);
	}
	
	private IFolder getFolder(){
		return Utils.getFolder(fResourcePath);
	}
	
	protected IResource getResource(){
		if (fIsFile)
			return getFile();
		else
			return getFolder();
	}
	
	IContainer getDestination(){
		if (fIsDestinationProject)
			return Utils.getProject(fDestinationPath);
		else
			return Utils.getFolder(fDestinationPath);	
	}
}

