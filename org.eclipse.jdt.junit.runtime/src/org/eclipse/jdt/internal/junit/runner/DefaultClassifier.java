/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public String getTrace(Throwable t) {
		StringWriter stringWriter= new StringWriter();
		PrintWriter writer= new PrintWriter(stringWriter);
		t.printStackTrace(writer);
		StringBuffer buffer= stringWriter.getBuffer();
		return buffer.toString();
	}

	@Override
	public boolean isComparisonFailure(Throwable throwable) {
		// avoid reference to comparison failure to avoid a dependency on 3.8.1 or 4.x
		String classname= throwable.getClass().getName();
		return "junit.framework.ComparisonFailure".equals(classname) //$NON-NLS-1$
				|| "org.junit.ComparisonFailure".equals(classname); //$NON-NLS-1$
	}
}
