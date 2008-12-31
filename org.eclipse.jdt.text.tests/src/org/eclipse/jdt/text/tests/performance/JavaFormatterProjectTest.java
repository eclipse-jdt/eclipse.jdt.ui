/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;


import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IViewSite;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;



/**
 * Measures the time to format a Java project.
 *
 * @since 3.1
 */
public class JavaFormatterProjectTest extends TextPerformanceTestCase {

	private static final Class THIS= JavaFormatterProjectTest.class;

	private static final int WARM_UP_RUNS= 5;

	private static final int MEASURED_RUNS= 5;

	private static final String FORMAT_ACTION_ID= "org.eclipse.jdt.ui.actions.Format";

	private static final String FORMAT_DIALOG_ID= "FormatAll";

	public static Test suite() {
		return new DisableAutoBuildTestSetup(new TextPluginTestSetup(new TestSuite(THIS), EditorTestHelper.JAVA_PERSPECTIVE_ID));
	}

	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	/**
	 * Measures the time to convert line delimiters of a project.
	 *
	 * @throws Exception
	 */
	public void test() throws Exception {
		measure(getNullPerformanceMeter(), getWarmUpRuns());
		measure(createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measure(PerformanceMeter performanceMeter, int runs) throws Exception {
		IPackagesViewPart view= (IPackagesViewPart) EditorTestHelper.getActivePage().findViewReference(EditorTestHelper.PACKAGE_EXPLORER_VIEW_ID).getView(false);
		IAction action= ((IViewSite) view.getSite()).getActionBars().getGlobalActionHandler(FORMAT_ACTION_ID);
		boolean wasEnabled= EditorTestHelper.setDialogEnabled(FORMAT_DIALOG_ID, false);
		StructuredSelection selection= new StructuredSelection(JavaCore.create(ResourceTestHelper.getProject(TextPluginTestSetup.PROJECT)));
		for (int i= 0; i < runs; i++) {
			performanceMeter.start();
			((SelectionDispatchAction) action).run(selection);
			performanceMeter.stop();
			TextPluginTestSetup.createProjectFromZip();
		}
		EditorTestHelper.setDialogEnabled(FORMAT_DIALOG_ID, wasEnabled);
	}
}
