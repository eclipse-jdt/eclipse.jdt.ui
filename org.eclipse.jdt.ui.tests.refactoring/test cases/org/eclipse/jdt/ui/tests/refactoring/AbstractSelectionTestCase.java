/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractCUTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;

public abstract class AbstractSelectionTestCase extends AbstractCUTestCase {

	private static final String SQUARE_BRACKET_OPEN= "/*[*/";
	private static final int    SQUARE_BRACKET_OPEN_LENGTH= SQUARE_BRACKET_OPEN.length();
	private static final String SQUARE_BRACKET_CLOSE=   "/*]*/";
	private static final int    SQUARE_BRACKET_CLOSE_LENGTH= SQUARE_BRACKET_CLOSE.length();
	
	public AbstractSelectionTestCase(String name) {
		super(name);
	}

	protected int[] getSelection(String source) {
		int start= -1;
		int end= -1;
		int includingStart= source.indexOf(SQUARE_BRACKET_OPEN);
		int excludingStart= source.indexOf(SQUARE_BRACKET_CLOSE);
		int includingEnd= source.lastIndexOf(SQUARE_BRACKET_CLOSE);
		int excludingEnd= source.lastIndexOf(SQUARE_BRACKET_OPEN);

		if (includingStart > excludingStart && excludingStart != -1) {
			includingStart= -1;
		} else if (excludingStart > includingStart && includingStart != -1) {
			excludingStart= -1;
		}
		
		if (includingEnd < excludingEnd) {
			includingEnd= -1;
		} else if (excludingEnd < includingEnd) {
			excludingEnd= -1;
		}
		
		if (includingStart != -1) {
			start= includingStart;
		} else {
			start= excludingStart + SQUARE_BRACKET_CLOSE_LENGTH;
		}
		
		if (excludingEnd != -1) {
			end= excludingEnd;
		} else {
			end= includingEnd + SQUARE_BRACKET_CLOSE_LENGTH;
		}
		
		assertTrue("Selection invalid", start >= 0 && end >= 0 && end >= start);
		
		int[] result= new int[] { start, end - start }; 
		// System.out.println("|"+ source.substring(result[0], result[0] + result[1]) + "|");
		return result;
	}
	
	protected InputStream getFileInputStream(String fileName) throws IOException {
		return RefactoringTestPlugin.getDefault().getTestResourceStream(fileName);
	}
}