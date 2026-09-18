/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vogella GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import org.eclipse.jdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests that the cache never hands out a hierarchy or override tester of a changed type.
 */
public class SuperTypeHierarchyCacheTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragment fPackage;

	@Before
	public void setUp() throws Exception {
		fJProject1= pts.getProject();
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPackage= sourceFolder.createPackageFragment("test1", false, null);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	private ICompilationUnit createBase() throws Exception {
		String str= """
			package test1;
			public class Base {
			    public void foo() {}
			}
			""";
		return fPackage.createCompilationUnit("Base.java", str, false, null);
	}

	private ICompilationUnit createSub() throws Exception {
		String str= """
			package test1;
			public class Sub extends Base {
			    @Override public void foo() {}
			}
			""";
		return fPackage.createCompilationUnit("Sub.java", str, false, null);
	}

	/**
	 * Replaces a compilation unit. Deletes it first, because overwriting a unit that is neither open
	 * nor a working copy produces no fine-grained delta and would leave hierarchies valid.
	 */
	private void recreate(String name, String contents) throws Exception {
		ICompilationUnit existing= fPackage.getCompilationUnit(name);
		if (existing.exists()) {
			existing.delete(true, null);
		}
		fPackage.createCompilationUnit(name, contents, false, null);
	}

	/**
	 * Replaces Base by a version that has a super type of its own, which changes every hierarchy
	 * containing Base.
	 */
	private void changeBaseSuperType() throws Exception {
		String middle= """
			package test1;
			public class Middle {
			}
			""";
		fPackage.createCompilationUnit("Middle.java", middle, false, null);

		String base= """
			package test1;
			public class Base extends Middle {
			    public void foo() {}
			}
			""";
		recreate("Base.java", base);
	}

	@Test
	public void testSecondAccessReturnsCachedHierarchy() throws Exception {
		createBase();
		IType sub= createSub().getType("Sub");

		ITypeHierarchy first= SuperTypeHierarchyCache.getTypeHierarchy(sub);
		assertTrue(SuperTypeHierarchyCache.hasInCache(sub));

		ITypeHierarchy second= SuperTypeHierarchyCache.getTypeHierarchy(sub);
		assertSame("the cached hierarchy must be reused", first, second);
	}

	@Test
	public void testHierarchyRebuiltAfterSuperTypeChange() throws Exception {
		createBase();
		IType sub= createSub().getType("Sub");

		ITypeHierarchy first= SuperTypeHierarchyCache.getTypeHierarchy(sub);
		assertTrue(SuperTypeHierarchyCache.hasInCache(sub));

		changeBaseSuperType();

		assertFalse("the invalidated entry must no longer count as cached", SuperTypeHierarchyCache.hasInCache(sub));

		ITypeHierarchy second= SuperTypeHierarchyCache.getTypeHierarchy(sub);
		assertNotSame("a stale hierarchy must not be handed out", first, second);

		IType base= second.getSuperclass(sub);
		assertNotNull(base);
		IType middle= second.getSuperclass(base);
		assertNotNull("the rebuilt hierarchy must know the new super type", middle);
		assertEquals("Middle", middle.getElementName());
	}

	@Test
	public void testCacheMissAfterSuperTypeChange() throws Exception {
		createBase();
		IType sub= createSub().getType("Sub");

		SuperTypeHierarchyCache.getTypeHierarchy(sub);
		int misses= SuperTypeHierarchyCache.getCacheMisses();

		changeBaseSuperType();
		SuperTypeHierarchyCache.getTypeHierarchy(sub);

		assertEquals("the hierarchy must be rebuilt, not served from the cache", misses + 1, SuperTypeHierarchyCache.getCacheMisses());
	}

	/**
	 * The override tester cache is keyed by type, so it has to drop testers of invalidated
	 * hierarchies even when the hierarchy cache itself was not accessed in between.
	 */
	@Test
	public void testMethodOverrideTesterRebuiltAfterSuperTypeChange() throws Exception {
		createBase();
		IType sub= createSub().getType("Sub");
		IMethod foo= sub.getMethod("foo", new String[0]);

		MethodOverrideTester first= SuperTypeHierarchyCache.getMethodOverrideTester(sub);
		assertNotNull(first.findOverriddenMethod(foo, true));

		changeBaseSuperType();

		MethodOverrideTester second= SuperTypeHierarchyCache.getMethodOverrideTester(sub);
		assertNotSame("a tester of an invalidated hierarchy must not be reused", first, second);
		assertSame(second.getTypeHierarchy(), SuperTypeHierarchyCache.getTypeHierarchy(sub));
	}

	@Test
	public void testMethodOverrideTesterSeesRemovedSuperType() throws Exception {
		createBase();
		IType sub= createSub().getType("Sub");
		IMethod foo= sub.getMethod("foo", new String[0]);

		MethodOverrideTester first= SuperTypeHierarchyCache.getMethodOverrideTester(sub);
		assertNotNull(first.findOverriddenMethod(foo, true));

		String standalone= """
			package test1;
			public class Sub {
			    public void foo() {}
			}
			""";
		recreate("Sub.java", standalone);

		IType reloaded= fPackage.getCompilationUnit("Sub.java").getType("Sub");
		MethodOverrideTester second= SuperTypeHierarchyCache.getMethodOverrideTester(reloaded);
		assertNull("nothing is overridden once the super type is gone",
				second.findOverriddenMethod(reloaded.getMethod("foo", new String[0]), true));
	}
}
