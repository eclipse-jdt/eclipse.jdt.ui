/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * A helper class that changes simple name nodes identified by their bindings
 * to a new name. It also renames constructors if a type is renamed.
 */
public class SimpleNameRenamer extends ASTVisitor {

	private ASTRewrite fRewriter;
	private IBinding[] fBindings;
	private String[] fNewNames;
	
	private SimpleNameRenamer(ASTRewrite rewriter, IBinding[] bindings, String[] newNames) {
		fRewriter= rewriter;
		fBindings= bindings;
		fNewNames= newNames;
	}
	
	public static void perform(ASTRewrite rewriter, IBinding[] bindings, String[] newNames, ASTNode node) {
		SimpleNameRenamer changer= new SimpleNameRenamer(rewriter, bindings, newNames);
		node.accept(changer);
	}
	
	public static void perform(ASTRewrite rewriter, IBinding[] bindings, String[] newNames, ASTNode[] nodes) {
		SimpleNameRenamer changer= new SimpleNameRenamer(rewriter, bindings, newNames);
		for (int i= 0; i < nodes.length; i++) {
			nodes[i].accept(changer);
		}
	}
	
	public boolean visit(MethodDeclaration node) {
		if (node.isConstructor()) {
			TypeDeclaration decl= (TypeDeclaration) ASTNodes.getParent(node, ASTNode.TYPE_DECLARATION);
			String newName= getNewName(decl.resolveBinding());
			if (newName != null)
				rename(node.getName(), newName);
		}
		return true;
	}
	
	public boolean visit(SimpleName node) {
		String newName= getNewName(node.resolveBinding());
		if (newName != null)
			rename(node, newName);
		return true;
	}
	
	private String getNewName(IBinding binding) {
		if (binding == null)
			return null;
		for (int i= 0; i < fBindings.length; i++) {
			if (equals(fBindings[i], binding))
				return fNewNames[i];
		}
		return null;
	}
	
	private void rename(SimpleName node, String newName) {
		ASTNode newNode= node.getAST().newSimpleName(newName);
		fRewriter.markAsReplaced(node, newNode);
	}

	private boolean equals(IBinding b1, IBinding b2) {
		if (b1 == b2)
			return true;
		String k1= b1.getKey();
		String k2= b2.getKey();
		if (k1 == null || k2 == null)
				return false;
		return k1.equals(k2);
	}
}
