/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.rename;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;

class RenameStaticMethodRefactoring extends RenameMethodRefactoring {
	RenameStaticMethodRefactoring(ITextBufferChangeCreator changeCreator, IMethod method) {
		super(changeCreator, method);
	}
	//---------- preconditions --------------
		
	/* non java-doc
	 * @see IPreactivatedRefactoring@checkPreactivation
	 */
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
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			pm.subTask(RefactoringCoreMessages.getString("RenameStaticMethodRefactoring.checking")); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(super.checkInput(new SubProgressMonitor(pm, 1)));
			pm.worked(1);
			pm.subTask(RefactoringCoreMessages.getString("RenameStaticMethodRefactoring.analyzing_hierachy")); //$NON-NLS-1$
			if (hierarchyDeclaresMethodName(pm, getMethod(), getNewName()))
				result.addError(RefactoringCoreMessages.getFormattedString("RenameStaticMethodRefactoring.hierachy_declares", getNewName())); //$NON-NLS-1$
			return result;
		} finally{
			pm.done();
		}	
	}
}