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
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug Encapuslate field can fail when two variables in one variable declaration (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=51540).
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [encapsulate field] Encapsulating parenthesized field assignment yields compilation error - https://bugs.eclipse.org/177095
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.COMPARE_WITH_OUTPUT;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import org.junit.ClassRule;
import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;

public class SefTests extends AbstractJunit4SelectionTestCase {

	@ClassRule
	public static SefTestSetup fgTestSetup= new SefTestSetup();

	@Override
	public void setUp() throws Exception {
		super.setUp();
		fIsPreDeltaTest= true;
	}

	@Override
	protected String getResourceLocation() {
		return "SefWorkSpace/SefTests/";
	}

	@Override
	protected String adaptName(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1) + ".java";
	}

	protected void performTest(IPackageFragment packageFragment, String id, String outputFolder, String fieldName) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		IField field= getField(unit, fieldName);
		assertNotNull(field);

		initializePreferences();

		SelfEncapsulateFieldRefactoring refactoring= ((Checks.checkAvailability(field).hasFatalError() || !RefactoringAvailabilityTester.isSelfEncapsulateAvailable(field)) ? null : new SelfEncapsulateFieldRefactoring(field));
		performTest(unit, refactoring, COMPARE_WITH_OUTPUT, getProofedContent(outputFolder, id), true);
	}

	protected void performInvalidTest(IPackageFragment packageFragment, String id, String fieldName) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		IField field= getField(unit, fieldName);
		assertNotNull(field);

		initializePreferences();

		SelfEncapsulateFieldRefactoring refactoring= ((Checks.checkAvailability(field).hasFatalError() || !RefactoringAvailabilityTester.isSelfEncapsulateAvailable(field)) ? null : new SelfEncapsulateFieldRefactoring(field));
		if (refactoring != null) {
			RefactoringStatus status= refactoring.checkAllConditions(new NullProgressMonitor());
			assertTrue(status.hasError());
		}
	}

	private void initializePreferences() {
		Hashtable<String, String> options= new Hashtable<>();
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		options.put(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		options.put(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		options.put(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");
		JavaCore.setOptions(options);
	}

	private static IField getField(ICompilationUnit unit, String fieldName) throws Exception {
		IField result= null;
		for (IType type : unit.getAllTypes()) {
			result= type.getField(fieldName);
			if (result != null && result.exists())
				break;
		}
		return result;
	}

	private void objectTest(String fieldName) throws Exception {
		performTest(fgTestSetup.getObjectPackage(), getName(), "object_out", fieldName);
	}

	private void baseTest(String fieldName) throws Exception {
		performTest(fgTestSetup.getBasePackage(), getName(), "base_out", fieldName);
	}

	private void invalidTest(String fieldName) throws Exception {
		performInvalidTest(fgTestSetup.getInvalidPackage(), getName(), fieldName);
	}

	private void existingTest(String fieldName) throws Exception {
		performTest(fgTestSetup.getExistingMethodPackage(), getName(), "existingmethods_out", fieldName);
	}
	//=====================================================================================
	// Invalid
	//=====================================================================================

	@Test
	public void testPostfixExpression() throws Exception {
		invalidTest("field");
	}

	@Test
	public void testInvalidOverwrite() throws Exception {
		invalidTest("field");
	}

	@Test
	public void testAnnotation() throws Exception {
		invalidTest("field");
	}

	//=====================================================================================
	// Primitiv Data Test
	//=====================================================================================

	@Test
	public void testPrefixInt() throws Exception {
		baseTest("field");
	}

	@Test
	public void testPrefixBoolean() throws Exception {
		baseTest("field");
	}

	@Test
	public void testPostfixInt() throws Exception {
		baseTest("field");
	}

	@Test
	public void testThisExpression() throws Exception {
		baseTest("field");
	}

	@Test
	public void testThisExpressionInner() throws Exception {
		baseTest("field");
	}

	@Test
	public void testFinal() throws Exception {
		baseTest("field");
	}

	@Test
	public void testTwoFragments() throws Exception {
		baseTest("field");
	}

	//=====================================================================================
	// Basic Object Test
	//=====================================================================================

	@Test
	public void testSimpleRead() throws Exception {
		objectTest("field");
	}

	@Test
	public void testSimpleWrite() throws Exception {
		objectTest("field");
	}

	@Test
	public void testSimpleParenthesizedWrite() throws Exception {
		objectTest("field");
	}

	@Test
	public void testSimpleReadWrite() throws Exception {
		objectTest("field");
	}

	@Test
	public void testEnumRead() throws Exception {
		objectTest("field");
	}

	@Test
	public void testEnumReadWrite() throws Exception {
		objectTest("field");
	}

	@Test
	public void testNestedRead() throws Exception {
		objectTest("field");
	}

	@Test
	public void testArrayRead() throws Exception {
		objectTest("field");
	}

    public void testCStyleArrayRead() throws Exception {
      objectTest("field");
    }


	@Test
	public void testSetterInAssignment() throws Exception {
		objectTest("field");
	}

	@Test
	public void testSetterInExpression() throws Exception {
		objectTest("field");
	}

	@Test
	public void testSetterInInitialization() throws Exception {
		objectTest("field");
	}

	@Test
	public void testSetterAsReceiver() throws Exception {
		objectTest("field");
	}

	@Test
	public void testCompoundParenthesizedWrite() throws Exception {
		objectTest("field");
	}

	@Test
	public void testCompoundWrite() throws Exception {
		objectTest("field");
	}

	@Test
	public void testCompoundWrite2() throws Exception {
		objectTest("field");
	}

	@Test
	public void testCompoundWrite3() throws Exception {
		objectTest("field");
	}

	@Test
	public void testCompoundWrite4() throws Exception {
		objectTest("field");
	}

	@Test
	public void testFinalField() throws Exception {
		objectTest("field");
	}

	@Test
	public void testGenericRead() throws Exception {
		objectTest("field");
	}

	@Test
	public void testGenericRead2() throws Exception {
		objectTest("field");
	}

	@Test
	public void testGenericReadWrite() throws Exception {
		objectTest("field");
	}

	@Test
	public void testArrayAnnotations() throws Exception {
		objectTest("field");
	}

	//=====================================================================================
	// static import tests
	//=====================================================================================

	private void performStaticImportTest(String referenceName) throws Exception, JavaModelException {
		ICompilationUnit provider= createCU(fgTestSetup.getStaticPackage(), getName());
		ICompilationUnit reference= createCU(fgTestSetup.getStaticRefPackage(), referenceName);

		IField field= getField(provider, "x");
		assertNotNull(field);

		initializePreferences();

		SelfEncapsulateFieldRefactoring refactoring= ((Checks.checkAvailability(field).hasFatalError() || !RefactoringAvailabilityTester.isSelfEncapsulateAvailable(field)) ? null : new SelfEncapsulateFieldRefactoring(field));
		performTest(provider, refactoring, COMPARE_WITH_OUTPUT, getProofedContent("static_out", getName()), false);
		String refContentOut= getProofedContent("static_ref_out", referenceName);
		refContentOut= refContentOut.replaceAll("import static static_out", "import static static_in");
		compareSource(reference.getSource(), refContentOut);
	}

	@Test
	public void testStaticImportRead() throws Exception {
		performStaticImportTest("StaticImportReadReference");
	}

	@Test
	public void testStaticImportWrite() throws Exception {
		performStaticImportTest("StaticImportWriteReference");
	}

	@Test
	public void testStaticImportReadWrite() throws Exception {
		performStaticImportTest("StaticImportReadWriteReference");
	}

	@Test
	public void testStaticImportNone() throws Exception {
		performStaticImportTest("StaticImportNoReference");
	}

	//=====================================================================================
	// existing getter/setter
	//=====================================================================================

	@Test
	public void testThisExpressionInnerWithSetter() throws Exception {
		existingTest("field");
	}

	@Test
	public void testThisExpressionWithGetterSetter() throws Exception {
		existingTest("field");
	}

	@Test
	public void testTwoFragmentsWithSetter() throws Exception {
		existingTest("field");
	}
}
