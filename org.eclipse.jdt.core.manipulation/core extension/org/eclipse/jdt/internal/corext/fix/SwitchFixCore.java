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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.InterruptibleVisitor;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class SwitchFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class SwitchStatementsFinder extends ASTVisitor {
		static final class Variable {
			private final Expression name;
			private final List<Expression> constantValues;

			private Variable(final Expression firstOp, final List<Expression> constantValues) {
				this.name= firstOp;
				this.constantValues= constantValues;
			}

			private boolean isSameVariable(final Variable other) {
				return other != null && ASTNodes.isSameVariable(name, other.name);
			}

			private Variable mergeValues(final Variable other) {
				List<Expression> values= new ArrayList<>(constantValues);
				values.addAll(other.constantValues);
				return new Variable(name, values);
			}
		}

		final class HasUnlabeledBreakVisitor extends InterruptibleVisitor {
			boolean hasUnlabeledBreak= false;

			@Override
			public boolean visit(final BreakStatement node) {
				if (node.getLabel() == null) {
					hasUnlabeledBreak= true;
					return interruptVisit();
				}

				return true;
			}

			@Override
			public boolean visit(final EnhancedForStatement visited) {
				// Unlabeled breaks in inner loops/switchs work OK with switch cleanup rule
				return false;
			}

			@Override
			public boolean visit(final ForStatement visited) {
				return false;
			}

			@Override
			public boolean visit(final SwitchStatement visited) {
				return false;
			}

			@Override
			public boolean visit(final WhileStatement visited) {
				return false;
			}

			@Override
			public boolean visit(final DoStatement visited) {
				return false;
			}
		}

		private List<SwitchFixOperation> fResult;

		public SwitchStatementsFinder(List<SwitchFixOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final Block visited) {
			SeveralIfVisitor severalIfVisitor= new SeveralIfVisitor(visited);
			visited.accept(severalIfVisitor);
			return severalIfVisitor.result;
		}

		final class SeveralIfVisitor extends ASTVisitor {
			private final Block startNode;
			private boolean result= true;

			public SeveralIfVisitor(final Block startNode) {
				this.startNode= startNode;
			}

			@Override
			public boolean visit(final Block visited) {
				return startNode == visited;
			}

			@Override
			public boolean visit(final IfStatement visited) {
				if (!result || hasUnlabeledBreak(visited)) {
					return true;
				}

				Variable variable= extractVariableAndValues(visited);

				if (variable == null) {
					return true;
				}

				Expression switchExpression= variable.name;
				List<IfStatement> ifStatements= new ArrayList<>();
				List<SwitchCaseSection> cases= new ArrayList<>();
				Statement remainingStatement= null;

				Set<SimpleName> variableDeclarationIds= new HashSet<>();
				IfStatement ifStatement= visited;
				boolean isFallingThrough= true;

				do {
					IfStatement currentNode= ifStatement;

					while (ASTNodes.isSameVariable(switchExpression, variable.name)) {
						if (detectDeclarationConflicts(currentNode.getThenStatement(), variableDeclarationIds)) {
							// Cannot declare two variables with the same name in two cases
							return true;
						}

						cases.add(new SwitchCaseSection(variable.constantValues,
								ASTNodes.asList(currentNode.getThenStatement())));

						if (!ASTNodes.fallsThrough(currentNode.getThenStatement())) {
							isFallingThrough= false;
						}

						remainingStatement= currentNode.getElseStatement();

						if (remainingStatement == null) {
							break;
						}

						variable= extractVariableAndValues(remainingStatement);

						if (variable == null) {
							break;
						}

						currentNode= (IfStatement) remainingStatement;
					}

					ifStatements.add(ifStatement);
					ifStatement= ASTNodes.as(ASTNodes.getNextSibling(ifStatement), IfStatement.class);
					variable= extractVariableAndValues(ifStatement);
				} while (isFallingThrough
						&& ifStatement != null
						&& !hasUnlabeledBreak(ifStatement)
						&& remainingStatement == null
						&& variable != null
						&& ASTNodes.isSameVariable(switchExpression, variable.name));

				List<SwitchCaseSection> filteredCases= filterDuplicateCaseValues(cases);
				return maybeReplaceWithSwitchStatement(ifStatements, switchExpression, filteredCases, remainingStatement);
			}

			private boolean maybeReplaceWithSwitchStatement(final List<IfStatement> ifStatements, final Expression switchExpression,
					final List<SwitchCaseSection> cases, final Statement remainingStatement) {
				if (switchExpression != null && cases.size() > 2) {
					fResult.add(new SwitchFixOperation(ifStatements, switchExpression, cases, remainingStatement));
					result= false;
					return false;
				}

				return true;
			}

			private boolean hasUnlabeledBreak(final IfStatement node) {
				HasUnlabeledBreakVisitor hasUnlabeledBreakVisitor= new HasUnlabeledBreakVisitor();
				hasUnlabeledBreakVisitor.traverseNodeInterruptibly(node);
				return hasUnlabeledBreakVisitor.hasUnlabeledBreak;
			}

			private boolean detectDeclarationConflicts(final Statement statement, final Set<SimpleName> variableDeclarationIds) {
				Set<SimpleName> varNames= ASTNodes.getLocalVariableIdentifiers(statement, false);
				boolean hasConflict= containsAny(variableDeclarationIds, varNames);
				variableDeclarationIds.addAll(varNames);
				return hasConflict;
			}

			private boolean containsAny(final Set<SimpleName> variableDeclarations, final Set<SimpleName> varNames) {
				for (SimpleName varName : varNames) {
					for (SimpleName variableDeclaration : variableDeclarations) {
						if (varName.getIdentifier() != null && Objects.equals(varName.getIdentifier(), variableDeclaration.getIdentifier())) {
							return true;
						}
					}
				}

				return false;
			}

			/**
			 * Side-effect: removes the dead branches in a chain of if-elseif.
			 *
			 * @param sourceCases The source cases
			 * @return The filtered cases
			 */
			private List<SwitchCaseSection> filterDuplicateCaseValues(final List<SwitchCaseSection> sourceCases) {
				List<SwitchCaseSection> results= new ArrayList<>();
				Set<Object> alreadyProccessedValues= new HashSet<>();

				for (SwitchCaseSection sourceCase : sourceCases) {
					List<Expression> filteredExprs= new ArrayList<>();

					for (Expression expression : sourceCase.literalExpressions) {
						Object constantValue= expression.resolveConstantExpressionValue();

						if (alreadyProccessedValues.add(constantValue)) {
							// This is a new value (never seen before)
							filteredExprs.add(expression);
						}
					}

					if (!filteredExprs.isEmpty()) {
						results.add(new SwitchCaseSection(filteredExprs, sourceCase.statements));
					}
				}

				return results;
			}

			private Variable extractVariableAndValues(final Statement statement) {
				if (statement instanceof IfStatement) {
					return extractVariableAndValues(((IfStatement) statement).getExpression());
				}

				return null;
			}

			private Variable extractVariableAndValues(final Expression expression) {
				InfixExpression infixExpression= ASTNodes.as(expression, InfixExpression.class);

				if (infixExpression != null) {
					return extractVariableAndValuesFromInfixExpression(infixExpression);
				}

				return null;
			}

			private Variable extractVariableAndValuesFromInfixExpression(final InfixExpression infixExpression) {
				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.OR, InfixExpression.Operator.XOR)) {
					List<Expression> operands= ASTNodes.allOperands(infixExpression);
					Variable mergedVariable= null;

					for (Expression operand : operands) {
						Variable variable= extractVariableAndValues(operand);

						if (variable == null) {
							return null;
						}

						if (mergedVariable == null) {
							mergedVariable= variable;
						} else if (mergedVariable.isSameVariable(variable)) {
							mergedVariable= mergedVariable.mergeValues(variable);
						} else {
							return null;
						}
					}

					return mergedVariable;
				}

				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.EQUALS) && !infixExpression.hasExtendedOperands()) {
					Expression leftOperand= infixExpression.getLeftOperand();
					Expression rightOperand= infixExpression.getRightOperand();

					Variable variable= extractVariableWithConstantValue(leftOperand, rightOperand);
					return variable != null ? variable : extractVariableWithConstantValue(rightOperand, leftOperand);
				}

				return null;
			}

			private Variable extractVariableWithConstantValue(final Expression variable, final Expression constant) {
				if ((variable instanceof Name || variable instanceof FieldAccess || variable instanceof SuperFieldAccess)
						&& ASTNodes.hasType(variable, char.class.getCanonicalName(), byte.class.getCanonicalName(), short.class.getCanonicalName(), int.class.getCanonicalName())
						&& constant.resolveTypeBinding() != null
						&& constant.resolveTypeBinding().isPrimitive()
						&& constant.resolveConstantExpressionValue() != null) {
					return new Variable(variable, Arrays.asList(constant));
				}

				return null;
			}
		}
	}

	public static class SwitchFixOperation extends CompilationUnitRewriteOperation {
		private final List<IfStatement> ifStatements;
		private final Expression switchExpression;
		private final List<SwitchCaseSection> cases;
		private final Statement remainingStatement;

		public SwitchFixOperation(final List<IfStatement> ifStatements, final Expression switchExpression, final List<SwitchCaseSection> cases, final Statement remainingStatement) {
			this.ifStatements= ifStatements;
			this.switchExpression= switchExpression;
			this.cases= cases;
			this.remainingStatement= remainingStatement;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_Switch_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			SwitchStatement switchStatement= ast.newSwitchStatement();
			switchStatement.setExpression(ASTNodes.createMoveTarget(rewrite, switchExpression));

			for (SwitchCaseSection aCase : cases) {
				addCaseWithStatements(rewrite, ast, switchStatement, aCase.literalExpressions, aCase.statements);
			}

			if (remainingStatement != null) {
				addCaseWithStatements(rewrite, ast, switchStatement, null, ASTNodes.asList(remainingStatement));
			} else {
				addCaseWithStatements(rewrite, ast, switchStatement, null, Collections.emptyList());
			}

			for (int i= 0; i < ifStatements.size() - 1; i++) {
				ASTNodes.removeButKeepComment(rewrite, ifStatements.get(i), group);
			}

			ASTNodes.replaceButKeepComment(rewrite, ifStatements.get(ifStatements.size() - 1), switchStatement, group);
		}

		private void addCaseWithStatements(final ASTRewrite rewrite, final AST ast, final SwitchStatement switchStatement, final List<Expression> caseValuesOrNullForDefault,
				final List<Statement> innerStatements) {
			List<Statement> switchStatements= switchStatement.statements();

			// Add the case statement(s)
			if (caseValuesOrNullForDefault != null && !caseValuesOrNullForDefault.isEmpty()) {
				for (Expression caseValue : caseValuesOrNullForDefault) {
					SwitchCase newSwitchCase= ast.newSwitchCase();
					newSwitchCase.expressions().add(ASTNodes.createMoveTarget(rewrite, caseValue));
					switchStatements.add(newSwitchCase);
				}
			} else {
				switchStatements.add(ast.newSwitchCase());
			}

			boolean isBreakNeeded= true;

			// Add the statement(s) for this case(s)
			if (!innerStatements.isEmpty()) {
				for (Statement statement : innerStatements) {
					switchStatements.add(ASTNodes.createMoveTarget(rewrite, statement));
				}

				isBreakNeeded= !ASTNodes.fallsThrough(innerStatements.get(innerStatements.size() - 1));
			}

			// When required: end with a break
			if (isBreakNeeded) {
				switchStatements.add(ast.newBreakStatement());
			}
		}
	}


	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<SwitchFixOperation> operations= new ArrayList<>();
		SwitchStatementsFinder finder= new SwitchStatementsFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new SwitchFixCore(FixMessages.SwitchFix_convert_if_to_switch, compilationUnit, ops);
	}

	protected SwitchFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

	/**
	 * Represents a switch case section (cases + statements).
	 * <p>
	 * It can represent a switch case to build (when converting if else if
	 * statements), or existing switch cases when representing the structure of a
	 * whole switch.
	 */
	private static final class SwitchCaseSection {
		/**
		 * Must resolve to constant values. Used when representing switch cases to
		 * build.
		 */
		private final List<Expression> literalExpressions;
		/** The statements executed for the switch cases. */
		private final List<Statement> statements;

		/**
		 * Used for if statements, only constant expressions are used.
		 *
		 * @param literalExpressions The constant expressions
		 * @param statements The statements
		 */
		private SwitchCaseSection(final List<Expression> literalExpressions, final List<Statement> statements) {
			this.literalExpressions= literalExpressions;
			this.statements= statements;
		}
	}
}
