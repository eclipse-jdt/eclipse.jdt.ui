/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.extensions;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.internal.ui.refactoring.StatusContextViewerDescriptor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.SourceRange;

import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStringStatusContext;


public class ExtensionPointTests extends TestCase {

	public static Test suite() {
		return new ExtensionPointTestSetup(new TestSuite(ExtensionPointTests.class));
	}

	public void testJavaStringStatusContextViewer() throws Exception {
		JavaStringStatusContext context= new JavaStringStatusContext("test", new SourceRange(0, 0));
		StatusContextViewerDescriptor descriptor= StatusContextViewerDescriptor.get(context);
		assertNotNull(descriptor);
		assertNotNull(descriptor.createViewer());
	}

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
		JavaProjectHelper.performDummySearch();
		unit.delete(true, new NullProgressMonitor());
	}

	private IPackageFragment getTestPackage() {
		IFolder folder= ResourcesPlugin.getWorkspace().getRoot().getFolder(
			new Path("/TestProject/src/test"));
		return (IPackageFragment)JavaCore.create(folder);
	}
}
