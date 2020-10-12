/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.search.FieldDeclarationMatch;
import org.eclipse.jdt.core.search.MethodDeclarationMatch;
import org.eclipse.jdt.core.search.SearchMatch;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.SourceRangeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

public class RenameAnalyzeUtil {

	private static class ProblemNodeFinder {

		private ProblemNodeFinder() {
			//static
		}

		public static SimpleName[] getProblemNodes(ASTNode methodNode, VariableDeclaration variableNode, TextEdit[] edits, TextChange change) {
			String key= variableNode.resolveBinding().getKey();
			NameNodeVisitor visitor= new NameNodeVisitor(edits, change, key);
			methodNode.accept(visitor);
			return visitor.getProblemNodes();
		}

		private static class NameNodeVisitor extends ASTVisitor {

			private Collection<IRegion> fRanges;
			private Collection<SimpleName> fProblemNodes;
			private String fKey;

			public NameNodeVisitor(TextEdit[] edits, TextChange change, String key) {
				Assert.isNotNull(edits);
				Assert.isNotNull(key);

				fRanges= new HashSet<>(Arrays.asList(RefactoringAnalyzeUtil.getNewRanges(edits, change)));
				fProblemNodes= new ArrayList<>(0);
				fKey= key;
			}

			public SimpleName[] getProblemNodes() {
				return fProblemNodes.toArray(new SimpleName[fProblemNodes.size()]);
			}

			//----- visit methods

			@Override
			public boolean visit(SimpleName node) {
				VariableDeclaration decl= getVariableDeclaration(node);
				if (decl == null)
					return super.visit(node);

				IVariableBinding binding= decl.resolveBinding();
				if (binding == null)
					return super.visit(node);

				boolean keysEqual= fKey.equals(binding.getKey());
				boolean rangeInSet= fRanges.contains(new Region(node.getStartPosition(), node.getLength()));

				if (keysEqual && !rangeInSet)
					fProblemNodes.add(node);

				if (!keysEqual && rangeInSet)
					fProblemNodes.add(node);

				/*
				 * if (!keyEquals && !rangeInSet)
				 * 		ok, different local variable.
				 *
				 * if (keyEquals && rangeInSet)
				 * 		ok, renamed local variable & has been renamed.
				 */

				return super.visit(node);
			}
		}
	}

	public static class LocalAnalyzePackage {
		public final TextEdit fDeclarationEdit;
		public final TextEdit[] fOccurenceEdits;

		public LocalAnalyzePackage(final TextEdit declarationEdit, final TextEdit[] occurenceEdits) {
			fDeclarationEdit = declarationEdit;
			fOccurenceEdits = occurenceEdits;
		}
	}

	private RenameAnalyzeUtil() {
		//no instance
	}

	public static RefactoringStatus analyzeRenameChanges(TextChangeManager manager,  SearchResultGroup[] oldOccurrences, SearchResultGroup[] newOccurrences) {
		RefactoringStatus result= new RefactoringStatus();
		for (SearchResultGroup oldGroup : oldOccurrences) {
			SearchMatch[] oldSearchResults= oldGroup.getSearchResults();
			ICompilationUnit cunit= oldGroup.getCompilationUnit();
			if (cunit == null)
				continue;
			for (SearchMatch oldSearchResult : oldSearchResults) {
				if (! RenameAnalyzeUtil.existsInNewOccurrences(oldSearchResult, newOccurrences, manager)){
					addShadowsError(cunit, oldSearchResult, result);
				}
			}
		}
		return result;
	}

	public static ICompilationUnit findWorkingCopyForCu(ICompilationUnit[] newWorkingCopies, ICompilationUnit cu){
		ICompilationUnit original= cu == null ? null : cu.getPrimary();
		for (ICompilationUnit newWorkingCopy : newWorkingCopies) {
			if (newWorkingCopy.getPrimary().equals(original))
				return newWorkingCopy;
		}
		return null;
	}

	public static ICompilationUnit[] createNewWorkingCopies(ICompilationUnit[] compilationUnitsToModify, TextChangeManager manager, WorkingCopyOwner owner, SubProgressMonitor pm) throws CoreException {
		pm.beginTask("", compilationUnitsToModify.length); //$NON-NLS-1$
		ICompilationUnit[] newWorkingCopies= new ICompilationUnit[compilationUnitsToModify.length];
		for (int i= 0; i < compilationUnitsToModify.length; i++) {
			ICompilationUnit cu= compilationUnitsToModify[i];
			newWorkingCopies[i]= createNewWorkingCopy(cu, manager, owner, new SubProgressMonitor(pm, 1));
		}
		pm.done();
		return newWorkingCopies;
	}

	public static ICompilationUnit createNewWorkingCopy(ICompilationUnit cu, TextChangeManager manager,
			WorkingCopyOwner owner, SubProgressMonitor pm) throws CoreException {
		ICompilationUnit newWc= cu.getWorkingCopy(owner, null);
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

		int oldRangeOffset = oldEditRange.getOffset();
		for (SearchMatch newSearchResult : newGroup.getSearchResults()) {
			if (newSearchResult.getOffset() == oldRangeOffset)
				return true;
		}
		return false;
	}

	private static IRegion getCorrespondingEditChangeRange(SearchMatch searchResult, TextChangeManager manager) {
		TextChange change= getTextChange(searchResult, manager);
		if (change == null)
			return null;

		IRegion oldMatchRange= createTextRange(searchResult);
		for (TextEditChangeGroup editChange : change.getTextEditChangeGroups()) {
			if (oldMatchRange.equals(editChange.getRegion()))
				return TextEdit.getCoverage(change.getPreviewEdits(editChange.getTextEdits()));
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
		for (SearchResultGroup newOccurrence : newOccurrences) {
			if (newOccurrence.getResource().equals(resource))
				return newOccurrence;
		}
		return null;
	}

//--- find missing changes in BOTH directions

	//TODO: Currently filters out declarations (MethodDeclarationMatch, FieldDeclarationMatch).
	//Long term solution: only pass reference search results in.
	public static RefactoringStatus analyzeRenameChanges2(TextChangeManager manager,
			SearchResultGroup[] oldReferences, SearchResultGroup[] newReferences, String newElementName) {
		RefactoringStatus result= new RefactoringStatus();

		HashMap<ICompilationUnit, SearchMatch[]> cuToNewResults= new HashMap<>(newReferences.length);
		for (SearchResultGroup newReference : newReferences) {
			ICompilationUnit cu= newReference.getCompilationUnit();
			if (cu != null)
				cuToNewResults.put(cu.getPrimary(), newReference.getSearchResults());
		}

		for (SearchResultGroup oldGroup : oldReferences) {
			SearchMatch[] oldMatches= oldGroup.getSearchResults();
			ICompilationUnit cu= oldGroup.getCompilationUnit();
			if (cu == null)
				continue;

			SearchMatch[] newSearchMatches= cuToNewResults.remove(cu);
			if (newSearchMatches == null) {
				for (SearchMatch oldMatch : oldMatches) {
					addShadowsError(cu, oldMatch, result);
				}
			} else {
				analyzeChanges(cu, manager.get(cu), oldMatches, newSearchMatches, newElementName, result);
			}
		}

		for (Entry<ICompilationUnit, SearchMatch[]> entry : cuToNewResults.entrySet()) {
			ICompilationUnit cu= entry.getKey();
			for (SearchMatch newMatch : entry.getValue()) {
				addReferenceShadowedError(cu, newMatch, newElementName, result);
			}
		}
		return result;
	}

	private static void analyzeChanges(ICompilationUnit cu, TextChange change,
			SearchMatch[] oldMatches, SearchMatch[] newMatches, String newElementName, RefactoringStatus result) {
		Map<Integer, SearchMatch> updatedOldOffsets= getUpdatedChangeOffsets(change, oldMatches);
		for (SearchMatch newMatch : newMatches) {
			Integer offsetInNew= newMatch.getOffset();
			SearchMatch oldMatch= updatedOldOffsets.remove(offsetInNew);
			if (oldMatch == null) {
				addReferenceShadowedError(cu, newMatch, newElementName, result);
			}
		}
		for (SearchMatch oldMatch : updatedOldOffsets.values()) {
			addShadowsError(cu, oldMatch, result);
		}
	}

	/**
	 *
	 * @param change
	 * @param oldMatches
	 * @return Map &lt;Integer updatedOffset, SearchMatch oldMatch&gt;
	 */
	private static Map<Integer, SearchMatch> getUpdatedChangeOffsets(TextChange change, SearchMatch[] oldMatches) {
		Map<Integer, SearchMatch> updatedOffsets= new HashMap<>();
		Map<Integer, Integer> oldToUpdatedOffsets= getEditChangeOffsetUpdates(change);
		for (SearchMatch oldMatch : oldMatches) {
			Integer updatedOffset= oldToUpdatedOffsets.get(Integer.valueOf(oldMatch.getOffset()));
			if (updatedOffset == null)
				updatedOffset= -1; //match not updated
			updatedOffsets.put(updatedOffset, oldMatch);
		}
		return updatedOffsets;
	}

	/**
	 *
	 * @param change
	 * @return Map &lt;Integer oldOffset, Integer updatedOffset&gt;
	 */
	private static Map<Integer, Integer> getEditChangeOffsetUpdates(TextChange change) {
		TextEditChangeGroup[] editChanges= change.getTextEditChangeGroups();
		Map<Integer, Integer> offsetUpdates= new HashMap<>(editChanges.length);
		for (TextEditChangeGroup editChange : editChanges) {
			IRegion oldRegion= editChange.getRegion();
			if (oldRegion == null)
				continue;
			IRegion updatedRegion= TextEdit.getCoverage(change.getPreviewEdits(editChange.getTextEdits()));
			if (updatedRegion == null)
				continue;

			offsetUpdates.put(oldRegion.getOffset(), updatedRegion.getOffset());
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
		String message= Messages.format(
				RefactoringCoreMessages.RenameAnalyzeUtil_reference_shadowed,
				new String[] {BasicElementLabels.getFileName(cu), BasicElementLabels.getJavaElementName(newElementName)});
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
		String message= Messages.format(RefactoringCoreMessages.RenameAnalyzeUtil_shadows, BasicElementLabels.getFileName(cu));
		result.addError(message, context);
	}

	/**
	 * This method analyzes a set of local variable renames inside one cu. It checks whether
	 * any new compile errors have been introduced by the rename(s) and whether the correct
	 * node(s) has/have been renamed.
	 *
	 * @param analyzePackages the LocalAnalyzePackages containing the information about the local renames
	 * @param cuChange the TextChange containing all local variable changes to be applied.
	 * @param oldCUNode the fully (incl. bindings) resolved AST node of the original compilation unit
	 * @param recovery whether statements and bindings recovery should be performed when parsing the changed CU
	 * @return a RefactoringStatus containing errors if compile errors or wrongly renamed nodes are found
	 * @throws CoreException thrown if there was an error greating the preview content of the change
	 */
	public static RefactoringStatus analyzeLocalRenames(LocalAnalyzePackage[] analyzePackages, TextChange cuChange, CompilationUnit oldCUNode, boolean recovery) throws CoreException {
		return analyzeLocalRenames(analyzePackages, cuChange, oldCUNode, false, recovery);
	}

	/**
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	@SuppressWarnings("javadoc")
	public static RefactoringStatus analyzeCompactConstructorLocalRenames(LocalAnalyzePackage[] analyzePackages, TextChange cuChange, CompilationUnit oldCUNode, boolean recovery) throws CoreException {
		return analyzeLocalRenames(analyzePackages, cuChange, oldCUNode, true, recovery);
	}

	private static RefactoringStatus analyzeLocalRenames(LocalAnalyzePackage[] analyzePackages, TextChange cuChange, CompilationUnit oldCUNode, boolean isCompactConstructor, boolean recovery) throws CoreException {

		RefactoringStatus result= new RefactoringStatus();
		ICompilationUnit compilationUnit= (ICompilationUnit) oldCUNode.getJavaElement();

		String newCuSource= cuChange.getPreviewContent(new NullProgressMonitor());
		CompilationUnit newCUNode= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL).parse(newCuSource, compilationUnit, true, recovery, null);

		result.merge(analyzeCompileErrors(newCuSource, newCUNode, oldCUNode));
		if (result.hasError())
			return result;

		for (LocalAnalyzePackage analyzePackage : analyzePackages) {
			ASTNode enclosing;
			IRegion newRegion;
			if (!isCompactConstructor) {
				enclosing= getEnclosingBlockOrMethodOrLambda(analyzePackage.fDeclarationEdit, cuChange, newCUNode);
				newRegion= RefactoringAnalyzeUtil.getNewTextRange(analyzePackage.fDeclarationEdit, cuChange);
			} else {
				enclosing= RefactoringAnalyzeUtil.getRecordDeclarationCompactConstructor(analyzePackage.fDeclarationEdit.getParent(), cuChange, newCUNode);
				newRegion= RefactoringAnalyzeUtil.getNewTextRange(analyzePackage.fDeclarationEdit.getParent(), cuChange);
			}

			// get new declaration
			ASTNode newDeclaration= NodeFinder.perform(newCUNode, newRegion.getOffset(), newRegion.getLength());
			Assert.isTrue(newDeclaration instanceof Name);

			VariableDeclaration declaration= getVariableDeclaration((Name) newDeclaration);
			Assert.isNotNull(declaration);

			SimpleName[] problemNodes= ProblemNodeFinder.getProblemNodes(enclosing, declaration, analyzePackage.fOccurenceEdits, cuChange);
			result.merge(RefactoringAnalyzeUtil.reportProblemNodes(newCuSource, problemNodes));
		}
		return result;
	}


	private static VariableDeclaration getVariableDeclaration(Name node) {
		IBinding binding= node.resolveBinding();
		if (binding == null && node.getParent() instanceof VariableDeclaration)
			return (VariableDeclaration) node.getParent();

		if (binding != null && binding.getKind() == IBinding.VARIABLE) {
			CompilationUnit cu= ASTNodes.getParent(node, CompilationUnit.class);
			return ASTNodes.findVariableDeclaration( ((IVariableBinding) binding), cu);
		}
		return null;
	}

	private static ASTNode getEnclosingBlockOrMethodOrLambda(TextEdit declarationEdit, TextChange change, CompilationUnit newCUNode) {
		ASTNode enclosing= RefactoringAnalyzeUtil.getBlock(declarationEdit, change, newCUNode);
		if (enclosing == null)
			enclosing= RefactoringAnalyzeUtil.getMethodDeclaration(declarationEdit, change, newCUNode);
		if (enclosing == null)
			enclosing= RefactoringAnalyzeUtil.getLambdaExpression(declarationEdit, change, newCUNode);
		return enclosing;
	}

	private static RefactoringStatus analyzeCompileErrors(String newCuSource, CompilationUnit newCUNode, CompilationUnit oldCUNode) {
		RefactoringStatus result= new RefactoringStatus();
		for (IProblem problem : RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode, oldCUNode)) {
			if (problem.isError())
				result.addEntry(new RefactoringStatusEntry(RefactoringStatus.ERROR, problem.getMessage(), new JavaStringStatusContext(newCuSource,
						SourceRangeFactory.create(problem))));
		}
		return result;
	}
}
