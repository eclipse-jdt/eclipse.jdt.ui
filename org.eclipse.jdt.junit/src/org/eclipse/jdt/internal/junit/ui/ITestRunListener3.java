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
package org.eclipse.jdt.internal.junit.ui;


/**
 * Extends ITestRunListener2 with a call back to trace the test contents
 */
public interface ITestRunListener3 extends ITestRunListener2 {

    public void testFailed(int status, String testId, String testName, String trace, String expected, String actual);
}
