/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug Encapuslate field can fail when two variables in one variable declaration (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=51540).
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

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

public class SefTests extends AbstractSelectionTestCase {

	private static SefTestSetup fgTestSetup;

	public SefTests(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new SefTestSetup(new TestSuite(SefTests.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test test) {
		fgTestSetup= new SefTestSetup(test);
		return fgTestSetup;
	}

	protected void setUp() throws Exception {
		super.setUp();
		fIsPreDeltaTest= true;
	}

	protected String getResourceLocation() {
		return "SefWorkSpace/SefTests/";
	}

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
		Hashtable options= new Hashtable();
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		options.put(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		options.put(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		options.put(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");
		JavaCore.setOptions(options);
	}

	private static IField getField(ICompilationUnit unit, String fieldName) throws Exception {
		IField result= null;
		IType[] types= unit.getAllTypes();
		for (int i= 0; i < types.length; i++) {
			IType type= types[i];
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

	public void testPostfixExpression() throws Exception {
		invalidTest("field");
	}

	public void testInvalidOverwrite() throws Exception {
		invalidTest("field");
	}

	public void testAnnotation() throws Exception {
		invalidTest("field");
	}

	//=====================================================================================
	// Primitiv Data Test
	//=====================================================================================

	public void testPrefixInt() throws Exception {
		baseTest("field");
	}

	public void testPrefixBoolean() throws Exception {
		baseTest("field");
	}

	public void testPostfixInt() throws Exception {
		baseTest("field");
	}

	public void testThisExpression() throws Exception {
		baseTest("field");
	}

	public void testThisExpressionInner() throws Exception {
		baseTest("field");
	}

	public void testFinal() throws Exception {
		baseTest("field");
	}

	public void testTwoFragments() throws Exception {
		baseTest("field");
	}

	//=====================================================================================
	// Basic Object Test
	//=====================================================================================

	public void testSimpleRead() throws Exception {
		objectTest("field");
	}

	public void testSimpleWrite() throws Exception {
		objectTest("field");
	}

	public void testSimpleReadWrite() throws Exception {
		objectTest("field");
	}

	public void testEnumRead() throws Exception {
		objectTest("field");
	}

	public void testEnumReadWrite() throws Exception {
		objectTest("field");
	}

	public void testNestedRead() throws Exception {
		objectTest("field");
	}

	public void testArrayRead() throws Exception {
		objectTest("field");
	}

    public void testCStyleArrayRead() throws Exception {
      objectTest("field");
    }


	public void testSetterInAssignment() throws Exception {
		objectTest("field");
	}

	public void testSetterInExpression() throws Exception {
		objectTest("field");
	}

	public void testSetterInInitialization() throws Exception {
		objectTest("field");
	}

	public void testSetterAsReceiver() throws Exception {
		objectTest("field");
	}

	public void testCompoundWrite() throws Exception {
		objectTest("field");
	}

	public void testCompoundWrite2() throws Exception {
		objectTest("field");
	}

	public void testCompoundWrite3() throws Exception {
		objectTest("field");
	}

	public void testFinalField() throws Exception {
		objectTest("field");
	}

	public void testGenericRead() throws Exception {
		objectTest("field");
	}

	public void testGenericRead2() throws Exception {
		objectTest("field");
	}

	public void testGenericReadWrite() throws Exception {
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

	public void testStaticImportRead() throws Exception {
		performStaticImportTest("StaticImportReadReference");
	}

	public void testStaticImportWrite() throws Exception {
		performStaticImportTest("StaticImportWriteReference");
	}

	public void testStaticImportReadWrite() throws Exception {
		performStaticImportTest("StaticImportReadWriteReference");
	}

	public void testStaticImportNone() throws Exception {
		performStaticImportTest("StaticImportNoReference");
	}

	//=====================================================================================
	// existing getter/setter
	//=====================================================================================

	public void testThisExpressionInnerWithSetter() throws Exception {
		existingTest("field");
	}

	public void testThisExpressionWithGetterSetter() throws Exception {
		existingTest("field");
	}

	public void testTwoFragmentsWithSetter() throws Exception {
		existingTest("field");
	}
}
