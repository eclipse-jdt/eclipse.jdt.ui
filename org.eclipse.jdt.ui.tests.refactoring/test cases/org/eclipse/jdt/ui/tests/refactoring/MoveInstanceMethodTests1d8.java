/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Hashtable;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d8Setup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class MoveInstanceMethodTests1d8 extends MoveInstanceMethodTests {
	public MoveInstanceMethodTests1d8() {
		super(new Java1d8Setup());
	}
	// test for bug 410056, move default method from interface to class
	@Test
	public void test18_1() throws Exception {
		String[] cuQNames= new String[] { "p.A", "p.B" };
		String selectionCuQName= "p.A";
		helper1(cuQNames, selectionCuQName, 3, 20, 3, 34, PARAMETER, "b", true, true);

	}

	// test for bug 410056, move default method from interface to interface
	@Test
	public void test18_2() throws Exception {
		String[] cuQNames= new String[] { "p.A", "p.B" };
		String selectionCuQName= "p.A";
		helper1(cuQNames, selectionCuQName, 3, 20, 3, 34, PARAMETER, "b", true, true);
	}

	// test for bug 410056, move default method from interface to interface(declared field)
	@Test
	public void test18_3() throws Exception {
		String[] cuQNames= new String[] { "p.A", "p.B" };
		String selectionCuQName= "p.A";
		helper1(cuQNames, selectionCuQName, 17, 25, 17, 28, FIELD, "fB", true, true);
	}

	// test for bug 410056, move default method from interface to class(declared field)
	@Test
	public void test18_4() throws Exception {
		String[] cuQNames= new String[] { "p.A", "p.B" };
		String selectionCuQName= "p.A";
		helper1(cuQNames, selectionCuQName, 17, 25, 17, 28, FIELD, "fB", true, true);
	}

	// test that no redundant @NonNull annotations are created
	@Test
	public void testNoRedundantNonNull1() throws Exception {
		IJavaProject javaProject= getRoot().getJavaProject();
		Map<String, String> originalOptions= javaProject.getOptions(false);
		try {
			Hashtable<String, String> newOptions= new Hashtable<>(originalOptions);
			newOptions.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
			javaProject.setOptions(newOptions);
			JavaProjectHelper.addLibrary(javaProject, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

			String[] cuQNames= new String[] { "p.Source", "p.Target" };
			String selectionCuQName= "p.Source";
			helper1(cuQNames, selectionCuQName, 7, 19, 7, 29, PARAMETER, "t", true, true);
		} finally {
			javaProject.setOptions(originalOptions);
		}
	}
	// test required @NonNull annotations are still created where @NonNullByDefault({}) is in effect
	@Test
	public void testNoRedundantNonNull2() throws Exception {
		IJavaProject javaProject= getRoot().getJavaProject();
		Map<String, String> originalOptions= javaProject.getOptions(false);
		try {
			Hashtable<String, String> newOptions= new Hashtable<>(originalOptions);
			newOptions.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
			javaProject.setOptions(newOptions);
			JavaProjectHelper.addLibrary(javaProject, new Path(Java1d8ProjectTestSetup.getJdtAnnotations20Path()));

			String[] cuQNames= new String[] { "p.Source", "p.Target" };
			String selectionCuQName= "p.Source";
			helper1(cuQNames, selectionCuQName, 11, 19, 11, 29, PARAMETER, "t", true, true);
		} finally {
			javaProject.setOptions(originalOptions);
		}
	}
}
