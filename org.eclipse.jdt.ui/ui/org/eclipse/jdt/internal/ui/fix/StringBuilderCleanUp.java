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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
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
 * A fix that replaces String concatenation by StringBuilder when possible:
 * <ul>
 * <li>It uses <code>StringBuffer</code> for Java 1.4-,</li>
 * <li>It only replaces strings on several statements,</li>
 * <li>It should only concatenate and should retrieve the string once.</li>
 * </ul>
 */
public class StringBuilderCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public StringBuilderCleanUp() {
		this(Collections.emptyMap());
	}

	public StringBuilderCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.STRINGBUILDER);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.STRINGBUILDER)) {
			return new String[] { MultiFixMessages.StringBuilderCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.STRINGBUILDER)) {
			return "" //$NON-NLS-1$
					+ "StringBuilder variable = new StringBuilder();\n" //$NON-NLS-1$
					+ "variable.append(\"foo\");\n" //$NON-NLS-1$
					+ "variable.append(\"bar\");\n" //$NON-NLS-1$
					+ "System.out.println(variable.toString());\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "String variable = \"\";\n" //$NON-NLS-1$
				+ "variable = variable + \"foo\";\n" //$NON-NLS-1$
				+ "variable += \"bar\";\n" //$NON-NLS-1$
				+ "System.out.println(variable);\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.STRINGBUILDER)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			class VarOccurrenceVisitor extends ASTVisitor {
				private final Set<SimpleName> searchedVariables;
				private final Set<SimpleName> foundVariables= new HashSet<>();
				private final boolean hasToVisitLoops;

				/**
				 * The constructor.
				 *
				 * @param searchedVariables The variable to search
				 * @param hasToVisitLoops Has to visit loops
				 */
				public VarOccurrenceVisitor(final Set<SimpleName> searchedVariables, final boolean hasToVisitLoops) {
					this.searchedVariables= searchedVariables;
					this.hasToVisitLoops= hasToVisitLoops;
				}

				/**
				 * Returns the found variables.
				 *
				 * @return the found variables.
				 */
				public Set<SimpleName> getFoundVariables() {
					return foundVariables;
				}

				@Override
				public boolean visit(final SimpleName aVariable) {
					if (searchedVariables.contains(aVariable)) {
						foundVariables.add(aVariable);
					}

					return true;
				}

				@Override
				public boolean visit(final ForStatement node) {
					return hasToVisitLoops;
				}

				@Override
				public boolean visit(final EnhancedForStatement node) {
					return hasToVisitLoops;
				}

				@Override
				public boolean visit(final WhileStatement node) {
					return hasToVisitLoops;
				}

				@Override
				public boolean visit(final DoStatement node) {
					return hasToVisitLoops;
				}

				@Override
				public boolean visit(final TypeDeclaration node) {
					return false;
				}

				@Override
				public boolean visit(final LambdaExpression node) {
					return false;
				}
			}
			@Override
			public boolean visit(final Block visited) {
				StringOccurrencesVisitor stringOccurrencesVisitor= new StringOccurrencesVisitor(visited);
				visited.accept(stringOccurrencesVisitor);
				return stringOccurrencesVisitor.result;
			}

			final class StringOccurrencesVisitor extends ASTVisitor {
				private final Block startNode;
				private boolean result= true;

				public StringOccurrencesVisitor(final Block startNode) {
					this.startNode= startNode;
				}

				@Override
				public boolean visit(final Block node) {
					return startNode == node;
				}

				@Override
				public boolean visit(final VariableDeclarationStatement node) {
					if (node.fragments().size() != 1) {
						return true;
					}

					VariableDeclarationFragment fragment= (VariableDeclarationFragment) node.fragments().get(0);
					return visitVariable(node.getType(), fragment.resolveBinding(), fragment.getExtraDimensions(), fragment.getName(), fragment.getInitializer());
				}

				@Override
				public boolean visit(final VariableDeclarationExpression node) {
					if (node.fragments().size() != 1) {
						return true;
					}

					VariableDeclarationFragment fragment= (VariableDeclarationFragment) node.fragments().get(0);
					return visitVariable(node.getType(), fragment.resolveBinding(), fragment.getExtraDimensions(), fragment.getName(), fragment.getInitializer());
				}

				@Override
				public boolean visit(final SingleVariableDeclaration node) {
					return visitVariable(node.getType(), node.resolveBinding(), node.getExtraDimensions(), node.getName(), node.getInitializer());
				}

				private boolean visitVariable(final Type type, final IVariableBinding variableBinding, final int extraDimensions, final SimpleName declaration, final Expression initializer) {
					if (result
							&& extraDimensions == 0
							&& initializer != null
							&& ASTNodes.hasType(type.resolveBinding(), String.class.getCanonicalName())
							&& !ASTNodes.is(initializer, NullLiteral.class)) {
						VarDefinitionsUsesVisitor varOccurrencesVisitor= new VarDefinitionsUsesVisitor(variableBinding,
								startNode, true).find();

						List<SimpleName> reads= varOccurrencesVisitor.getReads();
						List<SimpleName> writes= varOccurrencesVisitor.getWrites();
						writes.remove(declaration);
						reads.removeAll(writes);

						Set<SimpleName> unvisitedReads= new HashSet<>(reads);
						Set<SimpleName> assignmentWrites= new HashSet<>();
						Set<SimpleName> concatenationWrites= new HashSet<>();

						for (SimpleName simpleName : writes) {
							if (!isWriteValid(simpleName, unvisitedReads, assignmentWrites, concatenationWrites)) {
								return true;
							}
						}

						if (unvisitedReads.size() == 1
								&& !writes.isEmpty()
								&& writes.size() == assignmentWrites.size() + concatenationWrites.size()) {
							Statement declarationStatement= ASTNodes.getTypedAncestor(type, Statement.class);
							SimpleName finalRead= unvisitedReads.iterator().next();

							if (isOccurrencesValid(declarationStatement, reads, writes, finalRead)) {
								rewriteOperations.add(new StringBuilderOperation(type, initializer, assignmentWrites, concatenationWrites, finalRead));

								result= false;
								return false;
							}
						}
					}

					return true;
				}

				private boolean isOccurrencesValid(final Statement declaration, final List<SimpleName> reads, final List<SimpleName> writes,
						final SimpleName finalRead) {
					if (declaration != null) {
						Set<SimpleName> remainingWrites= new HashSet<>(writes);
						Set<SimpleName> remainingReads= new HashSet<>(reads);
						remainingReads.remove(finalRead);
						Set<SimpleName> foundVariables= findVariables(declaration, remainingReads, remainingWrites,
								finalRead);

						if (foundVariables.isEmpty()) {
							List<Statement> statements= ASTNodes.getNextSiblings(declaration);
							AtomicBoolean hasFinalReadBeenFound= new AtomicBoolean(false);

							if (isOccurrenceValid(statements, remainingWrites, remainingReads, finalRead, hasFinalReadBeenFound)) {
								return hasFinalReadBeenFound.get() && remainingReads.isEmpty() && remainingWrites.isEmpty();
							}
						}
					}

					return false;
				}

				private boolean isOccurrenceValid(final List<Statement> statements, final Set<SimpleName> remainingWrites,
						final Set<SimpleName> remainingReads, final SimpleName finalRead, final AtomicBoolean hasFinalReadBeenFound) {
					for (Statement statement : statements) {
						Set<SimpleName> foundVariables= findVariables(statement, remainingReads, remainingWrites,
								finalRead);

						if (foundVariables.contains(finalRead)) {
							hasFinalReadBeenFound.set(true);

							if (!findVariables(statement, remainingReads, remainingWrites,
									finalRead, false).contains(finalRead)) {
								return false;
							}

							if (remainingReads.isEmpty() && remainingWrites.isEmpty()) {
								return true;
							}

							if (!foundVariables.containsAll(remainingReads)
									|| !foundVariables.containsAll(remainingWrites)) {
								return false;
							}

							IfStatement ifStatement= ASTNodes.as(statement, IfStatement.class);

							if (ifStatement != null) {
								if (findVariables(ifStatement.getExpression(), remainingReads, remainingWrites,
										finalRead).isEmpty()
										&& isBlockValid(remainingWrites, remainingReads, finalRead, ifStatement.getThenStatement())
										&& isBlockValid(remainingWrites, remainingReads, finalRead, ifStatement.getElseStatement())) {
									remainingWrites.removeAll(foundVariables);
									remainingReads.removeAll(foundVariables);

									return true;
								}

								return false;
							}

							TryStatement tryStatement= ASTNodes.as(statement, TryStatement.class);

							if (tryStatement != null
									&& isEmptyNodes(tryStatement.resources(), remainingReads, remainingWrites,
											finalRead)
									&& isBlockValid(remainingWrites, remainingReads, finalRead, tryStatement.getBody())) {
								for (Object catchClause : tryStatement.catchClauses()) {
									if (!isBlockValid(remainingWrites, remainingReads, finalRead, ((CatchClause) catchClause).getBody())) {
										return false;
									}
								}

								return isBlockValid(remainingWrites, remainingReads, finalRead, tryStatement.getFinally());
							}

							return false;
						}

						remainingWrites.removeAll(foundVariables);
						remainingReads.removeAll(foundVariables);
					}

					return true;
				}

				private boolean isBlockValid(final Set<SimpleName> remainingWrites, final Set<SimpleName> remainingReads,
						final SimpleName finalRead, final Statement subStatement) {
					Set<SimpleName> subRemainingWrites= new HashSet<>(remainingWrites);
					Set<SimpleName> subRemainingReads= new HashSet<>(remainingReads);
					AtomicBoolean subHasFinalReadBeenFound= new AtomicBoolean(false);

					return isOccurrenceValid(ASTNodes.asList(subStatement), subRemainingWrites, subRemainingReads, finalRead, subHasFinalReadBeenFound)
							&& subHasFinalReadBeenFound.get() == (subRemainingReads.isEmpty() && subRemainingWrites.isEmpty());
				}

				private boolean isEmptyNodes(final List<?> nodes, final Set<SimpleName> remainingReads,
						final Set<SimpleName> remainingWrites, final SimpleName finalRead) {
					if (nodes != null) {
						for (Object currentNode : nodes) {
							if (!findVariables((ASTNode) currentNode, remainingReads, remainingWrites,
									finalRead).isEmpty()) {
								return false;
							}
						}
					}

					return true;
				}

				private Set<SimpleName> findVariables(final ASTNode currentNode, final Set<SimpleName> remainingReads,
						final Set<SimpleName> remainingWrites, final SimpleName finalRead) {
					return findVariables(currentNode, remainingReads, remainingWrites, finalRead, true);
				}

				private Set<SimpleName> findVariables(final ASTNode currentNode, final Set<SimpleName> remainingReads,
						final Set<SimpleName> remainingWrites, final SimpleName finalRead, final boolean hasToVisitLoops) {
					if (currentNode == null) {
						return Collections.emptySet();
					}

					Set<SimpleName> searchedVariables= new HashSet<>(remainingReads);
					searchedVariables.addAll(remainingWrites);
					searchedVariables.add(finalRead);
					VarOccurrenceVisitor varOccurrenceVisitor= new VarOccurrenceVisitor(searchedVariables, hasToVisitLoops);
					currentNode.accept(varOccurrenceVisitor);

					return varOccurrenceVisitor.getFoundVariables();
				}

				private boolean isWriteValid(final SimpleName simpleName, final Set<SimpleName> unvisitedReads, final Set<SimpleName> assignmentWrites, final Set<SimpleName> concatenationWrites) {
					if (simpleName.getParent() instanceof Assignment) {
						Assignment assignment= (Assignment) simpleName.getParent();

						if (assignment.getParent() instanceof ExpressionStatement
								&& simpleName.getLocationInParent() == Assignment.LEFT_HAND_SIDE_PROPERTY) {
							if (ASTNodes.hasOperator(assignment, Assignment.Operator.PLUS_ASSIGN)) {
								assignmentWrites.add(simpleName);
								return true;
							}

							if (ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)) {
								InfixExpression concatenation= ASTNodes.as(assignment.getRightHandSide(), InfixExpression.class);

								if (concatenation != null
										&& ASTNodes.hasOperator(concatenation, InfixExpression.Operator.PLUS)) {
									SimpleName stringRead= ASTNodes.as(concatenation.getLeftOperand(), SimpleName.class);

									if (stringRead != null
											&& unvisitedReads.contains(stringRead)) {
										unvisitedReads.remove(stringRead);
										concatenationWrites.add(simpleName);
										return true;
									}
								}
							}
						}
					}

					return false;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.StringBuilderCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public CompilationUnitChange createChange(final IProgressMonitor progressMonitor) throws CoreException {
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

	private static class StringBuilderOperation extends CompilationUnitRewriteOperation {
		private final Type type;
		private final Expression initializer;
		private final Set<SimpleName> assignmentWrites;
		private final Set<SimpleName> concatenationWrites;
		private final SimpleName finalRead;

		public StringBuilderOperation(final Type type, final Expression initializer, final Set<SimpleName> assignmentWrites,
				final Set<SimpleName> concatenationWrites, final SimpleName finalRead) {
			this.type= type;
			this.initializer= initializer;
			this.assignmentWrites= assignmentWrites;
			this.concatenationWrites= concatenationWrites;
			this.finalRead= finalRead;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.StringBuilderCleanUp_description, cuRewrite);

			Class<?> builder;
			if (JavaModelUtil.is50OrHigher(((CompilationUnit) type.getRoot()).getJavaElement().getJavaProject())) {
				builder= StringBuilder.class;
			} else {
				builder= StringBuffer.class;
			}

			ASTNodes.replaceButKeepComment(rewrite, type, ast.newSimpleType(ASTNodeFactory.newName(ast, builder.getSimpleName())), group);

			StringLiteral stringLiteral= ASTNodes.as(initializer, StringLiteral.class);
			ClassInstanceCreation newClassInstanceCreation= ast.newClassInstanceCreation();
			newClassInstanceCreation.setType(ast.newSimpleType(ASTNodeFactory.newName(ast, builder.getSimpleName())));

			if (stringLiteral == null || !stringLiteral.getLiteralValue().matches("")) { //$NON-NLS-1$
				newClassInstanceCreation.arguments().add(ASTNodes.createMoveTarget(rewrite, initializer));
			}

			ASTNodes.replaceButKeepComment(rewrite, initializer, newClassInstanceCreation, group);

			for (SimpleName simpleName : assignmentWrites) {
				Assignment assignment= (Assignment) simpleName.getParent();
				InfixExpression concatenation= ASTNodes.as(assignment.getRightHandSide(), InfixExpression.class);

				List<Expression> operands;
				if (concatenation != null
						&& ASTNodes.hasOperator(concatenation, InfixExpression.Operator.PLUS)) {
					operands= ASTNodes.allOperands(concatenation);
				} else {
					operands= Arrays.asList(assignment.getRightHandSide());
				}

				Expression createdExpression= ASTNodes.createMoveTarget(rewrite, assignment.getLeftHandSide());

				for (Object operand : operands) {
					MethodInvocation newMethodInvocation= ast.newMethodInvocation();
					newMethodInvocation.setExpression(createdExpression);
					newMethodInvocation.setName(ast.newSimpleName("append")); //$NON-NLS-1$
					newMethodInvocation.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression((Expression) operand)));
					createdExpression= newMethodInvocation;
				}

				ASTNodes.replaceButKeepComment(rewrite, assignment, createdExpression, group);
			}

			for (SimpleName simpleName : concatenationWrites) {
				Assignment assignment= (Assignment) simpleName.getParent();
				InfixExpression concatenation= (InfixExpression) assignment.getRightHandSide();

				MethodInvocation newExpression= ast.newMethodInvocation();
				newExpression.setExpression(ASTNodes.createMoveTarget(rewrite, assignment.getLeftHandSide()));
				newExpression.setName(ast.newSimpleName("append")); //$NON-NLS-1$
				newExpression.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(concatenation.getRightOperand())));

				if (concatenation.hasExtendedOperands()) {
					for (Object operand : concatenation.extendedOperands()) {
						MethodInvocation newMethodInvocation= ast.newMethodInvocation();
						newMethodInvocation.setExpression(newExpression);
						newMethodInvocation.setName(ast.newSimpleName("append")); //$NON-NLS-1$
						newMethodInvocation.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression((Expression) operand)));
						newExpression= newMethodInvocation;
					}
				}

				ASTNodes.replaceButKeepComment(rewrite, assignment, newExpression, group);
			}

			MethodInvocation newMethodInvocation= ast.newMethodInvocation();
			newMethodInvocation.setExpression(ASTNodes.createMoveTarget(rewrite, finalRead));
			newMethodInvocation.setName(ast.newSimpleName("toString")); //$NON-NLS-1$
			ASTNodes.replaceButKeepComment(rewrite, finalRead, newMethodInvocation, group);
		}
	}
}
