/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.fields;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IField;
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
import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class SafeDeletePrivateFieldRefactoring extends FieldRefactoring{
	
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public SafeDeletePrivateFieldRefactoring(ITextBufferChangeCreator creator, IJavaSearchScope scope, IField field) {
		super(scope, field);
		fTextBufferChangeCreator= creator;
		Assert.isNotNull(fTextBufferChangeCreator, "change creator");
	}
	
	public SafeDeletePrivateFieldRefactoring(ITextBufferChangeCreator creator, IField field) {
		super(field);
		fTextBufferChangeCreator= creator;
		Assert.isNotNull(fTextBufferChangeCreator, "change creator");
	}

	/**
	 * @see IRefactoring#getName
	 */
	public String getName() {
		return "Safe Delete Field";
	}

	/**
	 * @see IRefactoring#createChange
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("create change", 1);
		int offset= getField().getSourceRange().getOffset();
		HackFinder.fixMeSoon("1 is for the semicolon - it is wrong");
		int length= 1 + getField().getSourceRange().getLength();
		pm.worked(1);
		pm.done();
		ITextBufferChange change= fTextBufferChangeCreator.create("Delete Filed", getField().getCompilationUnit());
		change.addDelete("Delete Field", offset, length);
		return change;
	}

	/**
	 * @see Refactoring#checkActivation
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		Assert.isNotNull(getField());
		return checkAvailability(getField());
	}

	/**
	 * @see Refactoring#checkInput
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("checking preconditions", 1);
		Assert.isNotNull(getField());
		RefactoringStatus result= new RefactoringStatus();
		int r= countReferences(pm); //passing the same pm
		if (r == 1)
			result.addError("Field " + getField().getElementName() + " is referenced in 1 place");
		else if (r > 1)
			result.addError("Field " + getField().getElementName() + " is referenced " + r + " times");
		pm.worked(1);	
		pm.done();
		return result;
	}
	
	private int countReferences(IProgressMonitor pm) throws JavaModelException{
		ISearchPattern pattern= SearchEngine.createSearchPattern(getField(), IJavaSearchConstants.REFERENCES);
		return RefactoringSearchEngine.countingSearch(pm, getScope(), pattern);
	}
}

