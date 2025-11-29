/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java25ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 25.
 */
public class CleanUpTest25 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java25ProjectTestSetup(false);

	@Rule
	public ProjectTestSetup projectSetup2= new Java25ProjectTestSetup("project2", false);

	@After
	public void teardown2() throws Exception {
		JavaProjectHelper.clear(getProject2(), getDefaultClasspath());
		fSourceFolder= null;
	}

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	protected IJavaProject getProject2() {
		return projectSetup2.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testAddModuleCleanUp1() throws Exception {
		IPackageFragment defaultPkg= fSourceFolder.createPackageFragment("", false, null);
		String moduleInfo= """
				module module1 {
					requires java.base;
				}
				""";
		defaultPkg.createCompilationUnit("module-info.java", moduleInfo, false, null);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			// header comment
			import java.math.BigInteger;
			import java.util.List;
			import java.util.ArrayList;
			import java.util.Map;
			// header comment 2
			import static java.lang.Math.*; // comment

			public class E {

				List list;
				ArrayList list2;
				Map map;
				BigInteger b;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_MODULE_IMPORTS);

		sample= """
			package test1;

			// header comment 2
			import static java.lang.Math.*; // comment

			import module java.base;

			public class E {

				List list;
				ArrayList list2;
				Map map;
				BigInteger b;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testAddModuleCleanUp2() throws Exception {
		IPackageFragment defaultPkg= fSourceFolder.createPackageFragment("", false, null);
		String moduleInfo= """
				module module1 {
					requires java.base;
				}
				""";
		defaultPkg.createCompilationUnit("module-info.java", moduleInfo, false, null);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			// header comment
			import java.math.BigInteger;
			import java.util.*;
			// header comment 2
			import static java.lang.Math.*; // comment

			public class E {

				List list;
				ArrayList list2;
				Map map;
				BigInteger b;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_MODULE_IMPORTS);

		sample= """
			package test1;

			// header comment 2
			import static java.lang.Math.*; // comment

			import module java.base;

			public class E {

				List list;
				ArrayList list2;
				Map map;
				BigInteger b;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testAddModuleCleanUp3() throws Exception {
		IPackageFragment defaultPkg= fSourceFolder.createPackageFragment("", false, null);
		String moduleInfo= """
				module module1 {
					requires java.base;
				}
				""";
		defaultPkg.createCompilationUnit("module-info.java", moduleInfo, false, null);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			// header comment
			import module java.base;
			import java.math.BigInteger;
			import java.util.*;
			// header comment 2
			import static java.lang.Math.*; // comment

			public class E {

				List list;
				ArrayList list2;
				Map map;
				BigInteger b;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_MODULE_IMPORTS);

		sample= """
			package test1;

			// header comment
			import module java.base;

			// header comment 2
			import static java.lang.Math.*; // comment

			public class E {

				List list;
				ArrayList list2;
				Map map;
				BigInteger b;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testAddModuleCleanUp4() throws Exception {
		IPackageFragmentRoot sourceFolder2= JavaProjectHelper.addSourceContainer(getProject2(), "src");
		IPackageFragment pack2= sourceFolder2.createPackageFragment("test2", false, null);
		String test2List= """
				package test2;

				public class List {}
				""";
		pack2.createCompilationUnit("List.java", test2List, false, null);

		IPackageFragment defaultPkg2= sourceFolder2.createPackageFragment("", false, null);
		String moduleInfo2= """
				module module2 {
					exports test2;
				}
				""";
		defaultPkg2.createCompilationUnit("module-info.java", moduleInfo2, false, null);

		IPackageFragment defaultPkg= fSourceFolder.createPackageFragment("", false, null);
		String moduleInfo= """
				module module1 {
					requires java.base;
					requires module2;
				}
				""";
		defaultPkg.createCompilationUnit("module-info.java", moduleInfo, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			// header comment
			import module java.base;
			import java.math.BigInteger;
			// comment3
			import test2.List;
			import java.util.*;
			// header comment 2
			import static java.lang.Math.*; // comment

			public class E {

				List list;
				ArrayList list2;
				Map map;
				BigInteger b;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_MODULE_IMPORTS);

		sample= """
			package test1;

			// header comment
			import module java.base;
			// comment3
			import test2.List;

			// header comment 2
			import static java.lang.Math.*; // comment

			public class E {

				List list;
				ArrayList list2;
				Map map;
				BigInteger b;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testAddModuleCleanUp5() throws Exception {
		IPackageFragmentRoot sourceFolder2= JavaProjectHelper.addSourceContainer(getProject2(), "src");
		IPackageFragment pack2= sourceFolder2.createPackageFragment("test2", false, null);
		String test2List= """
				package test2;

				public class List {}
				""";
		pack2.createCompilationUnit("List.java", test2List, false, null);

		String test2Map= """
				package test2;

				public class Map {}
				""";
		pack2.createCompilationUnit("Map.java", test2Map, false, null);

		String test2ArrayList= """
				package test2;

				public class ArrayList {}
				""";
		pack2.createCompilationUnit("ArrayList.java", test2ArrayList, false, null);

		IPackageFragment defaultPkg2= sourceFolder2.createPackageFragment("", false, null);
		String moduleInfo2= """
				module module2 {
					exports test2;
				}
				""";
		defaultPkg2.createCompilationUnit("module-info.java", moduleInfo2, false, null);

		IPackageFragment defaultPkg= fSourceFolder.createPackageFragment("", false, null);
		String moduleInfo= """
				module module1 {
					requires java.base;
					requires module2;
				}
				""";
		defaultPkg.createCompilationUnit("module-info.java", moduleInfo, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			// header comment
			import module java.base;
			import java.math.BigInteger;
			// comment3
			import test2.List;
			import test2.ArrayList;
			import test2.Map;
			// header comment 2
			import static java.lang.Math.*; // comment

			public class E {

				List list;
				ArrayList list2;
				Map map;
				BigInteger b;
				Set set;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_MODULE_IMPORTS);

		sample= """
			package test1;

			import java.math.BigInteger;
			import java.util.Set;

			// header comment 2
			import static java.lang.Math.*; // comment

			import module module2;

			public class E {

				List list;
				ArrayList list2;
				Map map;
				BigInteger b;
				Set set;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testAddModuleCleanUp6() throws Exception {
		IPackageFragment defaultPkg= fSourceFolder.createPackageFragment("", false, null);
		String moduleInfo= """
				module module1 {
					requires java.base;
				}
				""";
		defaultPkg.createCompilationUnit("module-info.java", moduleInfo, false, null);
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;

			// header comment
			import java.math.BigInteger;
			import java.util.List;
			import java.util.ArrayList;
			import java.util.Map;
			// header comment 2
			import static java.lang.Math.*; // comment

			public class E {

				List<String> list;
				ArrayList<String> list2;
				Map<String, String> map;
				BigInteger b;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_MODULE_IMPORTS);

		sample= """
			package test1;

			// header comment 2
			import static java.lang.Math.*; // comment

			import module java.base;

			public class E {

				List<String> list;
				ArrayList<String> list2;
				Map<String, String> map;
				BigInteger b;

				public void foo() {
					sqrt(1.0);
					abs(6.4);
					pow(2,2);
				}

			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

}
