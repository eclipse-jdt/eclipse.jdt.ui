/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.ui.PlatformUI;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.Action;

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
	 */
	public RerunAction(TestRunnerViewPart runner, String testId, String className, String testName, String launchMode) {
		super(); 
		if (launchMode.equals(ILaunchManager.RUN_MODE))
			setText(JUnitMessages.RerunAction_label_run); 
		else if (launchMode.equals(ILaunchManager.DEBUG_MODE))
			setText(JUnitMessages.RerunAction_label_debug); 
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
