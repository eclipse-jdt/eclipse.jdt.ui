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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;

public class RenameNonVirtualMethodProcessor extends RenameMethodProcessor {
	
	public RenameNonVirtualMethodProcessor(IMethod method) {
		super(method);
	}
	
	public boolean isApplicable() throws CoreException {
		return super.isApplicable() && !MethodChecks.isVirtual(getMethod());
	}
	
	//----------- preconditions --------------
	
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext checkContext) throws CoreException {
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(super.checkFinalConditions(new SubProgressMonitor(pm, 1), checkContext));
			if (result.hasFatalError())
				return result;
			
			IMethod[] hierarchyMethods= hierarchyDeclaresMethodName(
				new SubProgressMonitor(pm, 1), getMethod(), getNewElementName());
			
			for (int i= 0; i < hierarchyMethods.length; i++) {
				IMethod hierarchyMethod= hierarchyMethods[i];
				RefactoringStatusContext context= JavaStatusContext.create(hierarchyMethod);
				if (Checks.compareParamTypes(getMethod().getParameterTypes(), hierarchyMethod.getParameterTypes())) {
					String message= RefactoringCoreMessages.getFormattedString(
						"RenamePrivateMethodRefactoring.hierarchy_defines", //$NON-NLS-1$
						new String[]{JavaModelUtil.getFullyQualifiedName(
							getMethod().getDeclaringType()), getNewElementName()});
					result.addError(message, context);				
				}else {
					String message= RefactoringCoreMessages.getFormattedString(
						"RenamePrivateMethodRefactoring.hierarchy_defines2", //$NON-NLS-1$
						new String[]{JavaModelUtil.getFullyQualifiedName(
							getMethod().getDeclaringType()), getNewElementName()});
					result.addWarning(message, context);				
				}
			}
			return result;
		} finally{
			pm.done();
		}
	}
	
	/*
	 * The code below is needed to due bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=39700.
	 * Declaration in hierarchy doesn't take visibility into account. 
	 */

	/*
	 * XXX working around bug 39700
	 */
	protected SearchResultGroup[] getOccurrences(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2);	 //$NON-NLS-1$
		SearchPattern pattern= createReferenceSearchPattern();
		SearchResultGroup[] groups= RefactoringSearchEngine.search(pattern, createRefactoringScope(),
			new MethodOccurenceCollector(getMethod().getElementName()), new SubProgressMonitor(pm, 1));
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			ICompilationUnit cu= group.getCompilationUnit();
			if (cu.equals(getDeclaringCU())) {
				IResource resource= group.getResource();
				int start= getMethod().getNameRange().getOffset();
				int length= getMethod().getNameRange().getLength();
				SearchMatch declarationResult= new SearchMatch(getMethod(), SearchMatch.A_ACCURATE, start, length, SearchEngine.getDefaultSearchParticipant(), resource);
				group.add(declarationResult);
				break;//no need to go further
			}	
		}
		return groups;	
	}
		
	/* non java-doc
	 * overriding RenameMethodrefactoring@addOccurrences
	 */
	void addOccurrences(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		addReferenceUpdates(manager, pm);
		addDeclarationUpdate(manager.get(getDeclaringCU()));
		pm.worked(1);
	}
	
	private ICompilationUnit getDeclaringCU() {
		return getMethod().getCompilationUnit();
	}

	/* non java-doc
	 * overriding RenameMethodrefactoring@createSearchPattern
	 */
	SearchPattern createOccurrenceSearchPattern(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		SearchPattern pattern= SearchPattern.createPattern(getMethod(), IJavaSearchConstants.ALL_OCCURRENCES);
		pm.done();
		return pattern;
	}

	private SearchPattern createReferenceSearchPattern() {
		return SearchPattern.createPattern(getMethod(), IJavaSearchConstants.REFERENCES);
	}
	
	private void addReferenceUpdates(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		SearchResultGroup[] grouped= getReferences(pm);
		for (int i= 0; i < grouped.length; i++) {
			SearchResultGroup group= grouped[i];
			SearchMatch[] results= group.getSearchResults();
			ICompilationUnit cu= group.getCompilationUnit();
			TextChange change= manager.get(cu);
			for (int j= 0; j < results.length; j++){
				String editName= RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.update"); //$NON-NLS-1$
				TextChangeCompatibility.addTextEdit(change, editName, createTextChange(results[j]));
			}
		}	
	}

	private SearchResultGroup[] getReferences(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2);	 //$NON-NLS-1$
		SearchPattern pattern= createReferenceSearchPattern();
		return RefactoringSearchEngine.search(pattern, createRefactoringScope(),
			new MethodOccurenceCollector(getMethod().getElementName()), new SubProgressMonitor(pm, 1));	
	}
}
