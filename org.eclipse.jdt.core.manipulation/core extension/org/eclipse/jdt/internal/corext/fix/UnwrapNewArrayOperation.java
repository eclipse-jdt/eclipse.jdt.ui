/*******************************************************************************
 * Copyright (c) 2019, 2024 Red Hat Inc. and others.
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

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSElement;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSLine;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSUtil;
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
		List<Expression> expressionsInArray= node != null && node.getInitializer() != null && node.getInitializer().expressions() != null ?
				node.getInitializer().expressions() : Collections.EMPTY_LIST;
		boolean isTagged[]= new boolean[expressionsInArray.size()];
		ICompilationUnit cu= cuRewrite.getCu();

		boolean tagged= false;
		for (int i= 0; i < expressionsInArray.size(); ++i) {
			Expression operand= expressionsInArray.get(i);
			NLSLine nlsLine= NLSUtil.scanCurrentLine(cu, operand.getStartPosition());
			if (nlsLine != null) {
				for (NLSElement element : nlsLine.getElements()) {
					if (element.getPosition().getOffset() == operand.getStartPosition()) {
						if (element.hasTag()) {
							tagged= true;
							isTagged[i]= true;
						}
					}
				}
			}
		}

		TextEditGroup group= createTextEditGroup(FixMessages.UnusedCodeFix_RemoveUnnecessaryArrayCreation_description, cuRewrite);

		if (!tagged) {
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

			if (node.getInitializer() != null && node.getInitializer().expressions() != null) {
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
		} else {
			// to avoid replacing/calculating non-NLS markers, simply remove the array declaration and it's braces
			// from the original call
			StringBuilder buf= new StringBuilder();
			CompilationUnit comp= (CompilationUnit)call.getRoot();
			int nodeStart= comp.getExtendedStartPosition(call);
			int nodeEnd= comp.getExtendedStartPosition(call) + comp.getExtendedLength(call);
			int arrayStart= node.getStartPosition();
			List<Expression> expressionList= node.getInitializer().expressions();
			int arrayExpressionStart= expressionList.get(0).getStartPosition();
			Expression lastExpression= expressionList.get(expressionList.size() - 1);
			int arrayExpressionEnd= lastExpression.getStartPosition() + lastExpression.getLength();
			int arrayInitializerEnd= node.getInitializer().getStartPosition() + node.getInitializer().getLength();
			buf.append(cu.getBuffer().getText(nodeStart, arrayStart - nodeStart));
			buf.append(cu.getBuffer().getText(arrayExpressionStart, arrayExpressionEnd - arrayExpressionStart));
			buf.append(cu.getBuffer().getText(arrayInitializerEnd, nodeEnd - arrayInitializerEnd));

			ASTNode replacementNode= null;
			if (call instanceof ClassInstanceCreation) {
				replacementNode= rewrite.createStringPlaceholder(buf.toString(),  ASTNode.CLASS_INSTANCE_CREATION);
			} else if (call instanceof MethodInvocation) {
				replacementNode= rewrite.createStringPlaceholder(buf.toString(),  ASTNode.METHOD_INVOCATION);
			} else if (call instanceof SuperMethodInvocation) {
				replacementNode= rewrite.createStringPlaceholder(buf.toString(),  ASTNode.SUPER_METHOD_INVOCATION);
			} else {
				return;
			}

			rewrite.replace(call, replacementNode, group);
		}
	}
}

