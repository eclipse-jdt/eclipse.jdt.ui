/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;

public class ASTRewriteClear extends ASTVisitor {

	private ASTRewrite fRewrite;

	public ASTRewriteClear(ASTRewrite rewrite) {
		fRewrite= rewrite;
	}
	
	private void clearList(List list) {
		for (int i= list.size() - 1; i >= 0 ; i--) {
			ASTNode element= (ASTNode) list.get(i);
			if (fRewrite.isInserted(element)) {
				list.remove(i);
			} else {
				if (fRewrite.isCollapsed(element)) {
					List collapsed= ((Block) element).statements();
					list.remove(i);
					for (int k= collapsed.size() - 1; k >= 0 ; k--) {
						Object curr= collapsed.remove(k);
						list.add(i, curr);
					}
				}
			}
		}
	}
	
	private boolean isInserted(ASTNode node) {
		return node != null && fRewrite.isInserted(node);
	}
	
	
	/*
	 * @see ASTVisitor#visit(AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		clearList(node.bodyDeclarations());
		return true;
	}

	/*
	 * @see ASTVisitor#visit(ArrayCreation)
	 */
	public boolean visit(ArrayCreation node) {
		clearList(node.dimensions());
		if (isInserted(node.getInitializer())) {
			node.setInitializer(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		clearList(node.expressions());
		return true;
	}


	/*
	 * @see ASTVisitor#visit(AssertStatement)
	 */
	public boolean visit(AssertStatement node) {
		node.getExpression().accept(this);
		if (isInserted(node.getMessage())) {
			node.setMessage(null);
		}		
		return true;
	}

	/*
	 * @see ASTVisitor#visit(Block)
	 */
	public boolean visit(Block node) {
		clearList(node.statements());
		return true;
	}

	/*
	 * @see ASTVisitor#visit(BreakStatement)
	 */
	public boolean visit(BreakStatement node) {
		if (isInserted(node.getLabel())) {
			node.setLabel(null);
		}		
		return true;
	}

	/*
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		if (isInserted(node.getExpression())) {
			node.setExpression(null);
		}	
		clearList(node.arguments());
		if (isInserted(node.getAnonymousClassDeclaration())) {
			node.setAnonymousClassDeclaration(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(CompilationUnit)
	 */
	public boolean visit(CompilationUnit node) {
		if (isInserted(node.getPackage())) {
			node.setPackage(null);
		}
		clearList(node.imports());
		clearList(node.types());
		return true;
	}


	/*
	 * @see ASTVisitor#visit(ConstructorInvocation)
	 */
	public boolean visit(ConstructorInvocation node) {
		clearList(node.arguments());
		return true;
	}

	/*
	 * @see ASTVisitor#visit(ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		if (isInserted(node.getLabel())) {
			node.setLabel(null);
		}		
		return true;
	}

	/*
	 * @see ASTVisitor#visit(FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		if (isInserted(node.getJavadoc())) {
			node.setJavadoc(null);
		}
		clearList(node.fragments());
		return true;
	}

	/*
	 * @see ASTVisitor#visit(ForStatement)
	 */
	public boolean visit(ForStatement node) {
		clearList(node.initializers());
		clearList(node.updaters());
		if (isInserted(node.getExpression())) {
			node.setExpression(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(IfStatement)
	 */
	public boolean visit(IfStatement node) {
		if (isInserted(node.getElseStatement())) {
			node.setElseStatement(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		clearList(node.extendedOperands());
		return true;
	}

	/*
	 * @see ASTVisitor#visit(Initializer)
	 */
	public boolean visit(Initializer node) {
		if (isInserted(node.getJavadoc())) {
			node.setJavadoc(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration node) {
		if (isInserted(node.getJavadoc())) {
			node.setJavadoc(null);
		}
		if (isInserted(node.getReturnType())) {
			node.setReturnType(node.getAST().newPrimitiveType(PrimitiveType.VOID));
		}	
		clearList(node.parameters());
		clearList(node.thrownExceptions());
		if (isInserted(node.getBody())) {
			node.setBody(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(MethodInvocation)
	 */
	public boolean visit(MethodInvocation node) {
		if (isInserted(node.getExpression())) {
			node.setExpression(null);
		}
		clearList(node.arguments());
		return true;
	}

	/*
	 * @see ASTVisitor#visit(ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		if (isInserted(node.getExpression())) {
			node.setExpression(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(SingleVariableDeclaration)
	 */
	public boolean visit(SingleVariableDeclaration node) {
		if (isInserted(node.getInitializer())) {
			node.setInitializer(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		if (isInserted(node.getExpression())) {
			node.setExpression(null);
		}
		clearList(node.arguments());
		return true;
	}

	/*
	 * @see ASTVisitor#visit(SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		if (isInserted(node.getQualifier())) {
			node.setQualifier(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		if (isInserted(node.getQualifier())) {
			node.setQualifier(null);
		}
		clearList(node.arguments());
		return true;
	}

	/*
	 * @see ASTVisitor#visit(SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		if (isInserted(node.getExpression())) {
			node.setExpression(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		clearList(node.statements());
		return true;
	}

	/*
	 * @see ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		if (isInserted(node.getQualifier())) {
			node.setQualifier(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(TryStatement)
	 */
	public boolean visit(TryStatement node) {
		clearList(node.catchClauses());
		if (isInserted(node.getFinally())) {
			node.setFinally(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		if (isInserted(node.getJavadoc())) {
			node.setJavadoc(null);
		}
		if (isInserted(node.getSuperclass())) {
			node.setSuperclass(null);
		}
		clearList(node.superInterfaces());
		clearList(node.bodyDeclarations());
		return true;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationExpression)
	 */
	public boolean visit(VariableDeclarationExpression node) {
		clearList(node.fragments());
		return true;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		if (isInserted(node.getInitializer())) {
			node.setInitializer(null);
		}
		return true;
	}

	/*
	 * @see ASTVisitor#visit(VariableDeclarationStatement)
	 */
	public boolean visit(VariableDeclarationStatement node) {
		clearList(node.fragments());
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#postVisit(org.eclipse.jdt.core.dom.ASTNode)
	 */
	public void postVisit(ASTNode node) {
		if (fRewrite.isInserted(node)) {
			Assert.isTrue(false, "Inserted node not removed " + node + ", parent: " + node.getParent()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}
