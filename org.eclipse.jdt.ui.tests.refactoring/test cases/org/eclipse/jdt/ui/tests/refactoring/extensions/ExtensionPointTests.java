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
package org.eclipse.jdt.ui.tests.refactoring.extensions;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.internal.ui.refactoring.StatusContextViewerDescriptor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.SourceRange;

import org.eclipse.jdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;

public class ExtensionPointTests {
	@Rule
	public ExtensionPointTestSetup eps= new ExtensionPointTestSetup();

	@Test
	public void testJavaStringStatusContextViewer() throws Exception {
		JavaStringStatusContext context= new JavaStringStatusContext("test", new SourceRange(0, 0));
		StatusContextViewerDescriptor descriptor= StatusContextViewerDescriptor.get(context);
		assertNotNull(descriptor);
		assertNotNull(descriptor.createViewer());
	}

	@Test
	public void testJavaStatusContextViewer() throws Exception {
		IPackageFragment pack= getTestPackage();
		ICompilationUnit unit= pack.createCompilationUnit(
			"A.java",
			"package test; class A { }",
			true, null);
		RefactoringStatusContext context= JavaStatusContext.create(unit);
		StatusContextViewerDescriptor descriptor= StatusContextViewerDescriptor.get(context);
		assertNotNull(descriptor);
		assertNotNull(descriptor.createViewer());
	}

	private IPackageFragment getTestPackage() {
		IFolder folder= ResourcesPlugin.getWorkspace().getRoot().getFolder(
			new Path("/TestProject/src/test"));
		return (IPackageFragment)JavaCore.create(folder);
	}
}
