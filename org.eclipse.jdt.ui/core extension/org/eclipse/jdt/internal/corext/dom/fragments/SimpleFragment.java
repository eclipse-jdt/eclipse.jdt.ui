/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom.fragments;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.internal.corext.dom.JdtASTMatcher;

class SimpleFragment extends ASTFragment {
	private ASTNode fNode;
	
	SimpleFragment(ASTNode node) {
		fNode= node;
	}

	public IASTFragment[] getMatchingFragmentsWithNode(ASTNode node) {
		if(!JdtASTMatcher.doNodesMatch(getAssociatedNode(), node))
			return new IASTFragment[0];
			
		IASTFragment match= ASTFragmentFactory.createFragmentForFullSubtree(node);
		Assert.isTrue(match.matches(this) || this.matches(match));
		return new IASTFragment[] { match };
	}
	public boolean matches(IASTFragment other) {
		Assert.isNotNull(other);
		return    other.getClass().equals(getClass())
		        && JdtASTMatcher.doNodesMatch(other.getAssociatedNode(), getAssociatedNode());
	}
	public IASTFragment[] getSubFragmentsMatching(IASTFragment toMatch) {
		return ASTMatchingFragmentFinder.findMatchingFragments(getAssociatedNode(), (ASTFragment) toMatch);
	}
	public int getStartPosition() {
		return fNode.getStartPosition();
	}
	public int getLength() {
		return fNode.getLength();	
	}
	public ASTNode getAssociatedNode() {
		Assert.isNotNull(fNode);
		return fNode;	
	}
}
