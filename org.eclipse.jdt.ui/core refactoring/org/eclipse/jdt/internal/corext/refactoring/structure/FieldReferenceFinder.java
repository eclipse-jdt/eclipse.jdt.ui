package org.eclipse.jdt.internal.corext.refactoring.structure;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.GenericVisitor;

class FieldReferenceFinder extends GenericVisitor{

	private List fFieldReferenceRanges;
	private SearchResult[] fSearchResults;
	
	private FieldReferenceFinder(SearchResult[] searchResults){
		fSearchResults= searchResults;
		fFieldReferenceRanges= new ArrayList();
	}
	
	public static ISourceRange[] findFieldReferenceRanges(SearchResultGroup searchResultGroup) throws JavaModelException {
		IJavaElement je= JavaCore.create(searchResultGroup.getResource());
		if (je == null || je.getElementType() != IJavaElement.COMPILATION_UNIT)
			return new ISourceRange[0];
		ICompilationUnit cu= (ICompilationUnit)je;
		return findFieldReferenceRanges(searchResultGroup.getSearchResults(), cu);
	}
	
	public static ISourceRange[] findFieldReferenceRanges(SearchResult[] searchResults, ICompilationUnit cu) throws JavaModelException {
		Assert.isNotNull(searchResults);
		if (searchResults.length == 0)
			return new ISourceRange[0];
		AST ast= new AST(cu);
		if (ast.isMalformed())
			return new ISourceRange[0];
		FieldReferenceFinder instance= new FieldReferenceFinder(searchResults);
		ast.accept(instance);
		return (ISourceRange[]) instance.fFieldReferenceRanges.toArray(new ISourceRange[instance.fFieldReferenceRanges.size()]);
	}
	
	//-----
	private static boolean areReportedForSameNode(SingleNameReference node, SearchResult searchResult){
		int nodeEnd= ASTUtil.getSourceEnd(node) + 1;//???
		
		if (ASTUtil.getSourceStart(node) != searchResult.getStart())
			return false;
		if (nodeEnd < searchResult.getEnd())	
			return false;
			
		return true;	
	}

	private static boolean areReportedForSameNode(FieldReference node, SearchResult searchResult){
		int nodeStart= ASTUtil.getSourceStart(node);
		int nodeEnd= ASTUtil.getSourceEnd(node) + 1;//???
		
		if (nodeStart > searchResult.getStart())
			return false;
		if (nodeEnd < searchResult.getEnd())	
			return false;
		if (ASTUtil.getStart(node.nameSourcePosition) != searchResult.getStart())
			return false;
			
		return true;	
	}	
	
	private static boolean areReportedForSameNode(QualifiedNameReference node, SearchResult searchResult){
		int nodeStart= ASTUtil.getSourceStart(node);
		int nodeEnd= ASTUtil.getSourceEnd(node) + 1;//???
		
		if (nodeStart > searchResult.getStart())
			return false;
		if (nodeEnd < searchResult.getEnd())	
			return false;
			
		return true;	
	}	
	
	private boolean isReported(FieldReference node){
		for (int i= 0; i < fSearchResults.length; i++) {
			if (areReportedForSameNode(node, fSearchResults[i]))
				return true;
		}
		return false;
	}
	
	private boolean isReported(QualifiedNameReference node){
		for (int i= 0; i < fSearchResults.length; i++) {
			if (areReportedForSameNode(node, fSearchResults[i]))
				return true;
		}
		return false;
	}
	
	private boolean isReported(SingleNameReference node){
		for (int i= 0; i < fSearchResults.length; i++) {
			if (areReportedForSameNode(node, fSearchResults[i]))
				return true;
		}
		return false;
	}
	
	private boolean isReported(AstNode node){
		if (node instanceof FieldReference)
			return isReported((FieldReference)node);
	
		if (node instanceof QualifiedNameReference)
			return isReported((QualifiedNameReference)node);
			
		if (node instanceof SingleNameReference)
			return isReported((SingleNameReference)node);

		return false;	
	}
	
	///----  visit methods 

	private void addIfReported(AstNode node) {
		if (! isReported(node))
			return;
			
		int start= getSourceStart(node);
		int end= getSourceEnd(node);
		int length= end - start + 1;
		fFieldReferenceRanges.add(new SourceRange(start, length));
	}

	private int getSourceEnd(AstNode node) {
		if (node instanceof QualifiedNameReference){
			QualifiedNameReference qnr= (QualifiedNameReference)node;
			
		}
		return ASTUtil.getSourceEnd(node);
	}

	private int getSourceStart(AstNode node) {
		if (node instanceof FieldReference){
			FieldReference fr= (FieldReference)node;
			if (fr.receiver != null)
				return ASTUtil.getSourceStart(fr.receiver);
			else	
				return ASTUtil.getSourceStart(node);
		}	
		return ASTUtil.getSourceStart(node);
	}

	/*
	 * @see GenericVisitor#visit(FieldReference, BlockScope)
	 */	
	public boolean visit(FieldReference node, BlockScope scope) {
		addIfReported(node);
		return visitNode(node, scope);
	}

	/*
	 * @see GenericVisitor#visit(QualifiedNameReference, BlockScope)
	 */		
	public boolean visit(QualifiedNameReference node, BlockScope scope) {
		addIfReported(node);
		return visitNode(node, scope);
	}

	/*
	 * @see GenericVisitor#visit(SingleNameReference, BlockScope)
	 */		
	public boolean visit(SingleNameReference node, BlockScope scope) {
		addIfReported(node);
		return visitNode(node, scope);
	}
}