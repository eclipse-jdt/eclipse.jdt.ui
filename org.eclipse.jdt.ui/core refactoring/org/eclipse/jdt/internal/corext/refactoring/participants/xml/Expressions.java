/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import java.util.Collection;
import java.util.List;


/* package */ class Expressions {
	
	private Expressions() {
		// no instance
	}
	
	public static void checkAttribute(String name, String value) throws ExpressionException {
		if (value == null) {
			throw new ExpressionException(
				ExpressionException.MISSING_ATTRIBUTE, 
				ExpressionMessages.getFormattedString("Expression.attribute.missing", name)); //$NON-NLS-1$
		}
	}
	
	public static void checkAttribute(String name, String value, String[] validValues) throws ExpressionException {
		checkAttribute(name, value);
		for (int i= 0; i < validValues.length; i++) {
			if (value.equals(validValues[i]))
				return;
		}
		throw new ExpressionException(
			ExpressionException.WRONG_ATTRIBUTE_VALUE, 
			ExpressionMessages.getFormattedString("Expression.attribute.invalid_value", value)); //$NON-NLS-1$
	}
	
	public static void checkCollection(Object var, Expression expression) throws ExpressionException {
		if (var instanceof Collection)
			return;
		throw new ExpressionException(
			ExpressionException.VARIABLE_IS_NOT_A_COLLECTION, 
			ExpressionMessages.getFormattedString("Expression.variable.not_a_collection", expression.toString())); //$NON-NLS-1$
	}
	
	public static void checkList(Object var, Expression expression) throws ExpressionException {
		if (var instanceof List)
			return;
		throw new ExpressionException(
			ExpressionException.VARIABLE_IS_NOT_A_LIST, 
			ExpressionMessages.getFormattedString("Expression.variable.not_a_list", expression.toString())); //$NON-NLS-1$
	}
	
//	public static int convertToInt(String value) throws ExpressionException {
//		try {
//			return Integer.parseInt(value);
//		} catch (NumberFormatException e) {
//			throw new ExpressionException(
//				ExpressionException.VALUE_IS_NOT_AN_INTEGER,
//				"Value doesn't represent an integer");
//		}
//	}
}
