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

package org.eclipse.jdt.text.tests.performance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Plugin;


public class FileTool {

	/**
	 * A buffer.
	 */
	private static byte[] fgBuffer = new byte[8192];

	/**
	 * Unzips the given zip file to the given destination directory extracting only those entries
	 * the pass through the given filter.
	 *
	 * @param zFile the zip file to unzip
	 * @param dstDir the destination directory
	 * @throws IOException if an I/O error occurs
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
				File file= new File(dstDir, changeSeparator(entryName, '/', File.separatorChar));
				file.getParentFile().mkdirs();
				try (InputStream src= zipFile.getInputStream(entry);
						OutputStream dst= new FileOutputStream(file)) {
					transferData(src, dst);
				} catch (IOException e) {
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

	/**
	 * Copies all bytes in the given source file to the given destination file.
	 *
	 * @param source the given source file
	 * @param destination the given destination file
	 * @throws IOException if an I/O error occurs
	 */
	public static void transferData(File source, File destination) throws IOException {
		destination.getParentFile().mkdirs();
		try (InputStream is = new FileInputStream(source);
			OutputStream os = new FileOutputStream(destination)) {
			transferData(is, os);
		}
	}

	/**
	 * Copies all bytes in the given source stream to the given destination stream. Neither streams
	 * are closed.
	 *
	 * @param source the given source stream
	 * @param destination the given destination stream
	 * @throws IOException if an I/O error occurs
	 */
	public static void transferData(InputStream source, OutputStream destination) throws IOException {
		int bytesRead = 0;
		while(bytesRead != -1){
			bytesRead = source.read(fgBuffer, 0, fgBuffer.length);
			if(bytesRead != -1){
				destination.write(fgBuffer, 0, bytesRead);
			}
		}
	}

	/**
	 * Copies the given source file to the given destination file.
	 *
	 * @param src the given source file
	 * @param dst the given destination file
	 * @throws IOException if an I/O error occurs
	 */
	public static void copy(File src, File dst) throws IOException {
		if(src.isDirectory()){
			for (String element : src.list()) {
				File srcChild= new File(src, element);
				File dstChild= new File(dst, element);
				copy(srcChild, dstChild);
			}
		} else
			transferData(src, dst);
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

	public static void write(String fileName, StringBuffer content) throws IOException {
		try (Writer writer= new FileWriter(fileName)) {
			writer.write(content.toString());
		}
	}
}
