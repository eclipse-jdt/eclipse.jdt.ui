/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code.flow;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.util.Selection;

public class InputFlowAnalyzer extends FlowAnalyzer {
	
	private static class LoopReentranceVisitor extends FlowAnalyzer {
		private Selection fSelection;
		private AstNode fLoopNode;
		public LoopReentranceVisitor(FlowContext context, Selection selection, AstNode loopNode) {
			super(context);
			fSelection= selection;
			fLoopNode= loopNode;
		}
		protected boolean traverseRange(int start, int end) {
			return end <= fSelection.end || fSelection.enclosedBy(start, end);	
		}
		protected AstNode getLoopNode() {
			return fLoopNode;
		}
		public void endVisit(DoStatement node, BlockScope scope) {
			if (skipNode(node))
				return;
			DoWhileFlowInfo info= createDoWhile();
			setFlowInfo(node, info);
			info.mergeAction(getFlowInfo(node.action), fFlowContext);
			// No need to merge the condition. It was already considered by the InputFlowAnalyzer.
			info.removeLabel(null);
			
		}
		public void endVisit(ForStatement node, BlockScope scope) {
			if (skipNode(node))
				return;
			FlowInfo initInfo= createSequential(node.initializations);
			FlowInfo conditionInfo= getFlowInfo(node.condition);
			FlowInfo incrementInfo= createSequential(node.increments);
			FlowInfo actionInfo= getFlowInfo(node.action);
			// the for statement is the outermost loop. In this case we only have
			// to consider the increment, condition and action.
			if (node == fLoopNode) {
				ForFlowInfo forInfo= createFor();
				setFlowInfo(node, forInfo);
				forInfo.mergeIncrement(incrementInfo, fFlowContext);
				forInfo.mergeCondition(conditionInfo, fFlowContext);
				forInfo.mergeAction(actionInfo, fFlowContext);
				forInfo.removeLabel(null);
			} else {
				// we have to merge two different cases. One if we reenter the for statement
				// immediatelly (that means we have to consider increments, condition and action
				// ) and the other case if we reenter the for in the next loop of
				// the outer loop. Then we have to consider initializations, condtion and action.
				// For a conditional flow info that means:
				// (initializations | increments) & condition & action.
				GenericConditionalFlowInfo initIncr= new GenericConditionalFlowInfo();
				initIncr.merge(initInfo, fFlowContext);
				initIncr.merge(incrementInfo, fFlowContext);
				ForFlowInfo forInfo= createFor();
				forInfo.mergeAccessModeSequential(initIncr, fFlowContext);
				forInfo.mergeCondition(conditionInfo, fFlowContext);
				forInfo.mergeAction(actionInfo, fFlowContext);
				forInfo.removeLabel(null);
				setFlowInfo(node, forInfo);
			}
		}
	}
	
	private Selection fSelection;
	private LoopReentranceVisitor fLoopReentranceVisitor;

	public InputFlowAnalyzer(FlowContext context, Selection selection) {
		super(context);
		fSelection= selection;
		Assert.isNotNull(fSelection);
	}

	public FlowInfo analyse(AbstractMethodDeclaration method, ClassScope scope) {
		FlowContext context= getFlowContext();
		method.traverse(this, scope);
		return getFlowInfo(method);
	}
	
	protected boolean traverseRange(int start, int end) {
		return end >= fSelection.end;
	}
	
	public boolean visit(DoStatement node, BlockScope scope) {
		createLoopReentranceVisitor(node);
		return super.visit(node, scope);			
	}
	
	public boolean visit(ForStatement node, BlockScope scope) {
		createLoopReentranceVisitor(node);
		return super.visit(node, scope);			
	}
	
	public boolean visit(WhileStatement node, BlockScope scope) {
		createLoopReentranceVisitor(node);
		return super.visit(node, scope);			
	}
	
	private void createLoopReentranceVisitor(AstNode node) {
		if (fLoopReentranceVisitor == null)
			fLoopReentranceVisitor= new LoopReentranceVisitor(fFlowContext, fSelection, node);
	}
	
	public void endVisit(DoStatement node, BlockScope scope) {
		super.endVisit(node, scope);
		handleLoopReentrance(node, scope);
	}

	public void endVisit(ForStatement node, BlockScope scope) {
		super.endVisit(node, scope);
		handleLoopReentrance(node, scope);
	}
	
	public void endVisit(WhileStatement node, BlockScope scope) {
		super.endVisit(node, scope);
		handleLoopReentrance(node, scope);
	}
	
	private void handleLoopReentrance(AstNode node, BlockScope scope) {
		if (!fSelection.enclosedBy(node) || fLoopReentranceVisitor.getLoopNode() != node)
			return;
			
		node.traverse(fLoopReentranceVisitor, scope);
		GenericSequentialFlowInfo info= createSequential();
		info.merge(getFlowInfo(node), fFlowContext);
		info.merge(fLoopReentranceVisitor.getFlowInfo(node), fFlowContext);
		setFlowInfo(node, info);
	}
}