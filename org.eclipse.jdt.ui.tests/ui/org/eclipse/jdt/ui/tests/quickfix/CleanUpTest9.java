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

import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java9ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

/**
 * Tests the cleanup features related to Java 9.
 */
public class CleanUpTest9 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java9ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testUseTryWithResource() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.FileInputStream;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void refactorFullyInitializedResourceRemoveFinally() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        final FileInputStream inputStream = new FileInputStream(\"data.txt\");\n" //
				+ "        // Keep this comment too\n" //
				+ "        try {\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        } finally {\n" //
				+ "            inputStream.close();\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFullyInitializedResourceDoNotRemoveFinally() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        final FileInputStream inputStream = new FileInputStream(\"out.txt\");\n" //
				+ "        // Keep this comment too\n" //
				+ "        try {\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        } finally {\n" //
				+ "            inputStream.close();\n" //
				+ "            System.out.println(\"Done\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean removeClosureOnStillUsedCloseable() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        final FileInputStream inputStream = new FileInputStream(\"input.txt\");\n" //
				+ "        // Keep this comment too\n" //
				+ "        try {\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        } finally {\n" //
				+ "            inputStream.close();\n" //
				+ "            System.out.println(\"Done\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return inputStream != null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFullyInitializedResourceOnlyRemoveFinallyIf() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        final FileInputStream inputStream = new FileInputStream(\"out.txt\");\n" //
				+ "        // Keep this comment too\n" //
				+ "        try {\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        } finally {\n" //
				+ "            if (inputStream != null) {\n" //
				+ "                inputStream.close();\n" //
				+ "            }\n" //
				+ "            System.out.println(\"Done\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorNullInitializedResourceRemoveFinally() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        FileInputStream inputStream = null;\n" //
				+ "        // Keep this comment too\n" //
				+ "        try {\n" //
				+ "            inputStream = new FileInputStream(\"output.txt\");\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        } finally {\n" //
				+ "            if (inputStream != null) {\n" //
				+ "                inputStream.close();\n" //
				+ "            }\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorNullInitializedResourceDoNotRemoveFinally() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        FileInputStream inputStream = null;\n" //
				+ "        // Keep this comment too\n" //
				+ "        try {\n" //
				+ "            inputStream = new FileInputStream(\"file.txt\");\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        } finally {\n" //
				+ "            if (null != inputStream) {\n" //
				+ "                inputStream.close();\n" //
				+ "            }\n" //
				+ "            System.out.println(\"Done\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.TRY_WITH_RESOURCE);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.FileInputStream;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void refactorFullyInitializedResourceRemoveFinally() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        final FileInputStream inputStream = new FileInputStream(\"data.txt\");\n" //
				+ "        // Keep this comment too\n" //
				+ "        try (inputStream) {\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFullyInitializedResourceDoNotRemoveFinally() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        final FileInputStream inputStream = new FileInputStream(\"out.txt\");\n" //
				+ "        // Keep this comment too\n" //
				+ "        try (inputStream) {\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        } finally {\n" //
				+ "            System.out.println(\"Done\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public boolean removeClosureOnStillUsedCloseable() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        final FileInputStream inputStream = new FileInputStream(\"input.txt\");\n" //
				+ "        // Keep this comment too\n" //
				+ "        try (inputStream) {\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        } finally {\n" //
				+ "            System.out.println(\"Done\");\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return inputStream != null;\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorFullyInitializedResourceOnlyRemoveFinallyIf() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        final FileInputStream inputStream = new FileInputStream(\"out.txt\");\n" //
				+ "        // Keep this comment too\n" //
				+ "        try (inputStream) {\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        } finally {\n" //
				+ "            System.out.println(\"Done\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorNullInitializedResourceRemoveFinally() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        // Keep this comment too\n" //
				+ "        try (FileInputStream inputStream = new FileInputStream(\"output.txt\")) {\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public void refactorNullInitializedResourceDoNotRemoveFinally() throws Exception {\n" //
				+ "        // Keep this comment\n" //
				+ "        // Keep this comment too\n" //
				+ "        try (FileInputStream inputStream = new FileInputStream(\"file.txt\")) {\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        } finally {\n" //
				+ "            System.out.println(\"Done\");\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.TryWithResourceCleanup_description)));
	}

	@Test
	public void testDoNotUseTryWithResource() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.FileInputStream;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public boolean doNotRefactorStillUsedCloseable() throws Exception {\n" //
				+ "        FileInputStream inputStream = null;\n" //
				+ "        try {\n" //
				+ "            inputStream = new FileInputStream(\"out.txt\");\n" //
				+ "            System.out.println(inputStream.read());\n" //
				+ "        } finally {\n" //
				+ "            inputStream.close();\n" //
				+ "        }\n" //
				+ "\n" //
				+ "        return inputStream != null;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.TRY_WITH_RESOURCE);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}
