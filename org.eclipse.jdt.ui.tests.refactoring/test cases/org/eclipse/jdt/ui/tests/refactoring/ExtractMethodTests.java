/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

public class ExtractMethodTests extends AbstractSelectionTestCase {
	private static ExtractMethodTestSetup fgTestSetup;
	
	public ExtractMethodTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		fgTestSetup= new ExtractMethodTestSetup(new TestSuite(ExtractMethodTests.class));
		return fgTestSetup;
	}
	
	public static Test setUpTest(Test test) {
		fgTestSetup= new ExtractMethodTestSetup(test);
		return fgTestSetup;
	}

	protected void setUp() throws Exception {
		super.setUp();
		fIsPreDeltaTest= true;
	}
	
	protected String getResourceLocation() {
		return "ExtractMethodWorkSpace/ExtractMethodTests/";
	}
	
	protected String adaptName(String name) {
		return name + "_" + getName() + ".java";
	}	
	
	protected void selectionTest(int startLine, int startColumn, int endLine, int endColumn) throws Exception{
		ICompilationUnit unit= createCU(getSelectionPackage(), "A");
		int[] selection= getSelection();
		ISourceRange expected= TextRangeUtil.getSelection(unit, startLine, startColumn, endLine, endColumn);
		assertEquals(expected.getOffset(), selection[0]);
		assertEquals(expected.getLength(), selection[1]);
	}
	
	private IPackageFragment getSelectionPackage() throws JavaModelException {
		return fgTestSetup.getSelectionPackage();
 	}
	
	protected void performTest(IPackageFragment packageFragment, String id, int mode, String outputFolder) throws Exception {
		performTest(packageFragment, id, mode, outputFolder, null, null, 0);
	}
	
	protected void performTest(IPackageFragment packageFragment, String id, int mode, String outputFolder, String[] newNames, int[] newOrder, int destination) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		int[] selection= getSelection();
		ExtractMethodRefactoring refactoring= new ExtractMethodRefactoring(unit, selection[0], selection[1]);
		refactoring.setMethodName("extracted");
		refactoring.setVisibility(Modifier.PROTECTED);
		TestModelProvider.clearDelta();
		RefactoringStatus status= refactoring.checkInitialConditions(new NullProgressMonitor());
		switch (mode) {
			case VALID_SELECTION:
				assertTrue(status.isOK());
				break;
			case INVALID_SELECTION:
				if (!status.isOK())
					return;
		}
		List parameters= refactoring.getParameterInfos();
		if (newNames != null && newNames.length > 0) {
			for (int i= 0; i < newNames.length; i++) {
				if (newNames[i] != null)
					((ParameterInfo)parameters.get(i)).setNewName(newNames[i]);
			}
		}
		if (newOrder != null && newOrder.length > 0) {
			assertTrue(newOrder.length == parameters.size());
			List current= new ArrayList(parameters);
			for (int i= 0; i < newOrder.length; i++) {
				parameters.set(newOrder[i], current.get(i));
			}
		}
		refactoring.setDestination(destination);
		
		String out= null;
		switch (mode) {
			case COMPARE_WITH_OUTPUT:
				out= getProofedContent(outputFolder, id);
				break;		
		}
		performTest(unit, refactoring, mode, out, true);
	}
	
	protected int getCheckingStyle() {
		return CheckConditionsOperation.FINAL_CONDITIONS;
	}
	
	protected void clearPreDelta() {
		// Do nothing. We clear the delta before 
		// initial condition checking
	}
	
	protected void invalidSelectionTest() throws Exception {
		performTest(fgTestSetup.getInvalidSelectionPackage(), "A", INVALID_SELECTION, null);
	}
	
	protected void validSelectionTest() throws Exception {
		performTest(fgTestSetup.getValidSelectionPackage(), "A", VALID_SELECTION, null);
	}
	
	protected void validSelectionTestChecked() throws Exception {
		performTest(fgTestSetup.getValidSelectionCheckedPackage(), "A", COMPARE_WITH_OUTPUT, "validSelection_out");
	}
	
	protected void semicolonTest() throws Exception {
		performTest(fgTestSetup.getSemicolonPackage(), "A", COMPARE_WITH_OUTPUT, "semicolon_out");
	}
	
	protected void tryTest() throws Exception {
		performTest(fgTestSetup.getTryPackage(), "A", COMPARE_WITH_OUTPUT, "try_out");
	}
	
	protected void localsTest() throws Exception {
		performTest(fgTestSetup.getLocalsPackage(), "A", COMPARE_WITH_OUTPUT, "locals_out");
	}
	
	protected void expressionTest() throws Exception {
		performTest(fgTestSetup.getExpressionPackage(), "A", COMPARE_WITH_OUTPUT, "expression_out");
	}
	
	protected void nestedTest() throws Exception {
		performTest(fgTestSetup.getNestedPackage(), "A", COMPARE_WITH_OUTPUT, "nested_out");
	}
	
	protected void returnTest() throws Exception {
		performTest(fgTestSetup.getReturnPackage(), "A", COMPARE_WITH_OUTPUT, "return_out");
	}
	
	protected void branchTest() throws Exception {
		performTest(fgTestSetup.getBranchPackage(), "A", COMPARE_WITH_OUTPUT, "branch_out");
	}
	
	protected void errorTest() throws Exception {
		performTest(fgTestSetup.getErrorPackage(), "A", COMPARE_WITH_OUTPUT, "error_out");
	}
	
	protected void wikiTest() throws Exception {
		performTest(fgTestSetup.getWikiPackage(), "A", COMPARE_WITH_OUTPUT, "wiki_out");
	}
	
	protected void duplicatesTest() throws Exception {
		performTest(fgTestSetup.getDuplicatesPackage(), "A", COMPARE_WITH_OUTPUT, "duplicates_out");
	}
	
	protected void initializerTest() throws Exception {
		performTest(fgTestSetup.getInitializerPackage(), "A", COMPARE_WITH_OUTPUT, "initializer_out");
	}
	
	protected void destinationTest(int destination) throws Exception {
		performTest(fgTestSetup.getDestinationPackage(), "A", COMPARE_WITH_OUTPUT, "destination_out",
			null, null, destination);
	}
	
	protected void genericTest() throws Exception {
		performTest(fgTestSetup.getGenericsPackage(), "A", COMPARE_WITH_OUTPUT, "generics_out");
	}
	
	protected void enumTest() throws Exception {
		performTest(fgTestSetup.getEnumsPackage(), "A", COMPARE_WITH_OUTPUT, "enums_out");
	}
	
	//=====================================================================================
	// Testing selections
	//=====================================================================================
	

	public void test1() throws Exception {
		selectionTest(5, 9, 5, 24);
	}
	
	public void test2() throws Exception {
		selectionTest(5, 9, 5, 19);
	}
	
	public void test3() throws Exception {
		selectionTest(5, 14, 5, 24);
	}
	
	public void test4() throws Exception {
		selectionTest(5, 14, 5, 19);
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
	
	public void test015() throws Exception {
		invalidSelectionTest();
	}
	
	public void test016() throws Exception {
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
	
	public void test123() throws Exception {
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
		invalidSelectionTest();
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
		invalidSelectionTest();
	}
	
	public void test171() throws Exception {
		invalidSelectionTest();
	}
	
	public void test172() throws Exception {
		invalidSelectionTest();
	}
	
	public void test173() throws Exception {
		invalidSelectionTest();
	}

	//---- Constructor
	
	public void test180() throws Exception {
		// System.out.println(getClass().getName() + "::"+  getName() + " disabled - see bug 11853");
		invalidSelectionTest();
	}
	
	public void test181() throws Exception {
		// System.out.println(getClass().getName() + "::"+  getName() + " disabled - see bug 11853");
		invalidSelectionTest();
	}
	
	//---- More return statement handling
	
	public void test190() throws Exception {
		invalidSelectionTest();
	}
	
	public void test191() throws Exception {
		invalidSelectionTest();
	}
	
	//---- Assignment
	
	public void test192() throws Exception {
		invalidSelectionTest();
	}
	
	public void test193() throws Exception {
		invalidSelectionTest();
	}
	
	//---- single names
	
	public void test194() throws Exception {
		invalidSelectionTest();
	}
	
	//---- case expression
	
	public void test195() throws Exception {
		invalidSelectionTest();
	}
	
	//---- more than one value to return
	
	public void test196() throws Exception {
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
		validSelectionTest();
	}
	
	public void test351() throws Exception {
		validSelectionTest();
	}
	
	public void test352() throws Exception {
		validSelectionTest();
	}
	
	public void test353() throws Exception {
		validSelectionTest();
	}
	
	public void test360() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test361() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test362() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test363() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test364() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test365() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test366() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test367() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test368() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test369() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test370() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test371() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test372() throws Exception {
		validSelectionTestChecked();
	}
	
	public void test373() throws Exception {
		validSelectionTestChecked();
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
	
	public void test406() throws Exception {
		semicolonTest();
	}	
	
	public void test407() throws Exception {
		semicolonTest();
	}	
	
	public void test409() throws Exception {
		semicolonTest();
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
	
	public void test454() throws Exception {
		tryTest();
	}
	
	public void test455() throws Exception {
		tryTest();
	}
	
	public void test456() throws Exception {
		tryTest();
	}
	
	public void test457() throws Exception {
		tryTest();
	}
	
	public void test458() throws Exception {
		tryTest();
	}
	
	public void test459() throws Exception {
		tryTest();
	}
	
	public void test460() throws Exception {
		tryTest();
	}
	
	public void test461() throws Exception {
		tryTest();
	}
	
	public void test462() throws Exception {
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
	
	public void test510() throws Exception {
		localsTest();
	}
	
	public void test511() throws Exception {
		localsTest();
	}
		
	public void test512() throws Exception {
		localsTest();
	}
	
	public void test513() throws Exception {
		localsTest();
	}
	
	public void test514() throws Exception {
		localsTest();
	}
	
	public void test515() throws Exception {
		localsTest();
	}
	
	public void test516() throws Exception {
		localsTest();
	}
	
	public void test517() throws Exception {
		localsTest();
	}
	
	public void test518() throws Exception {
		localsTest();
	}
	
	public void test519() throws Exception {
		localsTest();
	}
	
	public void test520() throws Exception {
		localsTest();
	}
	
	public void test521() throws Exception {
		localsTest();
	}
	
	public void test522() throws Exception {
		localsTest();
	}
	
	public void test523() throws Exception {
		localsTest();
	}
	
	public void test524() throws Exception {
		localsTest();
	}
	
	public void test525() throws Exception {
		localsTest();
	}
	
	public void test526() throws Exception {
		localsTest();
	}
	
	public void test527() throws Exception {
		localsTest();
	}
	
	public void test528() throws Exception {
		localsTest();
	}
	
	public void test530() throws Exception {
		localsTest();
	}
	
	public void test531() throws Exception {
		localsTest();
	}
	
	public void test532() throws Exception {
		localsTest();
	}
	
	public void test533() throws Exception {
		localsTest();
	}
	
	public void test534() throws Exception {
		localsTest();
	}
	
	public void test535() throws Exception {
		localsTest();
	}
	
	public void test536() throws Exception {
		localsTest();
	}
	
	public void test537() throws Exception {
		localsTest();
	}
	
	public void test538() throws Exception {
		localsTest();
	}
	
	public void test539() throws Exception {
		localsTest();
	}
	
	public void test540() throws Exception {
		localsTest();
	}
	
	public void test541() throws Exception {
		localsTest();
	}
	
	public void test542() throws Exception {
		localsTest();
	}
	
	public void test543() throws Exception {
		localsTest();
	}
	
	public void test550() throws Exception {
		localsTest();
	}
	
	public void test551() throws Exception {
		localsTest();
	}
	
	public void test552() throws Exception {
		localsTest();
	}
	
	public void test553() throws Exception {
		localsTest();
	}
	
	public void test554() throws Exception {
		localsTest();
	}
	
	public void test555() throws Exception {
		localsTest();
	}
	
	public void test556() throws Exception {
		localsTest();
	}
	
	public void test557() throws Exception {
		localsTest();
	}
	
	public void test558() throws Exception {
		localsTest();
	}
	
	public void test559() throws Exception {
		localsTest();
	}
	
	public void test560() throws Exception {
		localsTest();
	}
	
	public void test561() throws Exception {
		localsTest();
	}
	
	public void test562() throws Exception {
		localsTest();
	}
	
	public void test563() throws Exception {
		localsTest();
	}
	
	public void test564() throws Exception {
		localsTest();
	}
	
	public void test565() throws Exception {
		localsTest();
	}
	
	public void test566() throws Exception {
		localsTest();
	}
	
	public void test567() throws Exception {
		localsTest();
	}
	
	public void test568() throws Exception {
		localsTest();
	}
	
	public void test569() throws Exception {
		localsTest();
	}
	
	public void test570() throws Exception {
		localsTest();
	}
	
	public void test571() throws Exception {
		localsTest();
	}
	
	public void test572() throws Exception {
		localsTest();
	}
	
	public void test575() throws Exception {
		localsTest();
	}
	
	public void test576() throws Exception {
		localsTest();
	}
	
	public void test577() throws Exception {
		localsTest();
	}
	
	public void test578() throws Exception {
		localsTest();
	}
	
	//---- Test expressions
	
	public void test600() throws Exception {
		expressionTest();
	}
	
	public void test601() throws Exception {
		expressionTest();
	}
	
	public void test602() throws Exception {
		expressionTest();
	}
	
	public void test603() throws Exception {
		expressionTest();
	}
	
	public void test604() throws Exception {
		expressionTest();
	}
	
	public void test605() throws Exception {
		expressionTest();
	}
	
	public void test606() throws Exception {
		expressionTest();
	}
	
	public void test607() throws Exception {
		expressionTest();
	}
	
	public void test608() throws Exception {
		expressionTest();
	}
	
	public void test609() throws Exception {
		expressionTest();
	}
	
	public void test610() throws Exception {
		expressionTest();
	}
	
	public void test611() throws Exception {
		expressionTest();
	}
	
	public void test612() throws Exception {
		expressionTest();
	}
	
	public void test613() throws Exception {
		expressionTest();
	}
	
	public void test614() throws Exception {
		expressionTest();
	}
	
	public void test615() throws Exception {
		expressionTest();
	}
	
	public void test616() throws Exception {
		expressionTest();
	}
	
	public void test617() throws Exception {
		expressionTest();
	}
	
	public void test618() throws Exception {
		expressionTest();
	}
	
	public void test619() throws Exception {
		expressionTest();
	}
	
	public void test620() throws Exception {
		expressionTest();
	}
	
	public void test621() throws Exception {
		expressionTest();
	}
	
	public void test622() throws Exception {
		expressionTest();
	}
	
	//---- Test nested methods and constructor
	
	public void test650() throws Exception {
		nestedTest();
	}
	
	public void test651() throws Exception {
		nestedTest();
	}
	
	public void test652() throws Exception {
		nestedTest();
	}

	public void test653() throws Exception {
		nestedTest();
	}
	
	public void test654() throws Exception {
		nestedTest();
	}
	
	//---- Extracting method containing a return statement.
	
	public void test700() throws Exception {
		returnTest();
	}	
	
	public void test701() throws Exception {
		returnTest();
	}	
	
	public void test702() throws Exception {
		returnTest();
	}	
	
	public void test703() throws Exception {
		returnTest();
	}	
	
	public void test704() throws Exception {
		returnTest();
	}	
	
	public void test705() throws Exception {
		returnTest();
	}	
	
	public void test706() throws Exception {
		returnTest();
	}	
	
	public void test707() throws Exception {
		returnTest();
	}
		
	public void test708() throws Exception {
		returnTest();
	}	
	
	public void test709() throws Exception {
		returnTest();
	}	
	
	public void test710() throws Exception {
		returnTest();
	}	
	
	public void test711() throws Exception {
		returnTest();
	}	
	
	public void test712() throws Exception {
		returnTest();
	}	
	
	public void test713() throws Exception {
		returnTest();
	}	
	
	public void test714() throws Exception {
		returnTest();
	}	
	
	public void test715() throws Exception {
		returnTest();
	}	
	
	public void test716() throws Exception {
		returnTest();
	}	
	
	public void test717() throws Exception {
		returnTest();
	}	
	
	public void test718() throws Exception {
		returnTest();
	}	
	
	public void test719() throws Exception {
		returnTest();
	}
	
	public void test720() throws Exception {
		returnTest();
	}
	
	public void test721() throws Exception {
		returnTest();
	}
	
	public void test722() throws Exception {
		returnTest();
	}
	
	public void test723() throws Exception {
		returnTest();
	}
	
	public void test724() throws Exception {
		returnTest();
	}
	
	public void test725() throws Exception {
		returnTest();
	}
	
	public void test726() throws Exception {
		returnTest();
	}
	
	public void test727() throws Exception {
		returnTest();
	}
	
	public void test728() throws Exception {
		returnTest();
	}
	
	//---- Branch statements
	
	public void test750() throws Exception {
		branchTest();
	}
	
	public void test751() throws Exception {
		branchTest();
	}
	
	public void test752() throws Exception {
		branchTest();
	}
	
	public void test753() throws Exception {
		branchTest();
	}
	
	public void test754() throws Exception {
		branchTest();
	}
	
	public void test755() throws Exception {
		branchTest();
	}
	
	//---- Test for CUs with compiler errors
	
	public void test800() throws Exception {
		errorTest();
	}
	
	public void test801() throws Exception {
		errorTest();
	}
	
	public void test802() throws Exception {
		errorTest();
	}
	
	public void test803() throws Exception {
		errorTest();
	}
	
	//---- Test parameter name changes
	
	private void invalidParameterNameTest(String[] newNames) throws Exception {
		performTest(fgTestSetup.getParameterNamePackage(), "A", INVALID_SELECTION, null, newNames, null, 0);
	}
	
	private void parameterNameTest(String[] newNames, int[] newOrder) throws Exception {
		performTest(fgTestSetup.getParameterNamePackage(), "A", COMPARE_WITH_OUTPUT, "parameterName_out", newNames, newOrder, 0);
	}
	
	public void test900() throws Exception {
		invalidParameterNameTest(new String[] {"y"});
	}
	
	public void test901() throws Exception {
		invalidParameterNameTest(new String[] {null, "i"});
	}
	
	public void test902() throws Exception {
		invalidParameterNameTest(new String[] {"System"});
	}
	
	public void test903() throws Exception {
		parameterNameTest(new String[] {"xxx", "yyyy"}, null);
	}
	
	public void test904() throws Exception {
		parameterNameTest(new String[] {"xx", "zz"}, new int[] {1, 0});
	}
	
	public void test905() throws Exception {
		parameterNameTest(new String[] {"message"}, null);
	}
	
	public void test906() throws Exception {
		parameterNameTest(new String[] {"xxx"}, null);
	}
	
	//---- Test duplicate code snippets ----------------------------------------
	
	public void test950() throws Exception {
		duplicatesTest();
	}
	
	public void test951() throws Exception {
		duplicatesTest();
	}
	
	public void test952() throws Exception {
		duplicatesTest();
	}
	
	public void test953() throws Exception {
		duplicatesTest();
	}
	
	public void test954() throws Exception {
		duplicatesTest();
	}
	
	public void test955() throws Exception {
		duplicatesTest();
	}
	
	public void test956() throws Exception {
		duplicatesTest();
	}
	
	public void test957() throws Exception {
		duplicatesTest();
	}
	
	public void test958() throws Exception {
		duplicatesTest();
	}
	
	public void test959() throws Exception {
		duplicatesTest();
	}
	
	public void test960() throws Exception {
		duplicatesTest();
	}
	
	public void test961() throws Exception {
		duplicatesTest();
	}
	
	public void test962() throws Exception {
		duplicatesTest();
	}
	
	public void test963() throws Exception {
		duplicatesTest();
	}
	
	public void test964() throws Exception {
		duplicatesTest();
	}
	
	public void test965() throws Exception {
		duplicatesTest();
	}
	
	public void test966() throws Exception {
		duplicatesTest();
	}
	
	public void test967() throws Exception {
		duplicatesTest();
	}
	
	public void test968() throws Exception {
		duplicatesTest();
	}
	
	public void test969() throws Exception {
		duplicatesTest();
	}
	
	public void test970() throws Exception {
		duplicatesTest(); // for https://bugs.eclipse.org/bugs/show_bug.cgi?id=186061
	}
	
	//---- Test code in initializers -----------------------------------------------
	
	public void test1000() throws Exception {
		initializerTest();
	}
	
	public void test1001() throws Exception {
		initializerTest();
	}
	
	public void test1002() throws Exception {
		initializerTest();
	}
	
	public void test1003() throws Exception {
		initializerTest();
	}
	
	//---- Test destination -----------------------------------------------
	
	public void test1050() throws Exception {
		destinationTest(1);
	}
	
	public void test1051() throws Exception {
		destinationTest(1);
	}
	
	//---- Test Generics --------------------------------------------------
	
	public void test1100() throws Exception {
		genericTest();
	}
	
	public void test1101() throws Exception {
		genericTest();
	}
	
	public void test1102() throws Exception {
		genericTest();
	}
	
	public void test1103() throws Exception {
		genericTest();
	}
	
	public void test1104() throws Exception {
		genericTest();
	}
	
	public void test1105() throws Exception {
		genericTest();
	}
	
	public void test1106() throws Exception {
		genericTest();
	}
	
	public void test1107() throws Exception {
		genericTest();
	}
	
	public void test1108() throws Exception {
		genericTest();
	}
	
	public void test1109() throws Exception {
		genericTest();
	}
	
	public void test1110() throws Exception {
		genericTest();
	}
	
	public void test1111() throws Exception {
		genericTest();
	}
	
	public void test1112() throws Exception {
		genericTest();
	}
	
	public void test1113() throws Exception {
		genericTest();
	}
	
	public void test1114() throws Exception {
		genericTest();
	}
	
	public void test1115() throws Exception {
		genericTest();
	}
	
	public void test1116() throws Exception {
		genericTest();
	}
	
	public void test1117() throws Exception {
		genericTest();
	}
	
	public void test1118() throws Exception {
		genericTest();
	}
	
	public void test1119() throws Exception {
		genericTest();
	}
	
	//---- Test enums ---------------------------------
	
	public void test1150() throws Exception {
		enumTest();
	}
	
	public void test1151() throws Exception {
		enumTest();
	}
	
	public void test1152() throws Exception {
		enumTest();
	}
	
	//---- Test varargs ---------------------------------
	
	protected void varargsTest() throws Exception {
		performTest(fgTestSetup.getVarargsPackage(), "A", COMPARE_WITH_OUTPUT, "varargs_out");
	}
	
	public void test1200() throws Exception {
		varargsTest();
	}
	
	public void test1201() throws Exception {
		varargsTest();
	}
	
	public void test1202() throws Exception {
		varargsTest();
	}
	
	//---- Test copied from http://c2.com/cgi/wiki?RefactoringBenchmarksForExtractMethod
	
	public void test2001() throws Exception {
		wikiTest();
	}	
	
	public void test2002() throws Exception {
		wikiTest();
	}	
	
	public void test2003() throws Exception {
		wikiTest();
	}	
	
	public void test2004() throws Exception {
		wikiTest();
	}	
	
	public void test2005() throws Exception {
		wikiTest();
	}	
}
