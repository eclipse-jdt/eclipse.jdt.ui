/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.methods;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IChange;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class SafeDeleteMethodRefactoring extends MethodRefactoring{

	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public SafeDeleteMethodRefactoring(ITextBufferChangeCreator changeCreator, IJavaSearchScope scope, IMethod method) {
		super(scope, method);
		fTextBufferChangeCreator= changeCreator;
		Assert.isNotNull(fTextBufferChangeCreator, "change creator");
	}
	
	public SafeDeleteMethodRefactoring(ITextBufferChangeCreator changeCreator, IMethod method) {
		super(method);
		fTextBufferChangeCreator= changeCreator;
		Assert.isNotNull(fTextBufferChangeCreator, "change creator");
	}
	
	/**
	 * @see IRefactoring#getName
	 */
	 public String getName(){
	 	return "SafeDeleteMethodRefactoring: " + getMethod().getElementName();
	 }

	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("checking preconditions", 1);
		RefactoringStatus result= new RefactoringStatus();
		int r= countReferences(new SubProgressMonitor(pm, 1));
		if (r == 0)
			return result;
		else if  (r == 1)
			result.addFatalError("method is referenced in 1 place");
		else 
			result.addFatalError("method is referenced " + r + " times");
		return result;
	}
		
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(getMethod()));
		if (getMethod().isConstructor())
			result.addFatalError("can't rename a constructor");
		if 	(! Flags.isStatic(getMethod().getFlags()) && ! Flags.isPrivate(getMethod().getFlags()))
			result.addFatalError("only applicable for static or private methods");	
		return result;
	}
		
	private int countReferences(IProgressMonitor pm)  throws JavaModelException{
		return RefactoringSearchEngine.countingSearch(pm, getScope(), createSearchPattern());
	}
	
	private ISearchPattern createSearchPattern() throws JavaModelException{
		return SearchEngine.createSearchPattern(getMethod(), IJavaSearchConstants.REFERENCES);
	}
	
	//-------- Changes ---------------
	
	public IChange createChange(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("creating change", 1);
		int offset= getMethod().getSourceRange().getOffset();
		//wrong! the semicolon can be anywhere
		int length= 1 + getMethod().getSourceRange().getLength();
		IResource resource= getResource(getMethod());
		pm.worked(1);
		pm.done();
		
		ITextBufferChange change= fTextBufferChangeCreator.create("Delete Method", getMethod().getCompilationUnit());
		change.addDelete("Delete Method", offset, length);
		return change;
	}
	
}