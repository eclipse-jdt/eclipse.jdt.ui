package org.eclipse.jdt.ui.text.java;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IPath;

public interface SemanticTokensProvider {


	record SemanticToken(int ofset, int length, TokenType tokenType) {}

	enum TokenType {
		TYPE,
		METHOD,
		COMMENT,
		VARIABLE,
		KEYWORD
	}

	CompletableFuture<Collection<SemanticToken>> computeSemanticTokens(IPath resource);

}


