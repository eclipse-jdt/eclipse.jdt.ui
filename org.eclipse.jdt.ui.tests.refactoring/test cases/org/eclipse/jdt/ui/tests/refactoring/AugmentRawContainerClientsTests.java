/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.corext.refactoring.generics.AugmentRawContainerClientsRefactoring;

//TODO: add Test to AllTests
public class AugmentRawContainerClientsTests extends RefactoringTest {

	private static final Class clazz= AugmentRawContainerClientsTests.class;
	private static final String REFACTORING_PATH= "AugmentRawContainerTypes/";
	
	public static Test suite() {
		return new Java15Setup(new TestSuite(clazz));
	}
	
	public static Test setUpTest(Test someTest) {
		return new Java15Setup(someTest);
	}
	
	public AugmentRawContainerClientsTests(String name) {
		super(name);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	private void performCu(int expectedInitialStatus, int expectedFinalStatus) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IJavaElement[] elements= { cu };
		AugmentRawContainerClientsRefactoring refactoring= AugmentRawContainerClientsRefactoring.create(elements);
		
		NullProgressMonitor pm= new NullProgressMonitor();
		RefactoringStatus status= refactoring.checkInitialConditions(pm);
		assertEquals("wrong initial condition status", expectedInitialStatus, status.getSeverity());
		if (! status.isOK())
			return;
		
		// set client options here (from instance variables)

		status.merge(refactoring.checkFinalConditions(pm));
		assertEquals("wrong final condition status", expectedFinalStatus, status.getSeverity());
		if (status.getSeverity() == RefactoringStatus.FATAL)
			return;
		
		String expected= getFileContents(getOutputTestFileName("A"));
		String actual= cu.getSource();
		assertEqualLines(expected, actual);
	}
	
	private void performCuOK() throws Exception {
		performCu(RefactoringStatus.OK, RefactoringStatus.OK);
	}
	
	public void testCu1() throws Exception {
		performCuOK();
	}
	
}
