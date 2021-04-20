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
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.VarDefinitionsUsesVisitor;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class ReturnExpressionFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class ReturnExpressionFinder extends ASTVisitor {
		private List<CompilationUnitRewriteOperation> fResult;

		public ReturnExpressionFinder(List<CompilationUnitRewriteOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final Block visited) {
			ReturnStatementVisitor returnStatementVisitor= new ReturnStatementVisitor(visited);
			visited.accept(returnStatementVisitor);
			return returnStatementVisitor.result;
		}

		final class ReturnStatementVisitor extends ASTVisitor {
			private final Block startNode;
			private boolean result= true;

			public ReturnStatementVisitor(final Block startNode) {
				this.startNode= startNode;
			}

			@Override
			public boolean visit(final Block visited) {
				return startNode == visited;
			}

			@Override
			public boolean visit(final ReturnStatement visited) {
				if (result) {
					Statement previousSibling= ASTNodes.getPreviousSibling(visited);

					if (previousSibling instanceof VariableDeclarationStatement) {
						VariableDeclarationStatement variableDeclarationStatement= (VariableDeclarationStatement) previousSibling;
						VariableDeclarationFragment fragment= ASTNodes.getUniqueFragment(variableDeclarationStatement);

						if (fragment != null && ASTNodes.isSameLocalVariable(visited.getExpression(), fragment.getName())) {
							Expression returnExpression= fragment.getInitializer();

							if (returnExpression instanceof ArrayInitializer) {
								return maybeRemoveArrayVariable(visited, variableDeclarationStatement, (ArrayInitializer) returnExpression);
							}

							fResult.add(new ReturnExpressionOperation(visited, variableDeclarationStatement, returnExpression));
							result= false;
							return false;
						}
					} else {
						Assignment assignment= ASTNodes.asExpression(previousSibling, Assignment.class);

						if (ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)
								&& assignment.getLeftHandSide() instanceof Name
								&& ASTNodes.isSameLocalVariable(visited.getExpression(), assignment.getLeftHandSide())
								&& !isUsedAfterReturn((IVariableBinding) ((Name) assignment.getLeftHandSide()).resolveBinding(), visited)) {
							fResult.add(new ReturnExpressionOperation(visited, previousSibling, assignment.getRightHandSide()));
							result= false;
							return false;
						}
					}
				}

				return true;
			}

			private boolean isUsedAfterReturn(final IVariableBinding varToSearch, final ASTNode scopeNode) {
				TryStatement tryStatement= ASTNodes.getTypedAncestor(scopeNode, TryStatement.class);

				if (tryStatement == null) {
					return false;
				}

				if (tryStatement.getFinally() != null) {
					VarDefinitionsUsesVisitor variableUseVisitor;
					try {
						variableUseVisitor= new VarDefinitionsUsesVisitor(varToSearch, tryStatement.getFinally(), true);
					} catch (Exception e) {
						return true;
					}

					if (!variableUseVisitor.getReads().isEmpty()) {
						return true;
					}
				}

				return isUsedAfterReturn(varToSearch, tryStatement);
			}

			private boolean maybeRemoveArrayVariable(final ReturnStatement visited, final VariableDeclarationStatement variableDeclarationStatement, final ArrayInitializer returnExpression) {
				Type varType= variableDeclarationStatement.getType();
				VariableDeclarationFragment varDeclFrag= (VariableDeclarationFragment) variableDeclarationStatement.fragments().get(0);

				if (varType instanceof ArrayType) {
					ArrayType arrayType= (ArrayType) varType;

					// Mixed c style/var style not supported yet. Abort instead of generating wrong code
					if (varDeclFrag.getExtraDimensions() > 0) {
						return true;
					}

					fResult.add(new ReturnExpressionJavaStyleArrayOperation(visited, variableDeclarationStatement, returnExpression, arrayType));
				} else {
					fResult.add(new ReturnExpressionCStyleArrayOperation(visited, variableDeclarationStatement, returnExpression, varDeclFrag));
				}

				result= false;
				return false;
			}
		}
	}

	private static class ReturnExpressionOperation extends CompilationUnitRewriteOperation {
		private final ReturnStatement visited;
		private final Statement previousSibling;
		private final Expression returnExpression;

		public ReturnExpressionOperation(final ReturnStatement visited, final Statement previousSibling,
				final Expression returnExpression) {
			this.visited= visited;
			this.previousSibling= previousSibling;
			this.returnExpression= returnExpression;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ReturnExpressionCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			rewrite.remove(previousSibling, group);
			ReturnStatement newReturnStatement= ast.newReturnStatement();
			newReturnStatement.setExpression(ASTNodes.createMoveTarget(rewrite, returnExpression));
			ASTNodes.replaceButKeepComment(rewrite, visited, newReturnStatement, group);
		}
	}

	private static class ReturnExpressionJavaStyleArrayOperation extends CompilationUnitRewriteOperation {
		private final ReturnStatement visited;
		private final VariableDeclarationStatement variableDeclarationStatement;
		private final ArrayInitializer returnExpression;
		private final ArrayType arrayType;

		public ReturnExpressionJavaStyleArrayOperation(final ReturnStatement visited, final VariableDeclarationStatement variableDeclarationStatement, final ArrayInitializer returnExpression,
				final ArrayType arrayType) {
			this.visited= visited;
			this.variableDeclarationStatement= variableDeclarationStatement;
			this.returnExpression= returnExpression;
			this.arrayType= arrayType;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ReturnExpressionCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			// Java style array "Type[] var"
			ArrayCreation newArrayCreation= ast.newArrayCreation();
			ArrayType newArrayType= visited.getAST().newArrayType((Type) rewrite.createCopyTarget(arrayType.getElementType()), arrayType.getDimensions());
			newArrayCreation.setType(newArrayType);
			newArrayCreation.setInitializer(ASTNodes.createMoveTarget(rewrite, returnExpression));

			ReturnStatement newReturnStatement= ast.newReturnStatement();
			newReturnStatement.setExpression(newArrayCreation);
			rewrite.remove(variableDeclarationStatement, group);
			ASTNodes.replaceButKeepComment(rewrite, visited, newReturnStatement, group);
		}
	}

	private static class ReturnExpressionCStyleArrayOperation extends CompilationUnitRewriteOperation {
		private final ReturnStatement visited;
		private final VariableDeclarationStatement variableDeclarationStatement;
		private final ArrayInitializer returnExpression;
		private final VariableDeclarationFragment varDeclFrag;

		public ReturnExpressionCStyleArrayOperation(final ReturnStatement visited, final VariableDeclarationStatement variableDeclarationStatement, final ArrayInitializer returnExpression,
				final VariableDeclarationFragment varDeclFrag) {
			this.visited= visited;
			this.variableDeclarationStatement= variableDeclarationStatement;
			this.returnExpression= returnExpression;
			this.varDeclFrag= varDeclFrag;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ReturnExpressionCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			// C style array "Type var[]"
			ArrayType newArrayType= visited.getAST().newArrayType((Type) rewrite.createCopyTarget(variableDeclarationStatement.getType()), varDeclFrag.getExtraDimensions());
			ArrayCreation newArrayCreation= ast.newArrayCreation();
			newArrayCreation.setType(newArrayType);
			newArrayCreation.setInitializer(ASTNodes.createMoveTarget(rewrite, returnExpression));

			ReturnStatement newReturnStatement= ast.newReturnStatement();
			newReturnStatement.setExpression(newArrayCreation);
			rewrite.remove(variableDeclarationStatement, group);
			ASTNodes.replaceButKeepComment(rewrite, visited, newReturnStatement, group);
		}
	}

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		ReturnExpressionFinder finder= new ReturnExpressionFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new ReturnExpressionFixCore(FixMessages.ReturnExpressionFix_description, compilationUnit, ops);
	}

	protected ReturnExpressionFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
