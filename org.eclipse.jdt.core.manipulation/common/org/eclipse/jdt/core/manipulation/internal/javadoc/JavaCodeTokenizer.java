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
 *     Red Hat, Inc. - Initial API and implementation (using Claude AI)
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation.internal.javadoc;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A Java code tokenizer that produces HTML with CSS-class-annotated spans
 * for syntax highlighting in Javadoc hover code blocks.
 *
 * <p>Uses heuristic-based parsing to provide semantic-level coloring for
 * type names, method declarations/invocations, parameter references, and
 * local variable declarations/references, in addition to basic lexical
 * tokens (keywords, strings, comments, annotations, numbers).</p>
 *
 * <p>Token categories and their CSS classes:</p>
 * <ul>
 * <li>{@code jc-keyword} &mdash; Java keywords and primitive types</li>
 * <li>{@code jc-string} &mdash; String and character literals</li>
 * <li>{@code jc-comment} &mdash; Single-line and multi-line comments</li>
 * <li>{@code jc-annotation} &mdash; Annotations (@ followed by identifier)</li>
 * <li>{@code jc-type} &mdash; Type names (uppercase-starting identifiers)</li>
 * <li>{@code jc-method-decl} &mdash; Method declaration names</li>
 * <li>{@code jc-method} &mdash; Method invocation names</li>
 * <li>{@code jc-param} &mdash; Parameter variable references</li>
 * <li>{@code jc-local-var-decl} &mdash; Local variable declaration names</li>
 * <li>{@code jc-local-var} &mdash; Local variable references</li>
 * <li>{@code jc-number} &mdash; Numeric literals</li>
 * <li>{@code jc-type-arg} &mdash; Type arguments in generics</li>
 * <li>{@code jc-field} &mdash; Instance field references</li>
 * <li>{@code jc-static-field} &mdash; Static field references</li>
 * <li>{@code jc-static-final-field} &mdash; Static final field references</li>
 * <li>{@code jc-enum} &mdash; Enum type names</li>
 * </ul>
 */
public class JavaCodeTokenizer {

	private static final String CSS_CLASS_KEYWORD= "jc-keyword"; //$NON-NLS-1$
	private static final String CSS_CLASS_STRING= "jc-string"; //$NON-NLS-1$
	private static final String CSS_CLASS_COMMENT= "jc-comment"; //$NON-NLS-1$
	private static final String CSS_CLASS_ANNOTATION= "jc-annotation"; //$NON-NLS-1$
	private static final String CSS_CLASS_TYPE= "jc-type"; //$NON-NLS-1$
	private static final String CSS_CLASS_METHOD_DECL= "jc-method-decl"; //$NON-NLS-1$
	private static final String CSS_CLASS_METHOD= "jc-method"; //$NON-NLS-1$
	private static final String CSS_CLASS_PARAM= "jc-param"; //$NON-NLS-1$
	private static final String CSS_CLASS_LOCAL_VAR_DECL= "jc-local-var-decl"; //$NON-NLS-1$
	private static final String CSS_CLASS_LOCAL_VAR= "jc-local-var"; //$NON-NLS-1$
	private static final String CSS_CLASS_NUMBER= "jc-number"; //$NON-NLS-1$
	private static final String CSS_CLASS_TYPE_ARG= "jc-type-arg"; //$NON-NLS-1$
	private static final String CSS_CLASS_FIELD= "jc-field"; //$NON-NLS-1$
	private static final String CSS_CLASS_STATIC_FIELD= "jc-static-field"; //$NON-NLS-1$
	private static final String CSS_CLASS_STATIC_FINAL_FIELD= "jc-static-final-field"; //$NON-NLS-1$
	private static final String CSS_CLASS_ENUM= "jc-enum"; //$NON-NLS-1$

	private static final Set<String> KEYWORDS= Set.of(
			"abstract", "assert", //$NON-NLS-1$ //$NON-NLS-2$
			"boolean", "break", "byte", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"case", "catch", "char", "class", "const", "continue", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"default", "do", "double", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"else", "enum", "extends", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"false", "final", "finally", "float", "for", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			"goto", //$NON-NLS-1$
			"if", "implements", "import", "instanceof", "int", "interface", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"long", //$NON-NLS-1$
			"native", "new", "null", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"package", "private", "protected", "public", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"record", "return", //$NON-NLS-1$ //$NON-NLS-2$
			"sealed", "short", "static", "strictfp", "super", "switch", "synchronized", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
			"this", "throw", "throws", "transient", "true", "try", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"var", "void", "volatile", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"while", "yield" //$NON-NLS-1$ //$NON-NLS-2$
	);

	private static final Set<String> TYPE_KEYWORDS= Set.of(
			"boolean", "byte", "char", "double", "float", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			"int", "long", "short", "var", "void" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	);

	private static final Set<String> TYPE_DECLARING_KEYWORDS= Set.of(
			"class", "interface", "enum", "record" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	);

	private static final Set<String> MODIFIER_KEYWORDS= Set.of(
			"public", "protected", "private", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"static", "final", "abstract", "native", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"synchronized", "volatile", "transient", "strictfp", "sealed" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	);

	private enum Context {
		TOP_LEVEL, TYPE_BODY, ENUM_CONSTANTS, CODE_BODY
	}

	private final String code;
	private final int len;
	private final StringBuilder result;
	private int pos;
	private int braceDepth;
	private final Map<String, Deque<Integer>> parameterNames= new HashMap<>();
	private final Map<String, Integer> pendingParameters= new HashMap<>();
	private final Map<String, Deque<Integer>> localVariables= new HashMap<>();
	private final Map<String, Deque<Integer>> fields= new HashMap<>();
	private final Map<String, Deque<Integer>> staticFields= new HashMap<>();
	private final Map<String, Deque<Integer>> staticFinalFields= new HashMap<>();
	private final Deque<Context> contextStack= new ArrayDeque<>();
	private final Set<String> enumTypes= new HashSet<>();
	private boolean expectTypeBody;
	private boolean expectEnumBody;
	private boolean expectEnumName;
	private boolean seenStatic;
	private boolean seenFinal;

	public static String tokenize(String code) {
		return new JavaCodeTokenizer(code).doTokenize();
	}

	private JavaCodeTokenizer(String code) {
		this.code= code;
		this.len= code.length();
		this.result= new StringBuilder(len * 2);
	}

	private String doTokenize() {
		while (pos < len) {
			char ch= code.charAt(pos);

			if (ch == '/' && pos + 1 < len) {
				char next= code.charAt(pos + 1);
				if (next == '/') {
					consumeSingleLineComment();
					continue;
				}
				if (next == '*') {
					consumeMultiLineComment();
					continue;
				}
			}

			if (ch == '"') {
				consumeStringLiteral();
				continue;
			}
			if (ch == '\'') {
				consumeCharLiteral();
				continue;
			}

			if (ch == '@' && pos + 1 < len && Character.isJavaIdentifierStart(code.charAt(pos + 1))) {
				consumeAnnotation();
				continue;
			}

			if (Character.isJavaIdentifierStart(ch)) {
				consumeWord();
				continue;
			}

			if (Character.isDigit(ch)) {
				consumeNumber();
				continue;
			}

			if (ch == '{') {
				braceDepth++;
				if (expectEnumBody) {
					contextStack.push(Context.ENUM_CONSTANTS);
					expectTypeBody= false;
					expectEnumBody= false;
				} else if (expectTypeBody) {
					contextStack.push(Context.TYPE_BODY);
					expectTypeBody= false;
				} else {
					contextStack.push(Context.CODE_BODY);
				}
				activatePendingParameters();
				clearModifiers();
				appendEscaped(ch);
				pos++;
				continue;
			}
			if (ch == '}') {
				braceDepth--;
				cleanupScope();
				if (!contextStack.isEmpty()) {
					contextStack.pop();
				}
				clearModifiers();
				appendEscaped(ch);
				pos++;
				continue;
			}

			if (ch == ';') {
				if (currentContext() == Context.ENUM_CONSTANTS) {
					contextStack.pop();
					contextStack.push(Context.TYPE_BODY);
				}
				pendingParameters.clear();
			}
			appendEscaped(ch);
			pos++;
		}

		return result.toString();
	}

	private Context currentContext() {
		return contextStack.isEmpty() ? Context.TOP_LEVEL : contextStack.peek();
	}

	private void clearModifiers() {
		seenStatic= false;
		seenFinal= false;
	}

	private void activatePendingParameters() {
		for (Map.Entry<String, Integer> entry : pendingParameters.entrySet()) {
			addBinding(parameterNames, entry.getKey(), entry.getValue());
		}
		pendingParameters.clear();
	}

	private void cleanupScope() {
		cleanupBindings(localVariables);
		cleanupBindings(parameterNames);
		cleanupBindings(fields);
		cleanupBindings(staticFields);
		cleanupBindings(staticFinalFields);
	}

	private static void addBinding(Map<String, Deque<Integer>> map, String name, int depth) {
		map.computeIfAbsent(name, k -> new ArrayDeque<>()).push(depth);
	}

	private static boolean hasBinding(Map<String, Deque<Integer>> map, String name) {
		Deque<Integer> stack= map.get(name);
		return stack != null && !stack.isEmpty();
	}

	private void cleanupBindings(Map<String, Deque<Integer>> map) {
		map.entrySet().removeIf(e -> {
			Deque<Integer> stack= e.getValue();
			while (!stack.isEmpty() && stack.peek() > braceDepth) {
				stack.pop();
			}
			return stack.isEmpty();
		});
	}

	// ---- Lexical token consumers ----

	private void consumeSingleLineComment() {
		int start= pos;
		while (pos < len && code.charAt(pos) != '\n' && code.charAt(pos) != '\r') {
			pos++;
		}
		appendSpan(CSS_CLASS_COMMENT, code.substring(start, pos));
	}

	private void consumeMultiLineComment() {
		int start= pos;
		pos += 2;
		boolean terminated= false;
		while (pos + 1 < len) {
			if (code.charAt(pos) == '*' && code.charAt(pos + 1) == '/') {
				pos += 2;
				terminated= true;
				break;
			}
			pos++;
		}
		if (!terminated) {
			pos= len;
		}
		appendSpan(CSS_CLASS_COMMENT, code.substring(start, pos));
	}

	private void consumeStringLiteral() {
		if (pos + 2 < len && code.charAt(pos + 1) == '"' && code.charAt(pos + 2) == '"') {
			consumeTextBlock();
			return;
		}
		int start= pos;
		pos++;
		while (pos < len) {
			char ch= code.charAt(pos);
			if (ch == '\\' && pos + 1 < len) {
				pos += 2;
				continue;
			}
			if (ch == '"') {
				pos++;
				break;
			}
			if (ch == '\n' || ch == '\r') {
				break;
			}
			pos++;
		}
		appendSpan(CSS_CLASS_STRING, code.substring(start, pos));
	}

	private void consumeTextBlock() {
		int start= pos;
		pos += 3;
		while (pos + 2 < len) {
			if (code.charAt(pos) == '"' && code.charAt(pos + 1) == '"' && code.charAt(pos + 2) == '"') {
				pos += 3;
				appendSpan(CSS_CLASS_STRING, code.substring(start, pos));
				return;
			}
			if (code.charAt(pos) == '\\' && pos + 1 < len) {
				pos += 2;
			} else {
				pos++;
			}
		}
		pos= len;
		appendSpan(CSS_CLASS_STRING, code.substring(start, pos));
	}

	private void consumeCharLiteral() {
		int start= pos;
		pos++;
		while (pos < len) {
			char ch= code.charAt(pos);
			if (ch == '\\' && pos + 1 < len) {
				pos += 2;
				continue;
			}
			if (ch == '\'') {
				pos++;
				break;
			}
			if (ch == '\n' || ch == '\r') {
				break;
			}
			pos++;
		}
		appendSpan(CSS_CLASS_STRING, code.substring(start, pos));
	}

	private void consumeAnnotation() {
		int start= pos;
		pos++;
		while (pos < len && Character.isJavaIdentifierPart(code.charAt(pos))) {
			pos++;
		}
		appendSpan(CSS_CLASS_ANNOTATION, code.substring(start, pos));
	}

	private void consumeNumber() {
		int start= pos;
		if (code.charAt(pos) == '0' && pos + 1 < len) {
			char next= code.charAt(pos + 1);
			if (next == 'x' || next == 'X') {
				pos += 2;
				while (pos < len && isHexDigit(code.charAt(pos))) {
					pos++;
				}
			} else if (next == 'b' || next == 'B') {
				pos += 2;
				while (pos < len && isBinaryDigit(code.charAt(pos))) {
					pos++;
				}
			} else {
				consumeDecimalDigits();
			}
		} else {
			consumeDecimalDigits();
		}
		if (pos < len && code.charAt(pos) == '.') {
			if (pos + 1 < len && Character.isDigit(code.charAt(pos + 1))) {
				pos++;
				consumeDecimalDigits();
			}
		}
		if (pos < len && (code.charAt(pos) == 'e' || code.charAt(pos) == 'E')) {
			pos++;
			if (pos < len && (code.charAt(pos) == '+' || code.charAt(pos) == '-')) {
				pos++;
			}
			consumeDecimalDigits();
		}
		if (pos < len) {
			char ch= code.charAt(pos);
			if (ch == 'l' || ch == 'L' || ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D') {
				pos++;
			}
		}
		appendSpan(CSS_CLASS_NUMBER, code.substring(start, pos));
	}

	private void consumeDecimalDigits() {
		while (pos < len && (Character.isDigit(code.charAt(pos)) || code.charAt(pos) == '_')) {
			pos++;
		}
	}

	private static boolean isHexDigit(char ch) {
		return Character.isDigit(ch) || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F') || ch == '_';
	}

	private static boolean isBinaryDigit(char ch) {
		return ch == '0' || ch == '1' || ch == '_';
	}

	// ---- Word/identifier processing ----

	private void consumeWord() {
		boolean qualified= pos > 0 && code.charAt(pos - 1) == '.';
		int start= pos;
		while (pos < len && Character.isJavaIdentifierPart(code.charAt(pos))) {
			pos++;
		}
		String word= code.substring(start, pos);

		if (KEYWORDS.contains(word)) {
			appendSpan(CSS_CLASS_KEYWORD, word);
			if (TYPE_DECLARING_KEYWORDS.contains(word)) {
				if (!isPrecededByDot(start)) {
					expectTypeBody= true;
					expectEnumBody= "enum".equals(word); //$NON-NLS-1$
					expectEnumName= expectEnumBody;
				}
				clearModifiers();
			} else if (TYPE_KEYWORDS.contains(word)) {
				consumeOptionalArrayBrackets();
				tryConsumeDeclarationAfterType();
			} else if ("static".equals(word)) { //$NON-NLS-1$
				seenStatic= true;
			} else if ("final".equals(word)) { //$NON-NLS-1$
				seenFinal= true;
			} else if (!MODIFIER_KEYWORDS.contains(word)) {
				clearModifiers();
			}
		} else if (Character.isUpperCase(word.charAt(0))) {
			if (expectEnumName) {
				appendSpan(CSS_CLASS_ENUM, word);
				enumTypes.add(word);
				expectEnumName= false;
			} else if (currentContext() == Context.ENUM_CONSTANTS && looksLikeEnumConstant()) {
				appendSpan(CSS_CLASS_STATIC_FINAL_FIELD, word);
				addBinding(staticFinalFields, word, braceDepth);
			} else {
				String fieldCssClass= getFieldCssClass(word);
				if (fieldCssClass != null && !looksLikeTypeUsage()) {
					appendSpan(fieldCssClass, word);
				} else if (enumTypes.contains(word)) {
					appendSpan(CSS_CLASS_ENUM, word);
					consumeOptionalGenerics();
					consumeOptionalArrayBrackets();
					tryConsumeEnumConstantAccess();
					tryConsumeDeclarationAfterType();
				} else {
					appendSpan(CSS_CLASS_TYPE, word);
					consumeOptionalGenerics();
					consumeOptionalArrayBrackets();
					tryConsumeDeclarationAfterType();
				}
			}
		} else {
			classifyLowercaseIdentifier(word, qualified);
		}
	}

	private String getFieldCssClass(String word) {
		if (hasBinding(staticFinalFields, word)) {
			return CSS_CLASS_STATIC_FINAL_FIELD;
		}
		if (hasBinding(staticFields, word)) {
			return CSS_CLASS_STATIC_FIELD;
		}
		if (hasBinding(fields, word)) {
			return CSS_CLASS_FIELD;
		}
		return null;
	}

	private boolean isPrecededByDot(int position) {
		int p= position - 1;
		while (p >= 0) {
			char ch= code.charAt(p);
			if (Character.isWhitespace(ch)) {
				p--;
				continue;
			}
			if (ch == '/' && p >= 1 && code.charAt(p - 1) == '*') {
				p -= 2;
				while (p >= 1) {
					if (code.charAt(p) == '*' && code.charAt(p - 1) == '/') {
						p -= 2;
						break;
					}
					p--;
				}
				continue;
			}
			return ch == '.';
		}
		return false;
	}

	private boolean looksLikeEnumConstant() {
		int probe= pos;
		while (probe < len && (code.charAt(probe) == ' ' || code.charAt(probe) == '\t')) {
			probe++;
		}
		if (probe >= len) {
			return true;
		}
		char ch= code.charAt(probe);
		return ch == ',' || ch == ';' || ch == '(' || ch == '{' || ch == '}';
	}

	private boolean looksLikeTypeUsage() {
		int probe= pos;
		while (probe < len && (code.charAt(probe) == ' ' || code.charAt(probe) == '\t')) {
			probe++;
		}
		if (probe >= len) {
			return false;
		}
		char ch= code.charAt(probe);
		return ch == '<' || ch == '.' || ch == '[' || Character.isJavaIdentifierStart(ch);
	}

	private void tryConsumeEnumConstantAccess() {
		int probe= pos;
		while (probe < len && (code.charAt(probe) == ' ' || code.charAt(probe) == '\t')) {
			probe++;
		}
		if (probe >= len || code.charAt(probe) != '.') {
			return;
		}
		probe++;
		while (probe < len && (code.charAt(probe) == ' ' || code.charAt(probe) == '\t')) {
			probe++;
		}
		if (probe >= len || !Character.isJavaIdentifierStart(code.charAt(probe)) || !Character.isUpperCase(code.charAt(probe))) {
			return;
		}
		int nameStart= probe;
		int nameEnd= nameStart + 1;
		while (nameEnd < len && Character.isJavaIdentifierPart(code.charAt(nameEnd))) {
			nameEnd++;
		}
		String name= code.substring(nameStart, nameEnd);
		if (KEYWORDS.contains(name)) {
			return;
		}
		int afterName= nameEnd;
		while (afterName < len && (code.charAt(afterName) == ' ' || code.charAt(afterName) == '\t')) {
			afterName++;
		}
		if (afterName < len && code.charAt(afterName) == '(') {
			return;
		}
		appendEscapedRange(pos, nameStart);
		appendSpan(CSS_CLASS_STATIC_FINAL_FIELD, name);
		pos= nameEnd;
	}

	private void classifyLowercaseIdentifier(String word, boolean qualified) {
		int probe= pos;
		while (probe < len && (code.charAt(probe) == ' ' || code.charAt(probe) == '\t')) {
			probe++;
		}
		if (probe < len && code.charAt(probe) == '(') {
			appendSpan(CSS_CLASS_METHOD, word);
		} else if (!qualified && hasBinding(parameterNames, word)) {
			appendSpan(CSS_CLASS_PARAM, word);
		} else if (!qualified && hasBinding(localVariables, word)) {
			appendSpan(CSS_CLASS_LOCAL_VAR, word);
		} else if (hasBinding(staticFinalFields, word)) {
			appendSpan(CSS_CLASS_STATIC_FINAL_FIELD, word);
		} else if (hasBinding(staticFields, word)) {
			appendSpan(CSS_CLASS_STATIC_FIELD, word);
		} else if (hasBinding(fields, word)) {
			appendSpan(CSS_CLASS_FIELD, word);
		} else {
			appendEscapedText(word);
		}
	}

	// ---- Generics and array brackets ----

	private void consumeOptionalGenerics() {
		if (pos >= len || code.charAt(pos) != '<') {
			return;
		}
		appendEscaped('<');
		pos++;
		int depth= 1;
		while (pos < len && depth > 0) {
			char ch= code.charAt(pos);
			if (ch == '<') {
				depth++;
				appendEscaped(ch);
				pos++;
			} else if (ch == '>') {
				depth--;
				appendEscaped(ch);
				pos++;
			} else if (Character.isJavaIdentifierStart(ch)) {
				int wordStart= pos;
				while (pos < len && Character.isJavaIdentifierPart(code.charAt(pos))) {
					pos++;
				}
				String word= code.substring(wordStart, pos);
				if (KEYWORDS.contains(word)) {
					appendSpan(CSS_CLASS_KEYWORD, word);
				} else if (Character.isUpperCase(word.charAt(0))) {
					appendSpan(CSS_CLASS_TYPE_ARG, word);
					consumeOptionalGenerics();
				} else {
					appendEscapedText(word);
				}
			} else {
				appendEscaped(ch);
				pos++;
			}
		}
	}

	private void consumeOptionalArrayBrackets() {
		while (pos + 1 < len && code.charAt(pos) == '[' && code.charAt(pos + 1) == ']') {
			appendEscaped('[');
			appendEscaped(']');
			pos += 2;
		}
	}

	// ---- Declaration recognition ----

	private void tryConsumeDeclarationAfterType() {
		int probe= pos;
		while (probe < len && (code.charAt(probe) == ' ' || code.charAt(probe) == '\t')) {
			probe++;
		}
		int afterBrackets= probe;
		while (afterBrackets + 1 < len && code.charAt(afterBrackets) == '[' && code.charAt(afterBrackets + 1) == ']') {
			afterBrackets += 2;
		}
		if (afterBrackets > probe) {
			probe= afterBrackets;
			while (probe < len && (code.charAt(probe) == ' ' || code.charAt(probe) == '\t')) {
				probe++;
			}
		}
		if (probe + 2 < len && code.charAt(probe) == '.' && code.charAt(probe + 1) == '.' && code.charAt(probe + 2) == '.') {
			probe += 3;
			while (probe < len && (code.charAt(probe) == ' ' || code.charAt(probe) == '\t')) {
				probe++;
			}
		}

		boolean inTypeBody= currentContext() == Context.TYPE_BODY;
		if (probe >= len || !Character.isJavaIdentifierStart(code.charAt(probe))) {
			return;
		}
		if (!inTypeBody && !Character.isLowerCase(code.charAt(probe))) {
			return;
		}
		int nameStart= probe;
		int nameEnd= nameStart + 1;
		while (nameEnd < len && Character.isJavaIdentifierPart(code.charAt(nameEnd))) {
			nameEnd++;
		}
		String name= code.substring(nameStart, nameEnd);
		if (KEYWORDS.contains(name)) {
			return;
		}

		int afterName= nameEnd;
		while (afterName < len && (code.charAt(afterName) == ' ' || code.charAt(afterName) == '\t')) {
			afterName++;
		}
		if (afterName >= len) {
			return;
		}
		char nextChar= code.charAt(afterName);
		if (nextChar == '(') {
			appendEscapedRange(pos, nameStart);
			appendSpan(CSS_CLASS_METHOD_DECL, name);
			appendEscapedRange(nameEnd, afterName);
			pos= afterName;
			consumeMethodParameters();
			clearModifiers();
			return;
		}
		if (nextChar == ';' || nextChar == '=' || nextChar == ',' || nextChar == ':' || nextChar == ')') {
			appendEscapedRange(pos, nameStart);
			if (inTypeBody) {
				if (seenStatic && seenFinal) {
					appendSpan(CSS_CLASS_STATIC_FINAL_FIELD, name);
					addBinding(staticFinalFields, name, braceDepth);
				} else if (seenStatic) {
					appendSpan(CSS_CLASS_STATIC_FIELD, name);
					addBinding(staticFields, name, braceDepth);
				} else {
					appendSpan(CSS_CLASS_FIELD, name);
					addBinding(fields, name, braceDepth);
				}
			} else {
				appendSpan(CSS_CLASS_LOCAL_VAR_DECL, name);
				addBinding(localVariables, name, braceDepth);
			}
			clearModifiers();
			pos= nameEnd;
		}
	}

	// ---- Method parameter parsing ----

	private void consumeMethodParameters() {
		appendEscaped('(');
		pos++;

		while (pos < len) {
			char ch= code.charAt(pos);

			if (ch == ')') {
				appendEscaped(')');
				pos++;
				return;
			}

			if (Character.isWhitespace(ch)) {
				appendEscaped(ch);
				pos++;
				continue;
			}

			if (ch == ',') {
				appendEscaped(',');
				pos++;
				continue;
			}

			if (ch == '@' && pos + 1 < len && Character.isJavaIdentifierStart(code.charAt(pos + 1))) {
				consumeAnnotation();
				continue;
			}

			if (Character.isJavaIdentifierStart(ch)) {
				int start= pos;
				while (pos < len && Character.isJavaIdentifierPart(code.charAt(pos))) {
					pos++;
				}
				String word= code.substring(start, pos);

				if (KEYWORDS.contains(word)) {
					appendSpan(CSS_CLASS_KEYWORD, word);
					if (TYPE_KEYWORDS.contains(word)) {
						consumeOptionalArrayBrackets();
						consumeOptionalVarargs();
						tryConsumeParamName();
					}
				} else if (Character.isUpperCase(word.charAt(0))) {
					appendSpan(enumTypes.contains(word) ? CSS_CLASS_ENUM : CSS_CLASS_TYPE, word);
					consumeOptionalGenerics();
					consumeOptionalArrayBrackets();
					consumeOptionalVarargs();
					tryConsumeParamName();
				} else {
					appendEscapedText(word);
				}
				continue;
			}

			if (ch == '[' && pos + 1 < len && code.charAt(pos + 1) == ']') {
				appendEscaped('[');
				appendEscaped(']');
				pos += 2;
				continue;
			}

			appendEscaped(ch);
			pos++;
		}
	}

	private void consumeOptionalVarargs() {
		if (pos + 2 < len && code.charAt(pos) == '.' && code.charAt(pos + 1) == '.' && code.charAt(pos + 2) == '.') {
			result.append("..."); //$NON-NLS-1$
			pos += 3;
		}
	}

	private void tryConsumeParamName() {
		int probe= pos;
		while (probe < len && (code.charAt(probe) == ' ' || code.charAt(probe) == '\t')) {
			probe++;
		}
		if (probe >= len || !Character.isJavaIdentifierStart(code.charAt(probe)) || !Character.isLowerCase(code.charAt(probe))) {
			return;
		}
		int nameStart= probe;
		int nameEnd= nameStart + 1;
		while (nameEnd < len && Character.isJavaIdentifierPart(code.charAt(nameEnd))) {
			nameEnd++;
		}
		String name= code.substring(nameStart, nameEnd);
		if (KEYWORDS.contains(name)) {
			return;
		}
		appendEscapedRange(pos, nameStart);
		appendSpan(CSS_CLASS_PARAM, name);
		pendingParameters.put(name, braceDepth + 1);
		pos= nameEnd;
	}

	// ---- HTML output helpers ----

	private void appendSpan(String cssClass, String text) {
		result.append("<span class=\""); //$NON-NLS-1$
		result.append(cssClass);
		result.append("\">"); //$NON-NLS-1$
		appendEscapedText(text);
		result.append("</span>"); //$NON-NLS-1$
	}

	private void appendEscapedText(String text) {
		for (int i= 0; i < text.length(); i++) {
			appendEscaped(text.charAt(i));
		}
	}

	private void appendEscapedRange(int start, int end) {
		for (int i= start; i < end; i++) {
			appendEscaped(code.charAt(i));
		}
	}

	private void appendEscaped(char ch) {
		switch (ch) {
			case '&':
				result.append("&amp;"); //$NON-NLS-1$
				break;
			case '<':
				result.append("&lt;"); //$NON-NLS-1$
				break;
			case '>':
				result.append("&gt;"); //$NON-NLS-1$
				break;
			default:
				result.append(ch);
				break;
		}
	}
}
