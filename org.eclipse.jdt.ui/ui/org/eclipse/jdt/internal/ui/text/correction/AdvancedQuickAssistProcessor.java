/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Konstantin Scheglov (scheglov_ke@nlmk.ru) - initial API and implementation 
 *          (reports 71244 & 74746: New Quick Assist's [quick assist])
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.NamingConventions;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
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
		ArrayList coveredNodes = getFullyCoveredNodes(context, coveringNode);
		if (coveringNode != null) {
			return getInverseIfProposals(context, coveringNode, null)
					|| getIfReturnIntoIfElseAtEndOfVoidMethodProposals(context, coveringNode, null)
					|| getInverseIfContinueIntoIfThenInLoopsProposals(context, coveringNode, null)
					|| getInverseIfIntoContinueInLoopsProposals(context, coveringNode, null)
					|| getInverseConditionProposals(context, coveringNode, coveredNodes, null)
					|| getRemoveExtraParenthesisProposals(context, coveringNode, coveredNodes, null)
					|| getAddParanoidalParenthesisProposals(context, coveringNode, coveredNodes, null)
					|| getJoinAndIfStatementsProposals(context, coveringNode, null)
					|| getSplitAndConditionProposals(context, coveringNode, null)
					|| getJoinOrIfStatementsProposals(context, coveringNode, coveredNodes, null)
					|| getSplitOrConditionProposals(context, coveringNode, null)
					|| getInverseConditionalExpressionProposals(context, coveringNode, null)
					|| getExchangeInnerAndOuterIfConditionsProposals(context, coveringNode, null)
					|| getExchangeOperandsProposals(context, coveringNode, null)
					|| getCastAndAssignIfStatementProposals(context, coveringNode, null)
					|| getPickOutStringProposals(context, coveringNode, null)
					|| getReplaceIfElseReturnWithReturnConditionalProposals(context, coveringNode, null)
					|| getReplaceIfElseAssignWithReturnConditionalProposals(context, coveringNode, null)
					|| getReplaceReturnConditionalWithIfElseReturnProposals(context, coveringNode, null)
					|| getReplaceAssignConditionalWithIfElseAssignProposals(context, coveringNode, null);
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.IAssistProcessor#getAssists(org.eclipse.jdt.internal.ui.text.correction.IAssistContext, org.eclipse.jdt.internal.ui.text.correction.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		ASTNode coveringNode = context.getCoveringNode();
		ArrayList coveredNodes = getFullyCoveredNodes(context, coveringNode);
		if (coveringNode != null) {
			ArrayList resultingCollections = new ArrayList();
			if (noErrorsAtLocation(locations)) {
				getInverseIfProposals(context, coveringNode, resultingCollections);
				getIfReturnIntoIfElseAtEndOfVoidMethodProposals(context, coveringNode, resultingCollections);
				getInverseIfContinueIntoIfThenInLoopsProposals(context, coveringNode, resultingCollections);
				getInverseIfIntoContinueInLoopsProposals(context, coveringNode, resultingCollections);
				getInverseConditionProposals(context, coveringNode, coveredNodes, resultingCollections);
				getRemoveExtraParenthesisProposals(context, coveringNode, coveredNodes, resultingCollections);
				getAddParanoidalParenthesisProposals(context, coveringNode, coveredNodes, resultingCollections);
				getJoinAndIfStatementsProposals(context, coveringNode, resultingCollections);
				getSplitAndConditionProposals(context, coveringNode, resultingCollections);
				getJoinOrIfStatementsProposals(context, coveringNode, coveredNodes, resultingCollections);
				getSplitOrConditionProposals(context, coveringNode, resultingCollections);
				getInverseConditionalExpressionProposals(context, coveringNode, resultingCollections);
				getExchangeInnerAndOuterIfConditionsProposals(context, coveringNode, resultingCollections);
				getExchangeOperandsProposals(context, coveringNode, resultingCollections);
				getCastAndAssignIfStatementProposals(context, coveringNode, resultingCollections);
				getPickOutStringProposals(context, coveringNode, resultingCollections);
				getReplaceIfElseReturnWithReturnConditionalProposals(context, coveringNode, resultingCollections);
				getReplaceIfElseAssignWithReturnConditionalProposals(context, coveringNode, resultingCollections);
				getReplaceReturnConditionalWithIfElseReturnProposals(context, coveringNode, resultingCollections);
				getReplaceAssignConditionalWithIfElseAssignProposals(context, coveringNode, resultingCollections);
			}
			return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
		}
		return null;
	}
	private static boolean noErrorsAtLocation(IProblemLocation[] locations) {
		if (locations != null) {
			for (int i = 0; i < locations.length; i++) {
				if (locations[i].isError()) {
					return false;
				}
			}
		}
		return true;
	}
	private static boolean getIfReturnIntoIfElseAtEndOfVoidMethodProposals(IInvocationContext context, ASTNode covering,
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
		if (!(returnType instanceof PrimitiveType)
				|| ((PrimitiveType) returnType).getPrimitiveTypeCode() != PrimitiveType.VOID)
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
	private static boolean getInverseIfProposals(IInvocationContext context, ASTNode covering, Collection resultingCollections) {
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
		// set new nodes
		rewrite.set(ifStatement, IfStatement.EXPRESSION_PROPERTY, inversedExpression, null);
		if (ifStatement.getThenStatement() instanceof Block && !(ifStatement.getElseStatement() instanceof Block)) {
			// heuristic for if (..) {...} else if (..) {...} constructs (bug 74580)
			Block elseBlock = ast.newBlock();
			elseBlock.statements().add(elsePlaceholder);
			rewrite.set(ifStatement, IfStatement.THEN_STATEMENT_PROPERTY, elseBlock, null);
		} else {
			rewrite.set(ifStatement, IfStatement.THEN_STATEMENT_PROPERTY, elsePlaceholder, null);
		}
		rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, thenPlaceholder, null);
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.inverseIf.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getInverseIfContinueIntoIfThenInLoopsProposals(IInvocationContext context, ASTNode covering,
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
		} else {
			return false;
		}
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
	private static boolean getInverseIfIntoContinueInLoopsProposals(IInvocationContext context, ASTNode covering,
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
	private static boolean getInverseConditionProposals(IInvocationContext context, ASTNode covering, ArrayList coveredNodes,
			Collection resultingCollections) {
		if (coveredNodes.isEmpty()) {
			return false;
		}
		//
		final AST ast = covering.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// check sub-expressions in fully covered nodes
		boolean hasChanges = false;
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
			hasChanges = true;
		}
		//
		if (!hasChanges) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
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
				return getInversedInfixBooleanExpression(ast,
					rewrite,
					infixExpression,
					InfixExpression.Operator.GREATER_EQUALS);
			}
			if (operator == InfixExpression.Operator.GREATER) {
				return getInversedInfixBooleanExpression(ast,
					rewrite,
					infixExpression,
					InfixExpression.Operator.LESS_EQUALS);
			}
			if (operator == InfixExpression.Operator.LESS_EQUALS) {
				return getInversedInfixBooleanExpression(ast,
					rewrite,
					infixExpression,
					InfixExpression.Operator.GREATER);
			}
			if (operator == InfixExpression.Operator.GREATER_EQUALS) {
				return getInversedInfixBooleanExpression(ast, rewrite, infixExpression, InfixExpression.Operator.LESS);
			}
			if (operator == InfixExpression.Operator.EQUALS) {
				return getInversedInfixBooleanExpression(ast,
					rewrite,
					infixExpression,
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
	private static boolean getRemoveExtraParenthesisProposals(IInvocationContext context, ASTNode covering, ArrayList coveredNodes,
			Collection resultingCollections) {
		ArrayList nodes;
		if ((context.getSelectionLength() == 0) && (covering instanceof ParenthesizedExpression)) {
			nodes = new ArrayList();
			nodes.add(covering);
		} else {
			nodes= coveredNodes;
		}
		if (nodes.isEmpty())
			return false;
		//
		final AST ast= covering.getAST();
		final ASTRewrite rewrite= ASTRewrite.create(ast);
		// check sub-expressions in fully covered nodes
		final ArrayList changedNodes= new ArrayList();
		for (Iterator I= nodes.iterator(); I.hasNext();) {
			ASTNode covered= (ASTNode) I.next();
			covered.accept(new ASTVisitor() {
				public void postVisit(ASTNode node) {
					if (!(node instanceof ParenthesizedExpression)) {
						return;
					}
					ParenthesizedExpression parenthesizedExpression= (ParenthesizedExpression) node;
					Expression expression= parenthesizedExpression.getExpression();
					while (expression instanceof ParenthesizedExpression) {
						expression= ((ParenthesizedExpression) expression).getExpression();
					}
					// check case when this expression is cast expression and parent is method invocation with this expression as expression
					if ((parenthesizedExpression.getExpression() instanceof CastExpression)
						&& (parenthesizedExpression.getParent() instanceof MethodInvocation)) {
						MethodInvocation parentMethodInvocation = (MethodInvocation) parenthesizedExpression.getParent();
						if (parentMethodInvocation.getExpression() == parenthesizedExpression)
							return;
					}
					// if this is part of another expression, check for this and parent precedences
					if (parenthesizedExpression.getParent() instanceof Expression) {
						Expression parentExpression= (Expression) parenthesizedExpression.getParent();
						int expressionPrecedence= getExpressionPrecedence(expression);
						int parentPrecedence= getExpressionPrecedence(parentExpression);
						if ((expressionPrecedence > parentPrecedence)
							&& !(parenthesizedExpression.getParent() instanceof ParenthesizedExpression)) {
							return;
						}
						// check for case when precedences for expression and parent are same
						if ((expressionPrecedence == parentPrecedence) && (parentExpression instanceof InfixExpression)) {
							InfixExpression parentInfix= (InfixExpression) parentExpression;
							Operator parentOperator= parentInfix.getOperator();
							// check for PLUS with String
							if (parentOperator == InfixExpression.Operator.PLUS) {
								if (isStringExpression(parentInfix.getLeftOperand())
									|| isStringExpression(parentInfix.getRightOperand())) {
									return;
								}
								for (Iterator J= parentInfix.extendedOperands().iterator(); J.hasNext();) {
									Expression operand= (Expression) J.next();
									if (isStringExpression(operand)) {
										return;
									}
								}
							}
							// check for /, %, -
							if ((parentOperator == InfixExpression.Operator.DIVIDE)
								|| (parentOperator == InfixExpression.Operator.REMAINDER)
								|| parentOperator == InfixExpression.Operator.MINUS) {
								if (parentInfix.getLeftOperand() != parenthesizedExpression)
									return;
							}
						}
					}
					// remove parenthesis around expression
					rewrite.replace(parenthesizedExpression, rewrite.createMoveTarget(expression), null);
					changedNodes.add(node);
				}
			});
		}
		//
		if (changedNodes.isEmpty())
			return false;
		if (resultingCollections == null) {
			return true;
		}
		// add correction proposal
		String label= CorrectionMessages.getString("AdvancedQuickAssistProcessor.removeParenthesis.description"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean isStringExpression(Expression expression) {
		ITypeBinding binding = expression.resolveTypeBinding();
		return binding.getQualifiedName().equals("java.lang.String"); //$NON-NLS-1$
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
		if (expression instanceof MethodInvocation) {
			return 15;
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
	private static boolean getAddParanoidalParenthesisProposals(IInvocationContext context, ASTNode covering, ArrayList coveredNodes,
			Collection resultingCollections) {
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
					// check that parent is && or ||
					if (!(node.getParent() instanceof InfixExpression))
						return;
					InfixExpression parentExpression = (InfixExpression) node.getParent();
					InfixExpression.Operator parentOperator = parentExpression.getOperator();
					if ((parentOperator != InfixExpression.Operator.CONDITIONAL_AND)
							&& (parentOperator != InfixExpression.Operator.CONDITIONAL_OR)) {
						return;
					}
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
		if (resultingCollections == null) {
			return true;
		}
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.addParethesis.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static ArrayList getFullyCoveredNodes(IInvocationContext context, ASTNode coveringNode) {
		final ArrayList coveredNodes = new ArrayList();
		final int selectionBegin = context.getSelectionOffset();
		final int selectionEnd = selectionBegin + context.getSelectionLength();
		coveringNode.accept(new GenericVisitor() {
			protected boolean visitNode(ASTNode node) {
				int nodeStart= node.getStartPosition();
				int nodeEnd= nodeStart + node.getLength();
				// if node does not intersects with selection, don't visit children
				if (nodeEnd < selectionBegin || selectionEnd < nodeStart) {
					return false;
				}
				// if node is fully covered, we don't need to visit children
				if (isCovered(node)) {
					ASTNode parent = node.getParent();
					if ((parent == null) || !isCovered(parent)) {
						coveredNodes.add(node);
						return false;
					}
				}
				// if node only partly intersects with selection, we try to find fully covered children 
				return true;
			}
			private boolean isCovered(ASTNode node) {
				int begin = node.getStartPosition();
				int end = begin + node.getLength();
				return (begin >= selectionBegin) && (end <= selectionEnd);
			}
		});
		return coveredNodes;
	}
	private static boolean getJoinAndIfStatementsProposals(IInvocationContext context, ASTNode node,
			Collection resultingCollections) {
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
				result = true;
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
				result = true;
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
	private static boolean getSplitAndConditionProposals(IInvocationContext context, ASTNode node,
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
	private static boolean getJoinOrIfStatementsProposals(IInvocationContext context, ASTNode covering, ArrayList coveredNodes,
			Collection resultingCollections) {
		Operator orOperator = InfixExpression.Operator.CONDITIONAL_OR;
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
		if (resultingCollections == null) {
			return true;
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
	private static boolean getSplitOrConditionProposals(IInvocationContext context, ASTNode node,
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
	private static boolean getInverseConditionalExpressionProposals(IInvocationContext context, ASTNode covering,
			Collection resultingCollections) {
		// try to find conditional expression as parent
		while (covering instanceof Expression) {
			if (covering instanceof ConditionalExpression)
				break;
			covering = covering.getParent();
		}
		if (!(covering instanceof ConditionalExpression)) {
			return false;
		}
		ConditionalExpression expression = (ConditionalExpression) covering;
		// ok, we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast = covering.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// prepare new conditional expresion
		ConditionalExpression newExpression = ast.newConditionalExpression();
		newExpression.setExpression(getInversedBooleanExpression(ast, rewrite, expression.getExpression()));
		newExpression.setThenExpression((Expression) rewrite.createCopyTarget(expression.getElseExpression()));
		newExpression.setElseExpression((Expression) rewrite.createCopyTarget(expression.getThenExpression()));
		// replace old expression with new
		rewrite.replace(expression, newExpression, null);
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.inverseConditionalExpression.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(),
				rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getExchangeInnerAndOuterIfConditionsProposals(IInvocationContext context, ASTNode node,
			Collection resultingCollections) {
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
				// prepare conditions
				Expression outerCondition = (Expression) rewrite.createCopyTarget(outerIf.getExpression());
				Expression innerCondition = (Expression) rewrite.createCopyTarget(ifStatement.getExpression());
				// exchange conditions
				rewrite.replace(outerIf.getExpression(), innerCondition, null);
				rewrite.replace(ifStatement.getExpression(), outerCondition, null);
				// add correction proposal
				String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.exchangeInnerAndOuterIfConditions.description"); //$NON-NLS-1$
				Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
				ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label,
						context.getCompilationUnit(), rewrite, 1, image);
				resultingCollections.add(proposal);
				result = true;
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
				// prepare conditions
				Expression innerCondition = (Expression) rewrite.createCopyTarget(innerIf.getExpression());
				Expression outerCondition = (Expression) rewrite.createCopyTarget(ifStatement.getExpression());
				// exchange conditions
				rewrite.replace(innerIf.getExpression(), outerCondition, null);
				rewrite.replace(ifStatement.getExpression(), innerCondition, null);
				// add correction proposal
				String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.exchangeInnerAndOuterIfConditions.description"); //$NON-NLS-1$
				Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
				ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label,
						context.getCompilationUnit(), rewrite, 1, image);
				resultingCollections.add(proposal);
				result = true;
			}
		}
		return result;
	}
	private static boolean getExchangeOperandsProposals(IInvocationContext context, ASTNode node,
			Collection resultingCollections) {
		// check that user invokes quick assist on infix expression
		if (!(node instanceof InfixExpression)) {
			return false;
		}
		InfixExpression infixExpression = (InfixExpression) node;
		Operator operator = infixExpression.getOperator();
		if ((operator != InfixExpression.Operator.CONDITIONAL_AND) && (operator != InfixExpression.Operator.AND)
				&& (operator != InfixExpression.Operator.CONDITIONAL_OR) && (operator != InfixExpression.Operator.OR)
				&& (operator != InfixExpression.Operator.EQUALS) && (operator != InfixExpression.Operator.PLUS)
				&& (operator != InfixExpression.Operator.TIMES) && (operator != InfixExpression.Operator.XOR)) {
			return false;
		}
		// ok, we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		AST ast = infixExpression.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// prepare left and right expressions
		Expression leftExpression = null;
		Expression rightExpression = null;
		InfixExpression currentExpression = infixExpression;
		leftExpression= addRightOperandInInfixExpression(operator, ast, rewrite, leftExpression, infixExpression.getLeftOperand());
		if (infixExpression.getRightOperand().getStartPosition() <= context.getSelectionOffset()) {
			leftExpression= addRightOperandInInfixExpression(operator, ast, rewrite, leftExpression, infixExpression.getRightOperand());
		} else {
			rightExpression= addRightOperandInInfixExpression(operator, ast, rewrite, rightExpression, infixExpression.getRightOperand());
		}
		for (Iterator I= currentExpression.extendedOperands().iterator(); I.hasNext();) {
			Expression extendedOperand= (Expression) I.next();
			if (extendedOperand.getStartPosition() <= context.getSelectionOffset()) {
				leftExpression= addRightOperandInInfixExpression(operator, ast, rewrite, leftExpression, extendedOperand);
			} else {
				rightExpression= addRightOperandInInfixExpression(operator, ast, rewrite, rightExpression, extendedOperand);
			}
		}
		// create new infix expression
		InfixExpression newInfix = ast.newInfixExpression();
		newInfix.setOperator(operator);
		newInfix.setLeftOperand(rightExpression);
		newInfix.setRightOperand(leftExpression);
		rewrite.replace(currentExpression, newInfix, null);
		// add correction proposal
		String label = CorrectionMessages.getString("AdvancedQuickAssistProcessor.exchangeOperands.description"); //$NON-NLS-1$
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static Expression addRightOperandInInfixExpression(Operator operator, AST ast, ASTRewrite rewrite,
			Expression expression, Expression rightOperand) {
		Expression rightPlaceholder = (Expression) rewrite.createCopyTarget(rightOperand);
		if (expression == null) {
			return rightPlaceholder;
		}
		InfixExpression infix = ast.newInfixExpression();
		infix.setOperator(operator);
		infix.setLeftOperand(expression);
		infix.setRightOperand(rightPlaceholder);
		expression = infix;
		return expression;
	}

	private static boolean getCastAndAssignIfStatementProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		if (!(node instanceof InstanceofExpression)) {
			return false;
		}
		InstanceofExpression expression= (InstanceofExpression) node;
		// test that we are the expression of a 'while' or 'if'
		while (node.getParent() instanceof Expression) {
			node= node.getParent();
		}
		StructuralPropertyDescriptor locationInParent= node.getLocationInParent();
		
		Statement body= null;
		if (locationInParent == IfStatement.EXPRESSION_PROPERTY) {
			body= ((IfStatement) node.getParent()).getThenStatement();
		} else if (locationInParent == WhileStatement.EXPRESSION_PROPERTY) {
			body= ((WhileStatement) node.getParent()).getBody();
		}	
		if (body == null) {
			return false;
		}
		
		Type originalType= expression.getRightOperand();
		if (originalType.resolveBinding() == null) {
			return false;
		}
		
		// ok, we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		
		final String KEY_NAME= "name"; //$NON-NLS-1$
		final String KEY_TYPE= "type"; //$NON-NLS-1$
		//
		AST ast= expression.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		ICompilationUnit cu= context.getCompilationUnit();
		// prepare correction proposal
		String label= CorrectionMessages.getString("AdvancedQuickAssistProcessor.castAndAssign"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 1, image);
		// prepare possible variable names
		String[] varNames= suggestLocalVariableNames(cu, originalType.resolveBinding());
		for (int i= 0; i < varNames.length; i++) {
			proposal.addLinkedPositionProposal(KEY_NAME, varNames[i], null);
		}
		CastExpression castExpression= ast.newCastExpression();
		castExpression.setExpression((Expression) rewrite.createCopyTarget(expression.getLeftOperand()));
		castExpression.setType((Type) ASTNode.copySubtree(ast, originalType));
		// prepare new variable declaration
		VariableDeclarationFragment vdf= ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName(varNames[0]));
		vdf.setInitializer(castExpression);
		// prepare new variable declaration statement
		VariableDeclarationStatement vds= ast.newVariableDeclarationStatement(vdf);
		vds.setType((Type) ASTNode.copySubtree(ast, originalType));
		// add new variable declaration statement
		if (body instanceof Block) {
			ListRewrite listRewriter= rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
			listRewriter.insertAt(vds, 0, null);
		} else {
			Block newBlock= ast.newBlock();
			List statements= newBlock.statements();
			statements.add(vds);
			statements.add(rewrite.createMoveTarget(body));
			rewrite.replace(body, newBlock, null);
		}

		// setup linked positions
		proposal.addLinkedPosition(rewrite.track(vdf.getName()), true, KEY_NAME);
		proposal.addLinkedPosition(rewrite.track(vds.getType()), false, KEY_TYPE);
		proposal.addLinkedPosition(rewrite.track(castExpression.getType()), false, KEY_TYPE);
		proposal.setEndPosition(rewrite.track(vds)); // set cursor after expression statement
		// add correction proposal
		resultingCollections.add(proposal);
		return true;
	}
	
	private static String[] suggestLocalVariableNames(ICompilationUnit cu, ITypeBinding binding) {
		ITypeBinding base= binding.isArray() ? binding.getElementType() : binding;
		IPackageBinding packBinding= base.getPackage();
		String packName= packBinding != null ? packBinding.getName() : ""; //$NON-NLS-1$
		String typeName= base.getName();
		return NamingConventions.suggestLocalVariableNames(cu.getJavaProject(), packName, typeName, binding.getDimensions(), new String[0]);
	}

	private static boolean getPickOutStringProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		// we work with String's
		if (!(node instanceof StringLiteral)) {
			return false;
		}
		// user should select part of String
		int selectionPos= context.getSelectionOffset();
		int selectionLen= context.getSelectionLength();
		if (selectionLen == 0) {
			return false;
		}
		if ((selectionPos < node.getStartPosition()) || (selectionPos > node.getStartPosition() + node.getLength())) {
			return false;
		}
		// prepare string parts positions
		StringLiteral stringLiteral= (StringLiteral) node;
		String stringValue= stringLiteral.getLiteralValue();
		int stringPos= selectionPos - stringLiteral.getStartPosition() - 1; // -1 for "
		// check if selection starts on "
		if (stringPos == -1) {
			stringPos= 0;
			selectionLen--;
		}
		// check if selection ends on "
		if (stringPos + selectionLen == stringValue.length() + 1) {
			selectionLen--;
		}
		// check that after all checks part and only part is selected 
		if ((selectionLen == 0) || (selectionLen == stringValue.length())) {
			return false;
		}
		// ok, we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		// prepare string parts
		String leftPart= stringValue.substring(0, stringPos);
		String centerPart= stringValue.substring(stringPos, stringPos + selectionLen);
		String rightPart= stringValue.substring(stringPos + selectionLen);
		//
		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		// prepare StringLiteral's for parts
		StringLiteral leftLiteral= ast.newStringLiteral();
		leftLiteral.setLiteralValue(leftPart);
		StringLiteral centerLiteral= ast.newStringLiteral();
		centerLiteral.setLiteralValue(centerPart);
		StringLiteral rightLiteral= ast.newStringLiteral();
		rightLiteral.setLiteralValue(rightPart);
		// prepare new expression instead of StringLiteral
		InfixExpression expression= ast.newInfixExpression();
		expression.setOperator(InfixExpression.Operator.PLUS);
		if (leftPart.length() == 0) {
			expression.setLeftOperand(centerLiteral);
			expression.setRightOperand(rightLiteral);
		} else if (rightPart.length() == 0) {
			expression.setLeftOperand(leftLiteral);
			expression.setRightOperand(centerLiteral);
		} else {
			expression.setLeftOperand(leftLiteral);
			expression.setRightOperand(centerLiteral);
			expression.extendedOperands().add(rightLiteral);
		}
		// use new expression instead of old StirngLiteral
		rewrite.replace(stringLiteral, expression, null);
		// add correction proposal
		String label= CorrectionMessages.getString("AdvancedQuickAssistProcessor.pickSelectedString"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		proposal.addLinkedPosition(rewrite.track(centerLiteral), true, "CENTER_STRING"); //$NON-NLS-1$
		resultingCollections.add(proposal);
		return true;
	}
	
	private static Statement getSingleStatement(Statement statement) {
		if (statement instanceof Block) {
			List blockStatements= ((Block) statement).statements();
			if (blockStatements.size() != 1) {
				return null;
			}
			return (Statement) blockStatements.get(0);
		}
		return statement;
	}
	
	private static boolean getReplaceIfElseReturnWithReturnConditionalProposals(IInvocationContext context,
			ASTNode node, Collection resultingCollections) {
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement= (IfStatement) statement;
		// check 'then' statement
		Statement thenStatement= getSingleStatement(ifStatement.getThenStatement());
		if (!(thenStatement instanceof ReturnStatement)) {
			return false;
		}
		ReturnStatement thenReturn= (ReturnStatement) thenStatement;
		// check 'else' statement
		Statement elseStatement= getSingleStatement(ifStatement.getElseStatement());
		if (!(elseStatement instanceof ReturnStatement)) {
			return false;
		}
		ReturnStatement elseReturn= (ReturnStatement) elseStatement;
		// check that both return statements have returning expressions
		if ((thenReturn.getExpression() == null) || (elseReturn.getExpression() == null)) {
			return false;
		}
		// ok, we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		// prepare conditional expression
		ConditionalExpression conditionalExpression = ast.newConditionalExpression();
		Expression conditionCopy= (Expression) rewrite.createCopyTarget(ifStatement.getExpression());
		conditionalExpression.setExpression(conditionCopy);
		Expression thenCopy= (Expression) rewrite.createCopyTarget(thenReturn.getExpression());
		conditionalExpression.setThenExpression(thenCopy);
		Expression elseCopy= (Expression) rewrite.createCopyTarget(elseReturn.getExpression());
		conditionalExpression.setElseExpression(elseCopy);
		// replace 'if' statement with conditional expression
		ReturnStatement returnStatement = ast.newReturnStatement();
		returnStatement.setExpression(conditionalExpression);
		rewrite.replace(ifStatement, returnStatement, null);
		// add correction proposal
		String label= CorrectionMessages.getString("AdvancedQuickAssistProcessor.replaceIfWithConditionalReturn"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getReplaceIfElseAssignWithReturnConditionalProposals(IInvocationContext context,
			ASTNode node, Collection resultingCollections) {
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement= (IfStatement) statement;
		// check 'then' statement
		Statement thenStatement= getSingleStatement(ifStatement.getThenStatement());
		if (!(thenStatement instanceof ExpressionStatement)) {
			return false;
		}
		Expression thenExpression= ((ExpressionStatement) thenStatement).getExpression();
		if (!(thenExpression instanceof Assignment)) {
			return false;
		}
		Assignment thenAssignment= (Assignment) thenExpression;
		// check 'else' statement
		Statement elseStatement= getSingleStatement(ifStatement.getElseStatement());
		if (!(elseStatement instanceof ExpressionStatement)) {
			return false;
		}
		Expression elseExpression= ((ExpressionStatement) elseStatement).getExpression();
		if (!(elseExpression instanceof Assignment)) {
			return false;
		}
		Assignment elseAssignment= (Assignment) elseExpression;
		// check that both assignmens are for same left expression
		if (!thenAssignment.getLeftHandSide().toString().equals(elseAssignment.getLeftHandSide().toString())) {
			return false;
		}
		// ok, we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		// prepare conditional expression
		ConditionalExpression conditionalExpression= ast.newConditionalExpression();
		Expression conditionCopy= (Expression) rewrite.createCopyTarget(ifStatement.getExpression());
		conditionalExpression.setExpression(conditionCopy);
		Expression thenCopy= (Expression) rewrite.createCopyTarget(thenAssignment.getRightHandSide());
		conditionalExpression.setThenExpression(thenCopy);
		Expression elseCopy= (Expression) rewrite.createCopyTarget(elseAssignment.getRightHandSide());
		conditionalExpression.setElseExpression(elseCopy);
		// replace 'if' statement with conditional expression
		Assignment assignment= ast.newAssignment();
		assignment.setLeftHandSide((Expression) rewrite.createCopyTarget(thenAssignment.getLeftHandSide()));
		assignment.setRightHandSide(conditionalExpression);
		ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
		rewrite.replace(ifStatement, expressionStatement, null);
		// add correction proposal
		String label= CorrectionMessages.getString("AdvancedQuickAssistProcessor.replaceIfWithConditionalAssign"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static boolean getReplaceReturnConditionalWithIfElseReturnProposals(IInvocationContext context,
			ASTNode covering, Collection resultingCollections) {
		// check that parent statement is 'return'
		Statement statement= ASTResolving.findParentStatement(covering);
		if (!(statement instanceof ReturnStatement)) {
			return false;
		}
		ReturnStatement returnStatement= (ReturnStatement) statement;
		// check that reutrn expression is conditional expression
		if (!(returnStatement.getExpression() instanceof ConditionalExpression)) {
			return false;
		}
		ConditionalExpression conditional= (ConditionalExpression) returnStatement.getExpression();
		// ok, we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast= covering.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		// prepare new 'if' statement
		IfStatement ifStatement = ast.newIfStatement();
		ifStatement.setExpression((Expression) rewrite.createCopyTarget(conditional.getExpression()));
		//
		ReturnStatement thenReturn= createReturnExpression(ast, rewrite, conditional.getThenExpression());
		ifStatement.setThenStatement(thenReturn);
		//
		ReturnStatement elseReturn= createReturnExpression(ast, rewrite, conditional.getThenExpression());
		ifStatement.setElseStatement(elseReturn);
		//
		// replace return conditional expression with if/then/else/return
		rewrite.replace(statement, ifStatement, null);
		// add correction proposal
		String label= CorrectionMessages.getString("AdvancedQuickAssistProcessor.replaceConditionalWithIfReturn"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	private static ReturnStatement createReturnExpression(AST ast, ASTRewrite rewrite, Expression expression) {
		ReturnStatement thenReturn = ast.newReturnStatement();
		thenReturn.setExpression((Expression) rewrite.createCopyTarget(expression));
		return thenReturn;
	}
	private static boolean getReplaceAssignConditionalWithIfElseAssignProposals(IInvocationContext context,
			ASTNode covering, Collection resultingCollections) {
		// check that parent statement is assignment
		Statement statement= ASTResolving.findParentStatement(covering);
		if (!(statement instanceof ExpressionStatement)) {
			return false;
		}
		ExpressionStatement expressionStatement= (ExpressionStatement) statement;
		if (!(expressionStatement.getExpression() instanceof Assignment)) {
			return false;
		}
		Assignment assignment= (Assignment) expressionStatement.getExpression();
		// check that reutrn expression is conditional expression
		if (!(assignment.getRightHandSide() instanceof ConditionalExpression)) {
			return false;
		}
		ConditionalExpression conditional= (ConditionalExpression) assignment.getRightHandSide();
		// ok, we could produce quick assist
		if (resultingCollections == null) {
			return true;
		}
		//
		AST ast= covering.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		// prepare new 'if' statement
		IfStatement ifStatement= ast.newIfStatement();
		ifStatement.setExpression((Expression) rewrite.createCopyTarget(conditional.getExpression()));
		//
		Assignment thenAssignment= ast.newAssignment();
		thenAssignment.setLeftHandSide((Expression) rewrite.createCopyTarget(assignment.getLeftHandSide()));
		thenAssignment.setRightHandSide((Expression) rewrite.createCopyTarget(conditional.getThenExpression()));
		ExpressionStatement thenStatement = ast.newExpressionStatement(thenAssignment);
		ifStatement.setThenStatement(thenStatement);
		//
		Assignment elseAssignment= ast.newAssignment();
		elseAssignment.setLeftHandSide((Expression) rewrite.createCopyTarget(assignment.getLeftHandSide()));
		elseAssignment.setRightHandSide((Expression) rewrite.createCopyTarget(conditional.getElseExpression()));
		ExpressionStatement elseStatement = ast.newExpressionStatement(elseAssignment);
		ifStatement.setElseStatement(elseStatement);
		// replace return conditional expression with if/then/else/return
		rewrite.replace(statement, ifStatement, null);
		// add correction proposal
		String label= CorrectionMessages.getString("AdvancedQuickAssistProcessor.replaceConditionalWithIfAssign"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
}
