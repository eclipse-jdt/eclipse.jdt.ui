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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
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
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.InterruptibleVisitor;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that replaces <code>if</code>/<code>else if</code>/<code>else</code> blocks to use <code>switch</code> where possible:
 * <ul>
 * <li>Convert to switch when there are more than two cases,</li>
 * <li>Do not convert if the discriminant can be null, that is to say only primitive and enum,</li>
 * <li>Do a variable conflict analyze.</li>
 * </ul>
 */
public class SwitchCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public SwitchCleanUp() {
		this(Collections.emptyMap());
	}

	public SwitchCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.USE_SWITCH);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.USE_SWITCH)) {
			return new String[] { MultiFixMessages.CodeStyleCleanUp_Switch_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.USE_SWITCH)) {
			return "" //$NON-NLS-1$
					+ "switch (number) {\n" //$NON-NLS-1$
					+ "case 0:\n" //$NON-NLS-1$
					+ "  i = 0;\n" //$NON-NLS-1$
					+ "  break;\n" //$NON-NLS-1$
					+ "case 1:\n" //$NON-NLS-1$
					+ "  j = 10;\n" //$NON-NLS-1$
					+ "  break;\n" //$NON-NLS-1$
					+ "case 2:\n" //$NON-NLS-1$
					+ "  k = 20;\n" //$NON-NLS-1$
					+ "  break;\n" //$NON-NLS-1$
					+ "default:\n" //$NON-NLS-1$
					+ "  m = -1;\n" //$NON-NLS-1$
					+ "  break;\n" //$NON-NLS-1$
					+ "}\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "if (number == 0) {\n" //$NON-NLS-1$
				+ "    i = 0;\n" //$NON-NLS-1$
				+ "} else if (number == 1) {\n" //$NON-NLS-1$
				+ "    j = 10;\n" //$NON-NLS-1$
				+ "} else if (2 == number) {\n" //$NON-NLS-1$
				+ "    k = 20;\n" //$NON-NLS-1$
				+ "} else {\n" //$NON-NLS-1$
				+ "    m = -1;\n" //$NON-NLS-1$
				+ "}\n\n\n\n\n\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.USE_SWITCH)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			final class Variable {
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
						rewriteOperations.add(new SwitchOperation(ifStatements, switchExpression, cases, remainingStatement));
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

				/** Side-effect: removes the dead branches in a chain of if-elseif. */
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
					if ((variable instanceof Name || variable instanceof FieldAccess || variable instanceof SuperFieldAccess) && ASTNodes.hasType(variable, char.class.getCanonicalName(), byte.class.getCanonicalName(), short.class.getCanonicalName(), int.class.getCanonicalName())
							&& constant.resolveTypeBinding() != null
							&& constant.resolveTypeBinding().isPrimitive()
							&& constant.resolveConstantExpressionValue() != null) {
						return new Variable(variable, Arrays.asList(constant));
					}

					return null;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.CodeStyleCleanUp_Switch_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		return null;
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class SwitchOperation extends CompilationUnitRewriteOperation {
		private final List<IfStatement> ifStatements;
		private final Expression switchExpression;
		private final List<SwitchCaseSection> cases;
		private final Statement remainingStatement;

		public SwitchOperation(final List<IfStatement> ifStatements, final Expression switchExpression, final List<SwitchCaseSection> cases, final Statement remainingStatement) {
			this.ifStatements= ifStatements;
			this.switchExpression= switchExpression;
			this.cases= cases;
			this.remainingStatement= remainingStatement;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_Switch_description, cuRewrite);

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
				if (JavaModelUtil.is12OrHigher(((CompilationUnit) ifStatements.get(0).getRoot()).getJavaElement().getJavaProject())) {
					// Uncomment this when the formatting will work with multi expression cases

//					SwitchCase newSwitchCase= ast.newSwitchCase();
//					newSwitchCase.expressions().addAll(ASTNodes.createMoveTarget(rewrite, caseValuesOrNullForDefault));
//					switchStatements.add(newSwitchCase);

					// Remove this when the formatting will work with multi expression cases
					for (Expression caseValue : caseValuesOrNullForDefault) {
						SwitchCase newSwitchCase= ast.newSwitchCase();
						newSwitchCase.expressions().add(ASTNodes.createMoveTarget(rewrite, caseValue));
						switchStatements.add(newSwitchCase);
					}
				} else {
					for (Expression caseValue : caseValuesOrNullForDefault) {
						SwitchCase newSwitchCase= ast.newSwitchCase();
						newSwitchCase.expressions().add(ASTNodes.createMoveTarget(rewrite, caseValue));
						switchStatements.add(newSwitchCase);
					}
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

	/**
	 * Represents a switch case section (cases + statements).
	 * <p>
	 * It can represent a switch case to build (when converting if else if
	 * statements), or existing switch cases when representing the structure of a
	 * whole switch.
	 */
	private final class SwitchCaseSection {
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
