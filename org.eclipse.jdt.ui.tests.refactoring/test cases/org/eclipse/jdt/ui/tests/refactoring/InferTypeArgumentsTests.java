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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipInputStream;

import org.junit.Ignore;
import org.junit.Test;

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
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d6Setup;

public class InferTypeArgumentsTests extends GenericRefactoringTest {

	private static final String REFACTORING_PATH= "InferTypeArguments/";

	private boolean fAssumeCloneReturnsSameType= true;
	private boolean fLeaveUnconstrainedRaw= true;

	public InferTypeArgumentsTests() {
		rts= new Java1d6Setup();
	}

	@Override
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

		assertFalse("Validation check failed: " + op.getValidationStatus(), op.getValidationStatus().hasFatalError());
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

	@Test
	public void testCuQualifiedName() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuAnonymous01() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuTypeParams9() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuExistingParameterized01() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuGetClass() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuGetClass2() throws Exception {
		// Test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=211037
		// In 1.6, Object#getClass() declares return type Class<?>, but in 1.5, it's Class<? extends Object>.
		performCuOK();

		// Test the same with 1.5:
		IJavaProject project= rts.getProject();

		ArrayList<IClasspathEntry> classpath= new ArrayList<>(Arrays.asList(project.getRawClasspath()));
		IClasspathEntry jreEntry= rts.getJRELibrary().getRawClasspathEntry();
		classpath.remove(jreEntry);
		IClasspathEntry[] noRTJarCPEs= classpath.toArray(new IClasspathEntry[classpath.size()]);

		project.setRawClasspath(noRTJarCPEs, new NullProgressMonitor());
		JavaProjectHelper.addRTJar15(project);

		try {
			performCuOK();
		} finally {
			project.setRawClasspath(noRTJarCPEs, new NullProgressMonitor());
			JavaProjectHelper.addRTJar16(project);
		}
	}

	@Test
	public void testCuGetSuperclass() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuTypeLiteral() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuMethodTypeParam() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuGetTakeClassStayRaw() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuGetClassNewInstance() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuSynchronizedList() throws Exception {
		fLeaveUnconstrainedRaw= false;
		performCuOK();
	}

	@Test
	public void testCuAddAll() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuNestedCells1() throws Exception {
		createCUfromTestFile(getPackageP(), "Cell");
		fLeaveUnconstrainedRaw= false;
		performCuOK();
	}

	@Test
	public void testCuNestedVectors0() throws Exception {
		fLeaveUnconstrainedRaw= false;
		performCuOK();
	}

	@Test
	public void testCuNestedVectors1() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuInferTypeVariable01() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuBoxing01() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuBoxing02() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuConstructor01() throws Exception {
		performCuOK();
	}

	@Test
	public void testJUnit() throws Exception {
		fAssumeCloneReturnsSameType= false;
		fLeaveUnconstrainedRaw= true;
		IJavaProject javaProject= JavaProjectHelper.createJavaProject("InferTypeArguments", "bin");
		try {
			IPackageFragmentRoot jdk= JavaProjectHelper.addRTJar(javaProject);
			assertNotNull(jdk);

			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			assertNotNull(junitSrcArchive);
            assertTrue(junitSrcArchive.exists());

			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainerWithImport(javaProject, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);

			boolean performed= perform(new IJavaElement[] { javaProject }, RefactoringStatus.OK, RefactoringStatus.OK);
			assertTrue(performed);

			compareWithZipFile(src, "junit381-noUI-generified-src.zip");
		} finally {
			if (javaProject != null && javaProject.exists())
				JavaProjectHelper.delete(javaProject);
		}

	}

	@Test
	public void testJUnitWithCloneNotRaw() throws Exception {
		fAssumeCloneReturnsSameType= true;
		fLeaveUnconstrainedRaw= false;

		IJavaProject javaProject= JavaProjectHelper.createJavaProject("InferTypeArguments", "bin");
		try {
			IPackageFragmentRoot jdk= JavaProjectHelper.addRTJar(javaProject);
			assertNotNull(jdk);

			File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
			assertNotNull(junitSrcArchive);
            assertTrue(junitSrcArchive.exists());

			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainerWithImport(javaProject, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);

			boolean performed= perform(new IJavaElement[] { javaProject }, RefactoringStatus.OK, RefactoringStatus.OK);
			assertTrue(performed);

			compareWithZipFile(src, "junit381-noUI-clone-not-raw-src.zip");
		} finally {
			if (javaProject != null && javaProject.exists())
				JavaProjectHelper.delete(javaProject);
		}

	}

	@Test
	public void testCuTwoVectorElements() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuHalfPair() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuMethodAndTypeGeneric01() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuMethodAndTypeGeneric02() throws Exception {
		performCuOK();
	}

	@Test
	public void testPairDance() throws Exception {
		createCUfromTestFile(getPackageP(), "Pair");
		createCUfromTestFile(getPackageP(), "InvertedPair");
		performCuOK();
		// deleted in tearDown
	}

	@Test
	public void testCuAddString() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuAddString2() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuIntermediateLocal() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuSuperAndSub() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuCommonSuper() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuAddGetString() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuAddIntegerGetNumber() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuAddGetIterator() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuContains() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuMethodParam() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuMethodReturns() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuCollectionsMin() throws Exception {
		performCuOK();
	}

	@Ignore("currently, we don't follow flow through variables of type Object")
	@Test
	public void testCuAddStringInteger() throws Exception {
		performCuOK(); //TODO
	}

	@Test
	public void testCuAddStringIntegerA() throws Exception {
		performCuOK();
	}

	@Ignore("not implemented yet")
	@Test
	public void testCuInferFromCast() throws Exception {
		performCuOK(); //TODO
	}

	@Test
	public void testCuRippleMethods() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuRippleMethods2() throws Exception {
		performCuOK();
	}

	@Ignore("not implemented yet")
	@Test
	public void testCuCannotStringDouble() throws Exception {
		performCuOK();
	}

	@Ignore("not implemented yet")
	@Test
	public void testCuRippleMethods3() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuVarargs01() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuArrays01() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuArrays02() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuArrays03() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuArrays04() throws Exception {
		performCuOK();
	}

	@Ignore("DETERMINE_ELEMENT_TYPE_FROM_CAST")
	@Test
	public void testCuArrays05() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuArrays06() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuArrays07() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuToArray01() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuToArray02() throws Exception {
		performCuOK();
	}

	@Ignore("BUG_map_entrySet_iterator")
	@Test
	public void testCuMapEntry01() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuFieldAccess01() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuFieldAccess02() throws Exception {
		performCuOK();
	}

	@Test
	public void testCuMemberOfRaw() throws Exception {
		// regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=110594
		performCuOK();
	}

	@Test
	public void testCuParameterizedTypes1() throws Exception {
		// regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=176742
		performCuOK();
	}

	@Test
	public void testCuParameterizedTypes2() throws Exception {
		// regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=216627
		performCuOK();
	}

}
