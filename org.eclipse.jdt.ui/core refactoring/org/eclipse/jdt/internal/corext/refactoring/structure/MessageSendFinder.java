package org.eclipse.jdt.internal.corext.refactoring.structure;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.GenericVisitor;

class MessageSendFinder extends GenericVisitor{
	
	private List fMessageSendRanges;
	private SearchResult[] fSearchResults;
	
	private MessageSendFinder(SearchResult[] searchResults){
		fSearchResults= searchResults;
		fMessageSendRanges= new ArrayList();
	}
	
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
		
		AST ast= new AST(cu);
		if (ast.isMalformed())
			return new ISourceRange[0];
		MessageSendFinder instance= new MessageSendFinder(searchResults);
		ast.accept(instance);
		return (ISourceRange[]) instance.fMessageSendRanges.toArray(new ISourceRange[instance.fMessageSendRanges.size()]);
	}
	//---
	
	private static boolean areReportedForSameNode(MessageSend node, SearchResult searchResult){
		int nodeEnd= ASTUtil.getSourceEnd(node) + 1;//???
		
		if (ASTUtil.getSourceStart(node) > searchResult.getStart())
			return false;
		if (nodeEnd < searchResult.getEnd())	
			return false;
		if (ASTUtil.getStart(node.nameSourcePosition) != searchResult.getStart())
			return false;
			
		return true;	
	}
	
	private boolean isReported(MessageSend node){
		for (int i= 0; i < fSearchResults.length; i++) {
			if (areReportedForSameNode(node, fSearchResults[i]))
				return true;
		}
		return false;
	}
	
	/*
	 * @see GenericVisitor#visit(MessageSend, BlockScope)
	 */
	public boolean visit(MessageSend node, BlockScope scope) {
		if (! isReported(node))
			return visitNode(node, scope);
			
		int start= ASTUtil.getSourceStart(node);
		int end= ASTUtil.getEnd(node.nameSourcePosition);
		int length= end - start + 1;
		fMessageSendRanges.add(new SourceRange(start, length));
		return visitNode(node, scope);
	}
	
}
