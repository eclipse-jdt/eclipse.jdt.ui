/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.jdt.ui.tests.performance.SWTTestProject;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringHeapTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestSetup;

public class ExtractInterfaceHeapAcceptanceTests extends RefactoringHeapTestCase {
	
	private SWTTestProject fProject;
	private ExtractInterfaceRefactoring fRefactoring;
	
	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("ExtractInterfaceHeapAcceptanceTests");
		suite.addTest(new ExtractInterfaceHeapAcceptanceTests("testExtractControl"));
        return new RefactoringPerformanceTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringPerformanceTestSetup(someTest);
	}

	public ExtractInterfaceHeapAcceptanceTests(String test) {
		super(test);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		fProject= new SWTTestProject();
		IType control= fProject.getProject().findType("org.eclipse.swt.widgets.Control");
		fRefactoring= new ExtractInterfaceRefactoring(new ExtractInterfaceProcessor(control, JavaPreferencesSettings.getCodeGenerationSettings(fProject.getProject())));
		IMethod[] methods= control.getMethods();
		List extractedMembers= new ArrayList();
		for (int i= 0; i < methods.length; i++) {
			IMethod method= methods[i];
			int flags= method.getFlags();
			if (Flags.isPublic(flags) && !Flags.isStatic(flags) && !method.isConstructor()) {
				extractedMembers.add(method);
			}
		}
		ExtractInterfaceProcessor processor= fRefactoring.getExtractInterfaceProcessor();
		processor.setTypeName("IControl");
		processor.setExtractedMembers((IMember[])extractedMembers.toArray(new IMember[extractedMembers.size()]));
		processor.setReplace(true);
	}
	
	protected void tearDown() throws Exception {
		fProject.delete();
		super.tearDown();
	}
	
	public void testExtractControl() throws Exception {
		executeRefactoring(fRefactoring, true);
	}
}
