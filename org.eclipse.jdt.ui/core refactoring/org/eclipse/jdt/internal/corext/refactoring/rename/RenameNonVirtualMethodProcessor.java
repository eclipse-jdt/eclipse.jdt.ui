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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class RenameNonVirtualMethodProcessor extends RenameMethodProcessor {
	
	public boolean isAvailable() throws CoreException {
		return super.isAvailable() && !MethodChecks.isVirtual(getMethod());
	}
	
	//----------- preconditions --------------
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(super.checkInput(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;
			
			IMethod hierarchyMethod= hierarchyDeclaresMethodName(
				new SubProgressMonitor(pm, 1), getMethod(), getNewElementName());
			
			if (hierarchyMethod != null){
				Context context= JavaStatusContext.create(hierarchyMethod);
				String message= RefactoringCoreMessages.getFormattedString(
					"RenamePrivateMethodRefactoring.hierarchy_defines", //$NON-NLS-1$
					new String[]{JavaModelUtil.getFullyQualifiedName(
						getMethod().getDeclaringType()), getNewElementName()});
                result.addError(message, context);
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
	
	/* non java-doc
	 * overriding RenameMethodrefactoring@addOccurrences
	 */
	void addOccurrences(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getMethod().getCompilationUnit());
		addReferenceUpdates(manager);
		addDeclarationUpdate(manager.get(cu));
		pm.worked(1);
	}
	
	/* non java-doc
	 * overriding RenameMethodrefactoring@createSearchPattern
	 */
	ISearchPattern createSearchPattern(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		ISearchPattern pattern= SearchEngine.createSearchPattern(getMethod(), IJavaSearchConstants.REFERENCES);
		pm.done();
		return pattern;
	}
	
	private void addReferenceUpdates(TextChangeManager manager) throws CoreException {
		SearchResultGroup[] grouped= getOccurrences();
		for (int i= 0; i < grouped.length; i++) {
			SearchResultGroup group= grouped[i];
			SearchResult[] results= group.getSearchResults();
			ICompilationUnit cu= group.getCompilationUnit();
			TextChange change= manager.get(cu);
			for (int j= 0; j < results.length; j++){
				String editName= RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.update"); //$NON-NLS-1$
				change.addTextEdit(editName , createTextChange(results[j]));
			}
		}	
	}
}
