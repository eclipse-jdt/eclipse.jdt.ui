/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.core.refactoring;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.util.HackFinder;

public abstract class RefactoringASTAnalyzer extends AbstractSyntaxTreeVisitorAdapter{

	private List fSearchResults;
	private RefactoringStatus fResult;
	private CompilationUnit fCu;
	
	private int[] fLineSeparatorPositions; //set in visit(CompilationUnitDeclaration)
	
	public final RefactoringStatus analyze(List searchResults, ICompilationUnit cu) throws JavaModelException{
		fSearchResults= searchResults;
		fResult= new RefactoringStatus();
		fCu= (CompilationUnit)cu;
		fCu.accept(this);
		return fResult;	
	}
	
	public boolean doVisit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope){
		return true;
	}
	
	/* non java-doc
	 * sublasses implement doVisit instead
	 */ 
	public final boolean visit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		fLineSeparatorPositions= compilationUnitDeclaration.compilationResult.lineSeparatorPositions;
		return doVisit(compilationUnitDeclaration, scope);
	}
	
	//-------
	
	protected int getLineNumber(AstNode node){
		Assert.isNotNull(fLineSeparatorPositions);
		return ProblemHandler.searchLineNumber(fLineSeparatorPositions, node.sourceStart);
	}
	
	protected void addWarning(String msg){
		fResult.addError(msg);
	}
	
	protected void addError(String msg){
		fResult.addFatalError(msg);
	}
	
	protected static String getFullPath(ICompilationUnit cu) {
		Assert.isTrue(cu.exists());
		IPath path= null;
		try {
			return Refactoring.getResource(cu).getFullPath().toString();
		} catch (JavaModelException e) {
			return cu.getElementName();
		}
	}

	protected String cuFullPath() {
		return getFullPath(fCu);
	}
	
	protected boolean sourceRangeOnList(int start, int end){
		//DebugUtils.dump("start:" + start + " end:" + end);
		Iterator iter= fSearchResults.iterator();
		while (iter.hasNext()){
			SearchResult searchResult= (SearchResult)iter.next();
			//DebugUtils.dump("[" + searchResult.getStart() + ", " + searchResult.getEnd() + "]");
			if (start == searchResult.getStart() && end == searchResult.getEnd())
				return true;
		}
		return false;
	}
	
	protected boolean sourceRangeOnList(QualifiedNameReference qualifiedNameReference){
		int start= qualifiedNameReference.sourceStart;
		return sourceRangeOnList(start, start + qualifiedNameReference.tokens[0].length);
	}
	
	protected boolean sourceRangeOnList(QualifiedTypeReference qualifiedTypeReference){
		int start= qualifiedTypeReference.sourceStart;
		return sourceRangeOnList(start, start + qualifiedTypeReference.tokens[0].length);
	}
	
	protected boolean sourceRangeOnList(AstNode astNode){		
		if (astNode instanceof QualifiedNameReference)
			return sourceRangeOnList((QualifiedNameReference)astNode);
		else if (astNode instanceof QualifiedTypeReference)
			return sourceRangeOnList((QualifiedTypeReference)astNode);
		
		return sourceRangeOnList(astNode.sourceStart, astNode.sourceEnd + 1);
	}
}