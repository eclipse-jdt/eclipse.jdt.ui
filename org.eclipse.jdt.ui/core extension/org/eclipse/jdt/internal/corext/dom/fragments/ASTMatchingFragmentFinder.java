/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
		super(true);
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
