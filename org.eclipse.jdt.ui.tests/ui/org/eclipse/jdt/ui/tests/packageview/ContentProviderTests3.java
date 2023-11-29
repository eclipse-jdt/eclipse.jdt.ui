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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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

import org.eclipse.jdt.internal.ui.util.CoreUtility;


/**
 * Tests for the PackageExplorerContentProvider.
 *
 * @since 2.1
 */
public class ContentProviderTests3{

	private IJavaProject fJProject1;
	private IJavaProject fJProject2;

	private IPackageFragmentRoot fRoot1;
	private IPackageFragment fPack1;
	private IPackageFragment fPack2;
	private IPackageFragment fPack4;
	private IPackageFragment fPack3;
	private IWorkspace fWorkspace;
	private IWorkbench fWorkbench;
	private MockPluginView fMyPart;

	private ITreeContentProvider fProvider;
	private IPackageFragmentRoot fArchiveFragmentRoot;
	private IPackageFragment fPackJunit;
	private IPackageFragment fPackJunitSamples;
	private IPackageFragment fPackJunitSamplesMoney;

	private IPackageFragment fPack6;
	private IPackageFragment fPackJunitExtentions;
	private IPackageFragment fPackJunitFramework;
	private IPackageFragment fPackJunitRunner;
	private IPackageFragment fPackJunitTextUi;
	private IPackageFragment fPackJunitUi;
	private IPackageFragment fPackJunitTests;

	private ICompilationUnit fCUIMoney;
	private ICompilationUnit fCUMoney;
	private ICompilationUnit fCUMoneyBag;
	private ICompilationUnit fCUMoneyTest;
	private ICompilationUnit fCU1;
	private ICompilationUnit fCU2;
	private ICompilationUnit fCU3;
	private IFile fFile1;
	private IFile fFile2;
	private IFolder dotSettings;

	private IWorkbenchPage page;
	private IPackageFragmentRoot jdk;
	private boolean fEnableAutoBuildAfterTesting;

	//---------Test for getChildren-------------------

	@Test
	public void testGetChildrenProjectWithSourceFolders() throws Exception{
		Object[] expectedChildren= new Object[]{fRoot1, fFile1, fFile2, dotSettings, jdk};
		Object[] children= fProvider.getChildren(fJProject2);
		assertTrue("Wrong children found for project with folding", compareArrays(children, expectedChildren));//$NON-NLS-1$
	}


	@Test
	public void testGetChildrentMidLevelFragment() throws Exception{
		Object[] expectedChildren= new Object[]{fPack4, fPack6};
		Object[] children= fProvider.getChildren(fPack3);
		assertTrue("Wrong children found for PackageFragment with folding",compareArrays(children, expectedChildren));//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenBottomLevelFragment() throws Exception{
		Object[] expectedChildren= new Object[]{};
		Object[] children= fProvider.getChildren(fPack1);
		assertTrue("Wrong children found for PackageFragment with folding",compareArrays(children, expectedChildren));//$NON-NLS-1$

	}

	@Test
	public void testGetChildrenBottomLevelFragmentWithCU() throws Exception{
		Object[] expectedChildren= new Object[]{fCU1};
		Object[] children= fProvider.getChildren(fPack2);
		assertTrue("Wrong children found for PackageFragment with folding",compareArrays(children, expectedChildren));	//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenBottomLevelFragmentInArchive() throws Exception{
		Object[] expectedChildren= new Object[]{fCUIMoney, fCUMoney, fCUMoneyBag, fCUMoneyTest};
		Object[] children= fProvider.getChildren(fPackJunitSamplesMoney);
		assertTrue("wrong children found for a bottom PackageFragment in PackageFragmentRoot Archive with folding", compareArrays(children, expectedChildren));	//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenSource() throws Exception{
		Object[] expectedChildren= new Object[]{fPack1,fPack2,fPack3, fRoot1.getPackageFragment("")};//$NON-NLS-1$
		Object[] children= fProvider.getChildren(fRoot1);
		assertTrue("Wrong children found for PackageFragmentRoot with folding", compareArrays(children, expectedChildren));	//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenArchive(){
		Object[] expectedChildren= new Object[]{fPackJunit, fArchiveFragmentRoot.getPackageFragment("")};//$NON-NLS-1$
		Object[] children= fProvider.getChildren(fArchiveFragmentRoot);
		assertTrue("Wrong child found for PackageFragmentRoot Archive with folding", compareArrays(children,expectedChildren));//$NON-NLS-1$

	}

	//---------------Get Parent Tests-----------------------------

	@Test
	public void testGetParentArchive() throws Exception{
		Object parent= fProvider.getParent(fArchiveFragmentRoot);
		assertSame("Wrong parent found for PackageFragmentRoot Archive with folding", parent, fJProject1);//$NON-NLS-1$
	}

	@Test
	public void testGetParentTopLevelFragmentInArchive() throws Exception{
		Object expectedParent= fPackJunit;
		Object parent= fProvider.getParent(fPackJunitSamples);
		assertEquals("Wrong parent found for a top level PackageFragment in an Archive with folding", expectedParent, parent);	//$NON-NLS-1$
	}

	@Test
	public void testGetParentTopLevelFragment() throws Exception{
		Object expectedParent= fRoot1;
		Object parent= fProvider.getParent(fPack3);
		assertEquals("Wrong parent found for a top level PackageFragment with folding", expectedParent, parent); //$NON-NLS-1$
	}

	@Test
	public void testGetParentFoldedBottomFragment() throws Exception{
		Object expectedParent= fRoot1;
		Object parent= fProvider.getParent(fPack3);
		assertEquals("Wrong parent found for a top level PackageFragment with folding", expectedParent, parent);//$NON-NLS-1$

	}

	@Test
	public void testGetParentMidLevelFragment() throws Exception{
		Object expectedParent= fPack3;
		Object parent= fProvider.getParent(fPack4);
		assertEquals("Wrong parent found for a NON top level PackageFragment with folding", expectedParent, parent);//$NON-NLS-1$
	}

	@Test
	public void testDeleteBottomLevelFragmentFolding() throws Exception {

		//send a delta indicating fragment deleted
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createDelta(fPack4, IJavaElementDelta.REMOVED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from dispaly
		while(fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch())

		assertTrue("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertTrue("Correct Refresh", fMyPart.wasObjectRefreshed(fRoot1)); //$NON-NLS-1$
		assertEquals("Single refresh", 1, fMyPart.getRefreshedObject().size()); //$NON-NLS-1$
	}

	@Test
	public void testAddBottomLevelFragmentFolding() throws Exception {
		IPackageFragment test= fRoot1.createPackageFragment("test", true, null); //$NON-NLS-1$

		//send a delta indicating fragment deleted
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createDelta(test, IJavaElementDelta.ADDED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from dispaly
		while(fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
		}

		assertFalse("No add happened", fMyPart.hasAddHappened()); //$NON-NLS-1$
		assertTrue("Refresh happened", fMyPart.hasRefreshHappened()); //$NON-NLS-1$
		assertTrue("Correct Refresh", fMyPart.wasObjectRefreshed(fRoot1)); //$NON-NLS-1$
		assertEquals("Single refresh", 1, fMyPart.getRefreshedObject().size()); //$NON-NLS-1$
	}

	@Test
	public void testChangedTopLevelPackageFragmentFolding() throws Exception {
		//send a delta indicating fragment deleted
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createDelta(fPack3, IJavaElementDelta.CHANGED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from dispaly
		while(fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
		}

		assertEquals("No refreshs", 0, fMyPart.getRefreshedObject().size()); //$NON-NLS-1$
	}

	@Test
	public void testChangeBottomLevelPackageFragmentFolding() throws Exception {
		//send a delta indicating fragment deleted
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createDelta(fPack6, IJavaElementDelta.CHANGED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from dispaly
		while(fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
		}

		assertEquals("No refreshs",0,  fMyPart.getRefreshedObject().size()); //$NON-NLS-1$
	}

	@Test
	public void testRemoveCUsFromPackageFragment() throws Exception{

		//send a delta indicating fragment deleted
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createCUDelta(new ICompilationUnit[] { fCU2, fCU3 }, fPack6, IJavaElementDelta.REMOVED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while(fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
		}

		// removing more than one CU now results in a refresh.
		assertEquals("One refresh", 1, fMyPart.getRefreshedObject().size()); //$NON-NLS-1$
	}

	@Test
	public void testRemoveCUFromPackageFragment() throws Exception {

		//send a delta indicating fragment deleted
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		IJavaElementDelta delta= TestDelta.createCUDelta(new ICompilationUnit[]{fCU2}, fPack6, IJavaElementDelta.REMOVED);
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from display
		while(fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
		}

		assertTrue("Remove happened", fMyPart.hasRemoveHappened()); //$NON-NLS-1$
		assertTrue("Correct refresh", fMyPart.getRemovedObjects().contains(fCU2)); //$NON-NLS-1$
		assertEquals("No refreshes", 0, fMyPart.getRefreshedObject().size()); //$NON-NLS-1$
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

		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");//$NON-NLS-1$//$NON-NLS-2$
		fJProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");//$NON-NLS-1$//$NON-NLS-2$

		assertNotNull("project1 null", fJProject1);//$NON-NLS-1$
		assertNotNull("project2 null", fJProject2);//$NON-NLS-1$

		for (Object object : fJProject2.getNonJavaResources()) {
			if(object instanceof IFile){
				IFile file = (IFile) object;
				if(".classpath".equals(file.getName()))//$NON-NLS-1$
					fFile1= file;
				else if (".project".equals(file.getName()))//$NON-NLS-1$
					fFile2= file;
			} else if (object instanceof IFolder) {
				IFolder folder= (IFolder) object;
				if(".settings".equals(folder.getName())) {
					dotSettings= folder;
				}
			}
		}
		assertNotNull(fFile1);
		assertNotNull(fFile2);
		assertNotNull(dotSettings);

		//set up project #1 : External Jar and zip file
		jdk= JavaProjectHelper.addVariableRTJar(fJProject1, "JRE_LIB_TEST", null, null);//$NON-NLS-1$
		assertNotNull("jdk not found", jdk);//$NON-NLS-1$

		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);

		assertNotNull("junit src not found", junitSrcArchive);//$NON-NLS-1$
		assertTrue("junit src not found", junitSrcArchive.exists());//$NON-NLS-1$

		fArchiveFragmentRoot= JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);//$NON-NLS-1$

		fPackJunit= fArchiveFragmentRoot.getPackageFragment("junit");//$NON-NLS-1$
		fPackJunitSamples= fArchiveFragmentRoot.getPackageFragment("junit.samples");//$NON-NLS-1$
		fPackJunitSamplesMoney= fArchiveFragmentRoot.getPackageFragment("junit.samples.money");//$NON-NLS-1$
		fPackJunitExtentions= fArchiveFragmentRoot.getPackageFragment("junit.extensions");//$NON-NLS-1$
		fPackJunitFramework= fArchiveFragmentRoot.getPackageFragment("junit.framework");//$NON-NLS-1$
		fPackJunitRunner= fArchiveFragmentRoot.getPackageFragment("junit.runner");//$NON-NLS-1$
		fPackJunitTests= fArchiveFragmentRoot.getPackageFragment("junit.tests");//$NON-NLS-1$
		fPackJunitTextUi= fArchiveFragmentRoot.getPackageFragment("junit.textui");//$NON-NLS-1$
		fPackJunitUi= fArchiveFragmentRoot.getPackageFragment("junit.ui");//$NON-NLS-1$

		assertNotNull("creating fPackJunit", fPackJunit);//$NON-NLS-1$
		assertNotNull("creating fPackJunitSamples", fPackJunitSamples);//$NON-NLS-1$
		assertNotNull("creating fPackJunitSamplesMoney",fPackJunitSamplesMoney);//$NON-NLS-1$
		assertNotNull("", fPackJunitExtentions);//$NON-NLS-1$
		assertNotNull("",fPackJunitFramework);//$NON-NLS-1$
		assertNotNull("",fPackJunitRunner);//$NON-NLS-1$
		assertNotNull("",fPackJunitTests);//$NON-NLS-1$
		assertNotNull("",fPackJunitTextUi);//$NON-NLS-1$
		assertNotNull("",fPackJunitUi);//$NON-NLS-1$

		fCUIMoney= fPackJunitSamplesMoney.getCompilationUnit("IMoney.java");//$NON-NLS-1$
		fCUMoney= fPackJunitSamplesMoney.getCompilationUnit("Money.java");//$NON-NLS-1$
		fCUMoneyBag= fPackJunitSamplesMoney.getCompilationUnit("MoneyBag.java");//$NON-NLS-1$
		fCUMoneyTest= fPackJunitSamplesMoney.getCompilationUnit("MoneyTest.java");//$NON-NLS-1$

		File mylibJar= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);
		assertNotNull("lib not found", mylibJar);//$NON-NLS-1$
		assertTrue("lib not found", mylibJar.exists());//$NON-NLS-1$
		JavaProjectHelper.addLibraryWithImport(fJProject1, Path.fromOSString(mylibJar.getPath()), null, null);

		//set up project #2: file system structure with in a source folder

		JavaProjectHelper.addVariableEntry(fJProject2, new Path("JRE_LIB_TEST"), null, null);//$NON-NLS-1$

		fRoot1= JavaProjectHelper.addSourceContainer(fJProject2, "src1");//$NON-NLS-1$
		fPack1= fRoot1.createPackageFragment("pack1", true, null);//$NON-NLS-1$
		fPack2= fRoot1.createPackageFragment("pack2", true, null);//$NON-NLS-1$
		fPack3= fRoot1.createPackageFragment("pack3",true,null);//$NON-NLS-1$
		fPack4= fRoot1.createPackageFragment("pack3.pack4", true,null);//$NON-NLS-1$
		fRoot1.createPackageFragment("pack3.pack5",true,null);//$NON-NLS-1$
		fPack6= fRoot1.createPackageFragment("pack3.pack5.pack6", true, null);//$NON-NLS-1$

		fCU1= fPack2.createCompilationUnit("Object.java", "", true, null);//$NON-NLS-1$//$NON-NLS-2$
		fCU2= fPack6.createCompilationUnit("Object.java","", true, null);//$NON-NLS-1$//$NON-NLS-2$
		fCU3= fPack6.createCompilationUnit("Jen.java","", true,null);//$NON-NLS-1$//$NON-NLS-2$

		//set up the mock view
		setUpMockView();
	}

	public void setUpMockView() throws Exception{


//		fWorkspace= ResourcesPlugin.getWorkspace();
//		assertNotNull(fWorkspace);

		fWorkbench= PlatformUI.getWorkbench();
		assertNotNull(fWorkbench);

		page= fWorkbench.getActiveWorkbenchWindow().getActivePage();
		assertNotNull(page);

		//just testing to make sure my part can be created
		IViewPart myPart= new MockPluginView();
		assertNotNull(myPart);

		myPart= page.showView("org.eclipse.jdt.ui.tests.packageview.MockPluginView");//$NON-NLS-1$
		if (myPart instanceof MockPluginView) {
			fMyPart= (MockPluginView) myPart;
			//turn on folding
			fMyPart.setFolding(true);
			fMyPart.setFlatLayout(false);
			// above call might cause a property change event being sent
			fMyPart.clear();
			fProvider= (ITreeContentProvider)fMyPart.getTreeViewer().getContentProvider();

		}else fail("Unable to get view");//$NON-NLS-1$

		assertNotNull(fProvider);
	}

	@After
	public void tearDown() throws Exception {
		fArchiveFragmentRoot.close();
		JavaProjectHelper.delete(fJProject1);
		JavaProjectHelper.delete(fJProject2);
		page.hideView(fMyPart);

		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(true);

	}

	/**
	 * Method compareArrays. Both arrays must be of IPackageFragments or compare will fail.
	 * @return boolean
	 */
	private boolean compareArrays(Object[] children, Object[] expectedChildren) {
		if(children.length!=expectedChildren.length)
			return false;
		for (Object child : children) {
			if (child instanceof IJavaElement) {
				IJavaElement el= (IJavaElement) child;
				if(!contains(el, expectedChildren))
					return false;
			} else if(child instanceof IResource){
				IResource res = (IResource) child;
				if(!contains(res, expectedChildren)){
					return false;
				}
			}
		}
		return true;
	}
	/**
	 * Method contains.
	 * @return boolean
	 */
	private boolean contains(IResource res, Object[] expectedChildren) {
		for (Object object : expectedChildren) {
			if (object instanceof IResource) {
				IResource expres= (IResource) object;
				if(expres.equals(res))
					return true;
			}
		}
		return false;
	}

	/**
	 * Method contains.
	 * @return boolean
	 */
	private boolean contains(IJavaElement fragment, Object[] expectedChildren) {
		for (Object object : expectedChildren) {
			if (object instanceof IJavaElement) {
				IJavaElement expfrag= (IJavaElement) object;
				if(expfrag.equals(fragment))
					return true;
			}
		}
		return false;
	}

}
