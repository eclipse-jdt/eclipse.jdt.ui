/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JDTChange;
import org.eclipse.jdt.internal.corext.util.Messages;

/**
 * Represents a change that renames a given resource
 */
public class RenameResourceChange extends JDTChange {

	private static final String ID_RENAME_RESOURCE= "org.eclipse.jdt.ui.rename.resource"; //$NON-NLS-1$

	private static final String ATTRIBUTE_PATH= "path"; //$NON-NLS-1$

	private static final String ATTRIBUTE_NAME= "name"; //$NON-NLS-1$

	/*
	 * we cannot use handles because they became invalid when you rename the resource.
	 * paths do not.
	 */
	private IPath fResourcePath;
	private String fNewName;
	private long fStampToRestore;

	/**
	 * @param newName includes the extension
	 */
	public RenameResourceChange(IResource resource, String newName) {
		this(resource.getFullPath(), newName, IResource.NULL_STAMP);
	}

	private RenameResourceChange(IPath resourcePath, String newName, long stampToRestore) {
		fResourcePath= resourcePath;
		fNewName= newName;
		fStampToRestore= stampToRestore;
	}

	private IResource getResource() {
		return ResourcesPlugin.getWorkspace().getRoot().findMember(fResourcePath);
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		IResource resource= getResource();
		if (resource == null || ! resource.exists()) {
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.RenameResourceChange_does_not_exist, fResourcePath.toString())); 
		} else {
			// don't check read only. We don't change the
			// content of the file hence we don't call
			// validate edit upfront.
			return super.isValid(pm, DIRTY);
		}
	}
	
	/*
	 * to avoid the exception senders should check if a resource with the new name
	 * already exists
	 */
	public Change perform(IProgressMonitor pm) throws CoreException {
		try {
			if (false)
				throw new NullPointerException();
			pm.beginTask(RefactoringCoreMessages.RenameResourceChange_rename_resource, 1); 

			IResource resource= getResource();
			long currentStamp= resource.getModificationStamp();
			IPath newPath= renamedResourcePath(fResourcePath, fNewName);
			resource.move(newPath, getCoreRenameFlags(), pm);
			if (fStampToRestore != IResource.NULL_STAMP) {
				IResource newResource= ResourcesPlugin.getWorkspace().getRoot().findMember(newPath);
				newResource.revertModificationStamp(fStampToRestore);
			}
			String oldName= fResourcePath.lastSegment();
			return new RenameResourceChange(newPath, oldName, currentStamp);
		} finally {
			pm.done();
		}
	}

	private int getCoreRenameFlags() {
		if (getResource().isLinked())
			return IResource.SHALLOW;
		else
			return IResource.NONE;
	}

	/*
	 * changes resource names /s/p/A.java renamed to B.java becomes /s/p/B.java
	 */
	public static IPath renamedResourcePath(IPath path, String newName) {
		return path.removeLastSegments(1).append(newName);
	}

	public String getName() {
		return Messages.format(
			RefactoringCoreMessages.RenameResourceChange_name, new String[]{fResourcePath.toString(), 
			fNewName});
	}

	public Object getModifiedElement() {
		return getResource();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Change#getRefactoringDescriptor()
	 */
	public RefactoringDescriptor getRefactoringDescriptor() {
		final Map arguments= new HashMap();
		arguments.put(ATTRIBUTE_PATH, fResourcePath.toPortableString());
		arguments.put(ATTRIBUTE_NAME, fNewName);
		return new RefactoringDescriptor(ID_RENAME_RESOURCE, getResource().getProject().getName(), MessageFormat.format(RefactoringCoreMessages.RenameResourceChange_descriptor_description, new String[] { getResource().getName(), fNewName}), null, arguments);
	}
}
