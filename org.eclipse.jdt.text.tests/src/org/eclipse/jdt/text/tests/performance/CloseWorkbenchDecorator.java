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

package org.eclipse.jdt.text.tests.performance;

import junit.extensions.TestDecorator;
import junit.framework.Test;
import junit.framework.TestResult;

import org.eclipse.ui.PlatformUI;

/**
 * Closes the workbench at the end of the test run, if run from the <code>EclipseTestRunner</code>.
 */
public class CloseWorkbenchDecorator extends TestDecorator {

	public CloseWorkbenchDecorator(Test test) {
		super(test);
	}

	public void run(TestResult result) {
		super.run(result);
		
		/* 
		 * ensure the workbench state gets saved when running with the Automated Testing Framework
                 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=71362
		 * keep when bug gets fixed in order to run with 3.0 when new reference data is created
                 */
		StackTraceElement[] elements=  new Throwable().getStackTrace();
		for (int i= 0; i < elements.length; i++) {
			StackTraceElement element= elements[i];
			if (element.getClassName().equals("org.eclipse.test.EclipseTestRunner")) {
				PlatformUI.getWorkbench().close();
				break;
			}
		}
	}
}
