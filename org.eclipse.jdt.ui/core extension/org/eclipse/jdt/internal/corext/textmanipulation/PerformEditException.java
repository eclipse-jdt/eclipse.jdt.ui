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
package org.eclipse.jdt.internal.corext.textmanipulation;

/**
 * Thrown to indicate that an edit can't be perform.
 */
public class PerformEditException extends Exception {

	private TextEdit fEdit;
	private Throwable fException;
	
	/**
	 * Constructs a new perform edit exception with the given
	 * edit and detail message.
	 * 
	 * @param edit the edit that can't be performed
	 * @param message the detail message
	 * @param exception the relevant low-level exception. Can be
	 *  <code>null</code>
	 */
	public PerformEditException(TextEdit edit, String message, Throwable exception) {
		super(message);
		fEdit= edit;
		fException= exception;
	}
}
