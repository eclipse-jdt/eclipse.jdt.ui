/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.util;

import java.util.List;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.Scope;

public class JavaElementMapper extends GenericVisitor {

	private int fStart;
	private int fEnd;
	private AstNode fResult;
	private AstNode[] fParents;
	
	public JavaElementMapper(ISourceReference element) {
		try {
			ISourceRange sourceRange= element.getSourceRange();
			fStart= sourceRange.getOffset();
			fEnd= fStart + sourceRange.getLength() - 1;
		} catch (JavaModelException e) {
			fStart= -1;
			fEnd= -1;
		}
	}
	
	public AstNode getResult() {
		return fResult;
	}
	
	public AstNode[] getParents() {
		return fParents;
	}
	
	protected boolean visitRange(int start, int end, AstNode node, Scope scope) {
		if (start == fStart && end == fEnd) {
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

