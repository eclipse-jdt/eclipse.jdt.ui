/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class UseSupertypeWherePossibleRefactoring extends Refactoring{
	
	private final ASTNodeMappingManager fASTMappingManager;
	private final CodeGenerationSettings fCodeGenerationSettings;
	private IType fInputType;
	private TextChangeManager fChangeManager;
	private IType fSuperTypeToUse;
	private IType[] fSuperTypes;
	private boolean fUseSupertypeInInstanceOf;
	
	public UseSupertypeWherePossibleRefactoring(IType clazz, CodeGenerationSettings codeGenerationSettings){
		Assert.isNotNull(clazz);
		Assert.isNotNull(codeGenerationSettings);
		fInputType= clazz;
		fCodeGenerationSettings= codeGenerationSettings;
		fASTMappingManager= new ASTNodeMappingManager();
		fUseSupertypeInInstanceOf= false;
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
	
	public RefactoringStatus checkPreactivation() throws JavaModelException {
		RefactoringStatus result= Checks.checkAvailability(fInputType);	
		if (result.hasFatalError())
			return result;
		return result;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
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

	/* non java-doc
	 * @see Refactoring#checkPreconditions(IProgressMonitor)
	 */
	public RefactoringStatus checkPreconditions(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= checkPreactivation();
		if (result.hasFatalError())
			return result;
		result.merge(super.checkPreconditions(pm));
		return result;
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
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);//$NON-NLS-1$
		try{
			RefactoringStatus result= new RefactoringStatus();		
			
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e){
			throw new JavaModelException(e);
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
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			CompositeChange builder= new CompositeChange(RefactoringCoreMessages.getString("UseSupertypeWherePossibleRefactoring.name")); //$NON-NLS-1$
			builder.addAll(fChangeManager.getAllChanges());
			return builder;	
		} finally{
			clearIntermediateState();
			pm.done();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("UseSupertypeWherePossibleRefactoring.name"); //$NON-NLS-1$
	}

	private void clearIntermediateState() {
		fASTMappingManager.clear();
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			pm.setTaskName(RefactoringCoreMessages.getString("UseSupertypeWherePossibleRefactoring.analyzing...")); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();
			updateReferences(manager, new SubProgressMonitor(pm, 1));
			return manager;
		} finally{
			pm.done();
		}	
	}

	private void updateReferences(TextChangeManager manager, IProgressMonitor pm) throws JavaModelException, CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		try{
			IMember[] members= getAllDeclaredAndInheritedMembers(fSuperTypeToUse, new SubProgressMonitor(pm, 1));
			String superTypeName= fSuperTypeToUse.getElementName();
			UseSupertypeWherePossibleUtil.updateReferences(manager, 
																				members, 
																				superTypeName, 
																				fInputType, 
																				fCodeGenerationSettings, 
																				fASTMappingManager, 
																				new SubProgressMonitor(pm, 1), 
																				fSuperTypeToUse, 
																				fUseSupertypeInInstanceOf);
		} finally {
			pm.done();
		}
	}
	
	private static IMember[] getAllDeclaredAndInheritedMembers(IType type, IProgressMonitor pm) throws JavaModelException{
		Set result= new HashSet();
		IType[] allClasses= type.newSupertypeHierarchy(pm).getAllSupertypes(type);
		for (int i= 0; i < allClasses.length; i++) {
			result.addAll(getMembers(allClasses[i]));
		}
		result.addAll(getMembers(type));
		result.addAll(getMembers(getObject(type.getJavaProject())));
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}
	
	//return a List of IMembers
	private static List getMembers(IType type) throws JavaModelException{
		IJavaElement[] allChildren= type.getChildren();
		List result= new ArrayList(allChildren.length);
		for (int i= 0; i < allChildren.length; i++) {
			if (! (allChildren[i] instanceof IMember))
				continue;
			if (allChildren[i] instanceof IInitializer)	
				continue;
			IMember member= (IMember)allChildren[i];
			if (allChildren[i] instanceof IMethod && ((IMethod)member).isConstructor())
				continue;
			result.add(member);	
		}
		return result;
	}

	private static IType getObject(IJavaProject jProject) throws JavaModelException {
		return jProject.findType("java.lang.Object"); //$NON-NLS-1$
	}

}
