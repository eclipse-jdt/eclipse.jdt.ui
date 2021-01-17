/*******************************************************************************
 * Copyright (c) 2021 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * NodeMatcher.
 *
 * @param <N> ASTNode type.
 */
public abstract class NodeMatcher<N extends ASTNode> {
	/**
	 * Returns true if it matches, false if it is a boolean expression and it matches the opposite, or null otherwise.
	 *
	 * @param node The node
	 * @return true if it matches, false if it is a boolean expression and it matches the opposite, or null otherwise.
	 */
	public abstract Boolean isMatching(N node);

	/**
	 * Returns true if it is a boolean expression and it matches the opposite, false if it matches, or null otherwise.
	 *
	 * @return true if it is a boolean expression and it matches the opposite, false if it matches, or null otherwise.
	 */
	public NodeMatcher<N> negate() {
		final NodeMatcher<N> thisNodeMatcher= this;
		return new NodeMatcher<N>() {
			@Override
			public Boolean isMatching(final N node) {
				Boolean isMatching= thisNodeMatcher.isMatching(node);

				if (isMatching == null) {
					return null;
				}

				return !isMatching;
			}
		};
	}
}
