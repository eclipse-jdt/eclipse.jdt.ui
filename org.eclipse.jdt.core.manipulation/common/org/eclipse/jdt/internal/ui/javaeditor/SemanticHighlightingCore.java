/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - copied from SemanticHighlighting and modified
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

/**
 * Semantic highlighting
 *
 * @since 1.11
 */
public abstract class SemanticHighlightingCore {

	/**
	 * @return the display name
	 */
	public abstract String getDisplayName();

	/**
	 * Returns <code>true</code> iff the semantic highlighting consumes the semantic token.
	 * <p>
	 * NOTE: Implementors are not allowed to keep a reference on the token or on any object
	 * retrieved from the token.
	 * </p>
	 *
	 * @param token the semantic token for a {@link org.eclipse.jdt.core.dom.SimpleName}
	 * @return <code>true</code> iff the semantic highlighting consumes the semantic token
	 */
	public abstract boolean consumes(SemanticToken token);

	/**
	 * Returns <code>true</code> iff the semantic highlighting consumes the
	 * semantic token.
	 * <p>
	 * NOTE: Implementors are not allowed to keep a reference on the token or on
	 * any object retrieved from the token.
	 * </p>
	 * @param token the semantic token for a
	 *        {@link org.eclipse.jdt.core.dom.NumberLiteral},
	 *        {@link org.eclipse.jdt.core.dom.BooleanLiteral} or
	 *        {@link org.eclipse.jdt.core.dom.CharacterLiteral}
	 * @return <code>true</code> iff the semantic highlighting consumes the
	 *         semantic token
	 */
	public boolean consumesLiteral(SemanticToken token) {
		return false;
	}

}
