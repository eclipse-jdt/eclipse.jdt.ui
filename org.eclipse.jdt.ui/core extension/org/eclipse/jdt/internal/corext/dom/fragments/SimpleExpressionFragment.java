package org.eclipse.jdt.internal.corext.dom.fragments;

import org.eclipse.jdt.core.dom.Expression;

class SimpleExpressionFragment extends SimpleFragment implements IExpressionFragment {
	SimpleExpressionFragment(Expression node) {
		super(node);	
	}
	
	public Expression getAssociatedExpression() {
		return (Expression) getAssociatedNode();
	}
	
}
