/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d8Setup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class PullUpTests1d8 extends PullUpTests {
	public PullUpTests1d8() {
		super(new Java1d8Setup());
	}

	@Test
	public void test18_1() throws Exception {
		String[] methodNames= new String[] { "getArea" };
		String[][] signatures= new String[][] { new String[] { "QInteger;" } };
		JavaProjectHelper.addLibrary((IJavaProject) getPackageP().getAncestor(IJavaElement.JAVA_PROJECT), new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");


		IType type= getType(cuB, "B");
		IMethod[] methods= getMethods(type, methodNames, signatures);

		PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
		Refactoring ref= processor.getRefactoring();

		assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
		setSuperclassAsTargetClass(processor);

		RefactoringStatus result= performRefactoring(ref);
		assertTrue("precondition was supposed to pass", result == null || !result.hasError());

		assertEqualLines("A", getFileContents(getOutputTestFileName("A")), cuA.getSource());
		assertEqualLines("B", getFileContents(getOutputTestFileName("B")), cuB.getSource());
	}

	// bug 394551 comment 2
	@Test
	public void test18_2() throws Exception {
		String[] selectedMethodNames= { "m" };
		String[][] selectedMethodSignatures= { new String[0] };
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				new String[0], namesOfMethodsToPullUp,
				signaturesOfMethodsToPullUp,
				namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
				signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 1);
	}

	// bug 394551 expect @java.lang.Override
	@Test
	public void test18_3() throws Exception {
		String[] selectedMethodNames= { "m" };
		String[][] selectedMethodSignatures= { new String[0] };
		String[] selectedFieldNames= {};
		String[] namesOfMethodsToPullUp= {};
		String[][] signaturesOfMethodsToPullUp= {};
		String[] namesOfFieldsToPullUp= {};
		String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
		String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

		declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
				selectedFieldNames,
				new String[0], namesOfMethodsToPullUp,
				signaturesOfMethodsToPullUp,
				namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
				signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 1);
	}

	// bug 394551: no override annotation expected
	@Test
	public void test18_4() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		boolean previousValue= store.getBoolean(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION);
		try {
			store.setValue(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, false);
			String[] selectedMethodNames= { "m" };
			String[][] selectedMethodSignatures= { new String[0] };
			String[] selectedFieldNames= {};
			String[] namesOfMethodsToPullUp= {};
			String[][] signaturesOfMethodsToPullUp= {};
			String[] namesOfFieldsToPullUp= {};
			String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
			String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

			declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
					selectedFieldNames,
					new String[0], namesOfMethodsToPullUp,
					signaturesOfMethodsToPullUp,
					namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
					signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 1);
		} finally {
			store.setValue(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, previousValue);
		}
	}

	// bug 394551: no override annotation expected
	@Test
	public void test18_5() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		boolean previousValue= store.getBoolean(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION);
		Hashtable<String, String> options= JavaCore.getOptions();
		Hashtable<String, String> changedOptions= new Hashtable<>(options);
		changedOptions.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.WARNING);
		changedOptions.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION, JavaCore.DISABLED);
		try {
			JavaCore.setOptions(changedOptions);
			store.setValue(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, false);
			String[] selectedMethodNames= { "m" };
			String[][] selectedMethodSignatures= { new String[0] };
			String[] selectedFieldNames= {};
			String[] namesOfMethodsToPullUp= {};
			String[][] signaturesOfMethodsToPullUp= {};
			String[] namesOfFieldsToPullUp= {};
			String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
			String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

			declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
					selectedFieldNames,
					new String[0], namesOfMethodsToPullUp,
					signaturesOfMethodsToPullUp,
					namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
					signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 1);
		} finally {
			store.setValue(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, previousValue);
			JavaCore.setOptions(options);
		}
	}

	// bug 394551: override annotation expected
	@Test
	public void test18_6() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		boolean previousValue= store.getBoolean(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION);
		Hashtable<String, String> options= JavaCore.getOptions();
		Hashtable<String, String> changedOptions= new Hashtable<>(options);
		changedOptions.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.WARNING);
		try {
			JavaCore.setOptions(changedOptions);
			store.setValue(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, false);
			String[] selectedMethodNames= { "m" };
			String[][] selectedMethodSignatures= { new String[0] };
			String[] selectedFieldNames= {};
			String[] namesOfMethodsToPullUp= {};
			String[][] signaturesOfMethodsToPullUp= {};
			String[] namesOfFieldsToPullUp= {};
			String[] namesOfMethodsToDeclareAbstract= selectedMethodNames;
			String[][] signaturesOfMethodsToDeclareAbstract= selectedMethodSignatures;

			declareAbstractHelper(selectedMethodNames, selectedMethodSignatures,
					selectedFieldNames,
					new String[0], namesOfMethodsToPullUp,
					signaturesOfMethodsToPullUp,
					namesOfFieldsToPullUp, namesOfMethodsToDeclareAbstract,
					signaturesOfMethodsToDeclareAbstract, new String[0], true, true, 1);
		} finally {
			store.setValue(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, previousValue);
			JavaCore.setOptions(options);
		}
	}

	// bug 497368
	@Test
	public void test54() throws Exception {
		ICompilationUnit cuFoo= createCUfromTestFile(getPackageP(), "Foo");
		ICompilationUnit cuIFoo= createCUfromTestFile(getPackageP(), "IFoo");
		ICompilationUnit cuFooImpl= createCUfromTestFile(getPackageP(), "FooImpl");

		String[] methodNames= new String[] { "log" };
		String[][] signatures= new String[][] { new String[] { "QField;", "QString;" } };

		IType type= getType(cuFoo, "Foo");
		IMethod[] methods= getMethods(type, methodNames, signatures);

		PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
		Refactoring ref= processor.getRefactoring();

		assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
		setSuperclassAsTargetClass(processor);

		RefactoringStatus result= performRefactoring(ref);
		assertTrue("precondition was supposed to pass", result == null || !result.hasError());

		assertEqualLines("IFoo", getFileContents(getOutputTestFileName("IFoo")), cuIFoo.getSource());
		assertEqualLines("FooImpl", getFileContents(getOutputTestFileName("FooImpl")), cuFooImpl.getSource());
		assertEqualLines("Foo", getFileContents(getOutputTestFileName("Foo")), cuFoo.getSource());
	}

	// bug 552377
	@Test
	public void testGenerics16() throws Exception {
		ICompilationUnit cuIGeneric= createCUfromTestFile(getPackageP(), "IGeneric");
		ICompilationUnit cuMainImpl= createCUfromTestFile(getPackageP(), "MainImpl");
		ICompilationUnit cuOtherImpl= createCUfromTestFile(getPackageP(), "OtherImpl");

		String[] methodNames= { "genericMethod" };
		String[][] signatures= { { "QString;" } };

		IType typeIGeneric= getType(cuIGeneric, "IGeneric");
		IType typeMainImpl= getType(cuMainImpl, "MainImpl");
		IMethod[] methods= getMethods(typeMainImpl, methodNames, signatures);

		PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
		Refactoring ref= processor.getRefactoring();

		assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
		processor.setDestinationType(typeIGeneric);

		processor.setDeletedMethods(getMethods(processor.getMatchingElements(new NullProgressMonitor(), false)));

		RefactoringStatus result= performRefactoring(ref);

		if (result != null && result.hasError()) {
			fail("No errors should occur: " + result.getMessageMatchingSeverity(RefactoringStatus.ERROR));
		}

		assertEqualLines("IGeneric", getFileContents(getOutputTestFileName("IGeneric")), cuIGeneric.getSource());
		assertEqualLines("MainImpl", getFileContents(getOutputTestFileName("MainImpl")), cuMainImpl.getSource());
		assertEqualLines("OtherImpl", getFileContents(getOutputTestFileName("OtherImpl")), cuOtherImpl.getSource());
	}
}
