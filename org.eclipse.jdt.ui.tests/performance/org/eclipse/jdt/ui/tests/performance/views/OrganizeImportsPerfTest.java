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
import java.util.ArrayList;
import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;

import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;

public class OrganizeImportsPerfTest extends JdtPerformanceTestCase {

	private static class MyTestSetup extends TestSetup {
		public static final String SRC_CONTAINER= "src";

		public static IJavaProject fJProject1;

		public MyTestSetup(Test test) {
			super(test);
		}

		protected void setUp() throws Exception {
			fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
			assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);
			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		}

		protected void tearDown() throws Exception {
			if (fJProject1 != null && fJProject1.exists())
				JavaProjectHelper.delete(fJProject1);
		}
	}

	public static Test suite() {
		return new MyTestSetup(new TestSuite(OrganizeImportsPerfTest.class));
	}

	public static Test setUpTest(Test someTest) {
		return new MyTestSetup(someTest);
	}

	private void addAllCUs(IJavaElement[] children, List result) throws JavaModelException {
		for (int i= 0; i < children.length; i++) {
			IJavaElement element= children[i];
			if (element instanceof ICompilationUnit) {
				result.add(element);
			} else if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= ((IPackageFragmentRoot)element);
				addAllCUs(root.getChildren(), result);
			} else if (element instanceof IPackageFragment) {
				IPackageFragment pack= ((IPackageFragment)element);
				addAllCUs(pack.getChildren(), result);
			}
		}
	}

	private CompilationUnit[] createASTs(ICompilationUnit[] cus) {
		CompilationUnit[] result= new CompilationUnit[cus.length];
		for (int i= 0; i < cus.length; i++) {
			result[i]= SharedASTProvider.getAST(cus[i], SharedASTProvider.WAIT_YES, new NullProgressMonitor());
		}
		return result;
	}

	public void testOrganizeImport() throws Exception {
		measure(Performance.getDefault().getNullPerformanceMeter(), 10);
		measure(fPerformanceMeter, 10);
		
		// test is too short and hence the relative numbers spread too far (but still in an acceptable absolute band) 
//		tagAsSummary("Organize Imports", Dimension.ELAPSED_PROCESS);
		
		commitMeasurements();
		Performance.getDefault().assertPerformance(fPerformanceMeter);
	}

	private void measure(PerformanceMeter performanceMeter, int runs) throws Exception {
		for (int j= 0; j < runs; j++) {
			List cusList= new ArrayList();
			addAllCUs(MyTestSetup.fJProject1.getChildren(), cusList);
			ICompilationUnit[] cus= (ICompilationUnit[])cusList.toArray(new ICompilationUnit[cusList.size()]);
			CompilationUnit[] roots= createASTs(cus);

			joinBackgroudActivities();
			
			performanceMeter.start();
			for (int i= 0; i < roots.length; i++) {
				OrganizeImportsOperation op= new OrganizeImportsOperation(cus[i], roots[i], true, true, true, null);
				op.run(new NullProgressMonitor());
			}
			performanceMeter.stop();
		}
		
	}

}
