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

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameRefactoring;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

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

	/** See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=47316">Bug 47316</a>. */
	public static Test setUpTest(Test someTest) {
		return new MySetup(someTest);
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
	
	/**
	 * Custom project and source folder structure.
	 * @param roots source folders
	 * @param packageNames package names per root
	 * @param newPackageName the new package name for packageNames[0][0]
	 * @param cuNames cu names per package
	 */
	private void helperMultiProjects(IPackageFragmentRoot[] roots, String[][] packageNames, String newPackageName, String[][][] cuNames) throws Exception{
		ICompilationUnit[][][] cus=new ICompilationUnit[roots.length][][]; 
		IPackageFragment thisPackage= null;

		for (int r= 0; r < roots.length; r++) {
			IPackageFragment[] packages= new IPackageFragment[packageNames[r].length];
			cus[r]= new ICompilationUnit[packageNames[r].length][];
			for (int pa= 0; pa < packageNames[r].length; pa++){
				packages[pa]= roots[r].createPackageFragment(packageNames[r][pa], true, null);
				cus[r][pa]= new ICompilationUnit[cuNames[r][pa].length];
				if (r == 0 && pa == 0)
					thisPackage= packages[pa];
				for (int typ= 0; typ < cuNames[r][pa].length; typ++){
					cus[r][pa][typ]= createCUfromTestFile(packages[pa], cuNames[r][pa][typ],
							roots[r].getElementName() + "/" + packageNames[r][pa].replace('.', '/') + "/");
				}
			}
		}
		
		RenameRefactoring ref= createRefactoring(thisPackage, newPackageName);
		((RenamePackageProcessor) ref.getProcessor()).setUpdateReferences(true);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("preconditions were supposed to pass", null, result);
		
		assertTrue("package not renamed", ! roots[0].getPackageFragment(packageNames[0][0]).exists());
		IPackageFragment newPackage= roots[0].getPackageFragment(newPackageName);
		assertTrue("new package does not exist", newPackage.exists());
		
		for (int r = 0; r < cuNames.length; r++) {
			for (int pa= 0; pa < cuNames[r].length; pa++){
				String packageName= roots[r].getElementName() + "/"	+ 
						((r == 0 && pa == 0) ? newPackageName : packageNames[r][pa]).replace('.', '/') + "/";
				for (int typ= 0; typ < cuNames[r][pa].length; typ++){
					String s1= getFileContents(getOutputTestFileName(cuNames[r][pa][typ], packageName));
					ICompilationUnit cu= (r == 0 && pa == 0)
							? newPackage.getCompilationUnit(cuNames[r][pa][typ] + ".java")
							: cus[r][pa][typ];
					//DebugUtils.dump("cu:" + cu.getElementName());		
					String s2= cu.getSource();
					
					//DebugUtils.dump("expected:" + s1);
					//DebugUtils.dump("was:" + s2);
					assertEqualLines("invalid update in file " + cu.toString(), s1,	s2);
				}
			}
		}
	}
		
	/**
	 * 2 Projects with a root each:
	 * Project RenamePack2 (root: srcTest) requires project RenamePack1 (root: srcPrg).
	 * @param packageNames package names per root
	 * @param newPackageName the new package name for packageNames[0][0]
	 * @param cuNames cu names per package
	 */
	private void helperProjectsPrgTest(String[][] packageNames, String newPackageName, String[][][] cuNames) throws Exception{
		IJavaProject projectPrg= null;
		IJavaProject projectTest= null;
		try {
			projectPrg= JavaProjectHelper.createJavaProject("RenamePack1", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectPrg));
			IPackageFragmentRoot srcPrg= JavaProjectHelper.addSourceContainer(projectPrg, "srcPrg");

			projectTest= JavaProjectHelper.createJavaProject("RenamePack2", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectTest));
			IPackageFragmentRoot srcTest= JavaProjectHelper.addSourceContainer(projectTest, "srcTest");

			JavaProjectHelper.addRequiredProject(projectTest, projectPrg);

			helperMultiProjects(new IPackageFragmentRoot[] { srcPrg, srcTest }, packageNames, newPackageName, cuNames);
		} finally {
			JavaProjectHelper.delete(projectPrg);
			JavaProjectHelper.delete(projectTest);
		}
	}
	
	/**
	 * Multiple source folders in the same project.
	 * @param newPackageName the new package name for packageNames[0][0]
	 */
	private void helperMultiRoots(String[] rootNames, String[][] packageNames, String newPackageName, String[][][] typeNames) throws Exception{
		IPackageFragmentRoot[] roots= new IPackageFragmentRoot[rootNames.length];
		try {
			for (int r= 0; r < roots.length; r++)
				roots[r]= JavaProjectHelper.addSourceContainer(getRoot().getJavaProject(), rootNames[r]);
			helperMultiProjects(roots, packageNames, newPackageName, typeNames);
		} catch (CoreException e) {
		}
		for (int r= 0; r < roots.length; r++)
			JavaProjectHelper.removeSourceContainer(getRoot().getJavaProject(), rootNames[r]);
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
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("preconditions were supposed to pass", null, result);
		
		assertTrue("package not renamed", ! getRoot().getPackageFragment(packageNames[0]).exists());
		IPackageFragment newPackage= getRoot().getPackageFragment(newPackageName);
		assertTrue("new package does not exist", newPackage.exists());
		assertTrue("new package should be read-only", newPackage.getCorrespondingResource().isReadOnly());
	}
	
	public void testImportFromMultiRoots1() throws Exception {
		helperProjectsPrgTest(
			new String[][] {
				new String[] { "p.p" }, new String[] { "p.p", "tests" }
				},
			"q",
			new String[][][] {
				new String[][] { new String[] { "A" }},
				new String[][] { new String[] { "ATest" }, new String[] { "AllTests" }}
		});
	}
	
	public void testImportFromMultiRoots2() throws Exception {
		/*helperProjectsPrgTest(
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
			*/
		// TODO: ma reenable after I20040302
	}

	public void testImportFromMultiRoots3() throws Exception {
		helperMultiRoots(new String[]{"srcPrg", "srcTest"}, 
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
		//circular buildpath references
		IJavaProject projectPrg= null;
		IJavaProject projectTest= null;
		Hashtable options= JavaCore.getOptions();
		Object cyclicPref= JavaCore.getOption(JavaCore.CORE_CIRCULAR_CLASSPATH);
		try {
			projectPrg= JavaProjectHelper.createJavaProject("RenamePack1", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectPrg));
			IPackageFragmentRoot srcPrg= JavaProjectHelper.addSourceContainer(projectPrg, "srcPrg");

			projectTest= JavaProjectHelper.createJavaProject("RenamePack2", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectTest));
			IPackageFragmentRoot srcTest= JavaProjectHelper.addSourceContainer(projectTest, "srcTest");

			options.put(JavaCore.CORE_CIRCULAR_CLASSPATH, JavaCore.WARNING);
			JavaCore.setOptions(options);
			JavaProjectHelper.addRequiredProject(projectTest, projectPrg);
			JavaProjectHelper.addRequiredProject(projectPrg, projectTest);
			
			helperMultiProjects(new IPackageFragmentRoot[] {srcPrg, srcTest},
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
		} finally {
			options.put(JavaCore.CORE_CIRCULAR_CLASSPATH, cyclicPref);
			JavaCore.setOptions(options);	
			JavaProjectHelper.delete(projectPrg);
			JavaProjectHelper.delete(projectTest);
		}
	}
	
	public void testImportFromMultiRoots5() throws Exception {
		//rename srcTest-p.p to q => ATest now must import p.p.A
		IJavaProject projectPrg= null;
		IJavaProject projectTest= null;
		try {
			projectPrg= JavaProjectHelper.createJavaProject("RenamePack1", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectPrg));
			IPackageFragmentRoot srcPrg= JavaProjectHelper.addSourceContainer(projectPrg, "srcPrg");

			projectTest= JavaProjectHelper.createJavaProject("RenamePack2", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(projectTest));
			IPackageFragmentRoot srcTest= JavaProjectHelper.addSourceContainer(projectTest, "srcTest");

			JavaProjectHelper.addRequiredProject(projectTest, projectPrg);

			helperMultiProjects(new IPackageFragmentRoot[] { srcTest, srcPrg },
				new String[][] {
					new String[] {"p.p"}, new String[] {"p.p"}
				},
				"q",
				new String[][][] {
					new String[][] {new String[] {"ATest"}},
					new String[][] {new String[] {"A"}}
				}
			);
		} finally {
			JavaProjectHelper.delete(projectPrg);
			JavaProjectHelper.delete(projectTest);
		}
		
	}
	
	public void testImportFromMultiRoots6() throws Exception {
		//rename srcTest-p.p to a.b.c => ATest must retain import p.p.A
		helperMultiRoots(new String[]{"srcTest", "srcPrg"}, 
				new String[][] {
								new String[]{"p.p"},
								new String[]{"p.p"}
								},
				"cheese",
				new String[][][] {
								  new String[][] {new String[]{"ATest"}},
								  new String[][] {new String[]{"A"}}
								  }
		);
	}

	public void testImportFromMultiRoots7() throws Exception {
		IJavaProject prj= null;
		IJavaProject prjRef= null;
		IJavaProject prjOther= null;
		try {
			prj= JavaProjectHelper.createJavaProject("prj", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(prj));
			IPackageFragmentRoot srcPrj= JavaProjectHelper.addSourceContainer(prj, "srcPrj"); //$NON-NLS-1$

			prjRef= JavaProjectHelper.createJavaProject("prj.ref", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(prjRef));
			IPackageFragmentRoot srcPrjRef= JavaProjectHelper.addSourceContainer(prjRef, "srcPrj.ref"); //$NON-NLS-1$

			prjOther= JavaProjectHelper.createJavaProject("prj.other", "bin");
			assertNotNull(JavaProjectHelper.addRTJar(prjOther));
			IPackageFragmentRoot srcPrjOther= JavaProjectHelper.addSourceContainer(prjRef, "srcPrj.other"); //$NON-NLS-1$

			JavaProjectHelper.addRequiredProject(prjRef, prj);
			JavaProjectHelper.addRequiredProject(prjRef, prjOther);

			helperMultiProjects(
				new IPackageFragmentRoot[] { srcPrj, srcPrjRef, srcPrjOther },
				new String[][] {
					new String[] {"pack"},
					new String[] {"pack", "pack.man"},
					new String[] {"pack"}
				},
				"com.packt",
				new String[][][] {
					new String[][] {new String[] {"DingsDa"}},
					new String[][] {new String[] {"Referer"}, new String[] {"StarImporter"}},
					new String[][] {new String[] {"Namesake"}}
				}
			);
		} finally {
			JavaProjectHelper.delete(prj);
			JavaProjectHelper.delete(prjRef);
			JavaProjectHelper.delete(prjOther);
		}
	}

}
