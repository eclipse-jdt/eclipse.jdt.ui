/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.runner;

import java.io.PrintWriter;
import java.io.StringWriter;


public class DefaultClassifier implements IClassifiesThrowables {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jdt.internal.junit.runner.ThrowableClassifier#getTrace(java.lang.Throwable)
	 */
	public String getTrace(Throwable t) {
		StringWriter stringWriter= new StringWriter();
		PrintWriter writer= new PrintWriter(stringWriter);
		t.printStackTrace(writer);
		StringBuffer buffer= stringWriter.getBuffer();
		return buffer.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jdt.internal.junit.runner.ThrowableClassifier#isComparisonFailure(java.lang.Throwable)
	 */
	public boolean isComparisonFailure(Throwable throwable) {
		// avoid reference to comparison failure to avoid a dependency on 3.8.1 or 4.x
		String classname= throwable.getClass().getName();
		return classname.equals("junit.framework.ComparisonFailure") //$NON-NLS-1$
				|| classname.equals("org.junit.ComparisonFailure"); //$NON-NLS-1$
	}
}
