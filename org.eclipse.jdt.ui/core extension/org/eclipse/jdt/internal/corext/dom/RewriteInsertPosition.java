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
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 *
 */
public class RewriteInsertPosition {


	public static RewriteInsertPosition first(ASTNode parent, int childPropery) {
		return new RewriteInsertPosition(parent, childPropery, null, false);  // before null -> last
	}
	
	public static RewriteInsertPosition last(ASTNode parent, int childPropery) {
		return new RewriteInsertPosition(parent, childPropery, null, true); // after null -> first
	}
	
	public static RewriteInsertPosition after(ASTNode parent, int childPropery, ASTNode sibling) {
		return new RewriteInsertPosition(parent, childPropery, sibling, false);
	}
	
	public static RewriteInsertPosition before(ASTNode parent, int childPropery, ASTNode sibling) {
		return new RewriteInsertPosition(parent, childPropery, sibling, true);
	}
	
	
	private int fIndex;
	private int fChildPropery;
	private ASTNode fParent;
	private ASTNode fSibling;
	private boolean fBefore;

	/**
	 * 
	 */
	private RewriteInsertPosition(ASTNode parent, int childPropery, ASTNode sibling, boolean before) {
		fBefore= before;
		fSibling= sibling;
		fParent= parent;
		fChildPropery= childPropery;
	}
	
	/**
	 * @return Returns the before.
	 */
	public boolean isBefore() {
		return fBefore;
	}
	/**
	 * @return Returns the childPropery.
	 */
	public int getChildPropery() {
		return fChildPropery;
	}
	/**
	 * @return Returns the index.
	 */
	public int getIndex() {
		return fIndex;
	}
	/**
	 * @return Returns the parent.
	 */
	public ASTNode getParent() {
		return fParent;
	}
	/**
	 * @return Returns the sibling.
	 */
	public ASTNode getSibling() {
		return fSibling;
	}
	
	


}
