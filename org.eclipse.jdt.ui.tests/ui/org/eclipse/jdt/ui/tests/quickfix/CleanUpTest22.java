/*******************************************************************************
 * Copyright (c) 2020, 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Hashtable;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java22ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 22.
 */
public class CleanUpTest22 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java22ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testUnusedCleanUpUnnamedVariable() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNUSED_LAMBDA_PARAMETER, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.WARNING);
		JavaCore.setOptions(options);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			public class E {
				private interface J {
					public void run(String a, String b);
				}
				record R(int i, long l) {}
				public void test () {
				  	J j = (a, b) -> System.out.println(a);
					j.run("a", "b");
					R r = new R(1, 1);
					switch (r) {
					case R(_, long l) -> {}
					case R r2 -> {}
					}
				}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES);

		sample= """
			package test1;

			public class E {
				private interface J {
					public void run(String a, String b);
				}
				record R(int i, long l) {}
				public void test () {
				  	J j = (a, _) -> System.out.println(a);
					j.run("a", "b");
					R r = new R(1, 1);
					switch (r) {
					case R(_, long _) -> {}
					case R _ -> {}
					}
				}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

}
