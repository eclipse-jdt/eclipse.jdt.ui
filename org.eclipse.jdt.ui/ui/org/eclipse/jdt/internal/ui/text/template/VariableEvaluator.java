/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

/**
 * An interface for evaluating variables.
 */
public interface VariableEvaluator {

	/**
	 * Resets the evaluator.
	 */
	void reset();

	/**
	 * Accepts a non-variable text.
	 * 
	 * @param text the text to accept.
	 */
	void acceptText(String text);

	/**
	 * Accepts a variable.
	 * 
	 * @param variable the variable to evaluate.
	 */
	void acceptVariable(String variable);
	
}

