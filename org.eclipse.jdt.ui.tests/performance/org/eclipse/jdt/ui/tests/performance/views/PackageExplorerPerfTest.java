/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.performance.views;

import java.io.File;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.performance.OSPerformanceMeterFactory;
import org.eclipse.jdt.ui.tests.performance.PerformanceMeter;
import org.eclipse.jdt.ui.tests.performance.PerformanceMeterFactory;

public class PackageExplorerPerfTest extends TestCase {

	private static class MyTestSetup extends TestSetup {
		public static final String SRC_CONTAINER= "src";
		
		public static IJavaProject fJProject1;
		public static IPackageFragmentRoot fJunitSrcRoot;
		
		public MyTestSetup(Test test) {
			super(test);
		}
		protected void setUp() throws Exception {
			fJProject1= JavaProjectHelper.createJavaProject("Testing", "bin");
			assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);
			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC);
			fJunitSrcRoot= JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive);
		}
		protected void tearDown() throws Exception {
			if (fJProject1 != null && fJProject1.exists())
				JavaProjectHelper.delete(fJProject1);
		}
	}
	
	private PerformanceMeterFactory fPerformanceMeterFactory= new OSPerformanceMeterFactory();
	
	public PackageExplorerPerfTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MyTestSetup(new TestSuite(PackageExplorerPerfTest.class));
	}

	public static Test setUpTest(Test someTest) {
		return new MyTestSetup(someTest);
	}
	
	public void testPackageExplorer() throws Exception {
		PerformanceMeter openMeter= fPerformanceMeterFactory.createPerformanceMeter(this, "open");
		IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		openMeter.start();
		PackageExplorerPart part= (PackageExplorerPart)page.showView(JavaUI.ID_PACKAGES);
		openMeter.stop();
		openMeter.commit();
		
		TreeViewer viewer= part.getTreeViewer();
		PerformanceMeter selectMeter= fPerformanceMeterFactory.createPerformanceMeter(this, "select");
		StructuredSelection selection= new StructuredSelection(MyTestSetup.fJProject1);
		selectMeter.start();
		viewer.setSelection(selection);
		selectMeter.stop();
		selectMeter.commit();
		
		PerformanceMeter expandMeter= fPerformanceMeterFactory.createPerformanceMeter(this, "expand");
		expandMeter.start();
		viewer.expandToLevel(MyTestSetup.fJProject1, 1);
		expandMeter.stop();
		expandMeter.commit();
	}	
}
