package org.eclipse.jdt.internal.ui.text.java;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
 

import org.eclipse.jdt.internal.compiler.ConfigurableOption;import org.eclipse.jdt.internal.formatter.CodeFormatter;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jface.text.formatter.IFormattingStrategy;import org.eclipse.jface.text.source.ISourceViewer; 

public class JavaFormattingStrategy implements IFormattingStrategy {
	
	private String fInitialIndentation;
	private ISourceViewer fViewer;
	private final static int CLEAR_ALL_NEWLINES_OPTION = 3;
	

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
		
		ConfigurableOption[] options = JavaPlugin.getDefault().getCodeFormatterOptions();

		boolean clearAllNewlines = false;

		for (int i=0; i < options.length; i++)

		{ 
			if (options[i].getID() == CLEAR_ALL_NEWLINES_OPTION)
			{
				clearAllNewlines = options[i].getCurrentValueIndex() == 0 ? true: false;
				break;
			}
		}

		CodeFormatter formatter= new CodeFormatter(options);	

		String lineSeparator= null;
		try {
			lineSeparator = fViewer.getDocument().getLineDelimiter(1);
		} catch (Exception e) {}

		if (lineSeparator == null)
			lineSeparator = System.getProperty("line.separator");
		int index= 0;
		while (index < content.length() && Character.isWhitespace(content.charAt(index)) && ! clearAllNewlines)
		{
			index++;
		}
		int reverseIndex= content.length();
		while(reverseIndex > 1 && Character.isWhitespace(content.charAt(reverseIndex - 1)) && ! clearAllNewlines)
		{
			reverseIndex--;
		}
		if (positions != null)
			formatter.setPositionsToMap(positions);

		formatter.setInitialIndentationLevel(fInitialIndentation == null ? 0 : fInitialIndentation.length());

		String result = null;

		if (reverseIndex > index)
			result = formatter.formatSourceString(content.substring(index, reverseIndex));

		if (positions != null) {
			int[] newPositions= formatter.getMappedPositions();
			for (int i= 0; i < positions.length; i++)
				positions[i]= newPositions[i];
		}
		
		if (reverseIndex >= index)
		{
			if (!clearAllNewlines)
				return content.substring(0, index) + result + content.substring(reverseIndex) ;
			else 
				return content.substring(0, index) + result + content.substring(reverseIndex) + lineSeparator;
		}
		else
			return "";

	}
}