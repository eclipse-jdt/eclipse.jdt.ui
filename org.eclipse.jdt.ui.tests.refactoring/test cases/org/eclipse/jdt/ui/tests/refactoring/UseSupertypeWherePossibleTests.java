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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.refactoring.descriptors.UseSupertypeDescriptor;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class UseSupertypeWherePossibleTests extends GenericRefactoringTest {

	private static final String REFACTORING_PATH= "UseSupertypeWherePossible/";

	public UseSupertypeWherePossibleTests() {
		rts= new RefactoringTestSetup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	@Before
	public void before() throws Exception {
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID,
				"${package_declaration}" +
					System.getProperty("line.separator", "\n") +
				"${"+ CodeTemplateContextType.TYPE_COMMENT+"}" +
				System.getProperty("line.separator", "\n") +
				"${type_declaration}", null);

			StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "/** typecomment template*/", null);
	}

	private IType getClassFromTestFile(IPackageFragment pack, String className) throws Exception{
		return getType(createCUfromTestFile(pack, className), className);
	}

	private void validatePassingTest(String className, String[] cuNames, String superTypeFullName, boolean replaceInstanceOf) throws Exception {
		final IType subType= getClassFromTestFile(getPackageP(), className);
		final ICompilationUnit[] units= new ICompilationUnit[cuNames.length];
		for (int i= 0; i < cuNames.length; i++) {
			if (cuNames[i].equals(subType.getCompilationUnit().findPrimaryType().getElementName()))
				units[i]= subType.getCompilationUnit();
			else
				units[i]= createCUfromTestFile(subType.getPackageFragment(), cuNames[i]);

		}
		final IType superType= subType.getJavaProject().findType(superTypeFullName, (IProgressMonitor) null);
		final UseSupertypeDescriptor descriptor= RefactoringSignatureDescriptorFactory.createUseSupertypeDescriptor();
		descriptor.setSubtype(subType);
		descriptor.setSupertype(superType);
		descriptor.setReplaceInstanceof(replaceInstanceOf);
		final RefactoringStatus status= new RefactoringStatus();
		final Refactoring refactoring= descriptor.createRefactoring(status);
		assertTrue("status should be ok", status.isOK());
		assertNotNull("refactoring should not be null", refactoring);
		assertNull("was supposed to pass", performRefactoring(refactoring));

		for (int i= 0; i < units.length; i++) {
			String expected= getFileContents(getOutputTestFileName(cuNames[i]));
			String actual= units[i].getSource();
			String message= "incorrect changes in " + units[i].getElementName();
			assertEqualLines(message, expected, actual);
		}
	}

	private void validatePassingTest(String className, String[] cuNames, String superTypeFullName) throws Exception {
		validatePassingTest(className, cuNames, superTypeFullName, false);
	}

	//---------------tests ----------------------

	@Test
	public void testNew0() throws Exception{
		validatePassingTest("A", new String[]{"A"}, "java.lang.Object");
	}

	@Test
	public void testNew1() throws Exception{
//		printTestDisabledMessage("bug 23597 ");
		validatePassingTest("A", new String[]{"A"}, "java.lang.Object");
	}

	@Test
	public void testNew2() throws Exception{
		validatePassingTest("A", new String[]{"A"}, "java.lang.Object");
	}

	@Test
	public void testNew3() throws Exception{
		validatePassingTest("A", new String[]{"A"}, "java.lang.Object");
	}

	@Test
	public void testNew4() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test"}, "java.lang.Object");
	}

	@Test
	public void testNew5() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test"}, "java.lang.Object");
	}

	@Test
	public void testNew6() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test"}, "java.lang.Object");
	}

	@Test
	public void testNew7() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test", "B"}, "p.B");
	}

	@Test
	public void testNew8() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test", "B"}, "p.B");
	}

	@Test
	public void testNew9() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test"}, "java.lang.Object");
	}

	@Test
	public void testNew10() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test", "B"}, "p.B");
	}

	@Test
	public void testNew11() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test", "B"}, "p.B");
	}

	@Test
	public void testNew12() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test", "B"}, "p.B");
	}

	@Test
	public void testNew13() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test", "B"}, "p.B");
	}

	@Test
	public void testNew14() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test", "B"}, "p.B");
	}

	@Test
	public void testNew15() throws Exception{
		validatePassingTest("A", new String[]{"A", "Test", "B"}, "p.B");
	}

	@Test
	public void testNew16() throws Exception{
//		printTestDisabledMessage("instanceof ");
		validatePassingTest("A", new String[]{"A", "Test", "B"}, "p.B");
	}

	@Test
	public void testNew17() throws Exception{
		validatePassingTest("A", new String[]{"A"}, "p.C");
	}

	@Test
	public void testNew18() throws Exception{
		validatePassingTest("A", new String[]{"A"}, "p.B");
	}

	@Test
	public void testNew19() throws Exception{
		validatePassingTest("A", new String[]{"A"}, "p.I");
	}

	@Test
	public void testNew20() throws Exception{
//		printTestDisabledMessage("http://dev.eclipse.org/bugs/show_bug.cgi?id=23829");
		validatePassingTest("A", new String[]{"A"}, "p.B");
	}

	@Test
	public void testNew21() throws Exception{
		validatePassingTest("A", new String[]{"A"}, "java.lang.Object");
	}

	@Test
	public void testNew22() throws Exception{
		validatePassingTest("A", new String[]{"A", "B", "Test"}, "p.B");
	}

	@Test
	public void testNew23() throws Exception{
		validatePassingTest("A", new String[]{"A", "B", "Test"}, "java.lang.Object");
	}

	@Test
	public void testNew24() throws Exception{
		validatePassingTest("A", new String[]{"A", "B"}, "java.lang.Object");
	}

	@Test
	public void testNew25() throws Exception{
		validatePassingTest("A", new String[]{"A", "B", "C"}, "java.lang.Object");
	}

	@Test
	public void testNew26() throws Exception{
		validatePassingTest("A", new String[]{"A", "B"}, "java.lang.Object");
	}

	@Test
	public void testNew27() throws Exception{
		validatePassingTest("A", new String[]{"A", "B"}, "p.B");
	}

	@Test
	public void testNew28() throws Exception{
		validatePassingTest("A", new String[]{"A", "B"}, "p.B");
	}

	@Test
	public void testNew29() throws Exception{
//		printTestDisabledMessage("bug 24278");
		validatePassingTest("A", new String[]{"A"}, "p.B");
	}

	@Test
	public void testNew30() throws Exception{
//		printTestDisabledMessage("bug 24278");
		validatePassingTest("A", new String[]{"A"}, "p.B");
	}

	@Test
	public void testNew31() throws Exception{
//		printTestDisabledMessage("bug 24278");
		validatePassingTest("A", new String[]{"A"}, "p.B");
	}

	@Test
	public void testNew32() throws Exception{
//		printTestDisabledMessage();
		validatePassingTest("A", new String[]{"A"}, "p.B");
	}
	@Test
    public void testNew33() throws Exception{
    //		printTestDisabledMessage("bug 26282");
       validatePassingTest("A", new String[]{"A"}, "java.util.Vector");
	}
	@Test
    public void testNew34() throws Exception{
    //		printTestDisabledMessage("bug 26282");
       validatePassingTest("A", new String[]{"A"}, "java.util.Vector");
    }
	@Test
    public void testNew35() throws Exception{
    //		printTestDisabledMessage("bug 26282");
       validatePassingTest("A", new String[]{"A"}, "java.util.Vector");
    }

	@Test
	public void testNew36() throws Exception{
	//		printTestDisabledMessage("bug 26288");
	   validatePassingTest("A", new String[]{"A"}, "p.B");
	}

	@Test
	public void testNew37() throws Exception{
	//		printTestDisabledMessage("bug 26288");
	   validatePassingTest("A", new String[]{"A"}, "p.B");
	}

	@Test
	public void testNew38() throws Exception{
	//		printTestDisabledMessage("bug 40373");
	   validatePassingTest("A", new String[]{"A"}, "p.B");
	}

	@Test
	public void testNew39() throws Exception{
		//		printTestDisabledMessage("bug 169608");
		validatePassingTest("C", new String[]{"C"}, "p.B");
	}

	/* i had to rename tests 0-15 because of cvs problems*/


	@Test
	public void test0_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test1_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test2_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test3_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test4_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test5_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test6_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test7_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test8_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test9_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test10_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test11_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test12_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test13_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test14_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test15_() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test16() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test17() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test18() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test19() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test20() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test21() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test22() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test23() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test24() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test25() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test26() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test27() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test28() throws Exception{
//		printTestDisabledMessage("bug 22883");
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test29() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test30() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test31() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test32() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test33() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test34() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test35() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test36() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test37() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test38() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test39() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test40() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test41() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test42() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test43() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test44() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test45() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test46() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test47() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test48() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test49() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test50() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test51() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test52() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test53() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test54() throws Exception{
		validatePassingTest("A", new String[]{"A", "A1", "I"}, "p.I");
	}
	@Test
	public void test55() throws Exception{
		validatePassingTest("A", new String[]{"A", "A1", "I"}, "p.I");
	}
	@Test
	public void test56() throws Exception{
		validatePassingTest("A", new String[]{"A", "A1", "I"}, "p.I");
	}
	@Test
	public void test57() throws Exception{
		validatePassingTest("A", new String[]{"A", "A1", "I"}, "p.I");
	}
	@Test
	public void test58() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test59() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test60() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test61() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test62() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test63() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test64() throws Exception{
		validatePassingTest("A", new String[]{"A", "I", "Inter"}, "p.I");
	}
	@Test
	public void test65() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test66() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test67() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
	@Test
	public void test68() throws Exception{
		validatePassingTest("A", new String[]{"A", "As", "I"}, "p.I");
	}
	@Test
	public void test69() throws Exception{
		validatePassingTest("A", new String[]{"A", "As", "I"}, "p.I");
	}
	@Test
	public void test70() throws Exception{
		validatePassingTest("A", new String[]{"A", "I"}, "p.I");
	}
}
