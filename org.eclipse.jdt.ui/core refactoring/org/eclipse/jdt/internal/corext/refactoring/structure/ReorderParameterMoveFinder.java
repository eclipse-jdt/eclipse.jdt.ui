package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import org.eclipse.jdt.internal.corext.SourceRange;
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
		
		private boolean isStartPositionOnList(int start){
			for (int i= 0; i< fSearchResults.length; i++){
				if (fSearchResults[i].getStart() == start)
					return true;
			};
			return false;
		}

		private static ISourceRange[] createRegionsArray(MethodInvocation methodInvocation){
			List args= methodInvocation.arguments();
			return createRegionsArray((Expression[]) args.toArray(new Expression[args.size()]));
		}
		
		private static ISourceRange[] createRegionsArray(MethodDeclaration methodDeclaration){
			List params= methodDeclaration.parameters();
			return createRegionsArray((SingleVariableDeclaration[]) params.toArray(new SingleVariableDeclaration[params.size()]));
		}
		
		private static ISourceRange[] createRegionsArray(SuperMethodInvocation superMethodInvocation){
			List args= superMethodInvocation.arguments();
			return createRegionsArray((Expression[]) args.toArray(new Expression[args.size()]));
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
			if (isStartPositionOnList(methodInvocation.getName().getStartPosition()))
				fReferenceRegionsFound.add(createRegionsArray(methodInvocation));
			return true;
		}
		
		public boolean visit(MethodDeclaration methodDeclaration) {
			if (isStartPositionOnList(methodDeclaration.getName().getStartPosition()))
				fDeclarationRegionsFound.add(createRegionsArray(methodDeclaration));
			return true;
		}
		
		public boolean visit(SuperMethodInvocation superMethodInvocation) {
			if (isStartPositionOnList(superMethodInvocation.getName().getStartPosition()))
				fReferenceRegionsFound.add(createRegionsArray(superMethodInvocation));
			return true;
		}
		
	}
}


