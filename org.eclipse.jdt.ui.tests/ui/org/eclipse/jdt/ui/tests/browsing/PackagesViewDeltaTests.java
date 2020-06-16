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
package org.eclipse.jdt.ui.tests.browsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

public class PackagesViewDeltaTests {
	private IJavaProject fJProject;

	private IPackageFragmentRoot fRoot1;
	private IWorkspace fWorkspace;
	private IWorkbench fWorkbench;
	private MockPluginView fMyPart;

	private ITreeContentProvider fProvider;

	private IWorkbenchPage fPage;
	private IPackageFragmentRoot fRoot2;
	private IPackageFragment fPack12;
	private IPackageFragment fPack32;
	private IPackageFragment fPack42;
	private IPackageFragment fPack52;
	private IPackageFragment fPack62;
	private IPackageFragment fPack21;
	private IPackageFragment fPack61;
	private IPackageFragment fPack51;
	private IPackageFragment fPack41;
	private IPackageFragment fPack31;
	private IPackageFragment fPack81;
	private IPackageFragment fPack91;
	private IPackageFragmentRoot fInternalJarRoot;
	private IPackageFragment fInternalPack3;
	private IPackageFragment fInternalPack4;
	private IPackageFragment fInternalPack5;
	private IPackageFragment fInternalPack10;
	private IPackageFragment fInternalPack6;
	private IPackageFragment fPack102;
	private boolean fEnableAutoBuildAfterTesting;

	//-----------------Remove delta test cases------------------

	@Test
	public void testRemoveTopLevelFragmentNotLogicalPackage() throws Exception {
		// Create a logical package for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		// Initialize Map
		fMyPart.fViewer.setInput(fJProject);
		fProvider.getChildren(cp3);

		fMyPart.clear();

		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.REMOVED, fPack12);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		// Force events from display
		fMyPart.pushDisplay();

		assertTrue("Remove happened", fMyPart.hasRemoveHappened()); //$NON-NLS-1$
		assertTrue("Correct package removed", fMyPart.getRemovedObject().contains(fPack12)); //$NON-NLS-1$
	}

	@Test
	public void testRemoveBottomLevelFragmentNotLogicalPackage() throws Exception {
		// Initialize Map
		fMyPart.fViewer.setInput(fJProject);

		fMyPart.clear();

		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.REMOVED, fPack42);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		// Force events from display
		fMyPart.pushDisplay();

		assertFalse("No remove happened, in Logical Package", fMyPart.hasRemoveHappened()); //$NON-NLS-1$
	}

	//This is a bogus test because this situation could never occur
	//while fPack42 exists you cannot remove fPack32 still it tests
	//correct delta handling.
	@Test
	public void testRemoveFragmentInMultiFragmentLogicalPackage() throws Exception {
		// Initialize the map
		fMyPart.fViewer.setInput(fJProject);
		for (Object object : fProvider.getChildren(fJProject)) {
			fProvider.getChildren(object);
		}

		// Create a logical package with name "pack3"
		LogicalPackage expectedParent= new LogicalPackage(fPack31);
		expectedParent.add(fInternalPack3);

		// Create a logical package with name "pack3.pack4"
		LogicalPackage ChildCp1= new LogicalPackage(fPack41);
		ChildCp1.add(fPack42);
		ChildCp1.add(fInternalPack4);

		fMyPart.clear();

		// Delete a fragment
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.REMOVED, fPack32);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		// Force events from display
		fMyPart.pushDisplay();

		// Assert remove happened
		assertFalse("Remove happened", fMyPart.hasRemoveHappened()); //$NON-NLS-1$
		assertFalse("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$

		// Test life cycle of Logical Package
		Object parent= fProvider.getParent(ChildCp1);
		assertTrue("wrong parent found for logical package after remove", parent instanceof LogicalPackage);

		LogicalPackage lp= (LogicalPackage) parent;
		assertEquals("PackageFragment removed from logical package", expectedParent, lp); //$NON-NLS-1$
	}

	@Test
	public void testRemoveBottomLevelFragmentInMultiFragmentLogicalPackage() throws Exception {
		// Delete a fragment
		fPack62.delete(true, null);

		// Create a logical package child of cp with name "pack3.pack5.pack6"
		LogicalPackage expectedChild= new LogicalPackage(fPack61);
		expectedChild.add(fInternalPack6);

		// Create a logical package child of cp with name "pack3.pack5"
		LogicalPackage ParentCp5= new LogicalPackage(fPack51);
		ParentCp5.add(fPack52);
		ParentCp5.add(fInternalPack5);

		// Create a logical package for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		// Initialize map
		fProvider.getChildren(fJProject);
		fProvider.getChildren(cp3);
		fProvider.getChildren(ParentCp5);

		fMyPart.clear();

		// Send a delta indicating fragment deleted
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.REMOVED, fPack62);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		// Force events from display
		fMyPart.pushDisplay();

		// Assert delta correct (no remove or refresh, only change to logicalpackage)
		assertFalse("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertFalse("Refresh happened", fMyPart.hasRemoveHappened()); //$NON-NLS-1$

		// Test life cycle of LogicalPackage
		Object[] child= fProvider.getChildren(ParentCp5);

		assertTrue("wrong parent found for logical package after remove", (child.length == 1) && (child[0] instanceof LogicalPackage)); //$NON-NLS-1$

		LogicalPackage lp= (LogicalPackage) child[0];
		assertEquals("PackageFragment removed from logical package", expectedChild, lp); //$NON-NLS-1$
	}

	@Test
	public void testRemoveFragmentInTwoFragmentLogicalPackage() throws Exception {
		// Create a logical package child of cp
		LogicalPackage ParentCp4= new LogicalPackage(fPack41);
		ParentCp4.add(fPack42);
		ParentCp4.add(fInternalPack4);

		// Create a logical package for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		// Initialize map
		fProvider.getChildren(fJProject);
		fProvider.getChildren(cp3);
		fProvider.getChildren(ParentCp4);

		// Create logical package with name "pack3.pack4.pack10"
		LogicalPackage cp10= new LogicalPackage(fInternalPack10);

		// Delete fragment
		fPack102.delete(true, null);

		fMyPart.clear();

		// Send a delta indicating fragment deleted
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.REMOVED, fPack102);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		fMyPart.pushDisplay();

		// Assert remove happened (delta worked)
		assertTrue("Refresh happened", fMyPart.hasRemoveHappened()); //$NON-NLS-1$
		assertTrue("Refresh happened", fMyPart.hasAddHappened()); //$NON-NLS-1$
		Object addedObject= fMyPart.getAddedObject().get(0);
		Object removedObject= fMyPart.getRemovedObject().get(0);
		assertEquals("Correct guy removed", cp10, removedObject); //$NON-NLS-1$
		assertEquals("Correct guy added", fInternalPack10, addedObject); //$NON-NLS-1$

		// Assert correct children gotten
		Object[] children= fProvider.getChildren(ParentCp4);
		assertTrue("PackageFragment removed from logial package", compareArrays(children, new Object[] { fPack91, fInternalPack10 })); //$NON-NLS-1$
	}

	//-----------------------Add delta test cases----------------------------------
	@Test
	public void testAddTopLevelFragmentNotLogicalPackage() throws Exception {
		// Initialize Map
		fMyPart.fViewer.setInput(fJProject);

		fMyPart.clear();

		IPackageFragment test= fRoot1.createPackageFragment("pack3.test", true, null); //$NON-NLS-1$
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.ADDED, test);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		// Force events from display
		fMyPart.pushDisplay();

		assertTrue("Add happened", fMyPart.hasAddHappened()); //$NON-NLS-1$
		assertTrue("Correct package added", fMyPart.getAddedObject().contains(test)); //$NON-NLS-1$
	}

	@Test
	public void testAddFragmentToLogicalPackage() throws Exception {
		// Create a logical package with name "pack3.pack4"
		LogicalPackage cp4= new LogicalPackage(fPack41);
		cp4.add(fPack42);
		cp4.add(fInternalPack4);

		// Initialize Map
		fProvider.getChildren(cp4);

		// Send delta
		IPackageFragment pack101= fRoot1.createPackageFragment("pack3.pack4.pack10", true, null); //$NON-NLS-1$
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.ADDED, pack101);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		// Force events from display
		fMyPart.pushDisplay();

		// Make sure no refresh happened
		assertFalse("Refresh did not happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
	}

	@Test
	public void testAddCUFromFragmentNotLogicalPackageVisible() throws Exception {
		// Create a logical package for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		ICompilationUnit cu= fPack81.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$

		// Initialize Map
		fMyPart.fViewer.setInput(fJProject);
		fProvider.getChildren(cp3);

		fMyPart.fViewer.reveal(fPack81);

		fMyPart.clear();

		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createCUDelta(new ICompilationUnit[] { cu }, fPack81, IJavaElementDelta.ADDED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		// Force events from display
		fMyPart.pushDisplay();

		assertTrue("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertTrue("Correct package refreshed", fMyPart.getRefreshedObject().contains(fPack81)); //$NON-NLS-1$
		assertEquals("Correct number of refreshes", 1, fMyPart.getRefreshedObject().size());//$NON-NLS-1$
	}

	@Test
	public void testAddCUFromFragmentNotLogicalPackageNotVisible() throws Exception {
		// Create a logical package for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		ICompilationUnit cu= fPack81.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$

		// Initialize Map
		fMyPart.fViewer.setInput(fJProject);
		fProvider.getChildren(cp3);

		fMyPart.clear();

		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createCUDelta(new ICompilationUnit[] { cu }, fPack81, IJavaElementDelta.ADDED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		// Force events from display
		fMyPart.pushDisplay();

		assertTrue("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertTrue("Correct package refreshed", fMyPart.getRefreshedObject().contains(cp3)); //$NON-NLS-1$
		assertEquals("Correct number of refreshes", 1, fMyPart.getRefreshedObject().size()); //$NON-NLS-1$
	}

	@Test
	public void testRemoveCUFromFragmentNotLogicalPackage() throws Exception {
		// Create a logical package for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		// Initialize Map
		fMyPart.fViewer.setInput(fJProject);
		fProvider.getChildren(cp3);

		ICompilationUnit cu= fPack81.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$

		// Make sure parent is visible
		fMyPart.fViewer.setInput(fJProject);
		fMyPart.fViewer.reveal(fPack81);

		fMyPart.clear();

		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createCUDelta(new ICompilationUnit[] { cu }, fPack81, IJavaElementDelta.REMOVED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		// Force events from display
		fMyPart.pushDisplay();

		assertTrue("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertTrue("Correct package refreshed", fMyPart.getRefreshedObject().contains(fPack81)); //$NON-NLS-1$
		assertEquals("Correct number of refreshes", 1, fMyPart.getRefreshedObject().size());//$NON-NLS-1$
	}

	@Test
	public void testRemoveCUFromFragmentNotLogicalPackageWithParentNotVisible() throws Exception {
		// Create a logical package for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		// Initialize Map
		fMyPart.fViewer.setInput(fJProject);
		fProvider.getChildren(cp3);

		ICompilationUnit cu= fPack81.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$

		fMyPart.clear();

		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createCUDelta(new ICompilationUnit[] { cu }, fPack81, IJavaElementDelta.REMOVED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		// Force events from display
		fMyPart.pushDisplay();

		assertTrue("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertTrue("Correct package refreshed", fMyPart.getRefreshedObject().contains(cp3)); //$NON-NLS-1$
		assertEquals("Correct number of refreshes", 1, fMyPart.getRefreshedObject().size());//$NON-NLS-1$
	}

	@Test
	public void testAddBottomLevelFragmentNotLogicalPackage() throws Exception {
		// Create a logical package for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		// Initialize Map
		fMyPart.fViewer.setInput(fJProject);
		fProvider.getChildren(cp3);

		fMyPart.clear();

		IPackageFragment test= fRoot1.createPackageFragment("pack3.pack5.test", true, null); //$NON-NLS-1$
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.ADDED, test);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		// Force events from display
		fMyPart.pushDisplay();

		assertTrue("Add happened", fMyPart.hasAddHappened()); //$NON-NLS-1$
		assertTrue("Corrent package added", fMyPart.getAddedObject().contains(test)); //$NON-NLS-1$
	}

	/*
	 * @see TestCase#setUp()
	 */
	@Before
	public void setUp() throws Exception {
		fWorkspace= ResourcesPlugin.getWorkspace();
		assertNotNull(fWorkspace);

		IWorkspaceDescription workspaceDesc= fWorkspace.getDescription();
		fEnableAutoBuildAfterTesting= workspaceDesc.isAutoBuilding();
		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(false);

		//------------set up project ------------------------------
		fJProject= JavaProjectHelper.createJavaProject("TestProject2", "bin"); //$NON-NLS-1$//$NON-NLS-2$
		assertNotNull("project null", fJProject); //$NON-NLS-1$

		//----------------Set up internal jar----------------------------
		File myInternalJar= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/compoundtest.jar")); //$NON-NLS-1$
		assertNotNull("lib not found", myInternalJar); //$NON-NLS-1$
		assertTrue("lib not found", myInternalJar.exists()); //$NON-NLS-1$
		fInternalJarRoot= JavaProjectHelper.addLibraryWithImport(fJProject, Path.fromOSString(myInternalJar.getPath()), null, null);
		fInternalJarRoot.getPackageFragment(""); //$NON-NLS-1$
		fInternalPack3= fInternalJarRoot.getPackageFragment("pack3"); //$NON-NLS-1$
		fInternalPack4= fInternalJarRoot.getPackageFragment("pack3.pack4"); //$NON-NLS-1$
		fInternalPack5= fInternalJarRoot.getPackageFragment("pack3.pack5"); //$NON-NLS-1$
		fInternalPack6= fInternalJarRoot.getPackageFragment("pack3.pack5.pack6"); //$NON-NLS-1$
		fInternalPack10= fInternalJarRoot.getPackageFragment("pack3.pack4.pack10"); //$NON-NLS-1$
		fInternalJarRoot.getPackageFragment("meta-inf"); //$NON-NLS-1$

		//-----------------Set up source folder--------------------------

		fRoot2= JavaProjectHelper.addSourceContainer(fJProject, "src2"); //$NON-NLS-1$
		fRoot2.createPackageFragment("", true, null); //$NON-NLS-1$
		fPack12= fRoot2.createPackageFragment("pack1", true, null); //$NON-NLS-1$
		fRoot2.createPackageFragment("pack1.pack7", true, null); //$NON-NLS-1$
		fPack32= fRoot2.createPackageFragment("pack3", true, null); //$NON-NLS-1$
		fPack42= fRoot2.createPackageFragment("pack3.pack4", true, null); //$NON-NLS-1$
		fPack52= fRoot2.createPackageFragment("pack3.pack5", true, null); //$NON-NLS-1$
		fPack62= fRoot2.createPackageFragment("pack3.pack5.pack6", true, null); //$NON-NLS-1$
		fPack102= fRoot2.createPackageFragment("pack3.pack4.pack10", true, null); //$NON-NLS-1$

		fPack12.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$
		fPack62.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$
		// So that fPack52 won't get deleted when we delete fPack62 in certain tests
		fPack52.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$
		// So that fPack42 won't get deleted when we delete fPack102 in certain tests
		fPack42.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$

		// Set up project #2: file system structure with in a source folder

		//	JavaProjectHelper.addVariableEntry(fJProject2, new Path("JRE_LIB_TEST"), null, null);

		//----------------Set up source folder--------------------------

		fRoot1= JavaProjectHelper.addSourceContainer(fJProject, "src1"); //$NON-NLS-1$
		fRoot1.createPackageFragment("", true, null); //$NON-NLS-1$
		fPack21= fRoot1.createPackageFragment("pack2", true, null); //$NON-NLS-1$
		fPack31= fRoot1.createPackageFragment("pack3", true, null); //$NON-NLS-1$
		fPack41= fRoot1.createPackageFragment("pack3.pack4", true, null); //$NON-NLS-1$
		fPack91= fRoot1.createPackageFragment("pack3.pack4.pack9", true, null); //$NON-NLS-1$
		fPack51= fRoot1.createPackageFragment("pack3.pack5", true, null); //$NON-NLS-1$
		fPack61= fRoot1.createPackageFragment("pack3.pack5.pack6", true, null); //$NON-NLS-1$
		fPack81= fRoot1.createPackageFragment("pack3.pack8", true, null); //$NON-NLS-1$

		fPack21.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$
		fPack61.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$

		// Set up the mock view
		setUpMockView();
	}

	public void setUpMockView() throws Exception {
		fWorkbench= PlatformUI.getWorkbench();
		assertNotNull(fWorkbench);

		fPage= fWorkbench.getActiveWorkbenchWindow().getActivePage();
		assertNotNull(fPage);

		MockPluginView.setListState(false);
		IViewPart myPart= fPage.showView("org.eclipse.jdt.ui.tests.browsing.MockPluginView"); //$NON-NLS-1$
		if (myPart instanceof MockPluginView) {
			fMyPart= (MockPluginView) myPart;
			fProvider= (ITreeContentProvider) fMyPart.getTreeViewer().getContentProvider();
			JavaCore.removeElementChangedListener((IElementChangedListener) fProvider);
		} else {
			fail("Unable to get view"); //$NON-NLS-1$
		}

		assertNotNull(fProvider);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject);
		fProvider.inputChanged(null, null, null);
		fPage.hideView(fMyPart);

		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(true);
	}

	/**
	 * Method compareArrays. Both arrays must be of IPackageFragments or compare will fail.
	 *
	 * @param children the children
	 * @param expectedChildren the expected children
	 * @return boolean returns true if the arrays contain the same elements
	 */
	private boolean compareArrays(Object[] children, Object[] expectedChildren) {
		if (children.length != expectedChildren.length)
			return false;

		for (Object child : children) {
			if (child instanceof IJavaElement) {
				IJavaElement el= (IJavaElement) child;
				if (!contains(el, expectedChildren))
					return false;
			} else if (child instanceof IResource) {
				IResource res= (IResource) child;
				if (!contains(res, expectedChildren)) {
					return false;
				}
			} else if (child instanceof LogicalPackage) {
				if (!contains((LogicalPackage) child, expectedChildren))
					return false;
			}
		}

		return true;
	}

	private boolean contains(IResource res, Object[] expectedChildren) {
		for (Object object : expectedChildren) {
			if (object instanceof IResource) {
				IResource expres= (IResource) object;
				if (expres.equals(res))
					return true;
			}
		}

		return false;
	}

	private boolean contains(IJavaElement fragment, Object[] expectedChildren) {
		for (Object object : expectedChildren) {
			if (object instanceof IJavaElement) {
				IJavaElement expfrag= (IJavaElement) object;
				if (expfrag.equals(fragment))
					return true;
			}
		}

		return false;
	}

	private boolean contains(LogicalPackage lp, Object[] expectedChildren) {
		for (Object object : expectedChildren) {
			if (object instanceof LogicalPackage) {
				LogicalPackage explp= (LogicalPackage) object;
				if (explp.equals(lp))
					return true;
			}
		}

		return false;
	}
}
