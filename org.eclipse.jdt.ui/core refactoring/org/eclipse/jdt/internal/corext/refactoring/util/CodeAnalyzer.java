/*
 * (c) Copyright IBM Corp. 2000, 2001, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.corext.refactoring.ExtendedBuffer;

public class CodeAnalyzer extends NewStatementAnalyzer {
	
	protected AbstractMethodDeclaration fEnclosingMethod;
	protected ClassScope fClassScope;
	protected MethodScope fOuterMostMethodScope;
	
	public CodeAnalyzer(ExtendedBuffer buffer, Selection selection) {
		super(buffer, selection);
	}

	protected void initialize() {
		AstNode[] parents= getParents();
		for (int i= parents.length - 1; i >= 0; i--) {
			AstNode parent= parents[i];
			if (parent instanceof AbstractMethodDeclaration) {
				fEnclosingMethod= (AbstractMethodDeclaration)parent;
				break;
			}
		}
		if (fEnclosingMethod != null) {
			fOuterMostMethodScope= fEnclosingMethod.scope.outerMostMethodScope();
			fClassScope= getClassScope(fEnclosingMethod.scope);
			fStatus.merge(LocalTypeAnalyzer.perform(fEnclosingMethod, fClassScope, fSelection));
		}
	}
	
	private ClassScope getClassScope(Scope scope) {
		while (scope != null) {
			if (scope instanceof ClassScope)
				return (ClassScope) scope;
			scope= scope.parent;
		}
		return null;
	}	
}
