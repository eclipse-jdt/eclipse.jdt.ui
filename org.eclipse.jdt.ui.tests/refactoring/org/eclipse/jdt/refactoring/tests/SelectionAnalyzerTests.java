/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.util.SelectionAnalyzer;

import org.eclipse.jdt.testplugin.TestPluginLauncher;

public class SelectionAnalyzerTests extends AbstractSelectionTestCase {

	private static SelectionAnalyzerTestSetup fgTestSetup;
	
	private static final int VALID_SELECTION=     1;
	private static final int INVALID_SELECTION=   2;
	
	public SelectionAnalyzerTests(String name) {
		super(name);
	}
	
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), SelectionAnalyzerTests.class, args);
	}
	
	public static Test suite() {
		fgTestSetup= new SelectionAnalyzerTestSetup(new TestSuite(SelectionAnalyzerTests.class));
		return fgTestSetup;
	}

	protected IPackageFragmentRoot getRoot() {
		return fgTestSetup.getRoot();
	}
	
	protected String getResourceLocation() {
		return "SelectionAnalyzerWorkSpace/SelectionAnalyzerTests/";
	}
	
	protected void selectionTest(int start, int length) throws Exception{
		ICompilationUnit unit= createCUfromTestFile(getSelectionPackage(), "A");
		String source= unit.getSource();
		int[] selection= getSelection(source);
		assertEquals(start, selection[0]);
		assertEquals(length, selection[1]);
	}
	
	private IPackageFragment getSelectionPackage() throws JavaModelException {
		return fgTestSetup.getSelectionPackage();
 	}
	
	protected void performTest(IPackageFragment packageFragment, String id, int mode, String outputFolder) throws Exception {
		IProgressMonitor pm= new NullProgressMonitor();
		ICompilationUnit unit= createCUfromTestFile(packageFragment, id);
		String source= unit.getSource();
		int[] selection= getSelection(source);
		SelectionAnalyzer analyzer= new SelectionAnalyzer(unit.getBuffer(), selection[0], selection[1]);
		((CompilationUnit)unit).accept(analyzer.getParentTracker());
		RefactoringStatus status= analyzer.getStatus();
		switch (mode) {
			case VALID_SELECTION:
				System.out.println(status);
				assertTrue(status.isOK());
				break;
			case INVALID_SELECTION:
				System.out.println(status);
				assertTrue(!status.isOK());
				break;
		}
	}
	
	protected void invalidSelectionTest() throws Exception {
		performTest(fgTestSetup.getInvalidSelectionPackage(), "A", INVALID_SELECTION, null);
	}
	
	protected void validSelectionTest() throws Exception {
		performTest(fgTestSetup.getValidSelectionPackage(), "A", VALID_SELECTION, null);
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
	
	public void test048() throws Exception {
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
	
	//---- invalid return statement selection
	
	public void test140() throws Exception {
		invalidSelectionTest();
	}
	
	public void test141() throws Exception {
		invalidSelectionTest();
	}
	
	public void test142() throws Exception {
		invalidSelectionTest();
	}
	
	public void test143() throws Exception {
		invalidSelectionTest();
	}
	
	public void test144() throws Exception {
		invalidSelectionTest();
	}
	
	public void test145() throws Exception {
		invalidSelectionTest();
	}
	
	public void test146() throws Exception {
		invalidSelectionTest();
	}
	
	public void test147() throws Exception {
		invalidSelectionTest();
	}
	
	public void test148() throws Exception {
		invalidSelectionTest();
	}
	
	public void test149() throws Exception {
		invalidSelectionTest();
	}
	
	//---- Synchronized statement
	
	public void test150() throws Exception {
		invalidSelectionTest();
	}
	
	public void test151() throws Exception {
		System.out.println("\n151 disabled since it fails. See 1GE2LO2");
		// invalidSelectionTest();
	}
	
	public void test152() throws Exception {
		invalidSelectionTest();
	}
	
	public void test153() throws Exception {
		invalidSelectionTest();
	}
	
	public void test160() throws Exception {
		invalidSelectionTest();
	}
	
	public void test161() throws Exception {
		invalidSelectionTest();
	}
	
	//----- local declarations
	
	public void test170() throws Exception {
		System.out.println("\n170 disabled since it fails. See 1GF089K");
		// invalidSelectionTest();
	}
	
	public void test171() throws Exception {
		System.out.println("\n171 disabled since it fails. See 1GF089K");
		// invalidSelectionTest();
	}
	
	public void test172() throws Exception {
		System.out.println("\n172 disabled since it fails. See 1GF089K");
		// invalidSelectionTest();
	}
	
	public void test173() throws Exception {
		invalidSelectionTest();
	}

	//---- Constructor
	
	public void test180() throws Exception {
		invalidSelectionTest();
	}
	
	public void test181() throws Exception {
		invalidSelectionTest();
	}
	
	//---- More return statement handling
	
	public void test190() throws Exception {
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
	
	public void test202() throws Exception {
		validSelectionTest();
	}
	
	public void test203() throws Exception {
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
	
	//---- Synchronized statement
	
	public void test350() throws Exception {
		System.out.println("\n350 disabled since it fails. See 1GIRHRP");
		// validSelectionTest();
	}
	
	public void test351() throws Exception {
		System.out.println("\n351 disabled since it fails. See 1GIRHRP");
		// validSelectionTest();
	}
	
	public void test352() throws Exception {
		validSelectionTest();
	}
	
	public void test353() throws Exception {
		validSelectionTest();
	}	
}