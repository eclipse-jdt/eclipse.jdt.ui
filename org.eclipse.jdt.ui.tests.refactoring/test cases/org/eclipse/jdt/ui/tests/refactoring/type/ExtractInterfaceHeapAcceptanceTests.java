/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.type;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceProcessor;

import org.eclipse.jdt.ui.tests.performance.SWTTestProject;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringHeapTestCase;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringPerformanceTestSetup;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class ExtractInterfaceHeapAcceptanceTests extends RefactoringHeapTestCase {

	private SWTTestProject fProject;
	private Refactoring fRefactoring;

	@Rule
	public RefactoringPerformanceTestSetup rpts= new RefactoringPerformanceTestSetup();

	@Override
	public void setUp() throws Exception {
		super.setUp();
		fProject= new SWTTestProject();
		IType control= fProject.getProject().findType("org.eclipse.swt.widgets.Control");

		ExtractInterfaceProcessor processor= new ExtractInterfaceProcessor(control, JavaPreferencesSettings.getCodeGenerationSettings(fProject.getProject()));
		fRefactoring= new ProcessorBasedRefactoring(processor);

		List<IMethod> extractedMembers= new ArrayList<>();
		for (IMethod method : control.getMethods()) {
			int flags= method.getFlags();
			if (Flags.isPublic(flags) && !Flags.isStatic(flags) && !method.isConstructor()) {
				extractedMembers.add(method);
			}
		}
		processor.setTypeName("IControl");
		processor.setExtractedMembers(extractedMembers.toArray(new IMember[extractedMembers.size()]));
		processor.setReplace(true);
	}

	@Override
	public void tearDown() throws Exception {
		fProject.delete();
		super.tearDown();
	}

	@Test
	public void testExtractControl() throws Exception {
		executeRefactoring(fRefactoring, true);
	}
}
