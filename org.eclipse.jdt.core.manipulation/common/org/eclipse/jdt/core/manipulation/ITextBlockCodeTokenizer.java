/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
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

import java.util.List;

/**
 * Interface for tokenizing text blocks annotated with {@code // language=xxx}
 * comments. Implementations provide structured token data for editor-level
 * syntax coloring of non-Java languages embedded in text blocks.
 * <p>
 * Implementations are contributed via the
 * <code>org.eclipse.jdt.core.manipulation.textBlockCodeTokenizers</code>
 * extension point.
 * </p>
 *
 * @since 1.26
 */
public interface ITextBlockCodeTokenizer {

	/**
	 * Token type categories for structured code tokenization.
	 *
	 * @since 1.26
	 */
	enum CodeTokenType {
		/** Language keyword */
		KEYWORD,
		/** String literal */
		STRING,
		/** Single-line comment */
		SINGLE_LINE_COMMENT,
		/** Multi-line or block comment */
		MULTI_LINE_COMMENT,
		/** Numeric literal */
		NUMBER,
		/** Operator symbol */
		OPERATOR,
		/** Type or class name */
		TYPE,
		/** Method or function name */
		METHOD,
		/** Variable reference */
		VARIABLE,
		/** Bracket, parenthesis, or brace */
		BRACKET,
		/** Default token with no specific category */
		DEFAULT
	}

	/**
	 * A structured code token with position, length, and type information.
	 *
	 * @param offset the offset relative to the start of the code string
	 * @param length the length of the token
	 * @param type the token type category
	 * @since 1.27
	 */
	record CodeToken(int offset, int length, CodeTokenType type) {}

	/**
	 * Tokenizes the given source code for the specified language and returns
	 * structured token data suitable for editor-level syntax coloring.
	 *
	 * @param language the language identifier (e.g., "python", "sql"),
	 *            already lowercased
	 * @param code the source code to tokenize
	 * @return list of code tokens with offsets relative to the start of
	 *         {@code code}, or {@code null} if the language is not supported
	 * @since 1.27
	 */
	List<CodeToken> tokenizeCode(String language, String code);
}
