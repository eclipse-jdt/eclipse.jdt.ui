/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import org.eclipse.test.performance.Dimension;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;

public class TypeHierarchyPerfTest extends JdtPerformanceTestCase {

	private static class MyTestSetup extends TestSetup {
		public static final String SRC_CONTAINER= "src";

		public static IJavaProject fJProject1;
		public static IPackageFragmentRoot fJunitSrcRoot;

		public MyTestSetup(Test test) {
			super(test);
		}

		protected void setUp() throws Exception {
			fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
			// we must make sure that the performance test are compatible to 2.1.3 & 3.0 so use rt13
			assertTrue("rt not found", JavaProjectHelper.addRTJar13(fJProject1) != null);
			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			fJunitSrcRoot= JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		}

		protected void tearDown() throws Exception {
			if (fJProject1 != null && fJProject1.exists())
				JavaProjectHelper.delete(fJProject1);
		}
	}

	public static Test suite() {
		return new MyTestSetup(new TestSuite(TypeHierarchyPerfTest.class));
	}

	public static Test setUpTest(Test someTest) {
		return new MyTestSetup(someTest);
	}

	public void testOpenObjectHierarchy() throws Exception {
		//cold
		measureOpenHierarchy(MyTestSetup.fJProject1.findType("java.lang.Object"));
	}

	public void testOpenCollHierarchy() throws Exception {
		//junit source folder
		measureOpenHierarchy(MyTestSetup.fJunitSrcRoot);
	}

	public void testOpenObjectHierarchy2() throws Exception {
		//warm
		tagAsSummary("Open type hierarchy on Object", Dimension.ELAPSED_PROCESS);
		measureOpenHierarchy(MyTestSetup.fJProject1.findType("java.lang.Object"));
	}

	private void measureOpenHierarchy(IJavaElement element) throws Exception {
		joinBackgroudActivities();

		startMeasuring();

		OpenTypeHierarchyUtil.open(element, JavaPlugin.getActiveWorkbenchWindow());

		finishMeasurements();
	}

	protected void finishMeasurements() {
		stopMeasuring();
		commitMeasurements();
		assertPerformanceInRelativeBand(Dimension.ELAPSED_PROCESS, -100, +10);
	}
}
