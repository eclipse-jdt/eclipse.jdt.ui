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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.DebugUtils;
import org.eclipse.jdt.testplugin.JavaProjectHelper;


public class RenamePackageTests extends RefactoringTest {
	
	private static final Class clazz= RenamePackageTests.class;
	private static final String REFACTORING_PATH= "RenamePackage/";
	
	public RenamePackageTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new MySetup(someTest);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	// -------------
	private RenamePackageRefactoring createRefactoring(IPackageFragment pack, String newName) {
		RenamePackageRefactoring ref= new RenamePackageRefactoring(pack);
		ref.setNewName(newName);
		return ref;
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
			IRefactoring ref= createRefactoring(thisPackage, newPackageName);
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
			IRefactoring ref= createRefactoring(thisPackage, newPackageName);
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
			RenamePackageRefactoring ref= createRefactoring(thisPackage, newPackageName);
			ref.setUpdateReferences(updateReferences);
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
					assertEquals("invalid update in file " + cu.getElementName(), s1,	s2);
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
	
	/**
	 * @param newPackageName the new package name for packageNames[0][0]
	 */
	private void helper3(String[] rootNames, String[][] packageNames, String newPackageName, String[][][] typeNames) throws Exception{
		IPackageFragmentRoot[] roots= new IPackageFragmentRoot[rootNames.length];
		try{
			ICompilationUnit[][][] cus=new ICompilationUnit[rootNames.length][][]; 
			IPackageFragment thisPackage= null;

			for (int r= 0; r < roots.length; r++) {
				roots[r]= JavaProjectHelper.addSourceContainer(getRoot().getJavaProject(), rootNames[r]);
				IPackageFragment[] packages= new IPackageFragment[packageNames[r].length];
				cus[r]= new ICompilationUnit[packageNames[r].length][];
				for (int pa= 0; pa < packageNames[r].length; pa++){
					packages[pa]= roots[r].createPackageFragment(packageNames[r][pa], true, null);
					cus[r][pa]= new ICompilationUnit[typeNames[r][pa].length];
					if (r == 0 && pa == 0)
						thisPackage= packages[pa];
					for (int typ= 0; typ < typeNames[r][pa].length; typ++){
						cus[r][pa][typ]= createCUfromTestFile(packages[pa], typeNames[r][pa][typ],
							rootNames[r] + "/" + packageNames[r][pa].replace('.', '/') + "/");
					}
				}
			}
			
			RenamePackageRefactoring ref= createRefactoring(thisPackage, newPackageName);
			ref.setUpdateReferences(true);
			RefactoringStatus result= performRefactoring(ref);
			assertEquals("preconditions were supposed to pass", null, result);
			
			//---
			
			assertTrue("package not renamed", ! roots[0].getPackageFragment(packageNames[0][0]).exists());
			IPackageFragment newPackage= roots[0].getPackageFragment(newPackageName);
			assertTrue("new package does not exist", newPackage.exists());
			
			for (int r = 0; r < typeNames.length; r++) {
				for (int pa= 0; pa < typeNames[r].length; pa++){
					String packageName= (r == 0 && pa == 0) 
						? rootNames[r] + "/" + newPackageName.replace('.', '/') + "/"
						: rootNames[r] + "/" + packageNames[r][pa].replace('.', '/') + "/";
					for (int typ= 0; typ < typeNames[r][pa].length; typ++){
						String s1= getFileContents(getOutputTestFileName(typeNames[r][pa][typ], packageName));
						ICompilationUnit cu= (r == 0 && pa == 0)
							? newPackage.getCompilationUnit(typeNames[r][pa][typ] + ".java")
							: cus[r][pa][typ];
						//DebugUtils.dump("cu:" + cu.getElementName());		
						String s2= cu.getSource();
						
						//DebugUtils.dump("expected:" + s1);
						//DebugUtils.dump("was:" + s2);
						assertEquals("invalid update in file " + cu.toString(), s1,	s2);
					}
				}
			}
		} finally{
			performDummySearch();
			for (int r = 0; r < rootNames.length; r++) {
				JavaProjectHelper.removeSourceContainer(getRoot().getJavaProject(), rootNames[r]);
			}
		}	
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
		RenamePackageRefactoring ref= createRefactoring(thisPackage, newPackageName);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("preconditions were supposed to pass", null, result);
		
		assertTrue("package not renamed", ! getRoot().getPackageFragment(packageNames[0]).exists());
		IPackageFragment newPackage= getRoot().getPackageFragment(newPackageName);
		assertTrue("new package does not exist", newPackage.exists());
		assertTrue("new package should be read-only", newPackage.getCorrespondingResource().isReadOnly());
	}
	
	public void testImportFromMultiRoots1() throws Exception {
		helper3(new String[]{"srcPrg", "srcTest"}, 
			new String[][] {
							new String[]{"p.p"},
							new String[]{"p.p", "tests"}
							},
			"q",
			new String[][][] {
							  new String[][] {new String[]{"A"}},
							  new String[][] {new String[]{"ATest"}, new String[]{"AllTests"}}
							  }
			);
	}
	
	public void testImportFromMultiRoots2() throws Exception {
		helper3(new String[]{"srcPrg", "srcTest"}, 
			new String[][] {
							new String[]{"p.p"},
							new String[]{"p.p", "tests"}
							},
			"q",
			new String[][][] {
							  new String[][] {new String[]{"A"}},
							  new String[][] {new String[]{"ATest", "TestHelper"}, new String[]{"AllTests", "QualifiedTests"}}
							  }
			);
	}

	public void testImportFromMultiRoots3() throws Exception {
		helper3(new String[]{"srcPrg", "srcTest"}, 
			new String[][] {
							new String[]{"p.p"},
							new String[]{"p.p"}
							},
			"q",
			new String[][][] {
							  new String[][] {new String[]{"ToQ"}},
							  new String[][] {new String[]{"Ref"}}
							  }
			);
	}

	public void testImportFromMultiRoots4() throws Exception {
		helper3(new String[]{"srcPrg", "srcTest"}, 
			new String[][] {
							new String[]{"p"},
							new String[]{"p"}
		},
		"a.b.c",
		new String[][][] {
						  new String[][] {new String[]{"A", "B"}},
						  new String[][] {new String[]{"ATest"}}
		}
		);
	}
	
}
