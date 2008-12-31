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

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;

public class PackageExplorerWorkspaceWarmPerfTest extends JdtPerformanceTestCase {

	private static class MyTestSetup extends TestSetup {

		public MyTestSetup(Test test) {
			super(test);
		}

		protected void setUp() throws Exception {
//			IJavaModel model= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
//			if (model.getJavaProjects().length == 0) {
//				ExternalModelManager manager= PDECore.getDefault().getExternalModelManager();
//				IPluginModelBase[] allModels= manager.getAllModels();
//				PluginImportOperation op= new PluginImportOperation(allModels, PluginImportOperation.IMPORT_BINARY_WITH_LINKS, new PluginImportOperation.IReplaceQuery() {
//
//					public int doQuery(IProject project) {
//						return YES;
//					}
//				});
//				ResourcesPlugin.getWorkspace().run(op, new NullProgressMonitor());
//			}
		}

		protected void tearDown() throws Exception {
		}
	}

	public static Test setUpTest(Test someTest) {
		return new MyTestSetup(someTest);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite("PackageExplorerWorkspaceWarmPerfTest");
		suite.addTest(new PackageExplorerWorkspaceWarmPerfTest("testOpen"));
		return new MyTestSetup(suite);
	}

	public PackageExplorerWorkspaceWarmPerfTest(String name) {
		super(name);
	}

	public void testOpen() throws Exception {
		IWorkbenchWindow activeWorkbenchWindow= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page= activeWorkbenchWindow.getActivePage();
		page.close();
		page= activeWorkbenchWindow.openPage("org.eclipse.ui.resourcePerspective", ResourcesPlugin.getWorkspace().getRoot());
		joinBackgroudActivities();
		page.showView(JavaUI.ID_PACKAGES);
		page.close();
		page= activeWorkbenchWindow.openPage("org.eclipse.ui.resourcePerspective", ResourcesPlugin.getWorkspace().getRoot());
		startMeasuring();
		page.showView(JavaUI.ID_PACKAGES);
		finishMeasurements();
	}
}