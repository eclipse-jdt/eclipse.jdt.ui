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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.core.tests.harness.FussyProgressMonitor;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoringProcessor.MemberActionInfo;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d5Setup;

public class PushDownTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "PushDown/";

	public PushDownTests() {
		rts= new Java1d5Setup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private Refactoring createRefactoringPrepareForInputCheck(String[] selectedMethodNames, String[][] selectedMethodSignatures,
			String[] selectedFieldNames,
			String[] namesOfMethodsToPullUp, String[][] signaturesOfMethodsToPullUp,
			String[] namesOfFieldsToPullUp,
			String[] namesOfMethodsToDeclareAbstract, String[][] signaturesOfMethodsToDeclareAbstract,
			ICompilationUnit cu) throws CoreException {

		IType type= getType(cu, "A");
		IMethod[] selectedMethods= getMethods(type, selectedMethodNames, selectedMethodSignatures);
		IField[] selectedFields= getFields(type, selectedFieldNames);
		IMember[] selectedMembers= merge(selectedFields, selectedMethods);

		assertTrue(RefactoringAvailabilityTester.isPushDownAvailable(selectedMembers));
		PushDownRefactoringProcessor processor= new PushDownRefactoringProcessor(selectedMembers);
		Refactoring ref= new ProcessorBasedRefactoring(processor);

		FussyProgressMonitor monitor= new FussyProgressMonitor();
		assertTrue("activation", ref.checkInitialConditions(monitor).isOK());
		monitor.assertUsedUp();

		prepareForInputCheck(processor, selectedMethods, selectedFields, namesOfMethodsToPullUp, signaturesOfMethodsToPullUp, namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
				signaturesOfMethodsToDeclareAbstract);
		return ref;
	}

	private void prepareForInputCheck(PushDownRefactoringProcessor processor, IMethod[] selectedMethods, IField[] selectedFields, String[] namesOfMethodsToPullUp, String[][] signaturesOfMethodsToPullUp, String[] namesOfFieldsToPullUp, String[] namesOfMethodsToDeclareAbstract, String[][] signaturesOfMethodsToDeclareAbstract) {
		IMethod[] methodsToPushDown= findMethods(selectedMethods, namesOfMethodsToPullUp, signaturesOfMethodsToPullUp);
		IField[] fieldsToPushDown= findFields(selectedFields, namesOfFieldsToPullUp);
		List<IMember> membersToPushDown= Arrays.asList(merge(methodsToPushDown, fieldsToPushDown));
		List<IMethod> methodsToDeclareAbstract= Arrays.asList(findMethods(selectedMethods, namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract));

		for (MemberActionInfo info : processor.getMemberActionInfos()) {
			if (membersToPushDown.contains(info.getMember())) {
				info.setAction(MemberActionInfo.PUSH_DOWN_ACTION);
				assertFalse(methodsToDeclareAbstract.contains(info.getMember()));
			}
			if (methodsToDeclareAbstract.contains(info.getMember())) {
				info.setAction(MemberActionInfo.PUSH_ABSTRACT_ACTION);
				assertFalse(membersToPushDown.contains(info.getMember()));
			}
		}
	}

	private void helper(String[] selectedMethodNames, String[][] selectedMethodSignatures,
			String[] selectedFieldNames,
			String[] namesOfMethodsToPullUp, String[][] signaturesOfMethodsToPullUp,
			String[] namesOfFieldsToPullUp, String[] namesOfMethodsToDeclareAbstract,
			String[][] signaturesOfMethodsToDeclareAbstract, String[] additionalCuNames, String[] additionalPackNames) throws Exception{
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");

		IPackageFragment[] additionalPacks= createAdditionalPackages(additionalCuNames, additionalPackNames);
		ICompilationUnit[] additionalCus= createAdditionalCus(additionalCuNames, additionalPacks);

		Refactoring ref= createRefactoringPrepareForInputCheck(selectedMethodNames, selectedMethodSignatures, selectedFieldNames, namesOfMethodsToPullUp, signaturesOfMethodsToPullUp,
				namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, cuA);

		FussyProgressMonitor monitor= new FussyProgressMonitor();
		RefactoringStatus checkInputResult= ref.checkFinalConditions(monitor);
		monitor.assertUsedUp();
		assertFalse("precondition was supposed to pass but got " + checkInputResult.toString(), checkInputResult.hasError());
		performChange(ref, false);

		String expected= getFileContents(getOutputTestFileName("A"));
		String actual= cuA.getSource();
		assertEqualLines("A.java", expected, actual);

		for (int i= 0; i < additionalCus.length; i++) {
			ICompilationUnit unit= additionalCus[i];
			String expectedS= getFileContents(getOutputTestFileName(additionalCuNames[i]));
			String actualS= unit.getSource();
			assertEqualLines(unit.getElementName(), expectedS, actualS);
		}

	}

	private ICompilationUnit[] createAdditionalCus(String[] additionalCuNames, IPackageFragment[] addtionalPacks) throws Exception {
		ICompilationUnit[] additonalCus= new ICompilationUnit[0];
		if (additionalCuNames != null){
			additonalCus= new ICompilationUnit[additionalCuNames.length];
			for (int i= 0; i < additonalCus.length; i++) {
				additonalCus[i]= createCUfromTestFile(addtionalPacks[i], additionalCuNames[i]);
			}
		}
		return additonalCus;
	}

	private IPackageFragment[] createAdditionalPackages(String[] additionalCuNames, String[] additionalPackNames) {
		IPackageFragment[] additionalPacks= new IPackageFragment[0];
		if (additionalPackNames != null){
			additionalPacks= new IPackageFragment[additionalPackNames.length];
			assertEquals(additionalPackNames.length, additionalCuNames.length);
			for (int i= 0; i < additionalPackNames.length; i++) {
				additionalPacks[i]= getRoot().getPackageFragment(additionalPackNames[i]);
			}
		}
		return additionalPacks;
	}

	private void failActivationHelper(String[] selectedMethodNames, String[][] selectedMethodSignatures,
			String[] selectedFieldNames,
			int expectedSeverity) throws Exception{

		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType type= getType(cu, "A");
		IMethod[] selectedMethods= getMethods(type, selectedMethodNames, selectedMethodSignatures);
		IField[] selectedFields= getFields(type, selectedFieldNames);
		IMember[] selectedMembers= merge(selectedFields, selectedMethods);

		assertTrue(RefactoringAvailabilityTester.isPushDownAvailable(selectedMembers));
		PushDownRefactoringProcessor processor= new PushDownRefactoringProcessor(selectedMembers);
		Refactoring ref= new ProcessorBasedRefactoring(processor);

		FussyProgressMonitor monitor= new FussyProgressMonitor();
		assertEquals("activation was expected to fail", expectedSeverity, ref.checkInitialConditions(monitor).getSeverity());
		monitor.assertUsedUp();
	}

	private void failInputHelper(String[] selectedMethodNames, String[][] selectedMethodSignatures,
			String[] selectedFieldNames,
			String[] namesOfMethodsToPullUp, String[][] signaturesOfMethodsToPullUp,
			String[] namesOfFieldsToPullUp, String[] namesOfMethodsToDeclareAbstract,
			String[][] signaturesOfMethodsToDeclareAbstract,
			int expectedSeverity) throws Exception{

		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		Refactoring ref= createRefactoringPrepareForInputCheck(selectedMethodNames, selectedMethodSignatures, selectedFieldNames, namesOfMethodsToPullUp, signaturesOfMethodsToPullUp,
				namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, cu);
		FussyProgressMonitor monitor= new FussyProgressMonitor();
		RefactoringStatus checkInputResult= ref.checkFinalConditions(monitor);
		monitor.assertUsedUp();
		assertEquals("precondition was expected to fail", expectedSeverity, checkInputResult.getSeverity());
	}

	private void addRequiredMembersHelper(String[] fieldNames, String[] methodNames, String[][] methodSignatures, String[] expectedFieldNames, String[] expectedMethodNames, String[][] expectedMethodSignatures) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType type= getType(cu, "A");
		IField[] fields= getFields(type, fieldNames);
		IMethod[] methods= getMethods(type, methodNames, methodSignatures);

		IMember[] members= merge(methods, fields);
		assertTrue(RefactoringAvailabilityTester.isPushDownAvailable(members));
		PushDownRefactoringProcessor processor= new PushDownRefactoringProcessor(members);
		Refactoring ref= new ProcessorBasedRefactoring(processor);
		FussyProgressMonitor monitor= new FussyProgressMonitor();
		assertTrue("activation", ref.checkInitialConditions(monitor).isOK());
		monitor.assertUsedUp();
		monitor.prepare();
		processor.computeAdditionalRequiredMembersToPushDown(monitor);
		monitor.assertUsedUp();
		List<IMember> required= getMembersToPushDown(processor);
		processor.getMemberActionInfos();
		IField[] expectedFields= getFields(type, expectedFieldNames);
		IMethod[] expectedMethods= getMethods(type, expectedMethodNames, expectedMethodSignatures);
		List<IMember> expected= Arrays.asList(merge(expectedFields, expectedMethods));
		assertEquals("incorrect size", expected.size(), required.size());
		for (IMember each : expected) {
			assertTrue ("required does not contain " + each, required.contains(each));
		}
		for (IMember each : required) {
			assertTrue ("expected does not contain " + each, expected.contains(each));
		}
	}

	private static List<IMember> getMembersToPushDown(PushDownRefactoringProcessor processor) {
		MemberActionInfo[] infos= processor.getMemberActionInfos();
		List<IMember> result= new ArrayList<>(infos.length);
		for (MemberActionInfo info : infos) {
			if (info.isToBePushedDown()) {
				result.add(info.getMember());
			}
		}
		return result;
	}

	//--------------------------------------------------------

	@Test
	public void test0() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test1() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test2() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test3() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test4() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test5() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test6() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test7() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test8() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test9() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test10() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test11() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test12() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"f"};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {"f"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test13() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"f"};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {"f"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test14() throws Exception{
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test15() throws Exception{
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test16() throws Exception{
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test17() throws Exception{
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test18() throws Exception{
		String[] selectedMethodNames= {"f", "m"};
		String[][] selectedMethodSignatures= {new String[0], new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test19() throws Exception{
		String[] selectedMethodNames= {"f", "m"};
		String[][] selectedMethodSignatures= {new String[0], new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test20() throws Exception{
		String[] selectedMethodNames= {"f", "m"};
		String[][] selectedMethodSignatures= {new String[0], new String[0]};
		String[] selectedFieldNames= {"i"};
		String[] namesOfMethodsToPushDown= {"f"};
		String[][] signaturesOfMethodsToPushDown= {new String[0]};
		String[] namesOfFieldsToPushDown= {"i"};
		String[] namesOfMethodsToDeclareAbstract= {"m"};
		String[][] signaturesOfMethodsToDeclareAbstract= {new String[0]};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				new String[]{"B"}, new String[]{"p"});
	}

	@Test
	public void test21() throws Exception{
		String[] selectedMethodNames= {"f", "m"};
		String[][] selectedMethodSignatures= {new String[0], new String[0]};
		String[] selectedFieldNames= {"i"};
		String[] namesOfMethodsToPushDown= {"f", "m"};
		String[][] signaturesOfMethodsToPushDown= {new String[0], new String[0]};
		String[] namesOfFieldsToPushDown= {"i"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				new String[]{"B", "C"}, new String[]{"p", "p"});
	}

	@Test
	public void test22() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"bar"};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= selectedFieldNames;
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test23() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"bar"};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= selectedFieldNames;
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test24() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"foo", "bar"};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= selectedFieldNames;
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test25() throws Exception{
		String[] selectedMethodNames= {"foo"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test26() throws Exception{
		String[] selectedMethodNames= {"bar"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test27() throws Exception{
		String[] selectedMethodNames= {"bar"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test28() throws Exception{
//		if (true){
//			printTestDisabledMessage("37175");
//			return;
//		}
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"i", "j"};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {"i", "j"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test29() throws Exception{
		String[] selectedMethodNames= {"foo"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test30() throws Exception{
		String[] selectedMethodNames= {"foo"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test31() throws Exception{
		String[] selectedMethodNames= {"foo"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test32() throws Exception{
		String[] selectedMethodNames= {"foo"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test33() throws Exception{
		String[] selectedMethodNames= {"f", "m"};
		String[][] selectedMethodSignatures= {new String[0], new String[0]};
		String[] selectedFieldNames= {"i"};
		String[] namesOfMethodsToPushDown= {"f", "m"};
		String[][] signaturesOfMethodsToPushDown= {new String[0], new String[0]};
		String[] namesOfFieldsToPushDown= {"i"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				new String[]{"B", "C"}, new String[]{"p", "p"});
	}

	@Ignore("disabled due to missing support for statically imported methods")
	@Test
	public void test34() throws Exception{

		String[] selectedMethodNames= {"f", "m"};
		String[][] selectedMethodSignatures= {new String[0], new String[0]};
		String[] selectedFieldNames= {"i"};
		String[] namesOfMethodsToPushDown= {"f", "m"};
		String[][] signaturesOfMethodsToPushDown= {new String[0], new String[0]};
		String[] namesOfFieldsToPushDown= {"i"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
			   selectedFieldNames,
			   namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
			   namesOfFieldsToPushDown,
			   namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
			   new String[]{"B", "C"}, new String[]{"p", "p"});
	}

	@Test
	public void test35() throws Exception{
		String[] selectedMethodNames= {"foo"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test36() throws Exception {
		String[] selectedMethodNames= { "m" };
		String[][] selectedMethodSignatures= { new String[0] };
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test37() throws Exception {
		String[] selectedMethodNames= { "m" };
		String[][] selectedMethodSignatures= { new String[0] };
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void test38() throws Exception {
		String[] selectedMethodNames= { "m" };
		String[][] selectedMethodSignatures= { new String[0] };
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testFail0() throws Exception {
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};

		failActivationHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				RefactoringStatus.FATAL);
	}

	@Test
	public void testFail1() throws Exception {
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};

		failActivationHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				RefactoringStatus.FATAL);
	}

	@Test
	public void testFail2() throws Exception {
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		failInputHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				RefactoringStatus.ERROR);
	}

	@Test
	public void testFail3() throws Exception {
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"i"};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= selectedFieldNames;
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		failInputHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				RefactoringStatus.ERROR);
	}

	@Test
	public void testVisibility1() throws Exception {
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testVisibility2() throws Exception {
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testVisibility3() throws Exception {
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testFail7() throws Exception {
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		failInputHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				RefactoringStatus.ERROR);
	}

	@Test
	public void testFail8() throws Exception {
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		failInputHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				RefactoringStatus.ERROR);
	}

	@Test
	public void testFail9() throws Exception {
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"f"};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= selectedFieldNames;
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		failInputHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				RefactoringStatus.ERROR);
	}

	@Test
	public void testFail10() throws Exception {
		String[] selectedMethodNames= {"foo"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		failInputHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				RefactoringStatus.ERROR);
	}

	@Test
	public void testFail11() throws Exception {
		String[] selectedMethodNames= {"foo"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		failInputHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				RefactoringStatus.ERROR);
	}

	@Test
	public void testFail12() throws Exception {
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"bar"};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= selectedFieldNames;
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		failInputHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				RefactoringStatus.ERROR);
	}

	@Test
	public void testFail13() throws Exception {
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= { new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= selectedFieldNames;
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		failInputHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				RefactoringStatus.ERROR);
	}

	@Test
	public void testFail14() throws Exception {
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= { new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= selectedFieldNames;
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		failInputHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				RefactoringStatus.ERROR);
	}

	@Test
	public void testFail15() throws Exception {
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= { new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= selectedFieldNames;
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		failInputHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract,
				RefactoringStatus.ERROR);
	}

	@Test
	public void testVisibility0() throws Exception {
		String[] selectedMethodNames= {"foo"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testAddingRequiredMembers0() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	@Test
	public void testAddingRequiredMembers1() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	@Test
	public void testAddingRequiredMembers2() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	@Test
	public void testAddingRequiredMembers3() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= {"m", "f"};
		String[][] expectedMethodSignatures= {new String[0], new String[0]};
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	@Test
	public void testAddingRequiredMembers4() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m", "f"};
		String[][] methodSignatures= {new String[0], new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	@Test
	public void testAddingRequiredMembers5() throws Exception{
		String[] fieldNames= {};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= {"f"};
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	@Test
	public void testAddingRequiredMembers6() throws Exception{
		String[] fieldNames= {"f"};
		String[] methodNames= {"m"};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= methodNames;
		String[][] expectedMethodSignatures= methodSignatures;
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	@Test
	public void testAddingRequiredMembers7() throws Exception{
		String[] fieldNames= {"f"};
		String[] methodNames= {};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= fieldNames;
		String[] expectedMethodNames= {"m"};
		String[][] expectedMethodSignatures= {new String[0]};
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	@Test
	public void testAddingRequiredMembers8() throws Exception{
		String[] fieldNames= {"f"};
		String[] methodNames= {};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= {"f", "m"};
		String[] expectedMethodNames= {};
		String[][] expectedMethodSignatures= {new String[0]};
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	@Test
	public void testAddingRequiredMembers9() throws Exception{
		String[] fieldNames= {"f"};
		String[] methodNames= {};
		String[][] methodSignatures= {new String[0]};

		String[] expectedFieldNames= {"f", "m"};
		String[] expectedMethodNames= {};
		String[][] expectedMethodSignatures= {new String[0]};
		addRequiredMembersHelper(fieldNames, methodNames, methodSignatures, expectedFieldNames, expectedMethodNames, expectedMethodSignatures);
	}

	@Test
	public void testEnablement0() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cu.getType("A");
		IMember[] members= {typeA};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement1() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cu.getType("A");
		IMember[] members= {typeA};
		assertFalse("should be disabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement2() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("Outer").getType("B");
		IMember[] members= {typeB};
		assertFalse("should be disabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement3() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cu.getType("A");
		IType typeB= cu.getType("B");
		IMember[] members= {typeA, typeB};
		assertFalse("should be disabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement4() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cu.getType("A");
		IMember[] members= {typeA};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement5() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cu.getType("A");
		IMember[] members= {typeA};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement6() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cu.getType("A");
		IMember[] members= {typeA};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement7() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cu.getType("A");
		IMember[] members= {typeA};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement8() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cu.getType("A");
		IMember[] members= {typeA};
		assertTrue("should be enabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement9() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cu.getType("A");
		IMember[] members= {typeA};
		assertFalse("should be disabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement10() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cu.getType("A");
		IMember[] members= {typeA};
		assertFalse("should be disabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement11() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cu.getType("A");
		IMember[] members= {typeA};
		assertFalse("should be disabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement12() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("Outer").getType("B");
		IMember[] members= {typeB};
		assertFalse("should be disabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement13() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("Outer").getType("B");
		IMember[] members= {typeB};
		assertFalse("should be disabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement14() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("Outer").getType("B");
		IMember[] members= {typeB};
		assertFalse("should be disabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testEnablement15() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeB= cu.getType("Outer").getType("B");
		IMember[] members= {typeB};
		assertFalse("should be disabled", RefactoringAvailabilityTester.isPushDownAvailable(members));
	}

	@Test
	public void testGenerics0() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics1() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics2() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics3() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics4() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics5() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics6() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics7() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics8() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics9() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics10() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics11() throws Exception{
		String[] selectedMethodNames= {"m"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics12() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"f"};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {"f"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics13() throws Exception{
		String[] selectedMethodNames= {};
		String[][] selectedMethodSignatures= {};
		String[] selectedFieldNames= {"f"};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {"f"};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics14() throws Exception{
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics15() throws Exception{
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics16() throws Exception{
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics17() throws Exception{
		String[] selectedMethodNames= {"f"};
		String[][] selectedMethodSignatures= {new String[0]};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics18() throws Exception{
		String[] selectedMethodNames= {"f", "m"};
		String[][] selectedMethodSignatures= {new String[0], new String[] {"QT;"}};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= selectedMethodNames;
		String[][] signaturesOfMethodsToPushDown= selectedMethodSignatures;
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= {};
		String[][] signaturesOfMethodsToDeclareAbstract= {};

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}

	@Test
	public void testGenerics19() throws Exception{
		String[] selectedMethodNames= {"f", "m"};
		String[][] selectedMethodSignatures= {new String[0], new String[]{"QT;"}};
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPushDown= {};
		String[][] signaturesOfMethodsToPushDown= {};
		String[] namesOfFieldsToPushDown= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		helper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				namesOfMethodsToPushDown, signaturesOfMethodsToPushDown,
				namesOfFieldsToPushDown,
				namesOfMethodsToDeclareAbstract, signaturesOfMethodsToDeclareAbstract, null, null);
	}
}
