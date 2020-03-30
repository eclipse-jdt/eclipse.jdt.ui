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
package org.eclipse.jdt.ui.tests.refactoring;

import org.osgi.framework.Bundle;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;
import org.eclipse.jdt.ui.tests.refactoring.rules.AbstractRefactoringTestSetup;

/**
 * Sets up two projects for testing binary references. Contents taken from /resources/BinaryReferencesWorkspace.
 */
public class BinaryReferencesTestSetup extends AbstractRefactoringTestSetup {
	private IJavaProject fSource;
	private IJavaProject fBinaryReference;

	public IJavaProject getSourceProject() {
		return fSource;
	}

	public IJavaProject getBinaryReferenceProject() {
		return fBinaryReference;
	}

	@Override
	public void before() throws Exception {
		super.before();
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

	@Override
	public void after() {
		try {
			JavaProjectHelper.delete(fSource);
			JavaProjectHelper.delete(fBinaryReference);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.after();
	}
}