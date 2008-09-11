/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

/**
 * Requests to rerun a test.
 */
public class RerunAction extends Action {
	private String fTestId;
	private String fClassName;
	private String fTestName;
	private TestRunnerViewPart fTestRunner;
	private String fLaunchMode;

	/**
	 * Constructor for RerunAction.
	 * @param actionName the name of the action
	 * @param runner the JUnit view
	 * @param testId the test id
	 * @param className the class name containing the test
	 * @param testName the method to run or <code>null</code>
	 * @param launchMode the launch mode
	 */
	public RerunAction(String actionName, TestRunnerViewPart runner, String testId, String className, String testName,
			String launchMode) {
		super(actionName);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJUnitHelpContextIds.RERUN_ACTION);
		fTestRunner= runner;
		fTestId= testId;
		fClassName= className;
		fTestName= testName;
		fLaunchMode= launchMode;
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		fTestRunner.rerunTest(fTestId, fClassName, fTestName, fLaunchMode);
	}
}
