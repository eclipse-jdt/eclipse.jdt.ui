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
package org.eclipse.jdt.internal.corext.refactoring.changes;

/**
 * Class defining status codes used by refactoring core.
 * 
 * @see org.eclipse.core.runtime.Status#Status(int, java.lang.String, int, java.lang.String, java.lang.Throwable)
 * 
 * @since 3.0
 */
public class StatusCodes {
	
	private StatusCodes() {
		// no instance
	}
	
	/** 
	 * Constant (value 1) indicating that a bad location exception has 
	 * occured during change execution.
	 */ 
	public static final int BAD_LOCATION= 1;
}
