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
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCaseCommon;

public class OrganizeImportsPerfTest extends JdtPerformanceTestCaseCommon {

	private static class MyTestSetup extends ExternalResource {
		public static final String SRC_CONTAINER= "src";

		public static IJavaProject fJProject1;

		@Override
		public void before() throws Throwable {
			fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
			assertNotNull("rt not found", JavaProjectHelper.addRTJar(fJProject1));
			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		}

		@Override
		public void after() {
			try {
				if (fJProject1 != null && fJProject1.exists()) {
					JavaProjectHelper.delete(fJProject1);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	@Rule
	public MyTestSetup stup= new MyTestSetup();

	private void addAllCUs(IJavaElement[] children, List<IJavaElement> result) throws JavaModelException {
		for (IJavaElement element : children) {
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
			result[i]= SharedASTProviderCore.getAST(cus[i], SharedASTProviderCore.WAIT_YES, new NullProgressMonitor());
		}
		return result;
	}

	@Test
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
			List<IJavaElement> cusList= new ArrayList<>();
			addAllCUs(MyTestSetup.fJProject1.getChildren(), cusList);
			ICompilationUnit[] cus= cusList.toArray(new ICompilationUnit[cusList.size()]);
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
