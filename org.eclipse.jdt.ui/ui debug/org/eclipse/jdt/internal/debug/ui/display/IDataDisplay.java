/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui.display;

public interface IDataDisplay {
	
	void clear();
	
	void displayExpression(String expression);
	
	void displayExpressionValue(String value);
}