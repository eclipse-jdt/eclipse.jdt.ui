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

import org.eclipse.core.runtime.IStatus;

/**
 * Represents status related to XML expressions and defines the relevant 
 * status code constants.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see org.eclipse.core.runtime.IStatus
 */ 
public interface IExpressionStatus extends IStatus {

	public static final int TYPE_CONVERSION_ERROR= 1;
	public static final int WRONG_NUMBER_OF_ARGUMENTS= 2;
	public static final int VARIABLE_IS_NOT_A_COLLECTION= 3;
	public static final int VARIABLE_IS_NOT_A_LIST= 4;
	public static final int VALUE_IS_NOT_AN_INTEGER= 5;
	public static final int WRONG_INDEX= 6;
	
	public static final int MISSING_ATTRIBUTE= 50;
	public static final int WRONG_ATTRIBUTE_VALUE= 51;
}
