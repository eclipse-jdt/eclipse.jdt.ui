/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
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

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;

public class ExtractMethodTests17 extends AbstractSelectionTestCase {
	private static ExtractMethodTestSetup17 fgTestSetup;

	public ExtractMethodTests17(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new ExtractMethodTestSetup17(new TestSuite(ExtractMethodTests17.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test test) {
		fgTestSetup= new ExtractMethodTestSetup17(test);
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

	protected void tryTest() throws Exception {
		performTest(fgTestSetup.getTryPackage(), "A", COMPARE_WITH_OUTPUT, "try17_out");
	}

	//====================================================================================
	// Testing Extracted result
	//====================================================================================

	//---- Test Try / catch block

	public void test1() throws Exception {
		tryTest();
	}

	public void test2() throws Exception {
		tryTest();
	}

	public void test3() throws Exception {
		tryTest();
	}

	public void test4() throws Exception {
		tryTest();
	}

	public void test5() throws Exception {
		tryTest();
	}

	public void test6() throws Exception {
		tryTest();
	}
}
