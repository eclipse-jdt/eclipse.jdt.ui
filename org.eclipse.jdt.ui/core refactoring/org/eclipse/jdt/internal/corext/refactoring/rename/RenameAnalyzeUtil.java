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

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.WorkingCopyOwner;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;

class RenameAnalyzeUtil {
	
	private RenameAnalyzeUtil() {
		//no instance
	}
	
	static RefactoringStatus analyzeRenameChanges(TextChangeManager manager,  SearchResultGroup[] oldOccurrences, SearchResultGroup[] newOccurrences) {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < oldOccurrences.length; i++) {
			SearchResultGroup oldGroup= oldOccurrences[i];
			SearchResult[] oldSearchResults= oldGroup.getSearchResults();
			ICompilationUnit cunit= oldGroup.getCompilationUnit();
			if (cunit == null)
				continue;
			for (int j= 0; j < oldSearchResults.length; j++) {
				SearchResult oldSearchResult= oldSearchResults[j];
				if (! RenameAnalyzeUtil.existsInNewOccurrences(oldSearchResult, newOccurrences, manager)){
					ISourceRange range= new SourceRange(oldSearchResult.getStart(), oldSearchResult.getEnd() - oldSearchResult.getStart());
					RefactoringStatusContext context= JavaStatusContext.create(cunit, range); //XXX
					String message= RefactoringCoreMessages.getFormattedString("RenameAnalyzeUtil.shadows", cunit.getElementName());	//$NON-NLS-1$
					result.addError(message , context);
				}	
			}
		}
		return result;
	}

	static ICompilationUnit findWorkingCopyForCu(ICompilationUnit[] newWorkingCopies, ICompilationUnit cu){
		ICompilationUnit originalDeclaringCu= WorkingCopyUtil.getOriginal(cu);
		for (int i= 0; i < newWorkingCopies.length; i++) {
			if (newWorkingCopies[i].getPrimary().equals(originalDeclaringCu))
				return newWorkingCopies[i];
		}
		return null;
	}

	/** @deprecated TODO: use WorkingCopyOwner in RenameFieldProcessor */
	static ICompilationUnit[] getNewWorkingCopies(ICompilationUnit[] compilationUnitsToModify, TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", compilationUnitsToModify.length); //$NON-NLS-1$
		ICompilationUnit[] newWorkingCopies= new ICompilationUnit[compilationUnitsToModify.length];
		for (int i= 0; i < compilationUnitsToModify.length; i++) {
			ICompilationUnit cu= compilationUnitsToModify[i];
			newWorkingCopies[i]= WorkingCopyUtil.getNewWorkingCopy(cu);
			String previewContent= manager.get(cu).getPreviewContent();
			newWorkingCopies[i].getBuffer().setContents(previewContent);
			newWorkingCopies[i].reconcile(ICompilationUnit.NO_AST, false, null, new SubProgressMonitor(pm, 1));
		}
		return newWorkingCopies;
	}
	

	static ICompilationUnit[] createNewWorkingCopies(ICompilationUnit[] compilationUnitsToModify, TextChangeManager manager, WorkingCopyOwner owner, SubProgressMonitor pm) throws CoreException {
		pm.beginTask("", compilationUnitsToModify.length); //$NON-NLS-1$
		ICompilationUnit[] newWorkingCopies= new ICompilationUnit[compilationUnitsToModify.length];
		for (int i= 0; i < compilationUnitsToModify.length; i++) {
			ICompilationUnit cu= compilationUnitsToModify[i];
			newWorkingCopies[i]= cu.getWorkingCopy(owner, null, null);
			String previewContent= manager.get(cu).getPreviewContent();
			newWorkingCopies[i].getBuffer().setContents(previewContent);
			newWorkingCopies[i].reconcile(ICompilationUnit.NO_AST, false, owner, new SubProgressMonitor(pm, 1));
		}
		pm.done();
		return newWorkingCopies;
	}
	
	private static boolean existsInNewOccurrences(SearchResult searchResult, SearchResultGroup[] newOccurrences, TextChangeManager manager) {
		SearchResultGroup newGroup= findOccurrenceGroup(searchResult.getResource(), newOccurrences);
		if (newGroup == null)
			return false;
		
		IRegion oldEditRange= getCorrespondingEditChangeRange(searchResult, manager);
		if (oldEditRange == null)
			return false;
		
		SearchResult[] newSearchResults= newGroup.getSearchResults();
		int oldRangeOffset = oldEditRange.getOffset();
		for (int i= 0; i < newSearchResults.length; i++) {
			if (newSearchResults[i].getStart() == oldRangeOffset)
				return true;
		}
		return false;
	}
	
	private static IRegion getCorrespondingEditChangeRange(SearchResult searchResult, TextChangeManager manager) {
		TextChange change= getTextChange(searchResult, manager);
		if (change == null)
			return null;
		
		IRegion oldMatchRange= createTextRange(searchResult);
		TextEditChangeGroup[] editChanges= change.getTextEditChangeGroups();	
		for (int i= 0; i < editChanges.length; i++) {
			if (oldMatchRange.equals(editChanges[i].getRegion()))
				return TextEdit.getCoverage(change.getPreviewEdits(editChanges[i].getTextEdits()));
		}
		return null;
	}
	
	private static TextChange getTextChange(SearchResult searchResult, TextChangeManager manager) {
		ICompilationUnit cu= searchResult.getCompilationUnit();
		if (cu == null)
			return null;
		return manager.get(cu);
	}
	
	private static IRegion createTextRange(SearchResult searchResult) {
		int start= searchResult.getStart();
		return new Region(start, searchResult.getEnd() - start);
	}
	
	private static SearchResultGroup findOccurrenceGroup(IResource resource, SearchResultGroup[] newOccurrences) {
		for (int i= 0; i < newOccurrences.length; i++) {
			if (newOccurrences[i].getResource().equals(resource))
				return newOccurrences[i];
		}
		return null;
	}
}
