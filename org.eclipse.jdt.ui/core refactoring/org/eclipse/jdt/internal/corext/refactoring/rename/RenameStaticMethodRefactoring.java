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

	private RenameStaticMethodRefactoring(IMethod method) {
		super(method);
	}
	
	public static RenameMethodRefactoring create(IMethod method) throws JavaModelException{
		if (! isAvailable(method))
			return null;
		return new RenameStaticMethodRefactoring(method);
	}
	
	public static boolean isAvailable(IMethod method) throws JavaModelException{
		return RenameMethodRefactoring.isAvailable(method) && internalIsAvailable(method);
	}
	
	static boolean internalIsAvailable(IMethod method) throws JavaModelException{
		return ! JdtFlags.isPrivate(method) && JdtFlags.isStatic(method);
	}
	
	//---------- preconditions --------------
		
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
