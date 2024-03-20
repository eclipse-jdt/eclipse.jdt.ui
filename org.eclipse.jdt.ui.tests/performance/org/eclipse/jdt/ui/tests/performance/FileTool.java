/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

package org.eclipse.jdt.ui.tests.performance;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Plugin;


public class FileTool {

	/**
	 * Unzips the given zip file to the given destination directory
	 * extracting only those entries the pass through the given
	 * filter.
	 *
	 * @param zFile the zip file to unzip
	 * @param dstDir the destination directory
	 */
	public static void unzip(File zFile, File dstDir) throws IOException {
		try (ZipFile zipFile= new ZipFile(zFile)) {
			Enumeration<? extends ZipEntry> entries= zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry= entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				String entryName= entry.getName();
				if (!new File(dstDir, entryName).toPath().normalize().startsWith(dstDir.toPath().normalize())) {
					throw new RuntimeException("Bad zip entry: " + entryName); //$NON-NLS-1$
				}
				File file= new File(dstDir, changeSeparator(entryName, '/', File.separatorChar));
				file.getParentFile().mkdirs();
				try (InputStream inputStream= zipFile.getInputStream(entry)) {
					Files.write(file.toPath(), inputStream.readAllBytes());
				}
			}
		}
	}
	/**
	 * Returns the given file path with its separator
	 * character changed from the given old separator to the
	 * given new separator.
	 *
	 * @param path a file path
	 * @param oldSeparator a path separator character
	 * @param newSeparator a path separator character
	 * @return the file path with its separator character
	 * changed from the given old separator to the given new
	 * separator
	 */
	public static String changeSeparator(String path, char oldSeparator, char newSeparator){
		return path.replace(oldSeparator, newSeparator);
	}

	public static File getFileInPlugin(Plugin plugin, IPath path) {
		try {
			URL installURL= plugin.getBundle().getEntry(path.toString());
			URL localURL= FileLocator.toFileURL(installURL);
			return new File(localURL.getFile());
		} catch (IOException e) {
			return null;
		}
	}
}
