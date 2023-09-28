/*******************************************************************************
 * Copyright (c) 2023 Gayan Perera and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gayan Perera - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.code.Invocations;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public final class JoinVariableFixCore extends CompilationUnitRewriteOperationsFixCore {

	public JoinVariableFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] operations) {
		super(name, compilationUnit, operations);
	}

	public static JoinVariableFixCore createJoinVariableFix(CompilationUnit compilationUnit, ASTNode node) {
		ASTNode parent= node.getParent();

		VariableDeclarationFragment fragment= null;
		boolean onFirstAccess= false;
		if (node instanceof SimpleName && node.getLocationInParent() == Assignment.LEFT_HAND_SIDE_PROPERTY) {
			onFirstAccess= true;
			SimpleName name= (SimpleName) node;
			IBinding binding= name.resolveBinding();
			if (!(binding instanceof IVariableBinding)) {
				return null;
			}
			ASTNode declaring= compilationUnit.findDeclaringNode(binding);
			if (declaring instanceof VariableDeclarationFragment) {
				fragment= (VariableDeclarationFragment) declaring;
			} else {
				return null;
			}
		} else if (parent instanceof VariableDeclarationFragment) {
			fragment= (VariableDeclarationFragment) parent;
		} else {
			return null;
		}

		IVariableBinding binding= fragment.resolveBinding();
		Expression initializer= fragment.getInitializer();
		if ((initializer != null && initializer.getNodeType() != ASTNode.NULL_LITERAL) || binding == null || binding.isField()) {
			return null;
		}

		if (!(fragment.getParent() instanceof VariableDeclarationStatement)) {
			return null;
		}
		VariableDeclarationStatement statement= (VariableDeclarationStatement) fragment.getParent();

		SimpleName[] names= LinkedNodeFinder.findByBinding(statement.getParent(), binding);
		if (names.length <= 1 || names[0] != fragment.getName()) {
			return null;
		}
		SimpleName firstAccess= names[1];
		if (onFirstAccess) {
			if (firstAccess != node) {
				return null;
			}
		} else {
			if (firstAccess.getLocationInParent() != Assignment.LEFT_HAND_SIDE_PROPERTY) {
				return null;
			}
		}
		Assignment assignment= (Assignment) firstAccess.getParent();
		if (assignment.getLocationInParent() != ExpressionStatement.EXPRESSION_PROPERTY) {
			return null;
		}
		ExpressionStatement assignParent= (ExpressionStatement) assignment.getParent();
		IfStatement ifStatement= null;
		Expression thenExpression= null;
		Expression elseExpression= null;
		ITypeBinding exprBinding= null;

		ASTNode assignParentParent= assignParent.getParent();
		if (assignParentParent instanceof IfStatement
				|| (assignParentParent.getLocationInParent() == IfStatement.THEN_STATEMENT_PROPERTY
						&& !(assignParentParent.subtreeMatch(new ASTMatcher(), statement.getParent())))) {
			if (assignParentParent.getLocationInParent() == IfStatement.THEN_STATEMENT_PROPERTY) {
				assignParentParent= assignParentParent.getParent();
			}
			ifStatement= (IfStatement) assignParentParent;
			Statement thenStatement= getSingleStatement(ifStatement.getThenStatement());
			Statement elseStatement= getSingleStatement(ifStatement.getElseStatement());
			if (thenStatement == null || elseStatement == null) {
				return null;
			}

			if (thenStatement instanceof ExpressionStatement && elseStatement instanceof ExpressionStatement) {
				Expression inner1= ((ExpressionStatement) thenStatement).getExpression();
				Expression inner2= ((ExpressionStatement) elseStatement).getExpression();
				if (inner1 instanceof Assignment && inner2 instanceof Assignment) {
					Assignment assign1= (Assignment) inner1;
					Assignment assign2= (Assignment) inner2;
					Expression left1= assign1.getLeftHandSide();
					Expression left2= assign2.getLeftHandSide();
					if (left1 instanceof Name && left2 instanceof Name && assign1.getOperator() == assign2.getOperator()) {
						IBinding bind1= ((Name) left1).resolveBinding();
						IBinding bind2= ((Name) left2).resolveBinding();
						if (bind1 == bind2 && bind1 instanceof IVariableBinding) {
							exprBinding= ((IVariableBinding) bind1).getType();
							thenExpression= assign1.getRightHandSide();
							elseExpression= assign2.getRightHandSide();
						}
					}
				}
			}
			if (thenExpression == null || elseExpression == null) {
				return null;
			}
		} else {
			// Be conservative and don't allow anything but Blocks between the
			// VariableDeclarationStatement and the ExpressionStatement to join
			ASTNode n= assignParent.getParent();
			ASTNode statementParent= statement.getParent();
			ASTMatcher matcher= new ASTMatcher();
			while (n != null) {
				if (n.getNodeType() == statementParent.getNodeType() && (n.subtreeMatch(matcher, statementParent))) {
					break;
				} else if (n instanceof Block) {
					n= n.getParent();
				} else {
					return null;
				}
			}
		}

		return new JoinVariableFixCore(CorrectionMessages.QuickAssistProcessor_joindeclaration_description, compilationUnit,
				new CompilationUnitRewriteOperation[] {
						new JoinVariableProposalOperation(statement, ifStatement, assignParent, thenExpression, elseExpression, exprBinding, fragment, onFirstAccess, assignment) });
	}

	private static Statement getSingleStatement(Statement statement) {
		if (statement instanceof Block) {
			List<Statement> blockStatements= ((Block) statement).statements();
			if (blockStatements.size() != 1) {
				return null;
			}
			return blockStatements.get(0);
		}
		return statement;
	}

	private static class JoinVariableProposalOperation extends CompilationUnitRewriteOperation {
		private VariableDeclarationStatement statement;

		private IfStatement ifStatement;

		private ExpressionStatement assignParent;

		private Expression thenExpression;

		private Expression elseExpression;

		private ITypeBinding exprBinding;

		private VariableDeclarationFragment fragment;

		private boolean onFirstAccess;

		private Assignment assignment;

		public JoinVariableProposalOperation(VariableDeclarationStatement statement, IfStatement ifStatement, ExpressionStatement assignParent, Expression thenExpression, Expression elseExpression,
				ITypeBinding exprBinding, VariableDeclarationFragment fragment, boolean onFirstAccess, Assignment assignment) {
			this.statement= statement;
			this.ifStatement= ifStatement;
			this.assignParent= assignParent;
			this.thenExpression= thenExpression;
			this.elseExpression= elseExpression;
			this.exprBinding= exprBinding;
			this.fragment= fragment;
			this.onFirstAccess= onFirstAccess;
			this.assignment= assignment;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			final ASTRewrite rewrite= cuRewrite.getASTRewrite();
			final CompilationUnit cup= (CompilationUnit) statement.getRoot();
			final AST ast= cuRewrite.getAST();
			final IJavaProject project= cup.getTypeRoot().getJavaProject();

			TightSourceRangeComputer sourceRangeComputer= new TightSourceRangeComputer();
			sourceRangeComputer.addTightSourceNode(ifStatement != null ? ifStatement : assignParent);
			rewrite.setTargetSourceRangeComputer(sourceRangeComputer);

			if (ifStatement != null) {
				// prepare conditional expression
				ConditionalExpression conditionalExpression= ast.newConditionalExpression();
				Expression conditionCopy= (Expression) rewrite.createCopyTarget(ifStatement.getExpression());
				conditionalExpression.setExpression(conditionCopy);
				Expression thenCopy= (Expression) rewrite.createCopyTarget(thenExpression);
				Expression elseCopy= (Expression) rewrite.createCopyTarget(elseExpression);

				if (!JavaModelUtil.is50OrHigher(project)) {
					ITypeBinding thenBinding= thenExpression.resolveTypeBinding();
					ITypeBinding elseBinding= elseExpression.resolveTypeBinding();
					if (thenBinding != null && elseBinding != null && exprBinding != null && !elseBinding.isAssignmentCompatible(thenBinding)) {
						CastExpression castException= ast.newCastExpression();
						ImportRewrite importRewrite= cuRewrite.getImportRewrite();
						ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(cup, importRewrite);
						castException.setType(importRewrite.addImport(exprBinding, ast, importRewriteContext, TypeLocation.CAST));
						castException.setExpression(elseCopy);
						elseCopy= castException;
					}
				} else if (JavaModelUtil.is1d7OrHigher(project)) {
					addExplicitTypeArgumentsIfNecessary(rewrite, cuRewrite, thenExpression);
					addExplicitTypeArgumentsIfNecessary(rewrite, cuRewrite, elseExpression);
				}
				conditionalExpression.setThenExpression(thenCopy);
				conditionalExpression.setElseExpression(elseCopy);
				rewrite.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, conditionalExpression, null);
				rewrite.remove(ifStatement, null);
			} else {
				Expression placeholder= (Expression) rewrite.createMoveTarget(assignment.getRightHandSide());
				rewrite.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, placeholder, null);

				if (onFirstAccess) {
					// replace assignment with variable declaration
					rewrite.replace(assignParent, rewrite.createMoveTarget(statement), null);
				} else {
					// different scopes -> remove assignments, set variable initializer
					if (ASTNodes.isControlStatementBody(assignParent.getLocationInParent())) {
						Block block= ast.newBlock();
						rewrite.replace(assignParent, block, null);
					} else {
						rewrite.remove(assignParent, null);
					}
				}
			}
		}

		private static void addExplicitTypeArgumentsIfNecessary(ASTRewrite rewrite, CompilationUnitRewrite proposal, Expression invocation) {
			if (Invocations.isResolvedTypeInferredFromExpectedType(invocation)) {
				ITypeBinding[] typeArguments= Invocations.getInferredTypeArguments(invocation);
				if (typeArguments == null)
					return;

				ImportRewrite importRewrite= proposal.getImportRewrite();
				ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(invocation, importRewrite);

				AST ast= invocation.getAST();
				ListRewrite typeArgsRewrite= Invocations.getInferredTypeArgumentsRewrite(rewrite, invocation);

				for (ITypeBinding typeArgument : typeArguments) {
					Type typeArgumentNode= importRewrite.addImport(typeArgument, ast, importRewriteContext, TypeLocation.TYPE_ARGUMENT);
					typeArgsRewrite.insertLast(typeArgumentNode, null);
				}

				if (invocation instanceof MethodInvocation) {
					MethodInvocation methodInvocation= (MethodInvocation) invocation;
					Expression expression= methodInvocation.getExpression();
					if (expression == null) {
						IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
						if (methodBinding != null && Modifier.isStatic(methodBinding.getModifiers())) {
							expression= ast.newName(importRewrite.addImport(methodBinding.getDeclaringClass().getTypeDeclaration(), importRewriteContext));
						} else {
							expression= ast.newThisExpression();
						}
						rewrite.set(invocation, MethodInvocation.EXPRESSION_PROPERTY, expression, null);
					}
				}
			}
		}

	}

}
