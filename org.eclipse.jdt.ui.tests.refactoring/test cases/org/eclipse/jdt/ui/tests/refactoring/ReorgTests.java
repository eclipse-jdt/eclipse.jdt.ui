package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ICopyQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.MoveRefactoring;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

public class ReorgTests extends RefactoringTest {

	private static final Class clazz= ReorgTests.class;
	private static final String REFACTORING_PATH= "ReorgTests/";
	
	public ReorgTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
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
		CopyRefactoring ref= new CopyRefactoring(elements, new MockCopyQueries());
		assertEquals("copy", expected, ref.checkActivation(new NullProgressMonitor()).isOK());		
		
		MoveRefactoring moveRef= new MoveRefactoring(elements, JavaPreferencesSettings.getCodeGenerationSettings());
		assertEquals("move", expected, moveRef.checkActivation(new NullProgressMonitor()).isOK());		
	}
	
	public void testActivation0() throws Exception{
		checkActivation(new ArrayList(0), false);
	}
	
	public void testActivation1() throws Exception{
		List elements= new ArrayList();
		ICompilationUnit cu= getCu("A.java", getPackageP(), "A.java");
		try{
			elements.add(cu);
			checkActivation(elements, true);	
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}	
	}
	
	public void testActivation2() throws Exception{
		List elements= new ArrayList();
		ICompilationUnit cu= getCu("A.java", getPackageP(), "A.java");
		elements.add(cu);
		elements.add(getPackageP());
		checkActivation(elements, false);
		performDummySearch();
		cu.delete(false, null);
	}

	public void testActivation3() throws Exception{
		List elements= new ArrayList();		
		IPackageFragment p1= getRoot().createPackageFragment("p1", false, null);
		ICompilationUnit cu= getCu("p1/A.java", p1, "A.java");
		elements.add(cu);
		elements.add(p1);
		checkActivation(elements, false);
		performDummySearch();
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
		performDummySearch();
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
		performDummySearch();
		file.delete(false, null);
	}
	
	public void testActivation9() throws Exception{
		List elements= new ArrayList();
		IFolder folder= createFolder("newF");
		elements.add(folder);
		checkActivation(elements, true);
		performDummySearch();
		folder.delete(false, null);
	}	
	
	public void testActivation10() throws Exception{
		List elements= new ArrayList();
		IFolder folder= createFolder("newF");
		ICompilationUnit cu= getCu("A.java", getPackageP(), "A.java");
		elements.add(folder);
		elements.add(cu);
		checkActivation(elements, false);
		performDummySearch();
		cu.delete(false, null);
		folder.delete(false, null);
	}	

	public void testActivation11() throws Exception{
		List elements= new ArrayList();
		IPackageFragment p1= getRoot().createPackageFragment("p1", false, null);
		elements.add(p1);
		checkActivation(elements, true);
		performDummySearch();
		p1.delete(false, null);
	}		
	
	//--- destination tests
	
	public void testDestination0() throws Exception{
		ICompilationUnit cu= null;
		IFolder folder= null;
		IPackageFragment p1= null;
		
		try{
			List elements= new ArrayList();
			
			cu= getCu("A.java", getPackageP(), "A.java");
			elements.add(cu);
			
			p1= getRoot().createPackageFragment("p1", false, null);
			folder= createFolder("f");
			
			CopyRefactoring copyRef= new CopyRefactoring(elements, new MockCopyQueries());
			assertEquals("copy0", true, copyRef.isValidDestination(cu));
			assertEquals("copy1", true, copyRef.isValidDestination(p1));
			assertEquals("copy2", true, copyRef.isValidDestination(getPackageP()));
			assertEquals("copy3", true, copyRef.isValidDestination(getRoot()));
			assertEquals("copy4", true, copyRef.isValidDestination(getRoot().getJavaProject()));
			assertEquals("copy5", false, copyRef.isValidDestination(getRtJar()));
			assertEquals("copy6", true, copyRef.isValidDestination(folder));
	
			MoveRefactoring moveRef= new MoveRefactoring(elements, JavaPreferencesSettings.getCodeGenerationSettings());
			assertEquals("moveRef0", false, moveRef.isValidDestination(cu));
			assertEquals("moveRef1", true, moveRef.isValidDestination(p1));
			assertEquals("moveRef2", false, moveRef.isValidDestination(getPackageP()));
			assertEquals("moveRef3", true, moveRef.isValidDestination(getRoot()));
			assertEquals("moveRef4", true, moveRef.isValidDestination(getRoot().getJavaProject()));
			assertEquals("moveRef5", false, moveRef.isValidDestination(getRtJar()));
			assertEquals("moveRef6", true, moveRef.isValidDestination(folder));
		
		} finally{
			performDummySearch();
			folder.delete(false, null);
			p1.delete(false, null);
			cu.delete(false, null);
		}
	}
	
	public void testDestination2() throws Exception{
		ICompilationUnit cu= null;
		ICompilationUnit cu2= null;
		IFolder folder= null;
		IPackageFragment p1= null;
		
		try{
			List elements= new ArrayList();
			cu= getCu("A.java", getPackageP(), "A.java");
			cu2= getCu("B.java", getPackageP(), "B.java");
			folder= createFolder("f");
			
			elements.add(cu);
			elements.add(cu2);
			
			p1= getRoot().createPackageFragment("p1", false, null);
			CopyRefactoring copyRef= new CopyRefactoring(elements, new MockCopyQueries());
			assertEquals("copy0", true, copyRef.isValidDestination(cu));
			assertEquals("copy0a", true, copyRef.isValidDestination(cu2));
			assertEquals("copy1", true, copyRef.isValidDestination(p1));
			assertEquals("copy2", true, copyRef.isValidDestination(getPackageP()));
			assertEquals("copy3", true, copyRef.isValidDestination(getRoot()));
			assertEquals("copy4", true, copyRef.isValidDestination(getRoot().getJavaProject()));
			assertEquals("copy5", false, copyRef.isValidDestination(getRtJar()));
			assertEquals("copy6", true, copyRef.isValidDestination(folder));
			
			MoveRefactoring moveRef= new MoveRefactoring(elements, JavaPreferencesSettings.getCodeGenerationSettings());
			assertEquals("moveRef0", false, moveRef.isValidDestination(cu));
			assertEquals("moveRef0a", false, moveRef.isValidDestination(cu2));
			assertEquals("moveRef1", true, moveRef.isValidDestination(p1));
			assertEquals("moveRef2", false, moveRef.isValidDestination(getPackageP()));
			assertEquals("moveRef3", true, moveRef.isValidDestination(getRoot()));
			assertEquals("moveRef4", true, moveRef.isValidDestination(getRoot().getJavaProject()));
			assertEquals("moveRef5", false, moveRef.isValidDestination(getRtJar()));
			assertEquals("moveRef6", true, moveRef.isValidDestination(folder));

		}finally{		
			performDummySearch();
			folder.delete(false, null);	
			p1.delete(false, null);
			cu.delete(false, null);
			cu2.delete(false, null);
		}
	}

	public void testDestination3() throws Exception{
		ICompilationUnit cu= null;
		ICompilationUnit cu2= null;
		IFile file= null;
		IFolder folder= null;
		IPackageFragment p1= null;
		try{
			List elements= new ArrayList();
			cu= getCu("A.java", getPackageP(), "A.java");
			cu2= getCu("B.java", getPackageP(), "B.java");
			file= createFile("launcher.gif");
			elements.add(cu);
			elements.add(cu2);
			elements.add(file);
			folder= createFolder("f");
			
			p1= getRoot().createPackageFragment("p1", false, null);
			CopyRefactoring copyRef= new CopyRefactoring(elements, new MockCopyQueries());
			assertEquals("copy0", true, copyRef.isValidDestination(cu));
			assertEquals("copy0a", true, copyRef.isValidDestination(cu2));
			assertEquals("copy1", true, copyRef.isValidDestination(p1));
			assertEquals("copy2", true, copyRef.isValidDestination(getPackageP()));
			assertEquals("copy3", true, copyRef.isValidDestination(getRoot()));
			assertEquals("copy4", true, copyRef.isValidDestination(getRoot().getJavaProject()));
			assertEquals("copy5", false, copyRef.isValidDestination(getRtJar()));
			assertEquals("copy6", true, copyRef.isValidDestination(folder));
			assertEquals("copy7", true, copyRef.isValidDestination(file));
			
			MoveRefactoring moveRef= new MoveRefactoring(elements, JavaPreferencesSettings.getCodeGenerationSettings());
			assertEquals("moveRef0", false, moveRef.isValidDestination(cu));
			assertEquals("moveRef0a", false, moveRef.isValidDestination(cu2));
			assertEquals("moveRef1", true, moveRef.isValidDestination(p1));
			assertEquals("moveRef2", false, moveRef.isValidDestination(getPackageP()));
			assertEquals("moveRef3", true, moveRef.isValidDestination(getRoot()));
			assertEquals("moveRef4", true, moveRef.isValidDestination(getRoot().getJavaProject()));
			assertEquals("moveRef5", false, moveRef.isValidDestination(getRtJar()));
			assertEquals("moveRef6", true, moveRef.isValidDestination(folder));
			assertEquals("moveRef7", false, moveRef.isValidDestination(file));
		} finally{		
			performDummySearch();
			file.delete(false, null);
			folder.delete(false, null);	
			p1.delete(false, null);
			cu.delete(false, null);
			cu2.delete(false, null);
		}
	}
	
	//----
	public void testReadOnlyFolder() throws Exception{
		printTestDisabledMessage("waiting for bug#6058 (copying IFolder loses read-only flag setting)");
		if (true)
			return;
		
		IFolder folder= null;
		IJavaProject p2=  null;
		IFolder copiedFolder= null;
		try{
			List elements= new ArrayList();	
			String folderName= "f";
			folder= createFolder(folderName);
			folder.setReadOnly(true);
			assertTrue("folder should be read-only", folder.isReadOnly());
			elements.add(folder);
			
			p2= JavaProjectHelper.createJavaProject("P2", "bin");
			CopyRefactoring copyRef= new CopyRefactoring(elements, new MockCopyQueries());
			copyRef.setDestination(p2);
			assertTrue("copy read-only", copyRef.checkActivation(new NullProgressMonitor()).isOK());
			RefactoringStatus status= performRefactoring(copyRef);
			assertEquals("preconditions were expected to be ok", null, status);
			
			IResource p2res= p2.getCorrespondingResource();
			assertTrue("instance of IProject", p2res instanceof IProject);
			
			copiedFolder= ((IProject)p2res).getFolder(folderName);
			assertTrue("copied folder should exist", copiedFolder.exists());
		
			assertTrue("copied folder should be read-only", copiedFolder.isReadOnly());
		
		}finally{
			performDummySearch();
			JavaProjectHelper.delete(p2);
			folder.delete(false, null);
			copiedFolder.delete(false, null);
		}
	}
	
	public void testReadOnlySourceFolder() throws Exception{
		printTestDisabledMessage("waiting for bug#6058 (copying IFolder loses read-only flag setting)");
		if (true)
			return;
		IPackageFragmentRoot srcFolder= null;
		IJavaProject p2=  null;
		IFolder copiedFolder= null;
		try{
			List elements= new ArrayList();	
			String folderName= "ss";
			srcFolder= JavaProjectHelper.addSourceContainer(MySetup.getProject(), folderName);
			srcFolder.getCorrespondingResource().setReadOnly(true);
			assertTrue("folder should be read-only", srcFolder.getCorrespondingResource().isReadOnly());
			elements.add(srcFolder);
			
			p2= JavaProjectHelper.createJavaProject("P2", "bin");
			CopyRefactoring copyRef= new CopyRefactoring(elements, new MockCopyQueries());
			copyRef.setDestination(p2);
			assertTrue("copy read-only", copyRef.checkActivation(new NullProgressMonitor()).isOK());
			RefactoringStatus status= performRefactoring(copyRef);
			assertEquals("preconditions were expected to be ok", null, status);
			
			IResource p2res= p2.getCorrespondingResource();
			assertTrue("instance of IProject", p2res instanceof IProject);
			
			copiedFolder= ((IProject)p2res).getFolder(folderName);
			assertTrue("copied folder should exist", copiedFolder.exists());
		
			assertTrue("copied folder should be read-only", copiedFolder.isReadOnly());
		
		}finally{
			performDummySearch();
			if (p2 != null)
				JavaProjectHelper.delete(p2);
			srcFolder.getCorrespondingResource().delete(false, null);
			copiedFolder.delete(false, null);
		}
	}

	public void testReadOnlyPackage() throws Exception{
		printTestDisabledMessage("waiting for bug#6060 (IPackageFragment::copy loses the read-only setting)");
		if (true)
			return;
		
		IPackageFragment pack= null;
		IPackageFragmentRoot srcFolder2= null;		
		IPackageFragment copiedPackage= null;
		try{
			List elements= new ArrayList();	
			String packageName= "ss";
			pack= MySetup.getDefaultSourceFolder().createPackageFragment(packageName, false, new NullProgressMonitor());
			pack.getCorrespondingResource().setReadOnly(true);
			assertTrue("package should be read-only", pack.getCorrespondingResource().isReadOnly());
			elements.add(pack);
			
			srcFolder2= JavaProjectHelper.addSourceContainer(MySetup.getProject(), "src2");
			CopyRefactoring copyRef= new CopyRefactoring(elements, new MockCopyQueries());
			copyRef.setDestination(srcFolder2);
			assertTrue("copy read-only", copyRef.checkActivation(new NullProgressMonitor()).isOK());
			RefactoringStatus status= performRefactoring(copyRef);
			assertEquals("preconditions were expected to be ok", null, status);
			
			copiedPackage= srcFolder2.getPackageFragment(packageName);
			assertTrue("copied package should exist", copiedPackage.exists());
		
			assertTrue("copied package should be read-only", copiedPackage.isReadOnly());
			assertTrue("copied package should be read-only (2)", copiedPackage.getCorrespondingResource().isReadOnly());
		
		}finally{
			performDummySearch();
			srcFolder2.getCorrespondingResource().delete(false, null);
			pack.delete(false, null);
		}
	}
	
	private static class MockCopyQueries implements ICopyQueries{
		public INewNameQuery createNewCompilationUnitNameQuery(ICompilationUnit cu) {
			return null;
		}

		public INewNameQuery createNewPackageNameQuery(IPackageFragment pack) {
			return null;
		}

		public INewNameQuery createNewResourceNameQuery(IResource res) {
			return null;
		}

		public INewNameQuery createNullQuery() {
			return null;
		}

		public INewNameQuery createStaticQuery(String newName) {
			return null;
		}

	}
}

