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
package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.JDTChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public abstract class AbstractJavaElementRenameChange extends JDTChange {

	private String fNewName;

	private String fOldName;

	private IPath fResourcePath;

	protected AbstractJavaElementRenameChange(IPath resourcePath, String oldName, String newName) {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		Assert.isNotNull(oldName, "old name"); //$NON-NLS-1$

		fResourcePath= resourcePath;
		fOldName= oldName;
		fNewName= newName;
	}

	protected IResource getResource() {
		return ResourcesPlugin.getWorkspace().getRoot().findMember(fResourcePath);
	}

	/**
	 * May be <code>null</code>.
	 */
	public Object getModifiedElement() {
		return JavaCore.create(getResource());
	}

	protected abstract Change createUndoChange() throws JavaModelException;

	protected abstract void doRename(IProgressMonitor pm) throws CoreException;

	public final Change perform(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("AbstractRenameChange.Renaming"), 1); //$NON-NLS-1$
			Change result= createUndoChange();
			doRename(new SubProgressMonitor(pm, 1));
			return result;
		} finally {
			pm.done();
		}
	}

	/**
	 * Gets the newName.
	 * 
	 * @return Returns a String
	 */
	protected String getNewName() {
		return fNewName;
	}

	/**
	 * Gets the resourcePath.
	 * 
	 * @return Returns a IPath
	 */
	protected IPath getResourcePath() {
		return fResourcePath;
	}

	/**
	 * Gets the oldName
	 * 
	 * @return Returns a String
	 */
	protected String getOldName() {
		return fOldName;
	}

	protected static RefactoringStatus checkIfModifiable(IPackageFragmentRoot root, IProgressMonitor pm) throws CoreException {
		if (root == null)
			return null;

		if (!root.exists())
			return null;

		if (root.isArchive())
			return null;

		if (root.isExternal())
			return null;

		RefactoringStatus result= new RefactoringStatus();

		IJavaElement[] packs= root.getChildren();
		if (packs == null || packs.length == 0)
			return null;

		pm.beginTask("", packs.length); //$NON-NLS-1$
		for (int i= 0; i < packs.length; i++) {
			result.merge(checkIfModifiable((IPackageFragment)packs[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return result;
	}

	protected static RefactoringStatus checkIfModifiable(IPackageFragment pack, IProgressMonitor pm) throws CoreException {
		ICompilationUnit[] units= pack.getCompilationUnits();
		if (units == null || units.length == 0)
			return null;

		RefactoringStatus result= new RefactoringStatus();

		pm.beginTask("", units.length); //$NON-NLS-1$
		for (int i= 0; i < units.length; i++) {
			pm.subTask(
				RefactoringCoreMessages.getString("AbstractJavaElementRenameChange.checking_change")  //$NON-NLS-1$
				+ pack.getElementName());
			checkIfModifiable(result, units[i]);
			pm.worked(1);
		}
		pm.done();
		return result;
	}
}
