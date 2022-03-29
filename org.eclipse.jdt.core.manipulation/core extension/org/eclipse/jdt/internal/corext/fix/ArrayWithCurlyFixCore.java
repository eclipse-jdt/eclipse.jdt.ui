/*******************************************************************************
 * Copyright (c) 2021, 2022 Fabrice TIERCELIN and others.
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
 *     Christian Femers - Bug 579471
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class ArrayWithCurlyFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class ArrayWithCurlyFinder extends ASTVisitor {
		private List<ArrayWithCurlyFixOperation> fResult;

		public ArrayWithCurlyFinder(List<ArrayWithCurlyFixOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final ArrayCreation visited) {
			if (visited.getInitializer() != null || isVoid(visited)) {
				ITypeBinding arrayType= visited.resolveTypeBinding();
				ITypeBinding destinationType= ASTNodes.getTargetType(visited);

				if (arrayType != null
						&& Objects.equals(arrayType, destinationType)
						&& isDestinationAllowed(visited)) {
					fResult.add(new ArrayWithCurlyFixOperation(visited));
					return false;
				}
			}

			return true;
		}

		private boolean isVoid(final ArrayCreation visited) {
			List<Expression> dimensions= visited.dimensions();

			for (Expression dimension : dimensions) {
				if (!Long.valueOf(0L).equals(ASTNodes.getIntegerLiteral(dimension))) {
					return false;
				}
			}

			return true;
		}

		private boolean isDestinationAllowed(final ASTNode visited) {
			ASTNode parent= visited.getParent();
			int parentType= parent.getNodeType();

			boolean correctParent= parentType == ASTNode.FIELD_DECLARATION
					|| parentType == ASTNode.VARIABLE_DECLARATION_EXPRESSION
					|| parentType == ASTNode.VARIABLE_DECLARATION_FRAGMENT
					|| parentType == ASTNode.VARIABLE_DECLARATION_STATEMENT;
			if (!correctParent) {
				return false;
			}
			if (parent instanceof VariableDeclaration) {
				Type type= ASTNodes.getType((VariableDeclaration) parent);
				return type == null || !type.isVar();
			}
			return true;
		}
	}

	public static class ArrayWithCurlyFixOperation extends CompilationUnitRewriteOperation {
		private final ArrayCreation visited;

		public ArrayWithCurlyFixOperation(final ArrayCreation visited) {
			this.visited= visited;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ArrayWithCurlyCleanup_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			if (visited.getInitializer() != null) {
				ASTNodes.replaceButKeepComment(rewrite, visited, ASTNodes.createMoveTarget(rewrite, visited.getInitializer()), group);
			} else {
				ASTNodes.replaceButKeepComment(rewrite, visited, ast.newArrayInitializer(), group);
			}
		}
	}


	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<ArrayWithCurlyFixOperation> operations= new ArrayList<>();
		ArrayWithCurlyFinder finder= new ArrayWithCurlyFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new ArrayWithCurlyFixCore(FixMessages.ArrayWithCurlyFix_description, compilationUnit, ops);
	}

	protected ArrayWithCurlyFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
