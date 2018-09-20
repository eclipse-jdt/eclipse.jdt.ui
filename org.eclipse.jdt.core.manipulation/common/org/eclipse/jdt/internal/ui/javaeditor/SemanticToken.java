/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc - make hidden methods public now that class is refactored
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * Semantic token
 */
public final class SemanticToken {

	/** AST node */
	private SimpleName fNode;
	private Expression fLiteral;

	/** Binding */
	private IBinding fBinding;
	/** Is the binding resolved? */
	private boolean fIsBindingResolved= false;

	/** AST root */
	private CompilationUnit fRoot;
	private boolean fIsRootResolved= false;

	/**
	 * @return Returns the binding, can be <code>null</code>.
	 */
	public IBinding getBinding() {
		if (!fIsBindingResolved) {
			fIsBindingResolved= true;
			if (fNode != null)
				fBinding= fNode.resolveBinding();
		}

		return fBinding;
	}

	/**
	 * @return the AST node (a {@link SimpleName})
	 */
	public SimpleName getNode() {
		return fNode;
	}

	/**
	 * @return the AST node (a <code>Boolean-, Character- or NumberLiteral</code>)
	 */
	public Expression getLiteral() {
		return fLiteral;
	}

	/**
	 * @return the AST root
	 */
	public CompilationUnit getRoot() {
		if (!fIsRootResolved) {
			fIsRootResolved= true;
			fRoot= (CompilationUnit) (fNode != null ? fNode : fLiteral).getRoot();
		}

		return fRoot;
	}

	/**
	 * Update this token with the given AST node.
	 * <p>
	 * NOTE: Allowed to be used by {@link SemanticHighlightingReconciler} only.
	 * </p>
	 *
	 * @param node the AST simple name
	 */
	public void update(SimpleName node) {
		clear();
		fNode= node;
	}

	/**
	 * Update this token with the given AST node.
	 * <p>
	 * NOTE: Allowed to be used by {@link SemanticHighlightingReconciler} only.
	 * </p>
	 *
	 * @param literal the AST literal
	 */
	public void update(Expression literal) {
		clear();
		fLiteral= literal;
	}

	/**
	 * Clears this token.
	 * <p>
	 * NOTE: Allowed to be used by {@link SemanticHighlightingReconciler} only.
	 * </p>
	 */
	public void clear() {
		fNode= null;
		fLiteral= null;
		fBinding= null;
		fIsBindingResolved= false;
		fRoot= null;
		fIsRootResolved= false;
	}
}
