/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

public class ASTNodeCodeBlock extends AbstractCodeBlock {

	private List fNodes= new ArrayList(3);

	public void add(ASTNode node) {
		fNodes.add(node);
	}

	public boolean isEmpty() {
		return fNodes.isEmpty();
	}
	
	public void fill(StringBuffer buffer, String firstLineIndent, String indent, String lineSeparator) {
		StringBuffer code= new StringBuffer();
		for (Iterator iter= fNodes.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			code.append(ASTNodes.asString(node));
		}
		int tabWidth= CodeFormatterUtil.getTabWidth();
		ICodeFormatter formatter= ToolFactory.createCodeFormatter();
		int flin= Strings.computeIndent(firstLineIndent, tabWidth);
		int in= Strings.computeIndent(indent, tabWidth);
		String formatted= formatter.format(code.toString(), in, null, lineSeparator);
		if (flin > in) {
			buffer.append(CodeFormatterUtil.createIndentString(flin - in));
		} else if (flin < in) {
			formatted= Strings.trimIndent(formatted,  in - flin, tabWidth);
		} 
		buffer.append(formatted);
	}
}
