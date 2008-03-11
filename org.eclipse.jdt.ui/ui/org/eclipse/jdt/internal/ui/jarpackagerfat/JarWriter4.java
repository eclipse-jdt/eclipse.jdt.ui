/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 83258 [jar exporter] Deploy java application as executable jar
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 220257 [jar application] ANT build file does not create Class-Path Entry in Manifest
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackagerfat;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.jdt.ui.jarpackager.JarWriter3;

/**
 * @since 3.4
 */
public class JarWriter4 extends JarWriter3 {

	private final JarPackageData fJarPackage;

	public JarWriter4(JarPackageData jarPackage, Shell parent) throws CoreException {
		super(jarPackage, parent);
		fJarPackage= jarPackage;
	}

	public void addZipEntry(ZipEntry zipEntry, ZipFile zipFile, String path) throws IOException {
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

		addEntry(newEntry, zipFile.getInputStream(zipEntry));
	}
}
