/*******************************************************************************
 * Copyright (c) 2019, 2020 Red Hat Inc. and others.
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

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

/**
 * Unwrap a new array with initializer used as input for varargs and replace with initializer elements.
 */
public class UnwrapNewArrayOperation extends CompilationUnitRewriteOperation {
	private final ArrayCreation node;
	private final Expression call;

	public UnwrapNewArrayOperation(ArrayCreation node, Expression call) {
		this.node= node;
		this.call= call;
	}

	@Override
	public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();

		ListRewrite listRewrite;
		if (call instanceof ClassInstanceCreation) {
			listRewrite= rewrite.getListRewrite(call, ClassInstanceCreation.ARGUMENTS_PROPERTY);
		} else if (call instanceof MethodInvocation) {
			listRewrite= rewrite.getListRewrite(call, MethodInvocation.ARGUMENTS_PROPERTY);
		} else if (call instanceof SuperMethodInvocation) {
			listRewrite= rewrite.getListRewrite(call, SuperMethodInvocation.ARGUMENTS_PROPERTY);
		} else {
			return;
		}

		TextEditGroup group= createTextEditGroup(FixMessages.UnusedCodeFix_RemoveUnnecessaryArrayCreation_description, cuRewrite);

		if (node.getInitializer() != null && node.getInitializer().expressions() != null) {
			List<Expression> expressionsInArray= node.getInitializer().expressions();

			for (int i= 1; i < expressionsInArray.size(); i++) {
				listRewrite.insertLast(ASTNodes.createMoveTarget(rewrite, expressionsInArray.get(i)), group);
			}

			if (expressionsInArray.isEmpty()) {
				listRewrite.remove(node, group);
			} else {
				listRewrite.replace(node, ASTNodes.createMoveTarget(rewrite, expressionsInArray.get(0)), group);
			}
		} else {
			listRewrite.remove(node, group);
		}
	}
}

