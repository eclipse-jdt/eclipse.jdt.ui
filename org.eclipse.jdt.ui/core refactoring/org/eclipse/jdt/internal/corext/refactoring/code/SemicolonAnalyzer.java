/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.security.PublicKey;

import org.eclipse.jdt.internal.compiler.ast.AnonymousLocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
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

	public void endVisit(EmptyStatement node, BlockScope scope) {
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

	public void endVisit(AnonymousLocalTypeDeclaration anonymousTypeDeclaration, BlockScope scope) {
		fNeedsSemicolon= false;
		super.endVisit(anonymousTypeDeclaration, scope);
	}

	public void endVisit(LocalTypeDeclaration localTypeDeclaration, BlockScope scope) {
		fNeedsSemicolon= false;
		super.endVisit(localTypeDeclaration, scope);
	}

	public void endVisit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
		fNeedsSemicolon= false;
		super.endVisit(memberTypeDeclaration, scope);
	}

	public void endVisit(MethodDeclaration methodDeclaration, ClassScope scope) {
		fNeedsSemicolon= false;
		super.endVisit(methodDeclaration, scope);
	}

	public void endVisit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		fNeedsSemicolon= false;
		super.endVisit(typeDeclaration, scope);
	}
}
