/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Samrat Dhillon samrat.dhillon@gmail.com - [move member type] Moving a member interface to its own file adds the host's type parameters to it - https://bugs.eclipse.org/bugs/show_bug.cgi?id=385237
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d5Setup;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class MoveInnerToTopLevelTests extends GenericRefactoringTest {

	private static final String FIELD_COMMENT= "/** Comment */";
	private static final String REFACTORING_PATH= "MoveInnerToTopLevel/";

	private static final int NOT_AVAILABLE= 1001;

	private String fCompactPref;

	public MoveInnerToTopLevelTests() {
		rts= new Java1d5Setup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	@Before
	public void before() throws Exception {
		StubUtility.setCodeTemplate(CodeTemplateContextType.FIELDCOMMENT_ID, FIELD_COMMENT, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID,
			"${package_declaration}" +
			System.getProperty("line.separator", "\n") +
			"${type_declaration}", null);

		Hashtable<String, String> options= JavaCore.getOptions();

		String setting= DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR;
		fCompactPref= options.get(setting);
		options.put(setting, DefaultCodeFormatterConstants.TRUE);
		JavaCore.setOptions(options);
	}

	@After
	public void after() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, fCompactPref);
		JavaCore.setOptions(options);
	}


	private IType getClassFromTestFile(IPackageFragment pack, String className) throws Exception{
		return getType(createCUfromTestFile(pack, className), className);
	}

	private void validatePassingTestSecondaryType(String primaryTypeName, String secondaryTypeName, String packageName, String[] cuNames, String[] packageNames, String enclosingInstanceName, boolean makeFinal, boolean possible, boolean mandatory, boolean createFieldIfPossible) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackage(packageName), primaryTypeName);
		IType secType= getType(cu, secondaryTypeName);
		validatePassingTest(secondaryTypeName, secType, cuNames, packageNames, enclosingInstanceName, makeFinal, possible, mandatory, createFieldIfPossible);
	}

	private void validatePassingTest(String parentClassName, String className, String packageName, String[] cuNames, String[] packageNames, String enclosingInstanceName, boolean makeFinal, boolean possible, boolean mandatory, boolean createFieldIfPossible) throws Exception {
		IType parentClas= getClassFromTestFile(getPackage(packageName), parentClassName);
		IType clas= parentClas.getType(className);
		validatePassingTest(className, clas, cuNames, packageNames, enclosingInstanceName, makeFinal, possible, mandatory, createFieldIfPossible);
	}

	private void validatePassingTest(String parentClassName, String parentClassNameInParent, String className, String packageName, String[] cuNames, String[] packageNames, String enclosingInstanceName, boolean makeFinal, boolean possible, boolean mandatory, boolean createFieldIfPossible) throws Exception {
		IType parentClas= getClassFromTestFile(getPackage(packageName), parentClassName);
		IType parent2 = parentClas.getType(parentClassNameInParent);
		IType clas = parent2.getType(className);
		validatePassingTest(className, clas, cuNames, packageNames, enclosingInstanceName, makeFinal, possible, mandatory, createFieldIfPossible);
	}

	private void validatePassingTest(String className, IType clas, String[] cuNames, String[] packageNames, String enclosingInstanceName, boolean makeFinal, boolean possible, boolean mandatory, boolean createFieldIfPossible) throws JavaModelException, CoreException, Exception, IOException {
		assertTrue("should be enabled", RefactoringAvailabilityTester.isMoveInnerAvailable(clas));
		MoveInnerToTopRefactoring ref= ((RefactoringAvailabilityTester.isMoveInnerAvailable(clas)) ? new MoveInnerToTopRefactoring(clas, JavaPreferencesSettings.getCodeGenerationSettings(clas.getJavaProject())) : null);
		assertNotNull("Move to inner refactoring should be available", ref);
		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());
		assertTrue("activation was supposed to be successful" + preconditionResult.toString(), preconditionResult.isOK());

		assertEquals("reference creation possible", possible, ref.isCreatingInstanceFieldPossible());
		assertEquals("reference creation mandatory", mandatory, ref.isCreatingInstanceFieldMandatory());
		if (ref.isCreatingInstanceFieldPossible() && ! ref.isCreatingInstanceFieldMandatory())
			ref.setCreateInstanceField(createFieldIfPossible);
		if (enclosingInstanceName != null){
			ref.setEnclosingInstanceName(enclosingInstanceName);
			assertTrue("name should be ok ", ref.checkEnclosingInstanceName(enclosingInstanceName).isOK());
		}
		ref.setMarkInstanceFieldAsFinal(makeFinal);
		ICompilationUnit[] cus= new ICompilationUnit[cuNames.length];
		for (int i= 0; i < cuNames.length; i++) {
			if (cuNames[i].equals(clas.getCompilationUnit().findPrimaryType().getElementName()))
				cus[i]= clas.getCompilationUnit();
			else
				cus[i]= createCUfromTestFile(getPackage(packageNames[i]), cuNames[i]);
		}

		RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
		assertFalse("precondition was supposed to pass", checkInputResult.hasError());
		performChange(ref, false);

		for (int i= 0; i < cus.length; i++) {
			String actual= cus[i].getSource();
			String expected= getFileContents(getOutputTestFileName(cuNames[i]));
			assertEqualLines(cus[i].getElementName(), expected, actual);
		}
		ICompilationUnit newCu= clas.getPackageFragment().getCompilationUnit(className + ".java");
		String expected= getFileContents(getOutputTestFileName(className));
		String actual= newCu.getSource();
		assertEqualLines("new Cu:", expected, actual);
	}


	private void validatePassingTest(String parentClassName, String className, String[] cuNames, String[] packageNames, String enclosingInstanceName, boolean possible, boolean mandatory) throws Exception {
		validatePassingTest(parentClassName, className, "p", cuNames, packageNames, enclosingInstanceName, false, possible, mandatory, true);
	}

	private void validateFailingTest(String parentClassName, String className, String[] cuNames, String[] packageNames, String enclosingInstanceName, int expectedSeverity) throws Exception {
		IType parentClas= getClassFromTestFile(getPackageP(), parentClassName);
		Thread.sleep(800); // its magic..
		IType clas= parentClas.getType(className);

		MoveInnerToTopRefactoring ref= ((RefactoringAvailabilityTester.isMoveInnerAvailable(clas)) ? new MoveInnerToTopRefactoring(clas, JavaPreferencesSettings.getCodeGenerationSettings(clas.getJavaProject())) : null);
		if (expectedSeverity == NOT_AVAILABLE && ref == null)
			return;
		assertEquals("refactoring availability not as expected", expectedSeverity == NOT_AVAILABLE, ref == null);

		RefactoringStatus preconditionResult= ref.checkInitialConditions(new NullProgressMonitor());

		if (enclosingInstanceName != null){
			ref.setEnclosingInstanceName(enclosingInstanceName);
		}
		ref.setMarkInstanceFieldAsFinal(false);
		ICompilationUnit[] cus= new ICompilationUnit[cuNames.length];
		for (int i= 0; i < cuNames.length; i++) {
			if (cuNames[i].equals(clas.getCompilationUnit().findPrimaryType().getElementName()))
				cus[i]= clas.getCompilationUnit();
			else
				cus[i]= createCUfromTestFile(getPackage(packageNames[i]), cuNames[i]);
		}

		RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());

		RefactoringStatus result= new RefactoringStatus();
		result.merge(preconditionResult);
		result.merge(checkInputResult);
		assertEquals("different severity expected", expectedSeverity, result.getSeverity());
	}
	private IPackageFragment getPackage(String name) throws JavaModelException {
		if ("p".equals(name))
			return getPackageP();
		IPackageFragment pack= getRoot().getPackageFragment(name);
		if (pack.exists())
			return pack;
		return getRoot().createPackageFragment(name, false, new NullProgressMonitor());
	}


	//-- tests

	@Test
	public void test0() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test1() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test2() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test3() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test4() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test5() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test6() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}
	@Test
	public void test7() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test8() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}
	@Test
	public void test9() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test10() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}
	@Test
	public void test11() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test12() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}
	@Test
	public void test13() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test14() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test15() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"}, null, false, false);
	}

	@Test
	public void test16() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"}, null, false, false);
	}

	@Test
	public void test17() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test18() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"}, null, false, false);
	}

	@Ignore("bug 23078")
	@Test
	public void test19() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"}, null, false, false);
	}

	@Test
	public void test20() throws Exception{
//		printTestDisabledMessage("bug 23077 ");
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"}, null, false, false);
	}

	@Test
	public void test21() throws Exception{
//		printTestDisabledMessage("bug 23627");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}
	@Test
	public void test22() throws Exception{
//		printTestDisabledMessage("bug 23627");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test23() throws Exception{
//		printTestDisabledMessage("bug 24576 ");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test24() throws Exception{
//		printTestDisabledMessage("bug 28816 ");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	@Test
	public void test25() throws Exception{
//		printTestDisabledMessage("bug 39716");
		validatePassingTest("A", "Inner", "", new String[]{"A"}, new String[]{""}, null, false, false, false, true);
	}

	@Test
	public void test26() throws Exception{
		validatePassingTest("A", "Inner", "", new String[]{"A"}, new String[]{""}, null, false, true, true, true);
	}

	@Test
	public void test30() throws Exception{
		validatePassingTest("A", "Inner", "", new String[]{"A"}, new String[]{""}, null, false, true, true, true);
	}

	@Ignore("disabled due to missing support for statically imported methods")
	@Test
	public void test31() throws Exception{
		 validatePassingTest("A", "Inner", "", new String[]{"A"}, new String[]{""}, null, false, true, true, true);
	}

	// ---- Visibility issues with the moved member itself and its parents

	// Move inner class; enclosing class must remain private if not  used
	@Test
	public void test32() throws Exception{
		validatePassingTest("A", "Inner", "MoreInner", "p1", new String[]{"A"}, new String[]{"p1"}, null, false, false, false, false);
	}

	// Move inner class which has access to enclosing private class, enclosing class must be increased in visibility
	@Test
	public void test33() throws Exception{
		validatePassingTest("A", "Inner", "MoreInner", "p2", new String[]{"A"}, new String[]{"p2"}, null, false, false, false, false);
	}

	// --- Visibility issues with members of moved members

	// Move inner class which has private members, which are accessed from enclosing types.
	@Test
	public void test34() throws Exception {
		validatePassingTest("A", "SomeClass", "p", new String[] { "A"}, new String[] { "p"}, null, false, true, false, false);
	}

	// Move inner class which has private members, but they are unused (and must remain private)
	@Test
	public void test35() throws Exception {
		validatePassingTest("A", "Inner", "p", new String[] { "A"}, new String[] { "p"}, null, false, true, false, false);
	}

	// Move inner class which has access private members, and accessing private members of
	// enclosing class (4 visibility increments)
	@Test
	public void test36() throws Exception {
		validatePassingTest("A", "SomeInner", "Inner", "p", new String[] { "A"}, new String[] { "p"}, null, false, false, false, false);
	}

	// Move inner class with some private used and some private non-used members.
	// used members go default, non-used stay private
	// bug 97411 + 117465 (comment #1)
	@Test
	public void test37() throws Exception {
		validatePassingTest("A", "SomeInner", "p", new String[] { "A"}, new String[] { "p"}, null, false, false, false, false);
	}

	@Test
	public void test38() throws Exception {
		validatePassingTest("A", "B", "p", new String[] { "A"}, new String[] { "p"}, null, false, false, false, false);
	}

	// change visibility: https://bugs.eclipse.org/319069
	@Test
	public void test39() throws Exception {
		validatePassingTest("A", "B", "p", new String[] { "A"}, new String[] { "p"}, null, false, true, false, false);
	}

	// change visibility: https://bugs.eclipse.org/319069
	@Test
	public void test40() throws Exception {
		validatePassingTest("A", "B", "p", new String[] { "A"}, new String[] { "p"}, null, false, true, false, false);
	}

	// change visibility: https://bugs.eclipse.org/319069
	@Test
	public void test41() throws Exception {
		validatePassingTest("A", "B", "p", new String[] { "A"}, new String[] { "p"}, null, false, true, false, false);
	}

	// change visibility: https://bugs.eclipse.org/319069
	@Test
	public void test42() throws Exception {
		validatePassingTest("A", "B", "p", new String[] { "A"}, new String[] { "p"}, null, false, true, false, false);
	}

	@Test
	public void test43() throws Exception {
		validatePassingTest("A", "B", "p", new String[] { "A"}, new String[] { "p"}, "", false, true, false, false);
	}

	@Test
	public void test44() throws Exception {
		validatePassingTest("A", "B", "p", new String[] { "A"}, new String[] { "p"}, "a", true, true, true, true);
	}

	@Test
	public void testBug310510() throws Exception {
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	// static context: https://bugs.eclipse.org/bugs/show_bug.cgi?id=385237
	@Test
	public void test_static_context_0() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null, false, false);
	}

	// --- Non static

	@Test
	public void test_nonstatic_0() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}

	@Test
	public void test_nonstatic_1() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_2() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_3() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_4() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_5() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_6() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_7() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_8() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_9() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_10() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_11() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_12() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, true);
	}
	@Test
	public void test_nonstatic_13() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, true);
	}
	@Test
	public void test_nonstatic_14() throws Exception{
//		printTestDisabledMessage("bug 23488");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_15() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_16() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_17() throws Exception{
//		printTestDisabledMessage("bug 23488");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_18() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_19() throws Exception{
//		printTestDisabledMessage("bug 23464 ");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_20() throws Exception{
//		printTestDisabledMessage("bug 23464 ");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_21() throws Exception{
//		printTestDisabledMessage("must fix - consequence of fix for 23464");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_22() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_23() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_24() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_25() throws Exception{
//		printTestDisabledMessage("bug 23464 ");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_26() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_27() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_28() throws Exception{
//		printTestDisabledMessage("test for bug 23725");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Ignore("test for bug 23724")
	@Test
	public void test_nonstatic_29() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}
	@Test
	public void test_nonstatic_30() throws Exception{
//		printTestDisabledMessage("test for bug 23715");
		validatePassingTest("A", "Inner", "p", new String[]{"A"}, new String[]{"p"}, "a", true, true, false, true);
	}

	@Test
	public void test_nonstatic_31() throws Exception{
//		printTestDisabledMessage("test for bug 25537");
		validatePassingTest("A", "Inner", "p", new String[]{"A"}, new String[]{"p"}, "a", true, true, true, true);
	}

	@Test
	public void test_nonstatic_32() throws Exception{
//		printTestDisabledMessage("test for bug 25537");
		validatePassingTest("A", "Inner", "p", new String[]{"A"}, new String[]{"p"}, "a", true, true, true, true);
	}

	@Test
    public void test_nonstatic_33() throws Exception{
//		printTestDisabledMessage("test for bug 26252");
        validatePassingTest("A", "I", "p", new String[]{"A"}, new String[]{"p"}, "a", true, true, false, true);
    }

	@Test
	public void test_nonstatic_34() throws Exception{
//		printTestDisabledMessage("test for bug 31861");
		validatePassingTest("A", "Inner", "p", new String[]{"A"}, new String[]{"p"}, "a", true, true, true, true);
	}

	@Test
	public void test_nonstatic_35() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, false);
	}

	@Test
	public void test_nonstatic_36() throws Exception{
//		printTestDisabledMessage("test for bug 34591");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, true);
	}

	@Test
	public void test_nonstatic_37() throws Exception{
//		printTestDisabledMessage("test for bug 38114");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", true, true);
	}

	@Test
	public void test_nonstatic_38() throws Exception{
//		printTestDisabledMessage("test for bug 37540");
		validatePassingTest("A", "Inner", "p", new String[]{"A"}, new String[]{"p"}, "a", false, true, false, false);
	}

	@Test
	public void test_nonstatic_39() throws Exception{
//		printTestDisabledMessage("test for bug 37540");
		validatePassingTest("A", "Inner", "p", new String[]{"A"}, new String[]{"p"}, "a", false, true, false, false);
	}

	@Test
	public void test_nonstatic_40() throws Exception{
//		printTestDisabledMessage("test for bug 77083");
		validatePassingTest("A", "Inner", "p", new String[]{"A"}, new String[]{"p"}, "a", false, true, false, false);
	}

	@Test
	public void test_nonstatic_41() throws Exception{
		validatePassingTest("A", "Inner", "p", new String[]{"A"}, new String[]{"p"}, "a", false, true, false, false);
	}

	@Ignore("disabled due to missing support for statically imported methods")
	@Test
	public void test_nonstatic_42() throws Exception{
		validatePassingTest("A", "Inner", "p", new String[]{"A"}, new String[]{"p"}, "a", false, true, false, false);
	}

	// Using member of enclosing type, non-static edition
	@Test
	public void test_nonstatic_43() throws Exception{
		validatePassingTest("A", "Inner", "MoreInner", "p5", new String[]{"A"}, new String[]{"p5"}, "inner", true, true, true, true);
	}

	// Move inner class and create field; enclosing class must be changed to use default visibility.
	@Test
	public void test_nonstatic_44() throws Exception{
		validatePassingTest("A", "Inner", "MoreInner", "p2", new String[]{"A"}, new String[]{"p2"}, "p", true, true, false, true);
	}

	@Test
	public void test_nonstatic_45() throws Exception {
		validatePassingTest("A", "B", "p", new String[] { "A" }, new String[] { "p" }, null, false, true, false, false);
	}

	@Test
	public void test_nonstatic_46() throws Exception {
		validatePassingTest("A", "B", "p", new String[] { "A" }, new String[] { "p" }, null, false, true, false, false);
	}

	@Test
	public void test_nonstatic_47() throws Exception {
		validatePassingTest("A", "B", "p", new String[] { "A" }, new String[] { "p" }, null, false, true, false, false);
	}

	@Test
	public void test_nonstatic_48() throws Exception {
		String fileCommentTemplate= "/** Copy right or wrong. */";
		StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, fileCommentTemplate, null);
		String newFileTemplate= "${filecomment}\n${package_declaration}\n\n${typecomment}\n${type_declaration}";
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, newFileTemplate, null);

		try {
			validatePassingTest("A", "B", "p", new String[] { "A" }, new String[] { "p" }, null, false, true, false, false);
		} finally {
			fileCommentTemplate= "/**\n * \n */";
			StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, fileCommentTemplate, null);
		}
	}

	@Test
	public void test_nonstatic_49() throws Exception {
		validatePassingTest("A", "B", "p", new String[] { "A" }, new String[] { "p" }, "a", false, true, true, true);
	}

	@Test
	public void testFail_nonstatic_0() throws Exception{
		validateFailingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", RefactoringStatus.ERROR);
	}
	@Test
	public void testFail_nonstatic_1() throws Exception{
		validateFailingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", RefactoringStatus.ERROR);
	}
	@Test
	public void testFail_nonstatic_2() throws Exception{
		validateFailingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", RefactoringStatus.ERROR);
	}

	@Test
	public void testFail_nonstatic_3() throws Exception{
		IType parentClas= getClassFromTestFile(getPackageP(), "A");
		int offset= TextRangeUtil.getOffset(parentClas.getCompilationUnit(), 5, 25);
		IType nestedLocal= (IType) parentClas.getCompilationUnit().codeSelect(offset, 0)[0];

		MoveInnerToTopRefactoring ref= ((RefactoringAvailabilityTester.isMoveInnerAvailable(nestedLocal)) ? new MoveInnerToTopRefactoring(nestedLocal, JavaPreferencesSettings.getCodeGenerationSettings(parentClas.getJavaProject())) : null);
		assertNull("refactoring was not supposed to be available", ref);
	}

	// --- Secondary classes
	@Test
	public void test_secondary_0() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A" }, new String[] { "p" }, null, false, false, false, false);
	}

	@Test
	public void test_secondary_1() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A" }, new String[] { "p" }, null, false, false, false, false);
	}

	@Ignore("BUG_304827")
	@Test
	public void test_secondary_2() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A" }, new String[] { "p" }, null, false, false, false, false);
	}

	@Test
	public void test_secondary_3() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A", "S" }, new String[] { "p", "q" }, null, false, false, false, false);
	}

	@Ignore("too many imports, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=304827")
	@Test
	public void test_secondary_4() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A" }, new String[] { "p" }, null, false, false, false, false);
	}

	@Ignore("too many imports, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=304827")
	@Test
	public void test_secondary_5() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A" }, new String[] { "p" }, null, false, false, false, false);
	}

	@Ignore("too many imports, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=304827")
	@Test
	public void test_secondary_6() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A" }, new String[] { "p" }, null, false, false, false, false);
	}

	@Test
	public void test_secondary_7() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A", "S", "T" }, new String[] { "p", "q", "q" }, null, false, false, false, false);
	}

	@Test
	public void test_secondary_8() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A", "S", "T" }, new String[] { "p", "q", "q" }, null, false, false, false, false);
	}

	@Test
	public void test_secondary_9() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A" }, new String[] { "p" }, null, false, false, false, false);
	}

	@Test
	public void test_secondary_10() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A" }, new String[] { "p" }, null, false, false, false, false);
	}

	@Test
	public void test_secondary_11() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A" }, new String[] { "p" }, null, false, false, false, false);
	}

	@Test
	public void test_secondary_12() throws Exception {
		validatePassingTestSecondaryType("A", "Secondary", "p", new String[] { "A" }, new String[] { "p" }, null, false, false, false, false);
	}
}
