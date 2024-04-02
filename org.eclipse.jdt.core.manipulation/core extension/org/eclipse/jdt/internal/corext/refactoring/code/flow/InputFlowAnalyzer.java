/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       o inline call that is used in a field initializer
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38137)
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [extract method] Missing return value, while extracting code out of a loop - https://bugs.eclipse.org/bugs/show_bug.cgi?id=213519
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import java.util.List;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.YieldStatement;

import org.eclipse.jdt.internal.corext.dom.Selection;

public class InputFlowAnalyzer extends FlowAnalyzer {

	private static class LoopReentranceVisitor extends FlowAnalyzer {
		private Selection fSelection;
		private ASTNode fLoopNode;
		public LoopReentranceVisitor(FlowContext context, Selection selection, ASTNode loopNode) {
			super(context);
			fSelection= selection;
			fLoopNode= loopNode;
		}
		@Override
		protected boolean traverseNode(ASTNode node) {
			return true; // end <= fSelection.end || fSelection.enclosedBy(start, end);
		}
		@Override
		protected boolean createReturnFlowInfo(ReturnStatement node) {
			// Make sure that the whole return statement is selected or located before the selection.
			return node.getStartPosition() + node.getLength() <= fSelection.getExclusiveEnd();
		}
		@Override
		protected boolean createReturnFlowInfo(YieldStatement node) {
			// Make sure that the whole return statement is selected or located before the selection.
			return node.getStartPosition() + node.getLength() <= fSelection.getExclusiveEnd();
		}
		public void process(ASTNode node) {
			try {
				fFlowContext.setLoopReentranceMode(true);
				node.accept(this);
			} finally {
				fFlowContext.setLoopReentranceMode(false);
			}
		}
		@Override
		public void endVisit(BreakStatement node) {
			if (node.getStartPosition() + node.getLength() <= fSelection.getExclusiveEnd())
				return;
			super.endVisit(node);
		}
		@Override
		public void endVisit(DoStatement node) {
			if (skipNode(node))
				return;
			DoWhileFlowInfo info= createDoWhile();
			setFlowInfo(node, info);
			info.mergeAction(getFlowInfo(node.getBody()));
			// No need to merge the condition. It was already considered by the InputFlowAnalyzer.
			info.removeLabel(null);
		}
		@Override
		public void endVisit(EnhancedForStatement node) {
			if (skipNode(node))
				return;
			FlowInfo paramInfo= getFlowInfo(node.getParameter());
			FlowInfo expressionInfo= getFlowInfo(node.getExpression());
			FlowInfo actionInfo= getFlowInfo(node.getBody());
			EnhancedForFlowInfo forInfo= createEnhancedFor();
			setFlowInfo(node, forInfo);
			// If the for statement is the outermost loop then we only have to consider
			// the action. The parameter and expression are only evaluated once.
			if (node == fLoopNode) {
				forInfo.mergeAction(actionInfo, fFlowContext);
			} else {
				// Inner for loops are evaluated in the sequence expression, parameter,
				// action.
				forInfo.mergeExpression(expressionInfo, fFlowContext);
				forInfo.mergeParameter(paramInfo, fFlowContext);
				forInfo.mergeAction(actionInfo, fFlowContext);
			}
			forInfo.removeLabel(null);
		}
		@Override
		public void endVisit(ForStatement node) {
			if (skipNode(node))
				return;
			FlowInfo initInfo= createSequential(node.initializers());
			FlowInfo conditionInfo= getFlowInfo(node.getExpression());
			FlowInfo incrementInfo= createSequential(node.updaters());
			FlowInfo actionInfo= getFlowInfo(node.getBody());
			ForFlowInfo forInfo= createFor();
			setFlowInfo(node, forInfo);
			// the for statement is the outermost loop. In this case we only have
			// to consider the increment, condition and action.
			if (node == fLoopNode) {
				forInfo.mergeIncrement(incrementInfo, fFlowContext);
				forInfo.mergeCondition(conditionInfo, fFlowContext);
				forInfo.mergeAction(actionInfo, fFlowContext);
			} else {
				// we have to merge two different cases. One if we reenter the for statement
				// immediatelly (that means we have to consider increments, condition and action)
				// and the other case if we reenter the for in the next loop of
				// the outer loop. Then we have to consider initializations, condtion and action.
				// For a conditional flow info that means:
				// (initializations | increments) & condition & action.
				GenericConditionalFlowInfo initIncr= new GenericConditionalFlowInfo();
				initIncr.merge(initInfo, fFlowContext);
				initIncr.merge(incrementInfo, fFlowContext);
				forInfo.mergeAccessModeSequential(initIncr, fFlowContext);
				forInfo.mergeCondition(conditionInfo, fFlowContext);
				forInfo.mergeAction(actionInfo, fFlowContext);
			}
			forInfo.removeLabel(null);
		}
	}

	private Selection fSelection;
	private boolean fDoLoopReentrance;

	public InputFlowAnalyzer(FlowContext context, Selection selection, boolean doLoopReentrance) {
		super(context);
		fSelection= selection;
		Assert.isNotNull(fSelection);
		fDoLoopReentrance= doLoopReentrance;
	}

	public FlowInfo perform(BodyDeclaration node) {
		Assert.isTrue(!(node instanceof AbstractTypeDeclaration));
		node.accept(this);
		return getFlowInfo(node);
	}

	@Override
	protected boolean traverseNode(ASTNode node) {
		return node.getStartPosition() + node.getLength() > fSelection.getInclusiveEnd();
	}

	@Override
	protected boolean createReturnFlowInfo(ReturnStatement node) {
		// Make sure that the whole return statement is located after the selection. There can be cases like
		// return i + [x + 10] * 10; In this case we must not create a return info node.
		return node.getStartPosition() >= fSelection.getInclusiveEnd();
	}

	@Override
	protected boolean createReturnFlowInfo(YieldStatement node) {
		// Make sure that the whole return statement is located after the selection. There can be cases like
		// return i + [x + 10] * 10; In this case we must not create a return info node.
		return node.getStartPosition() >= fSelection.getInclusiveEnd();
	}

	@Override
	public void endVisit(ConditionalExpression node) {
		if (skipNode(node))
			return;
		Expression thenPart= node.getThenExpression();
		Expression elsePart= node.getElseExpression();
		if ((thenPart != null && fSelection.coveredBy(thenPart)) ||
				(elsePart != null && fSelection.coveredBy(elsePart))) {
			GenericSequentialFlowInfo info= createSequential();
			setFlowInfo(node, info);
			endVisitConditional(info, node.getExpression(), new ASTNode[] {thenPart, elsePart});
		} else {
			super.endVisit(node);
		}
	}

	@Override
	public void endVisit(DoStatement node) {
		super.endVisit(node);
		handleLoopReentrance(node);
	}

	@Override
	public void endVisit(IfStatement node) {
		if (skipNode(node))
			return;
		Statement thenPart= node.getThenStatement();
		Statement elsePart= node.getElseStatement();
		if ((thenPart != null && fSelection.coveredBy(thenPart)) ||
				(elsePart != null && fSelection.coveredBy(elsePart))) {
			GenericSequentialFlowInfo info= createSequential();
			setFlowInfo(node, info);
			endVisitConditional(info, node.getExpression(), new ASTNode[] {thenPart, elsePart});
		} else {
			super.endVisit(node);
		}
	}

	@Override
	public void endVisit(EnhancedForStatement node) {
		super.endVisit(node);
		handleLoopReentrance(node);
	}

	@Override
	public void endVisit(ForStatement node) {
		super.endVisit(node);
		handleLoopReentrance(node);
	}

	@Override
	public void endVisit(SwitchStatement node) {
		if (skipNode(node))
			return;
		SwitchData data= preEndVisit(node, node.statements(), node.getExpression());
		if (data == null) {
			return;
		}
		super.endVisit(node, data);
	}

	@Override
	public void endVisit(SwitchExpression node) {
		if (skipNode(node))
			return;
		SwitchData data= preEndVisit(node, node.statements(), node.getExpression());
		if (data == null) {
			return;
		}
		super.endVisit(node, data);
	}

	public SwitchData preEndVisit(ASTNode node, List<Statement> statements, Expression expression) {
		SwitchData data= createSwitchData(statements);
		IRegion[] ranges= data.getRanges();
		for (int i= 0; i < ranges.length; i++) {
			IRegion range= ranges[i];
			if (fSelection.coveredBy(range)) {
				GenericSequentialFlowInfo info= createSequential();
				setFlowInfo(node, info);
				info.merge(getFlowInfo(expression), fFlowContext);
				info.merge(data.getInfo(i), fFlowContext);
				info.removeLabel(null);
				return null;
			}
		}
		return data;
	}

	@Override
	public void endVisit(WhileStatement node) {
		super.endVisit(node);
		handleLoopReentrance(node);
	}

	private void endVisitConditional(GenericSequentialFlowInfo info, ASTNode condition, ASTNode[] branches) {
		info.merge(getFlowInfo(condition), fFlowContext);
		for (ASTNode branch : branches) {
			if (branch != null && fSelection.coveredBy(branch)) {
				info.merge(getFlowInfo(branch), fFlowContext);
				break;
			}
		}
	}

	private void handleLoopReentrance(ASTNode node) {
		if (fDoLoopReentrance && fSelection.coveredBy(node) && !fSelection.covers(node)) {
			LoopReentranceVisitor loopReentranceVisitor= new LoopReentranceVisitor(fFlowContext, fSelection, node);
			loopReentranceVisitor.process(node);
			GenericSequentialFlowInfo info= createSequential();
			info.merge(getFlowInfo(node), fFlowContext);
			info.merge(loopReentranceVisitor.getFlowInfo(node), fFlowContext);
			setFlowInfo(node, info);
		}
	}
}
