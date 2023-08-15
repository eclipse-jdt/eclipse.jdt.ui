/*******************************************************************************
 * Copyright (c) 2023 SAP and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP - initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.compare;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.compare.contentmergeviewer.IIgnoreWhitespaceContributor;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class JavaIgnoreWhitespaceContributor implements IIgnoreWhitespaceContributor {

	private TreeMap<Integer /* start offset */, Integer /* end offset */> stringsByOffset= null;
	private final IDocument document;

	public JavaIgnoreWhitespaceContributor(IDocument document) {
		this.document= document;
	}

	private void createStringsByOffsetIfAbsent() {
		if (stringsByOffset != null) {
			return;
		}
		stringsByOffset= new TreeMap<>();
		String source= document.get();
		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		scanner.setSource(source.toCharArray());
		try {
			int tokenType;
			while ((tokenType= scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
				if (ITerminalSymbols.TokenNameStringLiteral == tokenType || ITerminalSymbols.TokenNameTextBlock == tokenType) {
					int start= scanner.getCurrentTokenStartPosition();
					int end= scanner.getCurrentTokenEndPosition();
					stringsByOffset.put(start, end);
				}
			}
		} catch (InvalidInputException ex) {
			// We couldn't parse part of the input. Fall through and make the rest a single token
		}
	}

	@Override
	public boolean isIgnoredWhitespace(int lineNumber, int columnNumber) {
		createStringsByOffsetIfAbsent();
		try {
			int offset= document.getLineOffset(lineNumber) + columnNumber;
			Entry<Integer, Integer> entry= stringsByOffset.floorEntry(offset);
			if (entry != null) {
				int start= entry.getKey();
				int end= entry.getValue();
				if (offset >= start && offset <= end) {
					return false; // part of literal - whitespace cannot be ignored
				}
			}
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		return true;
	}
}
