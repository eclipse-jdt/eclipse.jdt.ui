package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.core.refactoring.packageroots.RenameSourceFolderChange;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;


public class RenameSourceFolderChangeTests extends RefactoringTest {
	
	private static final Class clazz= RenameSourceFolderChangeTests.class;
	public RenameSourceFolderChangeTests(String name){
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
	
	public void test0() throws Exception {
		String oldName= "oldName";
		String newName= "newName";
		
		IJavaProject testProject= MySetup.getProject();
		IPackageFragmentRoot oldRoot= JavaProjectHelper.addSourceContainer(MySetup.getProject(), oldName);
		//testProject.addSourceContainer(oldName);
		
		assertTrue("old folder should exist here", oldRoot.exists());
		
		RenameSourceFolderChange change= new RenameSourceFolderChange(oldRoot, newName);
		performChange(change);
		
		assertTrue("old folder should not exist", ! oldRoot.exists());
		assertEquals("expected 3 pfr's", 3, testProject.getPackageFragmentRoots().length);
		IPackageFragmentRoot[] newRoots= testProject.getPackageFragmentRoots();
		for (int i= 0; i < newRoots.length; i++){
			assertTrue("should exist " + i, newRoots[i].exists());
		}
	}
}

