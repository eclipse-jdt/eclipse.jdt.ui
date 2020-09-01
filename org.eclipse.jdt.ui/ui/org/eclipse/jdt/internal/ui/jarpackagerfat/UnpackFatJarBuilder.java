/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
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
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 219530 [jar application] add Jar-in-Jar ClassLoader option
 *     Microsoft Corporation - moved some methods to JarPackagerUtilCore for jdt.core.manipulation use
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackagerfat;

import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.internal.jarpackager.JarPackagerUtilCore;

import org.eclipse.jdt.ui.jarpackager.IManifestProvider;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

/**
 * A jar builder wich unpacks all referenced libraries into the generated jar.
 *
 * @since 3.5
 */
public class UnpackFatJarBuilder extends FatJarBuilder {

	public static final String BUILDER_ID= "org.eclipse.jdt.ui.fat_jar_builder"; //$NON-NLS-1$

	@Override
	public String getId() {
		return BUILDER_ID;
	}

	@Override
	public IManifestProvider getManifestProvider() {
		return new FatJarManifestProvider(this);
	}

	@Override
	public String getManifestClasspath() {
		return "."; //$NON-NLS-1$
	}

	@Override
	public boolean isMergeManifests() {
		return true;
	}

	@Override
	public boolean isRemoveSigners() {
		return true;
	}

	@Override
	public void writeArchive(ZipFile zipFile, IProgressMonitor progressMonitor) {
		JarWriter4 JarWriter= getJarWriter();
		JarPackageData JarPackage= JarWriter.getJarPackage();
		JarPackagerUtilCore.writeArchive(zipFile, JarPackage.areDirectoryEntriesIncluded(),
				JarPackage.isCompressed(), JarWriter.getJarOutputStream(), JarWriter.getDirectories(), getStatus(), progressMonitor);
	}

}
