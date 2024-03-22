/*******************************************************************************
 * Copyright (c) 2020 Julian Honnen
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Julian Honnen - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import org.junit.After;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;

public class MissingTypeCompletionTest extends AbstractCompletionTest {

	private ICompilationUnit missingType;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
	}

	private void createMissingType(String contents) throws CoreException {
		IPackageFragmentRoot root= (IPackageFragmentRoot) getAnonymousTestPackage().getParent();
		IPackageFragment missingFragment= root.createPackageFragment("missing", true, null);

		missingType= missingFragment.createCompilationUnit("MissingType.java", "package missing;\n\n" + contents, true, null);
		expectImport("missing.MissingType");
	}

	@After
	public void cleanupMissingType() throws CoreException {
		if (missingType != null) {
			JavaProjectHelper.delete(missingType);
		}
	}

	@Test
	public void testGenericType_method() throws Exception {
		createMissingType("""
			public class MissingType<T> {
			  public static void foo() {}
			}
			""");
		assertMethodBodyProposal("MissingType.", "foo", "MissingType.foo();");
	}

	@Test
	public void testGenericType_innerClass() throws Exception {
		createMissingType("""
			public class MissingType<T> {
			  public static class Member {}
			}
			""");
		assertMethodBodyProposal("MissingType.", "Member", "MissingType.Member");
	}

	@Test
	public void testGenericType_constructor() throws Exception {
		createMissingType("public class MissingType<T> {\n" +
				"}\n");
		assertMethodBodyProposal("new MissingType", "MissingType", "new MissingType<T>()");
	}

}
