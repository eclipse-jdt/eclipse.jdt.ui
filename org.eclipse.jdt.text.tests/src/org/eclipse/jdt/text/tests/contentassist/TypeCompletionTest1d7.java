/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.contentassist;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * Those tests should run on Java Dolphin 1.7 .
 *
 * @since 3.7
 */
public class TypeCompletionTest1d7 extends TypeCompletionTest {
	@Rule
	public Java1d7CompletionTestSetup cts1d7= new Java1d7CompletionTestSetup();

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		getJDTUIPrefs().setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, true);
	}

	@Override
	protected IPackageFragment getAnonymousTestPackage() throws CoreException {
		return cts1d7.getAnonymousTestPackage();
	}

	@Override
	@Test
	public void testGenericParameterGuessingUnambiguos() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<String> list= new A|", "ArrayList()", "List<String> list= new ArrayList<>()|");
	}

	@Override
	@Test
	public void testGenericParameterGuessingExtends() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<? extends Number> list= new A|", "ArrayList()", "List<? extends Number> list= new ArrayList<>()|");
	}

	@Override
	@Test
	public void testGenericParameterGuessingSuper() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<? super Number> list= new A|", "ArrayList()", "List<? super Number> list= new ArrayList<>()|");
	}

	@Override
	@Test
	public void testGenericParameterGuessingMixed() throws Exception {
		addImport("java.util.Map");
		expectImport("java.util.HashMap");
		expectImport("java.util.Map");
		assertMethodBodyProposal("Map<String, ? extends Number> list= new H|", "HashMap()", "Map<String, ? extends Number> list= new HashMap<>()|");
	}

	@Override
	@Test
	public void testBug182468() throws Exception {
		IPackageFragmentRoot src= (IPackageFragmentRoot)cts1d7.getTestPackage().getParent();

		IPackageFragment package1= src.createPackageFragment("package1", true, null);
		package1.createCompilationUnit("AClass.java", "package " + package1.getElementName() + "; public class AClass {}", true, null);

		IPackageFragment package2= src.createPackageFragment("package2", true, null);
		package1.createCompilationUnit("AClass.java", "package " + package2.getElementName() + "; public class AClass {}", true, null);

		waitBeforeCompleting(true);

		addImport(package1.getElementName() + ".AClass");
		expectImport(package1.getElementName() + ".AClass");
		assertMethodBodyProposal("new AClass|", "AClass() - " + package2.getElementName(), "new " + package2.getElementName() + ".AClass()");
	}
}
