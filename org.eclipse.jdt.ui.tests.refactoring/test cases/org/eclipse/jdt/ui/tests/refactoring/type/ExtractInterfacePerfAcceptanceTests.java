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
package org.eclipse.jdt.ui.tests.refactoring.type;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.jdt.ui.tests.performance.SWTTestProject;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestSetup;

public class ExtractInterfacePerfAcceptanceTests extends RefactoringPerformanceTestCase {
	
	private SWTTestProject fProject;
	private ExtractInterfaceRefactoring fRefactoring;
	
	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("ExtractInterfacePerfAcceptanceTests");
		suite.addTest(new ExtractInterfacePerfAcceptanceTests("testExtractControl"));
        return new RefactoringPerformanceTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringPerformanceTestSetup(someTest);
	}

	public ExtractInterfacePerfAcceptanceTests(String test) {
		super(test);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		fProject= new SWTTestProject();
		IType control= fProject.getProject().findType("org.eclipse.swt.widgets.Control");
		fRefactoring= ExtractInterfaceRefactoring.create(control, JavaPreferencesSettings.getCodeGenerationSettings());
		IMethod[] methods= control.getMethods();
		List extractedMembers= new ArrayList();
		for (int i= 0; i < methods.length; i++) {
			IMethod method= methods[i];
			int flags= method.getFlags();
			if (Flags.isPublic(flags) && !Flags.isStatic(flags) && !method.isConstructor()) {
				extractedMembers.add(method);
			}
		}
		fRefactoring.setNewInterfaceName("IControl");
		fRefactoring.setExtractedMembers((IMember[])extractedMembers.toArray(new IMember[extractedMembers.size()]));
		fRefactoring.setReplaceOccurrences(true);
	}
	
	protected void tearDown() throws Exception {
		fProject.delete();
		super.tearDown();
	}
	
	public void testExtractControl() throws Exception {
		executeRefactoring(fRefactoring, true);
	}
}
