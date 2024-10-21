/*******************************************************************************
 * Copyright (c) 2021, 2024 Fabrice TIERCELIN and others.
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
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.InterruptibleVisitor;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSElement;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSLine;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.fix.CleanUpNLSUtils;
import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class SwitchFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class SwitchStatementsFinder extends ASTVisitor {
		static final class Variable {
			private final Expression name;
			private final List<Expression> constantValues;
			private final List<Boolean> tagValues;

			private Variable(final Expression firstOp, final List<Expression> constantValues, final List<Boolean> tagValues) {
				this.name= firstOp;
				this.constantValues= constantValues;
				this.tagValues= tagValues;
			}

			private boolean isSameVariable(final Variable other) {
				return other != null && ASTNodes.isSameVariable(name, other.name);
			}

			private Variable mergeValues(final Variable other) {
				List<Expression> values= new ArrayList<>(constantValues);
				values.addAll(other.constantValues);
				List<Boolean> tags= new ArrayList<>(tagValues);
				tags.addAll(other.tagValues);
				return new Variable(name, values, tags);
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

				IfStatement ifStatement= visited;
				boolean isFallingThrough= true;

				do {
					IfStatement currentNode= ifStatement;

					while (ASTNodes.isSameVariable(switchExpression, variable.name)) {
						cases.add(new SwitchCaseSection(variable.constantValues,
								variable.tagValues,
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
					List<Boolean> filteredTagList= new ArrayList<>();

					for (int i= 0; i < sourceCase.literalExpressions.size(); ++i) {
						Expression expression= sourceCase.literalExpressions.get(i);
						Boolean hasTag= sourceCase.tagList.get(i);
						Object constantValue= expression.resolveConstantExpressionValue();
						ITypeBinding expressiontypeBinding= expression.resolveTypeBinding();
						if(constantValue == null && expressiontypeBinding != null && expressiontypeBinding.isEnum()) {
							constantValue= expression;
						}

						if (alreadyProccessedValues.add(constantValue)) {
							// This is a new value (never seen before)
							filteredExprs.add(expression);
							filteredTagList.add(hasTag);
						}
					}

					if (!filteredExprs.isEmpty()) {
						results.add(new SwitchCaseSection(filteredExprs, filteredTagList, sourceCase.statements));
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
				} else if (expression instanceof MethodInvocation) {
					MethodInvocation method= (MethodInvocation)expression;
					if (method.resolveMethodBinding() != null) {
						if (!"equals".equals(method.getName().getIdentifier())) { //$NON-NLS-1$
							return null;
						}
						List<?> arguments= method.arguments();
						if (arguments.size() != 1) {
							return null;
						}
						IMethodBinding methodBinding= method.resolveMethodBinding();
						String qualifiedName= methodBinding.getDeclaringClass().getQualifiedName();
						if ("java.lang.String".equals(qualifiedName)) { //$NON-NLS-1$
							return extractVariableAndValuesFromEqualsExpression(method.getExpression(),(Expression)(arguments.get(0)));
						}
					}
				}

				return null;
			}

			private Variable extractVariableAndValuesFromEqualsExpression(final Expression leftOperand,final Expression rightOperand) {
				Variable variable= extractVariableWithConstantStringValue(leftOperand, rightOperand);
				return variable != null ? variable : extractVariableWithConstantStringValue(rightOperand, leftOperand);
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
				if (!(variable instanceof Name || variable instanceof FieldAccess || variable instanceof SuperFieldAccess)) {
					return null;
				}

				ITypeBinding variabletypeBinding= variable.resolveTypeBinding();
				boolean isVariableEnum= variabletypeBinding != null && variabletypeBinding.isEnum();
				boolean hasType= ASTNodes.hasType(variable, char.class.getCanonicalName(),
						byte.class.getCanonicalName(), short.class.getCanonicalName(),
						int.class.getCanonicalName()) || isVariableEnum;

				ITypeBinding constanttypeBinding= constant.resolveTypeBinding();
				boolean isConstantEnum= constanttypeBinding != null && constanttypeBinding.isEnum();
				if (hasType
						&& constanttypeBinding != null
						&& (constanttypeBinding.isPrimitive() || isConstantEnum)
						&& (constant.resolveConstantExpressionValue() != null || isConstantEnum)) {
					return new Variable(variable, Arrays.asList(constant), Arrays.asList(false));
				}

				return null;
			}

			private Variable extractVariableWithConstantStringValue(final Expression variable, final Expression constant) {
				if ((variable instanceof Name || variable instanceof FieldAccess || variable instanceof SuperFieldAccess)
						&& ASTNodes.hasType(variable, String.class.getCanonicalName())
						&& constant.resolveTypeBinding() != null
						&& constant.resolveConstantExpressionValue() != null) {
					CompilationUnit cu= (CompilationUnit) variable.getRoot();
					ICompilationUnit icu= (ICompilationUnit) cu.getJavaElement();
					NLSLine nlsLine= CleanUpNLSUtils.scanCurrentLine(icu, variable);
					boolean hasTag= false;
					if (nlsLine != null) {
						for (NLSElement element : nlsLine.getElements()) {
							String value= element.getValue();
							if (value.length() >= 2) {
								value= value.substring(1, value.length() - 1);
							}
							if (value.equals(constant.resolveConstantExpressionValue()) && element.hasTag()) {
								hasTag= true;
							}
						}
					}
					return new Variable(variable, Arrays.asList(constant), Arrays.asList(hasTag));
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
			ASTNode newStatement= switchStatement;

			CompilationUnit unit= cuRewrite.getRoot();
			ICompilationUnit cu= (ICompilationUnit)unit.getJavaElement();
			ITypeBinding switchTypeBinding= switchExpression.resolveTypeBinding();
			boolean isEnum= switchTypeBinding != null && switchTypeBinding.isEnum();

			if (cu != null && !JavaModelUtil.is21OrHigher(cu.getJavaProject())) {
				if (switchTypeBinding != null && !switchTypeBinding.isPrimitive()) {
					IfStatement newIfStatement= ast.newIfStatement();
					InfixExpression newInfixExpression= ast.newInfixExpression();
					newInfixExpression.setOperator(Operator.NOT_EQUALS);
					newInfixExpression.setLeftOperand((Expression) rewrite.createCopyTarget(switchExpression));
					newInfixExpression.setRightOperand(ast.newNullLiteral());
					newIfStatement.setExpression(newInfixExpression);
					Block newBlock= ast.newBlock();
					newIfStatement.setThenStatement(newBlock);
					newBlock.statements().add(switchStatement);
					newStatement= newIfStatement;
				}
			}

			switchStatement.setExpression((Expression) rewrite.createCopyTarget(switchExpression));

			for (SwitchCaseSection aCase : cases) {
				addCaseWithStatements(rewrite, ast, switchStatement, aCase.literalExpressions, aCase.tagList, aCase.statements, isEnum);
			}

			if (remainingStatement != null) {
				remainingStatement.setProperty(UNTOUCH_COMMENT_PROPERTY, Boolean.TRUE);
				addCaseWithStatements(rewrite, ast, switchStatement, null, null, ASTNodes.asList(remainingStatement), isEnum);
				if (newStatement instanceof IfStatement ifStatement) {
					ifStatement.setElseStatement((Statement) rewrite.createCopyTarget(remainingStatement));
				}
			} else {
				addCaseWithStatements(rewrite, ast, switchStatement, null, null, Collections.emptyList(), isEnum);
			}

			for (int i= 0; i < ifStatements.size() - 1; i++) {
				ASTNodes.removeButKeepComment(rewrite, ifStatements.get(i), group);
			}

			ASTNodes.replaceButKeepComment(rewrite, ifStatements.get(ifStatements.size() - 1), newStatement, group);
		}

		private void addCaseWithStatements(final ASTRewrite rewrite, final AST ast, final SwitchStatement switchStatement,
				final List<Expression> caseValuesOrNullForDefault, final List<Boolean> tagList,
				final List<Statement> innerStatements, boolean isEnum) {
			List<Statement> switchStatements= switchStatement.statements();
			boolean needBlock= checkForLocalDeclarations(innerStatements);
			Boolean hasTag= false;

			// Add the case statement(s)
			if (caseValuesOrNullForDefault != null && !caseValuesOrNullForDefault.isEmpty()) {
				CompilationUnit unit= (CompilationUnit)caseValuesOrNullForDefault.get(0).getRoot();
				ICompilationUnit cu= (ICompilationUnit)unit.getJavaElement();
				String spaceBefore= getCoreOption(cu.getJavaProject(), DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE, false) ? " " : ""; //$NON-NLS-1$ //$NON-NLS-2$
				for (int i= 0; i < caseValuesOrNullForDefault.size(); ++i) {
				    Expression caseValue= caseValuesOrNullForDefault.get(i);
				    if (isEnum && caseValue instanceof QualifiedName qName) {
				    	caseValue= qName.getName();
				    }
				    hasTag= tagList.get(i);
				    if (hasTag) {
				    	try {
							IBuffer buffer= cu.getBuffer();
							String newline= i == caseValuesOrNullForDefault.size() - 1 && needBlock ? "\n" : ""; //$NON-NLS-1$ //$NON-NLS-2$
							String caseString= "case " + buffer.getText(caseValue.getStartPosition(), caseValue.getLength()) + spaceBefore + ": //$NON-NLS-1$" + newline; //$NON-NLS-1$ //$NON-NLS-2$
 							SwitchCase newSwitchCase= (SwitchCase)rewrite.createStringPlaceholder(caseString, ASTNode.SWITCH_CASE);
 							switchStatements.add(newSwitchCase);
						} catch (JavaModelException e) {
							JavaManipulationPlugin.log(e);
							hasTag= false;
						}

				    }
				    if (!hasTag) {
				    	SwitchCase newSwitchCase= ast.newSwitchCase();
				    	newSwitchCase.expressions().add(ASTNodes.createMoveTarget(rewrite, caseValue));
				    	switchStatements.add(newSwitchCase);
				    }
				}
			} else {
				CompilationUnit unit= (CompilationUnit)switchExpression.getRoot();
				ICompilationUnit cu= (ICompilationUnit)unit.getJavaElement();
				if (cu != null && JavaModelUtil.is21OrHigher(cu.getJavaProject())) {
					ITypeBinding switchTypeBinding= switchExpression.resolveTypeBinding();
					if (switchTypeBinding != null && !switchTypeBinding.isPrimitive()) {
						SwitchCase newSwitchCase= ast.newSwitchCase();
						newSwitchCase.expressions().add(ast.newNullLiteral());
						switchStatements.add(newSwitchCase);
					}
				}
				switchStatements.add(ast.newSwitchCase());
			}

			List<Statement> statementsList= switchStatement.statements();
			boolean isBreakNeeded= true;
			Block block= null;
			if (needBlock) {
				block= ast.newBlock();
				statementsList= block.statements();
			}

			// Add the statement(s) for this case(s)
			if (!innerStatements.isEmpty()) {
				for (Statement statement : innerStatements) {
					statementsList.add((Statement) rewrite.createCopyTarget(statement));
				}

				isBreakNeeded= !ASTNodes.fallsThrough(innerStatements.get(innerStatements.size() - 1));
			}

			// When required: end with a break
			if (isBreakNeeded) {
				statementsList.add(ast.newBreakStatement());
			}

			if (needBlock) {
				switchStatements.add(block);
			}
		}

		private boolean checkForLocalDeclarations(final List<Statement> statements) {
			for (Statement statement : statements) {
				if (statement instanceof VariableDeclarationStatement) {
					return true;
				}
			}
			return false;
		}

		protected boolean getCoreOption(IJavaProject project, String key, boolean def) {
			String option= getCoreOption(project, key);
			if (JavaCore.INSERT.equals(option))
				return true;
			if (JavaCore.DO_NOT_INSERT.equals(option))
				return false;
			return def;
		}

		protected String getCoreOption(IJavaProject project, String key) {
			if (project == null)
				return JavaCore.getOption(key);
			return project.getOption(key, true);
		}

	}


	public static ICleanUpFix createCleanUp(final CompilationUnit compilationUnit) {
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
		private final List<Boolean> tagList;
		/** The statements executed for the switch cases. */
		private final List<Statement> statements;

		/**
		 * Used for if statements, only constant expressions are used.
		 *
		 * @param literalExpressions The constant expressions
		 * @param statements The statements
		 */
		private SwitchCaseSection(final List<Expression> literalExpressions, final List<Boolean> tagList,
				final List<Statement> statements) {
			this.literalExpressions= literalExpressions;
			this.statements= statements;
			this.tagList= tagList;
		}

	}
}
