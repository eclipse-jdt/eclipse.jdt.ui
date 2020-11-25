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

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java1d4ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 1.4 (i.e. Merlin).
 */
public class CleanUpTest1d4 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java1d4ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testSerialVersion01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 implements Serializable {\n" //
				+ "}\n";

		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", given, false, null);
		getProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

		String expected= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 implements Serializable {\n" //
				+ "\n" //
				+ "    " + FIELD_COMMENT + "\n" //
				+ "    private static final long serialVersionUID = 1L;\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu}, new String[] {expected});
	}

	@Test
	public void testSerialVersion02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 implements Serializable {\n" //
				+ "    public class B1 implements Serializable {\n" //
				+ "    }\n" //
				+ "    public class B2 extends B1 {\n" //
				+ "    }\n" //
				+ "}\n";

		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", given, false, null);

		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

		String expected= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 implements Serializable {\n" //
				+ "    " + FIELD_COMMENT + "\n" //
				+ "    private static final long serialVersionUID = 1L;\n" //
				+ "    public class B1 implements Serializable {\n" //
				+ "\n" //
				+ "        " + FIELD_COMMENT + "\n" //
				+ "        private static final long serialVersionUID = 1L;\n" //
				+ "    }\n" //
				+ "    public class B2 extends B1 {\n" //
				+ "\n" //
				+ "        " + FIELD_COMMENT + "\n" //
				+ "        private static final long serialVersionUID = 1L;\n" //
				+ "    }\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu}, new String[] {expected});
	}

	@Test
	public void testSerialVersion03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 implements Serializable {\n" //
				+ "}\n";

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", given, false, null);

		String expected= "" //
				+ "package test1;\n" //
				+ "import java.io.Externalizable;\n" //
				+ "public class E2 implements Externalizable {\n" //
				+ "}\n";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", expected, false, null);

		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

		String expected2= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 implements Serializable {\n" //
				+ "\n" //
				+ "    " + FIELD_COMMENT + "\n" //
				+ "    private static final long serialVersionUID = 1L;\n" //
				+ "}\n";

		String expected1= "" //
				+ "package test1;\n" //
				+ "import java.io.Externalizable;\n" //
				+ "public class E2 implements Externalizable {\n" //
				+ "}\n";

		assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu1, cu2}, new String[] {expected1, expected2});
	}

	@Test
	public void testSerialVersion04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 implements Serializable {\n" //
				+ "    public void foo() {\n" //
				+ "        Serializable s= new Serializable() {\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";

		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", given, false, null);

		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

		String expected= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 implements Serializable {\n" //
				+ "    " + FIELD_COMMENT + "\n" //
				+ "    private static final long serialVersionUID = 1L;\n" //
				+ "\n" //
				+ "    public void foo() {\n" //
				+ "        Serializable s= new Serializable() {\n" //
				+ "\n" //
				+ "            " + FIELD_COMMENT + "\n" //
				+ "            private static final long serialVersionUID = 1L;\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu}, new String[] {expected});
	}

	@Test
	public void testSerialVersion05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 implements Serializable {\n" //
				+ "\n" //
				+ "    private Serializable s= new Serializable() {\n" //
				+ "        \n" //
				+ "    };\n" //
				+ "}\n";

		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", given, false, null);

		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

		String expected= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 implements Serializable {\n" //
				+ "\n" //
				+ "    " + FIELD_COMMENT + "\n" //
				+ "    private static final long serialVersionUID = 1L;\n" //
				+ "    private Serializable s= new Serializable() {\n" //
				+ "\n" //
				+ "        " + FIELD_COMMENT + "\n" //
				+ "        private static final long serialVersionUID = 1L;\n" //
				+ "        \n" //
				+ "    };\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu}, new String[] {expected});
	}

	@Test
	public void testSerialVersionBug139381() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 {\n" //
				+ "    void foo1() {\n" //
				+ "        new Serializable() {\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    void foo2() {\n" //
				+ "        new Object() {\n" //
				+ "        };\n" //
				+ "        new Serializable() {\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";

		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", given, false, null);

		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID);
		enable(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED);

		String expected= "" //
				+ "package test1;\n" //
				+ "import java.io.Serializable;\n" //
				+ "public class E1 {\n" //
				+ "    void foo1() {\n" //
				+ "        new Serializable() {\n" //
				+ "\n" //
				+ "            " + FIELD_COMMENT + "\n" //
				+ "            private static final long serialVersionUID = 1L;\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "    void foo2() {\n" //
				+ "        new Object() {\n" //
				+ "        };\n" //
				+ "        new Serializable() {\n" //
				+ "\n" //
				+ "            " + FIELD_COMMENT + "\n" //
				+ "            private static final long serialVersionUID = 1L;\n" //
				+ "        };\n" //
				+ "    }\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpectedIgnoreHashValue(new ICompilationUnit[] {cu}, new String[] {expected});
	}
}
