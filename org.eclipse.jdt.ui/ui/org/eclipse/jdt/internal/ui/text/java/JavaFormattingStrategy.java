package org.eclipse.jdt.internal.ui.text.java;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
 

import org.eclipse.jface.text.formatter.IFormattingStrategy;

import org.eclipse.jdt.internal.formatter.CodeFormatter; 

public class JavaFormattingStrategy implements IFormattingStrategy {
	
	private String fInitialIndentation;
		
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
		
		CodeFormatter formatter= new CodeFormatter(null);
		
		if (positions != null)
			formatter.setPositionsToMap(positions);
		
		formatter.setInitialIndentationLevel(fInitialIndentation == null ? 0 : fInitialIndentation.length());
		String result= formatter.formatSourceString(content);
		
		if (positions != null) {
			int[] newPositions= formatter.getMappedPositions();
			for (int i= 0; i < positions.length; i++)
				positions[i]= newPositions[i];
		}
		
		return result;
	}
}