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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.search.FieldDeclarationMatch;
import org.eclipse.jdt.core.search.MethodDeclarationMatch;
import org.eclipse.jdt.core.search.SearchMatch;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.SearchUtils;
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
			SearchMatch[] oldSearchResults= oldGroup.getSearchResults();
			ICompilationUnit cunit= oldGroup.getCompilationUnit();
			if (cunit == null)
				continue;
			for (int j= 0; j < oldSearchResults.length; j++) {
				SearchMatch oldSearchResult= oldSearchResults[j];
				if (! RenameAnalyzeUtil.existsInNewOccurrences(oldSearchResult, newOccurrences, manager)){
					addShadowsError(cunit, oldSearchResult, result);
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
			String previewContent= manager.get(cu).getPreviewContent(new NullProgressMonitor());
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
			newWorkingCopies[i]= createNewWorkingCopy(cu, manager, owner, new SubProgressMonitor(pm, 1));
		}
		pm.done();
		return newWorkingCopies;
	}
	
	static ICompilationUnit createNewWorkingCopy(ICompilationUnit cu, TextChangeManager manager,
			WorkingCopyOwner owner, SubProgressMonitor pm) throws CoreException {
		ICompilationUnit newWc= cu.getWorkingCopy(owner, null, null);
		String previewContent= manager.get(cu).getPreviewContent(new NullProgressMonitor());
		newWc.getBuffer().setContents(previewContent);
		newWc.reconcile(ICompilationUnit.NO_AST, false, owner, pm);
		return newWc;
	}
	
	private static boolean existsInNewOccurrences(SearchMatch searchResult, SearchResultGroup[] newOccurrences, TextChangeManager manager) {
		SearchResultGroup newGroup= findOccurrenceGroup(searchResult.getResource(), newOccurrences);
		if (newGroup == null)
			return false;
		
		IRegion oldEditRange= getCorrespondingEditChangeRange(searchResult, manager);
		if (oldEditRange == null)
			return false;
		
		SearchMatch[] newSearchResults= newGroup.getSearchResults();
		int oldRangeOffset = oldEditRange.getOffset();
		for (int i= 0; i < newSearchResults.length; i++) {
			if (newSearchResults[i].getOffset() == oldRangeOffset)
				return true;
		}
		return false;
	}
	
	private static IRegion getCorrespondingEditChangeRange(SearchMatch searchResult, TextChangeManager manager) {
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
	
	private static TextChange getTextChange(SearchMatch searchResult, TextChangeManager manager) {
		ICompilationUnit cu= SearchUtils.getCompilationUnit(searchResult);
		if (cu == null)
			return null;
		return manager.get(cu);
	}
	
	private static IRegion createTextRange(SearchMatch searchResult) {
		return new Region(searchResult.getOffset(), searchResult.getLength());
	}
	
	private static SearchResultGroup findOccurrenceGroup(IResource resource, SearchResultGroup[] newOccurrences) {
		for (int i= 0; i < newOccurrences.length; i++) {
			if (newOccurrences[i].getResource().equals(resource))
				return newOccurrences[i];
		}
		return null;
	}
	
//--- find missing changes in BOTH directions
	
	//TODO: Currently filters out declarations (MethodDeclarationMatch, FieldDeclarationMatch).
	//Long term solution: only pass reference search results in.
	static RefactoringStatus analyzeRenameChanges2(TextChangeManager manager,
			SearchResultGroup[] oldReferences, SearchResultGroup[] newReferences, String newElementName) {
		RefactoringStatus result= new RefactoringStatus();
		
		HashMap cuToNewResults= new HashMap(newReferences.length);
		for (int i1= 0; i1 < newReferences.length; i1++) {
			ICompilationUnit cu= newReferences[i1].getCompilationUnit();
			if (cu != null)
				cuToNewResults.put(cu.getPrimary(), newReferences[i1].getSearchResults());
		}
		
		for (int i= 0; i < oldReferences.length; i++) {
			SearchResultGroup oldGroup= oldReferences[i];
			SearchMatch[] oldMatches= oldGroup.getSearchResults();
			ICompilationUnit cu= oldGroup.getCompilationUnit();
			if (cu == null)
				continue;
			
			SearchMatch[] newSearchMatches= (SearchMatch[]) cuToNewResults.remove(cu);
			if (newSearchMatches == null) {
				for (int j = 0; j < oldMatches.length; j++) {
					SearchMatch oldMatch = oldMatches[j];
					addShadowsError(cu, oldMatch, result);
				}
			} else {
				analyzeChanges(cu, manager.get(cu), oldMatches, newSearchMatches, newElementName, result);
			}
		}
		
		for (Iterator iter= cuToNewResults.keySet().iterator(); iter.hasNext();) {
			ICompilationUnit cu= (ICompilationUnit) iter.next();
			SearchMatch[] newSearchMatches= (SearchMatch[]) cuToNewResults.remove(cu);
			for (int i= 0; i < newSearchMatches.length; i++) {
				SearchMatch newMatch= newSearchMatches[i];
				addReferenceShadowedError(cu, newMatch, newElementName, result);
			}
		}
		return result;
	}

	private static void analyzeChanges(ICompilationUnit cu, TextChange change,
			SearchMatch[] oldMatches, SearchMatch[] newMatches, String newElementName, RefactoringStatus result) {
		Map updatedOldOffsets= getUpdatedChangeOffsets(change, oldMatches);
		for (int i= 0; i < newMatches.length; i++) {
			SearchMatch newMatch= newMatches[i];
			Integer offsetInNew= new Integer(newMatch.getOffset());
			SearchMatch oldMatch= (SearchMatch) updatedOldOffsets.remove(offsetInNew);
			if (oldMatch == null) {
				addReferenceShadowedError(cu, newMatch, newElementName, result);
			}
		}
		for (Iterator iter= updatedOldOffsets.values().iterator(); iter.hasNext();) {
			// remaining old matches are not found any more -> they have been shadowed
			SearchMatch oldMatch= (SearchMatch) iter.next();
			addShadowsError(cu, oldMatch, result);
		}
	}
	
	/** @return Map &lt;Integer updatedOffset, SearchMatch oldMatch&gt; */
	private static Map getUpdatedChangeOffsets(TextChange change, SearchMatch[] oldMatches) {
		Map/*<Integer updatedOffset, SearchMatch oldMatch>*/ updatedOffsets= new HashMap();
		Map oldToUpdatedOffsets= getEditChangeOffsetUpdates(change);
		for (int i= 0; i < oldMatches.length; i++) {
			SearchMatch oldMatch= oldMatches[i];
			Integer updatedOffset= (Integer) oldToUpdatedOffsets.get(new Integer(oldMatch.getOffset()));
			if (updatedOffset == null)
				updatedOffset= new Integer(-1); //match not updated
			updatedOffsets.put(updatedOffset, oldMatch);
		}
		return updatedOffsets;
	}

	/** @return Map &lt;Integer oldOffset, Integer updatedOffset&gt; */
	private static Map getEditChangeOffsetUpdates(TextChange change) {
		TextEditChangeGroup[] editChanges= change.getTextEditChangeGroups();
		Map/*<oldOffset, newOffset>*/ offsetUpdates= new HashMap(editChanges.length);
		for (int i= 0; i < editChanges.length; i++) {
			TextEditChangeGroup editChange= editChanges[i];
			IRegion oldRegion= editChange.getRegion();
			if (oldRegion == null)
				continue;
			IRegion updatedRegion= TextEdit.getCoverage(change.getPreviewEdits(editChange.getTextEdits()));
			if (updatedRegion == null)
				continue;
			
			offsetUpdates.put(new Integer(oldRegion.getOffset()), new Integer(updatedRegion.getOffset()));
		}
		return offsetUpdates;
	}

	private static void addReferenceShadowedError(ICompilationUnit cu, SearchMatch newMatch, String newElementName, RefactoringStatus result) {
		//Found a new match with no corresponding old match.
		//-> The new match is a reference which was pointing to another element,
		//but that other element has been shadowed
		
		//TODO: should not have to filter declarations:
		if (newMatch instanceof MethodDeclarationMatch || newMatch instanceof FieldDeclarationMatch)
			return;
		ISourceRange range= getOldSourceRange(newMatch);
		RefactoringStatusContext context= JavaStatusContext.create(cu, range);
		String message= RefactoringCoreMessages.getFormattedString(
				"RenameAnalyzeUtil.reference_shadowed", //$NON-NLS-1$
				new String[] {cu.getElementName(), newElementName});
		result.addError(message, context);
	}

	private static ISourceRange getOldSourceRange(SearchMatch newMatch) {
		// cannot transfom offset in preview to offset in original -> just show enclosing method
		IJavaElement newMatchElement= (IJavaElement) newMatch.getElement();
		IJavaElement primaryElement= newMatchElement.getPrimaryElement();
		ISourceRange range= null;
		if (primaryElement.exists() && primaryElement instanceof ISourceReference) {
			try {
				range= ((ISourceReference) primaryElement).getSourceRange();
			} catch (JavaModelException e) {
				// can live without source range
			}
		}
		return range;
	}

	private static void addShadowsError(ICompilationUnit cu, SearchMatch oldMatch, RefactoringStatus result) {
		// Old match not found in new matches -> reference has been shadowed
		
		//TODO: should not have to filter declarations:
		if (oldMatch instanceof MethodDeclarationMatch || oldMatch instanceof FieldDeclarationMatch)
			return;
		ISourceRange range= new SourceRange(oldMatch.getOffset(), oldMatch.getLength());
		RefactoringStatusContext context= JavaStatusContext.create(cu, range);
		String message= RefactoringCoreMessages.getFormattedString("RenameAnalyzeUtil.shadows", cu.getElementName()); //$NON-NLS-1$
		result.addError(message, context);
	}
}
