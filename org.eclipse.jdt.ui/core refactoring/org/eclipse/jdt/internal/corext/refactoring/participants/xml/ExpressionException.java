/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * A special exception thrown when an error occurrs while evaluation
 * an expression tree.
 * 
 * @see Expression#evaluate
 * 
 * @since 3.0
 */
public class ExpressionException extends CoreException {
	
	/** Error code indicating that the variable in focus in not a collection */
	public static final int VARIABLE_IS_NOT_A_COLLECTION= 3;
	
	/** Error code indicating that the variable in focus in not a list */
	public static final int VARIABLE_IS_NOT_A_LIST= 4;
	
	/** Error code indicating that an attribute value doesn't present an integer */
	public static final int VALUE_IS_NOT_AN_INTEGER= 5;
	
	/** Error code indicating that a mandantory attribute is missing */
	public static final int MISSING_ATTRIBUTE= 50;
	
	/** Error code indicating that the value specified for an attribute is invalid */
	public static final int WRONG_ATTRIBUTE_VALUE= 51;

	/** Error code indicating that the number of arguments passed to resolve variable is incorrect. */
	public static final int VARAIBLE_POOL_WRONG_NUMBER_OF_ARGUMENTS= 100;
	
	/** Error code indicating that the argument passed to resolve a variable is not of type java.lang.String */
	public static final int VARAIBLE_POOL_ARGUMENT_IS_NOT_A_STRING= 101;
	
	/** Error code indicating that a plugin providing a certain type extender isn't loaded yet */ 
	public static final int TYPE_EXTENDER_PLUGIN_NOT_LOADED= 200;
	
	/** Error indicating that a property referenced in a test expression can't be resolved */
	public static final int TYPE_EXTENDER_UNKOWN_METHOD= 201;
	
	/** Error indicating that the value returned from a type extender isn't of type boolean */
	public static final int TEST_EXPRESSION_NOT_A_BOOLEAN= 202;
	
	
	/**
	 * Creates a new evaluation exception.
	 * 
	 * @param errorCode the exception's error code
	 * @param message a human-readable message, localized to the current locale
	 */
	public ExpressionException(int errorCode, String message) {
		super(new Status(IStatus.ERROR, ExpressionPlugin.getPluginId(), errorCode, message, null));
	}
}
