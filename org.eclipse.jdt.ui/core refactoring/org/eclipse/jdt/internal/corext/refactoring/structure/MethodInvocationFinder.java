package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;

class MethodInvocationFinder {

	public static ISourceRange[] findMessageSendRanges(SearchResultGroup searchResultGroup) throws JavaModelException {
		Assert.isNotNull(searchResultGroup);
		IJavaElement je= JavaCore.create(searchResultGroup.getResource());
		if (je == null || je.getElementType() != IJavaElement.COMPILATION_UNIT)
			return new ISourceRange[0];
		ICompilationUnit cu= (ICompilationUnit)je;
		return findMessageSendRanges(searchResultGroup.getSearchResults(), cu);
	}
	
	public static ISourceRange[] findMessageSendRanges(SearchResult[] searchResults, ICompilationUnit cu) throws JavaModelException {
		if (searchResults.length == 0)
			return new ISourceRange[0];
		
		MethodInvocationFinderVisitor visitor= new MethodInvocationFinderVisitor(searchResults);
		AST.parseCompilationUnit(cu, false).accept(visitor);
		return visitor.getFoundRanges();
	}

	private static class MethodInvocationFinderVisitor extends ASTVisitor{
		
		private Collection fFoundRanges;
		private SearchResult[] fSearchResults;
		
		MethodInvocationFinderVisitor(SearchResult[] searchResults){
			fSearchResults= searchResults;
			fFoundRanges= new ArrayList();
		}
		
		ISourceRange[] getFoundRanges(){
			return (ISourceRange[]) fFoundRanges.toArray(new ISourceRange[fFoundRanges.size()]);
		}
		
		private static boolean areReportedForSameNode(MethodInvocation node, SearchResult searchResult){
			if (node.getStartPosition() > searchResult.getStart())
				return false;
			if (ASTNodes.getExclusiveEnd(node) < searchResult.getEnd())	
				return false;
			if (node.getName().getStartPosition() != searchResult.getStart())
				return false;
				
			return true;	
		}
	
		private boolean isReported(MethodInvocation node){
			for (int i= 0; i < fSearchResults.length; i++) {
				if (areReportedForSameNode(node, fSearchResults[i]))
					return true;
			}
			return false;
		}
		
		//--- visit methods ----
		public boolean visit(MethodInvocation node) {
			if (! isReported(node))
				return true;
			
			int start= node.getStartPosition();
			int end= ASTNodes.getExclusiveEnd(node.getName());
			int length= end - start;
			fFoundRanges.add(new SourceRange(start, length));
			return true;
		}
		
	}
}
