
package org.eclipse.jdt.internal.debug.ui.display;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

public interface IDataDisplay {
	
	public void clear();
	
	public void displayExpression(String expression);
	
	public void displayExpressionValue(String value);
}