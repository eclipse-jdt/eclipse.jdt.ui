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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.refactoring.infra.DebugUtils;

import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

public class MoveMembersTests extends RefactoringTest {

	private static final Class clazz= MoveMembersTests.class;
	
	private static final String REFACTORING_PATH= "MoveMembers/";

	public MoveMembersTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}	

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	//---
	private static MoveRefactoring createRefactoring(IMember[] members, IType destination) throws JavaModelException{
		return createRefactoring(members, destination.getFullyQualifiedName());
	}
	
	private static MoveRefactoring createRefactoring(IMember[] members, String destination) throws JavaModelException{
		MoveStaticMembersProcessor processor= MoveStaticMembersProcessor.create(members, JavaPreferencesSettings.getCodeGenerationSettings());
		if (processor == null)
			return null;
		processor.setDestinationTypeFullyQualifiedName(destination);
		return new MoveRefactoring(processor);
	}
	
	protected void setUp() throws Exception {
		if (fIsVerbose)
			DebugUtils.dump("--------- " + getName() + " ---------------");
		super.setUp();
	}
	
	private void fieldMethodTypePackageHelper_passing(String[] fieldNames, String[] methodNames, String[][] signatures, String[] typeNames, IPackageFragment packForA, IPackageFragment packForB) throws Exception {
		ParticipantTesting.reset();
		ICompilationUnit cuA= createCUfromTestFile(packForA, "A");
		ICompilationUnit cuB= createCUfromTestFile(packForB, "B");
		IType typeA= getType(cuA, "A");
		IType typeB= getType(cuB, "B");
		IField[] fields= getFields(typeA, fieldNames);
		IMethod[] methods= getMethods(typeA, methodNames, signatures);
		IType[] types= getMemberTypes(typeA, typeNames);
	
		IType destinationType= typeB;
		IMember[] members= merge(methods, fields, types);
		String[] handles= ParticipantTesting.createHandles(members);
		MoveArguments[] args= new MoveArguments[handles.length];
		for (int i = 0; i < args.length; i++) {
			args[i]= new MoveArguments(destinationType, true);
		}
		MoveRefactoring ref= createRefactoring(members, destinationType);
	
		RefactoringStatus result= performRefactoringWithStatus(ref);
		assertTrue("precondition was supposed to pass", result.getSeverity() <= RefactoringStatus.WARNING);
		ParticipantTesting.testMove(handles, args);
	
		String expected;
		String actual;
	
		expected= getFileContents(getOutputTestFileName("A"));
		actual= cuA.getSource();
		assertEqualLines("incorrect modification of  A", expected, actual);
	
		expected= getFileContents(getOutputTestFileName("B"));
		actual= cuB.getSource();
		assertEqualLines("incorrect modification of  B", expected, actual);
		//tearDown() deletes resources and does performDummySearch();
	}
	
	/* Move members from p.A to r.B */
	private void fieldMethodTypeABHelper_passing(String[] fieldNames, String[] methodNames, String[][] signatures, String[] typeNames) throws Exception {
		IPackageFragment packageForB= getRoot().createPackageFragment("r", false, null);
		fieldMethodTypePackageHelper_passing(fieldNames, methodNames, signatures, typeNames, getPackageP(), packageForB);
		//tearDown() deletes resources and does performDummySearch();
	}

	private void fieldMethodTypeHelper_passing(String[] fieldNames, String[] methodNames, String[][] signatures, String[] typeNames) throws Exception{
		IPackageFragment packForA= getPackageP();
		IPackageFragment packForB= getPackageP();
		fieldMethodTypePackageHelper_passing(fieldNames, methodNames, signatures, typeNames, packForA, packForB);
	}
	
	private void fieldHelper_passing(String[] fieldNames) throws Exception {
		fieldMethodTypeHelper_passing(fieldNames, new String[0], new String[0][0], new String[0]);
	}
	
	private void methodHelper_passing(String[] methodNames, String[][] signatures) throws Exception {
		fieldMethodTypeHelper_passing(new String[0], methodNames, signatures, new String[0]);
	}

	private void typeHelper_passing(String[] typeNames) throws Exception {
		fieldMethodTypeHelper_passing(new String[0], new String[0], new String[0][0], typeNames);
	}
	
	private void fieldMethodTypePackageHelper_failing(String[] fieldNames,
												String[] methodNames, String[][] signatures,
												String[] typeNames,
												int errorLevel, String destinationTypeName,
												IPackageFragment packForA,
												IPackageFragment packForB) throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(packForA, "A");
		ICompilationUnit cuB= createCUfromTestFile(packForB, "B");
		try{
			IType typeA= getType(cuA, "A");
			IField[] fields= getFields(typeA, fieldNames);
			IMethod[] methods= getMethods(typeA, methodNames, signatures);
			IType[] types= getMemberTypes(typeA, typeNames);
		
			MoveRefactoring ref= createRefactoring(merge(methods, fields, types), destinationTypeName);
			if (ref == null){
				assertEquals(errorLevel, RefactoringStatus.FATAL);
				return;
			}
		
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
	
	private void fieldMethodTypeHelper_failing(String[] fieldNames, 
												String[] methodNames, String[][] signatures, 
												String[] typeNames,
												int errorLevel, String destinationTypeName) throws Exception {
		IPackageFragment packForA= getPackageP();
		IPackageFragment packForB= getPackageP();											
		fieldMethodTypePackageHelper_failing(fieldNames, methodNames, signatures, typeNames,
												errorLevel, destinationTypeName, packForA, packForB);	
	}


	//---

	public void test0() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test1() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test2() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test3() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test4() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test5() throws Exception{
		fieldHelper_passing(new String[]{"f"});
	}

	public void test6() throws Exception{
		fieldHelper_passing(new String[]{"f"});
	}

	public void test7() throws Exception{
		fieldHelper_passing(new String[]{"f"});
	}
	
	public void test8() throws Exception{
//		printTestDisabledMessage("36835");
		IPackageFragment packageForB= null;
		try{
			packageForB= getRoot().createPackageFragment("r", false, null);
			fieldMethodTypePackageHelper_passing(new String[]{"f"}, new String[0], new String[0][0], new String[0], getPackageP(), packageForB);
		} finally{
			performDummySearch();
			if (packageForB != null)
				packageForB.delete(true, null);
		}	
	}

	public void test9() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test10() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test11() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test12() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test13() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test14() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test15() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test16() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test17() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test18() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test19() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test20() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}
	
	public void test21() throws Exception{
		fieldHelper_passing(new String[]{"F", "i"});
	}
	
	public void test22() throws Exception{
		fieldHelper_passing(new String[]{"i"});
	}
	
	public void test23() throws Exception{
		fieldHelper_passing(new String[]{"FRED"});
	}
	
	public void test24() throws Exception{
		fieldHelper_passing(new String[]{"FRED"});
	}
	
	public void test25() throws Exception{
		//printTestDisabledMessage("test for 27098");
		fieldHelper_passing(new String[]{"FRED"});
	}
	
	public void test26() throws Exception{
		IPackageFragment packageForB= null;
		try{
			packageForB= getRoot().createPackageFragment("r", false, null);
			fieldMethodTypePackageHelper_passing(new String[0], new String[]{"n"}, new String[][]{new String[0]}, new String[0], getPackageP(), packageForB);
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
			fieldMethodTypePackageHelper_passing(new String[0], new String[]{"n"}, new String[][]{new String[0]}, new String[0], getPackageP(), packageForB);
		} finally{
			performDummySearch();
			if (packageForB != null)
				packageForB.delete(true, null);
		}	
	}
	
	public void test28() throws Exception{
		methodHelper_passing(new String[]{"m", "n"}, new String[][]{new String[0], new String[0]});
	}
	
	public void test29() throws Exception{ //test for bug 41691
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test30() throws Exception{ //test for bug 41691
		fieldHelper_passing(new String[]{"id"});
	}

	public void test31() throws Exception{ //test for bug 41691
		fieldHelper_passing(new String[]{"odd"});
	}

	public void test32() throws Exception{ //test for bug 41734, 41691
		printTestDisabledMessage("test for 41734");
		//methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test33() throws Exception{ //test for bug 28022
		fieldHelper_passing(new String[]{"i"});
	}

	public void test34() throws Exception{ //test for bug 28022
		fieldHelper_passing(new String[]{"i"});
	}

	public void test35() throws Exception{ //test for bug 28022
		fieldHelper_passing(new String[]{"i"});
	}
	
	//-- move types:
	
	public void test36() throws Exception {
		typeHelper_passing(new String[]{"I"});
	}
	
	public void test37() throws Exception {
		printTestDisabledMessage("qualified access to source");
//		typeHelper_passing(new String[] {"Inner"});
	}
	
	public void test38() throws Exception {
		fieldMethodTypeABHelper_passing(new String[0], new String[0], new String[0][0], new String[]{"Inner"});
	}

	public void test39() throws Exception {
		printTestDisabledMessage("complex imports - need more work");
//		fieldMethodType3CUsHelper_passing(new String[0], new String[0], new String[0][0],
//							new String[]{"Inner"});
	}
	
	public void test40() throws Exception{
		fieldMethodTypeHelper_passing(new String[] {"f"}, new String[]{"m"}, new String[][]{new String[0]}, new String[0]);
	}

	public void test41() throws Exception{
		methodHelper_passing(new String[] {"m"}, new String[][]{new String[0]});
	}

	//---
	public void testFail0() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
									  new String[]{"m"}, new String[][]{new String[0]},
									  new String[0],
									  RefactoringStatus.FATAL, "p.B");
	}
	

	public void testFail1() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  new String[0],
									  RefactoringStatus.ERROR, "p.B.X");
	}
	
	public void testFail2() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, 
									  new String[0],
									  RefactoringStatus.ERROR, "p.B");
	}

	public void testFail3() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[]{"I", "I"}}, 
									  new String[0],
									  RefactoringStatus.ERROR, "p.B");
	}

	public void testFail4() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[]{"I", "I"}}, 
									  new String[0],
									  RefactoringStatus.WARNING, "p.B");
	}
	
	public void testFail5() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[]{"I", "I"}}, 
									  new String[0],
									  RefactoringStatus.WARNING, "p.B");
	}

	public void testFail6() throws Exception{
		fieldMethodTypeHelper_failing(new String[]{"i"}, new String[0], new String[0][0], new String[0],
									  RefactoringStatus.ERROR, "p.B");
	}
	
	public void testFail7() throws Exception{
		fieldMethodTypeHelper_failing(new String[]{"i"}, new String[0], new String[0][0], new String[0], 
									  RefactoringStatus.ERROR, "p.B");
	}
	
	public void testFail8() throws Exception{
		fieldMethodTypeHelper_failing(new String[]{"i"}, new String[0], new String[0][0], new String[0],
									  RefactoringStatus.ERROR, "p.B");
	}
	
	public void testFail9() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, new String[0],
									  RefactoringStatus.ERROR, "p.B");
	}
	
	public void testFail10() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, new String[0],
									  RefactoringStatus.ERROR, "p.B");
	}

	public void testFail11() throws Exception{
		fieldMethodTypeHelper_failing(new String[]{"i"}, new String[0], new String[0][0], new String[0],
									  RefactoringStatus.ERROR, "p.B");
	}

	public void testFail12() throws Exception{
		fieldMethodTypeHelper_failing(new String[]{"i"}, new String[0], new String[0][0], new String[0],
									  RefactoringStatus.ERROR, "p.B");
	}

	public void testFail13() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, new String[0],
									  RefactoringStatus.ERROR, "p.B");
	}

	public void testFail14() throws Exception{
		fieldMethodTypeHelper_failing(new String[]{"i"}, new String[0], new String[0][0], new String[0],
									  RefactoringStatus.ERROR, "p.B");
	}

	public void testFail15() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, new String[0],
									  RefactoringStatus.WARNING, "p.B");
	}

	public void testFail16() throws Exception{
		IPackageFragment packageForB= null;
		try{
			packageForB= getRoot().createPackageFragment("r", false, null);
			fieldMethodTypePackageHelper_failing(new String[]{"f"}, new String[0], new String[0][0], new String[0],
										 RefactoringStatus.ERROR, "r.B", 
										 getPackageP(), packageForB);
		} finally{
			performDummySearch();
			if (packageForB != null)
				packageForB.delete(true, null);
		}	
	}

	public void testFail17() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, new String[0],
									  RefactoringStatus.FATAL, "java.lang.Object");
	}
	
	public void testFail18() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, new String[0],
									  RefactoringStatus.FATAL, "p.DontExist");
	}

	public void testFail19() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, new String[0],
									  RefactoringStatus.ERROR, "p.B");
	}
	
	public void testFail20() throws Exception{
		// was same as test19
	}
	
	public void testFail21() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, new String[0],
									  RefactoringStatus.FATAL, "p.B");
	}

	public void testFail22() throws Exception{
		//free slot
	}

	public void testFail23() throws Exception{
		//free slot
	}

	public void testFail24() throws Exception{
		fieldMethodTypeHelper_failing(new String[0], 
									  new String[]{"m"}, new String[][]{new String[0]}, new String[0],
									  RefactoringStatus.FATAL, "p.B");
	}

}
