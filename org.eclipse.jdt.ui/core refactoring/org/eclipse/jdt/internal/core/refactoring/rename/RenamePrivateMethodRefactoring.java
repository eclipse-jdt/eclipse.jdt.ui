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
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;

class RenamePrivateMethodRefactoring extends RenameMethodRefactoring {
	
	RenamePrivateMethodRefactoring(ITextBufferChangeCreator changeCreator, IMethod method) {
		super(changeCreator, method);
	}
	
	//----------- preconditions --------------
	
	/* non java-doc
	 * @see IPreactivatedRefactoring@checkPreactivation
	 */
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkPreactivation());
		result.merge(checkAvailability(getMethod()));
		if (! Flags.isPrivate(getMethod().getFlags()))
			result.addFatalError(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.only_private")); //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		try{
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
			return result;
		} finally{
			pm.done();
		}
	}
	
	private RefactoringStatus analyzeCompilationUnit(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1); //$NON-NLS-1$
		SearchResultGroup[] grouped= getOccurrences(pm);
		pm.done();
		Assert.isTrue(grouped.length <= 1 , RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.assert.references_outside_cu")); //$NON-NLS-1$
		if (grouped.length == 0)
			return null;
		SearchResult[] searchResults= grouped[0].getSearchResults();
		Assert.isTrue(searchResults.length > 0, RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.assert.nothing_found")); //$NON-NLS-1$
		return new RenameMethodASTAnalyzer(getNewName(), getMethod()).analyze(searchResults, getMethod().getCompilationUnit());
	}
	
	/* non java-doc
	 * overriding RenameMethodrefactoring@addOccurrences
	 */
	void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		ITextBufferChange change= getChangeCreator().create(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.rename_method"), getMethod().getCompilationUnit()); //$NON-NLS-1$
		
		if (getUpdateReferences())
			addReferenceUpdates(change);
			
		//there can be only 1 affected resource - the cu that declares the renamed method
		//change.addReplace(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.declaration_change"), getMethod().getNameRange().getOffset(), getMethod().getNameRange().getLength(), getNewName()); //$NON-NLS-1$
		addDeclarationUpdate(change);
		builder.addChange(change);
		pm.worked(1);
	}
	
	/* non java-doc
	 * overriding RenameMethodrefactoring@createSearchPattern
	 */
	ISearchPattern createSearchPattern(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1); //$NON-NLS-1$
		ISearchPattern pattern= SearchEngine.createSearchPattern(getMethod(), IJavaSearchConstants.REFERENCES);
		pm.done();
		return pattern;
	}
	
	private void addReferenceUpdates(ITextBufferChange change) throws JavaModelException{
		SearchResultGroup[] grouped= getOccurrences(null);
			if (grouped.length != 0){
				SearchResult[] results= grouped[0].getSearchResults();
				for (int i= 0; i < results.length; i++){
					change.addSimpleTextChange(createTextChange(results[i]));
				}
		}	
	}
}