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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;


public class RenameCompilationUnitChange extends AbstractJavaElementRenameChange {

	public RenameCompilationUnitChange(ICompilationUnit cu, String newName) throws JavaModelException{
		this(ResourceUtil.getResource(cu).getFullPath(), cu.getElementName(), newName);
		Assert.isTrue(!cu.isReadOnly(), "cu must not be read-only"); //$NON-NLS-1$
	}
	
	private RenameCompilationUnitChange(IPath resourcePath, String oldName, String newName){
		super(resourcePath, oldName, newName);
	}
	
	protected IPath createNewPath() throws JavaModelException{
		if (getResourcePath().getFileExtension() != null)
			return getResourcePath().removeFileExtension().removeLastSegments(1).append(getNewName());
		else	
			return getResourcePath().removeLastSegments(1).append(getNewName());
	}
	
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("RenameCompilationUnitChange.name", new String[]{getOldName(), getNewName()}); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * @see AbstractRenameChange#createUndoChange()
	 */
	protected Change createUndoChange() throws JavaModelException{
		return new RenameCompilationUnitChange(createNewPath(), getNewName(), getOldName());
	}
	
	protected void doRename(IProgressMonitor pm) throws CoreException {
		ICompilationUnit cu= (ICompilationUnit)getModifiedElement();
		if (cu != null)
			cu.rename(getNewName(), false, pm);
	}
}
