/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jface.action.Action;

/**
 * Requests to rerun a test.
 */
public class RerunAction extends Action {
	private String fClassName;
	private String fTestName;
	private TestRunnerViewPart fTestRunner;
	
	/**
	 * Constructor for RerunAction.
	 */
	public RerunAction(TestRunnerViewPart runner, String className, String testName) {
		super(JUnitMessages.getString("RerunAction.action.label"));  //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJUnitHelpContextIds.RERUN_ACTION);
		fTestRunner= runner;
		fClassName= className;
		fTestName= testName;
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		fTestRunner.rerunTest(fClassName, fTestName);
	}
}
