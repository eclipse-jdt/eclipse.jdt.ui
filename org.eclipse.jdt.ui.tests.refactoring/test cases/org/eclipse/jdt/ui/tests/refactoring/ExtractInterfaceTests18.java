/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

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

import org.eclipse.jdt.ui.tests.core.NoSuperTestsSuite;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class ExtractInterfaceTests18 extends ExtractInterfaceTests {

	private static final Class<ExtractInterfaceTests18> clazz= ExtractInterfaceTests18.class;

	private static final String REFACTORING_PATH= "ExtractInterface18/";

	public ExtractInterfaceTests18(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java18Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java18Setup(someTest);
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	public void testExtractInterfaceFromInterface1() throws Exception {
		validatePassingTest("A", "B", true, true);
	}

	public void testExtractInterfaceFromInterface2() throws Exception {
		String className= "A";
		String extendingInterfaceName= "I1";
		String newInterfaceName= "B";
	
		IType clas= getType(createCUfromTestFile(getPackageP(), getTopLevelTypeName(className)), className);
		ICompilationUnit cu= clas.getCompilationUnit();
		IPackageFragment pack= (IPackageFragment)cu.getParent();

		getType(createCUfromTestFile(getPackageP(), getTopLevelTypeName(extendingInterfaceName)), extendingInterfaceName);

		IPackageFragmentRoot root= RefactoringTestSetup.getDefaultSourceFolder();
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
		assertEquals("was supposed to pass", null, performRefactoring);
		assertEqualLines("incorrect changes in " + className,
				getFileContents(getOutputTestFileName(className)),
				cu.getSource());

		ICompilationUnit interfaceCu= pack.getCompilationUnit(newInterfaceName + ".java");
		assertEqualLines("incorrect interface created",
				getFileContents(getOutputTestFileName(newInterfaceName)),
				interfaceCu.getSource());

	}

	public void testExtractInterfaceFromClass() throws Exception {
		validatePassingTest("A", "B", true, true);
	}

	// bug 394551
	public void testExtractInterfaceFromClass2() throws Exception {
		fGenerateAnnotations= true;
		String[] names= new String[] { "m" };
		String[][] signatures= new String[][] { new String[0], new String[0] };
		validatePassingTest("A", new String[] { "A" }, "I", true, names, signatures, null);
	}

	public void testExtractInterfaceFromAbstractClass() throws Exception {
		validatePassingTest("A", "B", true, true);
	}

	public void testLambda1() throws Exception {
		// bug 488420 
		String[] names= new String[] { "m1" };
		String[][] signatures= new String[][] { new String[0] };
		validatePassingTest("X", new String[] { "X", "Util" }, "I", true, names, signatures, null);
	}

	public void testLambda2() throws Exception {
		// bug 488420 
		String[] names= new String[] { "m1" };
		String[][] signatures= new String[][] { new String[0] };
		validatePassingTest("X", new String[] { "X", "Util" }, "I", true, names, signatures, null);
	}

	public void testMethodRef1() throws Exception {
		// bug 489170
		String[] names= new String[] { "methodN" };
		String[][] signatures= new String[][] { new String[0] };
		validatePassingTest("X", new String[] { "X", "Util" }, "I", true, names, signatures, null);
	}

	public void testMethodRef2() throws Exception {
		// bug 489170
		String[] names= new String[] { "m1" };
		String[][] signatures= new String[][] { new String[0] };
		validatePassingTest("X", new String[] { "X", "Util" }, "I", true, names, signatures, null);
	}

	public void testMethodRef3() throws Exception {
		// bug 489170
		String[] names= new String[] { "m1" };
		String[][] signatures= new String[][] { new String[0] };
		validatePassingTest("X", new String[] { "X", "Util" }, "I", true, names, signatures, null);
	}
}
