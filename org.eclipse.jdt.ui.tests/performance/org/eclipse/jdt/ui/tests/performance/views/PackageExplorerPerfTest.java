/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
import java.util.zip.ZipFile;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.test.OrderedTestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

public class PackageExplorerPerfTest extends JdtPerformanceTestCase {

	private static class MyTestSetup extends TestSetup {
		public static final String SRC_CONTAINER= "src";

		public static IJavaProject fJProject1;

		public MyTestSetup(Test test) {
			super(test);
		}
		protected void setUp() throws Exception {
			fJProject1= JavaProjectHelper.createJavaProject("Testing", "bin");
			// we must make sure that the performance test are compatible to 2.1.3 & 3.0 so use rt13
			assertTrue("rt not found", JavaProjectHelper.addRTJar13(fJProject1) != null);
			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		}
		protected void tearDown() throws Exception {
			if (fJProject1 != null && fJProject1.exists())
				JavaProjectHelper.delete(fJProject1);
		}
	}

	public static Test suite() {
		TestSuite suite= new OrderedTestSuite(PackageExplorerPerfTest.class, new String[] {
			"testOpen", "testSelect", "testExpand",
			"testRefreshClassFolder"
		});
		return new MyTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new MyTestSetup(someTest);
	}

	public PackageExplorerPerfTest(String name) {
		super(name);
	}

	public void testOpen() throws Exception {
		IWorkbenchWindow activeWorkbenchWindow= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page= activeWorkbenchWindow.getActivePage();
		page.close();
		page= activeWorkbenchWindow.openPage("org.eclipse.ui.resourcePerspective", ResourcesPlugin.getWorkspace().getRoot());
		joinBackgroudActivities();
		startMeasuring();
		page.showView(JavaUI.ID_PACKAGES);
		finishMeasurements();
	}

	public void testSelect() throws Exception {
		joinBackgroudActivities();
		TreeViewer viewer= getViewer();
		StructuredSelection selection= new StructuredSelection(MyTestSetup.fJProject1);
		startMeasuring();
		viewer.setSelection(selection);
		finishMeasurements();
	}

	public void testExpand() throws Exception {
		joinBackgroudActivities();
		TreeViewer viewer= getViewer();
		startMeasuring();
		viewer.expandToLevel(MyTestSetup.fJProject1, 1);
		finishMeasurements();
	}

	// test for Bug 311212: [package explorer] Performance problem with refreshing external class folders
	public void testRefreshClassFolder() throws Throwable {
		// Import rtstubs a few times. Caveat: Only import class files, but not META-INF.
		// PackageExplorerContentProvider#processResourceDelta(..) refreshes the parent if a resource changes.
		// This is not what we want to test.
		final IJavaProject javaProject= MyTestSetup.fJProject1;
		File jreArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.RT_STUBS_15);
		IPackageFragmentRoot classFolder= JavaProjectHelper.addClassFolderWithImport(javaProject, "classes", null, null, jreArchive);
		((IFolder) classFolder.getResource()).getFolder("META-INF").delete(true, null);
		
		ZipFile zipFile= new ZipFile(jreArchive);
		try {
			for (int i= 1; i <= 5; i++) {
				String packName= "copy" + i;
				classFolder.createPackageFragment(packName, true, null);
				IFolder folder= javaProject.getProject().getFolder("classes").getFolder(packName);
				JavaProjectHelper.importFilesFromZip(zipFile, folder.getFullPath(), null);
				folder.getFolder("META-INF").delete(true, null);
			}
		} finally {
			zipFile.close();
		}
		
		javaProject.getProject().getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				javaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			}
		}, null);
		getViewer().expandToLevel(classFolder, 1);
		
		PackageExplorerPart view= getView();
		view.selectAndReveal(classFolder); // runs pending updates
		
		joinBackgroudActivities();
		touchAllFilesOnDisk((IFolder) classFolder.getResource());
		
		
		startMeasuring();
		javaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		view.selectAndReveal(classFolder); // runs pending updates
		finishMeasurements();
	}
	
	private void touchAllFilesOnDisk(IFolder folder) throws CoreException {
		final long now= System.currentTimeMillis();
		folder.accept(new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if (resource instanceof IFile) {
					IFile file= (IFile) resource;
					file.getRawLocation().toFile().setLastModified(now);
				}
				return true;
			}
		});
	}

	private TreeViewer getViewer() {
		return getView().getTreeViewer();
	}

	private PackageExplorerPart getView() {
		IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		return (PackageExplorerPart)page.findView(JavaUI.ID_PACKAGES);
	}
}
