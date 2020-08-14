/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.performance.views;

import org.junit.Test;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCaseCommon;

public class PackageExplorerWorkspaceWarmPerfTest extends JdtPerformanceTestCaseCommon {

//	private static class MyTestSetup extends TestSetup {
//
//		public MyTestSetup(Test test) {
//			super(test);
//		}
//
//		@Override
//		protected void setUp() throws Exception {
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
//		}
//
//		@Override
//		protected void tearDown() throws Exception {
//		}
//	}

	@Test
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