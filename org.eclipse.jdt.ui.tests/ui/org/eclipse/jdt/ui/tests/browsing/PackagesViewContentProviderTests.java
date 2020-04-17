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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.IPath;
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

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

public class PackagesViewContentProviderTests {
	private IJavaProject fJProject1;
	private IJavaProject fJProject2;

	private IPackageFragmentRoot fRoot1;
	private IWorkspace fWorkspace;
	private IWorkbench fWorkbench;
	private MockPluginView fMyPart;

	private ITreeContentProvider fProvider;
	private IPackageFragmentRoot fArchiveFragmentRoot;
	private IPackageFragment fPackJunit;
	private IPackageFragment fPackJunitSamples;
	private IPackageFragment fPackJunitSamplesMoney;

	private IWorkbenchPage page;
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
	private boolean fEnableAutoBuildAfterTesting;

	//---------Test for getChildren-------------------

	@Test
	public void testGetChildrenProjectWithSourceFolders() throws Exception {

		// Create a logical package for packages with name "pack3"
		LogicalPackage cp= new LogicalPackage(fPack31);
		cp.add(fPack32);
		cp.add(fInternalPack3);

		// Create a logical package for default package
		LogicalPackage defaultCp= new LogicalPackage(fPackDefault1);
		defaultCp.add(fPackDefault2);
		defaultCp.add(fInternalPackDefault);

		Object[] expectedChildren= new Object[]{fPack21, fPack12, cp, defaultCp};
		Object[] children= fProvider.getChildren(fJProject2);
		assertTrue("Wrong children found for project", compareArrays(children, expectedChildren));//$NON-NLS-1$
	}


	@Test
	public void testGetChildrentMidLevelFragmentNotLogicalPackage() throws Exception {
		// Initialize map
		fProvider.getChildren(fJProject2);

		Object[] expectedChildren= new Object[]{fPack17};
		Object[] actualChildren= fProvider.getChildren(fPack12);
		assertTrue("Wrong children found for PackageFragment", compareArrays(actualChildren, expectedChildren));//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenBottomLevelFragment() throws Exception {
		Object[] expectedChildren= new Object[]{};
		Object[] actualChildren= fProvider.getChildren(fPack21);
		assertTrue("Wrong children found for PackageFragment", compareArrays(actualChildren, expectedChildren));//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenLogicalPackage() throws Exception {
		// Create a logical package for packages with name "pack3"
		LogicalPackage cp= new LogicalPackage(fPack31);
		cp.add(fPack32);
		cp.add(fInternalPack3);

		// Create a logical package for packages with name "pack3.pack4"
		LogicalPackage childCp1= new LogicalPackage(fPack41);
		childCp1.add(fPack42);
		childCp1.add(fInternalPack4);

		// Create a logical package for packages with name "pack3.pack5"
		LogicalPackage childCp2= new LogicalPackage(fPack51);
		childCp2.add(fPack52);
		childCp2.add(fInternalPack5);

		// Initialize Map
		fProvider.getChildren(fJProject2);

		Object[] expectedChildren= new Object[]{fPack81, childCp1, childCp2};
		Object[] actualChildren= fProvider.getChildren(cp);
		assertTrue("Wrong children found for project", compareArrays(actualChildren, expectedChildren)); //$NON-NLS-1$
	}

	@Test
	public void testGetChildrenLogicalPackage2() throws Exception {
		// Create a logical package for packages with name "pack3.pack4"
		LogicalPackage cp= new LogicalPackage(fPack41);
		cp.add(fPack42);
		cp.add(fInternalPack4);

		// Create a logical package for packages with name "pack3"
		LogicalPackage cp3= new LogicalPackage(fPack31);
		cp3.add(fPack32);
		cp3.add(fInternalPack3);

		fProvider.getChildren(fJProject2);
		fProvider.getChildren(cp3);

		Object[] expectedChildren= new Object[]{fPack91, fInternalPack10};
		Object[] actualChildren= fProvider.getChildren(cp);
		assertTrue("Wrong children found for project", compareArrays(actualChildren, expectedChildren));//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenMidLevelFragmentInArchive() throws Exception {
		// Initialize map
		fProvider.getChildren(fArchiveFragmentRoot);
		fProvider.getChildren(fPackJunit);

		Object[] expectedChildren= new Object[]{fPackJunitSamplesMoney};
		Object[] actualChildren= fProvider.getChildren(fPackJunitSamples);
		assertTrue("wrong children found for a NON bottom PackageFragment in PackageFragmentRoot Archive", compareArrays(actualChildren, expectedChildren));//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenBottomLevelFragmentInArchive() throws Exception {
		Object[] expectedChildren= new Object[]{};
		Object[] actualChildren= fProvider.getChildren(fPackJunitSamplesMoney);
		assertTrue("wrong children found for a bottom PackageFragment in PackageFragmentRoot Archive", compareArrays(actualChildren, expectedChildren));	//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenSourceFolder() throws Exception {
		Object[] expectedChildren = new Object[] {fPack21, fPack31, fPackDefault1};
		Object[] actualChildren = fProvider.getChildren(fRoot1);
		assertTrue("Wrong children found for PackageFragmentRoot", compareArrays(actualChildren, expectedChildren));//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenArchive() {
		Object[] expectedChildren= new Object[] {fPackJunit, fArchiveFragmentRoot.getPackageFragment("")};//$NON-NLS-1$
		Object[] actualChildren= fProvider.getChildren(fArchiveFragmentRoot);
		assertTrue("Wrong child found for PackageFragmentRoot Archive", compareArrays(actualChildren, expectedChildren));//$NON-NLS-1$
	}

	//---------------Get Parent Tests-----------------------------

	@Test
	public void testGetParentArchive() throws Exception {
		Object actualParent= fProvider.getParent(fArchiveFragmentRoot);
		assertNull("Wrong parent found for PackageFragmentRoot Archive", actualParent);//$NON-NLS-1$
	}

	@Test
	public void testGetParentMidLevelFragmentInArchive() throws Exception {
		// Initialize Map
		fProvider.getChildren(fArchiveFragmentRoot);
		fProvider.getChildren(fPackJunit);
		fProvider.getChildren(fPackJunitSamples);

		Object expectedParent= fPackJunitSamples;
		Object actualParent= fProvider.getParent(fPackJunitSamplesMoney);
		assertEquals("Wrong parent found for a NON top level PackageFragment in an Archive", expectedParent, actualParent);//$NON-NLS-1$
	}

	@Test
	public void testGetParentTopLevelFragmentInArchive() throws Exception {
		Object expectedParent= fPackJunit;
		Object actualParent= fProvider.getParent(fPackJunitSamples);
		assertEquals("Wrong parent found for a top level PackageFragment in an Archive", expectedParent, actualParent);	//$NON-NLS-1$
	}

	@Test
	public void testGetParentTopLevelLogicalPackage() throws Exception {
		LogicalPackage cp= new LogicalPackage(fPack31);
		cp.add(fPack32);
		cp.add(fInternalPack3);

		Object actualParent= fProvider.getParent(cp);
		assertEquals("Wrong parent found for a top level LogicalPackage", fJProject2, actualParent);//$NON-NLS-1$
	}

	@Test
	public void testGetParentPackageFragmentWithLogicalPackageParent() throws Exception {
		LogicalPackage cp= new LogicalPackage(fPack31);
		cp.add(fPack32);
		cp.add(fInternalPack3);

		// Initialize map
		fProvider.getChildren(fJProject2);

		Object parent= fProvider.getParent(fPack81);
		assertEquals("Wrong parent found for a top level LogicalPackage", cp, parent);//$NON-NLS-1$
	}

	@Test
	public void testGetParentOfLogicalPackagetWithLogicalPackageParent() throws Exception {
		LogicalPackage cp= new LogicalPackage(fPack41);
		cp.add(fPack42);
		cp.add(fInternalPack4);

		LogicalPackage parentCp= new LogicalPackage(fPack31);
		parentCp.add(fPack32);
		parentCp.add(fInternalPack3);

		// Initialize map
		fProvider.getChildren(fJProject2);

		Object parent= fProvider.getParent(cp);
		assertEquals("Wrong parent found for a top level LogicalPackage", parentCp, parent);//$NON-NLS-1$
	}

//	public void testGetParentWithPFRootFocus(){
//		//set up map
//		Object[] children= fProvider.getChildren(fRoot1);
//		for (int i= 0; i < children.length; i++) {
//			Object object= children[i];
//			fProvider.getChildren(object);
//		}
//
//		Object parent= fProvider.getParent(fPack41);
//		Object expectedParent= fPack31;
//		assertTrue("Wrong parent found for a mid level Fragment with Root Focus", expectedParent.equals(parent));		//$NON-NLS-1$
//	}

	@Test
	public void testGetParentPFwithProjectFocus(){
		for (Object object : fProvider.getChildren(fJProject2)) {
			fProvider.getChildren(object);
		}

		LogicalPackage cp= new LogicalPackage(fPack31);
		cp.add(fPack32);
		cp.add(fInternalPack3);

		Object parent= fProvider.getParent(fPack41);
		assertEquals("Wrong parent found for a mid level Fragment with Root Focus", cp, parent);		//$NON-NLS-1$
	}

	@Test
	public void testGetParentWithRootFocusAfterProjectFocus(){
		//set up map with project focus
		Object[] actualChildren= fProvider.getChildren(fJProject2);
		for (Object object : actualChildren) {
			fProvider.getChildren(object);
		}

		LogicalPackage cp= new LogicalPackage(fPack31);
		cp.add(fPack32);
		cp.add(fInternalPack3);

		//create a logical package for default package
		LogicalPackage defaultCp= new LogicalPackage(fPackDefault1);
		defaultCp.add(fPackDefault2);
		defaultCp.add(fInternalPackDefault);

		Object[] expectedChildren= new Object[]{fPack21, fPack12, cp, defaultCp};

		assertTrue("expected children of project with focus", compareArrays(actualChildren, expectedChildren));//$NON-NLS-1$

		//set up map with root focus
		for (Object object : fProvider.getChildren(fRoot1)) {
			fProvider.getChildren(object);
		}//		Object parent= fProvider.getParent(fPack41);
//		Object expectedParent= fPack31;
//		assertTrue("Wrong parent found for a mid level Fragment with Project Focus", expectedParent.equals(parent));//$NON-NLS-1$
	}

	@Test
	public void testGetParentTopLevelFragment() throws Exception {
		Object actualParent= fProvider.getParent(fPack21);
		Object expectedParent= fRoot1;
		assertEquals("Wrong parent found for a top level PackageFragment", expectedParent, actualParent); //$NON-NLS-1$

	}

	@Test
	public void testGetParentMidLevelFragment() throws Exception {
		Object expectedParent= fPack12;
		Object actualParent= fProvider.getParent(fPack17);
		assertEquals("Wrong parent found for a NON top level PackageFragment", expectedParent, actualParent); //$NON-NLS-1$

	}

	@Test
	public void testBug123424_1() throws Exception {
		IClasspathEntry[] rawClasspath= fJProject2.getRawClasspath();
		IClasspathEntry src1= rawClasspath[1];
		CPListElement element= CPListElement.createFromExisting(src1, fJProject2);
		element.setAttribute(CPListElement.INCLUSION, new IPath[] {new Path("pack3/pack5/")});
		fJProject2.setRawClasspath(new IClasspathEntry[] {rawClasspath[0], element.getClasspathEntry(), rawClasspath[2]}, null);
		Object[] expectedChildren= new Object[]{fPack12.getResource(), fPack32.getResource()};
		Object[] actualChildren= fProvider.getChildren(fRoot2);
		assertTrue("Wrong children found for project", compareArrays(actualChildren, expectedChildren));//$NON-NLS-1$
	}

	@Test
	public void testBug123424_2() throws Exception {
		IClasspathEntry[] rawClasspath= fJProject2.getRawClasspath();
		IClasspathEntry src1= rawClasspath[1];
		CPListElement element= CPListElement.createFromExisting(src1, fJProject2);
		element.setAttribute(CPListElement.INCLUSION, new IPath[] {new Path("pack3/pack5/")});
		fJProject2.setRawClasspath(new IClasspathEntry[] {rawClasspath[0], element.getClasspathEntry(), rawClasspath[2]}, null);
		Object[] expectedChildren= new Object[]{fPack42.getResource(), fPack52};
		Object[] actualChildren= fProvider.getChildren(fPack32.getResource());
		assertTrue("Wrong children found for project", compareArrays(actualChildren, expectedChildren));//$NON-NLS-1$
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


		//------------set up project #1 : External Jar and zip file-------

		IPackageFragmentRoot jdk= JavaProjectHelper.addVariableRTJar(fJProject1, "JRE_LIB_TEST", null, null);//$NON-NLS-1$
		assertNotNull("jdk not found", jdk);//$NON-NLS-1$


		//---Create zip file-------------------

		java.io.File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);

		assertNotNull("junit src not found", junitSrcArchive);//$NON-NLS-1$
		assertTrue("junit src not found", junitSrcArchive.exists());//$NON-NLS-1$

		fArchiveFragmentRoot= JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);//$NON-NLS-1$

		fPackJunit= fArchiveFragmentRoot.getPackageFragment("junit");//$NON-NLS-1$
		fPackJunitSamples= fArchiveFragmentRoot.getPackageFragment("junit.samples");//$NON-NLS-1$
		fPackJunitSamplesMoney= fArchiveFragmentRoot.getPackageFragment("junit.samples.money");//$NON-NLS-1$

		assertNotNull("creating fPackJunit", fPackJunit);//$NON-NLS-1$
		assertNotNull("creating fPackJunitSamples", fPackJunitSamples);//$NON-NLS-1$
		assertNotNull("creating fPackJunitSamplesMoney",fPackJunitSamplesMoney);//$NON-NLS-1$

		fPackJunitSamplesMoney.getCompilationUnit("IMoney.java");//$NON-NLS-1$
		fPackJunitSamplesMoney.getCompilationUnit("Money.java");//$NON-NLS-1$
		fPackJunitSamplesMoney.getCompilationUnit("MoneyBag.java");//$NON-NLS-1$
		fPackJunitSamplesMoney.getCompilationUnit("MoneyTest.java");//$NON-NLS-1$

		//java.io.File mylibJar= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);
		//assertTrue("lib not found", mylibJar != null && mylibJar.exists());//$NON-NLS-1$
		//JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(mylibJar.getPath()), null, null);

		//----------------Set up internal jar----------------------------
		File myInternalJar= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/compoundtest.jar"));//$NON-NLS-1$
		assertNotNull("lib not found", myInternalJar);//$NON-NLS-1$
		assertTrue("lib not found", myInternalJar.exists());//$NON-NLS-1$

		fInternalJarRoot= JavaProjectHelper.addLibraryWithImport(fJProject2, Path.fromOSString(myInternalJar.getPath()), null, null);

		fInternalPackDefault= fInternalJarRoot.getPackageFragment("");//$NON-NLS-1$
		fInternalPack3= fInternalJarRoot.getPackageFragment("pack3");//$NON-NLS-1$
		fInternalPack4= fInternalJarRoot.getPackageFragment("pack3.pack4");//$NON-NLS-1$
		fInternalPack5= fInternalJarRoot.getPackageFragment("pack3.pack5");//$NON-NLS-1$
		 fInternalJarRoot.getPackageFragment("pack3.pack5.pack6");//$NON-NLS-1$
		fInternalPack10= fInternalJarRoot.getPackageFragment("pack3.pack4.pack10");//$NON-NLS-1$

		//-----------------Set up source folder--------------------------

		fRoot2= JavaProjectHelper.addSourceContainer(fJProject2, "src2");//$NON-NLS-1$
		fPackDefault2= fRoot2.createPackageFragment("",true,null);//$NON-NLS-1$
		fPack12= fRoot2.createPackageFragment("pack1", true, null);//$NON-NLS-1$
		fPack17= fRoot2.createPackageFragment("pack1.pack7",true,null);//$NON-NLS-1$
		fPack32= fRoot2.createPackageFragment("pack3",true,null);//$NON-NLS-1$
		fPack42= fRoot2.createPackageFragment("pack3.pack4", true,null);//$NON-NLS-1$
		fPack52= fRoot2.createPackageFragment("pack3.pack5",true,null);//$NON-NLS-1$
		fPack62= fRoot2.createPackageFragment("pack3.pack5.pack6", true, null);//$NON-NLS-1$

		fPack12.createCompilationUnit("Object.java", "", true, null);//$NON-NLS-1$//$NON-NLS-2$
		fPack62.createCompilationUnit("Object.java","", true, null);//$NON-NLS-1$//$NON-NLS-2$


		//set up project #2: file system structure with in a source folder

	//	JavaProjectHelper.addVariableEntry(fJProject2, new Path("JRE_LIB_TEST"), null, null);

		//----------------Set up source folder--------------------------

		fRoot1= JavaProjectHelper.addSourceContainer(fJProject2, "src1"); //$NON-NLS-1$
		fPackDefault1= fRoot1.createPackageFragment("",true,null); //$NON-NLS-1$
		fPack21= fRoot1.createPackageFragment("pack2", true, null);//$NON-NLS-1$
		fPack31= fRoot1.createPackageFragment("pack3",true,null);//$NON-NLS-1$
		fPack41= fRoot1.createPackageFragment("pack3.pack4", true,null);//$NON-NLS-1$
		fPack91= fRoot1.createPackageFragment("pack3.pack4.pack9",true,null);//$NON-NLS-1$
		fPack51= fRoot1.createPackageFragment("pack3.pack5",true,null);//$NON-NLS-1$
		fPack61= fRoot1.createPackageFragment("pack3.pack5.pack6", true, null);//$NON-NLS-1$
		fPack81= fRoot1.createPackageFragment("pack3.pack8", true, null);//$NON-NLS-1$

		fPack21.createCompilationUnit("Object.java", "", true, null);//$NON-NLS-1$//$NON-NLS-2$
		fPack61.createCompilationUnit("Object.java","", true, null);//$NON-NLS-1$//$NON-NLS-2$

		//set up the mock view
		setUpMockView();
	}

	public void setUpMockView() throws Exception {

		fWorkbench= PlatformUI.getWorkbench();
		assertNotNull(fWorkbench);

		page= fWorkbench.getActiveWorkbenchWindow().getActivePage();
		assertNotNull(page);

		MockPluginView.setListState(false);
		IViewPart myPart= page.showView("org.eclipse.jdt.ui.tests.browsing.MockPluginView");//$NON-NLS-1$
		if (myPart instanceof MockPluginView) {
			fMyPart= (MockPluginView) myPart;
			fProvider= (ITreeContentProvider)fMyPart.getTreeViewer().getContentProvider();
			//create map and set listener
			fProvider.inputChanged(null,null,fJProject2);
		} else {
			fail("Unable to get view");//$NON-NLS-1$
		}

		assertNotNull(fProvider);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
		JavaProjectHelper.delete(fJProject2);
		fProvider.inputChanged(null, null, null);
		page.hideView(fMyPart);

		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(true);
	}

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
			} else if (child instanceof LogicalPackage){
				if(!contains((LogicalPackage)child, expectedChildren))
					return false;
			}
		}

		return true;
	}

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
