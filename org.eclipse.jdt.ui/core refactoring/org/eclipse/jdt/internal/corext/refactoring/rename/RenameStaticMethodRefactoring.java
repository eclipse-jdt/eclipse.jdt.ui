/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

class RenameStaticMethodRefactoring extends RenameMethodRefactoring {

	RenameStaticMethodRefactoring(IMethod method) {
		super(method);
	}

	public RenameMethodRefactoring clone(IMethod method) {
		RenameMethodRefactoring result= new RenameStaticMethodRefactoring(method);
		setData(result);
		return result;
	}
	
	//---------- preconditions --------------
		
	/* non java-doc
	 * @see IPreactivatedRefactoring@checkPreactivation
	 */
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkPreactivation());
		result.merge(Checks.checkAvailability(getMethod()));
					
		if (JdtFlags.isPrivate(getMethod()))
			result.addFatalError(RefactoringCoreMessages.getString("RenameStaticMethodRefactoring.no_private")); //$NON-NLS-1$
		if (! JdtFlags.isStatic(getMethod()))
			result.addFatalError(RefactoringCoreMessages.getString("RenameStaticMethodRefactoring.only_static"));	 //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(super.checkInput(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;
			
			IMethod hierarchyMethod= hierarchyDeclaresMethodName(pm, getMethod(), getNewName());
			if (hierarchyMethod != null){
				Context context= JavaSourceContext.create(hierarchyMethod);
				result.addError(RefactoringCoreMessages.getFormattedString("RenameStaticMethodRefactoring.hierachy_declares", getNewName()), context); //$NON-NLS-1$
			}	
			return result;
		} finally{
			pm.done();
		}	
	}
}