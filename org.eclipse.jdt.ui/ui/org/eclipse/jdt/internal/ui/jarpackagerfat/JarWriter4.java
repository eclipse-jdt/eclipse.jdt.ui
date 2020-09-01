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
 *     Ferenc Hechler <ferenc_hechler@users.sourceforge.net> - [jar exporter] Deploy java application as executable jar - https://bugs.eclipse.org/bugs/show_bug.cgi?id=83258
 *     Ferenc Hechler <ferenc_hechler@users.sourceforge.net> - [jar application] ANT build file does not create Class-Path Entry in Manifest - https://bugs.eclipse.org/bugs/show_bug.cgi?id=220257
 *     Ferenc Hechler <ferenc_hechler@users.sourceforge.net> - [jar exporter] export directory entries in "Runnable JAR File" - https://bugs.eclipse.org/bugs/show_bug.cgi?id=243163
 *     Ferenc Hechler <ferenc_hechler@users.sourceforge.net> - [jar application] add Jar-in-Jar ClassLoader option - https://bugs.eclipse.org/bugs/show_bug.cgi?id=219530
 *     Microsoft Corporation - moved some methods to JarPackagerUtilCore for jdt.core.manipulation use
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackagerfat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.jarpackager.JarPackagerUtilCore;

import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.jdt.ui.jarpackager.JarWriter3;

import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerMessages;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;


/**
 * @since 3.4
 */
public class JarWriter4 extends JarWriter3 {

	private final JarPackageData fJarPackage;

	/**
	* Gets the fJarPackage of this JarWriter4
	*
	* @return the fJarPackage of this JarWriter4
	*
	* @since 3.22
	*/
	public JarPackageData getJarPackage() {
		return fJarPackage;
	}

	public JarWriter4(JarPackageData jarPackage, Shell parent) throws CoreException {
		super(jarPackage, parent);
		fJarPackage= jarPackage;
	}

	public void addZipEntry(ZipEntry zipEntry, ZipFile zipFile, String path) throws IOException {
		JarPackagerUtilCore.addZipEntry(zipEntry, zipFile, path, fJarPackage.areDirectoryEntriesIncluded(),
				fJarPackage.isCompressed(), this.getJarOutputStream(), this.getDirectories());
	}

	public void addZipEntryStream(ZipEntry zipEntry, InputStream is, String path) throws IOException {
		if (fJarPackage.areDirectoryEntriesIncluded())
			addDirectories(path);
		JarEntry newEntry= new JarEntry(path.replace(File.separatorChar, '/'));
		if (fJarPackage.isCompressed())
			newEntry.setMethod(ZipEntry.DEFLATED);
		// Entry is filled automatically.
		else {
			newEntry.setMethod(ZipEntry.STORED);
			newEntry.setSize(zipEntry.getSize());
			newEntry.setCrc(zipEntry.getCrc());
		}
		long lastModified= System.currentTimeMillis();
		// Set modification time
		newEntry.setTime(lastModified);
		addEntry(newEntry, is);
	}

	public void write(File file, IPath destinationPath) throws CoreException {
		try {
			addFile(file, destinationPath);
		} catch (IOException ex) {
			// Ensure full path is visible
			String message= null;
			IPath path= new Path(file.getAbsolutePath());
			if (ex.getLocalizedMessage() != null)
				message= Messages.format(JarPackagerMessages.JarWriter_writeProblemWithMessage,
						new Object[] { BasicElementLabels.getPathLabel(path, false), ex.getLocalizedMessage() });
			else
				message= Messages.format(JarPackagerMessages.JarWriter_writeProblem, BasicElementLabels.getPathLabel(path, false));
			throw JarPackagerUtil.createCoreException(message, ex);
		}
	}

	private void addFile(File file, IPath path) throws IOException {
		if (fJarPackage.areDirectoryEntriesIncluded())
			addDirectories(path);

		JarEntry newEntry= new JarEntry(path.toString().replace(File.separatorChar, '/'));

		if (fJarPackage.isCompressed())
			newEntry.setMethod(ZipEntry.DEFLATED);
			// Entry is filled automatically.
		else {
			newEntry.setMethod(ZipEntry.STORED);
			JarPackagerUtil.calculateCrcAndSize(newEntry, new FileInputStream(file), new byte[4096]);
		}

		newEntry.setTime(file.lastModified());
		addEntry(newEntry, new FileInputStream(file));
	}
}
