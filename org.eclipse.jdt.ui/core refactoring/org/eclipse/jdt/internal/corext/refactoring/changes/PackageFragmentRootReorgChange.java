/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

abstract class PackageFragmentRootReorgChange extends Change {

	private final String fRootHandle;
	private final IPath fDestinationPath;
	private final INewNameQuery fNewNameQuery;
	private final IPackageFragmentRootManipulationQuery fUpdateClasspathQuery;
	
	PackageFragmentRootReorgChange(IPackageFragmentRoot root, IProject destination, INewNameQuery newNameQuery, IPackageFragmentRootManipulationQuery updateClasspathQuery){
		Assert.isTrue(! root.isExternal());
		fRootHandle= root.getHandleIdentifier();
		fDestinationPath= Utils.getResourcePath(destination);
		fNewNameQuery= newNameQuery;
		fUpdateClasspathQuery= updateClasspathQuery;
	}

	/* non java-doc
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public final void perform(ChangeContext context, IProgressMonitor pm) throws ChangeAbortException, CoreException {
		pm.beginTask(getName(), 2);
		try{
			if (!isActive())
				return;
				
			String newName= getNewResourceName();
			doPerform(getDestinationProjectPath().append(newName), new SubProgressMonitor(pm, 1));
		} catch (Exception e) {
			handleException(context, e);
			setActive(false);	
		} finally{
			pm.done();
		}
	}

	protected abstract void doPerform(IPath destinationPath, IProgressMonitor pm) throws JavaModelException;

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		return new NullChange();
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return getRoot();
	}
	
	protected IPackageFragmentRoot getRoot(){
		return (IPackageFragmentRoot)JavaCore.create(fRootHandle);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#isUndoable()
	 */
	public boolean isUndoable() {
		return false;
	}
	
	protected IPath getDestinationProjectPath(){
		return fDestinationPath;
	}

	protected IProject getDestinationProject(){
		return Utils.getProject(getDestinationProjectPath());
	}
	
	private String getNewResourceName(){
		if (fNewNameQuery == null)
			return getRoot().getElementName();
		String name= fNewNameQuery.getNewName();
		if (name == null)
			return getRoot().getElementName();
		return name;
	}
	
	protected int getUpdateModelFlags(boolean isCopy) throws JavaModelException{
		final int destination= IPackageFragmentRoot.DESTINATION_PROJECT_CLASSPATH;
		final int replace= IPackageFragmentRoot.REPLACE;
		final int originating;
		final int otherProjects;
		if (isCopy){
			originating= 0; //ORIGINATING_PROJECT_CLASSPATH does not apply to copy
			otherProjects= 0;//OTHER_REFERRING_PROJECTS_CLASSPATH does not apply to copy
		} else{
			originating= IPackageFragmentRoot.ORIGINATING_PROJECT_CLASSPATH;
			otherProjects= IPackageFragmentRoot.OTHER_REFERRING_PROJECTS_CLASSPATH;
		}
		
		if (! JavaCore.create(getDestinationProject()).exists())
			return replace | originating;

		if (! getRoot().isArchive())
			return replace | originating | destination;

		if (fUpdateClasspathQuery == null)
			return replace | originating;

		IJavaProject[] referencingProjects= JavaElementUtil.getReferencingProjects(getRoot());
		if (referencingProjects.length == 0)
			return replace | originating | destination;

		boolean updateOtherProjectsToo= fUpdateClasspathQuery.confirmManipulation(getRoot(), referencingProjects);	
		if (updateOtherProjectsToo)
			return replace | originating | destination | otherProjects;
		else
			return replace | originating | destination;
	}
	
	protected int getResourceUpdateFlags(){
		return IResource.KEEP_HISTORY | IResource.SHALLOW;
	}
}
