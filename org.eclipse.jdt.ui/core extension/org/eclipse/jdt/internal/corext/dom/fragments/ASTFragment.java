package org.eclipse.jdt.internal.corext.dom.fragments;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * @see IASTFragment, ASTFragmentFactory
 */
abstract class ASTFragment implements IASTFragment {

	/**
	 * Tries to create or find as many fragments as possible
	 * such that each fragment f matches
	 * this fragment and f.getNode() is <code>node</code>
	 */
	abstract IASTFragment[] getMatchingFragmentsWithNode(ASTNode node);
}

