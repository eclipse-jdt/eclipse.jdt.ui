/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class SwitchExpressionsFixCore extends CompilationUnitRewriteOperationsFixCore {

	public static final class SwitchStatementsFinder extends ASTVisitor {

		private List<SwitchExpressionsFixOperation> fResult;

		public SwitchStatementsFinder(List<SwitchExpressionsFixOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(SwitchStatement node) {
			SwitchExpressionsFixOperation operation= getOperation(node);
			if (operation != null) {
				fResult.add(operation);
			}
			return true;
		}

		private boolean isInvalidStatement(Statement statement) {
			return statement instanceof ContinueStatement
					|| statement instanceof ForStatement
					|| statement instanceof ReturnStatement
					|| statement instanceof IfStatement
					|| statement instanceof DoStatement
					|| statement instanceof EnhancedForStatement
					|| statement instanceof SwitchStatement
					|| statement instanceof YieldStatement
					|| statement instanceof TryStatement
					|| statement instanceof WhileStatement;
		}
		private SwitchExpressionsFixOperation getOperation(SwitchStatement switchStatement) {
			final List<SwitchCase> throwList= new ArrayList<>();
			boolean defaultFound= false;
			List<Statement> currentBlock= null;
			SwitchCase currentCase= null;
			Map<SwitchCase, List<Statement>> caseMap= new LinkedHashMap<>();
			for (Iterator<Statement> iter= switchStatement.statements().iterator(); iter.hasNext();) {
				Statement statement= iter.next();
				if (statement instanceof SwitchCase) {
					SwitchCase switchCase= (SwitchCase)statement;
					if (switchCase.isDefault()) {
						defaultFound= true;
					}
					if (currentBlock != null && !currentBlock.isEmpty()) {
						return null;
					}
					if (currentCase != null) {
						caseMap.put(currentCase, currentBlock);
					}
					currentBlock= new ArrayList<>();
					currentCase= switchCase;
				} else if (isInvalidStatement(statement)) {
					return null;
				} else if (statement instanceof BreakStatement) {
					if (currentBlock != null && currentBlock.isEmpty()) {
						return null;
					}
					if (currentCase != null) {
						caseMap.put(currentCase,  currentBlock);
					}
					currentBlock= null;
					currentCase= null;
				} else if (statement instanceof ThrowStatement) {
					throwList.add(currentCase);
					if (currentBlock == null) {
						return null;
					}
					currentBlock.add(statement);
					caseMap.put(currentCase, currentBlock);
					currentBlock= null;
					currentCase= null;
				} else {
					if (currentBlock == null) {
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
								throwList.add(currentCase);
							}
						}
					}
					currentBlock.add(statement);
				}
			}

			// look for final case that has implicit break statement
			if (currentCase != null) {
				if (currentBlock != null && currentBlock.isEmpty()) {
					return null;
				}
				caseMap.put(currentCase,  currentBlock);
			}

			String commonAssignmentName= null;
			IBinding assignmentBinding= null;
			for (Map.Entry<SwitchCase, List<Statement>> entry : caseMap.entrySet()) {
				SwitchCase entryCase= entry.getKey();
				List<Statement> entryStatements= entry.getValue();
				if (throwList.contains(entryCase) || entryStatements.size() == 0) {
					continue;
				}
				Statement lastStatement= entryStatements.get(entryStatements.size() - 1);
				if (lastStatement instanceof Block) {
					@SuppressWarnings("rawtypes")
					List blockStatements= ((Block)lastStatement).statements();
					if (blockStatements.isEmpty()) {
						continue;
					}
					lastStatement= (Statement)(blockStatements.get(blockStatements.size() - 1));
				}
				// case must end in an assignment
				if (!(lastStatement instanceof ExpressionStatement) || !(((ExpressionStatement)lastStatement).getExpression() instanceof Assignment)) {
					return null;
				}
				Assignment assignment= (Assignment)((ExpressionStatement) lastStatement).getExpression();
				// must be simple assign operator
				if (assignment.getOperator() != Assignment.Operator.ASSIGN) {
					return null;
				}
				if (commonAssignmentName == null) {
					Expression exp= assignment.getLeftHandSide();
					if (exp instanceof Name) {
						commonAssignmentName= ((Name)exp).getFullyQualifiedName();
						assignmentBinding= ((Name) exp).resolveBinding();
					}
				} else {
					Expression exp= assignment.getLeftHandSide();
					if (exp instanceof Name) {
						Name name= (Name)exp;
						if (!name.getFullyQualifiedName().equals(commonAssignmentName)) {
							return null;
						}
					}
				}
			}
			if (assignmentBinding == null) {
				return null;
			}
			// ensure either we have default case or else expression is enum and all constants specified
			ITypeBinding binding= switchStatement.getExpression().resolveTypeBinding();
			if (binding != null && binding.isEnum()) {
				IVariableBinding[] fields= binding.getDeclaredFields();
				int enumCount= 0;
				for (IVariableBinding field : fields) {
					if (field.isEnumConstant()) {
						++enumCount;
					}
				}
				if (enumCount != caseMap.size() && !defaultFound) {
					return null;
				}
			} else if (!defaultFound) {
				return null;
			}
			return new SwitchExpressionsFixOperation(switchStatement, caseMap, commonAssignmentName, assignmentBinding);
		}
	}

	public static class SwitchExpressionsFixOperation extends CompilationUnitRewriteOperation {

		private final SwitchStatement switchStatement;
		private final Map<SwitchCase, List<Statement>> caseMap;
		private final String varName;
		private final IBinding assignmentBinding;

		public SwitchExpressionsFixOperation(SwitchStatement switchStatement, Map<SwitchCase, List<Statement>> caseMap,
				String varName, IBinding assignmentBinding) {
			this.switchStatement= switchStatement;
			this.caseMap= caseMap;
			this.varName= varName;
			this.assignmentBinding= assignmentBinding;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {

			final ASTRewrite rewrite= cuRewrite.getASTRewrite();
			final AST ast= rewrite.getAST();

			TextEditGroup group= createTextEditGroup(FixMessages.SwitchExpressionsFix_convert_to_switch_expression, cuRewrite);
			SwitchExpression newSwitchExpression= ast.newSwitchExpression();
			Expression newSwitchExpressionExpression= (Expression)rewrite.createCopyTarget(switchStatement.getExpression());
			newSwitchExpression.setExpression(newSwitchExpressionExpression);
			SwitchCase lastSwitchCase= null;
			// build switch expression
			for (Map.Entry<SwitchCase, List<Statement>> entry : caseMap.entrySet()) {
				SwitchCase oldSwitchCase= entry.getKey();
				List<Statement> oldStatements= entry.getValue();
				if (oldStatements.isEmpty()) {
					// fall-through, want all fall-through labels in single case
					if (lastSwitchCase == null) {
						lastSwitchCase= ast.newSwitchCase();
						lastSwitchCase.setSwitchLabeledRule(true);
						newSwitchExpression.statements().add(lastSwitchCase);
					}
					for (Object obj : oldSwitchCase.expressions()) {
						Expression oldExpression= (Expression)obj;
						Expression newExpression= (Expression)rewrite.createCopyTarget(oldExpression);
						lastSwitchCase.expressions().add(newExpression);
					}
					continue;
				}
				SwitchCase switchCase= null;
				if (lastSwitchCase == null) {
					SwitchCase newSwitchCase= ast.newSwitchCase();
					newSwitchExpression.statements().add(newSwitchCase);
					newSwitchCase.setSwitchLabeledRule(true);
					switchCase= newSwitchCase;
				} else {
					switchCase= lastSwitchCase;
				}
				lastSwitchCase= null;
				for (Object obj : oldSwitchCase.expressions()) {
					Expression oldExpression= (Expression)obj;
					Expression newExpression= (Expression)rewrite.createCopyTarget(oldExpression);
					switchCase.expressions().add(newExpression);
				}
				if (oldStatements.size() == 1 && oldStatements.get(0) instanceof Block) {
					oldStatements= ((Block)oldStatements.get(0)).statements();
				}
				if (oldStatements.size() == 1) {
					Statement oldStatement= oldStatements.get(0);
					Statement newStatement= null;
					if (oldStatement instanceof ThrowStatement) {
						ThrowStatement throwStatement= (ThrowStatement)oldStatement;
						newStatement= (Statement)rewrite.createCopyTarget(throwStatement);
					} else {
						ExpressionStatement oldExpStatement= (ExpressionStatement)oldStatement;
						Assignment oldAssignment= (Assignment)oldExpStatement.getExpression();
						Expression rhs= oldAssignment.getRightHandSide();
						// Ugly hack to tack on trailing comments
						IBuffer buffer= cuRewrite.getCu().getBuffer();
						StringBuilder b= new StringBuilder();
						b.append(buffer.getText(rhs.getStartPosition(), rhs.getLength()) + ";"); //$NON-NLS-1$
						List<Comment> trailingComments= ASTNodes.getTrailingComments(oldExpStatement);
						for (Comment comment : trailingComments) {
							b.append(" " + buffer.getText(comment.getStartPosition(), comment.getLength())); //$NON-NLS-1$
						}
						newStatement= (Statement) rewrite.createStringPlaceholder(b.toString(), ASTNode.EXPRESSION_STATEMENT);
					}
					newSwitchExpression.statements().add(newStatement);
				} else {
					Block newBlock= ast.newBlock();
					int statementsLen= oldStatements.size();
					for (int i= 0; i < statementsLen - 1; ++i) {
						Statement oldSwitchCaseStatement= oldStatements.get(i);
						newBlock.statements().add(rewrite.createCopyTarget(oldSwitchCaseStatement));
					}
					ExpressionStatement oldExpStatement= (ExpressionStatement)oldStatements.get(statementsLen - 1);
					Assignment oldAssignment= (Assignment)oldExpStatement.getExpression();
					Expression rhs= oldAssignment.getRightHandSide();
					IBuffer buffer= cuRewrite.getCu().getBuffer();
					StringBuilder b= new StringBuilder();
					List<Comment> leadingComments= ASTNodes.getLeadingComments(oldExpStatement);
					for (Comment comment : leadingComments) {
						b.append(buffer.getText(comment.getStartPosition(), comment.getLength()) + "\n"); //$NON-NLS-1$
					}
					b.append("yield "); //$NON-NLS-1$
					List<Comment> trailingComments= ASTNodes.getTrailingComments(oldExpStatement);
					b.append(buffer.getText(rhs.getStartPosition(), rhs.getLength()) + ";"); //$NON-NLS-1$
					for (Comment comment : trailingComments) {
						b.append(" " + buffer.getText(comment.getStartPosition(), comment.getLength())); //$NON-NLS-1$
					}

					YieldStatement newYield = (YieldStatement)rewrite.createStringPlaceholder(b.toString(), ASTNode.YIELD_STATEMENT);
					Expression newYieldExpression= (Expression) rewrite.createStringPlaceholder(b.toString(), rhs.getNodeType());
					newYield.setExpression(newYieldExpression);
					newBlock.statements().add(newYield);
					newSwitchExpression.statements().add(newBlock);
				}
			}

			// see if we can make new switch expression the initializer of assignment variable
			if (assignmentBinding instanceof IVariableBinding) {
				VariableDeclarationStatement varDeclarationStatement= null;
				int varIndex= -2;
				IVariableBinding binding= (IVariableBinding)assignmentBinding;
				if (!binding.isField() && !binding.isParameter() && !binding.isSynthetic()) {
					ASTNode parent= switchStatement.getParent();
					if (parent instanceof Block) {
						Block block= (Block)parent;
						List statements= block.statements();
						ListRewrite listRewrite= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
						for (int i= 0; i < statements.size(); ++i) {
							Statement statement= (Statement)statements.get(i);
							if (statement instanceof VariableDeclarationStatement) {
								VariableDeclarationStatement decl= (VariableDeclarationStatement)statement;
								List fragments= decl.fragments();
								if (fragments.size() == 1) { // must be single var declaration
									VariableDeclarationFragment fragment= (VariableDeclarationFragment)fragments.get(0);
									if (fragment.getInitializer() == null) { // must not already be initialized
										IVariableBinding fragBinding= fragment.resolveBinding();
										if (fragBinding != null && fragBinding.isEqualTo(binding)) {
											varDeclarationStatement= decl;
											varIndex= i;
										}
									}
								}
							} else if (statement instanceof SwitchStatement) {
								if (statement.subtreeMatch(new ASTMatcher(), switchStatement)) {
									// if previous statement declares assignment variable, we can set initializer
									if (varIndex == i - 1) {
										VariableDeclarationFragment newVarFragment= ast.newVariableDeclarationFragment();
										newVarFragment.setName(ast.newSimpleName(varName));
										newVarFragment.setInitializer(newSwitchExpression);
										VariableDeclarationStatement newVar= ast.newVariableDeclarationStatement(newVarFragment);
										ImportRewrite importRewrite= cuRewrite.getImportRewrite();
										newVar.setType(importRewrite.addImport(((IVariableBinding) assignmentBinding).getType(), ast));
										if (varDeclarationStatement != null && Modifier.isFinal(varDeclarationStatement.getModifiers())) {
											newVar.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
										}
										replaceWithLeadingComments(cuRewrite, listRewrite, varDeclarationStatement, group, newVar);
										listRewrite.remove(switchStatement, group);
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
			ExpressionStatement newExpressionStatement= ast.newExpressionStatement(newAssignment);
			newAssignment.setLeftHandSide(ast.newName(varName));
			newAssignment.setRightHandSide(newSwitchExpression);

			ASTNode parent= switchStatement.getParent();
			if (parent instanceof Block) {
				ListRewrite listRewrite= rewrite.getListRewrite(parent, Block.STATEMENTS_PROPERTY);
				replaceWithLeadingComments(cuRewrite, listRewrite, switchStatement, group, newExpressionStatement);
			} else {
				rewrite.replace(switchStatement, newExpressionStatement, group);
			}
		}

		private void replaceWithLeadingComments(CompilationUnitRewrite cuRewrite, ListRewrite listRewrite,
				ASTNode oldNode, TextEditGroup group, ASTNode newNode) throws JavaModelException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			List<Comment> comments= ASTNodes.getLeadingComments(oldNode);
			if (!comments.isEmpty()) {
				Comment firstComment= comments.get(0);
				String commentString= cuRewrite.getCu().getBuffer().getText(firstComment.getStartPosition(), firstComment.getLength());
				ASTNode lastComment= rewrite.createStringPlaceholder(commentString, firstComment.isBlockComment() ? ASTNode.BLOCK_COMMENT : ASTNode.LINE_COMMENT);
				listRewrite.replace(oldNode, lastComment, group);
				for (int j= 1; j < comments.size(); ++j) {
					Comment comment= comments.get(j);
					commentString= cuRewrite.getCu().getBuffer().getText(comment.getStartPosition(), comment.getLength());
					ASTNode newComment= rewrite.createStringPlaceholder(commentString, comment.isBlockComment() ? ASTNode.BLOCK_COMMENT : ASTNode.LINE_COMMENT);
					listRewrite.insertAfter(newComment, lastComment, group);
					lastComment= newComment;
				}
				listRewrite.insertAfter(newNode, lastComment, group);
			} else {
				listRewrite.replace(oldNode, newNode, group);
			}
		}

	}


	public static ICleanUpFixCore createCleanUp(CompilationUnit compilationUnit) {
		if (!JavaModelUtil.is14OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		List<SwitchExpressionsFixOperation> operations= new ArrayList<>();
		SwitchStatementsFinder finder= new SwitchStatementsFinder(operations);
		compilationUnit.accept(finder);
		if (operations.isEmpty())
			return null;

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[operations.size()]);
		return new SwitchExpressionsFixCore(FixMessages.SwitchExpressionsFix_convert_to_switch_expression, compilationUnit, ops);
	}

	protected SwitchExpressionsFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
