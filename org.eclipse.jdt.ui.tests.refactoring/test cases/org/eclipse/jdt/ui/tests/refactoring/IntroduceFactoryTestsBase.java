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
 *     Samrat Dhillon <samrat.dhillon@gmail.com> - [introduce factory] Introduce Factory on an abstract class adds a statement to create an instance of that class - https://bugs.eclipse.org/bugs/show_bug.cgi?id=395016
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;

import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceFactoryRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d6Setup;

/**
 * @author rfuhrer@watson.ibm.com
 */
public class IntroduceFactoryTestsBase extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "IntroduceFactory/";

	public IntroduceFactoryTestsBase() {
		rts= new Java1d6Setup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	/**
	 * Produces a test file name based on the name of this JUnit testcase.
	 * For input files, trims off the trailing part of the test name that
	 * begins with a '_', to get rid of the options part, so that we can
	 * have a single (suite of) input file(s) but several outputs dependent
	 * on the option settings.
	 * @param input true iff the requested file is an input file.
	 * @return the name of the test file, with a trailing "_in.java" if an input
	 * file and a trailing "_XXX.java" if an output file and the test name/options
	 * are "_XXX".
	 */
	private String getSimpleTestFileName(boolean input) {
		String	testName = getName();
		int		usIdx=  testName.indexOf('_');
		int		endIdx= (usIdx >= 0) ? usIdx : testName.length();
		String	fileName = (input ? (testName.substring(4, endIdx) + "_in") : testName.substring(4));

		return fileName + ".java";
	}

	/**
	 * Produces a test file name based on the name of this JUnit testcase,
	 * like getSimpleTestFileName(), but also prepends the appropriate version
	 * of the resource path (depending on the value of <code>positive</code>).
	 * Test files are assumed to be located in the resources directory.
	 * @param positive true iff the requested file is for a positive unit test
	 * @param input true iff the requested file is an input file
	 * @return the test file name
	 */
	private String getTestFileName(boolean positive, boolean input) {
		String path= TEST_PATH_PREFIX + getRefactoringPath();

		path += (positive ? "positive/": "negative/");
		return path + getSimpleTestFileName(input);
	}

	/**
	 * Produces a compilation unit from an input source file whose name
	 * is based on the testcase name.
	 * Test files are assumed to be located in the resources directory.
	 * @param pack
	 * @param positive
	 * @param input
	 * @return the ICompilationUnit created from the specified test file
	 * @throws Exception
	 */
	@SuppressWarnings("javadoc")
	private ICompilationUnit createCUForSimpleTest(IPackageFragment pack,
												  boolean positive, boolean input)
		throws Exception
	{
		String	fileName= getTestFileName(positive, input);
		String	cuName= getSimpleTestFileName(input);

		return createCU(pack, cuName, getFileContents(fileName));
	}

	/**
	 * Produces a test file name based on the name of this JUnit testcase,
	 * like getSimpleTestFileName(), but also prepends the appropriate version
	 * of the resource path (depending on the value of <code>positive</code>).
	 * Test files are assumed to be located in the resources directory.
	 * @param project the project
	 * @param pack the package fragment
	 * @param fileName the file name
	 * @param input true iff the requested file is an input file
	 * @return the test file name
	 */
	private String getBugTestFileName(IJavaProject project, IPackageFragment pack, String fileName, boolean input) {
		String testName= getName();
		String testNumber= testName.substring("test".length());//$NON-NLS-1$
		String path= TEST_PATH_PREFIX + getRefactoringPath() + "Bugzilla/" + testNumber + "/" +
									(project == null ? "" : project.getElementName() + "/") +
									(pack == getPackageP() ? "" : pack.getElementName() + "/");

		return path + fileName + (input ? "" : "_out") + ".java";//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Produces a compilation unit from an input source file whose path
	 * is based on the testcase name, but whose basename is supplied by
	 * the caller.
	 * Test files are assumed to be located in the resources directory.
	 * @param project can be null if only 1 project exists in the test workspace
	 * @param pack
	 * @param baseName
	 * @param input
	 * @return the ICompilationUnit created from the specified test file
	 * @throws Exception
	 */
	@SuppressWarnings("javadoc")
	private ICompilationUnit createCUForBugTestCase(IJavaProject project,
													IPackageFragment pack, String baseName, boolean input)
		throws Exception
	{
		String	fileName= getBugTestFileName(project, pack, baseName, input);
		String	cuName= baseName + (input ? "" : "_out") + ".java";

		return createCU(pack, cuName, getFileContents(fileName));
	}

	static final String SELECTION_START_HERALD= "/*[*/";
	static final String SELECTION_END_HERALD= "/*]*/";

	/**
	 * Finds and returns the selection markers in the given source string,
	 * i.e. the first occurrences of <code>SELECTION_START_HERALD</code> and
	 * <code>SELECTION_END_HERALD</code>. Fails an assertion if either of these
	 * markers is not present in the source string.
	 * @param source
	 * @return an ISourceRange representing the marked selection
	 * @throws Exception
	 */
	@SuppressWarnings("javadoc")
	private ISourceRange findSelectionInSource(String source) throws Exception {
		int		begin= source.indexOf(SELECTION_START_HERALD) + SELECTION_START_HERALD.length();
		int		end= source.indexOf(SELECTION_END_HERALD);

		if (begin < SELECTION_START_HERALD.length())
			fail("No selection start comment in input source file!");
		if (end < 0)
			fail("No selection end comment in input source file!");

		return new SourceRange(begin, end-begin);
	}

	private void doSingleUnitTest(boolean protectConstructor, ICompilationUnit cu, String outputFileName) throws Exception, JavaModelException, IOException {
		ISourceRange		selection= findSelectionInSource(cu.getSource());
		IntroduceFactoryRefactoring	ref= new IntroduceFactoryRefactoring(cu, selection.getOffset(), selection.getLength());

		ref.setProtectConstructor(protectConstructor);

		RefactoringStatus	activationResult= ref.checkInitialConditions(new NullProgressMonitor());

		assertTrue("activation was supposed to be successful", activationResult.isOK());

		RefactoringStatus	checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());

		if (!checkInputResult.isOK()) {
			performChange(ref, false);

			String newSource = cu.getSource();

			System.err.println("!!!Precondition failed for " + getName() + "!!!");
			System.err.println("Compile-time error: " + checkInputResult.toString());
			System.err.println("Offending source:");
			System.err.print(newSource);
			fail("precondition was supposed to pass but was " + checkInputResult.toString());
		}

		performChange(ref, false);

		String newSource = cu.getSource();

		assertEqualLines(getName() + ": ", getFileContents(outputFileName), newSource);
	}

	private void doSingleUnitTestWithWarning(boolean protectConstructor, ICompilationUnit cu, String outputFileName) throws Exception, JavaModelException, IOException {
		ISourceRange selection= findSelectionInSource(cu.getSource());
		IntroduceFactoryRefactoring ref= new IntroduceFactoryRefactoring(cu, selection.getOffset(), selection.getLength());

		ref.setProtectConstructor(protectConstructor);

		RefactoringStatus activationResult= ref.checkInitialConditions(new NullProgressMonitor());

		assertTrue("activation was supposed to be successful", activationResult.isOK());

		RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());

		assertEquals(RefactoringStatus.WARNING, checkInputResult.getSeverity());
		performChange(ref, false);

		String newSource= cu.getSource();

		assertEqualLines(getName() + ": ", getFileContents(outputFileName), newSource);
	}

	/**
	 * Tests the IntroduceFactoryRefactoring refactoring on a single input source file
	 * whose name is the test name (minus the "test" prefix and any trailing
	 * options indicator such as "_FFF"), and compares the transformed code
	 * to a source file whose name is the test name (minus the "test" prefix).
	 * Test files are assumed to be located in the resources directory.
	 * @param protectConstructor true iff IntroduceFactoryRefactoring should make the constructor private
	 * @throws Exception
	 */
	@SuppressWarnings("javadoc")
	void singleUnitHelper(boolean protectConstructor)
		throws Exception
	{
		ICompilationUnit	cu= createCUForSimpleTest(getPackageP(), true, true);

		doSingleUnitTest(protectConstructor, cu, getTestFileName(true, false));
	}

	/**
	 * Tests the IntroduceFactoryRefactoring refactoring on a single input source file
	 * whose name is the test name (minus the "test" prefix and any trailing
	 * options indicator such as "_FFF"), and compares the transformed code
	 * to a source file whose name is the test name (minus the "test" prefix).
	 * Test files are assumed to be located in the resources directory.
	 * @param baseFileName the base file name
	 * @param protectConstructor true iff IntroduceFactoryRefactoring should make the constructor private
	 * @throws Exception
	 */
	@SuppressWarnings("javadoc")
	protected void singleUnitBugHelper(String baseFileName, boolean protectConstructor)
		throws Exception
	{
		ICompilationUnit	cu= createCUForBugTestCase(null, getPackageP(), baseFileName, true);

		doSingleUnitTest(protectConstructor, cu, getBugTestFileName(null, getPackageP(), baseFileName, false));
	}

	protected void singleUnitBugHelperWithWarning(String baseFileName, boolean protectConstructor)
			throws Exception
	{
		ICompilationUnit cu= createCUForBugTestCase(null, getPackageP(), baseFileName, true);

		doSingleUnitTestWithWarning(protectConstructor, cu, getBugTestFileName(null, getPackageP(), baseFileName, false));
	}

	/**
	 * Like singleUnitHelper(), but allows for the specification of the names of
	 * the generated factory method, class, and interface, as appropriate.
	 * @param factoryMethodName the name to use for the generated factory method
	 * @param factoryClassName the name of the factory class
	 * @throws Exception
	 */
	@SuppressWarnings("javadoc")
	void namesHelper(String factoryMethodName, String factoryClassName)
		throws Exception
	{
		ICompilationUnit	cu= createCUForSimpleTest(getPackageP(), true, true);
		ISourceRange		selection= findSelectionInSource(cu.getSource());
		IntroduceFactoryRefactoring	ref= new IntroduceFactoryRefactoring(cu, selection.getOffset(), selection.getLength());

		RefactoringStatus	activationResult= ref.checkInitialConditions(new NullProgressMonitor());

		assertTrue("activation was supposed to be successful", activationResult.isOK());

		if (factoryMethodName != null)
			ref.setNewMethodName(factoryMethodName);
		if (factoryClassName != null)
			ref.setFactoryClass(factoryClassName);

		RefactoringStatus	checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());

		assertTrue("precondition was supposed to pass but was " + checkInputResult.toString(), checkInputResult.isOK());

		performChange(ref, false);

		String newSource = cu.getSource();

		assertEqualLines(getName() + ": ", getFileContents(getTestFileName(true, false)), newSource);
	}

	/**
	 * Creates a compilation unit for a source file with a given base name (plus
	 * "_in" suffix) in the given package. The source file is assumed to be
	 * located in the test resources directory.<br>
	 * Currently only handles positive tests.
	 * @param fileName the base name of the source file (minus the "_in" suffix)
	 * @param pack an IPackageFragment for the containing package
	 * @return the ICompilationUnit for the newly-created unit
	 * @throws Exception
	 */
	@SuppressWarnings("javadoc")
	private ICompilationUnit createCUFromFileName(String fileName, IPackageFragment pack) throws Exception {
		String fullName = TEST_PATH_PREFIX + getRefactoringPath() + "positive/" + fileName + "_in.java";

		return createCU(pack, fileName + "_in.java", getFileContents(fullName));
	}

	private void doMultiUnitTest(ICompilationUnit[] CUs, String testPath, String[] outputFileBaseNames, String factoryClassName) throws Exception, JavaModelException, IOException {
		ISourceRange selection= findSelectionInSource(CUs[0].getSource());
		IntroduceFactoryRefactoring	ref= new IntroduceFactoryRefactoring(CUs[0], selection.getOffset(), selection.getLength());

		RefactoringStatus activationResult= ref.checkInitialConditions(new NullProgressMonitor());

		assertTrue("activation was supposed to be successful", activationResult.isOK());

		if (factoryClassName != null)
			ref.setFactoryClass(factoryClassName);

		RefactoringStatus	checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());

		assertTrue("precondition was supposed to pass but was " + checkInputResult.toString(), checkInputResult.isOK());

		performChange(ref, false);

		String	testName= getName();

		for (int i = 0; i < CUs.length; i++) {
			int optIdx= testName.indexOf("_");
			String testOptions= (optIdx >= 0) ? testName.substring(optIdx) : "";
			String outFileName= testPath + outputFileBaseNames[i] + testOptions + "_out.java";
			String xformedSrc= CUs[i].getSource();
			String expectedSrc= getFileContents(outFileName);

			assertEqualLines(getName() + ": ", expectedSrc, xformedSrc);
		}
	}

	/**
	 * Tests the IntroduceFactoryRefactoring refactoring on a set of input source files
	 * whose names are supplied in the <code>fileBaseNames</code> argument,
	 * and compares the transformed code to source files whose names are
	 * the input base names plus the options suffix (e.g. "_FFF").
	 * Test files are assumed to be located in the resources directory.
	 * @param staticFactoryMethod true iff IntroduceFactoryRefactoring should make the factory method static
	 * @param inputFileBaseNames an array of input source file base names
	 * @throws Exception
	 */
	@SuppressWarnings("javadoc")
	void multiUnitHelper(boolean staticFactoryMethod, String[] inputFileBaseNames)
		throws Exception
	{
		IPackageFragment	pkg= getPackageP();
		ICompilationUnit	CUs[]= new ICompilationUnit[inputFileBaseNames.length];

		for (int i = 0; i < inputFileBaseNames.length; i++)
			CUs[i] = createCUFromFileName(inputFileBaseNames[i], pkg);

		String	testPath= TEST_PATH_PREFIX + getRefactoringPath() + "positive/";

		doMultiUnitTest(CUs, testPath, inputFileBaseNames, null);
	}

	/**
	 * Tests the IntroduceFactoryRefactoring refactoring on a set of input source files
	 * whose names are supplied in the <code>fileBaseNames</code> argument,
	 * and compares the transformed code to source files whose names are
	 * the input base names plus the options suffix (e.g. "_FFF").
	 * Test files are assumed to be located in the resources directory.
	 * @param staticFactoryMethod true iff IntroduceFactoryRefactoring should make the factory method static
	 * @param inputFileBaseNames an array of input source file base names
	 * @param factoryClassName the fully-qualified name of the class to receive the factory method, or null
	 * if the factory method is to be placed on the class defining the given constructor
	 * @throws Exception
	 */
	@SuppressWarnings("javadoc")
	void multiUnitBugHelper(boolean staticFactoryMethod, String[] inputFileBaseNames, String factoryClassName)
		throws Exception
	{
		ICompilationUnit CUs[]= new ICompilationUnit[inputFileBaseNames.length];

		for(int i= 0; i < inputFileBaseNames.length; i++) {
			int pkgEnd= inputFileBaseNames[i].lastIndexOf('/')+1;
			boolean explicitPkg= (pkgEnd > 0);
			IPackageFragment pkg= explicitPkg ? getRoot().createPackageFragment(inputFileBaseNames[i].substring(0, pkgEnd-1), true, new NullProgressMonitor()) : getPackageP();

			CUs[i]= createCUForBugTestCase(null, pkg, inputFileBaseNames[i].substring(pkgEnd), true);
		}

		String	testName= getName();
		String	testNumber= testName.substring("test".length());
		String	testPath= TEST_PATH_PREFIX + getRefactoringPath() + "Bugzilla/" + testNumber + "/";

		doMultiUnitTest(CUs, testPath, inputFileBaseNames, factoryClassName);
	}

	void multiProjectBugHelper(String[] inputFileBaseNames, String[] dependencies) throws Exception {
		Map<String, Set<String>> projName2PkgNames= collectProjectPackages(inputFileBaseNames);
		Map<String, IJavaProject> projName2Project= new HashMap<>();
		Map<IJavaProject, IPackageFragmentRoot> proj2PkgRoot= new HashMap<>();

		try {
			createProjectPackageStructure(projName2PkgNames, projName2Project, proj2PkgRoot);

			ICompilationUnit[] CUs= createCUs(inputFileBaseNames, projName2Project, proj2PkgRoot);

			addProjectDependencies(dependencies, projName2Project);

			String testName= getName();
			String testNumber= testName.substring("test".length());
			String testPath= TEST_PATH_PREFIX + getRefactoringPath() + "Bugzilla/" + testNumber + "/";

			doMultiUnitTest(CUs, testPath, inputFileBaseNames, null);

		} finally {
			for (IJavaProject project : proj2PkgRoot.keySet()) {
				if (project.exists()) {
					try {
						project.getProject().delete(true, null);
					} catch (CoreException e) {
						// swallow exception to avoid destroying the original one
						e.printStackTrace();
					}
				}
			}
		}
	}

	private ICompilationUnit[] createCUs(String[] inputFileBaseNames, Map<String, IJavaProject> projName2Project, Map<IJavaProject, IPackageFragmentRoot> proj2PkgRoot) throws Exception {
		ICompilationUnit CUs[]= new ICompilationUnit[inputFileBaseNames.length];

		for(int i= 0; i < inputFileBaseNames.length; i++) {
			String filePath= inputFileBaseNames[i];

			int projEnd= filePath.indexOf('/');
			int pkgEnd= filePath.lastIndexOf('/');
			int fileBegin= pkgEnd+1;

			String projName= filePath.substring(0, projEnd);
			String pkgName= filePath.substring(projEnd+1, pkgEnd).replace('/', '.');

			IJavaProject project= projName2Project.get(projName);
			IPackageFragmentRoot root= proj2PkgRoot.get(project);
			IPackageFragment pkg= root.getPackageFragment(pkgName);

			CUs[i]= createCUForBugTestCase(project, pkg, filePath.substring(fileBegin), true);
		}
		return CUs;
	}

	private void addProjectDependencies(String[] dependencies, Map<String, IJavaProject> projName2Project) throws JavaModelException {
		for (String dependency : dependencies) {
			// dependent:provider
			int colonIdx= dependency.indexOf(':');
			String depName= dependency.substring(0, colonIdx);
			String provName= dependency.substring(colonIdx+1);
			IJavaProject depProj= projName2Project.get(depName);
			IJavaProject provProj= projName2Project.get(provName);
			JavaProjectHelper.addRequiredProject(depProj, provProj);
		}
	}

	private void createProjectPackageStructure(Map<String, Set<String>> projName2PkgNames, Map<String, IJavaProject> projName2Project, Map<IJavaProject, IPackageFragmentRoot> proj2PkgRoot) throws CoreException, JavaModelException {
		for (Map.Entry<String, Set<String>> entry : projName2PkgNames.entrySet()) {
			String projName = entry.getKey();
			IJavaProject project= JavaProjectHelper.createJavaProject(projName, "bin");
			IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(project, CONTAINER);
			JavaProjectHelper.addRTJar(project);
			Set<IPackageFragment> pkgs= new HashSet<>();
			projName2Project.put(projName, project);
			proj2PkgRoot.put(project, root);
			for (String pkgName : entry.getValue()) {
				pkgs.add(root.createPackageFragment(pkgName, true, null));
			}
		}
	}

	private Map<String, Set<String>> collectProjectPackages(String[] inputFileBaseNames) {
		Map<String, Set<String>> proj2Pkgs= new HashMap<>();

		for (String filePath : inputFileBaseNames) {
			int projEnd= filePath.indexOf('/');
			String projName= filePath.substring(0, projEnd);
			String pkgName= filePath.substring(projEnd+1, filePath.lastIndexOf('/'));

			Set<String> projPkgs= proj2Pkgs.get(projName);

			if (projPkgs == null)
				proj2Pkgs.put(projName, projPkgs= new HashSet<>());
			projPkgs.add(pkgName);
		}
		return proj2Pkgs;
	}

	protected void failHelper(int expectedStatus) throws Exception {
		ICompilationUnit	cu= createCUForSimpleTest(getPackageP(), false, true);
		ISourceRange		selection= findSelectionInSource(cu.getSource());
		IntroduceFactoryRefactoring	ref= new IntroduceFactoryRefactoring(cu, selection.getOffset(), selection.getLength());
		RefactoringStatus	result= performRefactoring(ref);

		assertNotNull("precondition was supposed to fail", result);
		assertEquals("status", expectedStatus, result.getSeverity());
	}
}
