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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;

public class ExtractMethodTests extends AbstractSelectionTestCase {

	private String[] fNewNames;
	private int[] fNewOrder;

	private static ExtractMethodTestSetup fgTestSetup;
	
	public ExtractMethodTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		fgTestSetup= new ExtractMethodTestSetup(new TestSuite(ExtractMethodTests.class));
		return fgTestSetup;
	}

	protected IPackageFragmentRoot getRoot() {
		return fgTestSetup.getRoot();
	}
	
	protected String getResourceLocation() {
		return "ExtractMethodWorkSpace/ExtractMethodTests/";
	}
	
	protected String adaptName(String name) {
		return name + "_" + getName() + ".java";
	}	
	
	protected void selectionTest(int startLine, int startColumn, int endLine, int endColumn) throws Exception{
		ICompilationUnit unit= createCU(getSelectionPackage(), "A");
		String source= unit.getSource();
		int[] selection= getSelection(source);
		ISourceRange expected= TextRangeUtil.getSelection(unit, startLine, startColumn, endLine, endColumn);
		assertEquals(expected.getOffset(), selection[0]);
		assertEquals(expected.getLength(), selection[1]);
	}
	
	private IPackageFragment getSelectionPackage() throws JavaModelException {
		return fgTestSetup.getSelectionPackage();
 	}
	
	protected void performTest(IPackageFragment packageFragment, String id, int mode, String outputFolder) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		String source= unit.getSource();
		int[] selection= getSelection(source);
		ExtractMethodRefactoring refactoring= ExtractMethodRefactoring.create(
			unit, selection[0], selection[1],
			JavaPreferencesSettings.getCodeGenerationSettings());
		refactoring.setMethodName("extracted");
		refactoring.setVisibility(Modifier.PROTECTED);
		String out= null;
		switch (mode) {
			case COMPARE_WITH_OUTPUT:
				out= getProofedContent(outputFolder, id);
				break;		
		}
		performTest(unit, refactoring, mode, out);
	}	
	
	protected RefactoringStatus checkPreconditions(Refactoring refactoring, IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= refactoring.checkActivation(pm);
		if (result.hasFatalError())
			return result;
		ExtractMethodRefactoring extract= (ExtractMethodRefactoring)refactoring;
		List parameters= extract.getParameterInfos();
		if (fNewNames != null && fNewNames.length > 0) {
			for (int i= 0; i < fNewNames.length; i++) {
				if (fNewNames[i] != null)
					((ParameterInfo)parameters.get(i)).setNewName(fNewNames[i]);
			}
		}
		if (fNewOrder != null && fNewOrder.length > 0) {
			assertTrue(fNewOrder.length == parameters.size());
			List current= new ArrayList(parameters);
			for (int i= 0; i < fNewOrder.length; i++) {
				parameters.set(fNewOrder[i], current.get(i));
			}
		}
		result.merge(refactoring.checkInput(pm));
		return result;
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
	
	//---- Test parameter name changes
	
	private void invalidParameterNameTest(String[] newNames) throws Exception {
		fNewNames= newNames;
		fNewOrder= null;
		performTest(fgTestSetup.getParameterNamePackage(), "A", INVALID_SELECTION, null);
	}
	
	private void parameterNameTest(String[] newNames, int[] newOrder) throws Exception {
		fNewNames= newNames;
		fNewOrder= newOrder;
		performTest(fgTestSetup.getParameterNamePackage(), "A", COMPARE_WITH_OUTPUT, "parameterName_out");
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
