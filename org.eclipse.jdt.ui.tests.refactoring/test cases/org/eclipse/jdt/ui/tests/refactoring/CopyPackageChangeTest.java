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
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageChange;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

public class CopyPackageChangeTest extends RefactoringTest {

	private static final String REFACTORING_PATH= "CopyPackageChange/";
	private static final Class clazz= CopyPackageChangeTest.class;
	
	public CopyPackageChangeTest(String name) {
		super(name);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	public void test0() throws Exception{
		createCU(getPackageP(), "A.java", getFileContents(getRefactoringPath() + "A.java"));
		
		IPackageFragmentRoot newRoot= JavaProjectHelper.addSourceContainer(MySetup.getProject(), "newName");
		
		String packName= getPackageP().getElementName();
		CopyPackageChange change= new CopyPackageChange(getPackageP(), newRoot, null);
		change.initializeValidationData(new NullProgressMonitor());
		performChange(change);
		IPackageFragment copied= newRoot.getPackageFragment(packName);
		assertTrue("copied.exists()", copied.exists());
		assertTrue(copied.getChildren().length == 1);
	}
}

