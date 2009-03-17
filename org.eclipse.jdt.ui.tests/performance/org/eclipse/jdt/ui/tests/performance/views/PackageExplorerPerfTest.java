/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.performance.views;

import java.io.File;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

public class PackageExplorerPerfTest extends JdtPerformanceTestCase {

	private static class MyTestSetup extends TestSetup {
		public static final String SRC_CONTAINER= "src";

		public static IJavaProject fJProject1;

		public MyTestSetup(Test test) {
			super(test);
		}
		protected void setUp() throws Exception {
			fJProject1= JavaProjectHelper.createJavaProject("Testing", "bin");
			// we must make sure that the performance test are compatible to 2.1.3 & 3.0 so use rt13
			assertTrue("rt not found", JavaProjectHelper.addRTJar13(fJProject1) != null);
			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		}
		protected void tearDown() throws Exception {
			if (fJProject1 != null && fJProject1.exists())
				JavaProjectHelper.delete(fJProject1);
		}
	}

	public static Test suite() {
		TestSuite suite= new TestSuite("PackageExplorerPerfTest");
		suite.addTest(new PackageExplorerPerfTest("testOpen"));
		suite.addTest(new PackageExplorerPerfTest("testSelect"));
		suite.addTest(new PackageExplorerPerfTest("testExpand"));
		return new MyTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new MyTestSetup(someTest);
	}

	public PackageExplorerPerfTest(String name) {
		super(name);
	}

	public void testOpen() throws Exception {
		IWorkbenchWindow activeWorkbenchWindow= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page= activeWorkbenchWindow.getActivePage();
		page.close();
		page= activeWorkbenchWindow.openPage("org.eclipse.ui.resourcePerspective", ResourcesPlugin.getWorkspace().getRoot());
		joinBackgroudActivities();
		startMeasuring();
		page.showView(JavaUI.ID_PACKAGES);
		finishMeasurements();
	}

	public void testSelect() throws Exception {
		joinBackgroudActivities();
		TreeViewer viewer= getViewer();
		StructuredSelection selection= new StructuredSelection(MyTestSetup.fJProject1);
		startMeasuring();
		viewer.setSelection(selection);
		finishMeasurements();
	}

	public void testExpand() throws Exception {
		joinBackgroudActivities();
		TreeViewer viewer= getViewer();
		startMeasuring();
		viewer.expandToLevel(MyTestSetup.fJProject1, 1);
		finishMeasurements();
	}

	private TreeViewer getViewer() {
		IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		return ((PackageExplorerPart)page.findView(JavaUI.ID_PACKAGES)).getTreeViewer();
	}
}
