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
package org.eclipse.jdt.internal.corext.refactoring.structure;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class UseSupertypeWherePossibleRefactoring extends Refactoring{
	
	private IType fInputType;
	private TextChangeManager fChangeManager;
	private IType fSuperTypeToUse;
	private IType[] fSuperTypes;
	private boolean fUseSupertypeInInstanceOf;
    private CodeGenerationSettings fCodeGenerationSettings;
	
	private UseSupertypeWherePossibleRefactoring(IType clazz, CodeGenerationSettings codeGenerationSettings){
		Assert.isNotNull(clazz);
		Assert.isNotNull(codeGenerationSettings);
		fInputType= clazz;
		fUseSupertypeInInstanceOf= false;
		fCodeGenerationSettings= codeGenerationSettings;
	}
	
	public static UseSupertypeWherePossibleRefactoring create(IType type, CodeGenerationSettings codeGenerationSettings) throws JavaModelException{
		if (! isAvailable(type))
			return null;
		return new UseSupertypeWherePossibleRefactoring(type, codeGenerationSettings);
	}
	
	public static boolean isAvailable(IType type) throws JavaModelException{
		return Checks.isAvailable(type) && !type.isAnonymous();
	}
	
	public IType getInputType(){
		return fInputType;
	}
	
	public void setUseSupertypeInInstanceOf(boolean use){
		fUseSupertypeInInstanceOf= use;
	}

	public boolean getUseSupertypeInInstanceOf(){
		return fUseSupertypeInInstanceOf;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws CoreException {
		IType orig= (IType)WorkingCopyUtil.getOriginal(fInputType);
		if (orig == null || ! orig.exists()){
			String[] keys= {fInputType.getCompilationUnit().getElementName()};
			String message= RefactoringCoreMessages.getFormattedString("UseSupertypeWherePossibleRefactoring.deleted", keys); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fInputType= orig;
		fSuperTypes= getSuperTypes(pm);
		if (Checks.isException(fInputType, pm)){
			String message= RefactoringCoreMessages.getString("UseSupertypeWherePossibleRefactoring.unavailable_on_Throwable"); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}
		return Checks.checkIfCuBroken(fInputType);
	}

	private IType[] getSuperTypes(IProgressMonitor pm) throws JavaModelException {
		return JavaModelUtil.getAllSuperTypes(fInputType, pm);
	}

	public IType[] getSuperTypes() throws JavaModelException{
		return fSuperTypes;
	}
	
	public void setSuperTypeToUse(IType superType){
		Assert.isNotNull(superType);
		fSuperTypeToUse= superType;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1);//$NON-NLS-1$
		try {
			RefactoringStatus result= new RefactoringStatus();		
			
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1), result);
			if (result.hasFatalError())
				return result;
			result.merge(validateModifiesFiles());
			return result;
		} finally {
			pm.done();
		}	
	}

	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			return new ValidationStateChange(RefactoringCoreMessages.getString("UseSupertypeWherePossibleRefactoring.name"), fChangeManager.getAllChanges());//$NON-NLS-1$
		} finally {
			pm.done();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("UseSupertypeWherePossibleRefactoring.name"); //$NON-NLS-1$
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm, RefactoringStatus status) throws CoreException{
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.setTaskName(RefactoringCoreMessages.getString("UseSupertypeWherePossibleRefactoring.analyzing...")); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();
			updateReferences(manager, new SubProgressMonitor(pm, 1), status);
			return manager;
		} finally{
			pm.done();
		}	
	}

	private void updateReferences(TextChangeManager manager, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			ExtractInterfaceUtil.updateReferences(manager, fInputType, fSuperTypeToUse, 
			        new RefactoringWorkingCopyOwner(), true, new SubProgressMonitor(pm, 1), 
			        status, fCodeGenerationSettings);
		} finally {
			pm.done();
		}
	}
}
