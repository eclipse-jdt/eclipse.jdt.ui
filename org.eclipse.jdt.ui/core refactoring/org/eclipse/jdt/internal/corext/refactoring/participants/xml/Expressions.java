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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;


/* package */ class Expressions {
	private Expressions() {
		// no instance
	}
	
	public static void checkCollection(Object var) throws CoreException {
		if (var instanceof Collection)
			return;
		throw new CoreException(new ExpressionStatus(IStatus.ERROR,
			IExpressionStatus.VARIABLE_IS_NOT_A_COLLECTION, "Variable isn't of type java.util.Collection"));
	}
	
	public static void checkList(Object var) throws CoreException {
		if (var instanceof List)
			return;
		throw new CoreException(new ExpressionStatus(IStatus.ERROR,
			IExpressionStatus.VARIABLE_IS_NOT_A_LIST, "Variable isn't of type java.util.List"));
	}
	
	public static int convertToInt(String value) throws CoreException {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new CoreException(new ExpressionStatus(IStatus.ERROR,
				IExpressionStatus.VALUE_IS_NOT_AN_INTEGER, "Value doesn't represent an integer", e));
		}
	}
}
