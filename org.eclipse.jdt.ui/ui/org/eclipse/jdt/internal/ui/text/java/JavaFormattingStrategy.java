package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 

import org.eclipse.jface.text.BadLocationException;import org.eclipse.jface.text.IDocument;import org.eclipse.jface.text.formatter.IFormattingStrategy;import org.eclipse.jface.text.source.ISourceViewer;import org.eclipse.jdt.internal.compiler.ConfigurableOption;import org.eclipse.jdt.internal.formatter.CodeFormatter;import org.eclipse.jdt.internal.ui.JavaPlugin; 

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
	
	private String getLineDelimiter() {
		try {
			IDocument document= fViewer.getDocument();
			if (document != null) {
				return document.getLineDelimiter(1);
			}
		} catch (BadLocationException e) {
		}			
		return System.getProperty("line.separator", "\n");
	}
	
	/**
	 * @see IFormattingStrategy#format(String, boolean, String, int[])
	 */
	public String format(String content, boolean isLineStart, String indentation, int[] positions) {
		ConfigurableOption[] options= JavaPlugin.getDefault().getCodeFormatterOptions();
		CodeFormatter formatter= new CodeFormatter(options);

		String lineSeparator= getLineDelimiter();
		int index= 0;
		while (index < content.length() && Character.isWhitespace(content.charAt(index))) {
			index++;
		}
		int reverseIndex= content.length() - 1;
		while (reverseIndex > 0 && Character.isWhitespace(content.charAt(reverseIndex))) {
			reverseIndex--;
		}

		String result= null;

		if (reverseIndex >= index) {
			int counter= 0;
			int firstPosition= 0;
			if (positions != null && positions.length != 0) {
				for (int i= 0; i < positions.length; i++) {
					if (positions[i] >= index || positions[i] <= reverseIndex) {
						if (counter == 0)
							firstPosition= i;
						counter++;
					}
				}
				int[] activePositions= new int[counter];
				for (int i= 0; i < counter; i++)
					activePositions[i]= positions[i + firstPosition] - index;
				formatter.setPositionsToMap(activePositions);
			}
			formatter.setInitialIndentationLevel(fInitialIndentation == null ? 0 : fInitialIndentation.length());
			result= formatter.formatSourceString(content.substring(index, reverseIndex + 1));
			if (positions != null && positions.length != 0) {
				int[] newPositions= formatter.getMappedPositions();
				for (int i= 0; i < counter; i++)
					positions[i + firstPosition]= newPositions[i] + index;
			}
			return content.substring(0, index) + result + content.substring(reverseIndex + 1);
		} else {
			return content;
		}

	}
}