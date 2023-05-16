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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

public class JavaIgnoreWhitespaceContributorTest {

	@Test
	public void whitespaceInLiteralNotIgnored() throws Exception {
		String source= "String #ignored1# variable #ignored2# = \"lit #not_ignored#  eral\";";
		Map<String, ITextSelection> selections= new HashMap<>();
		source= extractSelection(source, selections);
		assertEquals(3,selections.size());
		assertSelections(source, selections);
	}

	@Test
	public void whitespaceInMultlineLiteralNotIgnored() throws Exception {
		String source= "String #ignored1# variable #ignored2# = \"\"\"\r\nlit\r\n #not_ignored#  eral\"\"\";";
		Map<String, ITextSelection> selections= new HashMap<>();
		source= extractSelection(source, selections);
		assertEquals(3,selections.size());
		assertSelections(source, selections);
	}

	private String extractSelection(String source, Map<String, ITextSelection> selections) {
		var result= new StringBuilder(source);
		var pattern = Pattern.compile("#([a-z0-9_]+)#");
		while (true) {
			Matcher matcher= pattern.matcher(result);
			if (matcher.find()) {
				int start= matcher.start();
				int end= matcher.end();
				String name= matcher.group(1);
				selections.put(name, new TextSelection(start, end - start));
				result.replace(start, end, "");
			} else {
				return result.toString();
			}
		}
	}

	private void assertSelections(String source, Map<String, ITextSelection> selections) throws BadLocationException {
		var doc= new Document(source);
		var c= new JavaIgnoreWhitespaceContributor(doc);
		for (var entry : selections.entrySet()) {
			int offset= entry.getValue().getOffset();
			int line= doc.getLineOfOffset(offset);
			int col= offset - doc.getLineOffset(line);
			if (entry.getKey().startsWith("not_")) {
				assertFalse(c.isIgnoredWhitespace(line, col));
			} else {
				assertTrue(c.isIgnoredWhitespace(line, col));
			}
		}
	}
}
