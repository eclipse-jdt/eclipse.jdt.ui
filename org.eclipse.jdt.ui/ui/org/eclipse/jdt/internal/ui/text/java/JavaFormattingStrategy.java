package org.eclipse.jdt.internal.ui.text.java;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
 

import org.eclipse.jdt.internal.compiler.ConfigurableOption;import org.eclipse.jdt.internal.formatter.CodeFormatter;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jface.text.BadLocationException;import org.eclipse.jface.text.IDocument;import org.eclipse.jface.text.formatter.IFormattingStrategy;import org.eclipse.jface.text.source.ISourceViewer; 

public class JavaFormattingStrategy implements IFormattingStrategy {
	
	private String fInitialIndentation;
	private ISourceViewer fViewer;
	private final static int CLEAR_ALL_NEWLINES_OPTION = 3;
	

	public JavaFormattingStrategy(ISourceViewer viewer)
	{
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
		
		ConfigurableOption[] options = JavaPlugin.getDefault().getCodeFormatterOptions();

		System.out.println(content);
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
			IDocument document = fViewer.getDocument();
			if (document != null)	
				lineSeparator = document.getLineDelimiter(1);
		} catch (Exception e) {e.printStackTrace();}

		if (lineSeparator == null)
			lineSeparator = System.getProperty("line.separator");
		int index= 0;
		while (index < content.length() && Character.isWhitespace(content.charAt(index)))
		{
			index++;
		}
		int reverseIndex= content.length() - 1 ;
		while(reverseIndex > 0 && Character.isWhitespace(content.charAt(reverseIndex)))
		{
			reverseIndex--;
		}
		
	

		String result= null;	
			
		if (reverseIndex >= index)
		{
			int counter=0;
			int firstPosition=0;
			if (positions != null && positions.length != 0)
			{
				for (int i=0; i < positions.length; i++)
				{
					if (positions[i] >= index || positions[i] <= reverseIndex)
					{
						if (counter ==0)
							firstPosition=i;
						counter++;
					}
				}
				int[] activePositions = new int[counter];
				for (int i=0; i < counter; i++)
					activePositions[i] = positions[i+firstPosition] - index;
				formatter.setPositionsToMap(activePositions);
			}
			formatter.setInitialIndentationLevel(fInitialIndentation == null ? 0 : fInitialIndentation.length());
			result = formatter.formatSourceString(content.substring(index, reverseIndex+1));
			if (positions != null && positions.length !=0) {
					int[] newPositions= formatter.getMappedPositions();
				for (int i= 0; i < counter; i++)
					positions[i+firstPosition] = newPositions[i] + index;
				System.out.println("");
			}
			return content.substring(0, index) + result + content.substring(reverseIndex+1);
		}
		else
			return content;

	}
}