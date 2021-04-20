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
import java.util.concurrent.atomic.AtomicLong;

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
import org.eclipse.jdt.internal.corext.dom.VarDefinitionsUsesVisitor;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.actions.IndentAction;

/**
 * A fix that replaces String concatenation by StringBuilder when possible:
 * <ul>
 * <li>It uses <code>StringBuffer</code> for Java 1.4-,</li>
 * <li>It only replaces strings on several statements,</li>
 * <li>It should only concatenate,</li>
 * <li>It should concatenate more than two objects,</li>
 * <li>It should retrieve the string once.</li>
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
				private static final long MINIMUM_CONCATENATION_OPERAND_NUMBER_REQUIRED= 3L;

				private final Block startNode;
				private boolean result= true;

				public StringOccurrencesVisitor(final Block startNode) {
					this.startNode= startNode;
				}

				@Override
				public boolean visit(final Block visited) {
					return startNode == visited;
				}

				@Override
				public boolean visit(final VariableDeclarationStatement visited) {
					if (visited.fragments().size() != 1) {
						return true;
					}

					VariableDeclarationFragment fragment= (VariableDeclarationFragment) visited.fragments().get(0);
					return visitVariable(visited.getType(), fragment.resolveBinding(), fragment.getExtraDimensions(), fragment.getName(), fragment.getInitializer());
				}

				@Override
				public boolean visit(final VariableDeclarationExpression visited) {
					if (visited.fragments().size() != 1) {
						return true;
					}

					VariableDeclarationFragment fragment= (VariableDeclarationFragment) visited.fragments().get(0);
					return visitVariable(visited.getType(), fragment.resolveBinding(), fragment.getExtraDimensions(), fragment.getName(), fragment.getInitializer());
				}

				@Override
				public boolean visit(final SingleVariableDeclaration visited) {
					return visitVariable(visited.getType(), visited.resolveBinding(), visited.getExtraDimensions(), visited.getName(), visited.getInitializer());
				}

				private boolean visitVariable(final Type type, final IVariableBinding variableBinding, final int extraDimensions, final SimpleName declaration, final Expression initializer) {
					if (!result
							|| extraDimensions != 0
							|| initializer == null
							|| !ASTNodes.hasType(type.resolveBinding(), String.class.getCanonicalName())
							|| ASTNodes.is(initializer, NullLiteral.class)) {
						return true;
					}

					AtomicLong concatenatedStringCount= countConcatenationOperandsInInitialization(initializer);

					VarDefinitionsUsesVisitor varOccurrencesVisitor= new VarDefinitionsUsesVisitor(variableBinding,
							startNode, true);

					List<SimpleName> reads= varOccurrencesVisitor.getReads();
					List<SimpleName> writes= varOccurrencesVisitor.getWrites();
					writes.remove(declaration);

					// In the case of += assignment, the occurrences are counted twice
					// but each occurrence should be processed once
					reads.removeAll(writes);

					Set<SimpleName> unvisitedReads= new HashSet<>(reads);
					Set<SimpleName> assignmentWrites= new HashSet<>();
					Set<SimpleName> concatenationWrites= new HashSet<>();

					for (SimpleName simpleName : writes) {
						if (!isWriteValid(simpleName, unvisitedReads, assignmentWrites, concatenationWrites, concatenatedStringCount)) {
							return true;
						}
					}

					if (unvisitedReads.size() != 1
							|| writes.isEmpty()
							|| (writes.size() != assignmentWrites.size() + concatenationWrites.size())) {
						return true;
					}

					SimpleName finalSerialization= unvisitedReads.iterator().next();
					countConcatenationOperandsInFinalSerialization(finalSerialization, concatenatedStringCount);
					Statement declarationStatement= ASTNodes.getTypedAncestor(type, Statement.class);

					if (concatenatedStringCount.get() < MINIMUM_CONCATENATION_OPERAND_NUMBER_REQUIRED
							|| !isOccurrencesValid(declarationStatement, reads, writes, finalSerialization)) {
						return true;
					}

					rewriteOperations.add(new StringBuilderOperation(type, initializer, assignmentWrites, concatenationWrites, finalSerialization));

					result= false;
					return false;
				}

				private AtomicLong countConcatenationOperandsInInitialization(final Expression initializer) {
					Object emptyString= initializer.resolveConstantExpressionValue();

					if (IndentAction.EMPTY_STR.equals(emptyString)) {
						return new AtomicLong(0L);
					}

					InfixExpression initializerConcatenation= asStringConcatenation(initializer);

					if (initializerConcatenation != null) {
						return new AtomicLong(ASTNodes.allOperands(initializerConcatenation).size());
					}

					return new AtomicLong(1L);
				}

				private void countConcatenationOperandsInFinalSerialization(SimpleName finalSerialization, AtomicLong concatenatedStringCount) {
					if (finalSerialization.getParent() instanceof InfixExpression) {
						InfixExpression serializationConcatenation= (InfixExpression) finalSerialization.getParent();

						if (ASTNodes.hasOperator(serializationConcatenation, InfixExpression.Operator.PLUS)) {
							List<Expression> operands= ASTNodes.allOperands(serializationConcatenation);

							if (operands.contains(finalSerialization)) {
								int index= operands.indexOf(finalSerialization);
								concatenatedStringCount.addAndGet(operands.size() - index - 1);
							}
						}
					}
				}

				private boolean isOccurrencesValid(final Statement declaration, final List<SimpleName> reads, final List<SimpleName> writes,
						final SimpleName finalSerialization) {
					if (declaration != null) {
						Set<SimpleName> remainingWrites= new HashSet<>(writes);
						Set<SimpleName> remainingReads= new HashSet<>(reads);
						remainingReads.remove(finalSerialization);
						Set<SimpleName> foundVariables= findVariables(declaration, remainingReads, remainingWrites,
								finalSerialization);

						if (foundVariables.isEmpty()) {
							List<Statement> statements= ASTNodes.getNextSiblings(declaration);
							AtomicBoolean hasFinalReadBeenFound= new AtomicBoolean(false);

							if (isOccurrenceValid(statements, remainingWrites, remainingReads, finalSerialization, hasFinalReadBeenFound)) {
								return hasFinalReadBeenFound.get() && remainingReads.isEmpty() && remainingWrites.isEmpty();
							}
						}
					}

					return false;
				}

				private boolean isOccurrenceValid(final List<Statement> statements, final Set<SimpleName> remainingWrites,
						final Set<SimpleName> remainingReads, final SimpleName finalSerialization, final AtomicBoolean hasFinalReadBeenFound) {
					for (Statement statement : statements) {
						Set<SimpleName> foundVariables= findVariables(statement, remainingReads, remainingWrites,
								finalSerialization);

						if (foundVariables.contains(finalSerialization)) {
							hasFinalReadBeenFound.set(true);

							if (!findVariables(statement, remainingReads, remainingWrites,
									finalSerialization, false).contains(finalSerialization)) {
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
										finalSerialization).isEmpty()
										&& isBlockValid(remainingWrites, remainingReads, finalSerialization, ifStatement.getThenStatement())
										&& isBlockValid(remainingWrites, remainingReads, finalSerialization, ifStatement.getElseStatement())) {
									remainingWrites.removeAll(foundVariables);
									remainingReads.removeAll(foundVariables);

									return true;
								}

								return false;
							}

							TryStatement tryStatement= ASTNodes.as(statement, TryStatement.class);

							if (tryStatement != null
									&& isEmptyNodes(tryStatement.resources(), remainingReads, remainingWrites,
											finalSerialization)
									&& isBlockValid(remainingWrites, remainingReads, finalSerialization, tryStatement.getBody())) {
								for (Object catchClause : tryStatement.catchClauses()) {
									if (!isBlockValid(remainingWrites, remainingReads, finalSerialization, ((CatchClause) catchClause).getBody())) {
										return false;
									}
								}

								return isBlockValid(remainingWrites, remainingReads, finalSerialization, tryStatement.getFinally());
							}

							return false;
						}

						remainingWrites.removeAll(foundVariables);
						remainingReads.removeAll(foundVariables);
					}

					return true;
				}

				private boolean isBlockValid(final Set<SimpleName> remainingWrites, final Set<SimpleName> remainingReads,
						final SimpleName finalSerialization, final Statement subStatement) {
					Set<SimpleName> subRemainingWrites= new HashSet<>(remainingWrites);
					Set<SimpleName> subRemainingReads= new HashSet<>(remainingReads);
					AtomicBoolean subHasFinalReadBeenFound= new AtomicBoolean(false);

					return isOccurrenceValid(ASTNodes.asList(subStatement), subRemainingWrites, subRemainingReads, finalSerialization, subHasFinalReadBeenFound)
							&& subHasFinalReadBeenFound.get() == (subRemainingReads.isEmpty() && subRemainingWrites.isEmpty());
				}

				private boolean isEmptyNodes(final List<?> nodes, final Set<SimpleName> remainingReads,
						final Set<SimpleName> remainingWrites, final SimpleName finalSerialization) {
					if (nodes != null) {
						for (Object currentNode : nodes) {
							if (!findVariables((ASTNode) currentNode, remainingReads, remainingWrites,
									finalSerialization).isEmpty()) {
								return false;
							}
						}
					}

					return true;
				}

				private Set<SimpleName> findVariables(final ASTNode currentNode, final Set<SimpleName> remainingReads,
						final Set<SimpleName> remainingWrites, final SimpleName finalSerialization) {
					return findVariables(currentNode, remainingReads, remainingWrites, finalSerialization, true);
				}

				private Set<SimpleName> findVariables(final ASTNode currentNode, final Set<SimpleName> remainingReads,
						final Set<SimpleName> remainingWrites, final SimpleName finalSerialization, final boolean hasToVisitLoops) {
					if (currentNode == null) {
						return Collections.emptySet();
					}

					Set<SimpleName> searchedVariables= new HashSet<>(remainingReads);
					searchedVariables.addAll(remainingWrites);
					searchedVariables.add(finalSerialization);
					VarOccurrenceVisitor varOccurrenceVisitor= new VarOccurrenceVisitor(searchedVariables, hasToVisitLoops);
					currentNode.accept(varOccurrenceVisitor);

					return varOccurrenceVisitor.getFoundVariables();
				}

				private boolean isWriteValid(final SimpleName simpleName,
						final Set<SimpleName> unvisitedReads,
						final Set<SimpleName> assignmentWrites,
						final Set<SimpleName> concatenationWrites,
						final AtomicLong concatenatedStringCount) {
					if (simpleName.getParent() instanceof Assignment) {
						Assignment assignment= (Assignment) simpleName.getParent();

						if (assignment.getParent() instanceof ExpressionStatement
								&& simpleName.getLocationInParent() == Assignment.LEFT_HAND_SIDE_PROPERTY) {
							if (ASTNodes.hasOperator(assignment, Assignment.Operator.PLUS_ASSIGN)) {
								InfixExpression concatenation= asStringConcatenation((assignment.getRightHandSide()));

								if (concatenation != null) {
									concatenatedStringCount.addAndGet(ASTNodes.allOperands(concatenation).size());
								} else {
									concatenatedStringCount.incrementAndGet();
								}

								assignmentWrites.add(simpleName);
								enoughConcatenationsIfInsideALoop(simpleName, concatenatedStringCount);

								return true;
							}

							if (ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)) {
								InfixExpression concatenation= asStringConcatenation((assignment.getRightHandSide()));

								if (concatenation != null) {
									SimpleName stringRead= ASTNodes.as(concatenation.getLeftOperand(), SimpleName.class);

									if (stringRead != null
											&& unvisitedReads.contains(stringRead)) {
										concatenatedStringCount.addAndGet(ASTNodes.allOperands(concatenation).size() - 1);
										unvisitedReads.remove(stringRead);
										concatenationWrites.add(simpleName);
										enoughConcatenationsIfInsideALoop(simpleName, concatenatedStringCount);
										return true;
									}
								}
							}
						}
					}

					return false;
				}

				private void enoughConcatenationsIfInsideALoop(final SimpleName simpleName, final AtomicLong concatenatedStringCount) {
					ASTNode loop= ASTNodes.getFirstAncestorOrNull(simpleName, EnhancedForStatement.class, WhileStatement.class, ForStatement.class, DoStatement.class);

					if (loop != null && ASTNodes.isParent(loop, startNode)) {
						concatenatedStringCount.set(MINIMUM_CONCATENATION_OPERAND_NUMBER_REQUIRED);
					}
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.StringBuilderCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	private static InfixExpression asStringConcatenation(final Expression expression) {
		InfixExpression concatenation= ASTNodes.as(expression, InfixExpression.class);

		if (concatenation != null
				&& ASTNodes.hasOperator(concatenation, InfixExpression.Operator.PLUS)) {
			return concatenation;
		}

		return null;
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
		private static final String APPEND_METHOD= "append"; //$NON-NLS-1$
		private static final String TO_STRING_METHOD= "toString"; //$NON-NLS-1$
		private static final String VALUE_OF_METHOD= "valueOf"; //$NON-NLS-1$

		private final Type type;
		private final Expression initializer;
		private final Set<SimpleName> assignmentWrites;
		private final Set<SimpleName> concatenationWrites;
		private final SimpleName finalSerialization;

		public StringBuilderOperation(final Type type, final Expression initializer, final Set<SimpleName> assignmentWrites,
				final Set<SimpleName> concatenationWrites, final SimpleName finalSerialization) {
			this.type= type;
			this.initializer= initializer;
			this.assignmentWrites= assignmentWrites;
			this.concatenationWrites= concatenationWrites;
			this.finalSerialization= finalSerialization;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.StringBuilderCleanUp_description, cuRewrite);

			refactorCreation(rewrite, ast, group);
			refactorAssignmentWrites(rewrite, ast, group);
			refactorConcatenationWrites(rewrite, ast, group);
			refactorFinalRead(rewrite, ast, group);
		}

		private void refactorCreation(final ASTRewrite rewrite, final AST ast, final TextEditGroup group) {
			// Transform those codes:
			// String builder = "";
			// String builder = "foo";
			// String builder = "foo" + "bar";
			// String builder = i + "bar";
			// String builder = "foo" + String.valueOf(i);
			//
			// ...into this:
			// StringBuilder builder = new StringBuilder();
			// StringBuilder builder = new StringBuilder("foo");
			// StringBuilder builder = new StringBuilder("foo").append("bar");
			// StringBuilder builder = new StringBuilder().append(i).append("bar");
			// StringBuilder builder = new StringBuilder("foo").append(i);

			Class<?> builder;
			if (JavaModelUtil.is50OrHigher(((CompilationUnit) type.getRoot()).getJavaElement().getJavaProject())) {
				builder= StringBuilder.class;
			} else {
				builder= StringBuffer.class;
			}

			ASTNodes.replaceButKeepComment(rewrite, type, ast.newSimpleType(ASTNodeFactory.newName(ast, builder.getSimpleName())), group);

			ClassInstanceCreation newClassInstanceCreation= ast.newClassInstanceCreation();
			newClassInstanceCreation.setType(ast.newSimpleType(ASTNodeFactory.newName(ast, builder.getSimpleName())));
			Expression initialization= newClassInstanceCreation;
			Object initialValue= initializer.resolveConstantExpressionValue();

			if (!IndentAction.EMPTY_STR.equals(initialValue)) {
				InfixExpression concatenation= asStringConcatenation(initializer);

				List<Expression> operands;
				if (concatenation != null) {
					operands= ASTNodes.allOperands(concatenation);
				} else {
					operands= new ArrayList<>(Arrays.asList(initializer));
				}

				Expression firstOperand= operands.get(0);

				if ((firstOperand.resolveConstantExpressionValue() != null || ASTNodes.is(firstOperand, StringLiteral.class))
						&& (operands.size() == 1 || ASTNodes.hasType(firstOperand, String.class.getCanonicalName()))) {
					newClassInstanceCreation.arguments().add(ASTNodes.createMoveTarget(rewrite, firstOperand));
					operands.remove(0);
				}

				for (Expression operand : operands) {
					initialization= newAppending(rewrite, ast, initialization, operand);
				}
			}

			ASTNodes.replaceButKeepComment(rewrite, initializer, initialization, group);
		}

		private void refactorAssignmentWrites(final ASTRewrite rewrite, final AST ast, final TextEditGroup group) {
			// Transform those codes:
			// builder += "foo";
			// builder += "foo" + "bar";
			// builder += "foo" + String.valueOf(i);
			//
			// ...into this:
			// builder.append("foo");
			// builder.append("foo").append("bar");
			// builder.append("foo").append(i);

			for (SimpleName simpleName : assignmentWrites) {
				Assignment assignment= (Assignment) simpleName.getParent();
				Expression expression= assignment.getRightHandSide();
				InfixExpression concatenation= asStringConcatenation(expression);

				List<Expression> operands;
				if (concatenation != null) {
					operands= ASTNodes.allOperands(concatenation);
				} else {
					operands= Arrays.asList(expression);
				}

				Expression createdExpression= ASTNodes.createMoveTarget(rewrite, assignment.getLeftHandSide());

				for (Object operand : operands) {
					createdExpression= newAppending(rewrite, ast, createdExpression, (Expression) operand);
				}

				ASTNodes.replaceButKeepComment(rewrite, assignment, createdExpression, group);
			}
		}

		private void refactorConcatenationWrites(final ASTRewrite rewrite, final AST ast, final TextEditGroup group) {
			// Transform those codes:
			// builder =  builder + "foo";
			// builder =  builder + "foo" + "bar";
			// builder =  builder + "foo" + String.valueOf(i);
			//
			// ...into this:
			// builder.append("foo");
			// builder.append("foo").append("bar");
			// builder.append("foo").append(i);

			for (SimpleName simpleName : concatenationWrites) {
				Assignment assignment= (Assignment) simpleName.getParent();
				InfixExpression concatenation= (InfixExpression) assignment.getRightHandSide();

				Expression stringBuilder= ASTNodes.createMoveTarget(rewrite, assignment.getLeftHandSide());
				Expression expression= concatenation.getRightOperand();
				MethodInvocation newExpression= newAppending(rewrite, ast, stringBuilder, expression);

				if (concatenation.hasExtendedOperands()) {
					for (Object operand : concatenation.extendedOperands()) {
						newExpression= newAppending(rewrite, ast, newExpression, (Expression) operand);
					}
				}

				ASTNodes.replaceButKeepComment(rewrite, assignment, newExpression, group);
			}
		}

		private void refactorFinalRead(final ASTRewrite rewrite, final AST ast, final TextEditGroup group) {
			// Transform those codes:
			// builder
			// builder + "foo"
			// "bar" + builder + "foo"
			// "bar" + builder + String.valueOf(i)
			//
			// ...into this:
			// builder.toString()
			// builder.append("foo").toString()
			// "bar" + builder.append("foo").toString()
			// "bar" + builder.append(i).toString()

			if (finalSerialization.getParent() instanceof InfixExpression) {
				InfixExpression originalConcatenation= (InfixExpression) finalSerialization.getParent();

				if (ASTNodes.hasOperator(originalConcatenation, InfixExpression.Operator.PLUS)) {
					List<Expression> operands= ASTNodes.allOperands(originalConcatenation);

					if (operands.contains(finalSerialization)) {
						int index= operands.indexOf(finalSerialization);
						List<Expression> previousOperands= operands.subList(0, index);
						List<Expression> nextOperands= operands.subList(index + 1, operands.size());

						Expression appending= ASTNodes.createMoveTarget(rewrite, finalSerialization);

						for (Object operand : nextOperands) {
							appending= newAppending(rewrite, ast, appending, (Expression) operand);
						}

						MethodInvocation toStringMethod= ast.newMethodInvocation();
						toStringMethod.setExpression(appending);
						toStringMethod.setName(ast.newSimpleName(TO_STRING_METHOD));

						Expression createdExpression;
						if (previousOperands.isEmpty()) {
							createdExpression= toStringMethod;
						} else {
							InfixExpression previousConcatenation= ast.newInfixExpression();
							previousConcatenation.setOperator(InfixExpression.Operator.PLUS);
							previousConcatenation.setLeftOperand(ASTNodes.createMoveTarget(rewrite, previousOperands.get(0)));

							if (previousOperands.size() == 1) {
								previousConcatenation.setRightOperand(toStringMethod);
							} else {
								previousConcatenation.setRightOperand(ASTNodes.createMoveTarget(rewrite, previousOperands.get(1)));

								for (int i= 2; i < previousOperands.size(); i++) {
									previousConcatenation.extendedOperands().add(ASTNodes.createMoveTarget(rewrite, previousOperands.get(i)));
								}

								previousConcatenation.extendedOperands().add(toStringMethod);
							}

							createdExpression= previousConcatenation;
						}

						ASTNodes.replaceButKeepComment(rewrite, originalConcatenation, createdExpression, group);
						return;
					}
				}
			}

			MethodInvocation newMethodInvocation= ast.newMethodInvocation();
			newMethodInvocation.setExpression(ASTNodes.createMoveTarget(rewrite, finalSerialization));
			newMethodInvocation.setName(ast.newSimpleName(TO_STRING_METHOD));
			ASTNodes.replaceButKeepComment(rewrite, finalSerialization, newMethodInvocation, group);
		}

		private MethodInvocation newAppending(final ASTRewrite rewrite, final AST ast, final Expression stringBuilder, final Expression expression) {
			MethodInvocation originalMethodInvocation= ASTNodes.as(expression, MethodInvocation.class);
			ClassInstanceCreation originalClassInstanceCreation= ASTNodes.as(expression, ClassInstanceCreation.class);

			Expression originalExpression;
			if (originalMethodInvocation != null
					&& (ASTNodes.usesGivenSignature(originalMethodInvocation, String.class.getCanonicalName(), VALUE_OF_METHOD, Object.class.getCanonicalName())
							|| ASTNodes.usesGivenSignature(originalMethodInvocation, String.class.getCanonicalName(), VALUE_OF_METHOD, boolean.class.getCanonicalName())
							|| ASTNodes.usesGivenSignature(originalMethodInvocation, String.class.getCanonicalName(), VALUE_OF_METHOD, int.class.getCanonicalName())
							|| ASTNodes.usesGivenSignature(originalMethodInvocation, String.class.getCanonicalName(), VALUE_OF_METHOD, long.class.getCanonicalName())
							|| ASTNodes.usesGivenSignature(originalMethodInvocation, String.class.getCanonicalName(), VALUE_OF_METHOD, char.class.getCanonicalName())
							|| ASTNodes.usesGivenSignature(originalMethodInvocation, String.class.getCanonicalName(), VALUE_OF_METHOD, double.class.getCanonicalName())
							|| ASTNodes.usesGivenSignature(originalMethodInvocation, String.class.getCanonicalName(), VALUE_OF_METHOD, byte.class.getCanonicalName())
							|| ASTNodes.usesGivenSignature(originalMethodInvocation, String.class.getCanonicalName(), VALUE_OF_METHOD, short.class.getCanonicalName())
							|| ASTNodes.usesGivenSignature(originalMethodInvocation, String.class.getCanonicalName(), VALUE_OF_METHOD, float.class.getCanonicalName()))) {
				originalExpression= (Expression) originalMethodInvocation.arguments().get(0);
			} else if (originalClassInstanceCreation != null
					&& originalClassInstanceCreation.getExpression() == null
					&& originalClassInstanceCreation.getAnonymousClassDeclaration() == null
					&& (originalClassInstanceCreation.typeArguments() == null || originalClassInstanceCreation.typeArguments().isEmpty())
					&& originalClassInstanceCreation.arguments().size() == 1
					&& ASTNodes.hasType(originalClassInstanceCreation.getType().resolveBinding(), String.class.getCanonicalName())) {
				originalExpression= (Expression) originalClassInstanceCreation.arguments().get(0);
			} else {
				originalExpression= expression;
			}

			MethodInvocation newExpression= ast.newMethodInvocation();
			newExpression.setExpression(stringBuilder);
			newExpression.setName(ast.newSimpleName(APPEND_METHOD));
			newExpression.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(originalExpression)));
			return newExpression;
		}
	}
}
