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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

class RenamePrivateMethodRefactoring extends RenameMethodRefactoring {
	
	private RenamePrivateMethodRefactoring(IMethod method) {
		super(method);
	}
	
	public static RenameMethodRefactoring create(IMethod method) throws JavaModelException{
		RenamePrivateMethodRefactoring ref= new RenamePrivateMethodRefactoring(method);
		if (ref.checkPreactivation().hasFatalError())
			return null;
		return ref;
	}
	
	//----------- preconditions --------------
	
	/* non java-doc
	 * @see IPreactivatedRefactoring@checkPreactivation
	 */
	RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkPreactivation());
		result.merge(Checks.checkAvailability(getMethod()));
		if (! JdtFlags.isPrivate(getMethod()))
			result.addFatalError(RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.only_private")); //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(super.checkInput(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;
			
			IMethod hierarchyMethod= hierarchyDeclaresMethodName(new SubProgressMonitor(pm, 1), getMethod(), getNewName());
			
			if (hierarchyMethod != null){
				Context context= JavaSourceContext.create(hierarchyMethod);
				String message= RefactoringCoreMessages.getFormattedString("RenamePrivateMethodRefactoring.hierarchy_defines", //$NON-NLS-1$
																			new String[]{JavaModelUtil.getFullyQualifiedName(getMethod().getDeclaringType()), getNewName()});
                result.addError(message, context);
			}																
			return result;
		} finally{
			pm.done();
		}
	}
	
	/* non java-doc
	 * overriding RenameMethodrefactoring@addOccurrences
	 */
	void addOccurrences(TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", 1); //$NON-NLS-1$
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
			String editName= RefactoringCoreMessages.getString("RenamePrivateMethodRefactoring.update"); //$NON-NLS-1$
			change.addTextEdit(editName , createTextChange(results[i]));
		}
	}
}
