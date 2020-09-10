/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.JavaElementDelta;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.LibraryContainer;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jdt.internal.ui.util.CoreUtility;


/**
 * Tests for the PackageExplorerContentProvider.
 * <ul>
 * <li>Bug 357450: Class folder in Java project have refresh problem</li>
 * </ul>
 *
 * @since 3.9
 */
public class ContentProviderTests6 {
	private boolean fEnableAutoBuildAfterTesting;

	private IWorkbenchPage page;
	private MockPluginView fMyPart;

	private ITreeContentProvider fProvider;

	private IJavaProject fJProject;
	private IPackageFragmentRoot classFolder;

	@Test
	public void testAddFileToClassFolder() throws Exception {
		IFile file= ((IFolder)classFolder.getResource()).getFile("testFile.class"); //$NON-NLS-1$
		if (!file.exists()) {
			file.create(new ByteArrayInputStream(new byte[] {}), false, null);
		}

		//send a delta indicating file added
		JavaElementDelta delta= new JavaElementDelta(classFolder.getJavaModel());
		delta.added(JavaCore.create(file));
		IElementChangedListener listener= (IElementChangedListener)fProvider;
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
		}

		assertFalse("No add happened", fMyPart.hasAddHappened()); //$NON-NLS-1$
		assertions();
	}

	@Test
	public void testAddFolderToClassFolder() throws Exception {
		IFolder folder= ((IFolder)classFolder.getResource()).getFolder("testFolder"); //$NON-NLS-1$
		if (!folder.exists()) {
			folder.create(false, true, null);
		}

		//send a delta indicating folder added
		JavaElementDelta delta= new JavaElementDelta(classFolder.getJavaModel());
		delta.added(JavaCore.create(folder));
		IElementChangedListener listener= (IElementChangedListener)fProvider;
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
		}

		assertFalse("No add happened", fMyPart.hasAddHappened()); //$NON-NLS-1$
		assertions();
	}

	@Test
	public void testRemoveFileFromClassFolder() throws Exception {
		IFile file= ((IFolder)classFolder.getResource()).getFile("testFile.class"); //$NON-NLS-1$
		if (!file.exists()) {
			file.create(new ByteArrayInputStream(new byte[] {}), false, null);
		}
		file.delete(false, null);

		//send a delta indicating file removed
		JavaElementDelta delta= new JavaElementDelta(classFolder.getJavaModel());
		delta.removed(JavaCore.create(file));
		IElementChangedListener listener= (IElementChangedListener)fProvider;
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
		}

		assertFalse("No remove happened", fMyPart.hasRemoveHappened()); //$NON-NLS-1$
		assertions();
	}

	@Test
	public void testRemoveFolderFromClassFolder() throws Exception {
		IFolder folder= ((IFolder)classFolder.getResource()).getFolder("testFolder"); //$NON-NLS-1$
		if (!folder.exists()) {
			folder.create(false, true, null);
		}
		folder.delete(false, null);

		//send a delta indicating folder deleted
		JavaElementDelta delta= new JavaElementDelta(classFolder.getJavaModel());
		delta.removed(JavaCore.create(folder));
		IElementChangedListener listener= (IElementChangedListener)fProvider;
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
		}

		assertFalse("No remove happened", fMyPart.hasRemoveHappened()); //$NON-NLS-1$
		assertions();
	}

	@Test
	public void testChangeClassInProject() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", "hello", false, null);

		//send a delta indicating file changed
		JavaElementDelta delta= new JavaElementDelta(sourceFolder.getJavaModel());
		delta.changed(cu, IJavaElementDelta.F_CHILDREN);
		IElementChangedListener listener= (IElementChangedListener)fProvider;
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
		}

		assertTrue("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertFalse("Project not refreshed", fMyPart.wasObjectRefreshed(fJProject)); //$NON-NLS-1$
	}

	private void assertions() {
		assertTrue("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertTrue("LibraryContainer Refreshed", fMyPart.wasObjectRefreshed(new LibraryContainer(fJProject))); //$NON-NLS-1$
		assertTrue("Resource Refreshed", fMyPart.wasObjectRefreshed(classFolder.getResource())); //$NON-NLS-1$
		assertEquals("Number of refreshed objects", 2, fMyPart.getRefreshedObject().size()); //$NON-NLS-1$
	}

	/*
	 * @see TestCase#setUp()
	 */
	@Before
	public void setUp() throws Exception {
		IWorkspaceDescription workspaceDesc= ResourcesPlugin.getWorkspace().getDescription();
		fEnableAutoBuildAfterTesting= workspaceDesc.isAutoBuilding();
		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(false);

		fJProject= JavaProjectHelper.createJavaProject("TestProject", "bin");//$NON-NLS-1$//$NON-NLS-2$
		assertNotNull("project null", fJProject);//$NON-NLS-1$

		//set up project : Add classFolder
		classFolder= JavaProjectHelper.addClassFolder(fJProject, "classFolder", null, null); //$NON-NLS-1$
		assertNotNull("class folder null", classFolder); //$NON-NLS-1$

		//set up the mock view
		setUpMockView();
	}

	public void setUpMockView() throws Exception{
		page= JavaPlugin.getActivePage();
		assertNotNull(page);

		IViewPart myPart= page.showView("org.eclipse.jdt.ui.tests.packageview.MockPluginView"); //$NON-NLS-1$
		if (myPart instanceof MockPluginView) {
			fMyPart= (MockPluginView)myPart;
			fMyPart.clear();
			fProvider= (ITreeContentProvider)fMyPart.getTreeViewer().getContentProvider();
			((PackageExplorerContentProvider)fProvider).setShowLibrariesNode(true);
		} else {
			fail("Unable to get view");//$NON-NLS-1$
		}
		assertNotNull(fProvider);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject);
		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(true);
	}
}