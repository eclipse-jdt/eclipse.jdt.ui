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
package org.eclipse.jdt.internal.corext.dom;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;


public class ContainerFinder extends ASTVisitor {

	private ASTNode fNode;

	private List fContainer;
	private int fIndex;

	public ContainerFinder(ASTNode node) {
		fNode= node;
		fIndex= -1;
		if (node.getParent() != null) {
			node.getParent().accept(this);
		}
	}
	
	public List getContainer() {
		return fContainer;
	}
	
	public int getIndex() {
		return fIndex;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		test(node.bodyDeclarations());
		return false;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.Block)
	 */
	public boolean visit(Block node) {
		test(node.statements());
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		test(node.statements());
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		test(node.bodyDeclarations());
		return false;
	}
	
	private boolean test(List nodes) {
		if ((fIndex= nodes.indexOf(fNode)) != -1) {
			fContainer= nodes;
			return true;
		}
		return false;
	}
}
