package org.eclipse.jdt.refactoring.tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.core.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.core.refactoring.reorg.MoveRefactoring;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

public class ReorgTests extends RefactoringTest {

	private static final Class clazz= ReorgTests.class;
	private static final String REFACTORING_PATH= "ReorgTests/";
	
	public ReorgTests(String name) {
		super(name);
	}
	
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), clazz, args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(noSetupSuite());
		return new MySetup(suite);
	}
	
	public static Test noSetupSuite() {
		return new TestSuite(clazz);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}	
	private ICompilationUnit getCu(String cuPath, IPackageFragment pack, String cuName)throws Exception{
		return createCU(pack, cuName, getFileContents(getTestPath() + cuPath));
	}
	
	private IPackageFragmentRoot getRtJar()throws Exception{
		IJavaProject proj= getPackageP().getJavaProject();
		IPackageFragmentRoot[] roots= proj.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++){
			if (roots[i].isReadOnly())
				return roots[i];
		}
		assertTrue(false);//not expected
		return null;
	}
	
	private IFolder createFolder(String name) throws Exception {
		IProject project= (IProject)getPackageP().getJavaProject().getCorrespondingResource();	
		IFolder newFolder= project.getFolder(name);
		newFolder.create(false, true, null);
		return newFolder;
	}
	
	private IFile createFile(String fileName) throws Exception {
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();	
		IFile file= folder.getFile(fileName);
		assertTrue("should not exist", ! file.exists());
		file.create(getFileInputStream(getTestPath() + fileName), true, new NullProgressMonitor());
		assertTrue("should exist", file.exists());
		return file;
	}
	
	//--- activation tests 
	private void checkActivation(List elements, boolean expected)throws Exception{
		CopyRefactoring ref= new CopyRefactoring(elements);
		assertEquals("copy", expected, ref.checkActivation(new NullProgressMonitor()).isOK());		
		
		MoveRefactoring moveRef= new MoveRefactoring(elements, fgChangeCreator);
		assertEquals("move", expected, moveRef.checkActivation(new NullProgressMonitor()).isOK());		
	}
	
	public void testActivation0() throws Exception{
		checkActivation(new ArrayList(0), false);
	}
	
	public void testActivation1() throws Exception{
		List elements= new ArrayList();
		ICompilationUnit cu= getCu("A.java", getPackageP(), "A.java");
		elements.add(cu);
		checkActivation(elements, true);		
		cu.delete(false, null);
	}
	
	public void testActivation2() throws Exception{
		List elements= new ArrayList();
		ICompilationUnit cu= getCu("A.java", getPackageP(), "A.java");
		elements.add(cu);
		elements.add(getPackageP());
		checkActivation(elements, false);
		cu.delete(false, null);
	}

	public void testActivation3() throws Exception{
		List elements= new ArrayList();		
		IPackageFragment p1= getRoot().createPackageFragment("p1", false, null);
		ICompilationUnit cu= getCu("p1/A.java", p1, "A.java");
		elements.add(cu);
		elements.add(p1);
		checkActivation(elements, false);
		cu.delete(false, null);		
		p1.delete(false, null);
	}
	
	public void testActivation4() throws Exception{
		List elements= new ArrayList();
		ICompilationUnit cu= getCu("A.java", getPackageP(), "A.java");
		ICompilationUnit cu2= getCu("B.java", getPackageP(), "B.java");
		
		elements.add(cu);
		elements.add(cu2);
		checkActivation(elements, true);
		cu2.delete(false, null);
		cu.delete(false, null);
	}
	
	public void testActivation5() throws Exception{
		List elements= new ArrayList();
		elements.add(getPackageP().getJavaProject());
		checkActivation(elements, false);
	}

	public void testActivation6() throws Exception{
		List elements= new ArrayList();
		IJavaProject proj= getPackageP().getJavaProject();
		IPackageFragmentRoot[] roots= proj.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++){
			if (roots[i].isReadOnly())
				elements.add(roots[i]);
		}
		checkActivation(elements, false);
	}

	public void testActivation7() throws Exception{
		List elements= new ArrayList();
		IJavaProject proj= getPackageP().getJavaProject();
		IPackageFragmentRoot root= proj.getPackageFragmentRoot(MySetup.CONTAINER);
		elements.add(root);
		checkActivation(elements, false);
	}
	
	public void testActivation8() throws Exception{
		List elements= new ArrayList();
		IFile file= createFile("launcher.gif");
		elements.add(file);
		checkActivation(elements, true);
		file.delete(false, null);
	}
	
	public void testActivation9() throws Exception{
		List elements= new ArrayList();
		IFolder folder= createFolder("newF");
		elements.add(folder);
		checkActivation(elements, true);
		folder.delete(false, null);
	}	
	
	public void testActivation10() throws Exception{
		List elements= new ArrayList();
		IFolder folder= createFolder("newF");
		ICompilationUnit cu= getCu("A.java", getPackageP(), "A.java");
		elements.add(folder);
		elements.add(cu);
		checkActivation(elements, false);
		cu.delete(false, null);
		folder.delete(false, null);
	}	

	public void testActivation11() throws Exception{
		List elements= new ArrayList();
		IPackageFragment p1= getRoot().createPackageFragment("p1", false, null);
		elements.add(p1);
		checkActivation(elements, true);
		p1.delete(false, null);
	}		
	
	//--- destination tests
	
	public void testDestination0() throws Exception{
		List elements= new ArrayList();
		
		ICompilationUnit cu= getCu("A.java", getPackageP(), "A.java");
		elements.add(cu);
		
		IPackageFragment p1= getRoot().createPackageFragment("p1", false, null);
		IFolder folder= createFolder("f");
		
		CopyRefactoring copyRef= new CopyRefactoring(elements);
		assertEquals("copy0", false, copyRef.isValidDestination(cu));
		assertEquals("copy1", true, copyRef.isValidDestination(p1));
		assertEquals("copy2", true, copyRef.isValidDestination(getPackageP()));
		assertEquals("copy3", true, copyRef.isValidDestination(getRoot()));
		assertEquals("copy4", true, copyRef.isValidDestination(getRoot().getJavaProject()));
		assertEquals("copy5", false, copyRef.isValidDestination(getRtJar()));
		assertEquals("copy6", true, copyRef.isValidDestination(folder));

		MoveRefactoring moveRef= new MoveRefactoring(elements, fgChangeCreator);
		assertEquals("moveRef0", false, moveRef.isValidDestination(cu));
		assertEquals("moveRef1", true, moveRef.isValidDestination(p1));
		assertEquals("moveRef2", false, moveRef.isValidDestination(getPackageP()));
		assertEquals("moveRef3", true, moveRef.isValidDestination(getRoot()));
		assertEquals("moveRef4", true, moveRef.isValidDestination(getRoot().getJavaProject()));
		assertEquals("moveRef5", false, moveRef.isValidDestination(getRtJar()));
		assertEquals("moveRef6", true, moveRef.isValidDestination(folder));
		
		folder.delete(false, null);
		p1.delete(false, null);
		cu.delete(false, null);
	}
	
	public void testDestination2() throws Exception{
		List elements= new ArrayList();
		ICompilationUnit cu= getCu("A.java", getPackageP(), "A.java");
		ICompilationUnit cu2= getCu("B.java", getPackageP(), "B.java");
		IFolder folder= createFolder("f");
		
		elements.add(cu);
		elements.add(cu2);
		
		IPackageFragment p1= getRoot().createPackageFragment("p1", false, null);
		CopyRefactoring copyRef= new CopyRefactoring(elements);
		assertEquals("copy0", false, copyRef.isValidDestination(cu));
		assertEquals("copy0a", false, copyRef.isValidDestination(cu2));
		assertEquals("copy1", true, copyRef.isValidDestination(p1));
		assertEquals("copy2", true, copyRef.isValidDestination(getPackageP()));
		assertEquals("copy3", true, copyRef.isValidDestination(getRoot()));
		assertEquals("copy4", true, copyRef.isValidDestination(getRoot().getJavaProject()));
		assertEquals("copy5", false, copyRef.isValidDestination(getRtJar()));
		assertEquals("copy6", true, copyRef.isValidDestination(folder));
		
		MoveRefactoring moveRef= new MoveRefactoring(elements, fgChangeCreator);
		assertEquals("moveRef0", false, moveRef.isValidDestination(cu));
		assertEquals("moveRef0a", false, moveRef.isValidDestination(cu2));
		assertEquals("moveRef1", true, moveRef.isValidDestination(p1));
		assertEquals("moveRef2", false, moveRef.isValidDestination(getPackageP()));
		assertEquals("moveRef3", true, moveRef.isValidDestination(getRoot()));
		assertEquals("moveRef4", true, moveRef.isValidDestination(getRoot().getJavaProject()));
		assertEquals("moveRef5", false, moveRef.isValidDestination(getRtJar()));
		assertEquals("moveRef6", true, moveRef.isValidDestination(folder));
		
		folder.delete(false, null);	
		p1.delete(false, null);
		cu.delete(false, null);
		cu2.delete(false, null);
	}

	public void testDestination3() throws Exception{
		List elements= new ArrayList();
		ICompilationUnit cu= getCu("A.java", getPackageP(), "A.java");
		ICompilationUnit cu2= getCu("B.java", getPackageP(), "B.java");
		IFile file= createFile("launcher.gif");
		elements.add(cu);
		elements.add(cu2);
		elements.add(file);
		IFolder folder= createFolder("f");
		
		IPackageFragment p1= getRoot().createPackageFragment("p1", false, null);
		CopyRefactoring copyRef= new CopyRefactoring(elements);
		assertEquals("copy0", false, copyRef.isValidDestination(cu));
		assertEquals("copy0a", false, copyRef.isValidDestination(cu2));
		assertEquals("copy1", true, copyRef.isValidDestination(p1));
		assertEquals("copy2", true, copyRef.isValidDestination(getPackageP()));
		assertEquals("copy3", true, copyRef.isValidDestination(getRoot()));
		assertEquals("copy4", true, copyRef.isValidDestination(getRoot().getJavaProject()));
		assertEquals("copy5", false, copyRef.isValidDestination(getRtJar()));
		assertEquals("copy6", true, copyRef.isValidDestination(folder));
		assertEquals("copy7", false, copyRef.isValidDestination(file));
		
		MoveRefactoring moveRef= new MoveRefactoring(elements, fgChangeCreator);
		assertEquals("moveRef0", false, moveRef.isValidDestination(cu));
		assertEquals("moveRef0a", false, moveRef.isValidDestination(cu2));
		assertEquals("moveRef1", true, moveRef.isValidDestination(p1));
		assertEquals("moveRef2", false, moveRef.isValidDestination(getPackageP()));
		assertEquals("moveRef3", true, moveRef.isValidDestination(getRoot()));
		assertEquals("moveRef4", true, moveRef.isValidDestination(getRoot().getJavaProject()));
		assertEquals("moveRef5", false, moveRef.isValidDestination(getRtJar()));
		assertEquals("moveRef6", true, moveRef.isValidDestination(folder));
		assertEquals("moveRef7", false, moveRef.isValidDestination(file));
		
		file.delete(false, null);
		folder.delete(false, null);	
		p1.delete(false, null);
		cu.delete(false, null);
		cu2.delete(false, null);
	}
	
}

