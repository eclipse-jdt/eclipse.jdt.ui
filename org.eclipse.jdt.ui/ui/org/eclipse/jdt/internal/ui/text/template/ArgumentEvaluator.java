/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import java.text.NumberFormat;

public class ArgumentEvaluator implements VariableEvaluator {

	private final String[] fArguments;

	/**
	 * Creates an argument evaluator with template arguments.
	 */
	public ArgumentEvaluator(String[] arguments) {
		fArguments= arguments;
	}

	/*
	 * @see VariableEvaluator#reset()
	 */
	public void reset() {
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
		if (fArguments == null)
			return null;
		
		try {
			int i= Integer.parseInt(variable);
			
			if ((i < 0) || (i >= fArguments.length))
				return null;
				
			return new String(fArguments[i]);
		
		} catch (NumberFormatException e) {
			return null;
		}	
	}

}

