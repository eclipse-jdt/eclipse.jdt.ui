/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

public class FailedComparison {

	private final String fExpected;
	private final String fActual;

	public FailedComparison(String expected, String actual) {
		fExpected = expected;
		fActual = actual;
	}

	public String getActual() {
		return fActual;
	}

	public String getExpected() {
		return fExpected;
	}

	void sendMessages(MessageSender sender) {
		sender.sendMessage(MessageIds.EXPECTED_START);
		sender.sendMessage(getExpected());
		sender.sendMessage(MessageIds.EXPECTED_END);

		sender.sendMessage(MessageIds.ACTUAL_START);
		sender.sendMessage(getActual());
		sender.sendMessage(MessageIds.ACTUAL_END);
	}

}
