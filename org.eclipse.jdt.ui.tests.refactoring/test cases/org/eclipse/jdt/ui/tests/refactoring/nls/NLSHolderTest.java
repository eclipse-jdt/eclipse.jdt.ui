/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHint;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSubstitution;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class NLSHolderTest {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject javaProject;

	private IPackageFragmentRoot fSourceFolder;

	private final static String ACCESSOR_KLAZZ= """
		package test;
		public class TestMessages {
			private static final String BUNDLE_NAME = "test.test";//$NON-NLS-1$
			public static String getString(String s) {\
				return "";
			}
		}
		""";

	@Before
	public void setUp() throws Exception {
		javaProject= pts.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(javaProject, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(javaProject, pts.getDefaultClasspath());
	}

	@Test
	public void substitutionWithAccessor() throws Exception {
		String klazz= """
			package test;
			public class Test {\
				private String str=TestMessages.getString("Key.5");//$NON-NLS-1$
			}
			""";
		IPackageFragment pack= fSourceFolder.createPackageFragment("test", false, null);
		ICompilationUnit cu= pack.createCompilationUnit("Test.java", klazz, false, null);
		pack.createCompilationUnit("TestMessages.java", ACCESSOR_KLAZZ, false, null);

		CompilationUnit astRoot= ASTCreator.createAST(cu, null);
		NLSHint hint= new NLSHint(cu, astRoot);
		NLSSubstitution[] substitution= hint.getSubstitutions();
		assertEquals(1, substitution.length);
		assertEquals("Key.5", substitution[0].getKey());
	}
}
