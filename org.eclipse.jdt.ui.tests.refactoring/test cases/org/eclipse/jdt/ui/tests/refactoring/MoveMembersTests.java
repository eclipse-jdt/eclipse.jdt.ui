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
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.refactoring.infra.DebugUtils;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersRefactoring;

public class MoveMembersTests extends RefactoringTest {

	private static final Class clazz= MoveMembersTests.class;
	
	private static final String REFACTORING_PATH= "MoveMembers/";

	public MoveMembersTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	//---
	private static MoveStaticMembersRefactoring createRefactoring(IMember[] members) throws JavaModelException{
		return MoveStaticMembersRefactoring.create(members, JavaPreferencesSettings.getCodeGenerationSettings());
	}
	
	protected void setUp() throws Exception {
		if (fIsVerbose)
			DebugUtils.dump("--------- " + getName() + " ---------------");
		super.setUp();
	}
	
	private void fieldMethodHelper_passingTest(String[] fieldNames, String[] methodNames, String[][] signatures, IPackageFragment packForA, IPackageFragment packForB) throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(packForA, "A");
		ICompilationUnit cuB= createCUfromTestFile(packForB, "B");
		try{
			IType typeA= getType(cuA, "A");
			IType typeB= getType(cuB, "B");
			IField[] fields= getFields(typeA, fieldNames);
			IMethod[] methods= getMethods(typeA, methodNames, signatures);
		
			MoveStaticMembersRefactoring ref= createRefactoring(merge(methods, fields));
			IType destinationType= typeB;
			ref.setDestinationTypeFullyQualifiedName(destinationType.getFullyQualifiedName());
		
			RefactoringStatus result= performRefactoringWithStatus(ref);
			assertTrue("precondition was supposed to pass", result.getSeverity() <= RefactoringStatus.WARNING);
		
			String expected;
			String actual;
		
			expected= getFileContents(getOutputTestFileName("A"));
			actual= cuA.getSource();
			assertEqualLines("incorrect modification of  A", expected, actual);
		
			expected= getFileContents(getOutputTestFileName("B"));
			actual= cuB.getSource();
			assertEqualLines("incorrect modification of  B", expected, actual);
		
		} finally{
			performDummySearch();
			cuA.delete(false, null);
			cuB.delete(false, null);
		}	
	}
	
	private void fieldMethodHelper_passingTest(String[] fieldNames, String[] methodNames, String[][] signatures) throws Exception{
		IPackageFragment packForA= getPackageP();
		IPackageFragment packForB= getPackageP();
		fieldMethodHelper_passingTest(fieldNames, methodNames, signatures, packForA, packForB);
	}
	
	private void fieldMethodHelper_failingTest(String[] fieldNames, 
												String[] methodNames, String[][] signatures, 
												int errorLevel, String destinationTypeName,
												IPackageFragment packForA,
												IPackageFragment packForB) throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(packForA, "A");
		ICompilationUnit cuB= createCUfromTestFile(packForB, "B");
		try{
			IType typeA= getType(cuA, "A");
			IField[] fields= getFields(typeA, fieldNames);
			IMethod[] methods= getMethods(typeA, methodNames, signatures);
		
			MoveStaticMembersRefactoring ref= createRefactoring(merge(methods, fields));
			if (ref == null){
				assertEquals(errorLevel, RefactoringStatus.FATAL);
				return;
			}
			ref.setDestinationTypeFullyQualifiedName(destinationTypeName);
		
			RefactoringStatus result= performRefactoring(ref);
			if (fIsVerbose)
				DebugUtils.dump("status:" + result);
			assertNotNull("precondition was supposed to fail", result);
			assertEquals("precondition was supposed to fail", errorLevel, result.getSeverity());
		
		} finally{
			performDummySearch();			
			cuA.delete(false, null);
			cuB.delete(false, null);
		}	
	}
	
	private void fieldMethodHelper_failingTest(String[] fieldNames, 
												String[] methodNames, String[][] signatures, 
												int errorLevel, String destinationTypeName) throws Exception {
		IPackageFragment packForA= getPackageP();
		IPackageFragment packForB= getPackageP();											
		fieldMethodHelper_failingTest(fieldNames, methodNames, signatures, errorLevel, destinationTypeName, packForA, packForB);	
	}
	//---
	public void test0() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test1() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test2() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test3() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test4() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test5() throws Exception{
		fieldMethodHelper_passingTest(new String[]{"f"}, new String[0], new String[0][0]);
	}

	public void test6() throws Exception{
		fieldMethodHelper_passingTest(new String[]{"f"}, new String[0], new String[0][0]);
	}

	public void test7() throws Exception{
		fieldMethodHelper_passingTest(new String[]{"f"}, new String[0], new String[0][0]);
	}
	
	public void test8() throws Exception{
//		printTestDisabledMessage("36835");
		IPackageFragment packageForB= null;
		try{
			packageForB= getRoot().createPackageFragment("r", false, null);
			fieldMethodHelper_passingTest(new String[]{"f"}, new String[0], new String[0][0], getPackageP(), packageForB);
		} finally{
			performDummySearch();
			if (packageForB != null)
				packageForB.delete(true, null);
		}	
	}

	public void test9() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test10() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test11() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test12() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test13() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test14() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test15() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test16() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test17() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test18() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test19() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test20() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}
	
	public void test21() throws Exception{
		printTestDisabledMessage("incorrect warning");	
//		fieldMethodHelper_passingTest(new String[]{"F", "i"}, new String[0], new String[0][0]);
	}
	
	public void test22() throws Exception{
		printTestDisabledMessage("nasty corner case");	
//		fieldMethodHelper_passingTest(new String[]{"i"}, new String[0], new String[0][0]);
	}
	
	public void test23() throws Exception{
		fieldMethodHelper_passingTest(new String[]{"FRED"}, new String[0], new String[0][0]);
	}
	
	public void test24() throws Exception{
		fieldMethodHelper_passingTest(new String[]{"FRED"}, new String[0], new String[0][0]);
	}
	
	public void test25() throws Exception{
		//printTestDisabledMessage("test for 27098");
		fieldMethodHelper_passingTest(new String[]{"FRED"}, new String[0], new String[0][0]);
	}
	
	public void test26() throws Exception{
		IPackageFragment packageForB= null;
		try{
			packageForB= getRoot().createPackageFragment("r", false, null);
			fieldMethodHelper_passingTest(new String[0], new String[]{"n"}, new String[][]{new String[0]}, getPackageP(), packageForB);
		} finally{
			performDummySearch();
			if (packageForB != null)
				packageForB.delete(true, null);
		}	
	}
	
	public void test27() throws Exception{
		IPackageFragment packageForB= null;
		try{
			packageForB= getRoot().createPackageFragment("r", false, null);
			fieldMethodHelper_passingTest(new String[0], new String[]{"n"}, new String[][]{new String[0]}, getPackageP(), packageForB);
		} finally{
			performDummySearch();
			if (packageForB != null)
				packageForB.delete(true, null);
		}	
	}
	
	public void test28() throws Exception{
		fieldMethodHelper_passingTest(new String[0], new String[]{"m", "n"}, new String[][]{new String[0], new String[0]});
	}
	
	public void test29() throws Exception{ //test for bug 41691
		fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test30() throws Exception{ //test for bug 41691
		fieldMethodHelper_passingTest(new String[]{"id"}, new String[0], new String[0][0]);
	}

	public void test31() throws Exception{ //test for bug 41691
		fieldMethodHelper_passingTest(new String[]{"odd"}, new String[0], new String[0][0]);
	}

	public void test32() throws Exception{ //test for bug 41734, 41691
		printTestDisabledMessage("test for 41734");
		//fieldMethodHelper_passingTest(new String[0], new String[]{"m"}, new String[][]{new String[0]});
	}

	//---
	public void testFail0() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.FATAL, "p.B");
	}
	

	public void testFail1() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.ERROR, "p.B.X");
	}
	
	public void testFail2() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.ERROR, "p.B");
	}

	public void testFail3() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[]{"I", "I"}}, 
									  RefactoringStatus.ERROR, "p.B");
	}

	public void testFail4() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[]{"I", "I"}}, 
									  RefactoringStatus.WARNING, "p.B");
	}
	
	public void testFail5() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[]{"I", "I"}}, 
									  RefactoringStatus.WARNING, "p.B");
	}

	public void testFail6() throws Exception{
		fieldMethodHelper_failingTest(new String[]{"i"}, new String[0], new String[0][0], 
									  RefactoringStatus.ERROR, "p.B");
	}
	
	public void testFail7() throws Exception{
		fieldMethodHelper_failingTest(new String[]{"i"}, new String[0], new String[0][0], 
									  RefactoringStatus.ERROR, "p.B");
	}
	
	public void testFail8() throws Exception{
		fieldMethodHelper_failingTest(new String[]{"i"}, new String[0], new String[0][0], 
									  RefactoringStatus.ERROR, "p.B");
	}
	
	public void testFail9() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.WARNING, "p.B");
	}
	
	public void testFail10() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.WARNING, "p.B");
	}

	public void testFail11() throws Exception{
		fieldMethodHelper_failingTest(new String[]{"i"}, new String[0], new String[0][0],  
									  RefactoringStatus.WARNING, "p.B");
	}

	public void testFail12() throws Exception{
		fieldMethodHelper_failingTest(new String[]{"i"}, new String[0], new String[0][0],  
									  RefactoringStatus.WARNING, "p.B");
	}

	public void testFail13() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.WARNING, "p.B");
	}

	public void testFail14() throws Exception{
		fieldMethodHelper_failingTest(new String[]{"i"}, new String[0], new String[0][0],  
									  RefactoringStatus.WARNING, "p.B");
	}

	public void testFail15() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.WARNING, "p.B");
	}

	public void testFail16() throws Exception{
		IPackageFragment packageForB= null;
		try{
			packageForB= getRoot().createPackageFragment("r", false, null);
			fieldMethodHelper_failingTest(new String[]{"f"}, new String[0], new String[0][0],
										 RefactoringStatus.WARNING, "r.B", 
										 getPackageP(), packageForB);
		} finally{
			performDummySearch();
			if (packageForB != null)
				packageForB.delete(true, null);
		}	
	}

	public void testFail17() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.FATAL, "java.lang.Object");
	}
	
	public void testFail18() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.FATAL, "p.DontExist");
	}

	public void testFail19() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.FATAL, "p.B");
	}
	
	public void testFail20() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.FATAL, "p.B");
	}
	
	public void testFail21() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.FATAL, "p.B");
	}

	public void testFail22() throws Exception{
		fieldMethodHelper_failingTest(new String[]{"i"}, 
									  new String[0], new String[0][0], 
									  RefactoringStatus.FATAL, "p.B");
	}

	public void testFail23() throws Exception{
		fieldMethodHelper_failingTest(new String[]{"i"}, 
									  new String[0], new String[0][0], 
									  RefactoringStatus.FATAL, "p.B");
	}

	public void testFail24() throws Exception{
		fieldMethodHelper_failingTest(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  RefactoringStatus.FATAL, "p.B");
	}

}
