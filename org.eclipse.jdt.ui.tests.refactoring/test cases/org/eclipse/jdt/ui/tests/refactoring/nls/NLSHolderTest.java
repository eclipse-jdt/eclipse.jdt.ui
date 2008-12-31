/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.nls;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHint;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSubstitution;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class NLSHolderTest extends TestCase {

	private IJavaProject javaProject;

	private IPackageFragmentRoot fSourceFolder;

	private final static String ACCESSOR_KLAZZ= "package test;\n" + "public class TestMessages {\n" + "	private static final String BUNDLE_NAME = \"test.test\";//$NON-NLS-1$\n" + "	public static String getString(String s) {" + "		return \"\";\n" + "	}\n" + "}\n";

	public NLSHolderTest(String arg) {
		super(arg);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(NLSHolderTest.class));
	}

	public static Test suite() {
		return allTests();
	}

	protected void setUp() throws Exception {
		javaProject= ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(javaProject, "src");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(javaProject, ProjectTestSetup.getDefaultClasspath());
	}

	public void testSubstitutionWithAccessor() throws Exception {
		String klazz= "package test;\n" + "public class Test {" + "	private String str=TestMessages.getString(\"Key.5\");//$NON-NLS-1$\n" + "}\n";
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
		pack.createCompilationUnit("TestMessages.java", ACCESSOR_KLAZZ, false, null);

		CompilationUnit astRoot= ASTCreator.createAST(cu, null);
		NLSHint hint= new NLSHint(cu, astRoot);
		NLSSubstitution[] substitution= hint.getSubstitutions();
		assertEquals(substitution.length, 1);
		assertEquals(substitution[0].getKey(), "Key.5");
	}
}
