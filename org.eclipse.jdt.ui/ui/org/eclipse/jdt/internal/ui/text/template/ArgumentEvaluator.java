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
	 * @see VariableEvaluator#evaluate(String, int)
	 */
	public String evaluate(String variable, int offset) {
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

	/*
	 * @see VariableEvaluator#getRecognizedVariables()
	 */
	public String[] getRecognizedVariables() {
		return new String[] {"0", "1"};  // $NON-NLS-1$ // XXX incomplete
	}

}

