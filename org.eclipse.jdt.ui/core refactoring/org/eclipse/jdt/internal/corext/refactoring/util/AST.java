/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.IAbstractSyntaxTreeVisitor;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.core.CompilationUnit;

public class AST {

	private static class RootVisitor extends AbstractSyntaxTreeVisitorAdapter {
		public CompilationUnitDeclaration root;
		public CompilationUnitScope scope;
		public List problems= new ArrayList(5);
		
		public boolean visit(CompilationUnitDeclaration node, CompilationUnitScope scope) {
			root= node;
			this.scope= scope;
			return false;
		}
		public void acceptProblem(IProblem problem) {
			problems.add(problem);	
		}	
	}
	
	private RootVisitor fRootVisitor;
	private IProblem[] fProblems;
	
	public AST(ICompilationUnit cunit) throws JavaModelException {
		fRootVisitor= new RootVisitor();
		((CompilationUnit)cunit).accept(fRootVisitor);
		fProblems= (IProblem[])fRootVisitor.problems.toArray(new IProblem[fRootVisitor.problems.size()]);
	}
	
	public boolean hasProblems() {
		return fProblems.length > 0;
	}
	
	public IProblem[] getProblems() {
		return fProblems;
	}
	
	public boolean hasProblem(AstNode node) {
		for (int i= 0; i < fProblems.length; i++) {
			IProblem problem= fProblems[i];
			if (problem.getSourceStart() <= node.sourceStart && node.sourceEnd <= problem.getSourceEnd())
				return true;
		}
		return false;
	}
	
	public void accept(IAbstractSyntaxTreeVisitor visitor) {
		if (visitor instanceof IParentProvider) {
			ASTParentTrackingAdapter tracker= new ASTParentTrackingAdapter(visitor);
			((IParentProvider)visitor).setParentTracker(tracker);
			visitor= tracker;
		}
		fRootVisitor.root.traverse(visitor, fRootVisitor.scope);
	}
	
	public boolean isMalformed() {
		return fRootVisitor.root == null;
	}
}

