package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.dom.ASTUtil;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;

public class TypeReferenceFinder2 {

	public static ISourceRange[] findTypeReferenceRanges(SearchResult[] searchResults, ICompilationUnit cu) throws JavaModelException {
		Assert.isNotNull(searchResults);
		if (searchResults.length == 0)
			return new ISourceRange[0];
		
		TypeReferenceFinderVisitor visitor= new TypeReferenceFinderVisitor(searchResults);
		AST.parseCompilationUnit(cu, false).accept(visitor);
		return visitor.getFoundRanges();
	}
	
	private static class TypeReferenceFinderVisitor extends ASTVisitor{
		
		private Collection fFoundRanges;
		private SearchResult[] fSearchResults;
		
		TypeReferenceFinderVisitor(SearchResult[] searchResults){
			fSearchResults= searchResults;
			fFoundRanges= new ArrayList();
		}
		
		ISourceRange[] getFoundRanges(){
			return (ISourceRange[]) fFoundRanges.toArray(new ISourceRange[fFoundRanges.size()]);
		}
		
		private static boolean areReportedForSameNode(SimpleType node, SearchResult searchResult){
			if (node.getStartPosition() != searchResult.getStart())
				return false;
			if (ASTUtil.getEndPosition(node) < searchResult.getEnd())	
				return false;
		
			return true;			
		}
		
		private static boolean areReportedForSameNode(ArrayType node, SearchResult searchResult){
			if (node.getStartPosition() != searchResult.getStart())
				return false;
			if (ASTUtil.getEndPosition(node) < searchResult.getEnd())	
				return false;
		
			return true;			
		}
		
		private boolean isReported(SimpleType node){
			for (int i= 0; i < fSearchResults.length; i++) {
				if (areReportedForSameNode(node, fSearchResults[i]))
					return true;
			}
			return false;
		}
		
		private boolean isReported(ArrayType node){
			for (int i= 0; i < fSearchResults.length; i++) {
				if (areReportedForSameNode(node, fSearchResults[i]))
					return true;
			}
			return false;
		}
		
		//--- visit methods ---
		public boolean visit(SimpleType node) {
			if (! isReported(node))
				return true;
			
			fFoundRanges.add(new SourceRange(node));
			return false;
		}
		
		public boolean visit(ArrayType node) {
			if (! isReported(node))
				return true;
			
			int end= ASTUtil.getEndPosition(node.getElementType());
			int length= end - node.getStartPosition();
			fFoundRanges.add(new SourceRange(node.getStartPosition(), length));
			return false;
		}
		

	}
}
