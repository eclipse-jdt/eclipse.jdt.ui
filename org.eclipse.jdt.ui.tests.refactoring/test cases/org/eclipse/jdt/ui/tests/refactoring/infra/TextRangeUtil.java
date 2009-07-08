/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.infra;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.SourceRange;


public class TextRangeUtil {

	//no instances
	private TextRangeUtil(){}

	public static ISourceRange getSelection(ICompilationUnit cu, int startLine, int startColumn, int endLine, int endColumn) throws Exception {
		IDocument document= new Document(cu.getSource());
		int offset= getOffset(document, startLine, startColumn);
		int end= getOffset(document, endLine, endColumn);
		return new SourceRange(offset, end - offset);
	}

	public static int getOffset(ICompilationUnit cu, int line, int column) throws Exception {
		IDocument document= new Document(cu.getSource());
		return getOffset(document, line, column) ;
	}

	public static int getOffset(String source, int line, int column) throws BadLocationException {
		IDocument document= new Document(source);
		return getOffset(document, line, column);
	}

	private static int getOffset(IDocument document, int line, int column) throws BadLocationException {
		int r= document.getLineInformation(line - 1).getOffset();
		IRegion region= document.getLineInformation(line - 1);
		int lineTabCount= calculateTabCountInLine(document.get(region.getOffset(), region.getLength()), column);
		r += (column - 1) - (lineTabCount * getTabWidth()) + lineTabCount;
		return r;
	}

	private static final int getTabWidth(){
		return 4;
	}

	public static int calculateTabCountInLine(String lineSource, int lastCharOffset){
		int acc= 0;
		int charCount= 0;
		for(int i= 0; charCount < lastCharOffset - 1; i++){
			if ('\t' == lineSource.charAt(i)){
				acc++;
				charCount += getTabWidth();
			}	else
				charCount += 1;
		}
		return acc;
	}
}
