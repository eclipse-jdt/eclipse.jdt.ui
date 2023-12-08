/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
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
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 219530 [jar application] add Jar-in-Jar ClassLoader option
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 262746 [jar exporter] Create a builder for jar-in-jar-loader.zip
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.jarpackagerfat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.jarpackager.IManifestProvider;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;

/**
 * A jar builder which copies the referenced libraries into the generated jar and adds a special
 * class loader which allows to load the classes from the referenced libraries.
 *
 * @since 3.5
 */
public class FatJarRsrcUrlBuilder extends FatJarBuilder {

	public static final String BUILDER_ID= "org.eclipse.jdt.ui.fat_jar_rsrc_url_builder"; //$NON-NLS-1$
	public static final String JAR_RSRC_LOADER_ZIP= "jar-in-jar-loader.zip"; //$NON-NLS-1$

	private Set<String> jarNames;
	private JarPackageData fJarPackage;

	@Override
	public String getId() {
		return BUILDER_ID;
	}

	/**
	 * we do not need to merge any manifests here.
	 * @return false
	 */
	@Override
	public boolean isMergeManifests() {
		return false;
	}

	/**
	 * we do not need to remove signers here.
	 * @return false
	 */
	@Override
	public boolean isRemoveSigners() {
		return false;
	}

	@Override
	public String getManifestClasspath() {
		return "."; //$NON-NLS-1$
	}

	@Override
	public IManifestProvider getManifestProvider() {
		return new FatJarRsrcUrlManifestProvider(this);
	}

	@Override
	public void open(JarPackageData jarPackage, Shell displayShell, MultiStatus status) throws CoreException {
		super.open(jarPackage, displayShell, status);
		fJarPackage= jarPackage;
		jarNames= new HashSet<>();
		try {
			writeRsrcUrlClasses();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, e.getMessage(), e));
		}
	}

	@Override
	public void writeArchive(ZipFile jarFile, IProgressMonitor progressMonitor) {
		File jarPathFile= new File(jarFile.getName());
		String jarName = jarPathFile.getName();
		while (jarNames.contains(jarName)) {
			jarName= FatJarPackagerUtil.nextNumberedFileName(jarName);
		}
		jarNames.add(jarName);
		JarEntry newEntry = new JarEntry(jarName);
		newEntry.setMethod(ZipEntry.STORED);
		try {
			byte[] allBytes= Files.readAllBytes(jarPathFile.toPath());
			if (!fJarPackage.isCompressed()) {
				JarPackagerUtil.setCrcAndSize(newEntry, allBytes);
			}
			getJarWriter().addZipEntryStream(newEntry, allBytes, jarName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeRsrcUrlClasses() throws IOException {
		try (ZipInputStream zis= new ZipInputStream(JavaPlugin.getDefault().getBundle().getEntry(JAR_RSRC_LOADER_ZIP).openStream())){
			ZipEntry zipEntry= zis.getNextEntry();
			while (zipEntry != null) {
				if (!zipEntry.isDirectory()) {
					String entryName= zipEntry.getName();
					byte[] content= zis.readAllBytes();
					getJarWriter().addZipEntryStream(zipEntry, content, entryName);
				}
				zipEntry= zis.getNextEntry();
			}
		}
	}
}
