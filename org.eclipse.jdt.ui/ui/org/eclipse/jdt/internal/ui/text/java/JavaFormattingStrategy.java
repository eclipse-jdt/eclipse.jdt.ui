package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 

import org.eclipse.jface.text.IDocument;import org.eclipse.jface.text.formatter.IFormattingStrategy;import org.eclipse.jface.text.source.ISourceViewer;import org.eclipse.jdt.internal.compiler.ConfigurableOption;import org.eclipse.jdt.internal.formatter.CodeFormatter;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.codemanipulation.StubUtility; 



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
		ConfigurableOption[] options= JavaPlugin.getDefault().getCodeFormatterOptions();
		CodeFormatter formatter= new CodeFormatter(options);
		
		IDocument doc= fViewer.getDocument();
		String lineDelimiter= StubUtility.getLineDelimiterFor(doc);
		formatter.options.setLineSeparator(lineDelimiter);

		formatter.setPositionsToMap(positions);
		formatter.setInitialIndentationLevel(fInitialIndentation == null ? 0 : fInitialIndentation.length());
		return formatter.formatSourceString(content);
	}	
}