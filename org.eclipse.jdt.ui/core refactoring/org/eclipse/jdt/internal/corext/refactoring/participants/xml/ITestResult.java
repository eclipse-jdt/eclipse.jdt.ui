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

/**
 * Interface to define the three states <code>FALSE</code>,
 * <code>TRUE</code> and <code>NOT_LOADED</code>.
 * 
 * TODO some mathematical background.
 */
public interface ITestResult {

	/**
	 * Constants indicating the <code>false</code> result
	 */
	public static final int FALSE= 0;
	
	/** 
	 * Constants indicating the <code>true</code> result
	 */
	public static final int TRUE= 1;
	
	/** 
	 * An expression couldn't be evaluated since the plug-in 
	 * providing the property tester isn't loaded yet.
	 */ 
	public static final int NOT_LOADED= 2;
	
}
