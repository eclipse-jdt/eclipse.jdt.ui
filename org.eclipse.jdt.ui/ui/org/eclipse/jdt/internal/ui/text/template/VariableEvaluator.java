/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

/**
 * An interface to allow custom evaluation of variables.
 */
public interface VariableEvaluator {

	/**
	 * Resets the evaluator.
	 */
	void reset();

	/**
	 * Returns the value of a variable.
	 * 
	 * @param variable the variable to evaluate.
	 * @param offset   the offset at which the variable is evaluated.
	 * @return        returns the value of the variable, <code>null</code> if the variable is not recognized.
	 */
	void acceptText(String text, int offset);

	/**
	 * Returns the value of a variable, <code>null</code> if the variable
	 * cannot be evaluated.
	 * 
	 * @param variable the variable to evaluate.
	 * @param offset   the offset at which the variable is evaluated.
	 * @return         returns the value of the variable, <code>null</code> if the variable is not recognized.
	 */
	String evaluateVariable(String variable, int offset);
	
}

