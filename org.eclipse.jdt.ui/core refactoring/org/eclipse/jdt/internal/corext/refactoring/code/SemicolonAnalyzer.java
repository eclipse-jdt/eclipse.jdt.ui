/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.corext.refactoring.util.GenericVisitor;

/* package */ class SemicolonAnalyzer extends GenericVisitor {

	private boolean fNeedsSemicolon;

	public static boolean perform(AstNode[] statements, BlockScope scope) {
		SemicolonAnalyzer analyzer= new SemicolonAnalyzer();
		for (int i= 0; i < statements.length; i++) {
			AstNode node= statements[i];
			if (scope instanceof MethodScope)
				node.traverse(analyzer, (MethodScope)scope);
			else
				node.traverse(analyzer, scope);
		}
		return analyzer.fNeedsSemicolon;
	}

	protected boolean visitRange(int start, int end, AstNode node, Scope scope) {
		fNeedsSemicolon= true;
		return true;
	}
	
	public void endVisit(Block node, BlockScope scope) {
		fNeedsSemicolon= false;
		super.endVisit(node, scope);
	}

	public void endVisit(SwitchStatement node, BlockScope scope) {
		fNeedsSemicolon= false;
		super.endVisit(node, scope);
	}
	
	public void endVisit(SynchronizedStatement node, BlockScope scope) {
		fNeedsSemicolon= false;
		super.endVisit(node, scope);
	}

	public void endVisit(TryStatement node, BlockScope scope) {
		fNeedsSemicolon= false;
		super.endVisit(node, scope);
	}

}
