/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDelegateUpdating;

import org.eclipse.jdt.ui.tests.refactoring.infra.DebugUtils;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class MoveMembersTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "MoveMembers/";

	public MoveMembersTests() {
		rts= new RefactoringTestSetup();
	}

	protected MoveMembersTests(RefactoringTestSetup rts) {
		super(rts);
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	//---
	private static MoveRefactoring createRefactoring(IMember[] members, IType destination) throws JavaModelException{
		return createRefactoring(members, destination.getFullyQualifiedName());
	}

	private static MoveRefactoring createRefactoring(IMember[] members, String destination) throws JavaModelException{
		IJavaProject project= null;
		if (members != null && members.length > 0)
			project= members[0].getJavaProject();
		MoveStaticMembersProcessor processor= (RefactoringAvailabilityTester.isMoveStaticMembersAvailable(members) ? new MoveStaticMembersProcessor(members, JavaPreferencesSettings.getCodeGenerationSettings(project)) : null);
		if (processor == null)
			return null;
		processor.setDestinationTypeFullyQualifiedName(destination);
		return new MoveRefactoring(processor);
	}

	@Override
	public void genericbefore() throws Exception {
		if (fIsVerbose)
			DebugUtils.dump("--------- " + getName() + " ---------------");
		super.genericbefore();
		fIsPreDeltaTest= true;
	}

	private void fieldMethodTypePackageHelper_passing(String[] fieldNames, String[] methodNames, String[][] signatures, String[] typeNames, IPackageFragment packForA, IPackageFragment packForB, boolean addDelegate) throws Exception {
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

		IDelegateUpdating delUp= ref.getProcessor().getAdapter(IDelegateUpdating.class);
		delUp.setDelegateUpdating(addDelegate);

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
		fieldMethodTypePackageHelper_passing(fieldNames, methodNames, signatures, typeNames, getPackageP(), packageForB, false);
		//tearDown() deletes resources and does performDummySearch();
	}

	private void fieldMethodTypeHelper_passing(String[] fieldNames, String[] methodNames, String[][] signatures, String[] typeNames, boolean addDelegates) throws Exception{
		IPackageFragment packForA= getPackageP();
		IPackageFragment packForB= getPackageP();
		fieldMethodTypePackageHelper_passing(fieldNames, methodNames, signatures, typeNames, packForA, packForB, addDelegates);
	}

	private void fieldHelper_passing(String[] fieldNames) throws Exception {
		fieldMethodTypeHelper_passing(fieldNames, new String[0], new String[0][0], new String[0], false);
	}

	private void fieldHelperDelegate_passing(String[] fieldNames) throws Exception {
		fieldMethodTypeHelper_passing(fieldNames, new String[0], new String[0][0], new String[0], true);
	}

	protected void methodHelper_passing(String[] methodNames, String[][] signatures) throws Exception {
		fieldMethodTypeHelper_passing(new String[0], methodNames, signatures, new String[0], false);
	}

	private void methodHelperDelegate_passing(String[] methodNames, String[][] signatures) throws Exception {
		fieldMethodTypeHelper_passing(new String[0], methodNames, signatures, new String[0], true);
	}

	private void typeHelper_passing(String[] typeNames) throws Exception {
		fieldMethodTypeHelper_passing(new String[0], new String[0], new String[0][0], typeNames, false);
	}

	private void fieldMethodTypePackageHelper_failing(String[] fieldNames,
			String[] methodNames, String[][] signatures,
			String[] typeNames,
			int errorLevel, String destinationTypeName,
			IPackageFragment packForA,
			IPackageFragment packForB) throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(packForA, "A");
		createCUfromTestFile(packForB, "B");
		IType typeA= getType(cuA, "A");
		IField[] fields= getFields(typeA, fieldNames);
		IMethod[] methods= getMethods(typeA, methodNames, signatures);
		IType[] types= getMemberTypes(typeA, typeNames);

		MoveRefactoring ref= createRefactoring(merge(methods, fields, types), destinationTypeName);
		if (ref == null){
			assertEquals(RefactoringStatus.FATAL, errorLevel);
			return;
		}

		RefactoringStatus result= performRefactoring(ref);
		if (fIsVerbose)
			DebugUtils.dump("status:" + result);
		assertNotNull("precondition was supposed to fail", result);
		assertEquals("precondition was supposed to fail", errorLevel, result.getSeverity());

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

	@Test
	public void test0() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test1() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test2() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test3() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test4() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test5() throws Exception{
		fieldHelper_passing(new String[]{"f"});
	}

	@Test
	public void test6() throws Exception{
		fieldHelper_passing(new String[]{"f"});
	}

	@Test
	public void test7() throws Exception{
		fieldHelper_passing(new String[]{"f"});
	}

	@Test
	public void test8() throws Exception{
//		printTestDisabledMessage("36835");
		IPackageFragment packageForB= getRoot().createPackageFragment("r", false, null);
		fieldMethodTypePackageHelper_passing(new String[]{"f"}, new String[0], new String[0][0], new String[0], getPackageP(), packageForB, false);
	}

	@Test
	public void test9() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test10() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test11() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test12() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test13() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test14() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test15() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test16() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test17() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test18() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test19() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test20() throws Exception{
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Ignore("not currently handling visibility issues induced by moving more than one static member")
	@Test
	public void test21() throws Exception{
		fieldHelper_passing(new String[]{"F", "i"});
	}

	@Test
	public void test22() throws Exception{
		fieldHelper_passing(new String[]{"i"});
	}

	@Test
	public void test23() throws Exception{
		fieldHelper_passing(new String[]{"FRED"});
	}

	@Test
	public void test24() throws Exception{
		fieldHelper_passing(new String[]{"FRED"});
	}

	@Test
	public void test25() throws Exception{
		//printTestDisabledMessage("test for 27098");
		fieldHelper_passing(new String[]{"FRED"});
	}

	@Test
	public void test26() throws Exception{
		IPackageFragment packageForB= getRoot().createPackageFragment("r", false, null);
		fieldMethodTypePackageHelper_passing(new String[0], new String[]{"n"}, new String[][]{new String[0]}, new String[0], getPackageP(), packageForB, false);
	}

	@Test
	public void test27() throws Exception{
		IPackageFragment packageForB= getRoot().createPackageFragment("r", false, null);
		fieldMethodTypePackageHelper_passing(new String[0], new String[]{"n"}, new String[][]{new String[0]}, new String[0], getPackageP(), packageForB, false);
	}

	@Test
	public void test28() throws Exception{
		methodHelper_passing(new String[]{"m", "n"}, new String[][]{new String[0], new String[0]});
	}

	@Test
	public void test29() throws Exception{ //test for bug 41691
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test30() throws Exception{ //test for bug 41691
		fieldHelper_passing(new String[]{"id"});
	}

	@Test
	public void test31() throws Exception{ //test for bug 41691
		fieldHelper_passing(new String[]{"odd"});
	}

	@Ignore("test for 41734")
	@Test
	public void test32() throws Exception{ //test for bug 41734, 41691
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test33() throws Exception{ //test for bug 28022
		fieldHelper_passing(new String[]{"i"});
	}

	@Test
	public void test34() throws Exception{ //test for bug 28022
		fieldHelper_passing(new String[]{"i"});
	}

	@Test
	public void test35() throws Exception{ //test for bug 28022
		fieldHelper_passing(new String[]{"i"});
	}

	//-- move types:

	@Test
	public void test36() throws Exception {
		typeHelper_passing(new String[]{"I"});
	}

	@Ignore("qualified access to source")
	@Test
	public void test37() throws Exception {
		typeHelper_passing(new String[] {"Inner"});
	}

	@Test
	public void test38() throws Exception {
		fieldMethodTypeABHelper_passing(new String[0], new String[0], new String[0][0], new String[]{"Inner"});
	}

	@Ignore("complex imports - need more work")
	@Test
	public void test39() throws Exception {
//		fieldMethodType3CUsHelper_passing(new String[0], new String[0], new String[0][0],
//							new String[]{"Inner"});
	}

	@Test
	public void test40() throws Exception{
		fieldMethodTypeHelper_passing(new String[] {"f"}, new String[]{"m"}, new String[][]{new String[0]}, new String[0], false);
	}

	@Test
	public void test41() throws Exception{
		methodHelper_passing(new String[] {"m"}, new String[][]{new String[0]});
	}

	//-- Visibility issues in the moved member:

	@Test
	public void test42() throws Exception{
		//former testFail9
		//Tests move of public static method m, which references private method f, into same package.
		methodHelper_passing(new String[] {"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test43() throws Exception{
		//former testFail10
		//Tests move of public static method m, which references private field F, into same package
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test44() throws Exception{
		//former testFail11
		//Tests move of public static field i, which references private field F, into same package
		fieldHelper_passing(new String[]{"i"});
	}

	@Test
	public void test45() throws Exception{
		//former testFail12
		//Tests move of public static field i, which references private method m, into same package
		fieldHelper_passing(new String[]{"i"});
	}

	@Test
	public void test46() throws Exception{
		//former testFail13
		//Tests move of public static method m, which gets referenced by a field, into same package
		methodHelper_passing(new String[]{"m"}, new String[][]{new String[0]});
	}

	@Test
	public void test47() throws Exception{
		//former testFail14
		//Tests move of public static field i, which gets referenced by a field, into same package
		fieldHelper_passing(new String[]{"i"});
	}

	@Test
	public void test48() throws Exception{
		//Move private unused method which calls another private method into another package
		IPackageFragment packageForB= getRoot().createPackageFragment("r", false, null);
		fieldMethodTypePackageHelper_passing(new String[0], new String[]{"bar"}, new String[][]{new String[0]}, new String[0], getPackageP(), packageForB, false);
	}

	// --- Visibility issues of the moved member itself

	@Test
	public void test49() throws Exception{
		//Move protected used field into another package
		IPackageFragment packageForB= getRoot().createPackageFragment("r", false, null);
		fieldMethodTypePackageHelper_passing(new String[]{"someVar"}, new String[0], new String[][]{new String[0]}, new String[0], getPackageP(), packageForB, false);
	}

	@Test
	public void test50() throws Exception{
		//Move private used method into subtype.
		methodHelper_passing(new String[]{"foo"}, new String[][]{new String[0]});
	}

	@Test
	public void test51() throws Exception {
		//Move private static inner class with private field (but used in outer class)
		//assure both class and field get their visibility increased
		typeHelper_passing(new String[] { "Inner" });
	}

	@Test
	public void test52() throws Exception {
		// assure moved unused field keeps its visibility
		fieldHelper_passing(new String[] { "a" });
	}

	@Test
	public void test53() throws Exception {
		// assure moved unusued class keeps its visibility
		typeHelper_passing(new String[] { "C" });
	}

	@Test
	public void test54() throws Exception {
		// moved used method is changed in visibility
		methodHelper_passing(new String[] { "b" }, new String[][]{new String[0]});
	}

	@Test
	public void test55() throws Exception {
		// moved used method is changed in visibility
		typeHelper_passing(new String[] { "C" });
	}

	// --- Visibility of members of the moved type

	@Test
	public void test56() throws Exception {
		// Move an inner class with two USED members
		typeHelper_passing(new String[] { "Inner" });
	}

	@Test
	public void test57() throws Exception {
		// Move an inner class with two UNUSED members
		typeHelper_passing(new String[] { "Inner" });
	}

	// --- Visibility of used outer members

	@Test
	public void test58() throws Exception {
		// Move a type which references a field in an enclosing type
		// and a field in a sibling
		typeHelper_passing(new String[] { "Inner" });
	}

	@Test
	public void test59() throws Exception {
		// Move a type which references a field in an enclosing type,
		// and the enclosing type is private
		typeHelper_passing(new String[] { "SomeInner.Inner" });
	}

	@Test
	public void test60() throws Exception{
		// Move a static private "getter" of a static field into another class
		// only the field should be changed to public (bug 122490)
		IPackageFragment packageForB= getRoot().createPackageFragment("e", false, null);
		fieldMethodTypePackageHelper_passing(new String[0], new String[] { "getNAME" }, new String[][]{new String[0]}, new String[0], getPackageP(), packageForB, false);
	}

	@Test
	public void test61() throws Exception{
		// Move some method which references a field with a getter and a setter
		// only the field should be changed to public (bug 122490)
		IPackageFragment packageForB= getRoot().createPackageFragment("e", false, null);
		fieldMethodTypePackageHelper_passing(new String[0], new String[] { "foo" }, new String[][]{new String[0]}, new String[0], getPackageP(), packageForB, false);
	}

	// parameterized type references


	@Test
	public void test62() throws Exception {
		// Move a type which references a field in an enclosing type
		// and a field in a sibling
		typeHelper_passing(new String[] { "SomeInner" });
	}

	@Test
	public void test63() throws Exception { // test for Bug 236473
		// Move a static method to a type that has a static import for it
		methodHelper_passing(new String[] { "m" }, new String[][] { new String[0] });
	}

	//---
	@Test
	public void testFail0() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[0]},
				new String[0],
				RefactoringStatus.FATAL, "p.B");
	}


	@Test
	public void testFail1() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[0]},
				new String[0],
				RefactoringStatus.ERROR, "p.B.X");
	}

	@Test
	public void testFail2() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[0]},
				new String[0],
				RefactoringStatus.ERROR, "p.B");
	}

	@Test
	public void testFail3() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[]{"I", "I"}},
				new String[0],
				RefactoringStatus.ERROR, "p.B");
	}

	@Test
	public void testFail4() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[]{"I", "I"}},
				new String[0],
				RefactoringStatus.WARNING, "p.B");
	}

	@Test
	public void testFail5() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[]{"I", "I"}},
				new String[0],
				RefactoringStatus.WARNING, "p.B");
	}

	@Test
	public void testFail6() throws Exception{
		fieldMethodTypeHelper_failing(new String[]{"i"}, new String[0], new String[0][0], new String[0],
				RefactoringStatus.ERROR, "p.B");
	}

	@Test
	public void testFail7() throws Exception{
		fieldMethodTypeHelper_failing(new String[]{"i"}, new String[0], new String[0][0], new String[0],
				RefactoringStatus.ERROR, "p.B");
	}

	@Test
	public void testFail8() throws Exception{
		fieldMethodTypeHelper_failing(new String[]{"i"}, new String[0], new String[0][0], new String[0],
				RefactoringStatus.ERROR, "p.B");
	}

	@Test
	public void testFail15() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[0]}, new String[0],
				RefactoringStatus.WARNING, "p.B");
	}

	@Test
	public void testFail16() throws Exception{
		IPackageFragment packageForB= getRoot().createPackageFragment("r", false, null);
		fieldMethodTypePackageHelper_failing(new String[]{"f"}, new String[0], new String[0][0], new String[0],
				RefactoringStatus.ERROR, "r.B",
				getPackageP(), packageForB);
	}

	@Test
	public void testFail17() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[0]}, new String[0],
				RefactoringStatus.FATAL, "java.lang.Object");
	}

	@Test
	public void testFail18() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[0]}, new String[0],
				RefactoringStatus.FATAL, "p.DontExist");
	}

	@Test
	public void testFail19() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[0]}, new String[0],
				RefactoringStatus.ERROR, "p.B");
	}

	@Test
	public void testFail20() throws Exception{
		// was same as test19
	}

	@Test
	public void testFail21() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[0]}, new String[0],
				RefactoringStatus.FATAL, "p.B");
	}

	@Test
	public void testFail22() throws Exception{
		//free slot
	}

	@Test
	public void testFail23() throws Exception{
		//free slot
	}

	@Test
	public void testFail24() throws Exception{
		fieldMethodTypeHelper_failing(new String[0],
				new String[]{"m"}, new String[][]{new String[0]}, new String[0],
				RefactoringStatus.FATAL, "p.B");
	}

	// Delegate creation

	@Test
	public void testDelegate01() throws Exception {
		// simple delegate method
		methodHelperDelegate_passing(new String[] { "foo" }, new String[][]{new String[0]});
	}

	@Test
	public void testDelegate02() throws Exception {
		// increase visibility
		methodHelperDelegate_passing(new String[] { "foo" }, new String[][]{new String[0]});
	}

	@Test
	public void testDelegate03() throws Exception {
		// ensure imports are removed correctly
		methodHelperDelegate_passing(new String[] { "foo" }, new String[][]{new String[0]});
	}

	@Test
	public void testDelegate04() throws Exception{
		// add import when moving to another package
		IPackageFragment packageForB= getRoot().createPackageFragment("r", false, null);
		fieldMethodTypePackageHelper_passing(new String[0], new String[] { "foo" }, new String[][]{new String[0]}, new String[0], getPackageP(), packageForB, true);
	}

	@Test
	public void testDelegate05() throws Exception {
		// simple delegate field
		fieldHelperDelegate_passing(new String[] { "FOO" });
	}

	@Test
	public void testDelegate06() throws Exception {
		// increase visibility
		fieldHelperDelegate_passing(new String[] { "FOO" });
	}

	@Test
	public void testDelegate07() throws Exception {
		// remove imports correctly
		fieldHelperDelegate_passing(new String[] { "FOO" });
	}

	@Test
	public void testDelegate08() throws Exception{
		// add import when moving to another package
		IPackageFragment packageForB= getRoot().createPackageFragment("r", false, null);
		fieldMethodTypePackageHelper_passing(new String[] { "FOO" }, new String[0], new String[][]{new String[0]}, new String[0], getPackageP(), packageForB, true);
	}
}
