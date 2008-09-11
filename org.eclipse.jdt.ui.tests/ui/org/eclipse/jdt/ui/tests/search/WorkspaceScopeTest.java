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
package org.eclipse.jdt.ui.tests.search;

import junit.framework.TestCase;

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

public class WorkspaceScopeTest extends TestCase {
	private IJavaProject fProject1;
	private IJavaProject fProject2;
	private IJavaProject fProject3;
	private IJavaProject fProject4;
	private ICompilationUnit fCompilationUnit;

	public WorkspaceScopeTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
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
		return "package test;\n" +
				"\n" +
				"public class Test {\n" +
				"	public void publicMethod() {\n" +
				"	}\n" +
				"	\n" +
				"	private void privateMethod() {\n" +
				"	}\n" +
				"	\n" +
				"	protected void protectedMethod() {\n" +
				"	}\n" +
				"	\n" +
				"	void defaultMethod() {\n" +
				"	}\n" +
				"}\n";

	}

	public void testPrivateScope() throws JavaModelException {
		IType type= fCompilationUnit.findPrimaryType();
		type.getMethod("privateMethod", new String[0]); //$NON-NLS-1$
		IJavaSearchScope scope= createWorkspaceScope(true);

		assertTrue(scope.encloses(fCompilationUnit));

		IPackageFragmentRoot[] roots= fProject1.getAllPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			IJavaElement[] fragments= roots[i].getChildren();
			for (int j= 0; j < fragments.length; j++) {
				assertFalse(scope.encloses(fragments[j]));
			}
		}

		checkNoRoots(scope, fProject2);
		checkNoRoots(scope, fProject3);
	}

	private IJavaSearchScope createWorkspaceScope(boolean includeJRE) {
		return JavaSearchScopeFactory.getInstance().createWorkspaceScope(includeJRE);
	}

	public void testDefaultScope() throws JavaModelException {
		IType type= fCompilationUnit.findPrimaryType();
		type.getMethod("defaultMethod", new String[0]); //$NON-NLS-1$
		IJavaSearchScope scope= createWorkspaceScope(true);

		assertTrue(scope.encloses(fCompilationUnit.getParent()));

		IPackageFragmentRoot[] roots= fProject1.getAllPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			IJavaElement[] fragments= roots[i].getChildren();
			for (int j= 0; j < fragments.length; j++) {
				if (!fragments[j].equals(fCompilationUnit.getParent()))
					assertFalse(scope.encloses(fragments[j]));
			}
		}

		checkNoRoots(scope, fProject2);
		checkNoRoots(scope, fProject3);
	}


	public void testPublicMethod() throws JavaModelException {
		IType type= fCompilationUnit.findPrimaryType();
		type.getMethod("publicMethod", new String[0]); //$NON-NLS-1$
		IJavaSearchScope scope= createWorkspaceScope(true);
		checkNoJreRoots(scope, fProject1);
		checkNoJreRoots(scope, fProject2);
		checkNoRoots(scope, fProject3);
	}

	public void testProtectedMethod() throws JavaModelException {
		IType type= fCompilationUnit.findPrimaryType();
		type.getMethod("protectedMethod", new String[0]); //$NON-NLS-1$
		IJavaSearchScope scope= createWorkspaceScope(true);
		checkNoJreRoots(scope, fProject1);
		checkNoJreRoots(scope, fProject2);
		checkNoRoots(scope, fProject3);
	}

	private void checkNoJreRoots(IJavaSearchScope scope, IJavaProject project) throws JavaModelException {
		IPackageFragmentRoot[] roots= project.getAllPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			if(scope.encloses(roots[i])) {
				assertFalse(roots[i].isExternal());
			} else {
				assertTrue(roots[i].isExternal());
			}
		}
	}

	private void checkJreRoots(IJavaSearchScope scope, IJavaProject project) throws JavaModelException {
		IPackageFragmentRoot[] roots= project.getAllPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			if(scope.encloses(roots[i])) {
				assertTrue(roots[i].isExternal());
			} else {
				assertFalse(roots[i].isExternal());
			}
		}
	}

	private void checkNoRoots(IJavaSearchScope scope, IJavaProject project) throws JavaModelException {
		IPackageFragmentRoot[] roots= project.getAllPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			assertFalse(scope.encloses(roots[i]));
		}
	}

	private void checkAllRoots(IJavaSearchScope scope, IJavaProject project) throws JavaModelException {
		IPackageFragmentRoot[] roots= project.getAllPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			assertTrue(scope.encloses(roots[i]));
		}
	}

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
