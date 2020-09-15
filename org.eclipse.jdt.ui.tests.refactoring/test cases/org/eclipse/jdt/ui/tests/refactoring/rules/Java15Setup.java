/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.rules;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public class Java15Setup extends RefactoringTestSetup {
	/*
	 * @see org.eclipse.jdt.ui.tests.refactoring.RefactoringTestSetup#addRTJar(org.eclipse.jdt.core.IJavaProject)
	 */
	@Override
	protected IPackageFragmentRoot addRTJar(IJavaProject project) throws CoreException {
		return JavaProjectHelper.addRTJar_15(project, true);
	}
}
