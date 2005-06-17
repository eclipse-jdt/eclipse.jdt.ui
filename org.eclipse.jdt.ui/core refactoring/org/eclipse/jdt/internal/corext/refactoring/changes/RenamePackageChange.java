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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

public class RenamePackageChange extends AbstractJavaElementRenameChange {

	private Map fCompilationUnitStamps;

	public RenamePackageChange(IPackageFragment pack, String newName) {
		this(pack.getPath(), pack.getElementName(), newName, IResource.NULL_STAMP, null);
		Assert.isTrue(!pack.isReadOnly(), "package must not be read only"); //$NON-NLS-1$
	}

	private RenamePackageChange(IPath resourcePath, String oldName, String newName, long stampToRestore,
		Map compilationUnitStamps) {
		super(resourcePath, oldName, newName, stampToRestore);
		fCompilationUnitStamps= compilationUnitStamps;
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		IJavaElement element= (IJavaElement)getModifiedElement();
		// don't check for read-only since we don't go through 
		// validate edit.
		result.merge(isValid(DIRTY));
		if (result.hasFatalError())
			return result;
		if (element != null && element.exists() && element instanceof IPackageFragment) {
			IPackageFragment pack= (IPackageFragment)element;
			ICompilationUnit[] units= pack.getCompilationUnits();
			if (units == null || units.length == 0)
				return result;

			pm.beginTask("", units.length); //$NON-NLS-1$
			for (int i= 0; i < units.length; i++) {
				pm.subTask(Messages.format(
					RefactoringCoreMessages.RenamePackageChange_checking_change, element.getElementName())); //$NON-NLS-1$
				checkIfModifiable(result, units[i], READ_ONLY | DIRTY);
				pm.worked(1);
			}
			pm.done();
		}
		return result;
	}

	protected IPath createNewPath() {
		IPackageFragment oldPackage= getPackage();
		IPath oldPackageName= createPath(oldPackage.getElementName());
		IPath newPackageName= createPath(getNewName());
		return getResourcePath().removeLastSegments(oldPackageName.segmentCount()).append(newPackageName);
	}

	private IPath createPath(String packageName) {
		return new Path(packageName.replace('.', IPath.SEPARATOR));
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.RenamePackageChange_name, new String[]{getOldName(), getNewName()}); 
	}

	protected Change createUndoChange(long stampToRestore) throws CoreException {
		IPackageFragment pack= getPackage();
		if (pack == null)
			return new NullChange();
		ICompilationUnit[] units= pack.getCompilationUnits();
		Map stamps= new HashMap();
		for (int i= 0; i < units.length; i++) {
			IResource resource= units[i].getResource();
			long stamp= IResource.NULL_STAMP;
			if (resource != null && (stamp= resource.getModificationStamp()) != IResource.NULL_STAMP) {
				stamps.put(resource, new Long(stamp));
			}
		}
		return new RenamePackageChange(createNewPath(), getNewName(), getOldName(), stampToRestore, stamps);
	}

	protected void doRename(IProgressMonitor pm) throws CoreException {
		IPackageFragment pack= getPackage();
		if (pack != null) {
			IPath newPath= createNewPath();
			pack.rename(getNewName(), false, pm);
			if (fCompilationUnitStamps != null) {
				IPackageFragment newPack= (IPackageFragment)JavaCore.create(
					ResourcesPlugin.getWorkspace().getRoot().getFolder(newPath));
				if (newPack.exists()) {
					ICompilationUnit[] units= newPack.getCompilationUnits();
					for (int i= 0; i < units.length; i++) {
						IResource resource= units[i].getResource();
						if (resource != null) {
							Long stamp= (Long)fCompilationUnitStamps.get(resource);
							if (stamp != null) {
								resource.revertModificationStamp(stamp.longValue());
							}
						}
					}
				}
			}
		}
	}

	private IPackageFragment getPackage() {
		return (IPackageFragment)getModifiedElement();
	}
}
