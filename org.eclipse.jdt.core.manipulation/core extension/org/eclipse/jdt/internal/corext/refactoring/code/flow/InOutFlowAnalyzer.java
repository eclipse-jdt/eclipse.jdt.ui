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
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.YieldStatement;

public class InOutFlowAnalyzer extends FlowAnalyzer {

	private ASTNode[] fSelectedNodes= null;

	public InOutFlowAnalyzer(FlowContext context) {
		super(context);
	}

	public FlowInfo perform(ASTNode[] selectedNodes) {
		FlowContext context= getFlowContext();
		GenericSequentialFlowInfo result= createSequential();
		fSelectedNodes= selectedNodes;
		for (ASTNode node : selectedNodes) {
			node.accept(this);
			result.merge(getFlowInfo(node), context);
		}
		return result;
	}

	@Override
	protected boolean traverseNode(ASTNode node) {
		// we are only traversing the selected nodes.
		return true;
	}

	@Override
	protected boolean createReturnFlowInfo(ReturnStatement node) {
		// we are only traversing selected nodes.
		return true;
	}

	@Override
	protected boolean createReturnFlowInfo(YieldStatement node) {
		ASTNode parent= node.getParent();
		while (parent != null && !(parent instanceof SwitchExpression)) {
			parent= parent.getParent();
		}
		return (parent instanceof SwitchExpression) && fSelectedNodes.length > 0 && parent.getStartPosition() < fSelectedNodes[0].getStartPosition();	// we are only traversing selected nodes.
	}

	@Override
	public void endVisit(Block node) {
		super.endVisit(node);
		clearAccessMode(accessFlowInfo(node), node.statements());
	}

	@Override
	public void endVisit(CatchClause node) {
		super.endVisit(node);
		clearAccessMode(accessFlowInfo(node), node.getException());
	}

	@Override
	public void endVisit(EnhancedForStatement node) {
		super.endVisit(node);
		clearAccessMode(accessFlowInfo(node), node.getParameter());
	}

	@Override
	public void endVisit(ForStatement node) {
		super.endVisit(node);
		clearAccessMode(accessFlowInfo(node), node.initializers());
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		super.endVisit(node);
		FlowInfo info= accessFlowInfo(node);
		for (Iterator<SingleVariableDeclaration> iter= node.parameters().iterator(); iter.hasNext();) {
			clearAccessMode(info, iter.next());
		}
	}

	private void clearAccessMode(FlowInfo info, SingleVariableDeclaration decl) {
		IVariableBinding binding= decl.resolveBinding();
		if (binding != null && !binding.isField())
			info.clearAccessMode(binding, fFlowContext);
	}

	private void clearAccessMode(FlowInfo info, List<? extends ASTNode> nodes) {
		if (nodes== null || nodes.isEmpty() || info == null)
			return;
		for (ASTNode node : nodes) {
			Iterator<VariableDeclarationFragment> fragments= null;
			if (node instanceof VariableDeclarationStatement) {
				fragments= ((VariableDeclarationStatement)node).fragments().iterator();
			} else if (node instanceof VariableDeclarationExpression) {
				fragments= ((VariableDeclarationExpression)node).fragments().iterator();
			}
			if (fragments != null) {
				while (fragments.hasNext()) {
					clearAccessMode(info, fragments.next());
				}
			}
		}
	}

	private void clearAccessMode(FlowInfo info, VariableDeclarationFragment fragment) {
			IVariableBinding binding= fragment.resolveBinding();
			if (binding != null && !binding.isField())
				info.clearAccessMode(binding, fFlowContext);
	}
}

