/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.methods;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenamePrivateMethodRefactoring extends RenameMethodRefactoring {

	/* non java-doc
	 * this constructor is only for consistency - should be replaced by:
	 * RenamePrivateMethodRefactoring(ITextBufferChangeCreator, IMethod, String)
	 */ 
	public RenamePrivateMethodRefactoring(ITextBufferChangeCreator changeCreator, IJavaSearchScope scope, IMethod method, String newName){
		super(changeCreator, scope, method, newName);
		correctScope();
	}
	
	public RenamePrivateMethodRefactoring(ITextBufferChangeCreator changeCreator, IMethod method) {
		super(changeCreator, method);
		correctScope();
	}
	
	/* non java-doc
	 * narrow down the scope
	 */ 
	private void correctScope(){
		if (getMethod().isBinary())
			return;
		try{
			//only the declaring compilation unit
			setScope(SearchEngine.createJavaSearchScope(new IResource[]{getResource(getMethod())}));
		} catch (JavaModelException e){
			//do nothing
		}
	}
	
	//----------- Conditions --------------
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 3);
		pm.subTask("checking preconditions");
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkInput(new SubProgressMonitor(pm, 1)));
		pm.subTask("analyzing hierarchy");
		if (hierarchyDeclaresMethodName(new SubProgressMonitor(pm, 1), getMethod(), getNewName()))
			result.addError(getMethod().getDeclaringType().getFullyQualifiedName() + " or a type in its hierarchy defines a method named " + getNewName());	
		pm.subTask("analyzing compilation unit");
		result.merge(analyzeCompilationUnit(new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}
		
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkActivation(pm));
		result.merge(checkAvailability(getMethod()));
		if (! Flags.isPrivate(getMethod().getFlags()))
			result.addFatalError("Only applicable to private methods");

		HackFinder.fixMeSoon("remove this constraint");	
		if (Flags.isNative(getMethod().getFlags()))
			result.addFatalError("Not applicable to native methods");

		if (Flags.isStatic(getMethod().getFlags()))
			result.addFatalError("Not applicable to static methods");	
		return result;
	}
	
	private RefactoringStatus analyzeCompilationUnit(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1);
		List grouped= getOccurrences(pm);
		pm.done();
		Assert.isTrue(grouped.size() <= 1 , "references to a private method found outside of its compilation unit");
		if (grouped.isEmpty())
			return null;
		List searchResults= (List)grouped.get(0);
		Assert.isTrue(searchResults.size() > 0, "no declarations/references to a method found");
		return new RenameMethodASTAnalyzer(getNewName(), getMethod()).analyze(searchResults, getMethod().getCompilationUnit());
	}
	
	/* non java-doc
	 * overriding IRefactoring#createChange
	 */
	void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		List grouped= (List)getOccurrences(null);
		ITextBufferChange change= getChangeCreator().create("Rename Method", getMethod().getCompilationUnit());
		if (! grouped.isEmpty()){
			List l= (List)grouped.get(0);
			for (Iterator subIter= l.iterator(); subIter.hasNext();){
				change.addSimpleTextChange(createTextChange((SearchResult)subIter.next()));
			}
		}	
		//there can be only 1 affected resource - the cu that declares the renamed method
		change.addReplace("Method declaration change", getMethod().getNameRange().getOffset(), getMethod().getNameRange().getLength(), getNewName());
		builder.addChange(change);
		pm.worked(1);
		HackFinder.fixMeSoon("maybe add dispose() method?");
		setOccurrences(null); //to prevent memory leak
	}
	
	/* non java-doc
	 * overriding IRefactoring#createChange
	 */
	/*package*/ ISearchPattern createSearchPattern(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1);
		ISearchPattern pattern= SearchEngine.createSearchPattern(getMethod(), IJavaSearchConstants.REFERENCES);
		pm.done();
		return pattern;
	}
}