/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import java.util.HashSet;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ReturnStatement;

class ReturnFlowInfo extends FlowInfo {
	
	public ReturnFlowInfo(ReturnStatement node) {
		super(getReturnFlag(node));
	}
	
	public void merge(FlowInfo info, FlowContext context) {
		if (info == null)
			return;
			
		assignAccessMode(info);
	}
	
	private static int getReturnFlag(ReturnStatement node) {
		Expression expression= node.getExpression();
		if (expression == null || expression.resolveTypeBinding() == node.getAST().resolveWellKnownType("void")) //$NON-NLS-1$
			return VOID_RETURN;
		return VALUE_RETURN;
	}
}


