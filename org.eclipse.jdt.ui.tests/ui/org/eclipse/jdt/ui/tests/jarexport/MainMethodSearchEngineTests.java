/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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
package org.eclipse.jdt.ui.tests.jarexport;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.tests.core.rules.Java25ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.util.MainMethodSearchEngine;

/**
 * Tests that {@link MainMethodSearchEngine} (used by the Runnable JAR export) finds a Java 25 instance
 * <code>void main()</code> method.
 */
public class MainMethodSearchEngineTests {

	@Rule
	public ProjectTestSetup pts= new Java25ProjectTestSetup();

	private IJavaProject fProject;

	@Before
	public void setUp() throws Exception {
		fProject= pts.getProject();
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fProject, pts.getDefaultClasspath());
	}

	@Test
	public void testInstanceMainWithoutParameters() throws Exception {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "src"); //$NON-NLS-1$
		IPackageFragment pack= root.createPackageFragment("p", true, null); //$NON-NLS-1$
		pack.createCompilationUnit("Main.java", "package p;\npublic class Main {\n\tvoid main() {}\n}\n", true, null); //$NON-NLS-1$ //$NON-NLS-2$

		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { root });
		IType[] types= new MainMethodSearchEngine().searchMainMethods(new NullProgressMonitor(), scope, 0);

		assertEquals(1, types.length);
		assertEquals("p.Main", types[0].getFullyQualifiedName()); //$NON-NLS-1$
	}
}
