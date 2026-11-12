/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat, Inc. - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation.internal.javadoc;

/**
 * Tokenizer for non-Java fenced code blocks in Javadoc markdown comments.
 * Implementations produce HTML with inline color styles for syntax highlighting.
 */
@FunctionalInterface
public interface FencedCodeTokenizer {

	/**
	 * Tokenize code in the given language and return HTML with inline color styles.
	 *
	 * @param language the markdown fence language specifier (e.g. "css", "xml")
	 * @param code the code text to tokenize
	 * @return HTML string with syntax highlighting spans, or {@code null} if the
	 *         language is not supported
	 */
	String tokenize(String language, String code);
}
