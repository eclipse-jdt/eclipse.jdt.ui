/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.reorg;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockClipboard;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockWorkbenchSite;

import org.eclipse.jdt.internal.ui.refactoring.reorg.CopyToClipboardAction;
import org.eclipse.jdt.internal.ui.refactoring.reorg.PasteAction;

public class PasteResourcesFromClipboardActionTest extends RefactoringTest{

	private static final Class clazz= PasteResourcesFromClipboardActionTest.class;

	private ICompilationUnit fCuA;
	private ICompilationUnit fCuB;
	
	private IPackageFragment fPackageQ;
	private IPackageFragment fPackageQ_R;
	private static final String CU_A_NAME= "A";
	private static final String CU_B_NAME= "B";
	private IFile faTxt;
	private IJavaProject fSimpleProject;
	private IJavaProject fAnotherProject;
	
	private Clipboard fClipboard;
	
	public PasteResourcesFromClipboardActionTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	private IFile createFile(IFolder folder, String fileName) throws Exception {
		IFile file= folder.getFile(fileName);
		file.create(getStream("aa"), true, null);	
		return file;
	}
	
	private static IJavaProject createProject(String name, String srcFolderName) throws Exception{
		IJavaProject project= JavaProjectHelper.createJavaProject(name, "bin");
		JavaProjectHelper.addRTJar(project);
		if (srcFolderName != null)
			JavaProjectHelper.addSourceContainer(project, srcFolderName);
			
		return project;	
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		fClipboard= new MockClipboard(Display.getDefault());
		fCuA= createCU(getPackageP(), CU_A_NAME + ".java", "package p; class A{}");
		fCuB= createCU(getPackageP(), CU_B_NAME + ".java", "package p; class B{}");
		
		fPackageQ= MySetup.getDefaultSourceFolder().createPackageFragment("q", true, null);
		fPackageQ_R= MySetup.getDefaultSourceFolder().createPackageFragment("q.r", true, null);
		
		faTxt= createFile((IFolder)getPackageP().getUnderlyingResource(), "a.txt");
		
		int count= 0;
		boolean success= false;
		while (count < 10 && ! success){
			success= clearClipboard();
			count++;
		}
		if (! success)
			return;
		
		fSimpleProject= createProject("SimpleProject", null);
		fAnotherProject= createProject("AnotherProject", "src");
		
		assertTrue("A.java does not exist", fCuA.exists());
		assertTrue("B.java does not exist", fCuB.exists());
		assertTrue("q does not exist", fPackageQ.exists());
		assertTrue("q.r does not exist", fPackageQ_R.exists());
		assertTrue("a.txt does not exist", faTxt.exists());
		assertTrue("project SP does not exist", fSimpleProject.exists());
		assertTrue("project AP does not exist", fAnotherProject.exists());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		fClipboard.dispose();
		performDummySearch();
		delete(fCuA);
		delete(fCuB);
		delete(fPackageQ_R);
		delete(fPackageQ);
		delete(faTxt);
		delete(fSimpleProject);
		delete(fAnotherProject);
	}
	
	/**
	 * @return true iff successful
	 */
	private boolean clearClipboard() {
		try{
			fClipboard.setContents(new Object[0], new Transfer[0]);
			return true;
		} catch (SWTError e){
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD)
				throw e;
			return false;
		}	
	}

	private static void delete(ISourceManipulation element) {
		try {
			if (element != null && ((IJavaElement)element).exists())
				element.delete(false, null);
		} catch(JavaModelException e) {
			//ignore, we must keep going
		}		
	}
	private static void delete(IFile element) {
		try {
			if (element != null && element.exists())
				element.delete(true, false, null);
		} catch(CoreException e) {
			//ignore, we must keep going
		}
	}
	
	private static void delete(IJavaProject project){
		try {
			if (project != null && project.exists()){
				project.setRawClasspath(new IClasspathEntry[0], project.getProject().getFullPath(), null);
				project.getProject().delete(true, true, null);
			}	
		} catch(CoreException e) {
			//ignore, we must keep going
		}
	}

	private void doCopy(Object[] copySelection) {
		if (copySelection == null)
			return; 
			
		SelectionDispatchAction pasteAction= new PasteAction(new MockWorkbenchSite(copySelection), fClipboard);	
		CopyToClipboardAction copyAction= new CopyToClipboardAction(new MockWorkbenchSite(copySelection), fClipboard, pasteAction);
		copyAction.update(copyAction.getSelection());
		copyAction.setAutoRepeatOnFailure(true);
		assertTrue("copy not enabled", copyAction.isEnabled());
		copyAction.run();
	}

	private void checkEnabled(Object[] copySelection, Object[] pasteSelection) {
		doCopy(copySelection);		
		SelectionDispatchAction pasteAction= new PasteAction(new MockWorkbenchSite(pasteSelection), fClipboard);
		pasteAction.update(pasteAction.getSelection());
		assertTrue("paste incorrectly disabled", pasteAction.isEnabled());
	}
	
	private void checkDisabled(Object[] copySelection, Object[] pasteSelection) {
		doCopy(copySelection);		
		SelectionDispatchAction pasteAction= new PasteAction(new MockWorkbenchSite(pasteSelection), fClipboard);
		pasteAction.update(pasteAction.getSelection());
		assertTrue("paste incorrectly enabled", ! pasteAction.isEnabled());
	}

	//--- tests --------

	public void testProject() throws Exception{
//		System.out.println("PasteResourcesFromClipboardActionTest#testProject - disabled due to failure under Motif");
//		if (true)
//			return;
		checkEnabled(new Object[]{MySetup.getProject()}, new Object[]{getPackageP()});
		checkEnabled(new Object[]{MySetup.getProject()}, new Object[]{fPackageQ});
		checkEnabled(new Object[]{MySetup.getProject()}, new Object[]{fSimpleProject});
		checkEnabled(new Object[]{MySetup.getProject()}, new Object[]{fAnotherProject});
		checkEnabled(new Object[]{MySetup.getProject()}, new Object[]{MySetup.getProject()});
		checkEnabled(new Object[]{MySetup.getProject()}, new Object[]{getRoot()});
		checkEnabled(new Object[]{MySetup.getProject()}, new Object[]{fCuA});
		checkEnabled(new Object[]{MySetup.getProject()}, new Object[]{faTxt});
	}
	
	public void testCu() throws Exception{
		checkEnabled(new Object[]{fCuA}, new Object[]{getPackageP()});
		checkEnabled(new Object[]{fCuA}, new Object[]{fPackageQ});
		checkEnabled(new Object[]{fCuA}, new Object[]{fSimpleProject});
		checkEnabled(new Object[]{fCuA}, new Object[]{fAnotherProject});
		checkEnabled(new Object[]{fCuA}, new Object[]{MySetup.getProject()});
		checkEnabled(new Object[]{fCuA}, new Object[]{getRoot()});
		checkEnabled(new Object[]{fCuA}, new Object[]{fCuA});		
		checkEnabled(new Object[]{fCuA}, new Object[]{faTxt});
	}
	
	public void testFile() throws Exception{
		checkEnabled(new Object[]{faTxt}, new Object[]{getPackageP()});
		checkEnabled(new Object[]{faTxt}, new Object[]{fPackageQ});
		checkEnabled(new Object[]{faTxt}, new Object[]{fSimpleProject});
		checkEnabled(new Object[]{faTxt}, new Object[]{fAnotherProject});
		checkEnabled(new Object[]{faTxt}, new Object[]{MySetup.getProject()});
		checkEnabled(new Object[]{faTxt}, new Object[]{getRoot()});
		checkEnabled(new Object[]{faTxt}, new Object[]{fCuA});
		checkEnabled(new Object[]{faTxt}, new Object[]{faTxt});
	}
	
	public void testPackage() throws Exception{
//		System.out.println("PasteResourcesFromClipboardActionTest#testPackage - disabled due to failure under Motif");
//		if (true)
//			return;
//		checkEnabled(new Object[]{getPackageP()}, new Object[]{fSimpleProject});
		checkEnabled(new Object[]{getPackageP()}, new Object[]{getRoot()});
		checkEnabled(new Object[]{getPackageP()}, new Object[]{getPackageP()});
		checkEnabled(new Object[]{getPackageP()}, new Object[]{fPackageQ});
		
		checkDisabled(new Object[]{getPackageP()}, new Object[]{fAnotherProject});
		checkDisabled(new Object[]{getPackageP()}, new Object[]{MySetup.getProject()});		
		checkDisabled(new Object[]{getPackageP()}, new Object[]{fCuA});
		checkDisabled(new Object[]{getPackageP()}, new Object[]{faTxt});
	}
	
	public void testRoot() throws Exception {		
//		System.out.println("PasteResourcesFromClipboardActionTest#testRoot - disabled due to failure under Motif");
//		if (true)
//			return;
		checkEnabled(new Object[]{getRoot()}, new Object[]{fAnotherProject});
		checkEnabled(new Object[]{getRoot()}, new Object[]{MySetup.getProject()});		
		
		checkDisabled(new Object[]{getRoot()}, new Object[]{getRoot()});
//		checkDisabled(new Object[]{getRoot()}, new Object[]{fSimpleProject});
		checkDisabled(new Object[]{getRoot()}, new Object[]{getPackageP()});
		checkDisabled(new Object[]{getRoot()}, new Object[]{fPackageQ});
		checkDisabled(new Object[]{getRoot()}, new Object[]{fCuA});
		checkDisabled(new Object[]{getRoot()}, new Object[]{faTxt});
	}
	

	public void testEnabled2() throws Exception{
		checkEnabled(new Object[]{fCuA, fCuB}, new Object[]{getPackageP()});
	}
}
