/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

public class MethodBlock {

	private String fSignature;
	private CodeBlock fBody;

	public MethodBlock(String signature, CodeBlock body) {
		fSignature= signature;
		fBody= body;
	}

	public void fill(StringBuffer buffer, String indent, String lineSeparator) {
		final String dummy= fSignature + "{ x(); }"; //$NON-NLS-1$
		final String placeHolder= "x();"; //$NON-NLS-1$
		ICodeFormatter formatter= ToolFactory.createCodeFormatter();
		int placeHolderStart= dummy.indexOf(placeHolder);
		Assert.isTrue(placeHolderStart != -1, "Place holder not found in original statements"); //$NON-NLS-1$
		int[] positions= new int[] {placeHolderStart, placeHolderStart + placeHolder.length() - 1};
		String formattedCode= formatter.format(dummy, 0, positions, lineSeparator);
		TextBuffer textBuffer= TextBuffer.create(formattedCode);
		String placeHolderLine= textBuffer.getLineContentOfOffset(positions[0]);
		String bodyIndent= indent + CodeFormatterUtil.createIndentString(placeHolderLine.substring(0, placeHolderLine.indexOf(placeHolder)));
		fill(buffer, formattedCode.substring(0, positions[0]), indent, lineSeparator, true);
		List lines= fBody.lines();
		int size= lines.size();
		int lastLine= size - 1;
		for (int i= 0; i < size; i++) {
			if (i > 0)
				buffer.append(bodyIndent);
			buffer.append((String)lines.get(i));
			if (i < lastLine)
				buffer.append(lineSeparator);
		}
		fill(buffer, formattedCode.substring(positions[1] + 1), indent, lineSeparator, false);
	}

	public static int probeSpacing(TextBuffer buffer, MethodDeclaration method) {
		MethodDeclaration[] methods= getSiblings(method);
		if (methods != null && methods.length >= 1) {
			int start;
			if (methods.length == 1)
				start= methods[0].getStartPosition();
			else
				start= methods[1].getStartPosition();
			int lineNumber= buffer.getLineOfOffset(start);
			int result= 0;
			while (lineNumber > 0) {
				lineNumber--;
				String line= buffer.getLineContent(lineNumber);
				if (Strings.containsOnlyWhitespaces(line)) {
					result++;
				} else {
					return result;
				}
			}
		}
		return 1;
	}
	
	private static MethodDeclaration[] getSiblings(MethodDeclaration method) {
		ASTNode parent= method.getParent();
		if (parent instanceof TypeDeclaration)
			return ((TypeDeclaration)parent).getMethods();
		if (parent instanceof AnonymousClassDeclaration) {
			List body= ((AnonymousClassDeclaration)parent).bodyDeclarations();
			List result= new ArrayList();
			for (Iterator iter= body.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (element instanceof MethodDeclaration)
					result.add(element);
			}
			return (MethodDeclaration[]) result.toArray(new MethodDeclaration[result.size()]);
		}
		return null;
	}
	
	private void fill(StringBuffer buffer, String code, String indent, String lineSeparator, boolean indentFirstLine) {
		if ("".equals(indent)) {
			buffer.append(code);
			return;
		}	
		String[] lines= Strings.convertIntoLines(code);
		final int lastLine= lines.length - 1;
		for (int i= 0; i < lines.length; i++) {
			if (i > 0 || indentFirstLine)
				buffer.append(indent);
			buffer.append(lines[i]);
			if (i < lastLine)
				buffer.append(lineSeparator);
		}
	}
}