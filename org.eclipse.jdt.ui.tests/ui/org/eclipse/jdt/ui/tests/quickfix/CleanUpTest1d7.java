/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.quickfix;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java1d7ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

@RunWith(JUnit4.class)
public class CleanUpTest1d7 extends CleanUpTestCase {

	@Rule
    public ProjectTestSetup projectsetup = new Java1d7ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return Java1d7ProjectTestSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return Java1d7ProjectTestSetup.getDefaultClasspath();
	}

	@Test
	public void testRemoveRedundantTypeArguments1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Map;\n" //
				+ "public class E {\n" //
				+ "    void foo() {\n" //
				+ "        new ArrayList<String>().add(\"a\")\n" //
				+ "        List<String> a = new ArrayList<String>();\n" //
				+ "        Map<Integer, String> m = new HashMap<Integer, String>();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS);

		sample= "" //
				+ "package test1;\n" //
				+ "import java.util.ArrayList;\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.List;\n" //
				+ "import java.util.Map;\n" //
				+ "public class E {\n" //
				+ "    void foo() {\n" //
				+ "        new ArrayList<String>().add(\"a\")\n" //
				+ "        List<String> a = new ArrayList<>();\n" //
				+ "        Map<Integer, String> m = new HashMap<>();\n" //
				+ "    }\n" //
				+ "}\n";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

}
