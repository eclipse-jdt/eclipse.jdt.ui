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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTMatcherSameVariablesAndMethods;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTSemanticMatcher;
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
 * A fix that factorizes common code in all if / else if / else statements at the end of each blocks:
 * <ul>
 * <li>It can ignore a falling through block,</li>
 * <li>Ultimately it removes the empty and passive if conditions.</li>
 * </ul>
 */
public class ControlFlowMergeCleanUp extends AbstractMultiFix {
	public ControlFlowMergeCleanUp() {
		this(Collections.emptyMap());
	}

	public ControlFlowMergeCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.CONTROLFLOW_MERGE);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.CONTROLFLOW_MERGE)) {
			return new String[] { MultiFixMessages.ControlFlowMergeCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.CONTROLFLOW_MERGE)) {
			return "" //$NON-NLS-1$
					+ "if (!isValid) {\n" //$NON-NLS-1$
					+ "    j++;\n" //$NON-NLS-1$
					+ "}\n" //$NON-NLS-1$
					+ "++i;\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "if (isValid) {\n" //$NON-NLS-1$
				+ "    ++i;\n" //$NON-NLS-1$
				+ "} else {\n" //$NON-NLS-1$
				+ "    j++;\n" //$NON-NLS-1$
				+ "    i = i + 1;\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.CONTROLFLOW_MERGE)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final IfStatement visited) {
				if (visited.getElseStatement() == null) {
					return true;
				}

				List<ASTNode> allCases= new ArrayList<>();
				List<List<Statement>> allCasesStatements= new ArrayList<>();

				// Collect all the if / else if / else if / ... / else cases
				if (collectAllCases(allCasesStatements, visited, allCases)) {
					@SuppressWarnings("unchecked")
					List<Statement>[] allCaseStmtsToRemove= new List[allCasesStatements.size()];

					// Initialize removedCaseStatements list
					for (int i= 0; i < allCasesStatements.size(); i++) {
						allCaseStmtsToRemove[i]= new LinkedList<>();
					}

					// If all cases exist
					ASTSemanticMatcher matcher= new ASTMatcherSameVariablesAndMethods();
					List<Integer> casesToRefactor= getMatchingCases(allCasesStatements, matcher);

					if (casesToRefactor == null || casesToRefactor.size() <= 1) {
						return true;
					}

					int smallestStatementNumber= smallestStatementNumber(allCasesStatements, casesToRefactor);

					// Identify matching statements starting from the end of each case
					for (int stmtIndex= 1; stmtIndex <= smallestStatementNumber; stmtIndex++) {
						if (!match(matcher, allCasesStatements, stmtIndex, casesToRefactor)) {
							break;
						}

						flagStmtsToRemove(allCasesStatements, stmtIndex, allCaseStmtsToRemove, casesToRefactor);
					}

					if (!hasVariableConflict(visited, allCaseStmtsToRemove)) {
						rewriteOperations.add(new ControlFlowMergeOperation(visited, allCases, allCasesStatements, allCaseStmtsToRemove, casesToRefactor));
						return false;
					}
				}

				return true;
			}

			private List<Integer> getMatchingCases(final List<List<Statement>> allCasesStatements,
					final ASTSemanticMatcher matcher) {
				List<StatementAndBlockIndices> matchingCases= new ArrayList<>();

				for (int i= 0; i < allCasesStatements.size(); i++) {
					boolean isMatching= false;
					Statement currentStatement= allCasesStatements.get(i).get(allCasesStatements.get(i).size() - 1);

					for (StatementAndBlockIndices pair : matchingCases) {
						if (ASTNodes.match(matcher, pair.getStatement(), currentStatement)) {
							pair.getBlockIndices().add(i);
							isMatching= true;
							break;
						}
					}

					if (!isMatching) {
						StatementAndBlockIndices newPair= new StatementAndBlockIndices(currentStatement, new ArrayList<>());
						newPair.getBlockIndices().add(i);
						matchingCases.add(newPair);
					}
				}

				Collections.sort(matchingCases, Comparator.<StatementAndBlockIndices, Integer>comparing(s -> s.getBlockIndices().size()));
				StatementAndBlockIndices notFallingThroughCase= null;

				for (StatementAndBlockIndices matchingCase : matchingCases) {
					if (!ASTNodes.fallsThrough(matchingCase.getStatement())) {
						if (notFallingThroughCase != null) {
							return null;
						}

						notFallingThroughCase= matchingCase;
					}
				}

				if (notFallingThroughCase != null) {
					return notFallingThroughCase.getBlockIndices();
				}

				return matchingCases.get(0).getBlockIndices();
			}

			private void flagStmtsToRemove(final List<List<Statement>> allCasesStatements, final int stmtIndex,
					final List<Statement>[] caseStmtsToRemove, final List<Integer> casesToRefactor) {
				for (int i : casesToRefactor) {
					List<Statement> caseStatements= allCasesStatements.get(i);
					Statement stmtToRemove= caseStatements.get(caseStatements.size() - stmtIndex);
					caseStmtsToRemove[i].add(stmtToRemove);
				}
			}

			private boolean match(final ASTSemanticMatcher matcher, final List<List<Statement>> allCasesStatements, final int stmtIndex, final List<Integer> casesToRefactor) {
				List<Statement> firstCaseToRefactor= allCasesStatements.get(casesToRefactor.get(0));

				for (int i= 1; i < casesToRefactor.size(); i++) {
					List<Statement> anotherCaseToRefactor= allCasesStatements.get(casesToRefactor.get(i));

					if (!ASTNodes.match(matcher, firstCaseToRefactor.get(firstCaseToRefactor.size() - stmtIndex),
							anotherCaseToRefactor.get(anotherCaseToRefactor.size() - stmtIndex))) {
						return false;
					}
				}

				return true;
			}

			private int smallestStatementNumber(final List<List<Statement>> allCasesStatements, final List<Integer> casesToRefactor) {
				int min= allCasesStatements.get(casesToRefactor.get(0)).size();

				for (Integer caseToRefactor : casesToRefactor) {
					List<Statement> statements= allCasesStatements.get(caseToRefactor);
					min= Math.min(min, statements.size());
				}

				return min;
			}

			/**
			 * Collects all cases (if/else, if/else if/else, etc.) and returns whether all
			 * are covered.
			 *
			 * @param allCasesStatements the output collection for all the cases
			 * @param visited     the {@link IfStatement} to examine
			 * @param allCases All the cases
			 * @return true if all cases (if/else, if/else if/else, etc.) are covered, false
			 *         otherwise
			 */
			private boolean collectAllCases(final List<List<Statement>> allCasesStatements, final IfStatement visited, final List<ASTNode> allCases) {
				List<Statement> thenStatements= ASTNodes.asList(visited.getThenStatement());
				List<Statement> elseStatements= ASTNodes.asList(visited.getElseStatement());

				if (thenStatements.isEmpty() || elseStatements.isEmpty()) {
					// If the then or else clause is empty, then there is no common code whatsoever.
					// let other cleanups take care of removing empty blocks.
					return false;
				}

				allCases.add(visited);
				allCasesStatements.add(thenStatements);

				if (elseStatements.size() == 1) {
					IfStatement ifStatement= ASTNodes.as(elseStatements.get(0), IfStatement.class);

					if (ifStatement != null) {
						return collectAllCases(allCasesStatements, ifStatement, allCases);
					}
				}

				allCases.add(visited.getElseStatement());
				allCasesStatements.add(elseStatements);
				return true;
			}

			private boolean hasVariableConflict(final IfStatement visited, final List<Statement>[] allCaseStatementsToRemove) {
				Set<SimpleName> ifVariableNames= new HashSet<>();

				for (List<Statement> caseStatementsToRemove : allCaseStatementsToRemove) {
					for (Statement statementToRemove : caseStatementsToRemove) {
						ifVariableNames.addAll(ASTNodes.getLocalVariableIdentifiers(statementToRemove, false));
					}
				}

				VarConflictVisitor varOccurrenceVisitor= new VarConflictVisitor(ifVariableNames, true);

				for (Statement furtherStatement : ASTNodes.getNextSiblings(visited)) {
					varOccurrenceVisitor.traverseNodeInterruptibly(furtherStatement);

					if (varOccurrenceVisitor.isVarConflicting()) {
						return true;
					}
				}

				return false;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.ControlFlowMergeCleanUp_description, unit,
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

	private static class ControlFlowMergeOperation extends CompilationUnitRewriteOperation {
		private final IfStatement visited;
		private final List<ASTNode> allCases;
		private final List<List<Statement>> allCasesStatements;
		private final List<Statement>[] caseStmtsToRemove;
		private final List<Integer> casesToRefactor;

		public ControlFlowMergeOperation(final IfStatement visited,
				final List<ASTNode> allCases,
				final List<List<Statement>> allCasesStatements,
				final List<Statement>[] caseStmtsToRemove2,
				final List<Integer> casesToRefactor) {
			this.visited= visited;
			this.allCases= allCases;
			this.allCasesStatements= allCasesStatements;
			this.caseStmtsToRemove= caseStmtsToRemove2;
			this.casesToRefactor= casesToRefactor;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.ControlFlowMergeCleanUp_description, cuRewrite);

			// Remove the nodes common to all cases
			boolean[] areCasesRemovable= new boolean[allCasesStatements.size()];
			Arrays.fill(areCasesRemovable, false);
			removeStmtsFromCases(rewrite, group, areCasesRemovable);
			List<Statement> oneCaseToRemove= caseStmtsToRemove[casesToRefactor.get(0)];

			if (allRemovable(areCasesRemovable, 0)) {
				if (ASTNodes.canHaveSiblings(visited)) {
					insertIdenticalCode(rewrite, group, oneCaseToRemove);

					ASTNodes.removeButKeepComment(rewrite, visited, group);
				} else {
					List<Statement> orderedStatements= new ArrayList<>(oneCaseToRemove.size());

					for (Statement stmtToRemove : oneCaseToRemove) {
						orderedStatements.add(0, ASTNodes.createMoveTarget(rewrite, stmtToRemove));
					}

					Block newBlock= ast.newBlock();
					newBlock.statements().addAll(orderedStatements);
					ASTNodes.replaceButKeepComment(rewrite, visited, newBlock, group);
				}
			} else {
				// Remove empty cases
				for (int i : casesToRefactor) {
					ASTNode parent= allCases.get(i);

					if (areCasesRemovable[i]) {
						if (i == areCasesRemovable.length - 2 && !areCasesRemovable[i + 1]) {
							// Then clause is empty and there is only one else clause
							// => revert if statement
							IfStatement newIfStatement= ast.newIfStatement();
							newIfStatement.setExpression(ASTNodeFactory.negate(ast, rewrite, ((IfStatement) parent).getExpression(), true));
							newIfStatement.setThenStatement(ASTNodes.createMoveTarget(rewrite, ((IfStatement) parent).getElseStatement()));
							ASTNodes.replaceButKeepComment(rewrite, parent, newIfStatement, group);
							break;
						}

						if (allRemovable(areCasesRemovable, i)) {
							rewrite.remove(parent, group);
							break;
						}

						ASTNodes.replaceButKeepComment(rewrite, ((IfStatement) parent).getThenStatement(), ast.newBlock(), group);
					}
				}

				if (ASTNodes.canHaveSiblings(visited)) {
					insertIdenticalCode(rewrite, group, oneCaseToRemove);
				} else {
					List<Statement> orderedStatements= new ArrayList<>(oneCaseToRemove.size() + 1);

					for (Statement stmtToRemove : oneCaseToRemove) {
						orderedStatements.add(0, ASTNodes.createMoveTarget(rewrite, stmtToRemove));
					}

					orderedStatements.add(0, ASTNodes.createMoveTarget(rewrite, visited));
					Block newBlock= ast.newBlock();
					newBlock.statements().addAll(orderedStatements);
					ASTNodes.replaceButKeepComment(rewrite, visited, newBlock, group);
				}
			}
		}

		private void insertIdenticalCode(final ASTRewrite rewrite, final TextEditGroup group, final List<Statement> stmtsToRemove) {
			ListRewrite listRewrite= rewrite.getListRewrite(visited.getParent(), (ChildListPropertyDescriptor) visited.getLocationInParent());

			for (Statement stmtToRemove : stmtsToRemove) {
				listRewrite.insertAfter(ASTNodes.createMoveTarget(rewrite, stmtToRemove), visited, group);
			}
		}

		private boolean allRemovable(final boolean[] areCasesRemovable, final int start) {
			for (int i= start; i < areCasesRemovable.length; i++) {
				if (!areCasesRemovable[i]) {
					return false;
				}
			}

			return true;
		}

		private void removeStmtsFromCases(final ASTRewrite rewrite, final TextEditGroup group, final boolean[] areCasesRemovable) {
			for (int i : casesToRefactor) {
				List<Statement> removedStatements= caseStmtsToRemove[i];
				ASTNode parent= allCases.get(i);

				if (removedStatements.containsAll(allCasesStatements.get(i))
						&& (!(parent instanceof IfStatement) || ASTNodes.isPassiveWithoutFallingThrough(((IfStatement) parent).getExpression()))) {
					areCasesRemovable[i]= true;
				} else {
					for (Statement removedStatement : removedStatements) {
						rewrite.remove(removedStatement, group);
					}
				}
			}
		}
	}

	private static final class StatementAndBlockIndices {
		private final Statement statement;
		private final List<Integer> blockIndices;

		private StatementAndBlockIndices(final Statement statement, final List<Integer> blockIndices) {
			this.statement= statement;
			this.blockIndices= blockIndices;
		}

		private Statement getStatement() {
			return statement;
		}

		private List<Integer> getBlockIndices() {
			return blockIndices;
		}
	}
}
