/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation;

/**
 * Interface for tokenizing fenced code blocks in Javadoc markdown comments.
 * Implementations provide syntax highlighting for non-Java languages by
 * converting source code into HTML with appropriate styling spans.
 * <p>
 * Implementations are contributed via the
 * <code>org.eclipse.jdt.core.manipulation.fencedCodeTokenizers</code>
 * extension point.
 * </p>
 *
 * @since 1.26
 */
public interface IFencedCodeTokenizer {

	/**
	 * Tokenizes the given source code for the specified language and returns
	 * HTML with syntax highlighting spans.
	 *
	 * @param language the language identifier from the fenced code block
	 *            (e.g., "python", "javascript"), already lowercased
	 * @param code the source code to tokenize
	 * @return HTML string with syntax highlighting, or {@code null} if the
	 *         language is not supported
	 */
	String tokenize(String language, String code);
}
