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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class PullOutIfFromIfElseFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class PullOutIfFromIfElseFinder extends ASTVisitor {
		private List<CompilationUnitRewriteOperation> fResult;

		public PullOutIfFromIfElseFinder(List<CompilationUnitRewriteOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final IfStatement visited) {
			IfStatement thenInnerIfStatement= ASTNodes.as(visited.getThenStatement(), IfStatement.class);
			IfStatement elseInnerIfStatement= ASTNodes.as(visited.getElseStatement(), IfStatement.class);

			if (thenInnerIfStatement != null
					&& elseInnerIfStatement != null
					&& thenInnerIfStatement.getElseStatement() == null
					&& elseInnerIfStatement.getElseStatement() == null
					&& ASTNodes.isPassive(visited.getExpression())
					&& ASTNodes.isPassive(thenInnerIfStatement.getExpression())
					&& ASTNodes.match(thenInnerIfStatement.getExpression(), elseInnerIfStatement.getExpression())) {
				fResult.add(new PullOutIfFromIfElseOperation(visited, thenInnerIfStatement, elseInnerIfStatement));
				return false;
			}

			return true;
		}
	}

	private static class PullOutIfFromIfElseOperation extends CompilationUnitRewriteOperation {
		private final IfStatement visited;
		private final IfStatement thenInnerIfStatement;
		private final IfStatement elseInnerIfStatement;

		public PullOutIfFromIfElseOperation(final IfStatement visited, final IfStatement thenInnerIfStatement, final IfStatement elseInnerIfStatement) {
			this.visited= visited;
			this.thenInnerIfStatement= thenInnerIfStatement;
			this.elseInnerIfStatement= elseInnerIfStatement;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PullOutIfFromIfElseCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			Expression newCondition= ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(thenInnerIfStatement.getExpression()));
			IfStatement newIfStatement= ast.newIfStatement();
			newIfStatement.setExpression(newCondition);
			Block newBlock= ast.newBlock();
			newBlock.statements().add(ASTNodes.createMoveTarget(rewrite, visited));
			newIfStatement.setThenStatement(newBlock);

			ASTNodes.replaceButKeepComment(rewrite, visited, newIfStatement, group);
			ASTNodes.replaceButKeepComment(rewrite, visited.getThenStatement(), ASTNodes.createMoveTarget(rewrite, thenInnerIfStatement.getThenStatement()), group);
			ASTNodes.replaceButKeepComment(rewrite, visited.getElseStatement(), ASTNodes.createMoveTarget(rewrite, elseInnerIfStatement.getThenStatement()), group);
		}
	}

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		PullOutIfFromIfElseFinder finder= new PullOutIfFromIfElseFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperation[0]);
		return new PullOutIfFromIfElseFixCore(FixMessages.PullOutIfFromIfElseFix_description, compilationUnit, ops);
	}

	protected PullOutIfFromIfElseFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
