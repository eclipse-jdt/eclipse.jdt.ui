/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jface.action.Action;

/**
 * Requests to rerun a test.
 */
public class RerunAction extends Action {
	private String fTestId;
	private String fClassName;
	private String fTestName;
	private TestRunnerViewPart fTestRunner;
	
	/**
	 * Constructor for RerunAction.
	 */
	public RerunAction(TestRunnerViewPart runner, String testId, String className, String testName) {
		super(JUnitMessages.getString("RerunAction.action.label"));  //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJUnitHelpContextIds.RERUN_ACTION);
		fTestRunner= runner;
		fTestId= testId;
		fClassName= className;
		fTestName= testName;
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		fTestRunner.rerunTest(fTestId, fClassName, fTestName);
	}
}
