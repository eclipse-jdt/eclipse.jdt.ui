/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

/**
 * 
 */
package org.eclipse.jdt.internal.junit4.runner;

import junit.framework.ComparisonFailure;

import org.eclipse.jdt.internal.junit.runner.MessageIds;

public class FailureException {
	private final Throwable exception;

	public FailureException(Throwable exception) {
		this.exception = exception;
	}

	public String getStatus() {
		if (exception instanceof AssertionError)
			return MessageIds.TEST_FAILED;
		if (exception instanceof ComparisonFailure)
			return MessageIds.TEST_FAILED;
		return MessageIds.TEST_ERROR;
	}
}