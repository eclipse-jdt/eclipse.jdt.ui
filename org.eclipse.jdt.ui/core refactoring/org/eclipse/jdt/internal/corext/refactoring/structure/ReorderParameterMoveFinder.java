package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;

class ReorderParameterMoveFinder {
	
	//no instances
	private ReorderParameterMoveFinder(){
	}
	
	/**
	 * returns List of ISourceRange[]
	 */
	static List findParameterSourceRanges(SearchResultGroup searchResultGroup) throws JavaModelException{
		ICompilationUnit cu= searchResultGroup.getCompilationUnit();
		if (cu == null)
			return new ArrayList(0);
		ReorderParameterMoveFinderVisitor visitor= new ReorderParameterMoveFinderVisitor(searchResultGroup.getSearchResults());
		AST.parseCompilationUnit(cu, false).accept(visitor);
		return visitor.getAllRegions();
	}
		
	/**
	 * returns List of ISourceRange[]
	 */
	static List findParameterDeclarationSourceRanges(SearchResultGroup searchResultGroup) throws JavaModelException{
		ICompilationUnit cu= searchResultGroup.getCompilationUnit();
		if (cu == null)
			return new ArrayList(0);
		ReorderParameterMoveFinderVisitor visitor= new ReorderParameterMoveFinderVisitor(searchResultGroup.getSearchResults());
		AST.parseCompilationUnit(cu, false).accept(visitor);
		return visitor.getDeclarationRegions();
	}

	/**
	 * returns List of ISourceRange[]
	 */
	static List findParameterReferenceSourceRanges(SearchResultGroup searchResultGroup) throws JavaModelException{
		ICompilationUnit cu= searchResultGroup.getCompilationUnit();
		if (cu == null)
			return new ArrayList(0);
		ReorderParameterMoveFinderVisitor visitor= new ReorderParameterMoveFinderVisitor(searchResultGroup.getSearchResults());
		AST.parseCompilationUnit(cu, false).accept(visitor);
		return visitor.getReferenceRegions();
	}
	
	private static class ReorderParameterMoveFinderVisitor extends ASTVisitor {
		private final SearchResult[] fSearchResults;
		private final List fDeclarationRegionsFound;
		private final List fReferenceRegionsFound;

		ReorderParameterMoveFinderVisitor(SearchResult[] searchResults){
			fSearchResults= searchResults;
			fDeclarationRegionsFound= new ArrayList(0);
			fReferenceRegionsFound= new ArrayList(0);
		}
		
		List getAllRegions(){
			List result= new ArrayList(fDeclarationRegionsFound.size() + fReferenceRegionsFound.size());
			result.addAll(fDeclarationRegionsFound);
			result.addAll(fReferenceRegionsFound);
			return result;
		}
		List getDeclarationRegions(){
			return fDeclarationRegionsFound;
		}
		List getReferenceRegions(){
			return fReferenceRegionsFound;
		}
		
		private boolean isStartPositionOnList(ASTNode node){
			return isStartPositionOnList(node.getStartPosition());
		}

		private boolean isStartPositionOnList(int start) {
			for (int i= 0; i< fSearchResults.length; i++){
				if (fSearchResults[i].getStart() == start)
					return true;
			};
			return false;
		}

		private boolean isEndPositionOnList(ASTNode node){
			return isEndPositionOnList(ASTNodes.getExclusiveEnd(node));
		}

		private boolean isEndPositionOnList(int end) {
			for (int i= 0; i< fSearchResults.length; i++){
				if (fSearchResults[i].getEnd() == end)
					return true;
			};
			return false;
		}
		
		private static ISourceRange[] createRegionsArray(MethodDeclaration methodDeclaration){
			return createNodeRegionArray(methodDeclaration.parameters());
		}

		private static ISourceRange[] createRegionsArray(MethodInvocation methodInvocation){
			return createNodeRegionArray(methodInvocation.arguments());
		}
		
		private static ISourceRange[] createRegionsArray(SuperMethodInvocation superMethodInvocation){
			return createNodeRegionArray(superMethodInvocation.arguments());
		}

		private static ISourceRange[] createRegionsArray(ConstructorInvocation node) {
			return createNodeRegionArray(node.arguments());
		}
		
		private static ISourceRange[] createRegionsArray(SuperConstructorInvocation node) {
			return createNodeRegionArray(node.arguments());
		}

		private static ISourceRange[] createRegionsArray(ClassInstanceCreation node) {
			return createNodeRegionArray(node.arguments());
		}

		private static ISourceRange[] createNodeRegionArray(List args) {
			return createRegionsArray((ASTNode[]) args.toArray(new ASTNode[args.size()]));
		}
						
		private static ISourceRange[] createRegionsArray(ASTNode[] nodes){
			ISourceRange[] result= new ISourceRange[nodes.length];
			for (int i= 0; i < result.length; i++){
				result[i]= new SourceRange(nodes[i]);
			}	
			return result;
		}
		
		//------visit methods
	
		public boolean visit(MethodInvocation methodInvocation) {
			if (isStartPositionOnList(methodInvocation.getName()))
				fReferenceRegionsFound.add(createRegionsArray(methodInvocation));
			return true;
		}
		
		public boolean visit(MethodDeclaration methodDeclaration) {
			if (isStartPositionOnList(methodDeclaration.getName()))
				fDeclarationRegionsFound.add(createRegionsArray(methodDeclaration));
			return true;
		}
		
		public boolean visit(SuperMethodInvocation superMethodInvocation) {
			if (isStartPositionOnList(superMethodInvocation.getName()))
				fReferenceRegionsFound.add(createRegionsArray(superMethodInvocation));
			return true;
		}
		
		public boolean visit(ConstructorInvocation node) {
			//XXX workaround for 23527
			if (isStartPositionOnList(node) && isEndPositionOnList(ASTNodes.getExclusiveEnd(node)-1))
				fReferenceRegionsFound.add(createRegionsArray(node));
			return true;
		}

		public boolean visit(SuperConstructorInvocation node) {
			//XXX workaround for 23527
			if (isStartPositionOnList(node) && isEndPositionOnList(ASTNodes.getExclusiveEnd(node)-1))
				fReferenceRegionsFound.add(createRegionsArray(node));
			return true;
		}

		public boolean visit(ClassInstanceCreation node) {
			if (isStartPositionOnList(node) && isEndPositionOnList(node))
				fReferenceRegionsFound.add(createRegionsArray(node));
			return true;
		}
	}
}


