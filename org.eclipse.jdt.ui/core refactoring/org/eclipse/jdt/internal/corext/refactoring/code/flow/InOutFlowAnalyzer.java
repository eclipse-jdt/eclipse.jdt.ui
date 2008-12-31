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
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class InOutFlowAnalyzer extends FlowAnalyzer {

	public InOutFlowAnalyzer(FlowContext context) {
		super(context);
	}

	public FlowInfo perform(ASTNode[] selectedNodes) {
		FlowContext context= getFlowContext();
		GenericSequentialFlowInfo result= createSequential();
		for (int i= 0; i < selectedNodes.length; i++) {
			ASTNode node= selectedNodes[i];
			node.accept(this);
			result.merge(getFlowInfo(node), context);
		}
		return result;
	}

	protected boolean traverseNode(ASTNode node) {
		// we are only traversing the selected nodes.
		return true;
	}

	protected boolean createReturnFlowInfo(ReturnStatement node) {
		// we are only traversing selected nodes.
		return true;
	}

	public void endVisit(Block node) {
		super.endVisit(node);
		clearAccessMode(accessFlowInfo(node), node.statements());
	}

	public void endVisit(CatchClause node) {
		super.endVisit(node);
		clearAccessMode(accessFlowInfo(node), node.getException());
	}

	public void endVisit(EnhancedForStatement node) {
		super.endVisit(node);
		clearAccessMode(accessFlowInfo(node), node.getParameter());
	}

	public void endVisit(ForStatement node) {
		super.endVisit(node);
		clearAccessMode(accessFlowInfo(node), node.initializers());
	}

	public void endVisit(MethodDeclaration node) {
		super.endVisit(node);
		FlowInfo info= accessFlowInfo(node);
		for (Iterator iter= node.parameters().iterator(); iter.hasNext();) {
			clearAccessMode(info, (SingleVariableDeclaration)iter.next());
		}
	}

	private void clearAccessMode(FlowInfo info, SingleVariableDeclaration decl) {
		IVariableBinding binding= decl.resolveBinding();
		if (binding != null && !binding.isField())
			info.clearAccessMode(binding, fFlowContext);
	}

	private void clearAccessMode(FlowInfo info, List nodes) {
		if (nodes== null || nodes.isEmpty() || info == null)
			return;
		for (Iterator iter= nodes.iterator(); iter.hasNext(); ) {
			Object node= iter.next();
			Iterator fragments= null;
			if (node instanceof VariableDeclarationStatement) {
				fragments= ((VariableDeclarationStatement)node).fragments().iterator();
			} else if (node instanceof VariableDeclarationExpression) {
				fragments= ((VariableDeclarationExpression)node).fragments().iterator();
			}
			if (fragments != null) {
				while (fragments.hasNext()) {
					clearAccessMode(info, (VariableDeclarationFragment)fragments.next());
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

