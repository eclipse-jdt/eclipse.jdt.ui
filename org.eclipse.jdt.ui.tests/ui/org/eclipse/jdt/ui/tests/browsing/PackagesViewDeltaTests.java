/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.tests.browsing;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

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

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;

/**
 * @author jthorsley
 */
public class PackagesViewDeltaTests extends TestCase {
	
	public static Test suite() {
		TestSuite suite= new TestSuite("org.eclipse.jdt.ui.tests.PackagesViewDeltaTests");//$NON-NLS-1$
		//$JUnit-BEGIN$
		suite.addTestSuite(PackagesViewDeltaTests.class);
		//$JUnit-END$
		return suite;
	}
	
	private IJavaProject fJProject;
	
	private IPackageFragmentRoot fRoot1;
	private IWorkspace fWorkspace;
	private IWorkbench fWorkbench;
	private MockPluginView fMyPart;
	
	private ITreeContentProvider fProvider;

	private ICompilationUnit fCU1;
	private ICompilationUnit fCU2;
	
	private IWorkbenchPage fPage;
	private IPackageFragmentRoot fRoot2;
	private IPackageFragment fPack12;
	private IPackageFragment fPack22;
	private IPackageFragment fPack32;
	private IPackageFragment fPack42;
	private IPackageFragment fPack52;
	private IPackageFragment fPack62;
	private ICompilationUnit fCU12;
	private ICompilationUnit fCU22;
	private IPackageFragment fPack21;
	private IPackageFragment fPack11;
	private ICompilationUnit fCU11;
	private ICompilationUnit fCU21;
	private IPackageFragment fPack61;
	private IPackageFragment fPack51;
	private IPackageFragment fPack41;
	private IPackageFragment fPack31;
	private IPackageFragment fPackDefault1;
	private IPackageFragment fPackDefault2;
	private IPackageFragment fPack17;
	private IPackageFragment fPack81;
	private IPackageFragment fPack91;
	private IPackageFragmentRoot fInternalJarRoot;
	private IPackageFragment fInternalPackDefault;
	private IPackageFragment fInternalPack3;
	private IPackageFragment fInternalPack4;
	private IPackageFragment fInternalPack5;
	private IPackageFragment fInternalPack10;
	private IPackageFragment fInternalPack6;
	private IPackageFragment fInternalPackMetaInf;
	private ICompilationUnit fCU23;
	private IPackageFragment fPack102;
	private ICompilationUnit fCU33;
	private ICompilationUnit fCU43;
	private boolean fEnableAutoBuildAfterTesting;
	
	public PackagesViewDeltaTests(String name) {
		super(name);
	}
	
	//-----------------Remove delta test cases------------------
	
	public void testRemoveTopLevelFragmentNotCompound() throws Exception {

		//create a compound element for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		//initialise Map
		fProvider.getChildren(fJProject);
		fProvider.getChildren(cp3);

		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.REMOVED, fPack12);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch());

		assertTrue("Remove happened", fMyPart.hasRemoveHappened()); //$NON-NLS-1$
		assertTrue("Correct package removed", fMyPart.getRemovedObject().contains(fPack12)); //$NON-NLS-1$
	}

	public void testRemoveBottomLevelFragmentNotCompound() throws Exception {

		//initialise Map
		fProvider.getChildren(fJProject);

		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.REMOVED, fPack42);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch());

		assertTrue("No remove happened, in Logical Package", !fMyPart.hasRemoveHappened()); //$NON-NLS-1$
	}

	//This is a bogus test because this situation could never occure
	//while fPack42 exists you cannot remove fPack32 still it tests 
	//correct delta handeling.
	public void testRemoveFragmentInMultiFragmentCompoundElement() throws Exception {

		//initialise the map
		Object[] children= fProvider.getChildren(fJProject);
		for (int i= 0; i < children.length; i++) {
			Object object= children[i];
			fProvider.getChildren(object);
		}

		//create a compound element child of cp
		LogicalPackage expectedParent= new LogicalPackage(fPack31);
		expectedParent.add(fInternalPack3);

		//create a compound element child of cp
		LogicalPackage ChildCp1= new LogicalPackage(fPack41);
		ChildCp1.add(fPack42);
		ChildCp1.add(fInternalPack4);

		//delete a fragment
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.REMOVED, fPack32);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch());

		//assert remove happened
		assertTrue("Refresh happened", !fMyPart.hasRemoveHappened() && !fMyPart.hasRefreshHappened()); //$NON-NLS-1$

		//test life cycle of CompoundElement
		Object parent= fProvider.getParent(ChildCp1);
		if (!(parent instanceof LogicalPackage)) {
			assertTrue("wrong parent found for compound after remove", false); //$NON-NLS-1$
		}

		LogicalPackage element= (LogicalPackage) parent;
		assertTrue("PackageFragment removed from CompoundElement", canFindEqualCompoundElement(element, new Object[] { expectedParent })); //$NON-NLS-1$

	}

	public void testRemoveBottomLevelFragmentInMultiFragmentCompoundElement() throws Exception {

		//delete a fragment
		fPack62.delete(true, null);

		//create a compound element child of cp with name "pack6"
		LogicalPackage expectedChild= new LogicalPackage(fPack61);
		expectedChild.add(fInternalPack6);

		//create a compound element child of cp with name "pack5"
		LogicalPackage ParentCp5= new LogicalPackage(fPack51);
		ParentCp5.add(fPack52);
		ParentCp5.add(fInternalPack5);

		//create a compound element for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		//initialise map
		fProvider.getChildren(fJProject);
		fProvider.getChildren(cp3);
		fProvider.getChildren(ParentCp5);

		//send a delta indicating fragment deleted
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.REMOVED, fPack62);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch());

		//assert delta correct (no remove or refresh, only change to logicalpackage)
		assertTrue("Refresh happened", !fMyPart.hasRefreshHappened() && !fMyPart.hasRemoveHappened()); //$NON-NLS-1$

		//test life cycle of CompoundElement
		Object[] child= fProvider.getChildren(ParentCp5);
		
		if ((child.length != 1) || (!(child[0] instanceof LogicalPackage))) {
			assertTrue("wrong parent found for compound after remove", false); //$NON-NLS-1$
		}

		LogicalPackage element= (LogicalPackage) child[0];
		assertTrue("PackageFragment removed from CompoundElement", canFindEqualCompoundElement(element, new Object[] { expectedChild })); //$NON-NLS-1$
	}

	public void testRemoveFragmentInTwoFragmentCompound() throws Exception {

		//create a compound element child of cp
		LogicalPackage ParentCp4= new LogicalPackage(fPack41);
		ParentCp4.add(fPack42);
		ParentCp4.add(fInternalPack4);

		//create a compound element for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		//initialise map
		fProvider.getChildren(fJProject);
		fProvider.getChildren(cp3);
		fProvider.getChildren(ParentCp4);

		//create CompoundElement containning
		LogicalPackage cp10= new LogicalPackage(fInternalPack10);
		//cp10.add(fInternalPack10);

		//delete fragment
		fPack102.delete(true, null);

		//send a delta indicating fragment deleted
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.REMOVED, fPack102);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//assert remove happened (delta worked)
		assertTrue("Refresh happened", fMyPart.hasRemoveHappened() && fMyPart.hasAddHappened()); //$NON-NLS-1$
		Object addedObject= fMyPart.getAddedObject().get(0);
		Object removedObject= fMyPart.getRemovedObject().get(0);
		assertTrue("Correct guy removed", canFindEqualCompoundElement(cp10, new Object[] { removedObject })); //$NON-NLS-1$
		assertTrue("Correct guy added", fInternalPack10.equals(addedObject)); //$NON-NLS-1$

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch());

		//assert correct children gotten
		Object[] children= fProvider.getChildren(ParentCp4);
		assertTrue("PackageFragment removed from CompoundElement", compareArrays(children, new Object[] { fPack91, fInternalPack10 })); //$NON-NLS-1$
	}

	//-----------------------Add delta test cases----------------------------------
	public void testAddTopLevelFragmentNotCompound() throws Exception {

		//initialise Map
		fProvider.getChildren(fJProject);

		IPackageFragment test= fRoot1.createPackageFragment("pack3.test", true, null); //$NON-NLS-1$
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.ADDED, test);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch());

		assertTrue("Add happened", fMyPart.hasAddHappened()); //$NON-NLS-1$
		assertTrue("Correct package added", fMyPart.getAddedObject().contains(test)); //$NON-NLS-1$
	}

	public void testAddCUFromFragmentNotCompound() throws Exception {

		//create a compound element for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		//initialise Map
		fProvider.getChildren(fJProject);
		fProvider.getChildren(cp3);

		ICompilationUnit cu= fPack81.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$

		//make sure parent is visible
		fMyPart.fViewer.setInput(fJProject);
		fMyPart.fViewer.reveal(fPack81);

		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createCUDelta(new ICompilationUnit[] { cu }, fPack81, IJavaElementDelta.ADDED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch());

		assertTrue("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertTrue("Correct package refreshed", fMyPart.getRefreshedObject().contains(fPack81)); //$NON-NLS-1$
		assertEquals("Correct number of refreshes", 1, fMyPart.getRefreshedObject().size());//$NON-NLS-1$
	}
	
	public void testRemoveCUFromFragmentNotCompound() throws Exception {

		//create a compound element for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		//initialise Map
		fProvider.getChildren(fJProject);
		fProvider.getChildren(cp3);

		ICompilationUnit cu= fPack81.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$

		//make sure parent is visible
		fMyPart.fViewer.setInput(fJProject);
		fMyPart.fViewer.reveal(fPack81);		

		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createCUDelta(new ICompilationUnit[] { cu }, fPack81, IJavaElementDelta.REMOVED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch());

		assertTrue("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertTrue("Correct package refreshed", fMyPart.getRefreshedObject().contains(fPack81)); //$NON-NLS-1$
		assertEquals("Correct number of refreshes", 1, fMyPart.getRefreshedObject().size());//$NON-NLS-1$
	}
	
	public void testRemoveCUFromFragmentNotCompoundParentNotVisible() throws Exception {

		//create a compound element for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		//initialise Map
		fProvider.getChildren(fJProject);
		fProvider.getChildren(cp3);

		ICompilationUnit cu= fPack81.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$

		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createCUDelta(new ICompilationUnit[] { cu }, fPack81, IJavaElementDelta.REMOVED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch());

		assertTrue("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertTrue("Correct package refreshed", fMyPart.getRefreshedObject().contains(cp3)); //$NON-NLS-1$
		assertEquals("Correct number of refreshes", 1, fMyPart.getRefreshedObject().size());//$NON-NLS-1$
	}
	
	public void testAddBottomLevelFragmentNotCompound() throws Exception {

		//create a compound element for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		//initialise Map
		fProvider.getChildren(fJProject);
		fProvider.getChildren(cp3);

		IPackageFragment test= fRoot1.createPackageFragment("pack3.pack5.test", true, null); //$NON-NLS-1$
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= new TestDelta(IJavaElementDelta.ADDED, test);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch());

		assertTrue("Add happened", fMyPart.hasAddHappened()); //$NON-NLS-1$
		assertTrue("Corrent package added", fMyPart.getAddedObject().contains(test)); //$NON-NLS-1$
	}
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();

		fWorkspace= ResourcesPlugin.getWorkspace();
		assertNotNull(fWorkspace);
		
		IWorkspaceDescription workspaceDesc= fWorkspace.getDescription();
		fEnableAutoBuildAfterTesting= workspaceDesc.isAutoBuilding();
		if (fEnableAutoBuildAfterTesting)
			JavaProjectHelper.setAutoBuilding(false);

		//------------set up project ------------------------------
		fJProject= JavaProjectHelper.createJavaProject("TestProject2", "bin"); //$NON-NLS-1$//$NON-NLS-2$
		assertNotNull("project null", fJProject); //$NON-NLS-1$

		//----------------Set up internal jar----------------------------
		File myInternalJar= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/compoundtest.jar")); //$NON-NLS-1$
		assertTrue("lib not found", myInternalJar != null && myInternalJar.exists()); //$NON-NLS-1$
		fInternalJarRoot= JavaProjectHelper.addLibraryWithImport(fJProject, new Path(myInternalJar.getPath()), null, null);
		fInternalPackDefault= fInternalJarRoot.getPackageFragment(""); //$NON-NLS-1$
		fInternalPack3= fInternalJarRoot.getPackageFragment("pack3"); //$NON-NLS-1$
		fInternalPack4= fInternalJarRoot.getPackageFragment("pack3.pack4"); //$NON-NLS-1$
		fInternalPack5= fInternalJarRoot.getPackageFragment("pack3.pack5"); //$NON-NLS-1$
		fInternalPack6= fInternalJarRoot.getPackageFragment("pack3.pack5.pack6"); //$NON-NLS-1$
		fInternalPack10= fInternalJarRoot.getPackageFragment("pack3.pack4.pack10"); //$NON-NLS-1$
		fInternalPackMetaInf= fInternalJarRoot.getPackageFragment("meta-inf"); //$NON-NLS-1$

		//-----------------Set up source folder--------------------------

		fRoot2= JavaProjectHelper.addSourceContainer(fJProject, "src2"); //$NON-NLS-1$
		fPackDefault2= fRoot2.createPackageFragment("", true, null); //$NON-NLS-1$
		fPack12= fRoot2.createPackageFragment("pack1", true, null); //$NON-NLS-1$
		fPack17= fRoot2.createPackageFragment("pack1.pack7", true, null); //$NON-NLS-1$
		fPack32= fRoot2.createPackageFragment("pack3", true, null); //$NON-NLS-1$
		fPack42= fRoot2.createPackageFragment("pack3.pack4", true, null); //$NON-NLS-1$
		fPack52= fRoot2.createPackageFragment("pack3.pack5", true, null); //$NON-NLS-1$
		fPack62= fRoot2.createPackageFragment("pack3.pack5.pack6", true, null); //$NON-NLS-1$
		fPack102= fRoot2.createPackageFragment("pack3.pack4.pack10", true, null); //$NON-NLS-1$

		fCU12= fPack12.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$
		fCU22= fPack62.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$
		//so that fPack52 won't get deleted when we delete fPack62 in certain tests
		fCU33= fPack52.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$
		//so that fPack42 won't get deleted when we delete fPack102 in certain tests
		fCU43= fPack42.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$

		//set up project #2: file system structure with in a source folder

		//	JavaProjectHelper.addVariableEntry(fJProject2, new Path("JRE_LIB_TEST"), null, null);

		//----------------Set up source folder--------------------------

		fRoot1= JavaProjectHelper.addSourceContainer(fJProject, "src1"); //$NON-NLS-1$
		fPackDefault1= fRoot1.createPackageFragment("", true, null); //$NON-NLS-1$
		fPack21= fRoot1.createPackageFragment("pack2", true, null); //$NON-NLS-1$
		fPack31= fRoot1.createPackageFragment("pack3", true, null); //$NON-NLS-1$
		fPack41= fRoot1.createPackageFragment("pack3.pack4", true, null); //$NON-NLS-1$
		fPack91= fRoot1.createPackageFragment("pack3.pack4.pack9", true, null); //$NON-NLS-1$
		fPack51= fRoot1.createPackageFragment("pack3.pack5", true, null); //$NON-NLS-1$
		fPack61= fRoot1.createPackageFragment("pack3.pack5.pack6", true, null); //$NON-NLS-1$
		fPack81= fRoot1.createPackageFragment("pack3.pack8", true, null); //$NON-NLS-1$

		fCU11= fPack21.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$
		fCU21= fPack61.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$//$NON-NLS-2$

		//set up the mock view
		setUpMockView();
	}
	public void setUpMockView() throws Exception {

		fWorkbench= PlatformUI.getWorkbench();
		assertNotNull(fWorkbench);

		fPage= fWorkbench.getActiveWorkbenchWindow().getActivePage();
		assertNotNull(fPage);

		//just testing to make sure my part can be created
		IViewPart myPart= new MockPluginView();
		assertNotNull(myPart);

		myPart= fPage.showView("org.eclipse.jdt.ui.tests.browsing.MockPluginView"); //$NON-NLS-1$
		if (myPart instanceof MockPluginView) {
			fMyPart= (MockPluginView) myPart;
			fProvider= (ITreeContentProvider) fMyPart.getTreeViewer().getContentProvider();
			JavaCore.removeElementChangedListener((IElementChangedListener) fProvider);
		} else
			assertTrue("Unable to get view", false); //$NON-NLS-1$

		assertNotNull(fProvider);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {

		JavaProjectHelper.delete(fJProject);
		fProvider.inputChanged(null, null, null);
		fPage.hideView(fMyPart);
		fMyPart.dispose();

		if (fEnableAutoBuildAfterTesting)
			JavaProjectHelper.setAutoBuilding(true);

		super.tearDown();
	}
		
	/**
	 * Method compareArrays. Both arrays must be of IPackageFragments or compare will fail.
	 * @param children 
	 * @param expectedChildren
	 * @return boolean returns true if the arrays contain the same elements
	 */
	private boolean compareArrays(Object[] children, Object[] expectedChildren) {
		if (children.length != expectedChildren.length)
			return false;
		for (int i= 0; i < children.length; i++) {
			Object child= children[i];
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
				if (!canFindEqualCompoundElement((LogicalPackage) child, expectedChildren))
					return false;
			}
		}
		return true;
	}

	private boolean canFindEqualCompoundElement(LogicalPackage compoundElement, Object[] expectedChildren) {
		for (int i= 0; i < expectedChildren.length; i++) {
			Object object= expectedChildren[i];
			if (object instanceof LogicalPackage) {
				LogicalPackage el= (LogicalPackage) object;
				if (el.getElementName().equals(compoundElement.getElementName()) && (el.getJavaProject().equals(compoundElement.getJavaProject()))) {
					if (compareArrays(el.getFragments(), compoundElement.getFragments()))
						return true;
				}
			}
		}
		return false;
	}

	private boolean contains(IResource res, Object[] expectedChildren) {
		for (int i= 0; i < expectedChildren.length; i++) {
			Object object= expectedChildren[i];
			if (object instanceof IResource) {
				IResource expres= (IResource) object;
				if (expres.equals(res))
					return true;
			}
		}
		return false;
	}

	private boolean contains(IJavaElement fragment, Object[] expectedChildren) {
		for (int i= 0; i < expectedChildren.length; i++) {
			Object object= expectedChildren[i];
			if (object instanceof IJavaElement) {
				IJavaElement expfrag= (IJavaElement) object;
				if (expfrag.equals(fragment))
					return true;
			}
		}
		return false;
	}
}
