/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.methods;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenameStaticMethodRefactoring extends RenameMethodRefactoring {

	public RenameStaticMethodRefactoring(ITextBufferChangeCreator changeCreator, IJavaSearchScope scope, IMethod method, String newName){
		super(changeCreator, scope, method, newName);
	}
	
	public RenameStaticMethodRefactoring(ITextBufferChangeCreator changeCreator, IMethod method) {
		super(changeCreator, method);
	}

	//---------- Conditions --------------
		
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("checking preconditions ...", 2);
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkInput(new SubProgressMonitor(pm, 1)));
		pm.worked(1);
		if (hierarchyDeclaresMethodName(pm, getMethod(), getNewName()))
			result.addError("Hierarchy declares a method named " + getNewName() + " with the same number of paramters.");
		pm.done();
		return result;
	}
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkActivation(pm));
		result.merge(checkAvailability(getMethod()));
		
		HackFinder.fixMeSoon("remove this constraint");	
		if (Flags.isNative(getMethod().getFlags()))
			result.addFatalError("not applicable to native methods");
			
		if (Flags.isPrivate(getMethod().getFlags()))
			result.addFatalError("must not be private");
		if (! Flags.isStatic(getMethod().getFlags()))
			result.addFatalError("must be static");	
		return result;
	}
}