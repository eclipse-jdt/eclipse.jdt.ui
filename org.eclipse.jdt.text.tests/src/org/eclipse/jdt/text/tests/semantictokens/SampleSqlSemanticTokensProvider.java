/*******************************************************************************
 * Copyright (c) 2024 Broadcom Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alex Boyko (Broadcom Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.semantictokens;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TextBlock;

import org.eclipse.jdt.ui.text.java.ISemanticTokensProvider;

/**
 * Semantic tokens are computed for string literals or text blocks starting with "SQL:" prefix.
 * <ul>
 * <li>SELECT, WHERE, IN, FROM are KEYWORD</li>
 * <li>*, &lt;, &gt;, ==, != etc are OPEARATOR</li>
 * <li>Words starting from capital are CLASS</li>
 * <li>Numbers are NUMBER</li>
 * <li>all other lower case starting words are LOCAL_VARIABLE</li>
 * </ul>
 */
public class SampleSqlSemanticTokensProvider implements ISemanticTokensProvider {

	private static final String SQL_PREFIX = "SQL:";

	@Override
	public Collection<SemanticToken> computeSemanticTokens(CompilationUnit ast) {
		List<SemanticToken> tokens = new ArrayList<>();
		ast.accept(new ASTVisitor() {

			@Override
			public boolean visit(StringLiteral node) {
				tokens.addAll(reconileEmbeddedExpression(node));
				return super.visit(node);
			}

			@Override
			public boolean visit(TextBlock node) {
				tokens.addAll(reconileEmbeddedExpression(node));
				return super.visit(node);
			}

		});
		return tokens;
	}

	private List<SemanticToken> reconileEmbeddedExpression(Expression valueExp) {
		String text = null;
		int offset = 0;
		if (valueExp instanceof StringLiteral sl && sl.getLiteralValue().startsWith(SQL_PREFIX)) {
			text = sl.getEscapedValue();
			int skip = 1 + SQL_PREFIX.length();
			text = text.substring(skip, text.length() - 1);
			offset = sl.getStartPosition() + skip; // +1 to skip over opening " and over "SQL:"
		} else if (valueExp instanceof TextBlock tb && tb.getLiteralValue().startsWith(SQL_PREFIX)) {
			text = tb.getEscapedValue();
			int skip = 3 + SQL_PREFIX.length();
			text = text.substring(skip, text.length() - 3);
			offset = tb.getStartPosition() + skip; // +3 to skip over opening """ and over "SQL:"
		}
		return compute(text, offset);
	}

	private List<SemanticToken> compute(String text, int offset) {
		if (text == null) {
			return Collections.emptyList();
		}
		List<SemanticToken> tokens = new ArrayList<>();
		Matcher matcher= Pattern.compile("[\\w*=><!]+").matcher(text);
		while (matcher.find()) {
			String token = matcher.group();
			if (!token.isBlank()) {
				int start = matcher.start();
				int end = matcher.end();
				tokens.add(new SemanticToken(start + offset, end - start, getTokenType(token)));
			}
		}
		return tokens;
	}

	private TokenType getTokenType(String token) {
		try {
			NumberFormat.getInstance().parse(token);
			return TokenType.NUMBER;
		} catch (ParseException e) {
			switch (token) {
				case "SELECT":
				case "WHERE":
				case "WHEN":
				case "ALL":
				case "BY":
				case "ORDER":
				case "LIKE":
				case "IN":
				case "FROM":
				case "NOT":
					return TokenType.KEYWORD;
				case "*":
				case "(":
				case "-":
				case ">":
				case "<":
				case ">=":
				case "<=":
				case "==":
				case "!=":
					return TokenType.OPERATOR;
				default:
					if (token.length() > 0 && Character.isUpperCase(token.charAt(0))) {
						return TokenType.CLASS;
					}
					return TokenType.LOCAL_VARIABLE;
			}
		}
	}


}
