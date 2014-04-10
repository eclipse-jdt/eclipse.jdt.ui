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

import junit.framework.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;

import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;

public class PullUpTests18 extends PullUpTests {

	private static final Class clazz= PullUpTests18.class;

	public PullUpTests18(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java18Setup(someTest);
	}

	public void test18_1() throws Exception {

		String[] methodNames= new String[] { "getArea" };
		String[][] signatures= new String[][] { new String[] { "QInteger;" } };
		JavaProjectHelper.addLibrary((IJavaProject)getPackageP().getAncestor(IJavaElement.JAVA_PROJECT), new Path(Java18ProjectTestSetup.getJdtAnnotations20Path()));

		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");


		IType type= getType(cuB, "B");
		IMethod[] methods= getMethods(type, methodNames, signatures);

		PullUpRefactoringProcessor processor= createRefactoringProcessor(methods);
		Refactoring ref= processor.getRefactoring();

		assertTrue("activation", ref.checkInitialConditions(new NullProgressMonitor()).isOK());
		setSuperclassAsTargetClass(processor);

		RefactoringStatus result= performRefactoring(ref);
		assertTrue("precondition was supposed to pass", result == null || !result.hasError());

		assertEqualLines("A", getFileContents(getOutputTestFileName("A")), cuA.getSource());
		assertEqualLines("B", getFileContents(getOutputTestFileName("B")), cuB.getSource());

	}
}
