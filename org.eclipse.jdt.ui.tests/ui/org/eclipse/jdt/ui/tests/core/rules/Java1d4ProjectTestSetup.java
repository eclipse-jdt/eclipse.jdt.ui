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
 * derived from corresponding file in org.eclipse.jdt.ui.tests.core
 * instead extending TestSetup for junit4 ExternalResource is extended
 * to allow use as junit "@Rule"
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.rules;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaProject;

/**
 * Setup the tests related to Java 1.4 (i.e. Merlin).
 */
public class Java1d4ProjectTestSetup extends ProjectTestSetup {

	public Java1d4ProjectTestSetup() {
		// Here we load Java 1.5 classes because JavaProjectHelper.RT_STUBS_14 does not exist
		super("TestSetupProject1d4", JavaProjectHelper.RT_STUBS_15);
	}

	@Override
	protected IJavaProject createAndInitializeProject() throws CoreException {
		IJavaProject javaProject= super.createAndInitializeProject();
		JavaProjectHelper.set14CompilerOptions(javaProject);
		return javaProject;
	}
}
