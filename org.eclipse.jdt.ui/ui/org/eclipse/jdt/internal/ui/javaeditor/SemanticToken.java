/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * Semantic token
 */
public final class SemanticToken {

	/** AST node */
	private SimpleName fNode;
	
	/** Binding */
	private IBinding fBinding;
	
	/**
	 * @return Returns the binding, can be <code>null</code>.
	 */
	public IBinding getBinding() {
		if (fBinding == null && fNode != null)
			fBinding= fNode.resolveBinding();
		
		return fBinding;
	}
	
	/**
	 * @return Returns the AST node.
	 */
	public SimpleName getNode() {
		return fNode;
	}
	
	/**
	 * Update this token with the given AST node.
	 * <p>
	 * NOTE: Allowed to be used by {@link SemanticHighlightingReconciler} only.
	 * </p>
	 * 
	 * @param node the AST node
	 */
	protected void update(SimpleName node) {
		fNode= node;
		fBinding= null;
	}
	
	/**
	 * Clears this token.
	 * <p>
	 * NOTE: Allowed to be used by {@link SemanticHighlightingReconciler} only.
	 * </p>
	 */
	protected void clear() {
		fNode= null;
		fBinding= null;
	}
}
