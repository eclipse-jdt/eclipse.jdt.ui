/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

public class CursorSelectionEvaluator implements VariableEvaluator {

	private static final String CURSOR= "cursor"; // $NON-NLS-1$
	private static final String CURSOR_END= "cursor-end"; // $NON-NLS-1$

	private int fStart;
	private int fEnd;
	
	/**
	 * Creates an instance of <code>CursorSelectionEvaluator</code>.
	 */
	public CursorSelectionEvaluator() {
		reset();
	}
	
	/**
	 * Resets the <code>CursorSelectionEvaluator</code>.
	 */ 
	public void reset() {
		fStart= -1;
		fEnd= 0;
	}
	
	/**
	 * Returns the position where the selection starts, <code>-1</code> if no
	 * cursor selection/positioning occured.
	 */
	public int getStart() {
		return fStart;
	}

	/**
	 * Returns the position where the selection ends.
	 */
	public int getEnd() {
		return fEnd;
	}
	 	 
	/*
	 * @see VariableEvaluator#evaluate(String)
	 */
	public String evaluate(String variable, int offset) {
		if (variable.equals(CURSOR)) {
			fStart= offset;
			fEnd= fStart;

		} else if (variable.equals(CURSOR_END)) {
			fEnd= offset;
		}
		
		return ""; // $NON-NLS-1$
	}

	/*
	 * @see VariableEvaluator#getRecognizedVariables()
	 */
	public String[] getRecognizedVariables() {
		return new String[] {CURSOR, CURSOR_END};
	}

}

