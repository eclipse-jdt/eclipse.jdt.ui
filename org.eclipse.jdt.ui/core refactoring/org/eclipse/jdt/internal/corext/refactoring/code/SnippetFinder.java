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
import org.eclipse.jdt.core.dom.MethodDeclaration;
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
		public void addLocal(IVariableBinding org, SimpleName local) {
			fLocalMappings.put(org, local);
		}
		public SimpleName getMappedName(IVariableBinding org) {
			return (SimpleName)fLocalMappings.get(org);
		}
		public boolean isEmpty() {
			return fNodes.isEmpty();
		}
		public boolean isMethodBody() {
			ASTNode first= (ASTNode)fNodes.get(0);
			if (first.getParent() == null)
				return false;
			ASTNode candidate= first.getParent().getParent();
			if (candidate == null || candidate.getNodeType() != ASTNode.METHOD_DECLARATION)
				return false;
			MethodDeclaration method= (MethodDeclaration)candidate;
			return method.getBody().statements().size() == fNodes.size();
		}
		public MethodDeclaration getEnclosingMethod() {
			ASTNode first= (ASTNode)fNodes.get(0);
			return (MethodDeclaration)ASTNodes.getParent(first, ASTNode.METHOD_DECLARATION);
		}
	}
	
	private class Matcher extends ASTMatcher {
		public boolean match(SimpleName candidate, Object s) {
			if (!(s instanceof SimpleName))
				return false;

			SimpleName snippet= (SimpleName)s;
			IBinding cb= candidate.resolveBinding();
			IBinding sb= snippet.resolveBinding();
			if (cb == null || sb == null)
				return false;
			IVariableBinding vcb= ASTNodes.getVariableBinding(candidate);
			IVariableBinding vsb= ASTNodes.getVariableBinding(snippet);
			if (vcb == null || vsb == null)
				return Bindings.equals(cb, sb);
			if (!vcb.isField() && !vsb.isField() && Bindings.equals(vcb.getType(), vsb.getType())) {
				SimpleName mapped= fMatch.getMappedName(vsb);
				if (mapped != null) {
					IVariableBinding mappedBinding= ASTNodes.getVariableBinding(mapped);
					if (!Bindings.equals(vcb, mappedBinding))
						return false;
				}
				fMatch.addLocal(vsb, candidate);
				return true;
			}
			return Bindings.equals(cb, sb);	
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
		reset();
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
		if (node == fSnippet[fIndex])
			return false;
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
