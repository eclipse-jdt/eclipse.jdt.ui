/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.infra.DebugUtils;


public class RenamePackageTests extends RefactoringTest {
	
	private static final Class clazz= RenamePackageTests.class;
	private static final String REFACTORING_PATH= "RenamePackage/";
	
	public RenamePackageTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	// -------------
	private RenameRefactoring createRefactoring(IPackageFragment pack, String newName) throws CoreException {
		RenameRefactoring result= new RenameRefactoring(pack);
		result.setNewName(newName);
		return result;
	}

	/* non java-doc
	 * the 0th one is the one to rename
	 */
	private void helper1(String packageNames[], String[][] packageFiles, String newPackageName) throws Exception{
		try{
			IPackageFragment[] packages= new IPackageFragment[packageNames.length];
			for (int i= 0; i < packageFiles.length; i++){
				packages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
				for (int j= 0; j < packageFiles[i].length; j++){
					createCUfromTestFile(packages[i], packageFiles[i][j], packageNames[i].replace('.', '/') + "/");
					//DebugUtils.dump(cu.getElementName() + "\n" + cu.getSource());
				}	
			}
			IPackageFragment thisPackage= packages[0];
			Refactoring ref= createRefactoring(thisPackage, newPackageName);
			performDummySearch();
			RefactoringStatus result= performRefactoring(ref);
			assertNotNull("precondition was supposed to fail", result);
			if (fIsVerbose)
				DebugUtils.dump("" + result);
		} finally{		
			performDummySearch();
		
			for (int i= 0; i < packageNames.length; i++){
				IPackageFragment pack= getRoot().getPackageFragment(packageNames[i]);
				if (pack.exists())
					pack.delete(true, null);
			}	
		}	
	}
	
	/* non java-doc
	 * the 0th one is the one to rename
	 */
	private void helper1(String[] packageNames, String newPackageName) throws Exception{
		try{
			IPackageFragment[] packages= new IPackageFragment[packageNames.length];
			for (int i= 0; i < packageNames.length; i++){
				packages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
			}
			IPackageFragment thisPackage= packages[0];
			Refactoring ref= createRefactoring(thisPackage, newPackageName);
			performDummySearch();
			RefactoringStatus result= performRefactoring(ref);
			assertNotNull("precondition was supposed to fail", result);
			if (fIsVerbose)
				DebugUtils.dump("" + result);
		} finally{		
			performDummySearch();	
			
			for (int i= 0; i < packageNames.length; i++){
				IPackageFragment pack= getRoot().getPackageFragment(packageNames[i]);
				if (pack.exists())
					pack.delete(true, null);
			}		
		}	
	}
	
	private void helper1() throws Exception{
		helper1(new String[]{"r"}, new String[][]{{"A"}}, "p1");
	}
	
	private void helper2(String[] packageNames, String[][] packageFileNames, String newPackageName, boolean updateReferences) throws Exception{
		try{
			IPackageFragment[] packages= new IPackageFragment[packageNames.length];
			ICompilationUnit[][] cus= new ICompilationUnit[packageFileNames.length][packageFileNames[0].length];
			for (int i= 0; i < packageNames.length; i++){
				packages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
				for (int j= 0; j < packageFileNames[i].length; j++){
					cus[i][j]= createCUfromTestFile(packages[i], packageFileNames[i][j], packageNames[i].replace('.', '/') + "/");
				}
			}
			IPackageFragment thisPackage= packages[0];
			RenameRefactoring ref= createRefactoring(thisPackage, newPackageName);
			((RenamePackageProcessor)ref.getProcessor()).setUpdateReferences(updateReferences);
			performDummySearch();
			RefactoringStatus result= performRefactoring(ref);
			assertEquals("preconditions were supposed to pass", null, result);
			
			//---
			
			assertTrue("package not renamed", ! getRoot().getPackageFragment(packageNames[0]).exists());
			IPackageFragment newPackage= getRoot().getPackageFragment(newPackageName);
			assertTrue("new package does not exist", newPackage.exists());
			
			for (int i= 0; i < packageFileNames.length; i++){
				String packageName= (i == 0) 
								? newPackageName.replace('.', '/') + "/"
								: packageNames[i].replace('.', '/') + "/";
				for (int j= 0; j < packageFileNames[i].length; j++){
					String s1= getFileContents(getOutputTestFileName(packageFileNames[i][j], packageName));
					ICompilationUnit cu= 
						(i == 0) 
							? newPackage.getCompilationUnit(packageFileNames[i][j] + ".java")
							: cus[i][j];
					//DebugUtils.dump("cu:" + cu.getElementName());		
					String s2= cu.getSource();
					
					//DebugUtils.dump("expected:" + s1);
					//DebugUtils.dump("was:" + s2);
					assertEqualLines("invalid update in file " + cu.getElementName(), s1,	s2);
				}
			}
		} finally{
			performDummySearch();
			getRoot().getPackageFragment(newPackageName).delete(true, null);
			for (int i= 1; i < packageNames.length; i++){
				getRoot().getPackageFragment(packageNames[i]).delete(true, null);
			}	
		}	
	}
	
	private void helper2(String[] packageNames, String[][] packageFileNames, String newPackageName) throws Exception{
		helper2(packageNames, packageFileNames, newPackageName, true);
	}
	
	// ---------- tests -------------	
	public void testFail0() throws Exception{
		helper1(new String[]{"r"}, new String[][]{{"A"}}, "9");
	}
	
	public void testFail1() throws Exception{
		printTestDisabledMessage("needs revisiting");
		//helper1(new String[]{"r.p1"}, new String[][]{{"A"}}, "r");
	}
	
	public void testFail2() throws Exception{
		helper1(new String[]{"r.p1", "fred"}, "fred");
	}	
	
	public void testFail3() throws Exception{
		helper1(new String[]{"r"}, new String[][]{{"A"}}, "fred");
	}
	
	public void testFail4() throws Exception{
		helper1();
	}
	
	public void testFail5() throws Exception{
		helper1();
	}
	
	public void testFail6() throws Exception{
		helper1();
	}
	
	public void testFail7() throws Exception{
		//printTestDisabledMessage("1GK90H4: ITPJCORE:WIN2000 - search: missing package reference");
		printTestDisabledMessage("corner case - name obscuring");
//		helper1(new String[]{"r", "p1"}, new String[][]{{"A"}, {"A"}}, "fred");
	}
	
	public void testFail8() throws Exception{
		printTestDisabledMessage("corner case - name obscuring");
//		helper1(new String[]{"r", "p1"}, new String[][]{{"A"}, {"A"}}, "fred");
	}
	
	//native method used r.A as a paramter
	public void testFail9() throws Exception{
		printTestDisabledMessage("corner case - qualified name used  as a paramter of a native method");
		//helper1(new String[]{"r", "p1"}, new String[][]{{"A"}, {"A"}}, "fred");
	}
	
	public void testFail10() throws Exception{
		helper1(new String[]{"r.p1", "r"}, new String[][]{{"A"}, {"A"}}, "r");
	}

	public void testFail11() throws Exception{
		helper1(new String[]{"q.p1", "q", "r.p1"}, new String[][]{{"A"}, {"A"}, {}}, "r.p1");
	}
	
	//-------
	public void test0() throws Exception{
		helper2(new String[]{"r"}, new String[][]{{"A"}}, "p1");
	}
	
	public void test1() throws Exception{
		helper2(new String[]{"r"}, new String[][]{{"A"}}, "p1");
	}
	
	public void test2() throws Exception{
		helper2(new String[]{"r", "fred"}, new String[][]{{"A"}, {"A"}}, "p1");
	}
	
	public void test3() throws Exception{
		helper2(new String[]{"fred", "r.r"}, new String[][]{{"A"}, {"B"}}, "r");
	}
	
	public void test4() throws Exception{
		helper2(new String[]{"r.p1", "r"}, new String[][]{{"A"}, {"A"}}, "q");
	}
	
	public void test5() throws Exception{
		helper2(new String[]{"r"}, new String[][]{{"A"}}, "p1", false);
	}
	
	public void testReadOnly() throws Exception{
		printTestDisabledMessage("see bug#6054 (renaming a read-only package resets the read-only flag)");
		if (true)
			return;
		
		String[] packageNames= new String[]{"r"};
		String[][] packageFileNames= new String[][]{{"A"}};
		String newPackageName= "p1";
		IPackageFragment[] packages= new IPackageFragment[packageNames.length];

		ICompilationUnit[][] cus= new ICompilationUnit[packageFileNames.length][packageFileNames[0].length];
		for (int i= 0; i < packageNames.length; i++){
			packages[i]= getRoot().createPackageFragment(packageNames[i], true, null);
			for (int j= 0; j < packageFileNames[i].length; j++){
				cus[i][j]= createCUfromTestFile(packages[i], packageFileNames[i][j], packageNames[i].replace('.', '/') + "/");
			}
		}
		IPackageFragment thisPackage= packages[0];
		thisPackage.getCorrespondingResource().setReadOnly(true);
		RenameRefactoring ref= createRefactoring(thisPackage, newPackageName);
		performDummySearch();
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("preconditions were supposed to pass", null, result);
		
		assertTrue("package not renamed", ! getRoot().getPackageFragment(packageNames[0]).exists());
		IPackageFragment newPackage= getRoot().getPackageFragment(newPackageName);
		assertTrue("new package does not exist", newPackage.exists());
		assertTrue("new package should be read-only", newPackage.getCorrespondingResource().isReadOnly());
	}
	
}
