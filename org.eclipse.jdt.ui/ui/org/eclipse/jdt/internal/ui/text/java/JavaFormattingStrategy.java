/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.textmanipulation.TextUtil;
import org.eclipse.jdt.internal.formatter.CodeFormatter;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage; 

public class JavaFormattingStrategy implements IFormattingStrategy {
	
	private String fInitialIndentation;
	private ISourceViewer fViewer;	

	public JavaFormattingStrategy(ISourceViewer viewer) {
		fViewer = viewer;
	}
	
	/**
	 * @see IFormattingStrategy#formatterStarts(String)
	 */
	public void formatterStarts(String initialIndentation) {
		fInitialIndentation= initialIndentation;
	}
	
	/**
	 * @see IFormattingStrategy#formatterStops()
	 */
	public void formatterStops() {
	}
	
	/**
	 * @see IFormattingStrategy#format(String, boolean, String, int[])
	 */
	public String format(String content, boolean isLineStart, String indentation, int[] positions) {
		CodeFormatter formatter= new CodeFormatter(JavaCore.getOptions());
		
		IDocument doc= fViewer.getDocument();
		String lineDelimiter= StubUtility.getLineDelimiterFor(doc);
		formatter.options.setLineSeparator(lineDelimiter);

		formatter.setPositionsToMap(positions);
		int indent= 0;
		if (fInitialIndentation != null) {
			indent= TextUtil.getIndent(fInitialIndentation, CodeFormatterPreferencePage.getTabSize());
		}
		String string= formatter.format(content, indent);
		System.arraycopy(formatter.getMappedPositions(), 0, positions, 0, positions.length);
		return string;
	}	
}