package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.changes.RenameSourceFolderChange;

import org.eclipse.jdt.testplugin.JavaProjectHelper;


public class RenameSourceFolderChangeTests extends RefactoringTest {
	
	private static final Class clazz= RenameSourceFolderChangeTests.class;

	public RenameSourceFolderChangeTests(String name){
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
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
		assertEquals("expected 2 pfr's", 2, testProject.getPackageFragmentRoots().length);
		IPackageFragmentRoot[] newRoots= testProject.getPackageFragmentRoots();
		for (int i= 0; i < newRoots.length; i++){
			assertTrue("should exist " + i, newRoots[i].exists());
		}
	}
}

