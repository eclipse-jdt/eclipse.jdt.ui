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

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class RenameSourceFolderChange extends AbstractJavaElementRenameChange {

	public RenameSourceFolderChange(IPackageFragmentRoot sourceFolder, String newName) {
		this(sourceFolder.getPath(), sourceFolder.getElementName(), newName);
		Assert.isTrue(!sourceFolder.isReadOnly(), "should not be read only");  //$NON-NLS-1$
		Assert.isTrue(!sourceFolder.isArchive(), "should not be an archive");  //$NON-NLS-1$
	}
	
	private RenameSourceFolderChange(IPath resourcePath, String oldName, String newName){
		super(resourcePath, oldName, newName);
	}
	
	private IPath createNewPath(){
		return getResourcePath().removeLastSegments(1).append(getNewName());
	}
	
	/* non java-doc
	 * @see AbstractRenameChange#createUndoChange()
	 */
	protected Change createUndoChange() {
		return new RenameSourceFolderChange(createNewPath(), getNewName(), getOldName());
	}

	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("RenameSourceFolderChange.rename", //$NON-NLS-1$
			new String[]{getOldName(), getNewName()});
	}

	/* non java-doc
	 * @see AbstractRenameChange#doRename
	 */	
	protected void doRename(IProgressMonitor pm) throws CoreException {
		IPackageFragmentRoot sourceFolder= getSourceFolder();
		if (sourceFolder != null)
			sourceFolder.move(getNewPath(), getCoreMoveFlags(), getJavaModelUpdateFlags(), null, pm);
	}

	private IPath getNewPath() {
		return getResource().getFullPath().removeLastSegments(1).append(getNewName());
	}

	private IPackageFragmentRoot getSourceFolder() {
		return (IPackageFragmentRoot)getModifiedElement();
	}

	private int getJavaModelUpdateFlags() {
		return 	IPackageFragmentRoot.DESTINATION_PROJECT_CLASSPATH 
				| 	IPackageFragmentRoot.ORIGINATING_PROJECT_CLASSPATH
				| 	IPackageFragmentRoot.OTHER_REFERRING_PROJECTS_CLASSPATH
				| 	IPackageFragmentRoot.REPLACE;
	}

	private int getCoreMoveFlags() {
		if (getResource().isLinked())
			return IResource.SHALLOW;
		else
			return IResource.NONE;
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();

		result.merge(checkIfModifiable(getSourceFolder(), pm));
		
		return result;
	}
}

