/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui.display;

import org.eclipse.jface.text.ITextSelection;

public interface IDataDisplay {
	
	void clear();
	
	void displayExpression(String expression);
	
	void displayExpressionValue(String value);
	
	void selectLineForEvaluation(ITextSelection selection);
}