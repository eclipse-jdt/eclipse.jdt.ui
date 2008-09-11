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
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;

import org.osgi.framework.Bundle;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractRefactoringTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;

/**
 * Sets up two projects for testing binary references. Contents taken from /resources/BinaryReferencesWorkspace.
 */
public class BinaryReferencesTestSetup extends AbstractRefactoringTestSetup {

	public BinaryReferencesTestSetup(Test test) {
		super(test);
	}

	private IJavaProject fSource;
	private IJavaProject fBinaryReference;

	public IJavaProject getSourceProject() {
		return fSource;
	}

	public IJavaProject getBinaryReferenceProject() {
		return fBinaryReference;
	}

	protected void setUp() throws Exception {
		super.setUp();
		Bundle bundle= RefactoringTestPlugin.getDefault().getBundle();

		fSource= JavaProjectHelper.createJavaProject("Source", "bin");
		JavaProjectHelper.addRTJar(fSource);
		IPackageFragmentRoot sourceContainer= JavaProjectHelper.addSourceContainer(fSource, "src");
		JavaProjectHelper.importResources((IFolder) sourceContainer.getResource(), bundle, "resources/BinaryReferencesWorkspace/Source/src");


		fBinaryReference= JavaProjectHelper.createJavaProject("BinaryReference", null);
		JavaProjectHelper.addRTJar(fBinaryReference);

		IClasspathEntry cpeSource= JavaCore.newProjectEntry(fSource.getProject().getFullPath());
		JavaProjectHelper.addToClasspath(fBinaryReference, cpeSource);

		IFolder binary= fBinaryReference.getProject().getFolder("binary");
		binary.create(false, true, null);
		JavaProjectHelper.importResources(binary, bundle, "resources/BinaryReferencesWorkspace/Reference/bin");
		// attach source to get search results ( https://bugs.eclipse.org/bugs/show_bug.cgi?id=127442 ):
		IFolder srcAtt= fBinaryReference.getProject().getFolder("srcAtt");
		srcAtt.create(false, true, null);
		JavaProjectHelper.importResources(srcAtt, bundle, "resources/BinaryReferencesWorkspace/Reference/src");

		IClasspathEntry cpeBinary= JavaCore.newLibraryEntry(binary.getFullPath(), srcAtt.getFullPath(), Path.ROOT, false);
		JavaProjectHelper.addToClasspath(fBinaryReference, cpeBinary);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fSource);
		JavaProjectHelper.delete(fBinaryReference);
		super.tearDown();
	}
}

