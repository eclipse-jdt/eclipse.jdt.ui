/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;


public class SnippetFinder extends GenericVisitor {
	
	public static class Match {
		private List fNodes;
		private Map fLocalMappings;
		
		public Match() {
			fNodes= new ArrayList(10);
			fLocalMappings= new HashMap();
		}
		public void add(ASTNode node) {
			fNodes.add(node);
		}
		public ASTNode[] getNodes() {
			return (ASTNode[])fNodes.toArray(new ASTNode[fNodes.size()]);
		}
		public void addLocal(String org, String local) {
			fLocalMappings.put(org, local);
		}
		public boolean isEmpty() {
			return fNodes.isEmpty();
		}
	}
	
	private class Matcher extends ASTMatcher {
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTMatcher#match(org.eclipse.jdt.core.dom.SimpleName, java.lang.Object)
		 */
		public boolean match(SimpleName node, Object o) {
			if (!(o instanceof SimpleName))
				return false;

			SimpleName other= (SimpleName)node;
			IBinding left= node.resolveBinding();
			IBinding right= node.resolveBinding();
			if (left == null || right == null)
				return false;
			if (Bindings.equals(left, right))
				return true;
			IVariableBinding vLeft= ASTNodes.getVariableBinding(node);
			IVariableBinding vRight= ASTNodes.getVariableBinding(other);
			if (vLeft == null || vRight == null)
				return false;
			if (!vLeft.isField() && !vRight.isField() && Bindings.equals(vLeft.getType(), vRight.getType())) {
				fMatch.addLocal(node.getIdentifier(), other.getIdentifier());
				return true;
			}
			return false;	
		}
	}

	private List fResult= new ArrayList(2);
	private Match fMatch;
	private ASTNode[] fSnippet;
	private int fIndex;
	private Matcher fMatcher;
	
	private SnippetFinder(ASTNode[] snippet) {
		fSnippet= snippet;
		fMatcher= new Matcher();
		fIndex= 0;
	}
	
	public static Match[] perform(ASTNode start, ASTNode[] snippet) {
		SnippetFinder finder= new SnippetFinder(snippet);
		start.accept(finder);
		return (Match[])finder.fResult.toArray(new Match[finder.fResult.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node) {
		if (matches(node)) {
			return false;
		} else if (!isResetted()){
			reset();
			if (matches(node))
				return false;
		}
		return true;
	}
	
	private boolean matches(ASTNode node) {
		if (node.subtreeMatch(fMatcher, fSnippet[fIndex])) {
			fMatch.add(node);
			fIndex++;
			if (fIndex == fSnippet.length) {
				fResult.add(fMatch);
				reset();
			}
			return true;
		}
		return false;
	}

	private boolean isResetted() {
		return fIndex == 0 && fMatch.isEmpty();
	}

	private void reset() {
		fIndex= 0;
		fMatch= new Match();
	}
}
