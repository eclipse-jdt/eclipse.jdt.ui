package org.eclipse.jdt.ui.text.java;

import java.util.Collection;

import org.eclipse.jdt.core.dom.CompilationUnit;

public interface SemanticTokensProvider {

	record SemanticToken(int ofset, int length, TokenType tokenType) {}

	enum TokenType {
		DEFAULT,
		OPERATOR,
		SINGLE_LINE_COMMENT,
		MULTI_LINE_COMMENT,
		BRACKET,
		STRING,
		RESTRICTED_IDENTIFIER,
		STATIC_FINAL_FIELD,
		STATIC_FIELD,
		FIELD,
		METHOD_DECLARATION,
		STATIC_METHOD_INVOCATION,
		INHERITED_METHOD_INVOCATION,
		ANNOTATION_ELEMENT_REFERENCE,
		ABSTRACT_METHOD_INVOCATION,
		LOCAL_VARIABLE_DECLARATION,
		LOCAL_VARIABLE,
		PARAMETER_VARIABLE,
		DEPRECATED_MEMBER,
		TYPE_VARIABLE,
		METHOD,
		AUTOBOXING,
		CLASS,
		ENUM,
		INTERFACE,
		ANNOTATION,
		TYPE_ARGUMENT,
		NUMBER,
		ABSTRACT_CLASS,
		INHERITED_FIELD,
		KEYWORD,
	}

	Collection<SemanticToken> computeSemanticTokens(CompilationUnit ast);

}


