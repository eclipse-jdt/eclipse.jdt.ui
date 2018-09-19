/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 83258 [jar exporter] Deploy java application as executable jar
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackager;

import java.util.zip.ZipFile;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.ui.jarpackager.IManifestProvider;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.jdt.ui.jarpackager.JarWriter3;

/**
 * Jar builder for the plain jar exported. Does not export archives.
 *
 * @since 3.4
 */
public class PlainJarBuilder extends JarBuilder {

	public static final String BUILDER_ID= "org.eclipse.jdt.ui.plain_jar_builder"; //$NON-NLS-1$

	private JarPackageData fJarPackage;
	private JarWriter3 fJarWriter;

	@Override
	public String getId() {
		return BUILDER_ID;
	}

	@Override
	public IManifestProvider getManifestProvider() {
		return new ManifestProvider();
	}

	@Override
	public void open(JarPackageData jarPackage, Shell displayShell, MultiStatus statusMsg) throws CoreException {
		super.open(jarPackage, displayShell, statusMsg);
		fJarPackage= jarPackage;
		fJarWriter= new JarWriter3(fJarPackage, displayShell);
	}

	@Override
	public void writeFile(IFile resource, IPath destinationPath) throws CoreException {
		fJarWriter.write(resource, destinationPath);
	}

	@Override
	public void writeArchive(ZipFile archiveRoot, IProgressMonitor progressMonitor) {
		//do nothing, plain jar builder can not handle archives, use fat jar builder
	}

	@Override
	public void close() throws CoreException {
		if (fJarWriter != null) {
			fJarWriter.close();
		}
	}

}
