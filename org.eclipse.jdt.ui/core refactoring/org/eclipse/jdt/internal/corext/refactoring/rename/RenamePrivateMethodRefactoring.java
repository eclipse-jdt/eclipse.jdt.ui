/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

class RenamePrivateMethodRefactoring extends RenameMethodRefactoring {
	
	RenamePrivateMethodRefactoring(IMethod method) {
		super(method);
	}
	
	//----------- preconditions --------------
	
	/* non java-doc
	 * @see IPreactivatedRefactoring@checkPreactivation
	 */
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkPreactivation());
		result.merge(checkAvailability(getMethod()));
		if (! JdtFlags.isPrivate(getMethod()))
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
			if (result.hasFatalError())
				return result;
			
			pm.subTask(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.analyzing_hierarchy")); //$NON-NLS-1$
			if (hierarchyDeclaresMethodName(new SubProgressMonitor(pm, 1), getMethod(), getNewName()))
				result.addError(RefactoringCoreMessages.getFormattedString("RenamePrivateMethodRefactoring.hierarchy_defines", //$NON-NLS-1$
																			new String[]{JavaModelUtil.getFullyQualifiedName(getMethod().getDeclaringType()), getNewName()}));
			pm.subTask(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.analyzing_cu")); //$NON-NLS-1$
			result.merge(analyzeCompilationUnit(new SubProgressMonitor(pm, 1)));
			return result;
		} finally{
			pm.done();
		}
	}
	
	private RefactoringStatus analyzeCompilationUnit(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1); //$NON-NLS-1$
		SearchResultGroup[] grouped= getOccurrences();
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
	void addOccurrences(TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", 1);
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getMethod().getCompilationUnit());
		addReferenceUpdates(manager.get(cu));
		addDeclarationUpdate(manager.get(cu));
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
	
	private void addReferenceUpdates(TextChange change) throws JavaModelException{
		SearchResultGroup[] grouped= getOccurrences();
		if (grouped.length == 0)
			return;
		SearchResult[] results= grouped[0].getSearchResults();
		for (int i= 0; i < results.length; i++){
			String editName= "Update method reference";
			change.addTextEdit(editName , createTextChange(results[i]));
		}
	}
}