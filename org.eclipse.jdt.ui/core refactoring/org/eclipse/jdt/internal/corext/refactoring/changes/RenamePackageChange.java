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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.ltk.refactoring.core.NullChange;


public class RenamePackageChange extends AbstractJavaElementRenameChange {

	public RenamePackageChange(IPackageFragment pack, String newName) throws JavaModelException{
		this(pack.getPath(), pack.getElementName(), newName);
		Assert.isTrue(!pack.isReadOnly(), "package must not be read only"); //$NON-NLS-1$
	}
	
	private RenamePackageChange(IPath resourcePath, String oldName, String newName){
		super(resourcePath, oldName, newName);
	}
		
	protected IPath createPath(String packageName){
		return new Path(packageName.replace('.', IPath.SEPARATOR));
	}
	
	private IPath createNewPath(){
		IPackageFragment oldPackage= getPackage();
		IPath oldPackageName= createPath(oldPackage.getElementName());
		IPath newPackageName= createPath(getNewName());
		return getResourcePath().removeLastSegments(oldPackageName.segmentCount()).append(newPackageName);
	}

	private IPackageFragment getPackage() {
		return (IPackageFragment)getModifiedElement();
	}
	
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("RenamePackageChange.name", new String[]{getOldName(), getNewName()}); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see AbstractRenameChange#createUndoChange()
	 */
	protected Change createUndoChange() {
		if (getPackage() == null)
			return new NullChange();
		return new RenamePackageChange(createNewPath(), getNewName(), getOldName());
	}
	
	protected void doRename(IProgressMonitor pm) throws JavaModelException, CoreException {
		IPackageFragment pack= getPackage();
		if (pack != null)
			pack.rename(getNewName(), false, pm);
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		IJavaElement element= (IJavaElement)getModifiedElement();
		if (element != null && element.exists() && element instanceof IPackageFragment) {
			IPackageFragment pack= (IPackageFragment)element;
			ICompilationUnit[] units= pack.getCompilationUnits();
			if (units == null || units.length == 0)
				return result;
				
			pm.beginTask("", units.length); //$NON-NLS-1$
			for (int i= 0; i < units.length; i++) {
				pm.subTask(RefactoringCoreMessages.getFormattedString("RenamePackageChange.checking_change", element.getElementName())); //$NON-NLS-1$
				checkIfModifiable(result, units[i], true, true);
				pm.worked(1);
			}
			pm.done();
		}
		return result;
	}
}
