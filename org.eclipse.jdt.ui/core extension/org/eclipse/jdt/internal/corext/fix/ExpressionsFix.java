/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.refactoring.code.OperatorPrecedence;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class ExpressionsFix extends CompilationUnitRewriteOperationsFix {

	private static final class MissingParenthesisVisitor extends ASTVisitor {

		private final ArrayList fNodes;

		private MissingParenthesisVisitor(ArrayList nodes) {
			fNodes= nodes;
		}

		public void postVisit(ASTNode node) {
			if (needsParentesis(node)) {
				fNodes.add(node);
			}
		}

		private boolean needsParentesis(ASTNode node) {
			// check that parent is && or ||
			if (!(node.getParent() instanceof InfixExpression))
				return false;

			// we want to add parenthesis around arithmetic operators and instanceof
			if (node instanceof InstanceofExpression)
				return true;

			if (node instanceof InfixExpression) {
				InfixExpression expression = (InfixExpression) node;
				InfixExpression.Operator operator = expression.getOperator();

				InfixExpression parentExpression = (InfixExpression) node.getParent();
				InfixExpression.Operator parentOperator = parentExpression.getOperator();

				if (parentOperator == operator)
					return false;


				return (operator == InfixExpression.Operator.LESS)
						|| (operator == InfixExpression.Operator.GREATER)
						|| (operator == InfixExpression.Operator.LESS_EQUALS)
						|| (operator == InfixExpression.Operator.GREATER_EQUALS)
						|| (operator == InfixExpression.Operator.EQUALS)
						|| (operator == InfixExpression.Operator.NOT_EQUALS)

						|| (operator == InfixExpression.Operator.CONDITIONAL_AND)
						|| (operator == InfixExpression.Operator.CONDITIONAL_OR);
			}

			return false;
		}
	}

	private static final class UnnecessaryParenthesisVisitor extends ASTVisitor {

		private final ArrayList fNodes;

		private UnnecessaryParenthesisVisitor(ArrayList nodes) {
			fNodes= nodes;
		}

		public boolean visit(ParenthesizedExpression node) {
			if (canRemoveParenthesis(node)) {
				fNodes.add(node);
			}

			return true;
		}

		/*
		 * Can the parenthesis around node be removed?
		 */
		private boolean canRemoveParenthesis(ParenthesizedExpression node) {
			ASTNode parent= node.getParent();
			if (!(parent instanceof Expression))
				return true;

			Expression parentExpression= (Expression) parent;
			if (parentExpression instanceof ParenthesizedExpression)
				return true;

			Expression expression= getExpression(node);

			int expressionPrecedence= OperatorPrecedence.getExpressionPrecedence(expression);
			int parentPrecedence= OperatorPrecedence.getExpressionPrecedence(parentExpression);

			if (expressionPrecedence > parentPrecedence)
				//(opEx) opParent and opEx binds more -> can safely remove
				return true;

			if (expressionPrecedence < parentPrecedence)
				//(opEx) opParent and opEx binds less -> do not remove
				return false;

			//(opEx) opParent binds equal

			if (parentExpression instanceof InfixExpression) {
				InfixExpression parentInfix= (InfixExpression) parentExpression;
				if (parentInfix.getLeftOperand() == node) {
					//we have (expr op expr) op expr
					//infix expressions are evaluated from left to right -> can safely remove
					return true;
				} else if (isAssociative(parentInfix)) {
					//we have parent op (expr op expr) and op is associative
					//left op (right) == (right) op left == right op left
					if (expression instanceof InfixExpression) {
						InfixExpression infixExpression= (InfixExpression) expression;
						Operator operator= infixExpression.getOperator();
						if (parentInfix.getOperator() != InfixExpression.Operator.TIMES)
							return true;

						if (operator == InfixExpression.Operator.TIMES)
							// x * (y * z) == x * y * z
							return true;

						if (operator == InfixExpression.Operator.REMAINDER)
							// x * (y % z) != x * y % z
							return false;

						//x * (y / z) == z * y / z  iff no rounding
						ITypeBinding binding= infixExpression.resolveTypeBinding();
						if (binding == null)
							return false;

						if (!binding.isPrimitive())
							return false;

						String name= binding.getName();
						if (isIntegerNumber(name))
							//rounding involved
							return false;

						return true;
					}
					return true;
				} else {
					return false;
				}
			} else if (parentExpression instanceof ConditionalExpression) {
				ConditionalExpression conditionalExpression= (ConditionalExpression) parentExpression;
				if (conditionalExpression.getElseExpression() != node)
					return false;
			}

			return true;
		}

		private boolean isIntegerNumber(String name) {
			return "int".equals(name) || "long".equals(name) || "byte".equals(name) || "char".equals(name) || "short".equals(name); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}

		/*
		 * Get the expression wrapped by the parentheses
		 * i.e. ((((expression)))) -> expression
		 */
		private Expression getExpression(ParenthesizedExpression node) {
			Expression expression= node.getExpression();
			while (expression instanceof ParenthesizedExpression) {
				expression= ((ParenthesizedExpression) expression).getExpression();
			}
			return expression;
		}

		/**
		 * Is the given expression associative?
		 * <p>
		 * This is true if and only if:<br>
		 * <code>left operator (right) == (right) operator left == right operator left</code>
		 * </p>
		 *
		 * @param expression the expression to inspect
		 * @return true if expression is associative
		 */
		public static boolean isAssociative(InfixExpression expression) {
			Operator operator= expression.getOperator();
			if (operator == InfixExpression.Operator.PLUS) {
				return isAllOperandsHaveSameType(expression);
			}

			if (operator == Operator.LESS || operator == Operator.LESS_EQUALS || operator == Operator.GREATER || operator == Operator.GREATER_EQUALS) {
				return isAllOperandsHaveSameType(expression);
			}

			if (operator == InfixExpression.Operator.CONDITIONAL_AND)
				return true;

			if (operator == InfixExpression.Operator.CONDITIONAL_OR)
				return true;

			if (operator == InfixExpression.Operator.AND)
				return true;

			if (operator == InfixExpression.Operator.OR)
				return true;

			if (operator == InfixExpression.Operator.XOR)
				return true;

			if (operator == InfixExpression.Operator.TIMES)
				return true;

			return false;
		}

		/*
		 * Do all operands in expression have same type
		 */
		private static boolean isAllOperandsHaveSameType(InfixExpression expression) {
			ITypeBinding binding= expression.getLeftOperand().resolveTypeBinding();
			if (binding == null)
				return false;

			ITypeBinding current= expression.getRightOperand().resolveTypeBinding();
			if (binding != current)
				return false;

			for (Iterator iterator= expression.extendedOperands().iterator(); iterator.hasNext();) {
				Expression operand= (Expression) iterator.next();
				current= operand.resolveTypeBinding();
				if (binding != current)
					return false;
			}

			return true;
		}
	}

	private static class AddParenthesisOperation extends CompilationUnitRewriteOperation {

		private final Expression[] fExpressions;

		public AddParenthesisOperation(Expression[] expressions) {
			fExpressions= expressions;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {
			TextEditGroup group= createTextEditGroup(FixMessages.ExpressionsFix_addParanoiacParenthesis_description, cuRewrite);

			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			for (int i= 0; i < fExpressions.length; i++) {
				// add parenthesis around expression
				Expression expression= fExpressions[i];

				ParenthesizedExpression parenthesizedExpression= ast.newParenthesizedExpression();
				parenthesizedExpression.setExpression((Expression) rewrite.createCopyTarget(expression));
				rewrite.replace(expression, parenthesizedExpression, group);
			}
		}
	}

	private static class RemoveParenthesisOperation extends CompilationUnitRewriteOperation {

		private final HashSet/*<ParenthesizedExpression>*/ fExpressions;

		public RemoveParenthesisOperation(HashSet expressions) {
			fExpressions= expressions;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {
			TextEditGroup group= createTextEditGroup(FixMessages.ExpressionsFix_removeUnnecessaryParenthesis_description, cuRewrite);

			ASTRewrite rewrite= cuRewrite.getASTRewrite();

			while (fExpressions.size() > 0) {
				ParenthesizedExpression parenthesizedExpression= (ParenthesizedExpression)fExpressions.iterator().next();
				fExpressions.remove(parenthesizedExpression);
				ParenthesizedExpression down= parenthesizedExpression;
				while (fExpressions.contains(down.getExpression())) {
					down= (ParenthesizedExpression)down.getExpression();
					fExpressions.remove(down);
				}

				ASTNode move= rewrite.createMoveTarget(down.getExpression());

				ParenthesizedExpression top= parenthesizedExpression;
				while (fExpressions.contains(top.getParent())) {
					top= (ParenthesizedExpression)top.getParent();
					fExpressions.remove(top);
				}

				rewrite.replace(top, move, group);
			}
		}
	}

	public static ExpressionsFix createAddParanoidalParenthesisFix(CompilationUnit compilationUnit, ASTNode[] coveredNodes) {
		if (coveredNodes == null)
			return null;

		if (coveredNodes.length == 0)
			return null;
		// check sub-expressions in fully covered nodes
		final ArrayList changedNodes = new ArrayList();
		for (int i= 0; i < coveredNodes.length; i++) {
			ASTNode covered = coveredNodes[i];
			if (covered instanceof InfixExpression)
				covered.accept(new MissingParenthesisVisitor(changedNodes));
		}
		if (changedNodes.isEmpty())
			return null;


		CompilationUnitRewriteOperation op= new AddParenthesisOperation((Expression[])changedNodes.toArray(new Expression[changedNodes.size()]));
		return new ExpressionsFix(FixMessages.ExpressionsFix_addParanoiacParenthesis_description, compilationUnit, new CompilationUnitRewriteOperation[] {op});
	}

	public static ExpressionsFix createRemoveUnnecessaryParenthesisFix(CompilationUnit compilationUnit, ASTNode[] nodes) {
		// check sub-expressions in fully covered nodes
		final ArrayList changedNodes= new ArrayList();
		for (int i= 0; i < nodes.length; i++) {
			ASTNode covered= nodes[i];
			if (covered instanceof ParenthesizedExpression || covered instanceof InfixExpression)
				covered.accept(new UnnecessaryParenthesisVisitor(changedNodes));
		}
		if (changedNodes.isEmpty())
			return null;

		HashSet expressions= new HashSet(changedNodes);
		RemoveParenthesisOperation op= new RemoveParenthesisOperation(expressions);
		return new ExpressionsFix(FixMessages.ExpressionsFix_removeUnnecessaryParenthesis_description, compilationUnit, new CompilationUnitRewriteOperation[] {op});
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit,
			boolean addParanoicParentesis,
			boolean removeUnnecessaryParenthesis) {

		if (addParanoicParentesis) {
			final ArrayList changedNodes = new ArrayList();
			compilationUnit.accept(new MissingParenthesisVisitor(changedNodes));

			if (changedNodes.isEmpty())
				return null;

			CompilationUnitRewriteOperation op= new AddParenthesisOperation((Expression[])changedNodes.toArray(new Expression[changedNodes.size()]));
			return new ExpressionsFix(FixMessages.ExpressionsFix_add_parenthesis_change_name, compilationUnit, new CompilationUnitRewriteOperation[] {op});
		} else if (removeUnnecessaryParenthesis) {
			final ArrayList changedNodes = new ArrayList();
			compilationUnit.accept(new UnnecessaryParenthesisVisitor(changedNodes));

			if (changedNodes.isEmpty())
				return null;

			HashSet expressions= new HashSet(changedNodes);
			CompilationUnitRewriteOperation op= new RemoveParenthesisOperation(expressions);
			return new ExpressionsFix(FixMessages.ExpressionsFix_remove_parenthesis_change_name, compilationUnit, new CompilationUnitRewriteOperation[] {op});
		}
		return null;
	}

	protected ExpressionsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
