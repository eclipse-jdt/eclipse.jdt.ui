/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

public class LocalVariableEvaluator implements VariableEvaluator {

	private static final String INDEX= "index"; // $NON-NLS-1$ //$NON-NLS-1$
	private static final String ARRAY= "array"; // $NON-NLS-1$ //$NON-NLS-1$
	private static final String ITERATOR= "iterator"; // $NON-NLS-1$ //$NON-NLS-1$
	private static final String COLLECTION= "collection"; // $NON-NLS-1$ //$NON-NLS-1$
	private static final String VECTOR= "vector"; // $NON-NLS-1$ //$NON-NLS-1$
	private static final String ENUMERATION= "enumeration"; // $NON-NLS-1$ //$NON-NLS-1$
	private static final String TYPE= "type"; // $NON-NLS-1$ //$NON-NLS-1$
	private static final String ELEMENT_TYPE= "element_type"; // $NON-NLS-1$ //$NON-NLS-1$
	private static final String ELEMENT= "element"; // $NON-NLS-1$ //$NON-NLS-1$

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
		// XXX return nothing useful for now
		if (variable.equals(INDEX)) {
			return "i"; // $NON-NLS-1$ //$NON-NLS-1$
		} else if (variable.equals(ARRAY)) {
			return "array";	// $NON-NLS-1$			 //$NON-NLS-1$
		} else if (variable.equals(ITERATOR)) {
			return "iterator"; // $NON-NLS-1$		 //$NON-NLS-1$
		} else if (variable.equals(COLLECTION)) {
			return "collection"; // $NON-NLS-1$		 //$NON-NLS-1$
		} else if (variable.equals(VECTOR)) {
			return "vector"; // $NON-NLS-1$		 //$NON-NLS-1$
		} else if (variable.equals(ENUMERATION)) {
			return "enumeration"; // $NON-NLS-1$		 //$NON-NLS-1$
		} else if (variable.equals(TYPE)) {
			return "Type"; // $NON-NLS-1$		 //$NON-NLS-1$
		} else if (variable.equals(ELEMENT_TYPE)) {
			return "Type"; // $NON-NLS-1$		 //$NON-NLS-1$
		} else if (variable.equals(ELEMENT)) {
			return "element"; // $NON-NLS-1$		 //$NON-NLS-1$

		} else {
			return null;
		}
	}

	/*
	 * @see VariableEvaluator#getRecognizedVariables()
	 */
	public String[] getRecognizedVariables() {
		return new String[] {INDEX, ARRAY};
	}

}

