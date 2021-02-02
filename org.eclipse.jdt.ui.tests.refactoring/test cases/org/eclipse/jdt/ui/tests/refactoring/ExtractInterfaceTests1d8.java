/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceProcessor;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d8Setup;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ExtractInterfaceTests1d8 extends ExtractInterfaceTests {
	private static final String REFACTORING_PATH= "ExtractInterface18/";

	public ExtractInterfaceTests1d8() {
		super(new Java1d8Setup());
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	@Test
	public void testExtractInterfaceFromInterface1() throws Exception {
		validatePassingTest("A", "B", true, true);
	}

	@Test
	public void testExtractInterfaceFromInterface2() throws Exception {
		String className= "A";
		String extendingInterfaceName= "I1";
		String newInterfaceName= "B";

		IType clas= getType(createCUfromTestFile(getPackageP(), getTopLevelTypeName(className)), className);
		ICompilationUnit cu= clas.getCompilationUnit();
		IPackageFragment pack= (IPackageFragment)cu.getParent();

		getType(createCUfromTestFile(getPackageP(), getTopLevelTypeName(extendingInterfaceName)), extendingInterfaceName);

		IPackageFragmentRoot root= rts.getDefaultSourceFolder();
		assertNotNull(root);
		IPackageFragment p2= root.createPackageFragment("p2", true, null);
		getType(createCUfromTestFile(p2, getTopLevelTypeName("I2")), "I2");

		ExtractInterfaceProcessor processor= new ExtractInterfaceProcessor(clas, JavaPreferencesSettings.getCodeGenerationSettings(clas.getJavaProject()));
		Refactoring ref= new ProcessorBasedRefactoring(processor);

		processor.setTypeName(newInterfaceName);
		assertEquals("interface name should be accepted", RefactoringStatus.OK, processor.checkTypeName(newInterfaceName).getSeverity());

		IMember[] extractableMembers= processor.getExtractableMembers();
		final IMember[] members= new IMember[extractableMembers.length - 1];
		List<IMember> list= new ArrayList<>();
		for (IMember iMember : extractableMembers) {
			if (!(iMember instanceof IField)) {
				list.add(iMember);
			}
		}
		processor.setExtractedMembers(list.toArray(members));
		processor.setReplace(true);
		processor.setAnnotations(false);
		RefactoringStatus performRefactoring= performRefactoring(ref);
		assertNull("was supposed to pass", performRefactoring);
		assertEqualLines("incorrect changes in " + className,
				getFileContents(getOutputTestFileName(className)),
				cu.getSource());

		ICompilationUnit interfaceCu= pack.getCompilationUnit(newInterfaceName + ".java");
		assertEqualLines("incorrect interface created",
				getFileContents(getOutputTestFileName(newInterfaceName)),
				interfaceCu.getSource());

	}

	@Test
	public void testExtractInterfaceFromClass() throws Exception {
		validatePassingTest("A", "B", true, true);
	}

	// bug 394551
	@Test
	public void testExtractInterfaceFromClass2() throws Exception {
		fGenerateAnnotations= true;
		String[] names= new String[] { "m" };
		String[][] signatures= new String[][] { new String[0], new String[0] };
		validatePassingTest("A", new String[] { "A" }, "I", true, names, signatures, null);
	}

	@Test
	public void testExtractInterfaceFromAbstractClass() throws Exception {
		validatePassingTest("A", "B", true, true);
	}

	@Test
	public void testLambda1() throws Exception {
		// bug 488420
		String[] names= new String[] { "m1" };
		String[][] signatures= new String[][] { new String[0] };
		validatePassingTest("X", new String[] { "X", "Util" }, "I", true, names, signatures, null);
	}

	@Test
	public void testLambda2() throws Exception {
		// bug 488420
		String[] names= new String[] { "m1" };
		String[][] signatures= new String[][] { new String[0] };
		validatePassingTest("X", new String[] { "X", "Util" }, "I", true, names, signatures, null);
	}

	@Test
	public void testMethodRef1() throws Exception {
		// bug 489170
		String[] names= new String[] { "methodN" };
		String[][] signatures= new String[][] { new String[0] };
		validatePassingTest("X", new String[] { "X", "Util" }, "I", true, names, signatures, null);
	}

	@Test
	public void testMethodRef2() throws Exception {
		// bug 489170
		String[] names= new String[] { "m1" };
		String[][] signatures= new String[][] { new String[0] };
		validatePassingTest("X", new String[] { "X", "Util" }, "I", true, names, signatures, null);
	}

	@Test
	public void testMethodRef3() throws Exception {
		// bug 489170
		String[] names= new String[] { "m1" };
		String[][] signatures= new String[][] { new String[0] };
		validatePassingTest("X", new String[] { "X", "Util" }, "I", true, names, signatures, null);
	}
}
