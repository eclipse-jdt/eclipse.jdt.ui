/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Konstantin Scheglov (scheglov_ke@nlmk.ru) - initial API and implementation 
 *          (report 71244: New Quick Assist's [quick assist])
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 */
public class AdvancedQuickAssistProcessor implements IQuickAssistProcessor {
	public AdvancedQuickAssistProcessor() {
		super();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.IAssistProcessor#hasAssists(org.eclipse.jdt.internal.ui.text.correction.IAssistContext)
	 */
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		ASTNode coveringNode = context.getCoveringNode();
		if (coveringNode != null) {
			return getInverseIfProposals(context, coveringNode, null)
					|| getIfReturnIntoIfElseAtEndOfVoidMethodProposals(context, coveringNode, null)
					|| getInverseIfContinueIntoIfThenInLoopsProposals(context, coveringNode, null)
					|| getInverseIfIntoContinueInLoopsProposals(context, coveringNode, null)
					|| getInverseConditionProposals(context, coveringNode, null)
					|| getRemoveExtraParenthesisProposals(context, coveringNode, null)
					|| getAddParanoidalParenthesisProposals(context, coveringNode, null)
					|| getJoinAndIfStatementsProposals(context, coveringNode, null)
					|| getSplitAndConditionProposals(context, coveringNode, null)
					|| getJoinOrIfStatementsProposals(context, coveringNode, null)
					|| getSplitOrConditionProposals(context, coveringNode, null);
	//				|| getVariableDebugOutputProposals(context, coveringNode, null);
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.IAssistProcessor#getAssists(org.eclipse.jdt.internal.ui.text.correction.IAssistContext, org.eclipse.jdt.internal.ui.text.correction.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		ASTNode coveringNode = context.getCoveringNode();
		if (coveringNode != null) {
			ArrayList resultingCollections = new ArrayList();
			if (noErrorsAtLocation(locations)) {
				getInverseIfProposals(context, coveringNode, resultingCollections);
				getIfReturnIntoIfElseAtEndOfVoidMethodProposals(context, coveringNode, resultingCollections);
				getInverseIfContinueIntoIfThenInLoopsProposals(context, coveringNode, resultingCollections);
				getInverseIfIntoContinueInLoopsProposals(context, coveringNode, resultingCollections);
				getInverseConditionProposals(context, coveringNode, resultingCollections);
				getRemoveExtraParenthesisProposals(context, coveringNode, resultingCollections);
				getAddParanoidalParenthesisProposals(context, coveringNode, resultingCollections);
				getJoinAndIfStatementsProposals(context, coveringNode, resultingCollections);
				getSplitAndConditionProposals(context, coveringNode, resultingCollections);
				getJoinOrIfStatementsProposals(context, coveringNode, resultingCollections);
				getSplitOrConditionProposals(context, coveringNode, resultingCollections);
			//	getVariableDebugOutputProposals(context, coveringNode, resultingCollections);
			}
			return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
		}
		return null;
	}
	private boolean noErrorsAtLocation(IProblemLocation[] locations) {
		if (locations != null) {
			for (int i = 0; i < locations.length; i++) {
				if (locations[i].isError()) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean getIfReturnIntoIfElseAtEndOfVoidMethodProposals(IInvocationContext context, ASTNode covering,
			Collection resultingCollections) {
		Statement coveringStatement = ASTResolving.findParentStatement(covering);
		if (!(coveringStatement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) coveringStatement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// 'then' block should have 'return' as last statement
		Statement thenStatement = ifStatement.getThenStatement();
		if (!(thenStatement instanceof Block)) {
			return false;
		}
		Block thenBlock = (Block) thenStatement;
		List thenStatements = thenBlock.statements();
		if (thenStatements.isEmpty() || !(thenStatements.get(thenStatements.size() - 1) instanceof ReturnStatement)) {
			return false;
		}
		// method should return 'void'
		MethodDeclaration coveringMetod = ASTResolving.findParentMethodDeclaration(covering);
		if (coveringMetod == null) {
			return false;
		}
		Type returnType = coveringMetod.getReturnType2();
		if (!(returnType instanceof PrimitiveType) || ((PrimitiveType) returnType).getPrimitiveTypeCode() != PrimitiveType.VOID)
			return false;
		//
		List statements = coveringMetod.getBody().statements();
		int ifIndex = statements.indexOf(ifStatement);
		if (ifIndex == -1) {
			return false;
		}
		// ok, we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast = coveringStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// remove last 'return' in 'then' block
		ListRewrite listRewriter = rewrite.getListRewrite(thenBlock,
				(ChildListPropertyDescriptor) ifStatement.getLocationInParent());
		listRewriter.remove((ASTNode) thenStatements.get(thenStatements.size() - 1), null);
		// prepare original nodes
		Expression conditionPlaceholder = (Expression) rewrite.createMoveTarget(ifStatement.getExpression());
		Statement thenPlaceholder = (Statement) rewrite.createMoveTarget(ifStatement.getThenStatement());
		// prepare 'else' block
		Block elseBlock = ast.newBlock();
		for (int i = ifIndex + 1; i < statements.size(); i++) {
			Statement statement = (Statement) statements.get(i);
			elseBlock.statements().add(rewrite.createMoveTarget(statement));
		}
		// prepare new 'if' statement
		IfStatement newIf = ast.newIfStatement();
		newIf.setExpression(conditionPlaceholder);
		newIf.setThenStatement(thenPlaceholder);
		newIf.setElseStatement(elseBlock);
		rewrite.replace(ifStatement, newIf, null);
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.convertToIfElse.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private boolean getInverseIfProposals(IInvocationContext context, ASTNode covering, Collection resultingCollections) {
		Statement coveringStatement = ASTResolving.findParentStatement(covering);
		if (!(coveringStatement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) coveringStatement;
		if (ifStatement.getElseStatement() == null) {
			return false;
		}
		// ok, we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast = coveringStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// prepare original nodes
		Expression inversedExpression = getInversedBooleanExpression(ast, rewrite, ifStatement.getExpression());
		Statement thenPlaceholder = (Statement) rewrite.createMoveTarget(ifStatement.getThenStatement());
		Statement elsePlaceholder = (Statement) rewrite.createMoveTarget(ifStatement.getElseStatement());
		// create inversed 'if' statement
		IfStatement newIf = ast.newIfStatement();
		newIf.setExpression(inversedExpression);
		newIf.setThenStatement(elsePlaceholder);
		newIf.setElseStatement(thenPlaceholder);
		// replace original 'if' statement with inversed one
		Block sourceBlock = (Block) ifStatement.getParent();
		ListRewrite listRewriter = rewrite.getListRewrite(sourceBlock,
				(ChildListPropertyDescriptor) coveringStatement.getLocationInParent());
		listRewriter.replace(ifStatement, newIf, null);
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.inverseIf.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private boolean getInverseIfContinueIntoIfThenInLoopsProposals(IInvocationContext context, ASTNode covering,
			Collection resultingCollections) {
		Statement coveringStatement = ASTResolving.findParentStatement(covering);
		if (!(coveringStatement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) coveringStatement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// check that 'then' is 'continue'
		if (!(ifStatement.getThenStatement() instanceof ContinueStatement)) {
			return false;
		}
		// check that 'if' statement is statement in block that is body of loop
		Block loopBlock = null;
		if ((ifStatement.getParent() instanceof Block) && (ifStatement.getParent().getParent() instanceof ForStatement)) {
			loopBlock = (Block) ifStatement.getParent();
		} else if ((ifStatement.getParent() instanceof Block)
				&& (ifStatement.getParent().getParent() instanceof WhileStatement)) {
			loopBlock = (Block) ifStatement.getParent();
		}
		//
		AST ast = coveringStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// create inversed 'if' statement
		Expression inversedExpression = getInversedBooleanExpression(ast, rewrite, ifStatement.getExpression());
		IfStatement newIf = ast.newIfStatement();
		newIf.setExpression(inversedExpression);
		// prepare 'then' for new 'if'
		Block thenBlock = ast.newBlock();
		int ifIndex = loopBlock.statements().indexOf(ifStatement);
		for (int i = ifIndex + 1; i < loopBlock.statements().size(); i++) {
			Statement statement = (Statement) loopBlock.statements().get(i);
			thenBlock.statements().add(rewrite.createMoveTarget(statement));
		}
		newIf.setThenStatement(thenBlock);
		// replace 'if' statement in loop
		rewrite.replace(ifStatement, newIf, null);
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.inverseIfContinue.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private boolean getInverseIfIntoContinueInLoopsProposals(IInvocationContext context, ASTNode covering,
			Collection resultingCollections) {
		Statement coveringStatement = ASTResolving.findParentStatement(covering);
		if (!(coveringStatement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) coveringStatement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// prepare outer control structure and block that contains 'if' statement
		ASTNode ifParent = ifStatement.getParent();
		Block ifParentBlock = null;
		ASTNode ifParentStructure = ifParent;
		if (ifParentStructure instanceof Block) {
			ifParentBlock = (Block) ifParent;
			ifParentStructure = ifParentStructure.getParent();
		}
		// check that control structure is loop and 'if' statement if last statement
		if (!(ifParentStructure instanceof ForStatement) && !(ifParentStructure instanceof WhileStatement)) {
			return false;
		}
		if ((ifParentBlock != null)
				&& (ifParentBlock.statements().indexOf(ifStatement) != ifParentBlock.statements().size() - 1)) {
			return false;
		}
		// ok, we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast = coveringStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// create inversed 'if' statement
		Expression inversedExpression = getInversedBooleanExpression(ast, rewrite, ifStatement.getExpression());
		IfStatement newIf = ast.newIfStatement();
		newIf.setExpression(inversedExpression);
		newIf.setThenStatement(ast.newContinueStatement());
		//
		if (ifParentBlock == null) {
			// if there is no block, create it
			ifParentBlock = ast.newBlock();
			ifParentBlock.statements().add(newIf);
			for (Iterator I = getUnwrappedStatements(ifStatement.getThenStatement()).iterator(); I.hasNext();) {
				Statement statement = (Statement) I.next();
				ifParentBlock.statements().add(rewrite.createMoveTarget(statement));
			}
			// replace 'if' statement as body with new block
			if (ifParentStructure instanceof ForStatement) {
				rewrite.set(ifParentStructure, ForStatement.BODY_PROPERTY, ifParentBlock, null);
			} else if (ifParentStructure instanceof WhileStatement) {
				rewrite.set(ifParentStructure, WhileStatement.BODY_PROPERTY, ifParentBlock, null);
			}
		} else {
			// if there was block, replace
			ListRewrite listRewriter = rewrite.getListRewrite(ifParentBlock,
					(ChildListPropertyDescriptor) ifStatement.getLocationInParent());
			listRewriter.replace(ifStatement, newIf, null);
			// add statements from 'then' to the end of block
			for (Iterator I = getUnwrappedStatements(ifStatement.getThenStatement()).iterator(); I.hasNext();) {
				Statement statement = (Statement) I.next();
				listRewriter.insertLast(rewrite.createMoveTarget(statement), null);
			}
		}
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.inverseIfToContinue.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static ArrayList getUnwrappedStatements(Statement body) {
		ArrayList statements = new ArrayList();
		if (body instanceof Block) {
			for (Iterator I = ((Block) body).statements().iterator(); I.hasNext();) {
				Statement statement = (Statement) I.next();
				statements.add(statement);
			}
		} else {
			statements.add(body);
		}
		return statements;
	}
	private boolean getInverseConditionProposals(IInvocationContext context, ASTNode covering,
			Collection resultingCollections) {
		ArrayList coveredNodes = getFullyCoveredNodes(context);
		if (coveredNodes.isEmpty()) {
			return false;
		}
		//
		final AST ast = covering.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// check sub-expressions in fully covered nodes
		final ArrayList changedNodes = new ArrayList();
		for (Iterator I = coveredNodes.iterator(); I.hasNext();) {
			ASTNode covered = (ASTNode) I.next();
			if (!(covered instanceof Expression)) {
				continue;
			}
			Expression coveredExpression = (Expression) covered;
			ITypeBinding typeBinding = coveredExpression.resolveTypeBinding();
			if ((typeBinding == null) || !typeBinding.getName().equals("boolean")) { //$NON-NLS-1$
				continue;
			}
			//
			Expression inversedExpression = getInversedBooleanExpression(ast, rewrite, coveredExpression);
			rewrite.replace(coveredExpression, inversedExpression, null);
			changedNodes.add(coveredExpression);
		}
		//
		if (changedNodes.isEmpty()) {
			return false;
		}
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.inverseConditions.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static Expression getInversedBooleanExpression(AST ast, ASTRewrite rewrite, Expression expression) {
		if (expression instanceof InfixExpression) {
			InfixExpression infixExpression = (InfixExpression) expression;
			InfixExpression.Operator operator = infixExpression.getOperator();
			if (operator == InfixExpression.Operator.LESS) {
				return getInversedInfixBooleanExpression(ast, rewrite, infixExpression,
						InfixExpression.Operator.GREATER_EQUALS);
			}
			if (operator == InfixExpression.Operator.GREATER) {
				return getInversedInfixBooleanExpression(ast, rewrite, infixExpression,
						InfixExpression.Operator.LESS_EQUALS);
			}
			if (operator == InfixExpression.Operator.LESS_EQUALS) {
				return getInversedInfixBooleanExpression(ast, rewrite, infixExpression,
						InfixExpression.Operator.GREATER);
			}
			if (operator == InfixExpression.Operator.GREATER_EQUALS) {
				return getInversedInfixBooleanExpression(ast, rewrite, infixExpression, InfixExpression.Operator.LESS);
			}
			if (operator == InfixExpression.Operator.EQUALS) {
				return getInversedInfixBooleanExpression(ast, rewrite, infixExpression,
						InfixExpression.Operator.NOT_EQUALS);
			}
			if (operator == InfixExpression.Operator.NOT_EQUALS) {
				return getInversedInfixBooleanExpression(ast, rewrite, infixExpression, InfixExpression.Operator.EQUALS);
			}
			if (operator == InfixExpression.Operator.CONDITIONAL_AND) {
				Operator newOperator = InfixExpression.Operator.CONDITIONAL_OR;
				return getInversedAndOrExpression(ast, rewrite, infixExpression, newOperator);
			}
			if (operator == InfixExpression.Operator.CONDITIONAL_OR) {
				Operator newOperator = InfixExpression.Operator.CONDITIONAL_AND;
				return getInversedAndOrExpression(ast, rewrite, infixExpression, newOperator);
			}
		}
		if (expression instanceof PrefixExpression) {
			PrefixExpression prefixExpression = (PrefixExpression) expression;
			if (prefixExpression.getOperator() == PrefixExpression.Operator.NOT) {
				return (Expression) rewrite.createCopyTarget(prefixExpression.getOperand());
			}
		}
		if (expression instanceof InstanceofExpression) {
			PrefixExpression prefixExpression = ast.newPrefixExpression();
			prefixExpression.setOperator(PrefixExpression.Operator.NOT);
			ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
			parenthesizedExpression.setExpression((Expression) rewrite.createCopyTarget(expression));
			prefixExpression.setOperand(parenthesizedExpression);
			return prefixExpression;
		}
		if (expression instanceof ParenthesizedExpression) {
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) expression;
			Expression innerExpression = parenthesizedExpression.getExpression();
			while (innerExpression instanceof ParenthesizedExpression) {
				innerExpression = ((ParenthesizedExpression) innerExpression).getExpression();
			}
			if (innerExpression instanceof InstanceofExpression) {
				return getInversedBooleanExpression(ast, rewrite, innerExpression);
			}
			parenthesizedExpression = ast.newParenthesizedExpression();
			parenthesizedExpression.setExpression(getInversedBooleanExpression(ast, rewrite, innerExpression));
			return parenthesizedExpression;
		}
		if (expression.resolveTypeBinding() == ast.resolveWellKnownType("boolean")) { //$NON-NLS-1$
			PrefixExpression prefixExpression = ast.newPrefixExpression();
			prefixExpression.setOperator(PrefixExpression.Operator.NOT);
			prefixExpression.setOperand((Expression) rewrite.createMoveTarget(expression));
			return prefixExpression;
		}
		return (Expression) rewrite.createCopyTarget(expression);
	}
	private static Expression getInversedInfixBooleanExpression(AST ast, ASTRewrite rewrite,
			InfixExpression expression, InfixExpression.Operator newOperator) {
		InfixExpression newExpression = ast.newInfixExpression();
		newExpression.setOperator(newOperator);
		newExpression.setLeftOperand(getInversedBooleanExpression(ast, rewrite, expression.getLeftOperand()));
		newExpression.setRightOperand(getInversedBooleanExpression(ast, rewrite, expression.getRightOperand()));
		return newExpression;
	}
	private static Expression getInversedAndOrExpression(AST ast, ASTRewrite rewrite, InfixExpression infixExpression,
			Operator newOperator) {
		int newOperatorPrecedence = getInfixOperatorPrecedence(newOperator);
		//
		Expression leftOperand = getInversedBooleanExpression(ast, rewrite, infixExpression.getLeftOperand());
		int leftPrecedence = getExpressionPrecedence(leftOperand);
		if (newOperatorPrecedence < leftPrecedence) {
			leftOperand = getParenthesizedExpression(ast, leftOperand);
		}
		//
		Expression rightOperand = getInversedBooleanExpression(ast, rewrite, infixExpression.getRightOperand());
		int rightPrecedence = getExpressionPrecedence(rightOperand);
		if (newOperatorPrecedence < rightPrecedence) {
			rightOperand = getParenthesizedExpression(ast, rightOperand);
		}
		//
		InfixExpression newExpression = ast.newInfixExpression();
		newExpression.setOperator(newOperator);
		newExpression.setLeftOperand(leftOperand);
		newExpression.setRightOperand(rightOperand);
		return newExpression;
	}
	private boolean getRemoveExtraParenthesisProposals(IInvocationContext context, ASTNode covering,
			Collection resultingCollections) {
		ArrayList coveredNodes = getFullyCoveredNodes(context);
		if (coveredNodes.isEmpty())
			return false;
		//
		final AST ast = covering.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// check sub-expressions in fully covered nodes
		final ArrayList changedNodes = new ArrayList();
		for (Iterator I = coveredNodes.iterator(); I.hasNext();) {
			ASTNode covered = (ASTNode) I.next();
			covered.accept(new ASTVisitor() {
				public void postVisit(ASTNode node) {
					if (!(node instanceof ParenthesizedExpression)) {
						return;
					}
					ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) node;
					Expression expression = parenthesizedExpression.getExpression();
					while (expression instanceof ParenthesizedExpression) {
						expression = ((ParenthesizedExpression) expression).getExpression();
					}
					int expressionPrecedence = getExpressionPrecedence(expression);
					if (!(parenthesizedExpression.getParent() instanceof Expression)) {
						return;
					}
					int parentPrecedence = getExpressionPrecedence((Expression) parenthesizedExpression.getParent());
					if ((expressionPrecedence > parentPrecedence)
							&& !(parenthesizedExpression.getParent() instanceof ParenthesizedExpression)) {
						return;
					}
					// remove parenthesis around expression
					rewrite.replace(parenthesizedExpression, expression, null);
					changedNodes.add(node);
				}
			});
		}
		//
		if (changedNodes.isEmpty())
			return false;
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.removeParenthesis.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static int getExpressionPrecedence(Expression expression) {
		if (expression instanceof PostfixExpression) {
			return 0;
		}
		if (expression instanceof PrefixExpression) {
			return 1;
		}
		if ((expression instanceof ClassInstanceCreation) || (expression instanceof CastExpression)) {
			return 2;
		}
		if (expression instanceof InfixExpression) {
			InfixExpression infixExpression = (InfixExpression) expression;
			InfixExpression.Operator operator = infixExpression.getOperator();
			return getInfixOperatorPrecedence(operator);
		}
		if (expression instanceof InstanceofExpression) {
			return 6;
		}
		if (expression instanceof ConditionalExpression) {
			return 13;
		}
		if (expression instanceof Assignment) {
			return 14;
		}
		return -1;
	}
	private static int getInfixOperatorPrecedence(InfixExpression.Operator operator) {
		if ((operator == InfixExpression.Operator.TIMES) || (operator == InfixExpression.Operator.DIVIDE)
				|| (operator == InfixExpression.Operator.REMAINDER)) {
			return 3;
		}
		if ((operator == InfixExpression.Operator.PLUS) || (operator == InfixExpression.Operator.MINUS)) {
			return 4;
		}
		if ((operator == InfixExpression.Operator.LEFT_SHIFT)
				|| (operator == InfixExpression.Operator.RIGHT_SHIFT_SIGNED)
				|| (operator == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED)) {
			return 5;
		}
		if ((operator == InfixExpression.Operator.LESS) || (operator == InfixExpression.Operator.GREATER)
				|| (operator == InfixExpression.Operator.LESS_EQUALS)
				|| (operator == InfixExpression.Operator.GREATER_EQUALS)) {
			return 6;
		}
		if ((operator == InfixExpression.Operator.EQUALS) || (operator == InfixExpression.Operator.NOT_EQUALS)) {
			return 7;
		}
		if (operator == InfixExpression.Operator.AND) {
			return 8;
		}
		if (operator == InfixExpression.Operator.XOR) {
			return 9;
		}
		if (operator == InfixExpression.Operator.OR) {
			return 10;
		}
		if (operator == InfixExpression.Operator.CONDITIONAL_AND) {
			return 11;
		}
		if (operator == InfixExpression.Operator.CONDITIONAL_OR) {
			return 12;
		}
		return -1;
	}
	private boolean getAddParanoidalParenthesisProposals(IInvocationContext context, ASTNode covering,
			Collection resultingCollections) {
		ArrayList coveredNodes = getFullyCoveredNodes(context);
		if (coveredNodes.isEmpty())
			return false;
		//
		final AST ast = covering.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// check sub-expressions in fully covered nodes
		final ArrayList changedNodes = new ArrayList();
		for (Iterator I = coveredNodes.iterator(); I.hasNext();) {
			ASTNode covered = (ASTNode) I.next();
			covered.accept(new ASTVisitor() {
				public void postVisit(ASTNode node) {
					// we want to add parenthesis around arithmetic operators and instanceof
					boolean needParenthesis = false;
					if (node instanceof InfixExpression) {
						InfixExpression expression = (InfixExpression) node;
						InfixExpression.Operator operator = expression.getOperator();
						needParenthesis = (operator == InfixExpression.Operator.LESS)
								|| (operator == InfixExpression.Operator.GREATER)
								|| (operator == InfixExpression.Operator.LESS_EQUALS)
								|| (operator == InfixExpression.Operator.GREATER_EQUALS)
								|| (operator == InfixExpression.Operator.EQUALS)
								|| (operator == InfixExpression.Operator.NOT_EQUALS);
					}
					if (node instanceof InstanceofExpression) {
						needParenthesis = true;
					}
					if (!needParenthesis) {
						return;
					}
					// check that parent is && or ||
					if (!(node.getParent() instanceof InfixExpression))
						return;
					InfixExpression parentExpression = (InfixExpression) node.getParent();
					InfixExpression.Operator parentOperator = parentExpression.getOperator();
					if ((parentOperator != InfixExpression.Operator.CONDITIONAL_AND)
							&& (parentOperator != InfixExpression.Operator.CONDITIONAL_OR)) {
						return;
					}
					// add parenthesis around expression
					ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
					Expression expressionPlaceholder = (Expression) rewrite.createCopyTarget(node);
					parenthesizedExpression.setExpression(expressionPlaceholder);
					rewrite.replace(node, parenthesizedExpression, null);
					changedNodes.add(node);
				}
			});
		}
		//
		if (changedNodes.isEmpty())
			return false;
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.addParethesis.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static ArrayList getFullyCoveredNodes(IInvocationContext context) {
		final ArrayList coveredNodes = new ArrayList();
		final int selectionBegin = context.getSelectionOffset();
		final int selectionEnd = selectionBegin + context.getSelectionLength();
		context.getASTRoot().accept(new ASTVisitor() {
			public void postVisit(ASTNode node) {
				if (isCovered(node)) {
					if (!isCovered(node.getParent())) {
						coveredNodes.add(node);
					}
				}
			}
			private boolean isCovered(ASTNode node) {
				int begin = node.getStartPosition();
				int end = begin + node.getLength();
				return (begin >= selectionBegin) && (end <= selectionEnd);
			}
		});
		return coveredNodes;
	}
	private boolean getJoinAndIfStatementsProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		Operator andOperator = InfixExpression.Operator.CONDITIONAL_AND;
		boolean result = false;
		//
		Statement statement = ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) statement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// case when current IfStatement is sole child of another IfStatement
		{
			IfStatement outerIf = null;
			if (ifStatement.getParent() instanceof IfStatement) {
				outerIf = (IfStatement) ifStatement.getParent();
			} else if (ifStatement.getParent() instanceof Block) {
				Block block = (Block) ifStatement.getParent();
				if ((block.getParent() instanceof IfStatement) && (block.statements().size() == 1)) {
					outerIf = (IfStatement) block.getParent();
				}
			}
			if ((outerIf != null) && (outerIf.getElseStatement() == null)) {
				if (resultingCollections == null) {
					return true;
				}
				//
				AST ast = statement.getAST();
				ASTRewrite rewrite = ASTRewrite.create(ast);
				// prepare condition parts, add parenthesis if needed
				Expression outerCondition = getParenthesizedForAndIfNeeded(ast, rewrite, outerIf.getExpression());
				Expression innerCondition = getParenthesizedForAndIfNeeded(ast, rewrite, ifStatement.getExpression());
				// create compound condition
				InfixExpression condition = ast.newInfixExpression();
				condition.setOperator(andOperator);
				condition.setLeftOperand(outerCondition);
				condition.setRightOperand(innerCondition);
				// create new IfStatement
				IfStatement newIf = ast.newIfStatement();
				newIf.setExpression(condition);
				Statement bodyPlaceholder = (Statement) rewrite.createCopyTarget(ifStatement.getThenStatement());
				newIf.setThenStatement(bodyPlaceholder);
				rewrite.replace(outerIf, newIf, null);
				// add correction proposal
				String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.joinWithOuter.description"); //$NON-NLS-1$
				Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
				ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label,
						context.getCompilationUnit(), rewrite, 1, image);
				resultingCollections.add(proposal);
			}
		}
		// case when current IfStatement has another IfStatement as sole child
		{
			IfStatement innerIf = null;
			if (ifStatement.getThenStatement() instanceof IfStatement) {
				innerIf = (IfStatement) ifStatement.getThenStatement();
			} else if (ifStatement.getThenStatement() instanceof Block) {
				Block block = (Block) ifStatement.getThenStatement();
				if ((block.statements().size() == 1) && (block.statements().get(0) instanceof IfStatement)) {
					innerIf = (IfStatement) block.statements().get(0);
				}
			}
			if ((innerIf != null) && (innerIf.getElseStatement() == null)) {
				if (resultingCollections == null) {
					return true;
				}
				//
				AST ast = statement.getAST();
				ASTRewrite rewrite = ASTRewrite.create(ast);
				// prepare condition parts, add parenthesis if needed
				Expression outerCondition = getParenthesizedForAndIfNeeded(ast, rewrite, ifStatement.getExpression());
				Expression innerCondition = getParenthesizedForAndIfNeeded(ast, rewrite, innerIf.getExpression());
				// create compound condition
				InfixExpression condition = ast.newInfixExpression();
				condition.setOperator(andOperator);
				condition.setLeftOperand(outerCondition);
				condition.setRightOperand(innerCondition);
				// create new IfStatement
				IfStatement newIf = ast.newIfStatement();
				newIf.setExpression(condition);
				Statement bodyPlaceholder = (Statement) rewrite.createCopyTarget(innerIf.getThenStatement());
				newIf.setThenStatement(bodyPlaceholder);
				rewrite.replace(ifStatement, newIf, null);
				// add correction proposal
				String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.joinWithInner.description"); //$NON-NLS-1$
				Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
				ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label,
						context.getCompilationUnit(), rewrite, 1, image);
				resultingCollections.add(proposal);
			}
		}
		return result;
	}
	private static Expression getParenthesizedForAndIfNeeded(AST ast, ASTRewrite rewrite, Expression expression) {
		boolean addParentheses = false;
		int nodeType = expression.getNodeType();
		if (nodeType == ASTNode.INFIX_EXPRESSION) {
			InfixExpression infixExpression = (InfixExpression) expression;
			addParentheses = infixExpression.getOperator() == InfixExpression.Operator.CONDITIONAL_OR;
		} else {
			addParentheses = nodeType == ASTNode.CONDITIONAL_EXPRESSION || nodeType == ASTNode.ASSIGNMENT
					|| nodeType == ASTNode.INSTANCEOF_EXPRESSION;
		}
		expression = (Expression) rewrite.createCopyTarget(expression);
		if (addParentheses) {
			return getParenthesizedExpression(ast, expression);
		}
		return expression;
	}
	private static Expression getParenthesizedExpression(AST ast, Expression expression) {
		ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
		parenthesizedExpression.setExpression(expression);
		return parenthesizedExpression;
	}
	private boolean getSplitAndConditionProposals(IInvocationContext context, ASTNode node,
			Collection resultingCollections) {
		Operator andOperator = InfixExpression.Operator.CONDITIONAL_AND;
		// check that user invokes quick assist on infix expression
		if (!(node instanceof InfixExpression)) {
			return false;
		}
		InfixExpression infixExpression = (InfixExpression) node;
		if (infixExpression.getOperator() != andOperator) {
			return false;
		}
		// check that infix expression belongs to IfStatement
		Statement statement = ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) statement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// check that infix expression is part of first level && condition of IfStatement
		InfixExpression topInfixExpression = infixExpression;
		while ((topInfixExpression.getParent() instanceof InfixExpression)
				&& ((InfixExpression) topInfixExpression.getParent()).getOperator() == andOperator) {
			topInfixExpression = (InfixExpression) topInfixExpression.getParent();
		}
		if (ifStatement.getExpression() != topInfixExpression) {
			return false;
		}
		//
		if (resultingCollections == null) {
			return true;
		}
		AST ast = ifStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// prepare left and right conditions
		Expression leftCondition = null;
		Expression rightCondition = null;
		Expression currentExpression = infixExpression;
		while (true) {
			if (leftCondition == null) {
				Expression leftOperand = ((InfixExpression) currentExpression).getLeftOperand();
				if (leftOperand instanceof ParenthesizedExpression)
					leftOperand = ((ParenthesizedExpression) leftOperand).getExpression();
				Expression leftPlaceholder = (Expression) rewrite.createCopyTarget(leftOperand);
				leftCondition = leftPlaceholder;
			}
			Expression rightOperand = ((InfixExpression) currentExpression).getRightOperand();
			if (rightCondition == null) {
				if (rightOperand instanceof ParenthesizedExpression)
					rightOperand = ((ParenthesizedExpression) rightOperand).getExpression();
				Expression rightPlaceholder = (Expression) rewrite.createCopyTarget(rightOperand);
				rightCondition = rightPlaceholder;
			} else {
				Expression rightPlaceholder = (Expression) rewrite.createCopyTarget(rightOperand);
				InfixExpression infix = ast.newInfixExpression();
				infix.setOperator(andOperator);
				infix.setLeftOperand(rightCondition);
				infix.setRightOperand(rightPlaceholder);
				rightCondition = infix;
			}
			if (currentExpression.getParent() == ifStatement)
				break;
			currentExpression = (Expression) currentExpression.getParent();
		}
		// replace condition in inner IfStatement
		rewrite.set(ifStatement, IfStatement.EXPRESSION_PROPERTY, rightCondition, null);
		// prepare outter IfStatement
		IfStatement outerIfStatement = ast.newIfStatement();
		outerIfStatement.setExpression(leftCondition);
		Block outerBlock = ast.newBlock();
		outerIfStatement.setThenStatement(outerBlock);
		ASTNode ifPlaceholder = rewrite.createMoveTarget(ifStatement);
		outerBlock.statements().add(ifPlaceholder);
		// replace ifStatement
		rewrite.replace(ifStatement, outerIfStatement, null);
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.splitAndCondition.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private boolean getJoinOrIfStatementsProposals(IInvocationContext context, ASTNode covering,
			Collection resultingCollections) {
		Operator orOperator = InfixExpression.Operator.CONDITIONAL_OR;
		ArrayList coveredNodes = getFullyCoveredNodes(context);
		if (coveredNodes.size() < 2)
			return false;
		// check that all covered nodes are IfStatement's with same 'then' statement and without 'else'
		String commonThenSource = null;
		for (Iterator I = coveredNodes.iterator(); I.hasNext();) {
			ASTNode node = (ASTNode) I.next();
			if (!(node instanceof IfStatement))
				return false;
			//
			IfStatement ifStatement = (IfStatement) node;
			if (ifStatement.getElseStatement() != null)
				return false;
			//
			Statement thenStatement = ifStatement.getThenStatement();
			try {
				String thenSource = context.getCompilationUnit().getBuffer().getText(thenStatement.getStartPosition(),
						thenStatement.getLength());
				if (commonThenSource == null) {
					commonThenSource = thenSource;
				} else {
					if (!commonThenSource.equals(thenSource))
						return false;
				}
			} catch (Throwable e) {
				return false;
			}
		}
		//
		final AST ast = covering.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// prepare OR'ed condition
		InfixExpression condition = null;
		boolean hasRightOperand = false;
		Statement thenStatement = null;
		for (Iterator I = coveredNodes.iterator(); I.hasNext();) {
			IfStatement ifStatement = (IfStatement) I.next();
			if (thenStatement == null)
				thenStatement = (Statement) rewrite.createCopyTarget(ifStatement.getThenStatement());
			Expression ifCondition = getParenthesizedForOrIfNeeded(ast, rewrite, ifStatement.getExpression());
			if (condition == null) {
				condition = ast.newInfixExpression();
				condition.setOperator(orOperator);
				condition.setLeftOperand(ifCondition);
			} else if (!hasRightOperand) {
				condition.setRightOperand(ifCondition);
				hasRightOperand = true;
			} else {
				InfixExpression newCondition = ast.newInfixExpression();
				newCondition.setOperator(orOperator);
				newCondition.setLeftOperand(condition);
				newCondition.setRightOperand(ifCondition);
				condition = newCondition;
			}
		}
		// prepare new IfStatement with OR'ed condition
		IfStatement newIf = ast.newIfStatement();
		newIf.setExpression(condition);
		newIf.setThenStatement(thenStatement);
		//
		ListRewrite listRewriter = null;
		for (Iterator I = coveredNodes.iterator(); I.hasNext();) {
			IfStatement ifStatement = (IfStatement) I.next();
			if (listRewriter == null) {
				Block sourceBlock = (Block) ifStatement.getParent();
				//int insertIndex = sourceBlock.statements().indexOf(ifStatement);
				listRewriter = rewrite.getListRewrite(sourceBlock,
						(ChildListPropertyDescriptor) ifStatement.getLocationInParent());
			}
			if (newIf != null) {
				listRewriter.replace(ifStatement, newIf, null);
				newIf = null;
			} else {
				listRewriter.remove(ifStatement, null);
			}
		}
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.joinWithOr.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static Expression getParenthesizedForOrIfNeeded(AST ast, ASTRewrite rewrite, Expression expression) {
		boolean addParentheses = false;
		int nodeType = expression.getNodeType();
		addParentheses = nodeType == ASTNode.CONDITIONAL_EXPRESSION || nodeType == ASTNode.ASSIGNMENT
				|| nodeType == ASTNode.INSTANCEOF_EXPRESSION;
		expression = (Expression) rewrite.createCopyTarget(expression);
		if (addParentheses) {
			return getParenthesizedExpression(ast, expression);
		}
		return expression;
	}
	private boolean getSplitOrConditionProposals(IInvocationContext context, ASTNode node,
			Collection resultingCollections) {
		Operator orOperator = InfixExpression.Operator.CONDITIONAL_OR;
		// check that user invokes quick assist on infix expression
		if (!(node instanceof InfixExpression)) {
			return false;
		}
		InfixExpression infixExpression = (InfixExpression) node;
		if (infixExpression.getOperator() != orOperator) {
			return false;
		}
		// check that infix expression belongs to IfStatement
		Statement statement = ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement = (IfStatement) statement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		// check that infix expression is part of first level || condition of IfStatement
		InfixExpression topInfixExpression = infixExpression;
		while ((topInfixExpression.getParent() instanceof InfixExpression)
				&& ((InfixExpression) topInfixExpression.getParent()).getOperator() == orOperator) {
			topInfixExpression = (InfixExpression) topInfixExpression.getParent();
		}
		if (ifStatement.getExpression() != topInfixExpression) {
			return false;
		}
		//
		if (resultingCollections == null) {
			return true;
		}
		AST ast = ifStatement.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// prepare left and right conditions
		Expression leftCondition = null;
		Expression rightCondition = null;
		Expression currentExpression = infixExpression;
		while (true) {
			if (leftCondition == null) {
				Expression leftOperand = ((InfixExpression) currentExpression).getLeftOperand();
				if (leftOperand instanceof ParenthesizedExpression)
					leftOperand = ((ParenthesizedExpression) leftOperand).getExpression();
				Expression leftPlaceholder = (Expression) rewrite.createCopyTarget(leftOperand);
				leftCondition = leftPlaceholder;
			}
			Expression rightOperand = ((InfixExpression) currentExpression).getRightOperand();
			if (rightCondition == null) {
				if (rightOperand instanceof ParenthesizedExpression)
					rightOperand = ((ParenthesizedExpression) rightOperand).getExpression();
				Expression rightPlaceholder = (Expression) rewrite.createCopyTarget(rightOperand);
				rightCondition = rightPlaceholder;
			} else {
				Expression rightPlaceholder = (Expression) rewrite.createCopyTarget(rightOperand);
				InfixExpression infix = ast.newInfixExpression();
				infix.setOperator(orOperator);
				infix.setLeftOperand(rightCondition);
				infix.setRightOperand(rightPlaceholder);
				rightCondition = infix;
			}
			if (currentExpression.getParent() == ifStatement)
				break;
			currentExpression = (Expression) currentExpression.getParent();
		}
		// prepare first statement
		IfStatement firstIf = ast.newIfStatement();
		firstIf.setExpression(leftCondition);
		firstIf.setThenStatement((Statement) rewrite.createCopyTarget(ifStatement.getThenStatement()));
		// prepare second statement
		IfStatement secondIf = ast.newIfStatement();
		secondIf.setExpression(rightCondition);
		secondIf.setThenStatement((Statement) rewrite.createCopyTarget(ifStatement.getThenStatement()));
		// add first and second IfStatement's
		Block sourceBlock = (Block) ifStatement.getParent();
		int insertIndex = sourceBlock.statements().indexOf(ifStatement);
		ListRewrite listRewriter = rewrite.getListRewrite(sourceBlock,
				(ChildListPropertyDescriptor) statement.getLocationInParent());
		listRewriter.replace(ifStatement, firstIf, null);
		listRewriter.insertAt(secondIf, insertIndex + 1, null);
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.splitOrCondition.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private boolean getVariableDebugOutputProposals(IInvocationContext context, ASTNode node,
			Collection resultingCollections) {
		if (!(node instanceof Expression)) {
			return false;
		}
		// prepare selected expression
		Expression expression = (Expression) node;
		if (expression instanceof Name) {
			if (context.getSelectionLength() == 0) {
				expression = (Name) node;
				while (expression.getParent() instanceof QualifiedName)
					expression = (QualifiedName) expression.getParent();
				//
				if ((expression.getParent() instanceof MethodInvocation)
						&& (((MethodInvocation) expression.getParent()).getName() == expression)) {
					MethodInvocation mi = (MethodInvocation) expression.getParent();
					IMethodBinding binding= mi.resolveMethodBinding();
					if (binding != null && binding.getReturnType().getName().equals("void"))  //$NON-NLS-1$
						return false;
					expression = mi;
				}
			} else {
				expression = (Name) node;
			}
		} else {
			ArrayList nodes = getFullyCoveredNodes(context);
			if (nodes.size() != 1)
				return false;
			ASTNode coveredNode = (ASTNode) nodes.get(0);
			if (!(coveredNode instanceof Expression))
				return false;
			expression = (Expression) coveredNode;
		}
		if (expression == null) {
			return false;
		}
		// check expression location and prepare location for debug output statement
		MethodDeclaration methodDeclaration = ASTResolving.findParentMethodDeclaration(expression);
		Block parentBlock = null;
		int insertIndex = -1;
		Statement parentStatement = ASTResolving.findParentStatement(expression);
		if (expression.getParent() instanceof SingleVariableDeclaration) {
			parentBlock = methodDeclaration.getBody();
			if (parentBlock.statements().isEmpty())
				return false;
			parentStatement = (Statement) parentBlock.statements().get(0);
			insertIndex = 0;
		} else if ((parentStatement != null) && (parentStatement.getParent() instanceof Block)) {
			parentBlock = (Block) parentStatement.getParent();
			int statementIndex = parentBlock.statements().indexOf(parentStatement);
			insertIndex = statementIndex;
			if (expression.getParent() instanceof Assignment) {
				Assignment assignment = (Assignment) expression.getParent();
				if (assignment.getLeftHandSide() == expression)
					insertIndex = statementIndex + 1;
			}
			if (expression.getParent() instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) expression.getParent();
				if (vdf.getName() == expression)
					insertIndex = statementIndex + 1;
			}
		} else {
			return false;
		}
		//
		AST ast = expression.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// prepare argument for debug output
		String debugTitle = "[" + methodDeclaration.getName().getIdentifier() + "] value of " + expression + ": "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		InfixExpression outExpression = ast.newInfixExpression();
		outExpression.setOperator(InfixExpression.Operator.PLUS);
		StringLiteral debugTitleLiteral = ast.newStringLiteral();
		debugTitleLiteral.setLiteralValue(debugTitle);
		outExpression.setLeftOperand(debugTitleLiteral);
		Expression parenthesizedExpression;
		if (expression instanceof ParenthesizedExpression) {
			parenthesizedExpression = (ParenthesizedExpression) rewrite.createCopyTarget(expression);
		} else {
			if (getExpressionPrecedence(expression) > getInfixOperatorPrecedence(InfixExpression.Operator.PLUS)) {
				ParenthesizedExpression newExpression = ast.newParenthesizedExpression();
				newExpression.setExpression((Expression) rewrite.createCopyTarget(expression));
				parenthesizedExpression = newExpression;
			} else {
				parenthesizedExpression = (Expression) rewrite.createCopyTarget(expression);
			}
		}
		outExpression.setRightOperand(parenthesizedExpression);
		// prepare debug output statement
		MethodInvocation sysout = ast.newMethodInvocation();
		sysout.setExpression(ast.newName(new String[]{"System", "out"})); //$NON-NLS-1$ //$NON-NLS-2$
		sysout.setName(ast.newSimpleName("println")); //$NON-NLS-1$
		sysout.arguments().add(outExpression);
		Statement newStatement = ast.newExpressionStatement(sysout);
		// add debug output statement
		ListRewrite listRewriter = rewrite.getListRewrite(parentBlock,
				(ChildListPropertyDescriptor) parentStatement.getLocationInParent());
		listRewriter.insertAt(newStatement, insertIndex, null);
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.debugOutput.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
}
