/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;import junit.framework.TestSuite;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.ChangeContext;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.RefactoringStatus;import org.eclipse.jdt.core.refactoring.code.ExtractMethodRefactoring;import org.eclipse.jdt.refactoring.tests.infra.TestExceptionHandler;import org.eclipse.jdt.refactoring.tests.infra.TextBufferChangeCreator;import org.eclipse.jdt.testplugin.JavaTestSetup;import org.eclipse.jdt.testplugin.TestPluginLauncher;import org.eclipse.jdt.testplugin.ui.TestPluginUILauncher;

public class ExtractMethodTests extends RefactoringTest {

	private static final String SQUARE_BRACKET_OPEN= "/*[*/";
	private static final int    SQUARE_BRACKET_OPEN_LENGTH= SQUARE_BRACKET_OPEN.length();
	private static final String SQUARE_BRACKET_CLOSE=   "/*]*/";
	private static final int    SQUARE_BRACKET_CLOSE_LENGTH= SQUARE_BRACKET_CLOSE.length();

	private IPackageFragment fSelectionPackage;
	private IPackageFragment fInvalidSelectionPackage;
	private IPackageFragment fValidSelectionPackage;
	private IPackageFragment fSemicolonPackage;
	private IPackageFragment fTryPackage;
	private IPackageFragment fLocalsPackage;
	
	private static final int VALID_SELECTION=     1;
	private static final int INVALID_SELECTION=   2;
	private static final int COMPARE_WITH_OUTPUT= 3;
	
	public ExtractMethodTests(String name) {
		super(name);
		// fgIsVerbose= true;
	}
	
	public static void main(String[] args) {
		TestPluginUILauncher.run(TestPluginLauncher.getLocationFromProperties(), ExtractMethodTests.class, args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());
		return new JavaTestSetup(suite);
	}

	public static Test noSetupSuite() {
		return new TestSuite(ExtractMethodTests.class);
	}
	
	protected String getRefactoringPath() {
		return "ExtractMethodWorkSpace/ExtractMethodTests/";
	}
	
	protected String getTestFileName(String packageName, String id) {
		String result= getTestPath() + packageName + "/" + id + "_" + name() + ".java";
		return result;
	}
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String id) throws Exception {
		return createCU(pack, createClassName(id) + ".java", getFileContents(
			getTestFileName(pack.getElementName(), id)));
	}
	
	private String createClassName(String id) {
		return id + "_" + name();
	}

	protected int[] getSelection(String source) {
		int start= -1;
		int end= -1;
		int includingStart= source.indexOf(SQUARE_BRACKET_OPEN);
		int excludingStart= source.indexOf(SQUARE_BRACKET_CLOSE);
		int includingEnd= source.lastIndexOf(SQUARE_BRACKET_CLOSE);
		int excludingEnd= source.lastIndexOf(SQUARE_BRACKET_OPEN);

		if (includingStart > excludingStart && excludingStart != -1) {
			includingStart= -1;
		} else if (excludingStart > includingStart && includingStart != -1) {
			excludingStart= -1;
		}
		
		if (includingEnd < excludingEnd) {
			includingEnd= -1;
		} else if (excludingEnd < includingEnd) {
			excludingEnd= -1;
		}
		
		if (includingStart != -1) {
			start= includingStart;
		} else {
			start= excludingStart + SQUARE_BRACKET_CLOSE_LENGTH;
		}
		
		if (excludingEnd != -1) {
			end= excludingEnd;
		} else {
			end= includingEnd + SQUARE_BRACKET_CLOSE_LENGTH;
		}
		
		assert("Selection invalid", start >= 0 && end >= 0 && end >= start);
		
		int[] result= new int[] { start, end - start }; 
		// System.out.println("|"+ source.substring(result[0], result[0] + result[1]) + "|");
		return result;
	}
	
	protected void selectionTest(int start, int length) throws Exception{
		ICompilationUnit unit= createCUfromTestFile(getSelectionPackage(), "A");
		String source= unit.getSource();
		int[] selection= getSelection(source);
		assertEquals(start, selection[0]);
		assertEquals(length, selection[1]);
	}
	
	private IPackageFragment getSelectionPackage() throws JavaModelException {
		if (fSelectionPackage == null)
			fSelectionPackage= getRoot().createPackageFragment("selection", true, null);
					
		return fSelectionPackage;
	}
	
	protected void performTest(IPackageFragment packageFragment, String id, int mode, String outputFolder) throws Exception {
		IProgressMonitor pm= new NullProgressMonitor();
		ICompilationUnit unit= createCUfromTestFile(packageFragment, id);
		String source= unit.getSource();
		int[] selection= getSelection(source);
		ExtractMethodRefactoring refactoring= new ExtractMethodRefactoring(
			unit, new TextBufferChangeCreator(), selection[0], selection[1]);
		refactoring.setMethodName("extracted");
		RefactoringStatus status= refactoring.checkPreconditions(pm);
		switch (mode) {
			case VALID_SELECTION:
				assert(status.isOK());
				break;
			case INVALID_SELECTION:
				assert(!status.isOK());
				break;
			case COMPARE_WITH_OUTPUT:
				IChange change= refactoring.createChange(pm);
				assertNotNull(change);
				change.aboutToPerform();
				change.perform(new ChangeContext(new TestExceptionHandler()), pm);
				change.performed();
				assertNotNull(change.getUndoChange());
				source= unit.getSource();
				String out= getFileContents(
					getTestFileName(outputFolder, id));
				assert(compareSource(source, out));
				break;		
		}
	}
	
	private boolean compareSource(String refactored, String proofed) {
		int index= refactored.indexOf(';');
		refactored= refactored.substring(index);
		index= proofed.indexOf(';');
		proofed= proofed.substring(index);
		// System.out.println(refactored);
		// System.out.println(proofed);
		return refactored.equals(proofed);
	}
	
	protected void invalidSelectionTest() throws Exception {
		performTest(getInvalidSelectionPackage(), "A", INVALID_SELECTION, null);
	}
	
	private IPackageFragment getInvalidSelectionPackage() throws JavaModelException {
		if (fInvalidSelectionPackage == null)
			fInvalidSelectionPackage= getRoot().createPackageFragment("invalidSelection", true, null);
					
		return fInvalidSelectionPackage;
	}
	
	protected void validSelectionTest() throws Exception {
		performTest(getValidSelectionPackage(), "A", VALID_SELECTION, null);
	}
	
	private IPackageFragment getValidSelectionPackage() throws JavaModelException {
		if (fValidSelectionPackage == null)
			fValidSelectionPackage= getRoot().createPackageFragment("validSelection", true, null);
					
		return fValidSelectionPackage;
	}
	
	protected void semicolonTest() throws Exception {
		performTest(getSemicolonPackage(), "A", COMPARE_WITH_OUTPUT, "semicolon_out");
	}
	
	private IPackageFragment getSemicolonPackage() throws JavaModelException {
		if (fSemicolonPackage == null)
			fSemicolonPackage= getRoot().createPackageFragment("semicolon_in", true, null);
					
		return fSemicolonPackage;
	}
	
	protected void tryTest() throws Exception {
		performTest(getTryPackage(), "A", COMPARE_WITH_OUTPUT, "try_out");
	}
	
	private IPackageFragment getTryPackage() throws JavaModelException {
		if (fTryPackage == null)
			fTryPackage= getRoot().createPackageFragment("try_in", true, null);
					
		return fTryPackage;
	}
	
	protected void localsTest() throws Exception {
		performTest(getLocalsPackage(), "A", COMPARE_WITH_OUTPUT, "locals_out");
	}
	
	private IPackageFragment getLocalsPackage() throws JavaModelException {
		if (fLocalsPackage == null)
			fLocalsPackage= getRoot().createPackageFragment("locals_in", true, null);
					
		return fLocalsPackage;
	}
	
	//=====================================================================================
	// Testing selections
	//=====================================================================================
	public void test1() throws Exception {
		selectionTest(66, 15);
	}
	
	public void test2() throws Exception {
		selectionTest(66, 10);
	}
	
	public void test3() throws Exception {
		selectionTest(71, 10);
	}
	
	public void test4() throws Exception {
		selectionTest(71, 5);
	}
	
	//=====================================================================================
	// Testing invalid selections
	//=====================================================================================
	
	//---- Misc
	
	public void test010() throws Exception {
		invalidSelectionTest();
	}
	
	public void test011() throws Exception {
		invalidSelectionTest();
	}
	
	public void test012() throws Exception {
		invalidSelectionTest();
	}
	
	public void test013() throws Exception {
		invalidSelectionTest();
	}
	
	public void test014() throws Exception {
		invalidSelectionTest();
	}
	
	//---- Switch / Case
	
	public void test020() throws Exception {
		invalidSelectionTest();
	}
	
	public void test021() throws Exception {
		invalidSelectionTest();
	}
	
	public void test022() throws Exception {
		invalidSelectionTest();
	}

	//---- Block
	
	public void test030() throws Exception {
		invalidSelectionTest();
	}
	
	public void test031() throws Exception {
		invalidSelectionTest();
	}
	
	//---- For
	
	public void test040() throws Exception {
		invalidSelectionTest();
	}
	
	public void test041() throws Exception {
		invalidSelectionTest();
	}
	
	public void test042() throws Exception {
		invalidSelectionTest();
	}
	
	public void test043() throws Exception {
		invalidSelectionTest();
	}
	
	public void test044() throws Exception {
		invalidSelectionTest();
	}
	
	public void test045() throws Exception {
		invalidSelectionTest();
	}
	
	public void test046() throws Exception {
		invalidSelectionTest();
	}
	
	public void test047() throws Exception {
		invalidSelectionTest();
	}
	
	//---- While
	
	public void test050() throws Exception {
		invalidSelectionTest();
	}
	
	public void test051() throws Exception {
		invalidSelectionTest();
	}
	
	public void test052() throws Exception {
		invalidSelectionTest();
	}
	
	//---- do / While
	
	public void test060() throws Exception {
		invalidSelectionTest();
	}
	
	public void test061() throws Exception {
		invalidSelectionTest();
	}
	
	public void test062() throws Exception {
		invalidSelectionTest();
	}
	
	public void test063() throws Exception {
		invalidSelectionTest();
	}
	
	//---- switch
	
	public void test070() throws Exception {
		invalidSelectionTest();
	}
	
	public void test071() throws Exception {
		invalidSelectionTest();
	}
	
	public void test072() throws Exception {
		invalidSelectionTest();
	}
	
	public void test073() throws Exception {
		invalidSelectionTest();
	}
	
	//---- if then else
	
	public void test080() throws Exception {
		invalidSelectionTest();
	}
	
	public void test081() throws Exception {
		invalidSelectionTest();
	}
	
	public void test082() throws Exception {
		invalidSelectionTest();
	}
	
	public void test083() throws Exception {
		invalidSelectionTest();
	}
	
	public void test084() throws Exception {
		invalidSelectionTest();
	}
	
	public void test085() throws Exception {
		invalidSelectionTest();
	}
	
	//---- Break
	
	public void test090() throws Exception {
		invalidSelectionTest();
	}
	
	public void test091() throws Exception {
		invalidSelectionTest();
	}
	
	public void test092() throws Exception {
		invalidSelectionTest();
	}
	
	public void test093() throws Exception {
		invalidSelectionTest();
	}
	
	public void test094() throws Exception {
		invalidSelectionTest();
	}
	
	public void test095() throws Exception {
		invalidSelectionTest();
	}
	
	public void test096() throws Exception {
		invalidSelectionTest();
	}
	
	//---- Try / catch / finally
	
	public void test100() throws Exception {
		invalidSelectionTest();
	}
	
	public void test101() throws Exception {
		invalidSelectionTest();
	}
	
	public void test102() throws Exception {
		invalidSelectionTest();
	}
	
	public void test103() throws Exception {
		invalidSelectionTest();
	}
	
	public void test104() throws Exception {
		invalidSelectionTest();
	}
	
	public void test105() throws Exception {
		invalidSelectionTest();
	}
	
	public void test106() throws Exception {
		invalidSelectionTest();
	}
	
	public void test107() throws Exception {
		invalidSelectionTest();
	}
	
	public void test108() throws Exception {
		invalidSelectionTest();
	}
	
	public void test109() throws Exception {
		invalidSelectionTest();
	}
	
	public void test110() throws Exception {
		invalidSelectionTest();
	}
	
	public void test111() throws Exception {
		invalidSelectionTest();
	}
	
	public void test112() throws Exception {
		invalidSelectionTest();
	}
	
	public void test113() throws Exception {
		invalidSelectionTest();
	}
	
	public void test114() throws Exception {
		invalidSelectionTest();
	}
	
	public void test115() throws Exception {
		invalidSelectionTest();
	}

	//---- invalid local var selection
	
	public void test120() throws Exception {
		invalidSelectionTest();
	}
	
	public void test121() throws Exception {
		invalidSelectionTest();
	}
	
	public void test122() throws Exception {
		invalidSelectionTest();
	}
	
	//---- invalid local type selection
	
	public void test130() throws Exception {
		invalidSelectionTest();
	}
	
	public void test131() throws Exception {
		invalidSelectionTest();
	}
	
	//====================================================================================
	// Testing valid selections
	//=====================================================================================
	
	//---- Misc
	
	public void test200() throws Exception {
		validSelectionTest();
	}
	
	public void test201() throws Exception {
		validSelectionTest();
	}
	
	//---- Block
	
	public void test230() throws Exception {
		validSelectionTest();
	}
	
	public void test231() throws Exception {
		validSelectionTest();
	}
	
	public void test232() throws Exception {
		validSelectionTest();
	}
	
	public void test233() throws Exception {
		validSelectionTest();
	}
	
	public void test234() throws Exception {
		validSelectionTest();
	}
	
	public void test235() throws Exception {
		validSelectionTest();
	}
	
	//---- For statement
	
	public void test240() throws Exception {
		validSelectionTest();
	}
	
	public void test241() throws Exception {
		validSelectionTest();
	}
	
	public void test242() throws Exception {
		validSelectionTest();
	}
	
	public void test243() throws Exception {
		validSelectionTest();
	}
	
	public void test244() throws Exception {
		validSelectionTest();
	}
	
	public void test245() throws Exception {
		validSelectionTest();
	}
	
	public void test246() throws Exception {
		validSelectionTest();
	}
	
	public void test247() throws Exception {
		validSelectionTest();
	}
			
	public void test248() throws Exception {
		validSelectionTest();
	}
			
	public void test249() throws Exception {
		validSelectionTest();
	}
			
	//---- While statement
	
	public void test250() throws Exception {
		validSelectionTest();
	}
	
	public void test251() throws Exception {
		validSelectionTest();
	}	
	
	public void test252() throws Exception {
		validSelectionTest();
	}	
	
	public void test253() throws Exception {
		validSelectionTest();
	}
		
	public void test254() throws Exception {
		validSelectionTest();
	}
	
	public void test255() throws Exception {
		validSelectionTest();
	}
	
	//---- do while statement
	
	public void test260() throws Exception {
		validSelectionTest();
	}
	
	public void test261() throws Exception {
		validSelectionTest();
	}	
		
	public void test262() throws Exception {
		validSelectionTest();
	}
	
	public void test263() throws Exception {
		validSelectionTest();
	}
	
	//---- switch
	
	public void test270() throws Exception {
		validSelectionTest();
	}
	
	public void test271() throws Exception {
		validSelectionTest();
	}	
	
	public void test272() throws Exception {
		validSelectionTest();
	}	
	
	public void test273() throws Exception {
		validSelectionTest();
	}
	
	public void test274() throws Exception {
		validSelectionTest();
	}
	
	public void test275() throws Exception {
		validSelectionTest();
	}
	
	//---- if then else
	
	public void test280() throws Exception {
		validSelectionTest();
	}
	
	public void test281() throws Exception {
		validSelectionTest();
	}	
	
	public void test282() throws Exception {
		validSelectionTest();
	}
	
	public void test283() throws Exception {
		validSelectionTest();
	}
	
	public void test284() throws Exception {
		validSelectionTest();
	}
	
	public void test285() throws Exception {
		validSelectionTest();
	}
	
	//---- try / catch / finally
	
	public void test300() throws Exception {
		validSelectionTest();
	}
	
	public void test301() throws Exception {
		validSelectionTest();
	}	
	
	public void test302() throws Exception {
		validSelectionTest();
	}				
	
	public void test304() throws Exception {
		validSelectionTest();
	}				
	
	public void test305() throws Exception {
		validSelectionTest();
	}				
	
	public void test306() throws Exception {
		validSelectionTest();
	}				
	
	public void test307() throws Exception {
		validSelectionTest();
	}				
	
	public void test308() throws Exception {
		validSelectionTest();
	}				
	
	public void test309() throws Exception {
		validSelectionTest();
	}
	
	public void test310() throws Exception {
		validSelectionTest();
	}
	
	public void test311() throws Exception {
		validSelectionTest();
	}
	
	//====================================================================================
	// Testing Extracted result
	//====================================================================================

	//---- Test semicolon
	
	public void test400() throws Exception {
		semicolonTest();
	}
	
	public void test401() throws Exception {
		semicolonTest();
	}
	
	public void test402() throws Exception {
		semicolonTest();
	}
	
	public void test403() throws Exception {
		semicolonTest();
	}	
	
	public void test404() throws Exception {
		semicolonTest();
	}	
	
	public void test405() throws Exception {
		System.out.println("\n405 disabled since it fails. See 1GDGFR8");
		// semicolonTest();
	}	
	
	//---- Test Try / catch block

	public void test450() throws Exception {
		tryTest();
	}
	
	public void test451() throws Exception {
		tryTest();
	}
	
	public void test452() throws Exception {
		tryTest();
	}
	
	public void test453() throws Exception {
		tryTest();
	}
	
	//---- Test local vars and types
	
	public void test500() throws Exception {
		localsTest();
	}
	
	public void test501() throws Exception {
		localsTest();
	}
	
	public void test502() throws Exception {
		localsTest();
	}
	
	public void test503() throws Exception {
		localsTest();
	}
	
	public void test504() throws Exception {
		localsTest();
	}
	
	public void test505() throws Exception {
		localsTest();
	}
	
	public void test506() throws Exception {
		localsTest();
	}
	
	public void test507() throws Exception {
		localsTest();
	}
	
	public void test508() throws Exception {
		localsTest();
	}
	
	public void test509() throws Exception {
		localsTest();
	}
}