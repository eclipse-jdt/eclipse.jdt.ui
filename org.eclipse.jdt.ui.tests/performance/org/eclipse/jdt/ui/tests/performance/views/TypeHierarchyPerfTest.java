/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runners.MethodSorters;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCaseCommon;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TypeHierarchyPerfTest extends JdtPerformanceTestCaseCommon {

	private static class MyTestSetup extends ExternalResource {
		public static final String SRC_CONTAINER= "src";

		public static IJavaProject fJProject1;
		public static IPackageFragmentRoot fJunitSrcRoot;

		@Override
		public void before() throws Throwable {
			fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
			// we must make sure that the performance test are compatible to 2.1.3 & 3.0 so use rt13
			assertNotNull("rt not found", JavaProjectHelper.addRTJar13(fJProject1));
			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			fJunitSrcRoot= JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		}

		@Override
		public void after() {
			try {
				if (fJProject1 != null && fJProject1.exists())
					JavaProjectHelper.delete(fJProject1);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	@Rule
	public MyTestSetup stup= new MyTestSetup();

	@Test
	public void testAOpenObjectHierarchy() throws Exception {
		//cold

		// make sure stuff like the Intro view gets closed and we start with a clean Java perspective:
		IWorkbenchWindow activeWorkbenchWindow= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page= activeWorkbenchWindow.getActivePage();
		page.close();
		page= activeWorkbenchWindow.openPage(JavaUI.ID_PERSPECTIVE, ResourcesPlugin.getWorkspace().getRoot());

		measureOpenHierarchy(MyTestSetup.fJProject1.findType("java.lang.Object"));
		Performance.getDefault().assertPerformanceInAbsoluteBand(fPerformanceMeter, Dimension.ELAPSED_PROCESS, 0, 2000);
	}

	@Test
	public void testBOpenCollHierarchy() throws Exception {
		//junit source folder
		measureOpenHierarchy(MyTestSetup.fJunitSrcRoot);
		Performance.getDefault().assertPerformanceInAbsoluteBand(fPerformanceMeter, Dimension.ELAPSED_PROCESS, 0, 1000);
	}

	@Test
	public void testCOpenObjectHierarchy2() throws Exception {
		//warm
		tagAsSummary("Open type hierarchy on Object", Dimension.ELAPSED_PROCESS);

		IJavaElement element= MyTestSetup.fJProject1.findType("java.lang.Object");
		IWorkbenchWindow workbenchWindow= JavaPlugin.getActiveWorkbenchWindow();

		TypeHierarchyViewPart viewPart= OpenTypeHierarchyUtil.open(element, workbenchWindow);

		for (int i= 0; i < 10; i++) {
			viewPart.setInputElement(MyTestSetup.fJProject1.findType("java.lang.String"));
			viewPart.getSite().getPage().hideView(viewPart);

			joinBackgroudActivities();
			startMeasuring();
			viewPart= OpenTypeHierarchyUtil.open(element, workbenchWindow);
			stopMeasuring();
		}

		commitMeasurements();
		assertPerformanceInRelativeBand(Dimension.ELAPSED_PROCESS, -100, +10);
	}

	private void measureOpenHierarchy(IJavaElement element) throws Exception {
		IWorkbenchWindow activeWorkbenchWindow= JavaPlugin.getActiveWorkbenchWindow();
		joinBackgroudActivities();

		startMeasuring();

		OpenTypeHierarchyUtil.open(element, activeWorkbenchWindow);

		stopMeasuring();
		commitMeasurements();
	}
}
