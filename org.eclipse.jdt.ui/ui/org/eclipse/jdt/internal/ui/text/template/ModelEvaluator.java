/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

public class ModelEvaluator implements VariableEvaluator {

	private TemplateModel fModel= new TemplateModel();

	/*
	 * @see VariableEvaluator#reset()
	 */
	public void reset() {
		fModel.reset();
	}

	/*
	 * @see VariableEvaluator#acceptText(String, int)
	 */
	public void acceptText(String text, int offset) {
		fModel.append(text, TemplateModel.NON_EDITABLE_TEXT);		
	}

	/*
	 * @see VariableEvaluator#evaluateVariable(String, int)
	 */
	public String evaluateVariable(String variable, int offset) {
		if (variable.equals("cursor")) { //$NON-NLS-1$
			fModel.append("", TemplateModel.CARET); //$NON-NLS-1$
			return "";			 //$NON-NLS-1$
		} else {
			fModel.append(variable, TemplateModel.EDITABLE_TEXT);
			return variable;
		}
	}

	public TemplateModel getModel() {
		return fModel; // XXX shared!
	}
	
}

