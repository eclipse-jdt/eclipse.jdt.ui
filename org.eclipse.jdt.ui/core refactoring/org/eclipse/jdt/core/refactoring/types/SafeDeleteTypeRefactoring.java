/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.types;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
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
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.DeleteCompilationUnitChange;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class SafeDeleteTypeRefactoring extends TypeRefactoring{

	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public SafeDeleteTypeRefactoring(ITextBufferChangeCreator changeCreator, IJavaSearchScope scope, IType type) {
		super(scope, type);
		Assert.isNotNull(changeCreator, "change creator");
		fTextBufferChangeCreator= changeCreator;
	}

	public SafeDeleteTypeRefactoring(ITextBufferChangeCreator changeCreator, IType type) {
		super(type);
		Assert.isNotNull(changeCreator, "change creator");
		fTextBufferChangeCreator= changeCreator;
	}
	
	/**
	 * @see IRefactoring#getName
	 */
	public String getName(){
		return "SafeDeleteTypeRefactoring:" + getType().getFullyQualifiedName();
	}
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		Assert.isNotNull(getType());
		return checkAvailability(getType());
	}
		
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		Assert.isNotNull(getType());
		pm.beginTask("checking preconditions", 3);
		RefactoringStatus result= new RefactoringStatus();
		int r= countReferences(new SubProgressMonitor(pm, 1));
		if (r == 1)
			result.addError("type " + getType().getFullyQualifiedName() + " is referenced in 1 place");
		else if (r > 1)
			result.addError("type " + getType().getFullyQualifiedName() + " is referenced " + r + " times");

		result.merge(Checks.checkForMainMethod(getType()));
		pm.worked(1);
		if (getType().getTypes().length > 0)
			result.addFatalError("Types with nested types not supported for this refactoring");
		pm.worked(1);	
		pm.done();	
		return result;
	}

	private int countReferences(IProgressMonitor pm) throws JavaModelException{
		return RefactoringSearchEngine.countingSearch(pm, getScope(), createSearchPattern());
	}
		
	private ISearchPattern createSearchPattern() throws JavaModelException{
		ISearchPattern pattern= SearchEngine.createSearchPattern(getType(), IJavaSearchConstants.REFERENCES);
		IMethod[] methods= getType().getMethods();
		for (int i= 0; i < methods.length; i++){
			if (methods[i].isConstructor()) {
				pattern= SearchEngine.createOrSearchPattern(pattern, SearchEngine.createSearchPattern(methods[i], IJavaSearchConstants.REFERENCES));
			}
		}	
		return pattern;	
	}

	public IChange createChange(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("create change", 1);
		int offset= getType().getSourceRange().getOffset();
		HackFinder.fixMeLater("1 is for the semicolon at the end. should probably create a deleteJavaElementChnage and use it instead");
		int length=1 + getType().getSourceRange().getLength();
		pm.worked(1);
		pm.done();
		if (onlyTypeInCU()){
			return new DeleteCompilationUnitChange(getType().getCompilationUnit());
		} else{
			ITextBufferChange change= fTextBufferChangeCreator.create("Delete Type", getType().getCompilationUnit());
			change.addDelete("Delete Type", offset, length);
			return change;
		}
	}
	
	private boolean onlyTypeInCU() throws JavaModelException{
		if (! Checks.isTopLevel(getType())){
			return false;
		}
		return 1 == ((ICompilationUnit)getType().getParent()).getTypes().length;
	}
}
