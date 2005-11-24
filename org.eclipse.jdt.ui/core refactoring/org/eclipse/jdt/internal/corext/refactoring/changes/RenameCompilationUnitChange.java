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

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.Messages;


public class RenameCompilationUnitChange extends AbstractJavaElementRenameChange {

	private static final String ID_RENAME_COMPILATION_UNIT= "org.eclipse.jdt.ui.rename.compilationunit"; //$NON-NLS-1$
	private static final String ATTRIBUTE_PATH= "path"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME= "name"; //$NON-NLS-1$

	public RenameCompilationUnitChange(ICompilationUnit cu, String newName) {
		this(ResourceUtil.getResource(cu).getFullPath(), cu.getElementName(), newName, IResource.NULL_STAMP);
		Assert.isTrue(!cu.isReadOnly(), "cu must not be read-only"); //$NON-NLS-1$
	}
	
	private RenameCompilationUnitChange(IPath resourcePath, String oldName, String newName, long stampToRestore){
		super(resourcePath, oldName, newName, stampToRestore);
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return super.isValid(pm, READ_ONLY | SAVE_IF_DIRTY);
	}
	
	protected IPath createNewPath() {
		if (getResourcePath().getFileExtension() != null)
			return getResourcePath().removeFileExtension().removeLastSegments(1).append(getNewName());
		else	
			return getResourcePath().removeLastSegments(1).append(getNewName());
	}
	
	public String getName() {
		return Messages.format(RefactoringCoreMessages.RenameCompilationUnitChange_name, new String[]{getOldName(), getNewName()}); 
	}
	
	/*
	 * @see AbstractRenameChange#createUndoChange()
	 */
	protected Change createUndoChange(long stampToRestore) throws JavaModelException{
		return new RenameCompilationUnitChange(createNewPath(), getNewName(), getOldName(), stampToRestore);
	}
	
	protected void doRename(IProgressMonitor pm) throws CoreException {
		ICompilationUnit cu= (ICompilationUnit)getModifiedElement();
		if (cu != null)
			cu.rename(getNewName(), false, pm);
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Change#getRefactoringDescriptor()
	 */
	public RefactoringDescriptor getRefactoringDescriptor() {
		final Map arguments= new HashMap();
		arguments.put(ATTRIBUTE_PATH, getResourcePath().toPortableString());
		arguments.put(ATTRIBUTE_NAME, getNewName());
		String label= null;
		final ICompilationUnit unit= (ICompilationUnit) getModifiedElement();
		if (unit != null) {
			final IPackageFragment fragment= (IPackageFragment) unit.getParent();
			if (!fragment.isDefaultPackage())
				label= fragment.getElementName() + "." + unit.getElementName(); //$NON-NLS-1$
			else
				label= unit.getElementName();
		} else
			label= getOldName();
		return new RefactoringDescriptor(ID_RENAME_COMPILATION_UNIT, getResource().getProject().getName(), MessageFormat.format(RefactoringCoreMessages.RenameCompilationUnitChange_descriptor_description, new String[] { label, getNewName()}), null, arguments, RefactoringDescriptor.STRUCTURAL_CHANGE);
	}
}