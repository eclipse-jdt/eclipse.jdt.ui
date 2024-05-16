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
package org.eclipse.jdt.ui.tests.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;

public class WorkspaceScopeTest {
	private IJavaProject fProject1;
	private IJavaProject fProject2;
	private IJavaProject fProject3;
	private IJavaProject fProject4;
	private ICompilationUnit fCompilationUnit;

	@Before
	public void setUp() throws Exception {
		fProject1= createStandardProject("Test", "test"); //$NON-NLS-1$ //$NON-NLS-2$
		IPackageFragment pkg= fProject1.findPackageFragment(new Path("/Test/src/test")); //$NON-NLS-1$
		fCompilationUnit= pkg.createCompilationUnit("Test.java", getSource(), true, null); //$NON-NLS-1$

		fProject2= createStandardProject("Test2", "test2");  //$NON-NLS-1$//$NON-NLS-2$
		JavaProjectHelper.addRequiredProject(fProject2, fProject1);

		fProject3= createStandardProject("Test3", "test3"); //$NON-NLS-1$ //$NON-NLS-2$

		fProject4= createStandardProject("Test4", "test4", false);  //$NON-NLS-1$//$NON-NLS-2$
	}

	private IJavaProject createStandardProject(String name, String pkgName) throws CoreException, JavaModelException {
		return createStandardProject(name, pkgName, true);
	}
	private IJavaProject createStandardProject(String name, String pkgName, boolean includeJRE) throws CoreException, JavaModelException {
		IJavaProject project= JavaProjectHelper.createJavaProject(name, "bin"); //$NON-NLS-1$
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(project, "src"); //$NON-NLS-1$
		root.createPackageFragment(pkgName, true, null);
		if (includeJRE) {
			IClasspathEntry jreLib= JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"));  //$NON-NLS-1$
			JavaProjectHelper.addToClasspath(project, jreLib);
		}
		return project;
	}

	private String getSource() {
		return """
			package test;
			
			public class Test {
				public void publicMethod() {
				}
			\t
				private void privateMethod() {
				}
			\t
				protected void protectedMethod() {
				}
			\t
				void defaultMethod() {
				}
			}
			""";

	}

	@Test
	public void testPrivateScope() throws JavaModelException {
		IType type= fCompilationUnit.findPrimaryType();
		type.getMethod("privateMethod", new String[0]); //$NON-NLS-1$
		IJavaSearchScope scope= createWorkspaceScope(true);

		assertTrue(scope.encloses(fCompilationUnit));

		for (IPackageFragmentRoot root : fProject1.getAllPackageFragmentRoots()) {
			for (IJavaElement fragment : root.getChildren()) {
				assertFalse(scope.encloses(fragment));
			}
		}

		checkNoRoots(scope, fProject2);
		checkNoRoots(scope, fProject3);
	}

	private IJavaSearchScope createWorkspaceScope(boolean includeJRE) {
		return JavaSearchScopeFactory.getInstance().createWorkspaceScope(includeJRE);
	}

	@Test
	public void testDefaultScope() throws JavaModelException {
		IType type= fCompilationUnit.findPrimaryType();
		type.getMethod("defaultMethod", new String[0]); //$NON-NLS-1$
		IJavaSearchScope scope= createWorkspaceScope(true);

		assertTrue(scope.encloses(fCompilationUnit.getParent()));

		for (IPackageFragmentRoot root : fProject1.getAllPackageFragmentRoots()) {
			for (IJavaElement fragment : root.getChildren()) {
				if (!fragment.equals(fCompilationUnit.getParent())) {
					assertFalse(scope.encloses(fragment));
				}
			}
		}

		checkNoRoots(scope, fProject2);
		checkNoRoots(scope, fProject3);
	}

	@Test
	public void testPublicMethod() throws JavaModelException {
		IType type= fCompilationUnit.findPrimaryType();
		type.getMethod("publicMethod", new String[0]); //$NON-NLS-1$
		IJavaSearchScope scope= createWorkspaceScope(true);
		checkNoJreRoots(scope, fProject1);
		checkNoJreRoots(scope, fProject2);
		checkNoRoots(scope, fProject3);
	}

	@Test
	public void testProtectedMethod() throws JavaModelException {
		IType type= fCompilationUnit.findPrimaryType();
		type.getMethod("protectedMethod", new String[0]); //$NON-NLS-1$
		IJavaSearchScope scope= createWorkspaceScope(true);
		checkNoJreRoots(scope, fProject1);
		checkNoJreRoots(scope, fProject2);
		checkNoRoots(scope, fProject3);
	}

	private void checkNoJreRoots(IJavaSearchScope scope, IJavaProject project) throws JavaModelException {
		for (IPackageFragmentRoot root : project.getAllPackageFragmentRoots()) {
			if (scope.encloses(root)) {
				assertFalse(root.isExternal());
			} else {
				assertTrue(root.isExternal());
			}
		}
	}

	private void checkJreRoots(IJavaSearchScope scope, IJavaProject project) throws JavaModelException {
		for (IPackageFragmentRoot root : project.getAllPackageFragmentRoots()) {
			if (scope.encloses(root)) {
				assertTrue(root.isExternal());
			} else {
				assertFalse(root.isExternal());
			}
		}
	}

	private void checkNoRoots(IJavaSearchScope scope, IJavaProject project) throws JavaModelException {
		for (IPackageFragmentRoot root : project.getAllPackageFragmentRoots()) {
			assertFalse(scope.encloses(root));
		}
	}

	private void checkAllRoots(IJavaSearchScope scope, IJavaProject project) throws JavaModelException {
		for (IPackageFragmentRoot root : project.getAllPackageFragmentRoots()) {
			assertTrue(scope.encloses(root));
		}
	}

	@Test
	public void testJREProtected() throws JavaModelException {
		IType object= fProject1.findType("java.lang.Object"); //$NON-NLS-1$
		object.getMethod("clone", new String [0]); //$NON-NLS-1$
		IJavaSearchScope scope= createWorkspaceScope(true);

		checkAllRoots(scope, fProject1);
		checkAllRoots(scope, fProject2);
		checkJreRoots(scope, fProject3);
		checkNoRoots(scope, fProject4);
	}
}
