/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceFactoryRefactoring;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * @author rfuhrer@watson.ibm.com
 */
public class IntroduceFactoryTests extends RefactoringTest {
	
	private static final Class clazz= IntroduceFactoryTests.class;
	private static final String REFACTORING_PATH= "IntroduceFactory/";

	public IntroduceFactoryTests(String name) {
		super(name);
	} 
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
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
	 * @see getSimpleTestFileName(boolean input)
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
	 * @see getTestFileName()
	 */
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
	 * @param positive true iff the requested file is for a positive unit test
	 * @param input true iff the requested file is an input file
	 * @see getSimpleTestFileName(boolean input)
	 */
	private String getBugTestFileName(IPackageFragment pack, String fileName, boolean input) {
		String testName= getName();
		String testNumber= testName.substring("test".length());//$NON-NLS-1$
		String path= TEST_PATH_PREFIX + getRefactoringPath() + "Bugzilla/" + testNumber + "/" +
									(pack == getPackageP() ? "" : pack.getElementName() + "/");

		return path + fileName + (input ? "" : "_out") + ".java";//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Produces a compilation unit from an input source file whose path
	 * is based on the testcase name, but whose basename is supplied by
	 * the caller.
	 * Test files are assumed to be located in the resources directory.
	 * @param pack
	 * @param baseName
	 * @param input
	 * @return the ICompilationUnit created from the specified test file
	 * @see getTestFileName()
	 */
	private ICompilationUnit createCUForBugTestCase(IPackageFragment pack,
													String baseName, boolean input)
		throws Exception
	{
		String	fileName= getBugTestFileName(pack, baseName, input);
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
	 * @return an ISourceRange representing the marked selection
	 */
	private ISourceRange findSelectionInSource(String source) throws Exception {
		int		begin= source.indexOf(SELECTION_START_HERALD) + SELECTION_START_HERALD.length();
		int		end= source.indexOf(SELECTION_END_HERALD);

		if (begin < SELECTION_START_HERALD.length())
			assertTrue("No selection start comment in input source file!", false);
		if (end < 0)
			assertTrue("No selection end comment in input source file!", false);

		return new SourceRange(begin, end-begin);
	}

	private void doSingleUnitTest(boolean protectConstructor, ICompilationUnit cu, String outputFileName) throws Exception, JavaModelException, IOException {
		ISourceRange		selection= findSelectionInSource(cu.getSource());
		IntroduceFactoryRefactoring	ref= IntroduceFactoryRefactoring.create(cu, selection.getOffset(), selection.getLength(), 
												   JavaPreferencesSettings.getCodeGenerationSettings());

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
			assertTrue("precondition was supposed to pass but was " + checkInputResult.toString(), false);
		}

		performChange(ref, false);

		String newSource = cu.getSource();

		assertEqualLines(getName() + ": ", newSource, getFileContents(outputFileName));
	}

	/**
	 * Tests the IntroduceFactoryRefactoring refactoring on a single input source file
	 * whose name is the test name (minus the "test" prefix and any trailing
	 * options indicator such as "_FFF"), and compares the transformed code
	 * to a source file whose name is the test name (minus the "test" prefix).
	 * Test files are assumed to be located in the resources directory.
	 * @param staticFactoryMethod true iff IntroduceFactoryRefactoring should make the factory method static
	 * @param protectConstructor true iff IntroduceFactoryRefactoring should make the constructor private
	 * @see getTestFileName()
	 */
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
	 * @param staticFactoryMethod true iff IntroduceFactoryRefactoring should make the factory method static
	 * @param protectConstructor true iff IntroduceFactoryRefactoring should make the constructor private
	 * @see getTestFileName()
	 */
	protected void singleUnitBugHelper(String baseFileName, boolean protectConstructor)
		throws Exception
	{
		ICompilationUnit	cu= createCUForBugTestCase(getPackageP(), baseFileName, true);

		doSingleUnitTest(protectConstructor, cu, getBugTestFileName(getPackageP(), baseFileName, false));
	}

	/**
	 * Like singleUnitHelper(), but allows for the specification of the names of
	 * the generated factory method, class, and interface, as appropriate.
	 * @param staticFactoryMethod true iff IntroduceFactoryRefactoring should make the factory method static
	 * @param factoryMethodName the name to use for the generated factory method
	 * @see singleUnitHelper()
	 */
	void namesHelper(String factoryMethodName)
		throws Exception
	{
		ICompilationUnit	cu= createCUForSimpleTest(getPackageP(), true, true);
		ISourceRange		selection= findSelectionInSource(cu.getSource());
		IntroduceFactoryRefactoring	ref= IntroduceFactoryRefactoring.create(cu, selection.getOffset(), selection.getLength(), 
												   JavaPreferencesSettings.getCodeGenerationSettings());

		RefactoringStatus	activationResult= ref.checkInitialConditions(new NullProgressMonitor());	

		assertTrue("activation was supposed to be successful", activationResult.isOK());																

		ref.setNewMethodName(factoryMethodName);

		RefactoringStatus	checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());

		assertTrue("precondition was supposed to pass but was " + checkInputResult.toString(), checkInputResult.isOK());

		performChange(ref, false);

		String newSource = cu.getSource();

		assertEqualLines(getName() + ": ", newSource, getFileContents(getTestFileName(true, false)));
	}

	/**
	 * Creates a compilation unit for a source file with a given base name (plus
	 * "_in" suffix) in the given package. The source file is assumed to be
	 * located in the test resources directory.<br>
	 * Currently only handles positive tests.
	 * @param fileName the base name of the source file (minus the "_in" suffix)
	 * @param pack an IPackageFragment for the containing package
	 * @return the ICompilationUnit for the newly-created unit
	 */
	private ICompilationUnit createCUFromFileName(String fileName, IPackageFragment pack) throws Exception {
		String fullName = TEST_PATH_PREFIX + getRefactoringPath() + "positive/" + fileName + "_in.java";

		return createCU(pack, fileName + "_in.java", getFileContents(fullName));
	}

	private void doMultiUnitTest(ICompilationUnit[] CUs, String testPath, String[] outputFileBaseNames, String factoryClassName) throws Exception, JavaModelException, IOException {
		ISourceRange selection= findSelectionInSource(CUs[0].getSource());
		IntroduceFactoryRefactoring	ref= IntroduceFactoryRefactoring.create(CUs[0], selection.getOffset(), selection.getLength(), 
												   JavaPreferencesSettings.getCodeGenerationSettings());

		RefactoringStatus activationResult= ref.checkInitialConditions(new NullProgressMonitor());

		assertTrue("activation was supposed to be successful", activationResult.isOK());																

		if (factoryClassName != null)
			ref.setFactoryClass(factoryClassName);

		RefactoringStatus	checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());

		assertTrue("precondition was supposed to pass but was " + checkInputResult.toString(), checkInputResult.isOK());

		performChange(ref, false);

		String	testName= getName();

		for (int i = 0; i < CUs.length; i++) {
			int		optIdx= testName.indexOf("_");
			String	testOptions= (optIdx >= 0) ? testName.substring(optIdx) : "";
			String	outFileName= testPath + outputFileBaseNames[i] + testOptions + "_out.java";
			String	newSource= CUs[i].getSource();

			assertEqualLines(getName() + ": ", newSource, getFileContents(outFileName));
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
	 * @see createCUFromFileName()
	 */
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
	 * @see createCUFromFileName()
	 */
	void multiUnitBugHelper(boolean staticFactoryMethod, String[] inputFileBaseNames, String factoryClassName)
		throws Exception
	{
		ICompilationUnit CUs[]= new ICompilationUnit[inputFileBaseNames.length];

		for(int i= 0; i < inputFileBaseNames.length; i++) {
			int pkgEnd= inputFileBaseNames[i].lastIndexOf('/')+1;
			boolean explicitPkg= (pkgEnd > 0);
			IPackageFragment pkg= explicitPkg ? getRoot().createPackageFragment(inputFileBaseNames[i].substring(0, pkgEnd-1), true, new NullProgressMonitor()) : getPackageP();

			CUs[i]= createCUForBugTestCase(pkg, inputFileBaseNames[i].substring(pkgEnd), true);
		}

		String	testName= getName();
		String	testNumber= testName.substring("test".length());
		String	testPath= TEST_PATH_PREFIX + getRefactoringPath() + "Bugzilla/" + testNumber + "/";

		doMultiUnitTest(CUs, testPath, inputFileBaseNames, factoryClassName);
	}

	private void failHelper(boolean staticFactory, int expectedStatus) throws Exception {
		ICompilationUnit	cu= createCUForSimpleTest(getPackageP(), false, true);
		ISourceRange		selection= findSelectionInSource(cu.getSource());
		IntroduceFactoryRefactoring	ref= IntroduceFactoryRefactoring.create(cu, selection.getOffset(), selection.getLength(), 
												   JavaPreferencesSettings.getCodeGenerationSettings());
		RefactoringStatus	result= performRefactoring(ref);

		assertNotNull("precondition was supposed to fail", result);
		assertEquals("status", expectedStatus, result.getSeverity());
	}	

	private void failBugHelper(String baseFileName, boolean staticFactory, int expectedStatus) throws Exception {
		ICompilationUnit	cu= createCUForBugTestCase(getPackageP(), baseFileName, true);
		ISourceRange		selection= findSelectionInSource(cu.getSource());
		IntroduceFactoryRefactoring	ref= IntroduceFactoryRefactoring.create(cu, selection.getOffset(), selection.getLength(), 
				JavaPreferencesSettings.getCodeGenerationSettings());
		RefactoringStatus	result= performRefactoring(ref);

		assertNotNull("precondition was supposed to fail", result);
		assertEquals("status", expectedStatus, result.getSeverity());
	}	

	//--- TESTS
	public void testStaticContext_FFF() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	public void testInstanceContext_FFF() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	static final String[]	k_Names = { "createThing", "ThingFactory", "IThingFactory" };

	public void testNames_FFF() throws Exception {
		namesHelper(k_Names[0]);
	}
	//
	// ================================================================================
	//
	public void testMultipleCallers_FFF() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	public void testSelectConstructor() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	public void testDifferentSigs() throws Exception {
		singleUnitHelper(false);
	}

	public void testDifferentArgs1() throws Exception {
		singleUnitHelper(false);
	}

	public void testDifferentArgs2() throws Exception {
		singleUnitHelper(false);
	}

	public void testDifferentArgs3() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	public void testUnmovableArg1() throws Exception {
		singleUnitHelper(false);
	}

	public void testUnmovableArg2() throws Exception {
		singleUnitHelper(false);
	}

	public void testDontMoveArgs1() throws Exception {
		singleUnitHelper(false);
	}

	public void testDontMoveArgs2() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	public void testProtectConstructor1() throws Exception {
		singleUnitHelper(true);
	}

	public void testProtectConstructor2() throws Exception {
		singleUnitHelper(true);
	}
	//
	// ================================================================================
	//
	public void testStaticInstance() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	public void testCtorThrows() throws Exception {
		singleUnitHelper(true);
	}
	//
	// ================================================================================
	//
	public void testNestedClass() throws Exception {
		failHelper(false, RefactoringStatus.FATAL);
	}
	//
	// ================================================================================
	//
	public void testMultipleUnits_FFF() throws Exception {
		multiUnitHelper(false, new String[] { "MultiUnit1A", "MultiUnit1B", "MultiUnit1C" });
	}
	//
	// ================================================================================
	// Bugzilla bug regression tests
	// ================================================================================
	//
	public void test45942() throws Exception {
		multiUnitBugHelper(true, new String[] { "TestClass", "UseTestClass" }, null);
	}

	public void test46189() throws Exception {
		singleUnitBugHelper("TestClass", true);
	}

	public void test46189B() throws Exception {
		failBugHelper("TestClass", true, RefactoringStatus.FATAL);
	}

	public void test46373() throws Exception {
		singleUnitBugHelper("ImplicitCtor", false);
	}

	public void test46374() throws Exception {
		singleUnitBugHelper("QualifiedName", false);
	}

	public void test46608() throws Exception {
		multiUnitBugHelper(true, new String[] { "p1/TT", "p2/TT" }, null);
	}

	public void test48504() throws Exception {
		multiUnitBugHelper(true, new String[] { "p1/A", "p1/B" }, "p1.B");
	}

	public void test58293() throws Exception {
		singleUnitBugHelper("ImplicitSuperCtorCall", true);
	}
}
