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
package org.eclipse.jdt.internal.ui.tm4e;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.grammar.IStateStack;
import org.eclipse.tm4e.core.grammar.IToken;
import org.eclipse.tm4e.core.grammar.ITokenizeLineResult;
import org.eclipse.tm4e.core.model.TMToken;
import org.eclipse.tm4e.registry.ITMScope;
import org.eclipse.tm4e.registry.TMEclipseRegistryPlugin;
import org.eclipse.tm4e.ui.TMUIPlugin;
import org.eclipse.tm4e.ui.themes.ITheme;

import org.eclipse.swt.graphics.Color;

import org.eclipse.jface.text.TextAttribute;

import org.eclipse.jdt.core.manipulation.internal.javadoc.FencedCodeTokenizer;

/**
 * Tokenizer that uses tm4e (TextMate for Eclipse) to provide syntax coloring
 * for non-Java fenced code blocks in Javadoc markdown comments.
 */
public class TM4EFencedCodeTokenizer implements FencedCodeTokenizer {

	private static final Map<String, String> FENCE_TO_SCOPE= Map.ofEntries(
			Map.entry("css", "source.css"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("xml", "text.xml"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("xsl", "text.xml.xsl"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("html", "text.html.basic"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("htm", "text.html.basic"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("javascript", "source.js"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("js", "source.js"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("jsx", "source.js.jsx"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("typescript", "source.ts"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("ts", "source.ts"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("tsx", "source.tsx"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("json", "source.json"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("jsonc", "source.json.comments"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("python", "source.python"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("py", "source.python"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("ruby", "source.ruby"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("rb", "source.ruby"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("go", "source.go"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("rust", "source.rust"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("rs", "source.rust"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("yaml", "source.yaml"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("yml", "source.yaml"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("sql", "source.sql"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("bash", "source.shell"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("sh", "source.shell"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("shell", "source.shell"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("c", "source.c"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("cpp", "source.cpp"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("c++", "source.cpp"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("csharp", "source.cs"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("cs", "source.cs"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("php", "source.php"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("swift", "source.swift"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("groovy", "source.groovy"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("lua", "source.lua"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("perl", "source.perl"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("r", "source.r"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("diff", "source.diff"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("makefile", "source.makefile"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("dockerfile", "source.dockerfile"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("ini", "source.ini"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("properties", "source.ini"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("less", "source.css.less"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("scss", "source.css.scss"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("markdown", "text.html.markdown"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("md", "text.html.markdown"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("powershell", "source.powershell"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("ps1", "source.powershell"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("bat", "source.batchfile"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("clojure", "source.clojure"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("dart", "source.dart"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("fsharp", "source.fsharp"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("latex", "text.tex.latex"), //$NON-NLS-1$ //$NON-NLS-2$
			Map.entry("tex", "text.tex") //$NON-NLS-1$ //$NON-NLS-2$
	);

	private final Map<String, IGrammar> grammarCache= new ConcurrentHashMap<>();

	@Override
	public String tokenize(String language, String code) {
		String scopeName= FENCE_TO_SCOPE.get(language);
		if (scopeName == null) {
			return null;
		}

		IGrammar grammar= getGrammar(scopeName);
		if (grammar == null) {
			return null;
		}

		ITheme theme= TMUIPlugin.getThemeManager().getThemeForScope(scopeName);

		StringBuilder result= new StringBuilder(code.length() * 2);
		String[] lines= code.split("\\r?\\n", -1); //$NON-NLS-1$
		IStateStack ruleStack= null;

		for (int i= 0; i < lines.length; i++) {
			if (i > 0) {
				result.append('\n');
			}
			String line= lines[i];
			if (line.isEmpty()) {
				continue;
			}

			ITokenizeLineResult<IToken[]> lineResult= grammar.tokenizeLine(line, ruleStack, null);
			ruleStack= lineResult.getRuleStack();

			for (IToken token : lineResult.getTokens()) {
				int startIndex= token.getStartIndex();
				int endIndex= token.getEndIndex();
				if (startIndex >= line.length()) {
					break;
				}
				endIndex= Math.min(endIndex, line.length());
				String tokenText= line.substring(startIndex, endIndex);

				Color foreground= getTokenForeground(token, theme);
				if (foreground != null) {
					result.append("<span style=\"color: "); //$NON-NLS-1$
					result.append(getCssColor(foreground));
					result.append(";\">"); //$NON-NLS-1$
					appendEscaped(result, tokenText);
					result.append("</span>"); //$NON-NLS-1$
				} else {
					appendEscaped(result, tokenText);
				}
			}
		}

		return result.toString();
	}

	private IGrammar getGrammar(String scopeName) {
		return grammarCache.computeIfAbsent(scopeName, scope -> {
			try {
				return TMEclipseRegistryPlugin.getGrammarRegistryManager()
						.getGrammarForScope(ITMScope.parse(scope));
			} catch (Exception e) {
				return null;
			}
		});
	}

	private static Color getTokenForeground(IToken token, ITheme theme) {
		List<String> scopes= token.getScopes();
		if (scopes == null || scopes.isEmpty()) {
			return null;
		}
		String type= scopes.get(scopes.size() - 1);
		TMToken tmToken= new TMToken(token.getStartIndex(), type, scopes, null);
		org.eclipse.jface.text.rules.IToken jfaceToken= theme.getToken(tmToken);
		Object data= jfaceToken.getData();
		if (data instanceof TextAttribute textAttr) {
			return textAttr.getForeground();
		}
		return null;
	}

	private static String getCssColor(Color color) {
		return "rgb(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	private static void appendEscaped(StringBuilder sb, String text) {
		for (int i= 0; i < text.length(); i++) {
			char ch= text.charAt(i);
			switch (ch) {
				case '&':
					sb.append("&amp;"); //$NON-NLS-1$
					break;
				case '<':
					sb.append("&lt;"); //$NON-NLS-1$
					break;
				case '>':
					sb.append("&gt;"); //$NON-NLS-1$
					break;
				default:
					sb.append(ch);
					break;
			}
		}
	}
}
