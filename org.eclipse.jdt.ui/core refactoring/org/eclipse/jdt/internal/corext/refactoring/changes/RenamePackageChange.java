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
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;


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
		return (IPackageFragment)getModifiedLanguageElement();
	}
	
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("RenamePackageChange.name", new String[]{getOldName(), getNewName()}); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see AbstractRenameChange#createUndoChange()
	 */
	protected IChange createUndoChange() {
		if (getPackage() == null)
			return new NullChange();
		return new RenamePackageChange(createNewPath(), getNewName(), getOldName());
	}
	
	protected void doRename(IProgressMonitor pm) throws JavaModelException, CoreException {
		IPackageFragment pack= getPackage();
		if (pack != null)
			pack.rename(getNewName(), false, pm);
	}

	public RefactoringStatus aboutToPerform(ChangeContext context, IProgressMonitor pm) {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		RefactoringStatus result= super.aboutToPerform(context, pm);
		IJavaElement element= (IJavaElement)getModifiedLanguageElement();
		if (element != null && element.exists() && context.getUnsavedFiles().length > 0 && element instanceof IPackageFragment) {
			IPackageFragment pack= (IPackageFragment)element;
			try {
				ICompilationUnit[] units= pack.getCompilationUnits();
				if (units == null || units.length == 0)
					return result;
					
				pm.beginTask("", units.length); //$NON-NLS-1$
				for (int i= 0; i < units.length; i++) {
					pm.subTask(RefactoringCoreMessages.getFormattedString("RenamePackageChange.checking_change", element.getElementName())); //$NON-NLS-1$
					checkIfModifiable(units[i], result, context);
					pm.worked(1);
				}
				pm.done();
			} catch (JavaModelException e) {
				handleJavaModelException(e, result);
			}
		}
		return result;
	}
}
