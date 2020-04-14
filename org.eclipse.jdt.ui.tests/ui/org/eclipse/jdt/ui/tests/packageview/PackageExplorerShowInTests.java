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

package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

public class PackageExplorerShowInTests {
	@Rule
	public ProjectTestSetup pts=new ProjectTestSetup();

	private IJavaProject fJProject;
	private PackageExplorerPart fPackageExplorer;
	private IWorkbenchPage fPage;

	@Before
	public void setUp() throws Exception {
		fJProject= pts.getProject();
		fPage= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		fPackageExplorer= (PackageExplorerPart) fPage.showView(JavaUI.ID_PACKAGES);
		fPackageExplorer.selectAndReveal(new StructuredSelection());
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject, pts.getDefaultClasspath());
		fPage.hideView(fPackageExplorer);
		fPage= null;
		fPackageExplorer= null;
		fJProject= null;
	}

////////////TODO: should also test non-UI part of tryToReveal(..) ///////////////////////////////


	@Test
	public void testCU() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");
		IPackageFragment pack= sourceFolder.createPackageFragment("p", true, null);
		ICompilationUnit cu= pack.createCompilationUnit("A.java", "package p;\nclass A {\n\n}", true, null);

		IStructuredSelection selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(cu));
		assertEquals(1, selection.size());
		assertEquals(cu, selection.getFirstElement());

		selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(pack));
		assertEquals(1, selection.size());
		assertEquals(pack, selection.getFirstElement());

		selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(sourceFolder));
		assertEquals(1, selection.size());
		assertEquals(sourceFolder, selection.getFirstElement());

		selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(fJProject));
		assertEquals(1, selection.size());
		assertEquals(fJProject, selection.getFirstElement());

		// check corresponding resources:
		selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(cu.getResource()));
		assertEquals(1, selection.size());
		assertEquals(cu, selection.getFirstElement());

		selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(pack.getResource()));
		assertEquals(1, selection.size());
		assertEquals(pack, selection.getFirstElement());

		selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(sourceFolder.getResource()));
		assertEquals(1, selection.size());
		assertEquals(sourceFolder, selection.getFirstElement());

		selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(fJProject.getProject()));
		assertEquals(1, selection.size());
		assertEquals(fJProject, selection.getFirstElement());
	}

	@Test
	public void testCUAdaptedCU() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");
		IPackageFragment pack= sourceFolder.createPackageFragment("p", true, null);
		final ICompilationUnit cu= pack.createCompilationUnit("A.java", "package p;\nclass A {\n\n}", true, null);

		IAdaptable adaptable= new IAdaptable() {
			@SuppressWarnings("unchecked")
			@Override
			public <T> T getAdapter(Class<T> adapter) {
				if (adapter == IJavaElement.class)
					return (T) cu;
				else
					return null;
			}
		};
		IStructuredSelection selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(adaptable));

		assertEquals(1, selection.size());
		assertEquals(cu, selection.getFirstElement());
	}


	@Test
	public void testCUAdaptedResource() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");
		IPackageFragment pack= sourceFolder.createPackageFragment("p", true, null);
		final ICompilationUnit cu= pack.createCompilationUnit("A.java", "package p;\nclass A {\n\n}", true, null);

		IAdaptable adaptable= new IAdaptable() {
			@SuppressWarnings("unchecked")
			@Override
			public <T> T getAdapter(Class<T> adapter) {
				if (adapter == IResource.class)
					return (T) cu.getResource();
				else
					return null;
			}
		};
		IStructuredSelection selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(adaptable));

		assertEquals(1, selection.size());
		assertEquals(cu, selection.getFirstElement());
	}

	@Test
	public void testCUNotOnClasspath() throws Exception {
		IFolder folder= fJProject.getProject().getFolder("folder");
		folder.create(true, true, null);
		IFile file= folder.getFile("A.java");
		file.create(new ByteArrayInputStream("package p;\nclass A {\n\n}".getBytes()), true, null);

		IStructuredSelection selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(file));
		assertEquals(1, selection.size());
		assertEquals(file, selection.getFirstElement());

		IEditorPart editor= EditorUtility.openInEditor(file);
		try {
			selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(file));
			assertEquals(1, selection.size());
			assertEquals(file, selection.getFirstElement());

			selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(JavaCore.create(file)));
			assertEquals(1, selection.size());
			assertEquals(file, selection.getFirstElement());

			selection= (IStructuredSelection) fPackageExplorer.convertSelection(new StructuredSelection(folder));
			assertEquals(1, selection.size());
			assertEquals(folder, selection.getFirstElement());
		} finally {
			editor.getSite().getPage().closeEditor(editor, false);
		}
	}
}
