/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;


/**
 * @since 3.7
 */
public class TypeCompletionTest17 extends TypeCompletionTest {

	private static final Class THIS= TypeCompletionTest17.class;

	public static Test setUpTest(Test test) {
		return new Java17CompletionTestSetup(test);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS, suiteName(THIS)));
	}

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		getJDTUIPrefs().setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, true);
	}

	protected IPackageFragment getAnonymousTestPackage() throws CoreException {
		return Java17CompletionTestSetup.getAnonymousTestPackage();
	}

	public void testGenericParameterGuessingUnambiguos() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<String> list= new A|", "ArrayList()", "List<String> list= new ArrayList<>()|");
	}

	public void testGenericParameterGuessingExtends() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<? extends Number> list= new A|", "ArrayList()", "List<? extends Number> list= new ArrayList<>()|");
	}

	public void testGenericParameterGuessingSuper() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<? super Number> list= new A|", "ArrayList()", "List<? super Number> list= new ArrayList<>()|");
	}

	public void testGenericParameterGuessingMixed() throws Exception {
		addImport("java.util.Map");
		expectImport("java.util.HashMap");
		expectImport("java.util.Map");
		assertMethodBodyProposal("Map<String, ? extends Number> list= new H|", "HashMap()", "Map<String, ? extends Number> list= new HashMap<>()|");
	}

	public void testBug182468() throws Exception {
		IPackageFragmentRoot src= (IPackageFragmentRoot)Java17CompletionTestSetup.getTestPackage().getParent();

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
