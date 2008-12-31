/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.performance.views;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;

public class PackageExplorerWarmPerfTest extends JdtPerformanceTestCase {

	public static Test setUpTest(Test someTest) {
		return new TestSetup(someTest);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite("PackageExplorerWarmPerfTest");
		suite.addTest(new PackageExplorerWarmPerfTest("testOpen"));
		return new TestSetup(suite);
	}

	public PackageExplorerWarmPerfTest(String name) {
		super(name);
	}

	public void testOpen() throws Exception {
		IWorkbenchWindow activeWorkbenchWindow= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page= activeWorkbenchWindow.getActivePage();
		page.close();
		page= activeWorkbenchWindow.openPage("org.eclipse.ui.resourcePerspective", ResourcesPlugin.getWorkspace().getRoot());
		joinBackgroudActivities();
		page.showView(JavaUI.ID_PACKAGES);
		for (int i = 0; i < 10; i++) {
			page.close();
			page= activeWorkbenchWindow.openPage("org.eclipse.ui.resourcePerspective", ResourcesPlugin.getWorkspace().getRoot());
			joinBackgroudActivities();
			startMeasuring();
			page.showView(JavaUI.ID_PACKAGES);
			stopMeasuring();
		}
		commitMeasurements();
		// don't spend more than 500 ms.
		Performance.getDefault().assertPerformanceInAbsoluteBand(fPerformanceMeter, Dimension.ELAPSED_PROCESS, 0, 500);
	}
}