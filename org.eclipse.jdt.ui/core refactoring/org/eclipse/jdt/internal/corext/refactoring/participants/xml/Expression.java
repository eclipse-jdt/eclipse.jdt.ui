/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

public abstract class Expression {
	
	protected static final String ATT_VALUE= "value"; //$NON-NLS-1$
	
	public static final Expression TRUE= new Expression() {
		public TestResult evaluate(IVariablePool pool) throws CoreException {
			return TestResult.TRUE;
		}	
	};
	
	public static final Expression FALSE= new Expression() {
		public TestResult evaluate(IVariablePool pool) throws CoreException {
			return TestResult.FALSE;
		}	
	};
	
	public abstract TestResult evaluate(IVariablePool pool) throws CoreException;

	protected void checkAttribute(String name, String value) throws CoreException {
		if (value == null) {
			throw new CoreException(new ExpressionStatus(IStatus.ERROR, 
				IExpressionStatus.MISSING_ATTRIBUTE, "Missing attribute " + name));
		}
	}
	
	protected void checkAttribute(String name, String value, String[] validValues) throws CoreException {
		checkAttribute(name, value);
		for (int i= 0; i < validValues.length; i++) {
			if (value.equals(validValues[i]))
				return;
		}
		throw new CoreException(new ExpressionStatus(IStatus.ERROR,
			IExpressionStatus.WRONG_ATTRIBUTE_VALUE, "Wrong attribute value"));
	}
	
	
	protected static boolean isInstanceOf(Object element, String type) {
		// null isn't an instanceof of anything.
		if (element == null)
			return false;
		return isSubtype(element.getClass(), type); 
	}
	
	private static boolean isSubtype(Class clazz, String type) {
		if (clazz.getName().equals(type))
			return true;
		Class superClass= clazz.getSuperclass();
		if (superClass != null && isSubtype(superClass, type))
			return true;
		Class[] interfaces= clazz.getInterfaces();
		for (int i= 0; i < interfaces.length; i++) {
			if (isSubtype(interfaces[i], type))
				return true;
		} 
		return false;
	}		
}
