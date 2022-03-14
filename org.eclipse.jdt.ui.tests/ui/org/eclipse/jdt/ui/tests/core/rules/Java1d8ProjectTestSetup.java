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

import java.io.File;

import org.osgi.framework.Bundle;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * This class is made to run tests on Java Spider 1.8 .
 */
public class Java1d8ProjectTestSetup extends ProjectTestSetup {

	public Java1d8ProjectTestSetup() {
		super("TestSetupProject1d8", JavaProjectHelper.RT_STUBS_18);
	}

	public static String getJdtAnnotations20Path() {
		Bundle[] annotationsBundles= JavaPlugin.getDefault().getBundles("org.eclipse.jdt.annotation", "2.0.0"); //$NON-NLS-1$
		File bundleFile= FileLocator.getBundleFileLocation(annotationsBundles[0]).get();
		String path= bundleFile.getPath();
		if (bundleFile.isDirectory()) {
			path= bundleFile.getPath() + "/bin";
		}
		return path;
	}

	@Override
	protected IJavaProject createAndInitializeProject() throws CoreException {
		IJavaProject javaProject= super.createAndInitializeProject();
		JavaProjectHelper.set18CompilerOptions(javaProject);
		return javaProject;
	}

}
