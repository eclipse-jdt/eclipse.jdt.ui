package junit.swingui;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

import javax.swing.JTabbedPane;
import junit.framework.*;

/**
 * A TestRunView is shown as a page in a tabbed folder. It contributes the page
 * contents and can return the currently selected tests. A TestRunView is
 * notified about the start and finish of a run.
 */
interface TestRunView {
	/**
	 * Returns the currently selected Test in the View
	 */
	Test getSelectedTest();

	/**
	 * Activates the TestRunView
	 */
	void activate();

	/**
	 * Reveals the given failure
	 */
	void revealFailure(Test failure);

	/**
	 * Adds the TestRunView to the test run views tab
	 */
	void addTab(JTabbedPane pane);

	/**
	 * Informs that the suite is about to start
	 */
	void aboutToStart(Test suite, TestResult result);

	/**
	 * Informs that the run of the test suite has finished
	 */
	void runFinished(Test suite, TestResult result);
}
