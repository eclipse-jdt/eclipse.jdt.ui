/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.text.TextSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class SurroundWithTests extends AbstractSelectionTestCase {

	private static SurroundWithTestSetup fgTestSetup;
	
	public SurroundWithTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		fgTestSetup= new SurroundWithTestSetup(new TestSuite(SurroundWithTests.class));
		return fgTestSetup;
	}

	protected IPackageFragmentRoot getRoot() {
		return fgTestSetup.getRoot();
	}
	
	protected String getResourceLocation() {
		return "SurroundWithWorkSpace/SurroundWithTests/";
	}
	
	protected String adaptName(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1) + ".java";
	}	

	protected void performTest(IPackageFragment packageFragment, String name, String outputFolder, int mode) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, name);
		SurroundWithTryCatchRefactoring refactoring= new SurroundWithTryCatchRefactoring(unit, 
			getTextSelection(unit.getSource()), 4, JavaPreferencesSettings.getCodeGenerationSettings());
		refactoring.setSaveChanges(true);
		String out= null;
		if (mode == COMPARE_WITH_OUTPUT)
			out= getProofedContent(outputFolder, name);
		performTest(unit, refactoring, mode, out);
	}
	
	protected void tryCatchInvalidTest() throws Exception {
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "trycatch_out", INVALID_SELECTION);
	}	
	
	protected void tryCatchTest() throws Exception {
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "trycatch_out", COMPARE_WITH_OUTPUT);
	}	
	
	public void testNoException() throws Exception {
		tryCatchInvalidTest();
	}
	
	public void testAlreadyCaught() throws Exception {
		tryCatchInvalidTest();
	}
	
	public void testInvalidParent1() throws Exception {
		tryCatchInvalidTest();
	}
	
	public void testInvalidParent2() throws Exception {
		tryCatchInvalidTest();
	}
	
	public void testThisConstructorCall() throws Exception {
		tryCatchInvalidTest();
	}
	
	public void testSuperConstructorCall() throws Exception {
		tryCatchInvalidTest();
	}
	
	public void testSimple() throws Exception {
		tryCatchTest();
	}
	
	public void testOneLine() throws Exception {
		tryCatchTest();
	}	
	
	public void testMultiLine() throws Exception {
		tryCatchTest();
	}
	
	public void testExceptionOrder()	throws Exception {
		tryCatchTest();
	}
	
	public void testWrappedLocal1() throws Exception {
		tryCatchTest();
	}
	
	public void testWrappedLocal2() throws Exception {
		tryCatchTest();
	}
	
	public void testWrappedLocal3() throws Exception {
		tryCatchTest();
	}
	
	public void testThrowInCatch() throws Exception {
		tryCatchTest();
	}
}