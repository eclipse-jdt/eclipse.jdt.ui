/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	private static final Class clazz= ExtractInterfaceTests18.class;

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
		List<IMember> list= new ArrayList<IMember>();
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

	public void testExtractInterfaceFromAbstractClass() throws Exception {
		validatePassingTest("A", "B", true, true);
	}
}
