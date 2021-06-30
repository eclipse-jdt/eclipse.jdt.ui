/*******************************************************************************
 * Copyright (c) 2021 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class PrimitiveRatherThanWrapperOperation extends CompilationUnitRewriteOperation {
	private final VariableDeclarationStatement visited;
	private final String primitiveTypeName;
	private final String wrapperFullyQualifiedName;
	private final Expression initializer;
	private final List<MethodInvocation> toStringMethods;
	private final List<MethodInvocation> compareToMethods;
	private final List<MethodInvocation> primitiveValueMethods;
	private final String parsingMethodName;

	public PrimitiveRatherThanWrapperOperation(
			final VariableDeclarationStatement visited,
			final String primitiveTypeName,
			final String wrapperFullyQualifiedName,
			final Expression initializer,
			final List<MethodInvocation> toStringMethods,
			final List<MethodInvocation> compareToMethods,
			final List<MethodInvocation> primitiveValueMethods,
			final String parsingMethodName) {
		this.visited= visited;
		this.primitiveTypeName= primitiveTypeName;
		this.wrapperFullyQualifiedName= wrapperFullyQualifiedName;
		this.initializer= initializer;
		this.toStringMethods= toStringMethods;
		this.compareToMethods= compareToMethods;
		this.primitiveValueMethods= primitiveValueMethods;
		this.parsingMethodName= parsingMethodName;
	}

	@Override
	public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();
		TextEditGroup group= createTextEditGroup(MultiFixMessages.PrimitiveRatherThanWrapperCleanUp_description, cuRewrite);
		rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
			@Override
			public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
				if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
					return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
				}

				return super.computeSourceRange(nodeWithComment);
			}
		});

		MethodInvocation methodInvocation= ASTNodes.as(initializer, MethodInvocation.class);

		if (methodInvocation != null) {
			if (ASTNodes.usesGivenSignature(methodInvocation, wrapperFullyQualifiedName, "valueOf", primitiveTypeName)) { //$NON-NLS-1$
				rewrite.replace(methodInvocation, ASTNodes.createMoveTarget(rewrite, (Expression) methodInvocation.arguments().get(0)), group);
			}

			if (ASTNodes.usesGivenSignature(methodInvocation, wrapperFullyQualifiedName, "valueOf", String.class.getCanonicalName()) //$NON-NLS-1$
					|| ASTNodes.usesGivenSignature(methodInvocation, wrapperFullyQualifiedName, "valueOf", String.class.getCanonicalName(), int.class.getSimpleName())) { //$NON-NLS-1$
				rewrite.set(methodInvocation, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(parsingMethodName), group);
			}
		}

		ClassInstanceCreation classInstanceCreation= ASTNodes.as(initializer, ClassInstanceCreation.class);

		if (classInstanceCreation != null) {
			List<Expression> classInstanceCreationArguments= classInstanceCreation.arguments();

			if (classInstanceCreationArguments.size() == 1
					&& ASTNodes.hasType(classInstanceCreation, wrapperFullyQualifiedName)) {
				Expression arg0= classInstanceCreationArguments.get(0);

				if (ASTNodes.hasType(arg0, String.class.getCanonicalName()) && parsingMethodName != null) {
					MethodInvocation newMethodInvocation= ast.newMethodInvocation();
					newMethodInvocation.setExpression((Expression) rewrite.createCopyTarget(((SimpleType) visited.getType()).getName()));
					newMethodInvocation.setName(ast.newSimpleName(parsingMethodName));
					newMethodInvocation.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(arg0)));

					ASTNodes.replaceButKeepComment(rewrite, initializer, newMethodInvocation, group);
				} else if (ASTNodes.hasType(arg0, primitiveTypeName)) {
					Expression newExpression= (Expression)rewrite.createCopyTarget(arg0);
					ASTNodes.replaceButKeepComment(rewrite, classInstanceCreation, newExpression, group);
				}
			}
		}

		for (MethodInvocation primitiveValueMethod : primitiveValueMethods) {
			rewrite.replace(primitiveValueMethod, ASTNodes.createMoveTarget(rewrite, primitiveValueMethod.getExpression()), group);
		}

		for (MethodInvocation toStringMethod : toStringMethods) {
			Type wrapperType= (Type) rewrite.createCopyTarget(visited.getType());

			ListRewrite targetListRewrite= rewrite.getListRewrite(toStringMethod, MethodInvocation.ARGUMENTS_PROPERTY);
			targetListRewrite.insertFirst(ASTNodes.createMoveTarget(rewrite, toStringMethod.getExpression()), group);
			rewrite.set(toStringMethod, MethodInvocation.EXPRESSION_PROPERTY, wrapperType, group);
		}

		for (MethodInvocation compareToMethod : compareToMethods) {
			Type wrapperType= (Type) rewrite.createCopyTarget(visited.getType());

			ListRewrite targetListRewrite= rewrite.getListRewrite(compareToMethod, MethodInvocation.ARGUMENTS_PROPERTY);
			targetListRewrite.insertFirst(ASTNodes.createMoveTarget(rewrite, compareToMethod.getExpression()), group);
			rewrite.set(compareToMethod, MethodInvocation.EXPRESSION_PROPERTY, wrapperType, group);
			rewrite.replace(compareToMethod.getName(), ast.newSimpleName("compare"), group); //$NON-NLS-1$
		}

		Type newPrimitiveType= ast.newPrimitiveType(PrimitiveType.toCode(primitiveTypeName));

		ASTNodes.replaceButKeepComment(rewrite, visited.getType(), newPrimitiveType, group);
	}
}
