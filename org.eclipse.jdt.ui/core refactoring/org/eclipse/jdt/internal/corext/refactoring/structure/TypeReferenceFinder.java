package org.eclipse.jdt.internal.corext.refactoring.structure;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.GenericVisitor;

class TypeReferenceFinder extends GenericVisitor {

	private List fTypeReferenceRanges;
	private SearchResult[] fSearchResults;
	
	private TypeReferenceFinder(SearchResult[] searchResults){
		fSearchResults= searchResults;
		fTypeReferenceRanges= new ArrayList();
	}
	
	public static ISourceRange[] findTypeReferenceRanges(SearchResult[] searchResults, ICompilationUnit cu) throws JavaModelException {
//		return TypeReferenceFinder2.findTypeReferenceRanges(searchResults, cu);
		Assert.isNotNull(searchResults);
		if (searchResults.length == 0)
			return new ISourceRange[0];
		
		AST ast= new AST(cu);
		if (ast.isMalformed())
			return new ISourceRange[0];
		TypeReferenceFinder instance= new TypeReferenceFinder(searchResults);
		ast.accept(instance);
		return (ISourceRange[]) instance.fTypeReferenceRanges.toArray(new ISourceRange[instance.fTypeReferenceRanges.size()]);
	}

	private static boolean areReportedForSameNode_noArray(AstNode node, SearchResult searchResult) {
		int nodeEnd= ASTUtil.getSourceEnd(node) + 1;//???
		
		if (ASTUtil.getSourceStart(node) != searchResult.getStart())
			return false;
		if (nodeEnd < searchResult.getEnd())	
			return false;
		
		return true;	
	}
	
	private static boolean areReportedForSameNode(SingleTypeReference node, SearchResult searchResult){
		return areReportedForSameNode_noArray(node, searchResult);
	}

	private static boolean areReportedForSameNode(QualifiedTypeReference node, SearchResult searchResult){
		return areReportedForSameNode_noArray(node, searchResult);
	}
	
	private static boolean areReportedForSameNode(ArrayTypeReference node, SearchResult searchResult){
		int nodeEnd= ASTUtil.getSourceEnd(node) + 1;//???
		
		if (ASTUtil.getSourceStart(node) != searchResult.getStart())
			return false;
		if (nodeEnd < searchResult.getEnd())	
			return false;
		
		return true;			
	}
	
	private static boolean areReportedForSameNode(ArrayQualifiedTypeReference node, SearchResult searchResult){
		int nodeEnd= ASTUtil.getSourceEnd(node) + 1;//???
		
		if (ASTUtil.getSourceStart(node) != searchResult.getStart())
			return false;
		if (nodeEnd < searchResult.getEnd())	
			return false;
		
		return true;			
	}
	
	private boolean isReported(SingleTypeReference node){
		for (int i= 0; i < fSearchResults.length; i++) {
			if (areReportedForSameNode(node, fSearchResults[i]))
				return true;
		}
		return false;
	}
	
	private boolean isReported(QualifiedTypeReference node){
		for (int i= 0; i < fSearchResults.length; i++) {
			if (areReportedForSameNode(node, fSearchResults[i]))
				return true;
		}
		return false;
	}
	
	private boolean isReported(ArrayTypeReference node){
		for (int i= 0; i < fSearchResults.length; i++) {
			if (areReportedForSameNode(node, fSearchResults[i]))
				return true;
		}
		return false;
	}
	
	private boolean isReported(ArrayQualifiedTypeReference node){
		for (int i= 0; i < fSearchResults.length; i++) {
			if (areReportedForSameNode(node, fSearchResults[i]))
				return true;
		}
		return false;
	}
	
	private boolean isReported(AstNode node){
		//sequence important here
		
		if (node instanceof ArrayTypeReference)
			return isReported((ArrayTypeReference)node);

		if (node instanceof ArrayQualifiedTypeReference)
			return isReported((ArrayQualifiedTypeReference)node);
		
		if (node instanceof SingleTypeReference)
			return isReported((SingleTypeReference)node);

		if (node instanceof QualifiedTypeReference)
			return isReported((QualifiedTypeReference)node);

		return false;
	}
	
	private void addIfReported(AstNode node) {
		if (! isReported(node))
			return;
			
		int start= getSourceStart(node);
		int end= getSourceEnd(node);
		int length= end - start + 1;
		fTypeReferenceRanges.add(new SourceRange(start, length));
	}

	private int getSourceEnd(AstNode node) {
		if (node instanceof ArrayTypeReference){
			ArrayTypeReference atr= (ArrayTypeReference)node;
			return getSourceStart(atr) + atr.token.length - 1;
		}
		if (node instanceof ArrayQualifiedTypeReference){
			ArrayQualifiedTypeReference atr= (ArrayQualifiedTypeReference)node;
			long[] poss= atr.sourcePositions;
			int lastTokenEnd= ASTUtil.getEnd(poss[poss.length - 1]);
			return lastTokenEnd;
		}
		
		return ASTUtil.getSourceEnd(node);
	}

	private int getSourceStart(AstNode node) {
		return ASTUtil.getSourceStart(node);
	}

	//---visit methods
	
	public boolean visit(SingleTypeReference node, ClassScope scope) {
		addIfReported(node);
		return super.visit(node, scope);
	}
	
	public boolean visit(QualifiedTypeReference node, ClassScope scope) {
		addIfReported(node);
		return super.visit(node, scope);
	}
	
	public boolean visit(ArrayTypeReference node, ClassScope scope) {
		addIfReported(node);
		return super.visit(node, scope);
	}
	
	public boolean visit(ArrayQualifiedTypeReference node, ClassScope scope) {
		addIfReported(node);
		return super.visit(node, scope);
	}
	
	public boolean visit(ArrayTypeReference node, BlockScope scope) {
		addIfReported(node);
		return visitNode(node, scope);
	}

	public boolean visit(ArrayQualifiedTypeReference node, BlockScope scope) {
		addIfReported(node);
		return visitNode(node, scope);
	}

	public boolean visit(SingleTypeReference node, BlockScope scope) {
		addIfReported(node);
		return visitNode(node, scope);
	}

	public boolean visit(QualifiedTypeReference node, BlockScope scope) {
		addIfReported(node);
		return visitNode(node, scope);
	}
}
