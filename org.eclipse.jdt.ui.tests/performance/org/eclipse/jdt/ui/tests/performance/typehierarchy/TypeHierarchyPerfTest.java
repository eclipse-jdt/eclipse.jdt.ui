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

package org.eclipse.jdt.ui.tests.performance.typehierarchy;

import java.io.File;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.jdt.ui.tests.performance.OSPerformanceMeterFactory;
import org.eclipse.jdt.ui.tests.performance.PerformanceMeter;
import org.eclipse.jdt.ui.tests.performance.PerformanceMeterFactory;

public class TypeHierarchyPerfTest extends TestCase {

	private static class MyTestSetup extends TestSetup {
		public static final String SRC_CONTAINER= "src";
		
		public static IJavaProject fJProject1;
		public static IPackageFragmentRoot fJunitSrcRoot;
		
		public MyTestSetup(Test test) {
			super(test);
		}
		
		protected void setUp() throws Exception {
			fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
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
	
	public TypeHierarchyPerfTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MyTestSetup(new TestSuite(TypeHierarchyPerfTest.class));
	}

	/** See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=47316">Bug 47316</a>. */
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
		measureOpenHierarchy(MyTestSetup.fJProject1.findType("java.lang.Object"));
	}
	
	private void measureOpenHierarchy(IJavaElement element) {
		PerformanceMeter performanceMeter= fPerformanceMeterFactory.createPerformanceMeter(this);
		performanceMeter.start();
		
		OpenTypeHierarchyUtil.open(element, JavaPlugin.getActiveWorkbenchWindow());
		
		performanceMeter.stop();
		performanceMeter.commit();
	}

}
