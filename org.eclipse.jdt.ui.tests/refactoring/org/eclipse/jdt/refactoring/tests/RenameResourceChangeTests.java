package org.eclipse.jdt.refactoring.tests;

import java.io.InputStream;
import java.io.StringBufferInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.resources.RenameResourceChange;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RenameResourceChangeTests extends RefactoringTest {
	
	public RenameResourceChangeTests(String name) {
		super(name);
	}
	
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), RenameResourceChangeTests.class, args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());
		return new JavaTestSetup(suite);
	}
	
	public static Test noSetupSuite() {
		return new TestSuite(RenameResourceChangeTests.class);
	}
	
	private InputStream getStream(String content){
		return new StringBufferInputStream(content);
	}
	
	public void testFile0() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();	
		String oldName= "a.txt";
		IFile file= folder.getFile(oldName);
		assert("should not exist", ! file.exists());
		String content= "aaaaaaaaa";
		file.create(getStream(content), true, new NullProgressMonitor());
		assert("should exist", file.exists());
		
		String newName= "b.txt";
		IChange change= new RenameResourceChange(file, newName);
		performChange(change);
		assert("after: should exist", folder.getFile(newName).exists());
		assert("after: old should not exist", ! folder.getFile(oldName).exists());
		folder.getFile(newName).delete(true, false, new NullProgressMonitor());
	}
	
	public void testFile1() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();	
		String oldName= "a.txt";
		IFile file= folder.getFile(oldName);
		assert("should not exist", ! file.exists());
		String content= "";
		file.create(getStream(content), true, new NullProgressMonitor());
		assert("should exist", file.exists());
		
		String newName= "b.txt";
		IChange change= new RenameResourceChange(file, newName);
		performChange(change);
		assert("after: should exist", folder.getFile(newName).exists());
		assert("after: old should not exist", ! folder.getFile(oldName).exists());
		folder.getFile(newName).delete(true, false, new NullProgressMonitor());
	}
	
	public void testFile2() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();	
		String oldName= "a.txt";
		IFile file= folder.getFile(oldName);
		assert("should not exist", ! file.exists());
		String content= "aaaaaaaaa";
		file.create(getStream(content), true, new NullProgressMonitor());
		assert("should exist", file.exists());
		
		String newName= "b.txt";
		IChange change= new RenameResourceChange(file, newName);
		performChange(change);
		assert("after: should exist", folder.getFile(newName).exists());
		assert("after: old should not exist", ! folder.getFile(oldName).exists());
		//------
		
		assert("should be undoable", change.isUndoable());	
		IChange undoChange= change.getUndoChange();
		performChange(undoChange);
		assert("after undo: should exist", folder.getFile(oldName).exists());
		assert("after undo: old should not exist", ! folder.getFile(newName).exists());
		
		folder.getFile(oldName).delete(true, false, new NullProgressMonitor());
	}
	
	
	public void testFolder0() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();	
		String oldName= "a";
		IFolder subFolder= folder.getFolder(oldName);
		assert("should not exist", ! subFolder.exists());
		subFolder.create(true, true, null);
		assert("should exist", subFolder.exists());
		
		String newName= "b";
		IChange change= new RenameResourceChange(subFolder, newName);
		performChange(change);
		assert("after: should exist", folder.getFolder(newName).exists());
		assert("after: old should not exist", ! folder.getFolder(oldName).exists());
		folder.getFolder(newName).delete(true, false, new NullProgressMonitor());
	}
	
	public void testFolder1() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();	
		String oldName= "a";
		IFolder subFolder= folder.getFolder(oldName);
		assert("should not exist", ! subFolder.exists());
		subFolder.create(true, true, null);
		IFile file1= subFolder.getFile("a.txt");
		IFile file2= subFolder.getFile("b.txt");
		file1.create(getStream("123"), true, null);
		file2.create(getStream("123345"), true, null);
		
		assert("should exist", subFolder.exists());
		assert("file1 should exist", file1.exists());
		assert("file2 should exist", file2.exists());
		
		String newName= "b";
		IChange change= new RenameResourceChange(subFolder, newName);
		performChange(change);
		assert("after: should exist", folder.getFolder(newName).exists());
		assert("after: old should not exist", ! folder.getFolder(oldName).exists());
		assertEquals("after: child count", 2, folder.getFolder(newName).members().length);
		folder.getFolder(newName).delete(true, false, new NullProgressMonitor());
	}	
	
	public void testFolder2() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();	
		String oldName= "a";
		IFolder subFolder= folder.getFolder(oldName);
		assert("should not exist", ! subFolder.exists());
		subFolder.create(true, true, null);
		assert("should exist", subFolder.exists());
		
		String newName= "b";
		IChange change= new RenameResourceChange(subFolder, newName);
		performChange(change);
		assert("after: should exist", folder.getFolder(newName).exists());
		assert("after: old should not exist", ! folder.getFolder(oldName).exists());
	
		//---
		assert("should be undoable", change.isUndoable());	
		IChange undoChange= change.getUndoChange();
		performChange(undoChange);
		assert("after undo: should exist", folder.getFolder(oldName).exists());
		assert("after undo: old should not exist", ! folder.getFolder(newName).exists());
		
		folder.getFolder(oldName).delete(true, false, new NullProgressMonitor());
	}
	
}

