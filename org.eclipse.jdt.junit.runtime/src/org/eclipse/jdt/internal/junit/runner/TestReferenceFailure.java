/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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




public class TestReferenceFailure {

	private final ITestIdentifier fTest;

	private final String fTrace;

	private final String fStatus;

	private FailedComparison fComparison;

	public TestReferenceFailure(ITestIdentifier ref, String status, String trace) {
		this(ref, status, trace, null);
	}

	public TestReferenceFailure(ITestIdentifier reference, String status,
			String trace, FailedComparison comparison) {
		fTest = reference;
		fStatus = status;
		fTrace = trace;
		fComparison = comparison;
	}

	public TestReferenceFailure(ITestReference reference, String status, String trace) { // not used
		this(reference.getIdentifier(), status, trace);
	}

	public String getStatus() {
		return fStatus;
	}

	public String getTrace() {
		return fTrace;
	}

	public ITestIdentifier getTest() {
		return fTest;
	}

	@Override
	public String toString() {
		return fStatus + " " + RemoteTestRunner.escapeText(fTest.getName())  //$NON-NLS-1$
			+ " " + RemoteTestRunner.escapeText(fTest.getParameterTypes()); //$NON-NLS-1$
	}

	public void setComparison(FailedComparison comparison) {
		fComparison = comparison;
	}

	public FailedComparison getComparison() {
		return fComparison;
	}
}
