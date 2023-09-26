/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.ClasspathEntry;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class Java15ProjectTestSetup extends ProjectTestSetup {

	private boolean enable_preview_feature;

	public Java15ProjectTestSetup() {
		super("TestSetupProject15", JavaProjectHelper.RT_STUBS15);
	}

	@Override
	public IClasspathEntry[] getDefaultClasspath() throws CoreException {
		IPath[] rtJarPath= JavaProjectHelper.findRtJar(ipath);
		IClasspathAttribute[] extraAttributes= { JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, Boolean.TRUE.toString()) };
		return new IClasspathEntry[] { JavaCore.newLibraryEntry(rtJarPath[0], rtJarPath[1], rtJarPath[2], ClasspathEntry.NO_ACCESS_RULES, extraAttributes, true) };
	}

	public Java15ProjectTestSetup( boolean enable_preview_feature) {
		this();
		this.enable_preview_feature= enable_preview_feature;
	}

	@Override
	protected IJavaProject createAndInitializeProject() throws CoreException {
		IJavaProject javaProject= super.createAndInitializeProject();
		JavaProjectHelper.set15CompilerOptions(javaProject, enable_preview_feature);
		return javaProject;
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


}
