/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

import org.eclipse.debug.core.ILaunchManager;

/**
 * Requests to rerun a test.
 */
public class RerunAction extends Action {
	private String fTestId;
	private String fClassName;
	private String fTestName;
	private String fTestDisplayName;
	private TestRunnerViewPart fTestRunner;
	private String fUniqueId;
	private String fLaunchMode;

	/**
	 * Constructor for RerunAction.
	 * @param actionName the name of the action
	 * @param runner the JUnit view
	 * @param testId the test id
	 * @param className the class name containing the test
	 * @param testName the method to run or <code>null</code>
	 * @param testDisplayName the display name of the test to re-run or <code>null</code>
	 * @param uniqueId the unique ID of the test to re-run or <code>null</code>
	 * @param launchMode the launch mode
	 */
	public RerunAction(String actionName, TestRunnerViewPart runner, String testId, String className, String testName,
			String testDisplayName, String uniqueId, String launchMode) {
		super(actionName);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJUnitHelpContextIds.RERUN_ACTION);
		fTestRunner= runner;
		fTestId= testId;
		fClassName= className;
		fTestName= testName;
		fTestDisplayName= testDisplayName;
		fUniqueId= uniqueId;
		fLaunchMode= launchMode;
		if (ILaunchManager.RUN_MODE == launchMode) {
			setImageDescriptor(JUnitPlugin.getImageDescriptor("etool16/run_exc.png")); //$NON-NLS-1$
		} else if (ILaunchManager.DEBUG_MODE == launchMode) {
			setImageDescriptor(JUnitPlugin.getImageDescriptor("etool16/debug_exc.png")); //$NON-NLS-1$
		}
	}

	@Override
	public void run() {
		fTestRunner.rerunTest(fTestId, fClassName, fTestName, fTestDisplayName, fUniqueId, fLaunchMode);
	}
}
