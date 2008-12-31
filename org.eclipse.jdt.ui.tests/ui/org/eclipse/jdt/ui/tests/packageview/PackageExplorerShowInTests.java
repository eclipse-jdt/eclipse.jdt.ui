/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.packageview;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

public class PackageExplorerShowInTests extends TestCase {
	private static final Class clazz= PackageExplorerShowInTests.class;

	public PackageExplorerShowInTests(String name) {
		super(name);
	}

	public static Test suite() {
		if (true) {
			return new ProjectTestSetup(new TestSuite(clazz));
		} else {
			System.err.println("*** Running only parts of " + clazz.getName() + "!");
			TestSuite suite= new TestSuite();
			suite.addTest(new PackageExplorerShowInTests("testXXX"));
			return new ProjectTestSetup(suite);
		}
	}

	public static Test setUpTest(Test someTest) {
		return new ProjectTestSetup(someTest);
	}

	private IJavaProject fJProject;
	private PackageExplorerPart fPackageExplorer;
	private IWorkbenchPage fPage;

	protected void setUp() throws Exception {
		fJProject= ProjectTestSetup.getProject();
		fPage= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		fPackageExplorer= (PackageExplorerPart) fPage.showView(JavaUI.ID_PACKAGES);
		fPackageExplorer.selectAndReveal(new StructuredSelection());
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject, ProjectTestSetup.getDefaultClasspath());
		fPage.hideView(fPackageExplorer);
		fPage= null;
		fPackageExplorer= null;
		fJProject= null;
	}

////////////TODO: should also test non-UI part of tryToReveal(..) ///////////////////////////////


	public void testCU() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");
		IPackageFragment pack= sourceFolder.createPackageFragment("p", true, null);
		ICompilationUnit cu= pack.createCompilationUnit("A.java", "package p;\nclass A {\n\n}", true, null);
		IStructuredSelection selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(cu));

		assertEquals(1, selection.size());
		assertEquals(cu, selection.getFirstElement());
	}

	public void testCUAdaptedCU() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");
		IPackageFragment pack= sourceFolder.createPackageFragment("p", true, null);
		final ICompilationUnit cu= pack.createCompilationUnit("A.java", "package p;\nclass A {\n\n}", true, null);

		IAdaptable adaptable= new IAdaptable() {
			public Object getAdapter(Class adapter) {
				if (adapter == IJavaElement.class)
					return cu;
				else
					return null;
			}
		};
		IStructuredSelection selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(adaptable));

		assertEquals(1, selection.size());
		assertEquals(cu, selection.getFirstElement());
	}


	public void testCUAdaptedResource() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");
		IPackageFragment pack= sourceFolder.createPackageFragment("p", true, null);
		final ICompilationUnit cu= pack.createCompilationUnit("A.java", "package p;\nclass A {\n\n}", true, null);

		IAdaptable adaptable= new IAdaptable() {
			public Object getAdapter(Class adapter) {
				if (adapter == IResource.class)
					return cu.getResource();
				else
					return null;
			}
		};
		IStructuredSelection selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(adaptable));

		assertEquals(1, selection.size());
		assertEquals(cu, selection.getFirstElement());
	}



}
