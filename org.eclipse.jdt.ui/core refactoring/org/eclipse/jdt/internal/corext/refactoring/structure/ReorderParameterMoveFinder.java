package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;

class ReorderParameterMoveFinder {
	
	//no instances
	private ReorderParameterMoveFinder(){
	}
	
	/**
	 * returns List of ISourceRange[]
	 */
	static List findParameterSourceRanges(SearchResultGroup searchResultGroup) throws JavaModelException{
		IJavaElement element= JavaCore.create(searchResultGroup.getResource());
		if (!(element instanceof CompilationUnit))
			return new ArrayList(0);
			
		ReorderParameterMoveFinderVisitor visitor= new ReorderParameterMoveFinderVisitor(searchResultGroup.getSearchResults());
		AST ast= new AST((ICompilationUnit)element);
		ast.accept(visitor);
		return visitor.getRegions();
	}
		
	private static class ReorderParameterMoveFinderVisitor extends AbstractSyntaxTreeVisitorAdapter {
		private List fRegionsFound;
		private SearchResult[] fSearchResults;

		ReorderParameterMoveFinderVisitor(SearchResult[] searchResults){
			fSearchResults= searchResults;
			fRegionsFound= new ArrayList();
		}
		
		List getRegions(){
			return fRegionsFound;
		}
		
		private boolean isStartPositionOnList(int start){
			for (int i= 0; i< fSearchResults.length; i++){
				if (fSearchResults[i].getStart() == start)
					return true;
			};
			return false;
		}

		private boolean isOurMethod(MessageSend messageSend){
			return isStartPositionOnList((int) (messageSend.nameSourcePosition >> 32));
		}
	
		private boolean isOurMethod(MethodDeclaration methodDeclaration){
			return isStartPositionOnList(methodDeclaration.sourceStart);
		}	
		
		private static ISourceRange[] createRegionsArray(MessageSend messageSend){
			ISourceRange[] result= new ISourceRange[messageSend.arguments.length];
			for (int i= 0; i < result.length; i++){
				int length= messageSend.arguments[i].sourceEnd - messageSend.arguments[i].sourceStart + 1;
				result[i]= new SourceRange(messageSend.arguments[i].sourceStart, length);
			}
			return result;
		}
		
		private static ISourceRange[] createRegionsArray(MethodDeclaration methodDeclaration){
			ISourceRange[] result= new ISourceRange[methodDeclaration.arguments.length];
			for (int i= 0; i < result.length; i++){
				int start= methodDeclaration.arguments[i].declarationSourceStart;
				int length= methodDeclaration.arguments[i].sourceEnd - start + 1;
				result[i]= new SourceRange(start, length);
			}
			return result;
		}
		
		//------visit methods
	
		public boolean visit(MessageSend messageSend, BlockScope scope) {
			if (isOurMethod(messageSend))
				fRegionsFound.add(createRegionsArray(messageSend));
			return true;
		}
		
		public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
			if (isOurMethod(methodDeclaration))
				fRegionsFound.add(createRegionsArray(methodDeclaration));
			return true;
		}		
	}
}

