/*******************************************************************************
 * Copyright (c) 2020 Fabrice TIERCELIN and others.
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
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.InterruptibleVisitor;
import org.eclipse.jdt.internal.corext.dom.VarConflictVisitor;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that adds a break to avoid passive for loop iterations.
 *
 * The conditions of triggering are:
 * <ul>
 *  <li>The structure of the code (for loop, including an if, including assignments)</li>
 *  <li>Two cases of reject:
 *   <ul>
 *    <li>The inner assignments should not do other different assignments in the future (assign other values or assign into other variables),</li>
 *    <li>No side effects after the first assignments.</li>
 *   </ul>
 *  </li>
 * </ul>
 */
public class BreakLoopCleanUp extends AbstractMultiFix {
	public BreakLoopCleanUp() {
		this(Collections.emptyMap());
	}

	public BreakLoopCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.BREAK_LOOP);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.BREAK_LOOP)) {
			return new String[] { MultiFixMessages.BreakLoopCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("boolean isFound = false;\n"); //$NON-NLS-1$
		bld.append("for (int i = 0; i < number; i++) {\n"); //$NON-NLS-1$
		bld.append("    if (i == 42) {\n"); //$NON-NLS-1$
		bld.append("        isFound = true;\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.BREAK_LOOP)) {
			bld.append("        break;\n"); //$NON-NLS-1$
		}

		bld.append("    }\n"); //$NON-NLS-1$
		bld.append("}\n"); //$NON-NLS-1$

		if (!isEnabled(CleanUpConstants.BREAK_LOOP)) {
			bld.append("\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.BREAK_LOOP)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			final class DisturbingEffectVisitor extends InterruptibleVisitor {
				private final Set<SimpleName> localVariableNames;
				private boolean hasDisturbingEffect;

				private DisturbingEffectVisitor(final Set<SimpleName> localVariableNames) {
					this.localVariableNames= localVariableNames;
				}

				private boolean hasDisturbingEffect() {
					return hasDisturbingEffect;
				}

				@Override
				public boolean visit(final Assignment node) {
					if (!ASTNodes.hasOperator(node, Assignment.Operator.ASSIGN)) {
						hasDisturbingEffect= true;
						return interruptVisit();
					}

					return visitVar(node.getLeftHandSide());
				}

				private boolean visitVar(final Expression modifiedVar) {
					if (!(modifiedVar instanceof SimpleName)) {
						hasDisturbingEffect= true;
						return interruptVisit();
					}

					boolean isFound= false;

					for (SimpleName localVariableName : localVariableNames) {
						if (ASTNodes.isSameVariable(localVariableName, modifiedVar)) {
							isFound= true;
							break;
						}
					}

					if (!isFound) {
						hasDisturbingEffect= true;
						return interruptVisit();
					}

					return true;
				}

				@Override
				public boolean visit(final PrefixExpression node) {
					if (ASTNodes.hasOperator(node, PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.DECREMENT)) {
						return visitVar(node.getOperand());
					}

					return true;
				}

				@Override
				public boolean visit(final PostfixExpression node) {
					return visitVar(node.getOperand());
				}

				@Override
				public boolean visit(final InfixExpression node) {
					if (ASTNodes.hasOperator(node, InfixExpression.Operator.PLUS) && ASTNodes.hasType(node, String.class.getCanonicalName())
							&& (mayCallImplicitToString(node.getLeftOperand())
									|| mayCallImplicitToString(node.getRightOperand())
									|| mayCallImplicitToString(node.extendedOperands()))) {
						hasDisturbingEffect= true;
						return interruptVisit();
					}

					return true;
				}

				private boolean mayCallImplicitToString(final List<Expression> extendedOperands) {
					if (extendedOperands != null) {
						for (Expression expression : extendedOperands) {
							if (mayCallImplicitToString(expression)) {
								return true;
							}
						}
					}

					return false;
				}

				private boolean mayCallImplicitToString(final Expression expression) {
					return !ASTNodes.hasType(expression, String.class.getCanonicalName(), boolean.class.getSimpleName(), short.class.getSimpleName(), int.class.getSimpleName(), long.class.getSimpleName(), float.class.getSimpleName(), double.class.getSimpleName(),
							Short.class.getCanonicalName(), Boolean.class.getCanonicalName(), Integer.class.getCanonicalName(), Long.class.getCanonicalName(), Float.class.getCanonicalName(),
							Double.class.getCanonicalName()) && !(expression instanceof PrefixExpression) && !(expression instanceof InfixExpression)
							&& !(expression instanceof PostfixExpression);
				}

				@Override
				public boolean visit(final SuperMethodInvocation node) {
					hasDisturbingEffect= true;
					return interruptVisit();
				}

				@Override
				public boolean visit(final MethodInvocation node) {
					hasDisturbingEffect= true;
					return interruptVisit();
				}

				@Override
				public boolean visit(final ClassInstanceCreation node) {
					hasDisturbingEffect= true;
					return interruptVisit();
				}

				@Override
				public boolean visit(final ThrowStatement node) {
					hasDisturbingEffect= true;
					return interruptVisit();
				}

				@Override
				public boolean visit(final ReturnStatement node) {
					hasDisturbingEffect= true;
					return interruptVisit();
				}

				@Override
				public boolean visit(final BreakStatement node) {
					if (node.getLabel() != null) {
						hasDisturbingEffect= true;
						return interruptVisit();
					}

					return true;
				}
			}

			@Override
			public boolean visit(final ForStatement node) {
				Set<SimpleName> vars= new HashSet<>();
				List<Expression> initializers= node.initializers();

				for (Expression initializer : initializers) {
					vars.addAll(ASTNodes.getLocalVariableIdentifiers(initializer, true));
				}

				if (node.getExpression() == null
						|| hasDisturbingEffect(node.getExpression(), vars)
						|| node.updaters().isEmpty()) {
					return true;
				}

				List<Expression> updaters= node.updaters();

				for (Expression updater : updaters) {
					if (hasDisturbingEffect(updater, vars)) {
						return true;
					}
				}

				return visitLoopBody(node.getBody(), vars);
			}

			private boolean hasDisturbingEffect(final ASTNode node, final Set<SimpleName> allowedVars) {
				DisturbingEffectVisitor variableUseVisitor= new DisturbingEffectVisitor(allowedVars);
				variableUseVisitor.traverseNodeInterruptibly(node);
				return variableUseVisitor.hasDisturbingEffect();
			}

			@Override
			public boolean visit(final EnhancedForStatement node) {
				if (ASTNodes.isArray(node.getExpression())) {
					Set<SimpleName> vars= new HashSet<>();
					vars.add(node.getParameter().getName());
					return visitLoopBody(node.getBody(), vars);
				}

				return true;
			}

			private boolean visitLoopBody(final Statement body, final Set<SimpleName> allowedVars) {
				List<Statement> statements= ASTNodes.asList(body);

				if (statements == null || statements.isEmpty()) {
					return true;
				}

				for (int i= 0; i < statements.size() - 1; i++) {
					Statement statement= statements.get(i);
					allowedVars.addAll(ASTNodes.getLocalVariableIdentifiers(statement, true));

					if (hasDisturbingEffect(statement, allowedVars)) {
						return true;
					}
				}

				IfStatement ifStatement= ASTNodes.as(statements.get(statements.size() - 1), IfStatement.class);

				if (ifStatement != null && ifStatement.getElseStatement() == null && !hasDisturbingEffect(ifStatement.getExpression(), allowedVars)) {
					List<Statement> assignments= ASTNodes.asList(ifStatement.getThenStatement());

					if (areAssignmentsValid(allowedVars, assignments)) {
						rewriteOperations.add(new BreakLoopOperation(ifStatement));
						return false;
					}
				}

				return true;
			}

			private boolean areAssignmentsValid(final Set<SimpleName> allowedVars, final List<Statement> assignments) {
				if (assignments.isEmpty()) {
					return false;
				}

				for (Statement statement : assignments) {
					VariableDeclarationStatement variableDeclaration= ASTNodes.as(statement, VariableDeclarationStatement.class);
					Assignment assignment= ASTNodes.asExpression(statement, Assignment.class);

					if (variableDeclaration != null) {
						for (Object obj : variableDeclaration.fragments()) {
							VariableDeclarationFragment fragment= (VariableDeclarationFragment) obj;

							if (!ASTNodes.isHardCoded(fragment.getInitializer())) {
								return false;
							}
						}
					} else if (assignment != null
							&& ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)
							&& ASTNodes.isHardCoded(assignment.getRightHandSide())
							&& ASTNodes.isPassive(assignment.getLeftHandSide())) {
						VarConflictVisitor varOccurrenceVisitor= new VarConflictVisitor(allowedVars, true);
						varOccurrenceVisitor.traverseNodeInterruptibly(assignment.getLeftHandSide());

						if (varOccurrenceVisitor.isVarConflicting()) {
							return false;
						}
					} else {
						return false;
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.BreakLoopCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class BreakLoopOperation extends CompilationUnitRewriteOperation {
		private final IfStatement ifStatement;

		public BreakLoopOperation(final IfStatement ifStatement) {
			this.ifStatement= ifStatement;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.BreakLoopCleanUp_description, cuRewrite);

			if (ifStatement.getThenStatement() instanceof Block) {
				ListRewrite listRewrite= rewrite.getListRewrite(ifStatement.getThenStatement(), Block.STATEMENTS_PROPERTY);
				listRewrite.insertLast(ast.newBreakStatement(), group);
			} else {
				Block newBlock= ast.newBlock();
				newBlock.statements().add(ASTNodes.createMoveTarget(rewrite, ifStatement.getThenStatement()));
				newBlock.statements().add(ast.newBreakStatement());
				ASTNodes.replaceButKeepComment(rewrite, ifStatement.getThenStatement(), newBlock, group);
			}
		}
	}
}
