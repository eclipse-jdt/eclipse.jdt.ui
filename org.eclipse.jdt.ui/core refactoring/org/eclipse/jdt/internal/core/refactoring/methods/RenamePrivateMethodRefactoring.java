/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.methods;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

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
		pm.beginTask("", 3); //$NON-NLS-1$
		pm.subTask(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.checking")); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkInput(new SubProgressMonitor(pm, 1)));
		pm.subTask(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.analyzing_hierarchy")); //$NON-NLS-1$
		if (hierarchyDeclaresMethodName(new SubProgressMonitor(pm, 1), getMethod(), getNewName()))
			result.addError(RefactoringCoreMessages.getFormattedString("RenamePrivateMethodRefactoring.hierarchy_defines", //$NON-NLS-1$
																		new String[]{getMethod().getDeclaringType().getFullyQualifiedName(), getNewName()}));
		pm.subTask(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.analyzing_cu")); //$NON-NLS-1$
		result.merge(analyzeCompilationUnit(new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}
		
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkPreactivation());
		result.merge(checkAvailability(getMethod()));
		if (! Flags.isPrivate(getMethod().getFlags()))
			result.addFatalError(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.only_private")); //$NON-NLS-1$

		return result;
	}
	
	private RefactoringStatus analyzeCompilationUnit(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1); //$NON-NLS-1$
		List grouped= getOccurrences(pm);
		pm.done();
		Assert.isTrue(grouped.size() <= 1 , RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.assert.references_outside_cu")); //$NON-NLS-1$
		if (grouped.isEmpty())
			return null;
		List searchResults= (List)grouped.get(0);
		Assert.isTrue(searchResults.size() > 0, RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.assert.nothing_found")); //$NON-NLS-1$
		return new RenameMethodASTAnalyzer(getNewName(), getMethod()).analyze(searchResults, getMethod().getCompilationUnit());
	}
	
	/* non java-doc
	 * overriding IRefactoring#createChange
	 */
	void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		List grouped= (List)getOccurrences(null);
		ITextBufferChange change= getChangeCreator().create(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.rename_method"), getMethod().getCompilationUnit()); //$NON-NLS-1$
		if (! grouped.isEmpty()){
			List l= (List)grouped.get(0);
			for (Iterator subIter= l.iterator(); subIter.hasNext();){
				change.addSimpleTextChange(createTextChange((SearchResult)subIter.next()));
			}
		}	
		//there can be only 1 affected resource - the cu that declares the renamed method
		change.addReplace(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.declaration_change"), getMethod().getNameRange().getOffset(), getMethod().getNameRange().getLength(), getNewName()); //$NON-NLS-1$
		builder.addChange(change);
		pm.worked(1);
		setOccurrences(null); //to prevent memory leak
	}
	
	/* non java-doc
	 * overriding IRefactoring#createChange
	 */
	/*package*/ ISearchPattern createSearchPattern(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1); //$NON-NLS-1$
		ISearchPattern pattern= SearchEngine.createSearchPattern(getMethod(), IJavaSearchConstants.REFERENCES);
		pm.done();
		return pattern;
	}
}