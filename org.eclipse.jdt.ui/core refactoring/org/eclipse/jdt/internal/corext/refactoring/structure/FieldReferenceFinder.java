package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;

class FieldReferenceFinder {

	public static ISourceRange[] findFieldReferenceRanges(SearchResultGroup searchResultGroup) throws JavaModelException {
		ICompilationUnit cu= searchResultGroup.getCompilationUnit();
		if (cu == null)
			return new ISourceRange[0];
		return findFieldReferenceRanges(searchResultGroup.getSearchResults(), cu);
	}
	
	public static ISourceRange[] findFieldReferenceRanges(SearchResult[] searchResults, ICompilationUnit cu) throws JavaModelException {
		Assert.isNotNull(searchResults);
		if (searchResults.length == 0)
			return new ISourceRange[0];
		FieldReferenceFinderVisitor visitor= new FieldReferenceFinderVisitor(searchResults);
		AST.parseCompilationUnit(cu, false).accept(visitor);
		return visitor.getFoundRanges();
	}
	
	///---- ast visitor ----------
	
	private static class FieldReferenceFinderVisitor extends ASTVisitor{
		private Collection fFoundRanges;
		private SearchResult[] fSearchResults;
		
		FieldReferenceFinderVisitor(SearchResult[] searchResults){
			fFoundRanges= new ArrayList();
			fSearchResults= searchResults;
		}
		
		ISourceRange[] getFoundRanges(){
			return (ISourceRange[]) fFoundRanges.toArray(new ISourceRange[fFoundRanges.size()]);
		}
		
		private static boolean areReportedForSameNode(SimpleName node, SearchResult searchResult){
			if (node.getStartPosition() != searchResult.getStart())
				return false;
			if (ASTNodes.getExclusiveEnd(node) != searchResult.getEnd())	
				return false;
				
			return true;	
		}
	
		private static boolean areReportedForSameNode(FieldAccess node, SearchResult searchResult){
			if (node.getStartPosition() > searchResult.getStart())
				return false;
			if (ASTNodes.getExclusiveEnd(node) != searchResult.getEnd())	
				return false;
			if (node.getName().getStartPosition() != searchResult.getStart())
				return false;
				
			return true;	
		}	
		
		private static boolean areReportedForSameNode(QualifiedName node, SearchResult searchResult){
			if (node.getStartPosition() > searchResult.getStart())
				return false;
			if (ASTNodes.getExclusiveEnd(node) != searchResult.getEnd())	
				return false;
				
			return true;	
		}
		
		private boolean isReported(FieldAccess node){
			for (int i= 0; i < fSearchResults.length; i++) {
				if (areReportedForSameNode(node, fSearchResults[i]))
					return true;
			}
			return false;
		}
		
		private boolean isReported(QualifiedName node){
			for (int i= 0; i < fSearchResults.length; i++) {
				if (areReportedForSameNode(node, fSearchResults[i]))
					return true;
			}
			return false;
		}
		
		private boolean isReported(SimpleName node){
			for (int i= 0; i < fSearchResults.length; i++) {
				if (areReportedForSameNode(node, fSearchResults[i]))
					return true;
			}
			return false;
		}
		
		//-- visit methods ---
		
		public boolean visit(FieldAccess node) {
			if (! isReported(node))
				return true;
			
			fFoundRanges.add(new SourceRange(node));
			return false;
		}
	
		public boolean visit(QualifiedName node) {
			if (! isReported(node))
				return true;
			
			fFoundRanges.add(new SourceRange(node));
			return false;
		}
	
		public boolean visit(SimpleName node) {
			if (! isReported(node))
				return true;
			
			fFoundRanges.add(new SourceRange(node));
			return false;
		}
		
	}
}
