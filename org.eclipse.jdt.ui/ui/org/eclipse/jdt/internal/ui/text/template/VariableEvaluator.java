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
	 * Returns the value of a variable.
	 * 
	 * @param variable the variable to evaluate.
	 * @param offset   the offset at which the variable is evaluated.
	 * @return        returns the value of the variable, <code>null</code> if the variable is not recognized.
	 */
	String evaluate(String variable, int offset);

	/**
	 * Returns the names of the variables the evaluator recognizes.
	 */
	String[] getRecognizedVariables();
	
}

