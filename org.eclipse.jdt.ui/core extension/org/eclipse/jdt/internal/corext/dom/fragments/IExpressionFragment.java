package org.eclipse.jdt.internal.corext.dom.fragments;

import org.eclipse.jdt.core.dom.Expression;

/**
 * Represents a fragment (@see IASTFragment) for which the node
 * to which the fragment maps is an Expression.
 */
public interface IExpressionFragment extends IASTFragment {
	
	/** 
	 * Every IASTFragment maps to an ASTNode, although this mapping may
	 * not be straightforward, and more than one fragment may map to the
	 * same node.
	 * An IExpressionFragment maps, specifically, to an Expression.
	 * 
	 * @return Expression	The node to which this fragment maps.
	 */
	public Expression getAssociatedExpression();
}
