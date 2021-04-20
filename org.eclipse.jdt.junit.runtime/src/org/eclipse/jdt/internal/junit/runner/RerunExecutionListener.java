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

public class RerunExecutionListener extends FirstRunExecutionListener {
	// Don't send ids here, since they don't match the ids of the original run:
	// RemoteTestRunner#rerunTest(..) reloads Test, so ITestReferences are not equals(..).

	public RerunExecutionListener(MessageSender sender, TestIdMap ids) {
		super(sender, ids);
	}

	private String fStatus = RemoteTestRunner.RERAN_OK;

	@Override
	public void notifyTestFailed(TestReferenceFailure failure) {
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
		// do nothing
	}

	@Override
	public void notifyTestEnded(ITestIdentifier test) {
		// do nothing
	}

	public String getStatus() {
		return fStatus;
	}

}
