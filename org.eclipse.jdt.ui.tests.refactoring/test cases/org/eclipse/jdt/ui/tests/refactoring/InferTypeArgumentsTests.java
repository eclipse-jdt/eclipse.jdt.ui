/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.refactoring;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipInputStream;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.infra.ZipTools;

public class InferTypeArgumentsTests extends RefactoringTest {

	private static final boolean DETERMINE_ELEMENT_TYPE_FROM_CAST= false;
	private static final boolean BUG_86967_core_restore_binding= true;
	private static final boolean BUG_86990_core_no_main_type= true;
	private static final boolean BUG_87050_core_resolve_method_type_param= false;

	private static final Class clazz= InferTypeArgumentsTests.class;
	private static final String REFACTORING_PATH= "InferTypeArguments/";

	private boolean fAssumeCloneReturnsSameType= true;
	private boolean fLeaveUnconstrainedRaw= true;

	public static Test suite() {
		return setUpTest(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java16Setup(someTest);
	}

	public InferTypeArgumentsTests(String name) {
		super(name);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void performCu(int expectedInitialStatus, int expectedFinalStatus) throws Exception {
		IPackageFragment packageP= getPackageP();
		String cuName="A";
		ICompilationUnit cu= packageP.getCompilationUnit(cuName + ".java");
		if (!cu.exists())
			cu= createCUfromTestFile(packageP, cuName);
		IJavaElement[] elements= { cu };
		boolean performed= perform(elements, expectedInitialStatus, expectedFinalStatus);
		if (! performed)
			return;

		String expected= getFileContents(getOutputTestFileName("A"));
		String actual= cu.getSource();
		assertEqualLines(expected, actual);
	}

	/**
	 * @param elements
	 * @param expectedInitialStatus
	 * @param expectedFinalStatus
	 * @return <code>true</code> iff performed
	 * @throws CoreException
	 */
	private boolean perform(IJavaElement[] elements, int expectedInitialStatus, int expectedFinalStatus) throws CoreException {
		InferTypeArgumentsRefactoring refactoring= ((RefactoringAvailabilityTester.isInferTypeArgumentsAvailable(elements)) ? new InferTypeArgumentsRefactoring(elements) : null);

		NullProgressMonitor pm= new NullProgressMonitor();
		RefactoringStatus initialStatus= refactoring.checkInitialConditions(pm);
		assertEquals("wrong initial condition status: " + initialStatus, expectedInitialStatus, initialStatus.getSeverity());
		if (! initialStatus.isOK())
			return false;

		refactoring.setAssumeCloneReturnsSameType(fAssumeCloneReturnsSameType);
		refactoring.setLeaveUnconstrainedRaw(fLeaveUnconstrainedRaw);

		PerformRefactoringOperation op= new PerformRefactoringOperation(
				refactoring, CheckConditionsOperation.FINAL_CONDITIONS);
		JavaCore.run(op, new NullProgressMonitor());
		RefactoringStatus finalStatus= op.getConditionStatus();
		assertEquals("wrong final condition status: " + finalStatus, expectedFinalStatus, finalStatus.getSeverity());
		if (finalStatus.getSeverity() == RefactoringStatus.FATAL)
			return false;

		assertTrue("Validation check failed: " + op.getValidationStatus(), !op.getValidationStatus().hasFatalError());
		assertNotNull("No Undo", op.getUndoChange());
		return true;
	}

	private void performCuOK() throws Exception {
		performCu(RefactoringStatus.OK, RefactoringStatus.OK);
	}

	public void compareWithZipFile(IPackageFragmentRoot src, String zipFileName) throws Exception {
		String fullName= TEST_PATH_PREFIX + getRefactoringPath() + zipFileName;
		ZipInputStream zis= new ZipInputStream(getFileInputStream(fullName));
		ZipTools.compareWithZipped(src, zis, JavaProjectHelper.JUNIT_SRC_ENCODING);
	}

	public void testCuQualifiedName() throws Exception {
		performCuOK();
	}

	public void testCuAnonymous01() throws Exception {
		performCuOK();
	}

	public void testCuTypeParams9() throws Exception {
		performCuOK();
	}

	public void testCuExistingParameterized01() throws Exception {
		performCuOK();
	}

	public void testCuGetClass() throws Exception {
		performCuOK();
	}

	public void testCuGetClass2() throws Exception {
		// Test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=211037
		// In 1.6, Object#getClass() declares return type Class<?>, but in 1.5, it's Class<? extends Object>.
		performCuOK();

		// Test the same with 1.5:
		IJavaProject project= RefactoringTestSetup.getProject();

		ArrayList classpath= new ArrayList(Arrays.asList(project.getRawClasspath()));
		IClasspathEntry jreEntry= RefactoringTestSetup.getJRELibrary().getRawClasspathEntry();
		classpath.remove(jreEntry);
		IClasspathEntry[] noRTJarCPEs= (IClasspathEntry[])classpath.toArray(new IClasspathEntry[classpath.size()]);

		project.setRawClasspath(noRTJarCPEs, new NullProgressMonitor());
		JavaProjectHelper.addRTJar15(project);

		try {
			performCuOK();
		} finally {
			project.setRawClasspath(noRTJarCPEs, new NullProgressMonitor());
			JavaProjectHelper.addRTJar16(project);
		}
	}

	public void testCuGetSuperclass() throws Exception {
		performCuOK();
	}

	public void testCuTypeLiteral() throws Exception {
		performCuOK();
	}

	public void testCuMethodTypeParam() throws Exception {
		performCuOK();
	}

	public void testCuGetTakeClassStayRaw() throws Exception {
		performCuOK();
	}

	public void testCuGetClassNewInstance() throws Exception {
		performCuOK();
	}

	public void testCuSynchronizedList() throws Exception {
		fLeaveUnconstrainedRaw= false;
		performCuOK();
	}

	public void testCuAddAll() throws Exception {
		performCuOK();
	}

	public void testCuNestedCells1() throws Exception {
		createCUfromTestFile(getPackageP(), "Cell");
		fLeaveUnconstrainedRaw= false;
		performCuOK();
	}

	public void testCuNestedVectors0() throws Exception {
		fLeaveUnconstrainedRaw= false;
		performCuOK();
	}

	public void testCuNestedVectors1() throws Exception {
		performCuOK();
	}

	public void testCuInferTypeVariable01() throws Exception {
		if (BUG_86990_core_no_main_type || BUG_87050_core_resolve_method_type_param) {
			printTestDisabledMessage("BUG_86990_core_no_main_type || BUG_87050_core_resolve_method_type_param");
			return;
		}
		performCuOK();
	}

	public void testCuBoxing01() throws Exception {
		performCuOK();
	}

	public void testCuBoxing02() throws Exception {
		performCuOK();
	}

	public void testCuConstructor01() throws Exception {
		performCuOK();
	}

	public void testJUnit() throws Exception {
		fAssumeCloneReturnsSameType= false;
		fLeaveUnconstrainedRaw= true;
		IJavaProject javaProject= JavaProjectHelper.createJavaProject("InferTypeArguments", "bin");
		try {
			IPackageFragmentRoot jdk= JavaProjectHelper.addRTJar(javaProject);
			Assert.assertNotNull(jdk);

			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			Assert.assertTrue(junitSrcArchive != null && junitSrcArchive.exists());

			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainerWithImport(javaProject, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);

			boolean performed= perform(new IJavaElement[] { javaProject }, RefactoringStatus.OK, RefactoringStatus.OK);
			assertTrue(performed);

			compareWithZipFile(src, "junit381-noUI-generified-src.zip");
		} finally {
			if (javaProject != null && javaProject.exists())
				JavaProjectHelper.delete(javaProject);
		}

	}

	public void testJUnitWithCloneNotRaw() throws Exception {
		fAssumeCloneReturnsSameType= true;
		fLeaveUnconstrainedRaw= false;

		IJavaProject javaProject= JavaProjectHelper.createJavaProject("InferTypeArguments", "bin");
		try {
			IPackageFragmentRoot jdk= JavaProjectHelper.addRTJar(javaProject);
			Assert.assertNotNull(jdk);

			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			Assert.assertTrue(junitSrcArchive != null && junitSrcArchive.exists());

			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainerWithImport(javaProject, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);

			boolean performed= perform(new IJavaElement[] { javaProject }, RefactoringStatus.OK, RefactoringStatus.OK);
			assertTrue(performed);

			compareWithZipFile(src, "junit381-noUI-clone-not-raw-src.zip");
		} finally {
			if (javaProject != null && javaProject.exists())
				JavaProjectHelper.delete(javaProject);
		}

	}

	public void testCuTwoVectorElements() throws Exception {
		performCuOK();
	}

	public void testCuHalfPair() throws Exception {
		performCuOK();
	}

	public void testCuMethodAndTypeGeneric01() throws Exception {
		performCuOK();
	}

	public void testCuMethodAndTypeGeneric02() throws Exception {
		performCuOK();
	}

	public void testPairDance() throws Exception {
		createCUfromTestFile(getPackageP(), "Pair");
		createCUfromTestFile(getPackageP(), "InvertedPair");
		performCuOK();
		// deleted in tearDown
	}

	public void testCuAddString() throws Exception {
		performCuOK();
	}

	public void testCuAddString2() throws Exception {
		performCuOK();
	}

	public void testCuIntermediateLocal() throws Exception {
		performCuOK();
	}

	public void testCuSuperAndSub() throws Exception {
		performCuOK();
	}

	public void testCuCommonSuper() throws Exception {
		performCuOK();
	}

	public void testCuAddGetString() throws Exception {
		performCuOK();
	}

	public void testCuAddIntegerGetNumber() throws Exception {
		performCuOK();
	}

	public void testCuAddGetIterator() throws Exception {
		performCuOK();
	}

	public void testCuContains() throws Exception {
		performCuOK();
	}

	public void testCuMethodParam() throws Exception {
		performCuOK();
	}

	public void testCuMethodReturns() throws Exception {
		performCuOK();
	}

	public void testCuCollectionsMin() throws Exception {
		performCuOK();
	}

	public void testCuAddStringInteger() throws Exception {
		printTestDisabledMessage("currently, we don't follow flow through variables of type Object");
//		performCuOK(); //TODO
	}

	public void testCuAddStringIntegerA() throws Exception {
		performCuOK();
	}

	public void testCuInferFromCast() throws Exception {
		printTestDisabledMessage("not implemented yet");
//		performCuOK(); //TODO
	}

	public void testCuRippleMethods() throws Exception {
		performCuOK();
	}

	public void testCuRippleMethods2() throws Exception {
		performCuOK();
	}

	public void testCuCannotStringDouble() throws Exception {
		printTestDisabledMessage("not implemented yet");
//		performCuOK();
	}

	public void testCuRippleMethods3() throws Exception {
		printTestDisabledMessage("not implemented yet");
//		performCuOK();
	}

	public void testCuVarargs01() throws Exception {
		performCuOK();
	}

	public void testCuArrays01() throws Exception {
		performCuOK();
	}

	public void testCuArrays02() throws Exception {
		performCuOK();
	}

	public void testCuArrays03() throws Exception {
		performCuOK();
	}

	public void testCuArrays04() throws Exception {
		performCuOK();
	}

	public void testCuArrays05() throws Exception {
		if (! DETERMINE_ELEMENT_TYPE_FROM_CAST) {
			printTestDisabledMessage("DETERMINE_ELEMENT_TYPE_FROM_CAST");
			return;
		}
		performCuOK();
	}

	public void testCuArrays06() throws Exception {
		performCuOK();
	}

	public void testCuArrays07() throws Exception {
		performCuOK();
	}

	public void testCuToArray01() throws Exception {
		performCuOK();
	}

	public void testCuToArray02() throws Exception {
		performCuOK();
	}

	public void testCuMapEntry01() throws Exception {
		if (BUG_86967_core_restore_binding) {
			printTestDisabledMessage("BUG_86967_core_restore_binding");
			return;
		}
		performCuOK();
	}

	public void testCuFieldAccess01() throws Exception {
		performCuOK();
	}

	public void testCuFieldAccess02() throws Exception {
		performCuOK();
	}

	public void testCuMemberOfRaw() throws Exception {
		// regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=110594
		performCuOK();
	}

	public void testCuParameterizedTypes1() throws Exception {
		// regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=176742
		performCuOK();
	}

}
