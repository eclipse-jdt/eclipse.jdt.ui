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
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.List;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

public class TryCatchBlock extends AbstractCodeBlock {

	private AbstractCodeBlock fTryBody;
	private ITypeBinding[] fExceptions;

	public TryCatchBlock(ITypeBinding[] exceptions, AbstractCodeBlock tryBody) {
		fTryBody= tryBody;
		fExceptions= exceptions;
	}
	
	public boolean isEmpty() {
		return false;
	}

	public void fill(StringBuffer buffer, String firstLineIndent, String indent, String lineSeparator) {
		final String dummy= createStatement();
		final String placeHolder= "x();"; //$NON-NLS-1$
		ICodeFormatter formatter= ToolFactory.createCodeFormatter();
		int placeHolderStart= dummy.indexOf(placeHolder);
		Assert.isTrue(placeHolderStart != -1, "Place holder not found in original statements"); //$NON-NLS-1$
		int[] positions= new int[] {placeHolderStart, placeHolderStart + placeHolder.length() - 1};
		String formattedCode= formatter.format(dummy, 0, positions, lineSeparator);
		TextBuffer textBuffer= TextBuffer.create(formattedCode);
		String placeHolderLine= textBuffer.getLineContentOfOffset(positions[0]);
		String bodyIndent= indent + CodeFormatterUtil.createIndentString(placeHolderLine.substring(0, placeHolderLine.indexOf(placeHolder)));
		
		fill(buffer, formattedCode.substring(0, positions[0]), firstLineIndent, indent, lineSeparator);
		fTryBody.fill(buffer, "", bodyIndent, lineSeparator);
		fill(buffer, formattedCode.substring(positions[1] + 1), "", indent, lineSeparator);
	}

	private String createStatement() {
		StringBuffer buffer= new StringBuffer();
		buffer.append("try {x();} "); //$NON-NLS-1$
		for (int i= 0; i < fExceptions.length; i++) {
			buffer.append("catch("); //$NON-NLS-1$
			buffer.append(fExceptions[i].getName());
			buffer.append(" e){}"); //$NON-NLS-1$
		}
		return buffer.toString();	
	}

}
