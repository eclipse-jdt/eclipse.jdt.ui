/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;

public class InlineMethodTests extends AbstractSelectionTestCase {

	private static InlineMethodTestSetup fgTestSetup;
	
	public InlineMethodTests(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new InlineMethodTestSetup(new TestSuite(InlineMethodTests.class));
		return fgTestSetup;
	}
	
	protected String getResourceLocation() {
		return "InlineMethodWorkspace/TestCases/";
	}
	
	protected String adaptName(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1) + ".java";
	}
	
	protected void performTest(IPackageFragment packageFragment, String id, int mode, String outputFolder) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		String source= unit.getSource();
		int[] selection= getSelection(source);
		ASTNode node= InlineMethodRefactoring.getSelectedNode(unit, Selection.createFromStartLength(selection[0], selection[1]));
		InlineMethodRefactoring refactoring= new InlineMethodRefactoring(unit, (MethodInvocation)node);
		refactoring.setSaveChanges(true);
		String out= null;
		switch (mode) {
			case COMPARE_WITH_OUTPUT:
				out= getProofedContent(outputFolder, id);
				break;		
		}
		performTest(unit, refactoring, mode, out);
	}

	/************************ Invalid Tests ********************************/
		
	protected void performInvalidTest() throws Exception {
		performTest(fgTestSetup.getInvalidPackage(), getName(), INVALID_SELECTION, null);
	}
	
	public void testRecursion() throws Exception {
		performInvalidTest();
	}
	
	public void testFieldInitializer() throws Exception {
		performInvalidTest();
	}
	
	/************************ Simple Tests ********************************/
		
	private void performSimpleTest() throws Exception {
		performTest(fgTestSetup.getSimplePackage(), getName(), COMPARE_WITH_OUTPUT, "simple_out");
	}
	
	public void testBasic1() throws Exception {
		performSimpleTest();
	}	

	public void testBasic2() throws Exception {
		performSimpleTest();
	}	
	
	/************************ Argument Tests ********************************/
		
	private void performArgumentTest() throws Exception {
		performTest(fgTestSetup.getArgumentPackage(), getName(), COMPARE_WITH_OUTPUT, "argument_out");
	}
	
	public void testLocalReference() throws Exception {
		performArgumentTest();
	}	
	
	public void testLiteralReferenceRead() throws Exception {
		performArgumentTest();
	}	
	
	public void testLiteralReferenceWrite() throws Exception {
		performArgumentTest();
	}	
	
	public void testParameterNameUsed1() throws Exception {
		performArgumentTest();
	}	
	
	public void testParameterNameUsed2() throws Exception {
		performArgumentTest();
	}	
	
	public void testParameterNameUsed3() throws Exception {
		performArgumentTest();
	}	
	
	public void testParameterNameUsed4() throws Exception {
		performArgumentTest();
	}	
	
	public void testParameterNameUnused1() throws Exception {
		performArgumentTest();
	}	
	
	public void testParameterNameUnused2() throws Exception {
		performArgumentTest();
	}	
	
	public void testParameterNameUnused3() throws Exception {
		performArgumentTest();
	}
	
	/************************ Name Conflict Tests ********************************/
		
	private void performNameConflictTest() throws Exception {
		performTest(fgTestSetup.getNameConflictPackage(), getName(), COMPARE_WITH_OUTPUT, "nameconflict_out");
	}
	
	public void testSameLocal() throws Exception {
		performNameConflictTest();
	}
	
	public void testSameType() throws Exception {
		performNameConflictTest();
	}
	
	public void testSameTypeAfter() throws Exception {
		performNameConflictTest();
	}
	
	public void testSameTypeInSibling() throws Exception {
		performNameConflictTest();
	}
	
	public void testLocalInType() throws Exception {
		performNameConflictTest();
	}
	
	public void testFieldInType() throws Exception {
		performNameConflictTest();
	}
}
