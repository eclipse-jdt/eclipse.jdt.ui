/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import java.util.ArrayList;

import org.eclipse.jface.text.source.translation.ITranslatorResultCollector;

/**
 * Collects the result for the JspTranslator.
 *
 * @since 3.0
 */
public class JspTranslatorResultCollector implements ITranslatorResultCollector {

	private StringBuffer fDeclarations= new StringBuffer();
	private StringBuffer fContent= new StringBuffer();
	private StringBuffer fLocalDeclarations= new StringBuffer();

	private ArrayList fContentLines= new ArrayList();
	private ArrayList fDeclarationLines= new ArrayList();
	private ArrayList fLocalDeclarationLines= new ArrayList();

	/**
	 * @param declarations
	 * @param localDeclarations
	 * @param content
	 * @param declarationLines
	 * @param localDeclarationLines
	 * @param contentLines
	 */
	public JspTranslatorResultCollector(StringBuffer declarations, StringBuffer localDeclarations, StringBuffer content, ArrayList declarationLines, ArrayList localDeclarationLines, ArrayList contentLines) {
		fDeclarations= declarations;
		fLocalDeclarations= localDeclarations;
		fContent= content;
		fDeclarationLines= declarationLines;
		fLocalDeclarationLines= localDeclarationLines;
		fContentLines= contentLines;
	}

	// XXX: In the real world we would need to pass a list of line numbers
	public void appendDeclaration(String string, int lineNumber)  {
		fDeclarations.append(string);
		fDeclarationLines.add(Integer.valueOf(lineNumber));
	}

	// XXX: In the real world we would need to pass a list of line numbers
	public void appendLocalDeclaration(String string, int lineNumber)  {
		fLocalDeclarations.append(string);
		fLocalDeclarationLines.add(Integer.valueOf(lineNumber));
	}

	// XXX: In the real world we would need to pass a list of line numbers
	public void appendContent(String string, int lineNumber)  {
		fContent.append(string);
		fContentLines.add(Integer.valueOf(lineNumber));
	}
}
