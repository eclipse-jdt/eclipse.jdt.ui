/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.corext.refactoring.Assert;

public class JavaElementMapper extends GenericVisitor {

	private boolean fMapped;
	private IMember fElement;
	private int fStart;
	private int fEnd;
	private AstNode fResult;
	private AstNode[] fParents;
	private IProblem[] fProblems;
	
	public JavaElementMapper(IMember element) throws JavaModelException {
		fElement= element;
		Assert.isNotNull(fElement);
		try {
			ISourceRange sourceRange= fElement.getSourceRange();
			fStart= sourceRange.getOffset();
			fEnd= fStart + sourceRange.getLength() - 1;
			ICompilationUnit unit= fElement.getCompilationUnit();
			AST ast= new AST(unit);
			ast.accept(this);
			fProblems= ast.getProblems();
		} catch (JavaModelException e) {
			fStart= -1;
			fEnd= -1;
			fMapped= true;
		}
	}
	
	public AstNode getResult() {
		return fResult;
	}
	
	public AstNode[] getParents() {
		return fParents;
	}
	
	public IProblem[] getProblems() {
		return fProblems;
	}
	
	protected boolean visitRange(int start, int end, AstNode node, Scope scope) {
		if (fResult != null) {
			return false;
		} else if (start == fStart && end == fEnd) {
			fResult= node;
			List p= internalGetParents();
			if (p != null)
				fParents= (AstNode[])p.toArray(new AstNode[p.size()]);
			return false;
		} else if (start <= fStart && fEnd <= end) {
			return true;
		}
		return false;
	}	
}

