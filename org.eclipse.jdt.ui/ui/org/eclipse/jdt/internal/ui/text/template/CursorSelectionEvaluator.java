/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

public class CursorSelectionEvaluator implements VariableEvaluator {

	private static final String CURSOR= "cursor"; // $NON-NLS-1$ //$NON-NLS-1$
	private static final String CURSOR_END= "cursor-end"; // $NON-NLS-1$ //$NON-NLS-1$

	private int fStart;
	private int fEnd;
	
	/**
	 * Creates an instance of <code>CursorSelectionEvaluator</code>.
	 */
	public CursorSelectionEvaluator() {
		reset();
	}
	
	/*
	 * @see VariableEvaluator#reset()
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
	 * @see VariableEvaluator#acceptText(String, int)
	 */
	public void acceptText(String variable, int offset) {
	}
		 	 
	/*
	 * @see VariableEvaluator#evaluateVariable(String, int)
	 */
	public String evaluateVariable(String variable, int offset) {
		if (variable.equals(CURSOR)) {
			fStart= offset;
			fEnd= fStart;
			return ""; // $NON-NLS-1$ //$NON-NLS-1$

		} else if (variable.equals(CURSOR_END)) {
			fEnd= offset;
			return ""; // $NON-NLS-1$ //$NON-NLS-1$
		}
		
		return null;
	}

}

