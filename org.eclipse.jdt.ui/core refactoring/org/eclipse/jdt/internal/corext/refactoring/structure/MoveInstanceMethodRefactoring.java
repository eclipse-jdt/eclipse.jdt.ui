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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.Corext;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;


public class MoveInstanceMethodRefactoring extends Refactoring {
	
	public static interface INewReceiver {
		public String getName();
		
		public ITypeBinding getType();
		
		public IBinding getBinding();
		
		public boolean isField();
		
		public boolean isParameter();
	}
	
	private final IMethod fMethod;
	private final ICompilationUnit fCU;
	private final int fSelectionStart, fSelectionLength;
	private CodeGenerationSettings fCodeGenerationSettings;

	private InstanceMethodMover fMover;
	
	public static boolean isAvailable(IMethod method) throws JavaModelException {
		return method.exists() && !method.isConstructor() && !method.isBinary()
				&& method.getCompilationUnit() != null && !JdtFlags.isStatic(method)
				&& !method.getDeclaringType().isLocal();
	}

	public static MoveInstanceMethodRefactoring create(IMethod method, CodeGenerationSettings codeGenerationSettings) throws JavaModelException {		
		if (! isAvailable(method))	
			return null;
		return new MoveInstanceMethodRefactoring(method, method.getCompilationUnit(), method.getNameRange().getOffset(), method.getNameRange().getLength(), codeGenerationSettings);
	}
	
	private MoveInstanceMethodRefactoring(IMethod method, ICompilationUnit cu, int selectionStart, int selectionLength, CodeGenerationSettings codeGenerationSettings) {
		fMethod= method;
		fCU= cu;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCodeGenerationSettings= codeGenerationSettings;		
	}
	
	public ICompilationUnit getSourceCU(){
		return fCU;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus status= initializeMover();
		if(status.hasFatalError())
			return status;
		status.merge(fMover.checkInitialState());
		return status;
	}
	
	private RefactoringStatus initializeMover() {
		RefactoringStatus status= new RefactoringStatus();
		MethodDeclaration declaration= findMethodDeclaration(status);
		if(status.hasFatalError())
			return status;
		Assert.isNotNull(declaration);
		fMover= InstanceMethodMover.create(declaration, fCU, fCodeGenerationSettings);
		if (fMover == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveInstanceMethodRefactoring.2")); //$NON-NLS-1$
		return new RefactoringStatus();
	}
	
	private MethodDeclaration findMethodDeclaration(RefactoringStatus status) {
		CompilationUnit root=  new RefactoringASTParser(AST.JLS2).parse(fCU, true);
		
		ASTNode node= NodeFinder.perform(root, fSelectionStart, fSelectionLength);

		if(node instanceof MethodDeclaration)
			return (MethodDeclaration) node;
		
		ASTNode parentNode= ASTNodes.getParent(node, MethodDeclaration.class);
		if(parentNode != null)
			return (MethodDeclaration) parentNode;
		
		status.merge(RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("MoveInstanceMethodRefactoring.method_declaration"), null, Corext.getPluginId(), RefactoringStatusCodes.METHOD_NOT_SELECTED, null)); //$NON-NLS-1$
		return null;
	} 
	
	public IMethod getMethodToMove() {
		return fMethod;
	}
	
	public String getNewMethodName() {
		return fMover.getNewMethodName();
	}
	
	public RefactoringStatus setNewMethodName(String newMethodName) {
		Assert.isNotNull(newMethodName);
		
		RefactoringStatus status= Checks.checkMethodName(newMethodName);
		if(status.hasFatalError())
			return status;
		
		fMover.setNewMethodName(newMethodName);
		return status;
	}
	
	public String getOriginalReceiverParameterName() {
		return fMover.getOriginalReceiverParameterName();
	}
	
	public RefactoringStatus setOriginalReceiverParameterName(String originalReceiverParameterName) {
		RefactoringStatus status= Checks.checkTempName(originalReceiverParameterName);
		if(status.hasFatalError())
			return status;
		fMover.setOriginalReceiverParameterName(originalReceiverParameterName);
		return status;
	}

	public void setInlineDelegator(boolean inlineDelegator) {
		fMover.setInlineDelegator(inlineDelegator);
	}

	public void setRemoveDelegator(boolean removeDelegator) {
		fMover.setRemoveDelegator(removeDelegator);
	}

	public INewReceiver[] getPossibleNewReceivers() {
		return fMover.getPossibleNewReceivers();
	}

	/**
	 * @param chosen	Must be a element of the result
	 * of a call to getPossibleNewReceivers()
	 */
	public void chooseNewReceiver(INewReceiver chosen) {
		fMover.chooseNewReceiver(chosen);
	}	
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		return fMover.checkInput(pm);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		return fMover.createChange(pm);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("MoveInstanceMethodRefactoring.name"); //$NON-NLS-1$
	}
}
