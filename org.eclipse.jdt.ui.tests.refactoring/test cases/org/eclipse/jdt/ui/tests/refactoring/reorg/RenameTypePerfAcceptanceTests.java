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
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;

import org.eclipse.jdt.ui.tests.performance.SWTTestProject;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestSetup;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameTypePerfAcceptanceTests extends RefactoringPerformanceTestCase {
	
	private SWTTestProject fTestProject;
	
	public static Test suite() {
		return new RefactoringPerformanceTestSetup(new TestSuite(RenameTypePerfAcceptanceTests.class));
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringPerformanceTestSetup(someTest);
	}

	protected void setUp() throws Exception {
		super.setUp();
		fTestProject= new SWTTestProject();
	}
	
	protected void tearDown() throws Exception {
		fTestProject.delete();
		super.tearDown();
	}
	
	public void testRenameType() throws Exception {
		IType control= fTestProject.getProject().findType("org.eclipse.swt.widgets.Control");
		RenameTypeProcessor processor= new RenameTypeProcessor(control);
		processor.setNewElementName("Control2");
		executeRefactoring(new RenameRefactoring(processor), "cold");
		
		control= fTestProject.getProject().findType("org.eclipse.swt.widgets.Control2");
		processor= new RenameTypeProcessor(control);
		processor.setNewElementName("Control");
		executeRefactoring(new RenameRefactoring(processor), "warm");
	}
}
