/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.methods;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenameStaticMethodRefactoring extends RenameMethodRefactoring {
	public RenameStaticMethodRefactoring(ITextBufferChangeCreator changeCreator, IMethod method) {
		super(changeCreator, method);
	}
	//---------- Conditions --------------
		
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 2); //$NON-NLS-1$
		pm.subTask(RefactoringCoreMessages.getString("RenameStaticMethodRefactoring.checking")); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkInput(new SubProgressMonitor(pm, 1)));
		pm.worked(1);
		pm.subTask(RefactoringCoreMessages.getString("RenameStaticMethodRefactoring.analyzing_hierachy")); //$NON-NLS-1$
		if (hierarchyDeclaresMethodName(pm, getMethod(), getNewName()))
			result.addError(RefactoringCoreMessages.getFormattedString("RenameStaticMethodRefactoring.hierachy_declares", getNewName())); //$NON-NLS-1$
		pm.done();
		return result;
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkPreactivation());
		result.merge(checkAvailability(getMethod()));
					
		if (Flags.isPrivate(getMethod().getFlags()))
			result.addFatalError(RefactoringCoreMessages.getString("RenameStaticMethodRefactoring.no_private")); //$NON-NLS-1$
		if (! Flags.isStatic(getMethod().getFlags()))
			result.addFatalError(RefactoringCoreMessages.getString("RenameStaticMethodRefactoring.only_static"));	 //$NON-NLS-1$
		return result;
	}
}