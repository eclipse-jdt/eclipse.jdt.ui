package org.eclipse.jdt.internal.corext.dom.fragments;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;

class ASTMatchingFragmentFinder extends GenericVisitor {

	public static IASTFragment[] findMatchingFragments(ASTNode scope, ASTFragment toMatch) {
		return new ASTMatchingFragmentFinder(toMatch).findMatches(scope);
	}

	private ASTFragment fFragmentToMatch;
	private List fMatches= new ArrayList();

	private ASTMatchingFragmentFinder(ASTFragment toMatch) {
		fFragmentToMatch= toMatch;	
	}
	private IASTFragment[] findMatches(ASTNode scope) {
		fMatches.clear();
		scope.accept(this);
		return getMatches();
	}
	private IASTFragment[] getMatches() {
		return (IASTFragment[]) fMatches.toArray(new IASTFragment[fMatches.size()]);
	}
	
	protected boolean visitNode(ASTNode node) {
		IASTFragment[] localMatches= fFragmentToMatch.getMatchingFragmentsWithNode(node);
		for(int i= 0; i < localMatches.length; i++) {
			fMatches.add(localMatches[i]);	
		}
		return true;
	}

}
