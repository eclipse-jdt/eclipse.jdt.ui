/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.genericize;

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class GenericizeContainerClientsRefactoring extends Refactoring {

	private TextChangeManager fChangeManager;
	private final IJavaElement[] fElements;
	private GenericizeContainerClientsAnalyzer fAnalyzer;
	
	private GenericizeContainerClientsRefactoring(IJavaElement[] elements) {
		fElements= elements;
	}
	
	public static boolean isAvailable(IJavaElement[] elements) {
		return elements.length > 0;
	}

	public static GenericizeContainerClientsRefactoring create(IJavaElement[] elements) {
		if (isAvailable(elements)) {
			return new GenericizeContainerClientsRefactoring(elements);
		}
		return null;
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("GenericizeContainerClientsRefactoring.name"); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		//TODO: check selection: no binaries
		return new RefactoringStatus();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask(RefactoringCoreMessages.getString("GenericizeContainerClientsRefactoring.checking_preconditions"), 1); //$NON-NLS-1$
		try {
			RefactoringStatus result= check15();
			fAnalyzer= new GenericizeContainerClientsAnalyzer(fElements);
			fAnalyzer.analyzeContainerReferences(pm, result);
			
//			result.merge(performAnalysis(pm));
			
			fChangeManager= new TextChangeManager();
			IFile[] filesToModify= ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
			result.merge(Checks.validateModifiesFiles(filesToModify, null));
			return result;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus check15() {
		RefactoringStatus result= new RefactoringStatus();
		HashMap/*<IJavaProject, Boolean>*/ project15ness= new HashMap/*<IJavaProject, Boolean>*/();
		
		for (int i= 0; i < fElements.length; i++) {
			IJavaProject javaProject= fElements[i].getJavaProject();
			if (! project15ness.containsKey(javaProject)) {
				String sourceLevel= javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
				boolean is15= ! JavaCore.VERSION_1_1.equals(sourceLevel)
						&& ! JavaCore.VERSION_1_2.equals(sourceLevel)
						&& ! JavaCore.VERSION_1_3.equals(sourceLevel)
						&& ! JavaCore.VERSION_1_4.equals(sourceLevel);
				if (! is15) {
					String message= "Changes in project '" + javaProject.getElementName() + "' will not compile until compiler source level is raised to 1.5.";
					result.addError(message);
				}
				project15ness.put(javaProject, Boolean.valueOf(is15));
			}
		}
		return result;
	}

//	/**
//	 * Perform the type inference analysis and collect the results.
//	 * @return RefactoringStatus
//	 */
//	private RefactoringStatus performAnalysis(IProgressMonitor pm) {
//		GenericizeCollectionClientAnalyzer analyzer= new GenericizeCollectionClientAnalyzer(fOwningClass, fAffectedUnits, fConstraintOptimizations, fDebugMessages);
//	
//		analyzer.setModifyPublicMembers(fModifyPublicMembers);
//		analyzer.setInferMethodTypeParms(fInferMethodTypeParms);
//		analyzer.analyze(pm);
//	
//		Set	affectedCUs= analyzer.getAffectedCUs();
//	
//		fAffectedUnits= (ICompilationUnit[]) affectedCUs.toArray(new ICompilationUnit[affectedCUs.size()]);
//	
//		fDeclarationSites= analyzer.computeDeclarationSiteModifications();
//		fAllocationSites= analyzer.computeAllocationSiteModifications();
//		fRemovableCasts= analyzer.computeRemovableCasts();
//	
//		fAnalyzerStatistics= analyzer.getStatistics();
//	
//		return new RefactoringStatus();
//	}

	

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			DynamicValidationStateChange result= new DynamicValidationStateChange(RefactoringCoreMessages.getString("Change.javaChanges"), fChangeManager.getAllChanges()); //$NON-NLS-1$
			fAnalyzer= null; // free memory
			return result;
		} finally{
			pm.done();
			//TODO: clear caches
		}	
	}

}
