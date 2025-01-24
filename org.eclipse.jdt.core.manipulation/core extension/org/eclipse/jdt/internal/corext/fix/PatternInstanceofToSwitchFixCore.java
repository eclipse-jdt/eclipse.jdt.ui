/*******************************************************************************
 * Copyright (c) 2021, 2025 Fabrice TIERCELIN and others.
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
 *     Red Hat Inc. - copied and modified SwitchFixCore for new cleanup
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PatternInstanceofExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypePattern;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.SwitchExpressionsFixCore.SwitchExpressionsFixOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class PatternInstanceofToSwitchFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class SwitchStatementsFinder extends ASTVisitor {
		static final class TypeVariable {
			private final Expression name;
			private final TypePattern typePattern;

			private TypeVariable(final Expression firstOp, final TypePattern typePattern) {
				this.name= firstOp;
				this.typePattern= typePattern;
			}

			public boolean isSameVariable(final TypeVariable other) {
				return other != null && ASTNodes.isSameVariable(name, other.name);
			}

			public TypePattern getTypePattern() {
				return typePattern;
			}
		}

		private List<CompilationUnitRewriteOperation> fResult;

		public SwitchStatementsFinder(List<CompilationUnitRewriteOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final Block visited) {
			SeveralIfVisitor severalIfVisitor= new SeveralIfVisitor();
			visited.accept(severalIfVisitor);
			return false;
		}

		final class SeveralIfVisitor extends ASTVisitor {

			@Override
			public boolean visit(final IfStatement visited) {
				TypeVariable variable= extractVariableAndValues(visited);

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
						cases.add(new SwitchCaseSection(variable.getTypePattern(),
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
						&& remainingStatement == null
						&& variable != null
						&& ASTNodes.isSameVariable(switchExpression, variable.name));

				return maybeReplaceWithSwitchStatement(ifStatements, switchExpression, cases, remainingStatement);
			}

			private boolean maybeReplaceWithSwitchStatement(final List<IfStatement> ifStatements, final Expression switchExpression,
					final List<SwitchCaseSection> cases, final Statement remainingStatement) {
				if (switchExpression != null && cases.size() > 2) {
					PatternToSwitchExpressionOperation op= getOperation(ifStatements, switchExpression, cases, remainingStatement);
					if (op != null) {
						fResult.add(op);
					} else {
						fResult.add(new PatternToSwitchOperation(ifStatements, switchExpression,
								cases, remainingStatement));
					}
					return false;
				}

				return true;
			}

			private boolean isInvalidStatement(Statement statement) {
				return statement instanceof ContinueStatement
						|| statement instanceof ForStatement
						|| statement instanceof IfStatement
						|| statement instanceof DoStatement
						|| statement instanceof EnhancedForStatement
						|| statement instanceof SwitchStatement
						|| statement instanceof YieldStatement
						|| statement instanceof TryStatement
						|| statement instanceof WhileStatement;
			}

			private PatternToSwitchExpressionOperation getOperation(List<IfStatement> ifStatements, Expression switchExpression,
					List<SwitchCaseSection> cases, Statement remainingStatement) {
				List<SwitchCaseSection> throwList= new ArrayList<>();
				List<Assignment> assignmentList= new ArrayList<>();
				List<SwitchCaseSection> returnList= new ArrayList<>();
				String assignmentName= null;
				IVariableBinding assignmentBinding= null;
				if (remainingStatement == null || ASTNodes.asList(remainingStatement).size() == 0) {
					return null;
				}
				// add default as a case section with null type pattern
				List<SwitchCaseSection> extendedCases= new ArrayList<>(cases);
				SwitchCaseSection defaultSection= new SwitchCaseSection(null, ASTNodes.asList(remainingStatement));
				extendedCases.add(defaultSection);

				for (SwitchCaseSection switchCaseSection : extendedCases) {
					boolean blockExit= false;
					for (Iterator<Statement> stmtIterator= switchCaseSection.statements.iterator(); stmtIterator.hasNext();) {
						Statement statement= stmtIterator.next();
						if (isInvalidStatement(statement)) {
							return null;
						} else if (statement instanceof ReturnStatement) {
							if (((ReturnStatement)statement).getExpression() == null || stmtIterator.hasNext() || blockExit) {
								return null;
							}
							blockExit= true;
							returnList.add(switchCaseSection);
						} else if (statement instanceof ThrowStatement) {
							if (stmtIterator.hasNext() || blockExit) {
								return null;
							}
							blockExit= true;
							throwList.add(switchCaseSection);
						} else if (statement instanceof ExpressionStatement expStmt && expStmt.getExpression() instanceof Assignment assignment) {
							if (blockExit) {
								return null;
							}
							if (!stmtIterator.hasNext()) {
								if (assignment.getLeftHandSide() instanceof Name name) {
									IBinding binding= name.resolveBinding();
									if (binding instanceof IVariableBinding varBinding) {
										if (assignmentName == null || varBinding.getName().equals(assignmentName)) {
											assignmentName= varBinding.getName();
											assignmentBinding= varBinding;
											assignmentList.add(assignment);
										}
									}
								}
							}
						} else {
							if (blockExit) {
								return null;
							}
							if (statement instanceof Block) {
								Block block= (Block)statement;
								// allow one level of block with no invalid statements inside
								for (Iterator<Statement> blockIter= block.statements().iterator(); blockIter.hasNext();) {
									Statement blockStatement= blockIter.next();
									if (isInvalidStatement(blockStatement) || blockStatement instanceof Block) {
										return null;
									}
									if (blockStatement instanceof ThrowStatement) {
										if (blockIter.hasNext() || blockExit) {
											return null;
										}
										blockExit= true;
										throwList.add(switchCaseSection);
									}
									if (blockStatement instanceof ReturnStatement) {
										if (blockIter.hasNext() || blockExit) {
											return null;
										}
										blockExit= true;
										returnList.add(switchCaseSection);
									}
									if (statement instanceof ExpressionStatement expStmt && expStmt.getExpression() instanceof Assignment assignment) {
										if (blockExit) {
											return null;
										}
										if (!blockIter.hasNext() && !stmtIterator.hasNext()) {
											if (assignment.getLeftHandSide() instanceof Name name) {
												IBinding binding= name.resolveBinding();
												if (binding instanceof IVariableBinding varBinding) {
													if (assignmentName == null || varBinding.getName().equals(assignmentName)) {
														assignmentName= varBinding.getName();
														assignmentList.add(assignment);
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				if (returnList.size() + throwList.size() == extendedCases.size()) {
					return new PatternToSwitchExpressionOperation(ifStatements, switchExpression, extendedCases,
							true, null, null);
				} else if (assignmentList.size() == extendedCases.size()) {
					return new PatternToSwitchExpressionOperation(ifStatements, switchExpression, extendedCases,
							false, assignmentName, assignmentBinding);
				}
				return null;
			}

			private TypeVariable extractVariableAndValues(final Statement statement) {
				if (statement instanceof IfStatement) {
					return extractVariableAndValues(((IfStatement) statement).getExpression());
				}

				return null;
			}

			private TypeVariable extractVariableAndValues(final Expression expression) {
				PatternInstanceofExpression pattern= ASTNodes.as(expression, PatternInstanceofExpression.class);

				if (pattern != null) {
					return extractVariableAndValuesFromPatternExpression(pattern);
				}

				return null;
			}

			private TypeVariable extractVariableAndValuesFromPatternExpression(final PatternInstanceofExpression pattern) {
				TypePattern typePattern= ASTNodes.as(pattern.getPattern(), TypePattern.class);
				if (typePattern != null) {
					return new TypeVariable(pattern.getLeftOperand(), typePattern);
				}
				return null;
			}

		}
	}

	public static class PatternToSwitchOperation extends CompilationUnitRewriteOperation {
		private final List<IfStatement> ifStatements;
		private final Expression switchExpression;
		private final List<SwitchCaseSection> cases;
		private final Statement remainingStatement;

		public PatternToSwitchOperation(final List<IfStatement> ifStatements, final Expression switchExpression, final List<SwitchCaseSection> cases,
				final Statement remainingStatement) {
			this.ifStatements= ifStatements;
			this.switchExpression= switchExpression;
			this.cases= cases;
			this.remainingStatement= remainingStatement;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_PatternInstanceOfToSwitch_description, cuRewrite);
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

			switchStatement.setExpression((Expression) rewrite.createCopyTarget(switchExpression));

			for (SwitchCaseSection aCase : cases) {
				addCaseWithStatements(rewrite, ast, switchStatement, aCase.typePattern, aCase.statements);
			}

			if (remainingStatement != null) {
				remainingStatement.setProperty(UNTOUCH_COMMENT_PROPERTY, Boolean.TRUE);
				addCaseWithStatements(rewrite, ast, switchStatement, null, ASTNodes.asList(remainingStatement));
			} else {
				addCaseWithStatements(rewrite, ast, switchStatement, null, Collections.emptyList());
			}

			for (int i= 0; i < ifStatements.size() - 1; i++) {
				ASTNodes.removeButKeepComment(rewrite, ifStatements.get(i), group);
			}

			ASTNodes.replaceButKeepComment(rewrite, ifStatements.get(ifStatements.size() - 1), newStatement, group);
		}

		private void addCaseWithStatements(final ASTRewrite rewrite, final AST ast, final SwitchStatement switchStatement,
				final TypePattern caseValueOrNullForDefault,
				final List<Statement> innerStatements) {
			List<Statement> switchStatements= switchStatement.statements();
			boolean needBlock= innerStatements.size() > 1 || (innerStatements.size() == 1 && innerStatements.get(0) instanceof ReturnStatement);

			// Add the case statement(s)
			if (caseValueOrNullForDefault != null) {
				Expression caseValue= caseValueOrNullForDefault;
				SwitchCase newSwitchCase= ast.newSwitchCase();
				newSwitchCase.setSwitchLabeledRule(true);
				newSwitchCase.expressions().add(ASTNodes.createMoveTarget(rewrite, caseValue));
				switchStatements.add(newSwitchCase);
			} else {
				SwitchCase newSwitchCase= ast.newSwitchCase();
				newSwitchCase.setSwitchLabeledRule(true);
				newSwitchCase.expressions().add(ast.newNullLiteral());
				newSwitchCase.expressions().add(ast.newCaseDefaultExpression());
				switchStatements.add(newSwitchCase);
			}

			List<Statement> statementsList= switchStatement.statements();
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
			}

			if (needBlock) {
				switchStatements.add(block);
			}
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


	public static class PatternToSwitchExpressionOperation extends CompilationUnitRewriteOperation {

		private List<IfStatement> ifStatements;
		private Expression switchExpression;
		private List<SwitchCaseSection> cases;
		private boolean createReturnStatement;
		private String varName;
		private IVariableBinding assignmentBinding;

		public PatternToSwitchExpressionOperation(final List<IfStatement> ifStatements, final Expression switchExpression,
				final List<SwitchCaseSection> cases, final boolean createReturnStatement,
				final String varName, final IVariableBinding assignmentBinding) {
			this.ifStatements= ifStatements;
			this.switchExpression= switchExpression;
			this.cases= cases;
			this.createReturnStatement= createReturnStatement;
			this.varName= varName;
			this.assignmentBinding= assignmentBinding;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			final ASTRewrite rewrite= cuRewrite.getASTRewrite();
			final AST ast= rewrite.getAST();

			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_PatternInstanceOfToSwitch_description, cuRewrite);
			SwitchExpression newSwitchExpression= ast.newSwitchExpression();
			Expression newSwitchExpressionExpression= (Expression)rewrite.createCopyTarget(switchExpression);
			newSwitchExpression.setExpression(newSwitchExpressionExpression);

			// build switch expression
			for (SwitchCaseSection switchCaseSection : cases) {
				List<Statement> oldStatements= switchCaseSection.statements;
				SwitchCase switchCase= null;
				SwitchCase newSwitchCase= ast.newSwitchCase();
				newSwitchExpression.statements().add(newSwitchCase);
				newSwitchCase.setSwitchLabeledRule(true);
				switchCase= newSwitchCase;
				if (switchCaseSection.typePattern == null) {
					// for the empty pattern, create a null/default case
					Expression nullExpression= ast.newNullLiteral();
					Expression defaultExpression= ast.newCaseDefaultExpression();
					switchCase.expressions().add(nullExpression);
					switchCase.expressions().add(defaultExpression);
				} else {
					Expression oldExpression= switchCaseSection.typePattern;
					Expression newExpression= (Expression)rewrite.createCopyTarget(oldExpression);
					switchCase.expressions().add(newExpression);
				}

				while (true) {
					if (oldStatements.size() == 1 && oldStatements.get(0) instanceof Block) {
						oldStatements= ((Block)oldStatements.get(0)).statements();
					}
					if (oldStatements.size() == 1) {
						Statement oldStatement= oldStatements.get(0);
						Statement newStatement= null;
						if (oldStatement instanceof ThrowStatement) {
							ThrowStatement throwStatement= (ThrowStatement)oldStatement;
							newStatement= (Statement)rewrite.createCopyTarget(throwStatement);
						} else if (oldStatement instanceof ReturnStatement && createReturnStatement) {
							newStatement= SwitchExpressionsFixOperation.getNewStatementFromReturn(cuRewrite, rewrite, (ReturnStatement)oldStatement);
						} else {
							newStatement= SwitchExpressionsFixOperation.getNewStatementForCase(cuRewrite, rewrite, oldStatement);
						}
						newSwitchExpression.statements().add(newStatement);
					} else {
						Block newBlock= ast.newBlock();
						int statementsLen= oldStatements.size();
						for (int i= 0; i < statementsLen - 1; ++i) {
							Statement oldSwitchCaseStatement= oldStatements.get(i);
							newBlock.statements().add(rewrite.createCopyTarget(oldSwitchCaseStatement));
						}
						Statement lastStatement= oldStatements.get(statementsLen - 1);
						Statement newStatement= null;
						if (lastStatement instanceof ThrowStatement) {
							ThrowStatement throwStatement= (ThrowStatement)lastStatement;
							newStatement= (Statement)rewrite.createCopyTarget(throwStatement);
						} else if (lastStatement instanceof ReturnStatement) {
							newStatement= SwitchExpressionsFixOperation.getNewYieldStatementFromReturn(cuRewrite, rewrite, (ReturnStatement)oldStatements.get(statementsLen-1));
						} else {
							newStatement= SwitchExpressionsFixOperation.getNewYieldStatement(cuRewrite, rewrite, (ExpressionStatement)oldStatements.get(statementsLen-1));
						}
						newBlock.statements().add(newStatement);
						newSwitchExpression.statements().add(newBlock);
					}
					break;
				}
			}

			Statement newExpressionStatement= null;

			if (createReturnStatement) {
				ReturnStatement newReturnStatement= ast.newReturnStatement();
				newReturnStatement.setExpression(newSwitchExpression);
				newExpressionStatement= newReturnStatement;
			} else {
				// see if we can make new switch expression the initializer of assignment variable
				if (assignmentBinding != null) {
					VariableDeclarationStatement varDeclarationStatement= null;
					int varIndex= -2;
					IVariableBinding binding= assignmentBinding;
					if (!binding.isField() && !binding.isParameter() && !binding.isSynthetic()) {
						ASTNode parent= ifStatements.get(0).getParent();
						if (parent instanceof Block) {
							Block block= (Block)parent;
							List<Statement> statements= block.statements();
							ListRewrite listRewrite= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
							for (int i= 0; i < statements.size(); ++i) {
								Statement statement= statements.get(i);
								if (statement instanceof VariableDeclarationStatement) {
									VariableDeclarationStatement decl= (VariableDeclarationStatement)statement;
									List<VariableDeclarationFragment> fragments= decl.fragments();
									if (fragments.size() == 1) { // must be single var declaration
										VariableDeclarationFragment fragment= fragments.get(0);
										if (fragment.getInitializer() == null) { // must not already be initialized
											IVariableBinding fragBinding= fragment.resolveBinding();
											if (fragBinding != null && fragBinding.isEqualTo(binding)) {
												varDeclarationStatement= decl;
												varIndex= i;
											}
										}
									}
								} else if (statement instanceof IfStatement) {
									if (statement.subtreeMatch(new ASTMatcher(), ifStatements.get(0))) {
										// if previous statement declares assignment variable, we can set initializer
										if (varIndex == i - 1) {
											VariableDeclarationFragment newVarFragment= ast.newVariableDeclarationFragment();
											newVarFragment.setName(ast.newSimpleName(varName));
											newVarFragment.setInitializer(newSwitchExpression);
											VariableDeclarationStatement newVar= ast.newVariableDeclarationStatement(newVarFragment);
											ImportRewrite importRewrite= cuRewrite.getImportRewrite();
											newVar.setType(importRewrite.addImport(assignmentBinding.getType(), ast));
											if (varDeclarationStatement != null && Modifier.isFinal(varDeclarationStatement.getModifiers())) {
												newVar.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
											}
											SwitchExpressionsFixOperation.replaceWithLeadingComments(cuRewrite, listRewrite, varDeclarationStatement, group, newVar);
											listRewrite.remove(ifStatements.get(0), group);
											return;
										}
										break;
									}
								}
							}
						}
					}
				}
				// otherwise just assign new switch expression to varName
				Assignment newAssignment= ast.newAssignment();
				newExpressionStatement= ast.newExpressionStatement(newAssignment);
				newAssignment.setLeftHandSide(ast.newName(varName));
				newAssignment.setRightHandSide(newSwitchExpression);
			}

			ASTNode parent= ifStatements.get(0).getParent();
			if (parent instanceof Block) {
				ListRewrite listRewrite= rewrite.getListRewrite(parent, Block.STATEMENTS_PROPERTY);
				SwitchExpressionsFixOperation.replaceWithLeadingComments(cuRewrite, listRewrite, ifStatements.get(0), group, newExpressionStatement);
			} else {
				rewrite.replace(ifStatements.get(0), newExpressionStatement, group);
			}

		}

	}

	public static ICleanUpFix createCleanUp(final CompilationUnit compilationUnit) {
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();

		if (cu == null || !JavaModelUtil.is21OrHigher(cu.getJavaProject())) {
			return null;
		}

		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		SwitchStatementsFinder finder= new SwitchStatementsFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new PatternInstanceofToSwitchFixCore(FixMessages.SwitchFix_convert_if_to_switch, compilationUnit, ops);
	}

	public static PatternInstanceofToSwitchFixCore createPatternInstanceofToSwitchFix(IfStatement ifStmt) {
		CompilationUnit compilationUnit= (CompilationUnit) ifStmt.getRoot();
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();

		if (cu == null || !JavaModelUtil.is21OrHigher(cu.getJavaProject())) {
			return null;
		}

		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		SwitchStatementsFinder finder= new SwitchStatementsFinder(operations);
		SwitchStatementsFinder.SeveralIfVisitor visitor= finder.new SeveralIfVisitor();
		ifStmt.accept(visitor);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new PatternInstanceofToSwitchFixCore(FixMessages.PatternInstanceof_convert_if_to_switch, compilationUnit, ops);
	}

	protected PatternInstanceofToSwitchFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
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
		/** The statements executed for the switch cases. */
		private final List<Statement> statements;

		/** The type pattern to use for the case. */
		private final TypePattern typePattern;

		/**
		 * Used for if statements, only constant expressions are used.
		 *
		 * @param typePattern The type pattern
		 * @param statements The statements
		 */
		private SwitchCaseSection(final TypePattern typePattern,
				final List<Statement> statements) {
			this.typePattern= typePattern;
			this.statements= statements;
		}

	}
}
