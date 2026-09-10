/*******************************************************************************
 * Copyright (c) 2006, 2026 IBM Corporation and others.
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

public class RerunExecutionListener extends FirstRunExecutionListener {
	// IDs assigned while reloading a rerun do not match the IDs of the original run.
	// Keep TEST_START/TEST_END suppressed, capture timing locally, and let
	// RemoteTestRunner emit the timing with the original test ID after TEST_RERAN.

	public RerunExecutionListener(MessageSender sender, TestIdMap ids) {
		super(sender, ids);
	}

	private static final String RERUN_TIMING_ID= "rerun"; //$NON-NLS-1$

	private String fStatus = RemoteTestRunner.RERAN_OK;
	private TestTiming fTiming;

	@Override
	public synchronized void notifyTestFailed(TestReferenceFailure failure) {
		sendFailure(failure, MessageIds.RTRACE_START, MessageIds.RTRACE_END);

		String status = failure.getStatus();
		if (MessageIds.TEST_FAILED.equals(status))
			fStatus = RemoteTestRunner.RERAN_FAILURE;
		else if (MessageIds.TEST_ERROR.equals(status))
			fStatus = RemoteTestRunner.RERAN_ERROR;
		else
			throw new IllegalArgumentException(status);
	}

	@Override
	public void notifyTestStarted(ITestIdentifier test) {
		startTiming(RERUN_TIMING_ID);
	}

	@Override
	public void notifyTestEnded(ITestIdentifier test) {
		fTiming= endTiming(RERUN_TIMING_ID);
	}

	public void sendTiming(String testId) {
		if (fTiming != null) {
			sendTiming(testId, fTiming);
			fSender.flush();
		}
	}

	public String getStatus() {
		return fStatus;
	}

}
