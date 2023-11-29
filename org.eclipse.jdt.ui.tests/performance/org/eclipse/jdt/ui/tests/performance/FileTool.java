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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;


public class FileTool {

	/**
	 * A buffer.
	 */
	private static byte[] fgBuffer = new byte[8192];

	/**
	 * Unzips the given zip file to the given destination directory
	 * extracting only those entries the pass through the given
	 * filter.
	 *
	 * @param zipFile the zip file to unzip
	 * @param dstDir the destination directory
	 */
	public static void unzip(ZipFile zipFile, File dstDir) throws IOException {

		Enumeration<? extends ZipEntry> entries = zipFile.entries();

		try {
			while(entries.hasMoreElements()){
				ZipEntry entry = entries.nextElement();
				if(entry.isDirectory()){
					continue;
				}
				String entryName = entry.getName();
				File file = new File(dstDir, changeSeparator(entryName, '/', File.separatorChar));
				file.getParentFile().mkdirs();
				InputStream src = null;
				OutputStream dst = null;
				try {
					src = zipFile.getInputStream(entry);
					dst = new FileOutputStream(file);
					transferData(src, dst);
				} finally {
					if(dst != null){
						try {
							dst.close();
						} catch(IOException e){
						}
					}
					if(src != null){
						try {
							src.close();
						} catch(IOException e){
						}
					}
				}
			}
		} finally {
			try {
				zipFile.close();
			} catch(IOException e){
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
	 * Copies all bytes in the given source file to
	 * the given destination file.
	 *
	 * @param source the given source file
	 * @param destination the given destination file
	 */
	public static void transferData(File source, File destination) throws IOException {
		destination.getParentFile().mkdirs();
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(destination);
			transferData(is, os);
		} finally {
			if(os != null){
				try {
					os.close();
				} catch(IOException e){
				}
			}
			if(is != null){
				try {
					is.close();
				} catch(IOException e){
				}
			}
		}
	}

	/**
	 * Copies all bytes in the given source stream to
	 * the given destination stream. Neither streams
	 * are closed.
	 *
	 * @param source the given source stream
	 * @param destination the given destination stream
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
	 */
	public static void copy(File src, File dst) throws IOException {
		if(src.isDirectory()){
			String[] srcChildren = src.list();
			for (String element : srcChildren) {
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
			URL localURL= Platform.asLocalURL(installURL);
			return new File(localURL.getFile());
		} catch (IOException e) {
			return null;
		}
	}

	public static StringBuffer read(String fileName) throws IOException {
		return read(new FileReader(fileName));
	}

	public static StringBuffer read(Reader reader) throws IOException {
		StringBuffer s= new StringBuffer();
		try {
			char[] buffer= new char[8196];
			int chars= reader.read(buffer);
			while (chars != -1) {
				s.append(buffer, 0, chars);
				chars= reader.read(buffer);
			}
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
		return s;
	}

	public static void write(String fileName, StringBuffer content) throws IOException {
		Writer writer= new FileWriter(fileName);
		try {
			writer.write(content.toString());
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
			}
		}
	}
}
