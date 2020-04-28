/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

/**
 * Unwrap a new array with initializer used as input for varargs and replace with initializer elements
 *
 */
public class UnwrapNewArrayOperation extends CompilationUnitRewriteOperation {
	private final ArrayCreation node;

	private final ASTNode call;

	public UnwrapNewArrayOperation(ArrayCreation node, MethodInvocation method) {
		this.node= node;
		this.call= method;
	}

	public UnwrapNewArrayOperation(ArrayCreation node, SuperMethodInvocation superMethod) {
		this.node= node;
		this.call= superMethod;
	}

	@Override
	public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();

		if (call instanceof MethodInvocation) {
			MethodInvocation method= (MethodInvocation)call;
			MethodInvocation newMethod= ast.newMethodInvocation();
			newMethod.setSourceRange(method.getStartPosition(), method.getLength());
			newMethod.setName(ast.newSimpleName(method.getName().getFullyQualifiedName()));
			newMethod.setExpression(ASTNodes.copySubtree(ast, method.getExpression()));
			if (method.typeArguments() != null) {
				@SuppressWarnings("unchecked")
				List<?> createMoveTarget= ASTNodes.createMoveTarget(rewrite, method.typeArguments());
				newMethod.typeArguments().addAll(createMoveTarget);
			}
			for (int i= 0; i < method.arguments().size() - 1; ++i) {
				newMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, (Expression) method.arguments().get(i)));
			}
			ArrayInitializer initializer= node.getInitializer();
			if (initializer != null && initializer.expressions() != null) {
				for (Object exp : initializer.expressions()) {
					newMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, (Expression) exp));
				}
			}
			rewrite.replace(method, newMethod, null);
		} else if (call instanceof SuperMethodInvocation) {
			SuperMethodInvocation method= (SuperMethodInvocation)call;
			SuperMethodInvocation newSuperMethod= ast.newSuperMethodInvocation();
			newSuperMethod.setSourceRange(method.getStartPosition(), method.getLength());
			newSuperMethod.setName(ast.newSimpleName(method.getName().getFullyQualifiedName()));
			if (method.typeArguments() != null) {
				@SuppressWarnings("unchecked")
				List<?> createMoveTarget= ASTNodes.createMoveTarget(rewrite, method.typeArguments());
				newSuperMethod.typeArguments().addAll(createMoveTarget);
			}
			for (int i= 0; i < method.arguments().size() - 1; ++i) {
				newSuperMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, (Expression) method.arguments().get(i)));
			}
			ArrayInitializer initializer= node.getInitializer();
			if (initializer != null && initializer.expressions() != null) {
				for (Object exp : initializer.expressions()) {
					newSuperMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, (Expression) exp));
				}
			}
			rewrite.replace(method, newSuperMethod, null);

		}
	}
}

