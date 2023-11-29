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

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.util.CoreUtility;


/**
 * Tests for the PackageExplorerContentProvider.
 *
 * @since 2.1
 */
public class ContentProviderTests2{

	private IPackageFragmentRoot fRoot1;
	private IPackageFragment fPack1;
	private IPackageFragment fPack2;
	private IPackageFragment fPack4;
	private IPackageFragment fPack3;
	private IWorkspace fWorkspace;
	private IWorkbench fWorkbench;
	private MockPluginView fMyPart;

	private ITreeContentProvider fProvider;

	private IPackageFragmentRoot jdk;

	private IPackageFragment fPack5;
	private IJavaProject fJProject3;
	private IPackageFragment fPack6;
	private IPackageFragmentRoot fInternalRoot1;
	private IPackageFragment fA;
	private IPackageFragment fX;
	private IPackageFragment fB;
	private IPackageFragment fC;
	private IPackageFragment fY;
	private IFile fFile1;
	private IFile fFile2;
	private IFolder dotSettings;
	private ICompilationUnit fCU1;
	private ICompilationUnit fCU2;
	private IClassFile fYClassFile;

	private IWorkbenchPage page;
	private boolean fEnableAutoBuildAfterTesting;

	@Test
	public void testGetChildrenProject() throws Exception{
		Object[] expectedChildren= new Object[]{fPack1, fPack2, fPack3, fRoot1.getPackageFragment(""), fFile1, fFile2, dotSettings, fInternalRoot1,jdk}; //$NON-NLS-1$
		Object[] children= fProvider.getChildren(fJProject3);
		assertTrue("Wrong children found for project", compareArrays(children, expectedChildren)); //$NON-NLS-1$
	}

	@Test
	public void testGetChildrentMidLevelFragment() throws Exception{
		Object[] expectedChildren= new Object[]{fPack4, fPack5};
		Object[] children= fProvider.getChildren(fPack3);
		assertTrue("Wrong children found for PackageFragment",compareArrays(children, expectedChildren)); //$NON-NLS-1$
	}

	@Test
	public void testGetChildrenBottomLevelFragment() throws Exception{
		Object[] expectedChildren= new Object[]{};
		Object[] children= fProvider.getChildren(fPack1);
		assertTrue("Wrong children found for PackageFragment",compareArrays(children, expectedChildren)); //$NON-NLS-1$
	}

	@Test
	public void testGetChildrenBottomLevelFragmentWithCU() throws Exception{
		Object[] expectedChildren= new Object[]{fCU1};
		Object[] children= fProvider.getChildren(fPack2);
		assertTrue("Wrong children found for PackageFragment with CU",compareArrays(children, expectedChildren)); //$NON-NLS-1$
	}

	@Test
	public void testGetChildrenBottomLevelFragmentFile() throws Exception{
		Object[] expectedChildren= new Object[]{};
		Object[] children= fProvider.getChildren(fPack1);
		assertTrue("Wrong children found for PackageFragment with File",compareArrays(children, expectedChildren)); //$NON-NLS-1$
	}

	@Test
	public void testGetChildrenBottomLevelFragment2() throws Exception{
		Object[] expectedChildren= new Object[]{fCU2};
		Object[] children= fProvider.getChildren(fPack6);
		assertTrue("Wrong children found for PackageFragment",compareArrays(children, expectedChildren)); //$NON-NLS-1$
	}

	@Test
	public void testGetChildrenMidLevelFragmentInInternalArchive() throws Exception{
		Object[] expectedChildren= new Object[]{fC};
		Object[] children= fProvider.getChildren(fB);
		assertTrue("wrong children found for a NON bottom PackageFragment in PackageFragmentRoot Internal Archive", compareArrays(children, expectedChildren));//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenBottomLevelFragmentInInternalArchive() throws Exception{
		Object[] expectedChildren= new Object[]{fYClassFile};
		Object[] children= fProvider.getChildren(fY);
		assertTrue("wrong children found for a bottom PackageFragment in PackageFragmentRoot Internal Archive", compareArrays(children, expectedChildren));	//$NON-NLS-1$
	}

	public void getChildrenInternalArchive() throws Exception{
		Object[] expectedChildren= new Object[]{fX,fA, fInternalRoot1.getPackageFragment("")}; //$NON-NLS-1$
		Object[] children= fProvider.getChildren(fInternalRoot1);
		assertTrue("Wrong child found for PackageFragmentRoot Internal Archive", compareArrays(children,expectedChildren));//$NON-NLS-1$
	}

	//---------------Get Parent Tests-----------------------------

	@Test
	public void testGetParentArchive() throws Exception{
		Object parent= fProvider.getParent(fInternalRoot1);
		assertSame("Wrong parent found for PackageFragmentRoot Archive", parent, fJProject3); //$NON-NLS-1$
	}

	@Test
	public void testGetParentMidLevelFragmentInArchive() throws Exception{
		Object expectedParent= fB;
		Object parent= fProvider.getParent(fC);
		assertEquals("Wrong parent found for a NON top level PackageFragment in an Archive", expectedParent, parent); //$NON-NLS-1$
	}

	@Test
	public void testGetParentTopLevelFragmentInArchive() throws Exception{
		Object expectedParent= fInternalRoot1;
		Object parent= fProvider.getParent(fA);
		assertEquals("Wrong parent found for a top level PackageFragment in an Archive", expectedParent, parent);	 //$NON-NLS-1$
	}

	@Test
	public void testGetParentTopLevelFragment() throws Exception{
		Object expectedParent= fJProject3;
		Object parent= fProvider.getParent(fPack3);
		assertEquals("Wrong parent found for a top level PackageFragment", expectedParent, parent); //$NON-NLS-1$
	}

	@Test
	public void testGetParentMidLevelFragment() throws Exception{
		Object expectedParent= fPack3;
		Object parent= fProvider.getParent(fPack5);
		assertEquals("Wrong parent found for a NON top level PackageFragment", expectedParent, parent); //$NON-NLS-1$
	}


	//-------------------Set up methods--------------------------------
	@Before
	public void setUp() throws Exception {


		fWorkspace= ResourcesPlugin.getWorkspace();
		assertNotNull(fWorkspace);
		IWorkspaceDescription workspaceDesc= fWorkspace.getDescription();
		fEnableAutoBuildAfterTesting= workspaceDesc.isAutoBuilding();
		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(false);

		//create project
		fJProject3= JavaProjectHelper.createJavaProject("TestProject3", "bin"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("project3 null", fJProject3); //$NON-NLS-1$

		for (Object object : fJProject3.getNonJavaResources()) {
			if(object instanceof IFile){
				IFile file = (IFile) object;
				if(".classpath".equals(file.getName())) //$NON-NLS-1$
					fFile1= file;
				else if (".project".equals(file.getName())) //$NON-NLS-1$
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

		//add rt.jar
		jdk= JavaProjectHelper.addVariableRTJar(fJProject3, "JRE_LIB_TEST", null, null); //$NON-NLS-1$
		assertNotNull("jdk not found", jdk); //$NON-NLS-1$

		//create the PackageFragmentRoot that represents the project as source folder
		fRoot1= JavaProjectHelper.addSourceContainer(fJProject3, ""); //$NON-NLS-1$
		assertNotNull("getting default package", fRoot1); //$NON-NLS-1$

		//set up project #3: file system structure with project as source folder
		//add an internal jar
		File myInternalLibJar= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/myinternallib.jar")); //$NON-NLS-1$
		assertNotNull("lib not found", myInternalLibJar); //$NON-NLS-1$
		assertTrue("lib not found", myInternalLibJar.exists()); //$NON-NLS-1$
		fInternalRoot1= JavaProjectHelper.addLibraryWithImport(fJProject3, Path.fromOSString(myInternalLibJar.getPath()), null, null);

		//create internal PackageFragments
		fA= fInternalRoot1.getPackageFragment("a"); //$NON-NLS-1$
		fX= fInternalRoot1.getPackageFragment("x"); //$NON-NLS-1$
		fB= fInternalRoot1.getPackageFragment("a.b"); //$NON-NLS-1$
		fC= fInternalRoot1.getPackageFragment("a.b.c"); //$NON-NLS-1$
		fInternalRoot1.getPackageFragment("a.d"); //$NON-NLS-1$
		fY= fInternalRoot1.getPackageFragment("x.y"); //$NON-NLS-1$

		fYClassFile= fY.getClassFile("Y.class"); //$NON-NLS-1$

		//create PackageFragments
		fPack1= fRoot1.createPackageFragment("pack1", true, null); //$NON-NLS-1$
		fPack2= fRoot1.createPackageFragment("pack2", true, null); //$NON-NLS-1$
		fPack3= fRoot1.createPackageFragment("pack3",true,null); //$NON-NLS-1$
		fPack4= fRoot1.createPackageFragment("pack3.pack4", true,null); //$NON-NLS-1$
		fPack5= fRoot1.createPackageFragment("pack3.pack5",true,null); //$NON-NLS-1$
		fPack6= fRoot1.createPackageFragment("pack3.pack5.pack6", true, null); //$NON-NLS-1$

		fCU1= fPack2.createCompilationUnit("Object.java", "", true, null); //$NON-NLS-1$ //$NON-NLS-2$
		fCU2= fPack6.createCompilationUnit("Object.java","", true, null); //$NON-NLS-1$ //$NON-NLS-2$

		//set up the mock view
		setUpMockView();
	}

	public void setUpMockView() throws Exception {
//		fWorkspace = ResourcesPlugin.getWorkspace();
//		assertNotNull(fWorkspace);

		fWorkbench = PlatformUI.getWorkbench();
		assertNotNull(fWorkbench);

		page = fWorkbench.getActiveWorkbenchWindow().getActivePage();
		assertNotNull(page);

		//just testing to make sure my part can be created
		IViewPart myPart = new MockPluginView();
		assertNotNull(myPart);

		myPart = page.showView("org.eclipse.jdt.ui.tests.packageview.MockPluginView"); //$NON-NLS-1$
		if (myPart instanceof MockPluginView) {
			fMyPart = (MockPluginView) myPart;
			fMyPart.setFolding(false);
			fMyPart.setFlatLayout(false);
			// above call might cause a property change event being sent
			fMyPart.clear();
			fProvider = (ITreeContentProvider) fMyPart.getTreeViewer().getContentProvider();
		} else
			fail("Unable to get view"); //$NON-NLS-1$

		assertNotNull(fProvider);
	}

	@After
	public void tearDown() throws Exception {
		fInternalRoot1.close();
		JavaProjectHelper.delete(fJProject3);
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
