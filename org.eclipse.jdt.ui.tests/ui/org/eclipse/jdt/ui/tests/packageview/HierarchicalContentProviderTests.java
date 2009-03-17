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

package org.eclipse.jdt.ui.tests.packageview;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;


public class HierarchicalContentProviderTests extends TestCase {

	private static class MyTestSetup extends TestSetup {

		public static IJavaProject fJProject;
		public static IPackageFragmentRoot fJAR;
		public static IPackageFragmentRoot fClassfolder;

		public static List fExpectedInJAR, fExpectedInCF;

		public MyTestSetup(Test test) {
			super(test);
		}
		protected void setUp() throws Exception {
			fJProject= JavaProjectHelper.createJavaProject("Testing", "bin");
			JavaProjectHelper.addRTJar(fJProject);

			File jreArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.RT_STUBS_15);

			fJAR= JavaProjectHelper.addLibrary(fJProject, new Path(jreArchive.getPath()));
			fClassfolder= JavaProjectHelper.addClassFolderWithImport(fJProject, "jre", null, null, jreArchive);

			// access JAR to avoid measuring JAR initialization
			fExpectedInJAR= getExpected(fJAR);
			fExpectedInCF= getExpected(fClassfolder);
		}

		private static List getExpected(IPackageFragmentRoot root) throws JavaModelException {
			ArrayList res= new ArrayList();
			IJavaElement[] packages= root.getChildren();
			for (int i= 0; i < packages.length; i++) {
				IJavaElement[] files= ((IPackageFragment) packages[i]).getChildren();
				for (int j= 0; j < files.length; j++) {
					testAndAdd(files[j], res);
				}
			}
			return res;
		}

		protected void tearDown() throws Exception {
			JavaProjectHelper.delete(fJProject);
			fExpectedInJAR= null;
			fExpectedInCF= null;
		}
	}


	private static final Class THIS= HierarchicalContentProviderTests.class;

	public HierarchicalContentProviderTests(String name) {
		super(name);
	}

	public static Test allTests() {
		return new MyTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		return allTests();
	}

	public static Test setUpTest(Test test) {
		return new MyTestSetup(test);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}


	private static void testAndAdd(Object curr, List res) {
		if (curr instanceof ICompilationUnit || curr instanceof IClassFile) {
			IJavaElement par= (IJavaElement) curr;
			res.add(par.getParent().getElementName() + '/' + par.getElementName());
		}
	}

	public void testJARHierarchical() {
		List res= collectChildren(MyTestSetup.fJAR, false, false);
		assertEquals(MyTestSetup.fExpectedInJAR, res);
	}

	public void testJARFoldedHierarchical() {
		List res= collectChildren(MyTestSetup.fJAR, true, false);
		assertEquals(MyTestSetup.fExpectedInJAR, res);
	}

	public void testClassFolderHierarchical() {
		List res= collectChildren(MyTestSetup.fClassfolder, false, false);
		assertEquals(MyTestSetup.fExpectedInCF, res);
	}

	public void testClassFolderFoldedHierarchical() {
		List res= collectChildren(MyTestSetup.fClassfolder, true, false);
		assertEquals(MyTestSetup.fExpectedInCF, res);
	}




	private void assertEquals(List expected, List current) {
		assertEquals(getString(expected), getString(current));
	}


	private String getString(List list) {
		Collections.sort(list, Collator.getInstance());
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < list.size(); i++) {
			buf.append(list.get(i)).append('\n');
		}
		return buf.toString();
	}


	private List collectChildren(Object elem, boolean fold, boolean flatLayout) {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER, fold);

		ArrayList result= new ArrayList();

		PackageExplorerContentProvider provider= new PackageExplorerContentProvider(false);
		provider.setIsFlatLayout(flatLayout);

		collectChildren(provider, elem, result);
		return result;
	}


	private void collectChildren(PackageExplorerContentProvider provider, Object elem, List result) {
		Object[] children= provider.getChildren(elem);
		for (int i= 0; i < children.length; i++) {
			Object curr= children[i];
			testAndAdd(curr, result);
			if (curr instanceof IPackageFragment) {
				collectChildren(provider, curr, result);
			}
		}
	}
}
