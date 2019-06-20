/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.core.manipulation.dom.NecessaryParenthesesChecker;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.NoCommentSourceRangeComputer;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class ExpressionsFix extends CompilationUnitRewriteOperationsFix {

	private static final class MissingParenthesisVisitor extends ASTVisitor {

		private final ArrayList<ASTNode> fNodes;

		private MissingParenthesisVisitor(ArrayList<ASTNode> nodes) {
			fNodes= nodes;
		}

		@Override
		public void postVisit(ASTNode node) {
			if (needsParentesis(node)) {
				fNodes.add(node);
			}
		}

		private boolean needsParentesis(ASTNode node) {
			if (!(node.getParent() instanceof InfixExpression))
				return false;

			if (node instanceof InstanceofExpression)
				return true;

			if (node instanceof InfixExpression) {
				InfixExpression expression = (InfixExpression) node;
				InfixExpression.Operator operator = expression.getOperator();

				InfixExpression parentExpression = (InfixExpression) node.getParent();
				InfixExpression.Operator parentOperator = parentExpression.getOperator();

				if (parentOperator == operator)
					return false;

				return true;
			}

			return false;
		}
	}

	private static final class UnnecessaryParenthesisVisitor extends ASTVisitor {

		private final ArrayList<ParenthesizedExpression> fNodes;

		private UnnecessaryParenthesisVisitor(ArrayList<ParenthesizedExpression> nodes) {
			fNodes= nodes;
		}

		@Override
		public boolean visit(ParenthesizedExpression node) {
			if (NecessaryParenthesesChecker.canRemoveParentheses(node)) {
				fNodes.add(node);
			}

			return true;
		}
	}

	private static class AddParenthesisOperation extends CompilationUnitRewriteOperation {

		private final Expression[] fExpressions;

		public AddParenthesisOperation(Expression[] expressions) {
			fExpressions= expressions;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {
			TextEditGroup group= createTextEditGroup(FixMessages.ExpressionsFix_addParanoiacParentheses_description, cuRewrite);

			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			for (Expression expression : fExpressions) {
				// add parenthesis around expression
				ParenthesizedExpression parenthesizedExpression= ast.newParenthesizedExpression();
				parenthesizedExpression.setExpression((Expression) rewrite.createCopyTarget(expression));
				rewrite.replace(expression, parenthesizedExpression, group);
			}
		}
	}

	private static class RemoveParenthesisOperation extends CompilationUnitRewriteOperation {

		private final HashSet<ParenthesizedExpression> fExpressions;

		public RemoveParenthesisOperation(HashSet<ParenthesizedExpression> expressions) {
			fExpressions= expressions;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {
			TextEditGroup group= createTextEditGroup(FixMessages.ExpressionsFix_removeUnnecessaryParentheses_description, cuRewrite);

			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			rewrite.setTargetSourceRangeComputer(new NoCommentSourceRangeComputer());

			while (fExpressions.size() > 0) {
				ParenthesizedExpression parenthesizedExpression= fExpressions.iterator().next();
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
		final ArrayList<ASTNode> changedNodes = new ArrayList<>();
		for (ASTNode covered : coveredNodes) {
			if (covered instanceof InfixExpression)
				covered.accept(new MissingParenthesisVisitor(changedNodes));
		}
		if (changedNodes.isEmpty() || (changedNodes.size() == 1 && changedNodes.get(0).equals(coveredNodes[0])))
			return null;


		CompilationUnitRewriteOperation op= new AddParenthesisOperation(changedNodes.toArray(new Expression[changedNodes.size()]));
		return new ExpressionsFix(FixMessages.ExpressionsFix_addParanoiacParentheses_description, compilationUnit, new CompilationUnitRewriteOperation[] {op});
	}

	public static ExpressionsFix createRemoveUnnecessaryParenthesisFix(CompilationUnit compilationUnit, ASTNode[] nodes) {
		// check sub-expressions in fully covered nodes
		final ArrayList<ParenthesizedExpression> changedNodes= new ArrayList<>();
		for (ASTNode covered : nodes) {
			if (covered instanceof ParenthesizedExpression || covered instanceof InfixExpression)
				covered.accept(new UnnecessaryParenthesisVisitor(changedNodes));
		}
		if (changedNodes.isEmpty())
			return null;

		HashSet<ParenthesizedExpression> expressions= new HashSet<>(changedNodes);
		RemoveParenthesisOperation op= new RemoveParenthesisOperation(expressions);
		return new ExpressionsFix(FixMessages.ExpressionsFix_removeUnnecessaryParentheses_description, compilationUnit, new CompilationUnitRewriteOperation[] {op});
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit,
			boolean addParanoicParentesis,
			boolean removeUnnecessaryParenthesis) {

		if (addParanoicParentesis) {
			final ArrayList<ASTNode> changedNodes = new ArrayList<>();
			compilationUnit.accept(new MissingParenthesisVisitor(changedNodes));

			if (changedNodes.isEmpty())
				return null;

			CompilationUnitRewriteOperation op= new AddParenthesisOperation(changedNodes.toArray(new Expression[changedNodes.size()]));
			return new ExpressionsFix(FixMessages.ExpressionsFix_add_parentheses_change_name, compilationUnit, new CompilationUnitRewriteOperation[] {op});
		} else if (removeUnnecessaryParenthesis) {
			final ArrayList<ParenthesizedExpression> changedNodes = new ArrayList<>();
			compilationUnit.accept(new UnnecessaryParenthesisVisitor(changedNodes));

			if (changedNodes.isEmpty())
				return null;

			HashSet<ParenthesizedExpression> expressions= new HashSet<>(changedNodes);
			CompilationUnitRewriteOperation op= new RemoveParenthesisOperation(expressions);
			return new ExpressionsFix(FixMessages.ExpressionsFix_remove_parentheses_change_name, compilationUnit, new CompilationUnitRewriteOperation[] {op});
		}
		return null;
	}

	protected ExpressionsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
