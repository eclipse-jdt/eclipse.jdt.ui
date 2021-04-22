/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureProcessor;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class RenameParametersTests extends GenericRefactoringTest{
	private static final String REFACTORING_PATH= "RenameParameters/";

	public RenameParametersTests() {
		rts= new RefactoringTestSetup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private String getSimpleTestFileName(boolean canRename, boolean input){
		StringBuilder fileName = new StringBuilder("A_").append(getName());
		if (canRename)
			fileName.append(input ? "_in": "_out");
		return fileName.append(".java").toString();
	}

	private String getTestFileName(boolean canRename, boolean input){
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canRename ? "canRename/": "cannotRename/");
		return fileName.append(getSimpleTestFileName(canRename, input)).toString();
	}

	//------------
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canRename, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canRename, input), getFileContents(getTestFileName(canRename, input)));
	}

	private void helper1(String[] newNames, String[] signature) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method= classA.getMethod("m", signature);
		assertTrue("refactoring not available", RefactoringAvailabilityTester.isChangeSignatureAvailable(method));
		ChangeSignatureProcessor processor= new ChangeSignatureProcessor(method);
		Refactoring ref= new ProcessorBasedRefactoring(processor);
		//ref.setUpdateReferences(updateReferences);
		//ref.setNewParameterNames(newNames);
		//ref.setNewNames(createRenamings(method, newNames));
		modifyInfos(processor.getParameterInfos(), newNames);

		RefactoringStatus result= performRefactoring(ref);
		assertNull("precondition was supposed to pass", result);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEquals("invalid renaming", getFileContents(getTestFileName(true, false)).length(), newcu.getSource().length());
		assertEqualLines("invalid renaming", getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	private void helper2(String[] newNames, String[] signature) throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), false, false), "A");
		//DebugUtils.dump("classA" + classA);
		IMethod method= classA.getMethod("m", signature);
		assertTrue("refactoring not available", RefactoringAvailabilityTester.isChangeSignatureAvailable(method));
		ChangeSignatureProcessor processor= new ChangeSignatureProcessor(method);
		Refactoring ref= new ProcessorBasedRefactoring(processor);
		modifyInfos(processor.getParameterInfos(), newNames);

		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
	}

	private void modifyInfos(List<ParameterInfo> list, String[] newNames) {
		int i= 0;
		for (Iterator<ParameterInfo> iter= list.iterator(); iter.hasNext(); i++) {
			ParameterInfo info= iter.next();
			info.setNewName(newNames[i]);
		}
	}


	@Test
	public void test0() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test1() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test2() throws Exception{
		helper1(new String[]{"j", "k"}, new String[]{"I", "I"});
	}

	@Test
	public void test3() throws Exception{
		helper1(new String[]{"j", "j1"}, new String[]{"I", "I"});
	}

	@Test
	public void test4() throws Exception{
		helper1(new String[]{"k"}, new String[]{"QA;"});
	}

	@Test
	public void test5() throws Exception{
		helper1(new String[]{"k"}, new String[]{"I"});
	}

	@Test
	public void test6() throws Exception{
		helper1(new String[]{"k"}, new String[]{"I"});
	}

	@Test
	public void test7() throws Exception{
		helper1(new String[]{"k"}, new String[]{"QA;"});
	}

	@Test
	public void test8() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test9() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test10() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Ignore("revisit in the context of anonymous types in type hierarchies")
	@Test
	public void test11() throws Exception{
		 helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test12() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test13() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test14() throws Exception{
		helper1(new String[]{"j"}, new String[]{"QA;"});
	}

	@Test
	public void test15() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}

	@Test
	public void test16() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test17() throws Exception{
		helper1(new String[]{"j", "i", "k"}, new String[]{"I", "I", "I"});
	}

	@Test
	public void test18() throws Exception{
		helper1(new String[]{"j"}, new String[]{"QObject;"});
	}

	@Test
	public void test19() throws Exception{
		helper1(new String[]{"j"}, new String[]{"QA;"});
	}

	@Test
	public void test20() throws Exception{
		helper1(new String[]{"j"}, new String[]{"Qi;"});
	}

	@Test
	public void test21() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test22() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test23() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}

	@Test
	public void test24() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test25() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Ignore("revisit in the context of anonymous types in type hierarchies")
	@Test
	public void test26() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

//removed - see testFail21
//	public void test27() throws Exception{
//		helper1(new String[]{"j"}, new String[]{"I"});
//	}

	@Test
	public void test28() throws Exception{
		helper1(new String[]{"j"}, new String[]{"[I"});
	}

	@Test
	public void test29() throws Exception{
		helper1(new String[]{"b"}, new String[]{"QA;"});
	}

	@Test
	public void test30() throws Exception{
		helper1(new String[]{"i", "k"}, new String[]{"I", "I"});
	}

	@Test
	public void test31() throws Exception{
		helper1(new String[]{"kk", "j"}, new String[]{"I", "I"});
	}

	@Ignore("must do - constructor params")
	@Test
	public void test32() throws Exception{
	}

	@Ignore("revisit - removed the 'no ref update' option")
	@Test
	public void test33() throws Exception{
//		helper1(new String[]{"b"}, new String[]{"QA;"}, false);
	}

	@Test
	public void test34() throws Exception{
//		printTestDisabledMessage("regression test for bug#9001");
		helper1(new String[]{"test2"}, new String[]{"Z"});
	}

	@Ignore("regression test for bug#6224")
	@Test
	public void test35() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void test36() throws Exception{
//		printTestDisabledMessage("regression test for bug#21163");
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	// -----

	@Ignore("must fix - name collision with an instance var")
	@Test
	public void testFail0() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void testFail1() throws Exception{
		helper2(new String[0], new String[0]);
	}

	@Test
	public void testFail2() throws Exception{
		helper2(new String[]{"i", "i"}, new String[]{"I", "I"});
	}

	@Test
	public void testFail3() throws Exception{
		helper2(new String[]{"i", "9"}, new String[]{"I", "I"});
	}

	@Test
	public void testFail4() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void testFail5() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Ignore("must fix - name collision with an instance var")
	@Test
	public void testFail6() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Ignore("waiting for better conflict detection story from DB")
	@Test
	public void testFail7() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Ignore("waiting for better conflict detection story from DB")
	@Test
	public void testFail8() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Ignore("waiting for better conflict detection story from DB")
	@Test
	public void testFail9() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void testFail10() throws Exception{
		helper2(new String[]{"j", "j"}, new String[]{"I", "I"});
	}

	@Test
	public void testFail11() throws Exception{
		helper2(new String[]{"j", "j"}, new String[]{"I", "I"});
	}

	@Ignore("waiting for better conflict detection story from DB")
	@Test
	public void testFail12() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Ignore("waiting for better conflict detection story from DB")
	@Test
	public void testFail13() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Ignore("waiting for better conflict detection story from DB")
	@Test
	public void testFail14() throws Exception{
		helper2(new String[]{"j"}, new String[]{"QA;"});
	}

	@Test
	public void testFail15() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void testFail16() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void testFail17() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Test
	public void testFail18() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Ignore("waiting for better conflict detection story from DB")
	@Test
	public void testFail19() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Ignore("waiting for better conflict detection story from DB")
	@Test
	public void testFail20() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	@Ignore("Disabled since 1.4 compliance level doesn't produce error message")
	@Test
	public void testFail21() throws Exception{
		 helper2(new String[]{"j"}, new String[]{"I"});
	}
}
