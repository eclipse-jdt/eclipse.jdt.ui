/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.ui.tests.packageview;


import java.util.zip.ZipFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

/**
 * Tests for the PackageExplorerContentProvider.
 * 
 * @since 2.1
 */
public class ContentProviderTests1 extends TestCase {



	public static Test suite() {
		TestSuite suite= new TestSuite("Tests for content provider - part 1"); //$NON-NLS-1$
		//$JUnit-BEGIN$
	   suite.addTestSuite(ContentProviderTests1.class);
		//$JUnit-END$
		return suite;
	}

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

	private IPackageFragment fPack5;
	private IPackageFragment fPack6;
	private IFile fFile1;
	private IFile fFile2;
	private ICompilationUnit fCUIMoney;
	private ICompilationUnit fCUMoney;
	private ICompilationUnit fCUMoneyBag;
	private ICompilationUnit fCUMoneyTest;
	
	private ICompilationUnit fCU1;
	private ICompilationUnit fCU2;
	
	private IWorkbenchPage page;
	private ICompilationUnit fCUAllTests;
	private ICompilationUnit fCUVectorTest;
	private ICompilationUnit fCUSimpleTest;
	
	public ContentProviderTests1(String name) {
		super(name);
	}
	
	//---------Test for getChildren-------------------

	public void testGetChildrenProjectWithSourceFolders() throws Exception{
		System.out.println("Testing getChildren of Project with source folders");
		Object[] expectedChildren= new Object[]{fRoot1, fFile1, fFile2};
		Object[] children= fProvider.getChildren(fJProject2);
		assertTrue("Wrong children found for project", compareArrays(children, expectedChildren));
	}
	
	
	public void testGetChildrentMidLevelFragment() throws Exception{
		System.out.println("Testing getChildren of a Non bottom level PackageFragment");
		Object[] expectedChildren= new Object[]{fPack4, fPack5};
		Object[] children= fProvider.getChildren(fPack3);
		assertTrue("Wrong children found for PackageFragment",compareArrays(children, expectedChildren));
	}
	
	public void testGetChildrenBottomLevelFragment() throws Exception{
		System.out.println("Testing getChildren of a bottom level PackageFragment with CU");
		Object[] expectedChildren= new Object[]{fCU1};
		Object[] children= fProvider.getChildren(fPack2);
		assertTrue("Wrong children found for PackageFragment",compareArrays(children, expectedChildren));
	}
	
//	public void testGetChildrenBottomLevelFragmentWithResource() throws Exception {
//		System.out.println("Testing getChildren of a bottom level PackageFragment with resource");
//		Object[] expectedChildren = new Object[] { fFile3 };
//		Object[] children = fProvider.getChildren(fPack1);
//		boolean assertion = false;
//		if (expectedChildren.length == children.length) {
//			if (children[0] instanceof IFile) {
//				IFile file = (IFile) children[0];
//				if (file.getName().equals(fFile3.getName()))
//					assertion = true;
//			}
//		}
//		assertTrue("Wrong children found for PackageFragment", assertion);
//	}

	public void testGetChildrenMidLevelFragmentInArchive() throws Exception{
		System.out.println("Testing getChildren of a Non bottom level PackageFragment in a PackageFragmentRoot Archive");
		Object[] expectedChildren= new Object[]{fPackJunitSamplesMoney, fCUAllTests, fCUSimpleTest, fCUVectorTest };
		Object[] children= fProvider.getChildren(fPackJunitSamples);
	}

	public void testGetChildrenBottomLevelFragmentInArchive() throws Exception{
		System.out.println("Testing getChildren of a bottom level PackageFragment in a PackageFragmentRoot Archive");
		Object[] expectedChildren= new Object[]{fCUIMoney, fCUMoney, fCUMoneyBag, fCUMoneyTest};
		Object[] children= fProvider.getChildren(fPackJunitSamplesMoney);
		assertTrue("wrong children found for a bottom PackageFragment in PackageFragmentRoot Archive", compareArrays(children, expectedChildren));	
	}
	
	public void testGetChildrenSourceFolder() throws Exception {
		System.out.println("Testing getChildren of PackageFragmentRoot NOT Archive");
		Object[] expectedChildren = new Object[] { fPack1, fPack2, fPack3, fRoot1.getPackageFragment("")};
		Object[] children = fProvider.getChildren(fRoot1);
		assertTrue("Wrong children found for PackageFragmentRoot", compareArrays(children, expectedChildren));
	}
	
	public void testGetChildrenArchive(){
		System.out.println("Testing getChildren of PackageFragmentRoot Archive");
		Object[] expectedChildren= new Object[]{fPackJunit, fArchiveFragmentRoot.getPackageFragment("")};
		Object[] children= fProvider.getChildren(fArchiveFragmentRoot);
		assertTrue("Wrong child found for PackageFragmentRoot Archive", compareArrays(children,expectedChildren));
	}
	
	//---------------Get Parent Tests-----------------------------
	
	public void testGetParentArchive() throws Exception{
		System.out.println("Testing getParent of PackageFragmentRoot Archive");
		Object parent= fProvider.getParent(fArchiveFragmentRoot);
		assertTrue("Wrong parent found for PackageFragmentRoot Archive", parent==null);
	}

	public void testGetParentMidLevelFragmentInArchive() throws Exception{
		System.out.println("Testing getParent of a NON top level PackageFragment in an Archive");
		Object expectedParent= fPackJunitSamples;
		Object parent= fProvider.getParent(fPackJunitSamplesMoney);
		assertTrue("Wrong parent found for a NON top level PackageFragment in an Archive", expectedParent.equals(parent));
	}	
	
	public void testGetParentTopLevelFragmentInArchive() throws Exception{
		System.out.println("Testing getParent of a top level PackageFragment in an Archive");
		Object expectedParent= fPackJunit;
		Object parent= fProvider.getParent(fPackJunitSamples);
		assertTrue("Wrong parent found for a top level PackageFragment in an Archive", expectedParent.equals(parent));	
	}
	
	public void testGetParentTopLevelFragment() throws Exception{
		System.out.println("Testing getParent of a top level PackageFragment");
		Object expectedParent= fRoot1;
		Object parent= fProvider.getParent(fPack3);
		assertTrue("Wrong parent found for a top level PackageFragment", expectedParent.equals(parent));
	}
	
	public void testGetParentMidLevelFragment() throws Exception{
		System.out.println("Testing getParent of a NON top level PackageFragment");
		Object expectedParent= fPack3;
		Object parent= fProvider.getParent(fPack4);
		assertTrue("Wrong parent found for a NON top level PackageFragment", expectedParent.equals(parent));
	}
	

	/**
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		fWorkspace= ResourcesPlugin.getWorkspace();
		assertNotNull(fWorkspace);	
		
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
		
		assertNotNull("project1 null", fJProject1);
		assertNotNull("project2 null", fJProject2);
		
		fJProject1.setRawClasspath(new IClasspathEntry[0], null);

		Object[] resource= fJProject2.getNonJavaResources();
		for (int i = 0; i < resource.length; i++) {
			Object object = resource[i];
			if(object instanceof IFile){
				IFile file = (IFile) object;
				if(".classpath".equals(file.getName()))
					fFile1= file;
				else if (".project".equals(file.getName()))
					fFile2= file;
			}
		}
		assertNotNull(fFile1);
		assertNotNull(fFile2);
		
		//set up project #1 : External Jar and zip file
		IPackageFragmentRoot jdk= JavaProjectHelper.addVariableRTJar(fJProject1, "JRE_LIB_TEST", null, null);
		assertTrue("jdk not found", jdk != null);

		java.io.File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC);
		
		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());
		ZipFile zipfile= new ZipFile(junitSrcArchive);
		fArchiveFragmentRoot= JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", zipfile);
		
		fPackJunit= fArchiveFragmentRoot.getPackageFragment("junit");
		fPackJunitSamples= fArchiveFragmentRoot.getPackageFragment("junit.samples");
		fPackJunitSamplesMoney= fArchiveFragmentRoot.getPackageFragment("junit.samples.money");
		
		assertNotNull("creating fPackJunit", fPackJunit);
		assertNotNull("creating fPackJunitSamples", fPackJunitSamples);
		assertNotNull("creating fPackJunitSamplesMoney",fPackJunitSamplesMoney);
		
		fCUIMoney= fPackJunitSamplesMoney.getCompilationUnit("IMoney.java");
		fCUMoney= fPackJunitSamplesMoney.getCompilationUnit("Money.java");
		fCUMoneyBag= fPackJunitSamplesMoney.getCompilationUnit("MoneyBag.java");
		fCUMoneyTest= fPackJunitSamplesMoney.getCompilationUnit("MoneyTest.java");
		
		fCUAllTests= fPackJunitSamples.getCompilationUnit("AllTests.java");
		fCUVectorTest= fPackJunitSamples.getCompilationUnit("VectorTest.java");
		fCUSimpleTest= fPackJunitSamples.getCompilationUnit("SimpleTest.java");
		
		java.io.File mylibJar= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);
		assertTrue("lib not found", mylibJar != null && mylibJar.exists());
		JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(mylibJar.getPath()), null, null);

		//set up project #2: file system structure with in a source folder

	//	JavaProjectHelper.addVariableEntry(fJProject2, new Path("JRE_LIB_TEST"), null, null);

		fRoot1= JavaProjectHelper.addSourceContainer(fJProject2, "src1");
		fPack1= fRoot1.createPackageFragment("pack1", true, null);
		fPack2= fRoot1.createPackageFragment("pack2", true, null);
		fPack3= fRoot1.createPackageFragment("pack3",true,null);
		fPack4= fRoot1.createPackageFragment("pack3.pack4", true,null);
		fPack5= fRoot1.createPackageFragment("pack3.pack5",true,null);
		fPack6= fRoot1.createPackageFragment("pack3.pack5.pack6", true, null);
		
		fCU1= fPack2.createCompilationUnit("Object.java", "", true, null);
		fCU2= fPack6.createCompilationUnit("Object.java","", true, null);
		
		//set up the mock view
		setUpMockView();
	}
	
	public void setUpMockView() throws Exception{
		
		fWorkbench= PlatformUI.getWorkbench();
		assertNotNull(fWorkbench);
		
		page= fWorkbench.getActiveWorkbenchWindow().getActivePage();
		assertNotNull(page);
		
		//just testing to make sure my part can be created
		IViewPart myPart= new MockPluginView();
		assertNotNull(myPart);
		
		myPart= page.showView("org.eclipse.jdt.ui.tests.packageview.MockPluginView");
		if (myPart instanceof MockPluginView) {
			fMyPart= (MockPluginView) myPart;
			fMyPart.setFolding(false);
			fProvider= (ITreeContentProvider)fMyPart.getTreeViewer().getContentProvider();
		}else assertTrue("Unable to get view",false);
	
		assertNotNull(fProvider);		
	}

	/**
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		fArchiveFragmentRoot.close();		
		JavaProjectHelper.delete(fJProject1);
		JavaProjectHelper.delete(fJProject2);
		page.hideView(fMyPart);
		fMyPart.dispose();
		super.tearDown();
	}
	
	/**
	 * Method compareArrays. Both arrays must be of IPackageFragments or compare will fail.
	 * @param children
	 * @param expectedChildren
	 * @return boolean
	 */
	private boolean compareArrays(Object[] children, Object[] expectedChildren) {
		if(children.length!=expectedChildren.length)
			return false;
		for (int i= 0; i < children.length; i++) {
			Object child= children[i];
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
	 * @param res
	 * @param expectedChildren
	 * @return boolean
	 */
	private boolean contains(IResource res, Object[] expectedChildren) {
		for (int i= 0; i < expectedChildren.length; i++) {
			Object object= expectedChildren[i];
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
	 * @param fragment
	 * @param expectedChildren
	 * @return boolean
	 */
	private boolean contains(IJavaElement fragment, Object[] expectedChildren) {
		for (int i= 0; i < expectedChildren.length; i++) {
			Object object= expectedChildren[i];
			if (object instanceof IJavaElement) {
				IJavaElement expfrag= (IJavaElement) object;
				if(expfrag.equals(fragment))
					return true;
			}
		}
		return false;
	}

}
