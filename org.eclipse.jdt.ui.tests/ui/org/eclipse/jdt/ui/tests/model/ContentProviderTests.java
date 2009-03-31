/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.model;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import junit.framework.ComparisonFailure;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;

import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.util.CoreUtility;


public class ContentProviderTests extends TestCase {

	private IWorkspace fWorkspace;
	private IJavaProject fJProject1;
	private boolean fEnableAutoBuildAfterTesting;
	private IWorkbench fWorkbench;
	private IWorkbenchPage fPage;
	private MockPluginView fMyPart;
	private ITreeContentProvider fProvider;
	private IPackageFragment fPackageFragment1;
	private IPackageFragment fPackageFragment2;
	private IFile fFile1;

	public static Test suite() {
		return new TestSuite(ContentProviderTests.class);
	}

	protected void setUp() throws Exception {
		super.setUp();

		fWorkspace= ResourcesPlugin.getWorkspace();
		assertNotNull(fWorkspace);
		IWorkspaceDescription workspaceDesc= fWorkspace.getDescription();
		fEnableAutoBuildAfterTesting= workspaceDesc.isAutoBuilding();
		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(false);

		assertNotNull(fWorkspace);

		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");//$NON-NLS-1$//$NON-NLS-2$
		assertNotNull("project1 null", fJProject1);//$NON-NLS-1$
		// Use the project root as the classpath
		fJProject1.setRawClasspath(null, null);

		// Create some packages
		IProject project = (IProject)fJProject1.getResource();
		project.getFolder("f1").create(false, true, null);
		fPackageFragment1 = (IPackageFragment)JavaCore.create(project.getFolder("f1"));
		project.getFolder("f2").create(false, true, null);
		fPackageFragment2 = (IPackageFragment)JavaCore.create(project.getFolder("f2"));

		//Create a non-Java file in one of the packges
		fFile1 = project.getFile("f1/b");
		fFile1.create(new ByteArrayInputStream("".getBytes()), false, null);


		setUpMockView();
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(true);
		tearDownMockView();
		super.tearDown();
	}

	private void setUpMockView() throws CoreException {
		fWorkbench= PlatformUI.getWorkbench();
		assertNotNull(fWorkbench);

		fPage= fWorkbench.getActiveWorkbenchWindow().getActivePage();
		assertNotNull(fPage);

		//just testing to make sure my part can be created
		IViewPart myPart= new MockPluginView();
		assertNotNull(myPart);

		myPart= fPage.showView("org.eclipse.jdt.ui.tests.model.MockPluginView");//$NON-NLS-1$
		if (myPart instanceof MockPluginView) {
			fMyPart= (MockPluginView) myPart;
			fProvider= fMyPart.getContentProvider();
			fMyPart.setProject(fJProject1);
		}else assertTrue("Unable to get view",false);//$NON-NLS-1$

		assertNotNull(fProvider);
	}

	private void tearDownMockView() {
		fPage.hideView(fMyPart);
	}

	private static void assertEqualSets(String message, Object[] expected, Object[] actual) {
		List expList= Arrays.asList(expected);
		List actList= Arrays.asList(actual);
		
		LinkedHashSet exp= new LinkedHashSet(expList);
		LinkedHashSet act= new LinkedHashSet(actList);
		
		if (!exp.equals(act))
			throw new ComparisonFailure(message, expList.toString(), actList.toString());
	}

	public void testOutgoingDeletion148118() {
		IProject project = (IProject)fJProject1.getResource();
		fMyPart.addOutgoingDeletion(project, "f1/a");
		fMyPart.addOutgoingDeletion(project, "f2/a");
		fMyPart.addOutgoingDeletion(project, "f3/");

		// Children of project
		Object[] expectedChildren = new Object[] { fPackageFragment1,  fPackageFragment2, project.getFolder("f3/")};
		Object[] children = fProvider.getChildren(fJProject1);
		assertEqualSets("Expected children of project does not match actual children", expectedChildren, children);

		// Children of fragment 1
		expectedChildren = new Object[] { ((IFolder)fPackageFragment1.getResource()).getFile("a")};
		children = fProvider.getChildren(fPackageFragment1);
		assertEqualSets("Expected children of f1 does not match actual children", expectedChildren, children);

		// Children of fragment 2
		expectedChildren = new Object[] { ((IFolder)fPackageFragment2.getResource()).getFile("a")};
		children = fProvider.getChildren(fPackageFragment2);
		assertEqualSets("Expected children of f2 does not match actual children", expectedChildren, children);
	}

	public void testOutgoingChangeInNonPackage261198() throws Exception {
		IProject project = (IProject)fJProject1.getResource();
		
		IFolder f1= ((IFolder) fPackageFragment1.getResource());
		IFolder noPackage= f1.getFolder("no-package");
		noPackage.create(false, true, null);
		
		IFile textfile = noPackage.getFile("textfile.txt");
		textfile.create(new ByteArrayInputStream("Hi".getBytes()), false, null);		
		
		fMyPart.addOutgoingChange(project, "f1/no-package/textfile.txt");
		
		// Children of project
		Object[] expectedChildren = new Object[] { fPackageFragment1 };
		Object[] children = fProvider.getChildren(fJProject1);
		assertEqualSets("Expected children of project does not match actual children", expectedChildren, children);
		
		// Children of fragment 1
		expectedChildren = new Object[] { noPackage };
		children = fProvider.getChildren(fPackageFragment1);
		assertEqualSets("Expected children of f1 does not match actual children", expectedChildren, children);
		
		// Children of no-package
		expectedChildren = new Object[] { textfile };
		children = fProvider.getChildren(noPackage);
		assertEqualSets("Expected children of no-package does not match actual children", expectedChildren, children);
	}
	
	public void testOutgoingPackageDeletion269167() throws Exception {
		IProject project = (IProject)fJProject1.getResource();
		
		fMyPart.addOutgoingDeletion(project, "f3/");
		IFolder f3= project.getFolder("f3");
		
		IPackageFragment packageFragment3= (IPackageFragment)JavaCore.create(f3);
		LogicalPackage logicalPackage3= new LogicalPackage(packageFragment3);
		ResourceMapping resourceMapping= (ResourceMapping)logicalPackage3.getAdapter(ResourceMapping.class);
		ResourceTraversal[] traversals= resourceMapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, null);
		assertEquals(1, traversals.length);
		assertEqualSets("", new IResource[] { f3 }, traversals[0].getResources());
		assertEquals(IResource.DEPTH_ONE, traversals[0].getDepth());
		assertEquals(0, traversals[0].getFlags());
		
		// Children of project
		Object[] expectedChildren = new Object[] { f3 };
		Object[] children = fProvider.getChildren(fJProject1);
		assertEqualSets("Expected children of project does not match actual children", expectedChildren, children);
	}
	
	public void testIncomingAddition159884() {
		IProject project = (IProject)fJProject1.getResource();
		fMyPart.addIncomingAddition(project, "f1/newFolder/");
		fMyPart.addIncomingAddition(project, "f1/newFolder/a");


		// Children of project
		IFolder f1 = project.getFolder("f1");
		Object[] expectedChildren = new Object[] { f1 };
		Object[] children = fProvider.getChildren(fJProject1);
		assertEqualSets("Expected children of project does not match actual children", expectedChildren, children);

		IFolder addedFolder = project.getFolder("f1/newFolder");

		// Children of f1
		expectedChildren = new Object[] { addedFolder};
		children = fProvider.getChildren(f1);
		assertEqualSets("Expected children of f1 does not match actual children", expectedChildren, children);

		// Children of newFolder

		expectedChildren = new Object[] {addedFolder.getFile("a")};
		children = fProvider.getChildren(addedFolder);
		assertEqualSets("Expected children of new folder does not match actual children", expectedChildren, children);
	}

	public void testIncomingAddition159884Part2() {
		IProject project = (IProject)fJProject1.getResource();
		fMyPart.addIncomingAddition(project, "f1/newFolder/a");


		// Children of project
		IFolder f1 = project.getFolder("f1");
		Object[] expectedChildren = new Object[] { f1 };
		Object[] children = fProvider.getChildren(fJProject1);
		assertEqualSets("Expected children of project does not match actual children", expectedChildren, children);

		IFolder addedFolder = project.getFolder("f1/newFolder");

		// Children of f1
		expectedChildren = new Object[] { addedFolder};
		children = fProvider.getChildren(f1);
		assertEqualSets("Expected children of f1 does not match actual children", expectedChildren, children);

		// Children of newFolder

		expectedChildren = new Object[] {addedFolder.getFile("a")};
		children = fProvider.getChildren(addedFolder);
		assertEqualSets("Expected children of new folder does not match actual children", expectedChildren, children);
	}
}