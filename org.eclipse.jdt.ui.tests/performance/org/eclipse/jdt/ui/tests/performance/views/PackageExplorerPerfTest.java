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
import java.util.zip.ZipFile;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runners.MethodSorters;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCaseCommon;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PackageExplorerPerfTest extends JdtPerformanceTestCaseCommon {

	private static class MyTestSetup extends ExternalResource {
		public static final String SRC_CONTAINER= "src";

		public static IJavaProject fJProject1;

		@Override
		public void before() throws Throwable {
			fJProject1= JavaProjectHelper.createJavaProject("Testing", "bin");
			// we must make sure that the performance test are compatible to 2.1.3 & 3.0 so use rt13
			assertNotNull("rt not found", JavaProjectHelper.addRTJar13(fJProject1));
			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			JavaProjectHelper.addSourceContainerWithImport(fJProject1, SRC_CONTAINER, junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		}
		@Override
		public void after() {
			try {
				if (fJProject1 != null && fJProject1.exists())
					JavaProjectHelper.delete(fJProject1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Rule
	public MyTestSetup stup= new MyTestSetup();

	@Test
	public void testAOpen() throws Exception {
		IWorkbenchWindow activeWorkbenchWindow= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page= activeWorkbenchWindow.getActivePage();
		page.close();
		page= activeWorkbenchWindow.openPage("org.eclipse.ui.resourcePerspective", ResourcesPlugin.getWorkspace().getRoot());
		joinBackgroudActivities();
		startMeasuring();
		page.showView(JavaUI.ID_PACKAGES);
		finishMeasurements();
	}

	@Test
	public void testBSelect() throws Exception {
		joinBackgroudActivities();
		TreeViewer viewer= getViewer();
		StructuredSelection selection= new StructuredSelection(MyTestSetup.fJProject1);
		startMeasuring();
		viewer.setSelection(selection);
		finishMeasurements();
	}

	@Test
	public void testCExpand() throws Exception {
		joinBackgroudActivities();
		TreeViewer viewer= getViewer();
		startMeasuring();
		viewer.expandToLevel(MyTestSetup.fJProject1, 1);
		finishMeasurements();
	}

	// test for Bug 311212: [package explorer] Performance problem with refreshing external class folders
	@Test
	public void testDRefreshClassFolder() throws Throwable {
		// Import rtstubs a few times. Caveat: Only import class files, but not META-INF.
		// PackageExplorerContentProvider#processResourceDelta(..) refreshes the parent if a resource changes.
		// This is not what we want to test.
		final IJavaProject javaProject= MyTestSetup.fJProject1;
		File jreArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.RT_STUBS_15);
		IPackageFragmentRoot classFolder= JavaProjectHelper.addClassFolderWithImport(javaProject, "classes", null, null, jreArchive);
		((IFolder) classFolder.getResource()).getFolder("META-INF").delete(true, null);

		try (ZipFile zipFile= new ZipFile(jreArchive)) {
			for (int i= 1; i <= 5; i++) {
				String packName= "copy" + i;
				classFolder.createPackageFragment(packName, true, null);
				IFolder folder= javaProject.getProject().getFolder("classes").getFolder(packName);
				JavaProjectHelper.importFilesFromZip(zipFile, folder.getFullPath(), null);
				folder.getFolder("META-INF").delete(true, null);
			}
		}

		javaProject.getProject().getWorkspace().run((IWorkspaceRunnable) monitor -> javaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null), null);
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
		folder.accept(resource -> {
			if (resource instanceof IFile) {
				IFile file= (IFile) resource;
				file.getRawLocation().toFile().setLastModified(now);
			}
			return true;
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
