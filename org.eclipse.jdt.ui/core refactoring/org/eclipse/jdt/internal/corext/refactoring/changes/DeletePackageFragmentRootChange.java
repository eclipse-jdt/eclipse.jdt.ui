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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

public class DeletePackageFragmentRootChange extends AbstractDeleteChange{
	
	private final String fHandle;
	private final IPackageFragmentRootManipulationQuery fUpdateClasspathQuery;

	public DeletePackageFragmentRootChange(IPackageFragmentRoot root, IPackageFragmentRootManipulationQuery updateClasspathQuery){
		Assert.isNotNull(root);
		Assert.isTrue(! root.isExternal());
		fHandle= root.getHandleIdentifier();
		fUpdateClasspathQuery= updateClasspathQuery;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#getName()
	 */
	public String getName() {
		String[] keys= {getRoot().getElementName()};
		return RefactoringCoreMessages.getFormattedString("DeletePackageFragmentRootChange.delete", keys); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedElement() {
		return getRoot();
	}
	
	private IPackageFragmentRoot getRoot(){
		return (IPackageFragmentRoot)JavaCore.create(fHandle);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.changes.AbstractDeleteChange#doDelete(org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void doDelete(IProgressMonitor pm) throws CoreException {
		if (! confirmDeleteIfReferenced())
			return;
		int resourceUpdateFlags= IResource.KEEP_HISTORY;
		int jCoreUpdateFlags= IPackageFragmentRoot.ORIGINATING_PROJECT_CLASSPATH | IPackageFragmentRoot.OTHER_REFERRING_PROJECTS_CLASSPATH;
		getRoot().delete(resourceUpdateFlags, jCoreUpdateFlags, pm);
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
