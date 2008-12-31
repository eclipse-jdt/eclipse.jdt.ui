/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.ccp;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockClipboard;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockWorkbenchSite;

import org.eclipse.jdt.internal.ui.refactoring.reorg.CopyToClipboardAction;

public class CopyResourcesToClipboardActionTest extends RefactoringTest{

	private static final Class clazz= CopyResourcesToClipboardActionTest.class;

	private ICompilationUnit fCuA;
	private ICompilationUnit fCuB;
	private IPackageFragment fPackageQ;
	private IPackageFragment fPackageQ_R;
	private IPackageFragment fDefaultPackage;
	private static final String CU_A_NAME= "A";
	private static final String CU_B_NAME= "B";
	private IFile faTxt;

	private Clipboard fClipboard;

	public CopyResourcesToClipboardActionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	private IFile createFile(IFolder folder, String fileName) throws Exception {
		IFile file= folder.getFile(fileName);
		file.create(getStream("aa"), true, null);
		return file;
	}

	protected void setUp() throws Exception {
		super.setUp();
		fClipboard= new MockClipboard(Display.getDefault());
		fDefaultPackage= RefactoringTestSetup.getDefaultSourceFolder().createPackageFragment("", true, null);

		fCuA= createCU(getPackageP(), CU_A_NAME + ".java", "package p; class A{}");

		fPackageQ= RefactoringTestSetup.getDefaultSourceFolder().createPackageFragment("q", true, null);
		fCuB= createCU(fPackageQ, CU_B_NAME + ".java", "package q; class B{}");

		fPackageQ_R= RefactoringTestSetup.getDefaultSourceFolder().createPackageFragment("q.r", true, null);

		faTxt= createFile((IFolder)getPackageP().getUnderlyingResource(), "a.txt");

		assertTrue("A.java does not exist", fCuA.exists());
		assertTrue("B.java does not exist", fCuB.exists());
		assertTrue("q does not exist", fPackageQ.exists());
		assertTrue("q.r does not exist", fPackageQ_R.exists());
		assertTrue(faTxt.exists());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		performDummySearch();
		fClipboard.dispose();
		delete(fCuA);
		delete(fCuB);
		delete(fPackageQ_R);
		delete(fPackageQ);
		delete(faTxt);
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
			element.delete(true, false, null);
		} catch(CoreException e) {
			//ignore, we must keep going
		}
	}

	private void checkEnabled(Object[] elements) {
		SelectionDispatchAction copyAction= new CopyToClipboardAction(new MockWorkbenchSite(elements), fClipboard);
		copyAction.update(copyAction.getSelection());
		assertTrue("action should be enabled", copyAction.isEnabled());
	}

	private void checkDisabled(Object[] elements) {
		SelectionDispatchAction copyAction= new CopyToClipboardAction(new MockWorkbenchSite(elements), fClipboard);
		copyAction.update(copyAction.getSelection());
		assertTrue("action should not be enabled", ! copyAction.isEnabled());
	}

	public void testEnabled0() throws Exception{
		checkEnabled(new Object[]{fCuA});
	}

	public void testEnabled1() throws Exception{
		checkEnabled(new Object[]{getRoot().getJavaProject()});
	}

	public void testEnabled2() throws Exception{
		checkEnabled(new Object[]{getPackageP()});
	}

	public void testEnabled3() throws Exception{
		checkEnabled(new Object[]{getPackageP(), fPackageQ, fPackageQ_R});
	}

	public void testEnabled4() throws Exception{
		checkEnabled(new Object[]{faTxt});
	}

	public void testEnabled5() throws Exception{
		checkEnabled(new Object[]{getRoot()});
	}

	public void testDisabled0() throws Exception{
		checkDisabled(new Object[]{});
	}

	public void testDisabled1() throws Exception{
		checkDisabled(new Object[]{getRoot().getJavaProject(), fCuA});
	}

	public void testDisabled2() throws Exception{
		checkDisabled(new Object[]{getRoot().getJavaProject(), fPackageQ});
	}

	public void testDisabled3() throws Exception{
		checkDisabled(new Object[]{getRoot().getJavaProject(), faTxt});
	}

	public void testDisabled4() throws Exception{
		checkDisabled(new Object[]{getPackageP(), fCuA});
	}

	public void testDisabled5() throws Exception{
		checkDisabled(new Object[]{getRoot(), fCuA});
	}

	public void testDisabled6() throws Exception{
		checkDisabled(new Object[]{getRoot(), fPackageQ});
	}

	public void testDisabled7() throws Exception{
		checkDisabled(new Object[]{getRoot(), faTxt});
	}

	public void testDisabled8() throws Exception{
		checkDisabled(new Object[]{getRoot(), getRoot().getJavaProject()});
	}

	public void testDisabled9() throws Exception{
		checkDisabled(new Object[]{RefactoringTestSetup.getProject().getPackageFragmentRoots()});
	}

	public void testDisabled10() throws Exception{
		checkDisabled(new Object[]{fCuA, fCuB});
	}

	public void testDisabled11() throws Exception{
		checkDisabled(new Object[]{fDefaultPackage});
	}


}
