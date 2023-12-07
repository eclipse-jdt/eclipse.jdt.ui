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
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Microsoft Corporation - based this file on JarWriter3, JarWriter4, UnpackFatJarBuilder, JarPackagerUtil and JarBuilder
 *******************************************************************************/
package org.eclipse.jdt.internal.jarpackager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.manipulation.JavaManipulation;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;

public class JarPackagerUtilCore {

	private JarPackagerUtilCore() {

	}
	/**
	 * Write the given entry describing the given content to the current archive. Extracted from
	 * org.eclipse.jdt.ui.jarpackager.JarWriter3
	 *
	 * @param entry the entry to write
	 * @param content the content to write
	 * @param jarOutputStream the destination JarOutputStream
	 *
	 * @throws IOException If an I/O error occurred
	 *
	 * @since 1.14
	 */
	public static void addEntry(JarEntry entry, InputStream content, JarOutputStream jarOutputStream) throws IOException {
		try (content) {
			jarOutputStream.putNextEntry(entry);
			content.transferTo(jarOutputStream);
		}
	}

	/**
	 * Write the contents of the given zipFile to the JarOutputStream. Extracted from
	 * org.eclipse.jdt.internal.ui.jarpackagerfat.UnpackFatJarBuilder
	 *
	 * @param zipFile the zipFile to extract
	 * @param areDirectoryEntriesIncluded the directory entries are included
	 * @param isCompressed the jar is compressed
	 * @param jarOutputStream the destination JarOutputStream
	 * @param directories the temporary set saves existing directories
	 * @param status the <code>MultiStatus</code> saving the warnings during the process
	 * @param progressMonitor the progressMonitor
	 *
	 * @since 1.14
	 */
	public static void writeArchive(ZipFile zipFile, boolean areDirectoryEntriesIncluded,
			boolean isCompressed, JarOutputStream jarOutputStream,
			Set<String> directories, MultiStatus status, IProgressMonitor progressMonitor) {
		Enumeration<? extends ZipEntry> jarEntriesEnum= zipFile.entries();
		File zipFile1= new File(zipFile.getName());
		try {
			String zipFileCanonical= zipFile1.getCanonicalPath();

			while (jarEntriesEnum.hasMoreElements()) {
				ZipEntry zipEntry= jarEntriesEnum.nextElement();
				if (!zipEntry.isDirectory()) {
					String entryName= zipEntry.getName();
					File zipEntryFile= new File(zipFile1, entryName);
					String zipEntryCanonical= zipEntryFile.getCanonicalPath();
					if (zipEntryCanonical.startsWith(zipFileCanonical + File.separator)) {
						addFile(entryName, zipEntry, zipFile, areDirectoryEntriesIncluded, isCompressed, jarOutputStream, directories, status);
					} else {
						addWarning("Invalid path" + entryName, null, status); //$NON-NLS-1$
					}
				}
				progressMonitor.worked(1);
				if (progressMonitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
		} catch (IOException e) {
			addWarning("ZipFile error" + zipFile.getName(), null, status); //$NON-NLS-1$
			e.printStackTrace();
		}
	}

	/**
	 * Write the entry to the destinationPath of the given JarOutputStream. Extracted from
	 * org.eclipse.jdt.internal.ui.jarpackagerfat.UnpackFatJarBuilder
	 *
	 * @param destinationPath the destinationPath in the jar file
	 * @param jarEntry the jar entry to write
	 * @param zipFile the zipFile to extract
	 * @param areDirectoryEntriesIncluded the directory entries are included
	 * @param isCompressed the jar is compressed
	 * @param jarOutputStream the destination JarOutputStream
	 * @param directories the temporary set saves existing directories
	 * @param status the <code>MultiStatus</code> saving the warnings during the process
	 *
	 * @since 1.14
	 */
	private static void addFile(String destinationPath, ZipEntry jarEntry, ZipFile zipFile,
			boolean areDirectoryEntriesIncluded, boolean isCompressed,
			JarOutputStream jarOutputStream, Set<String> directories, MultiStatus status) {
		// Handle META-INF/MANIFEST.MF
		if ("META-INF/MANIFEST.MF".equalsIgnoreCase(destinationPath) //$NON-NLS-1$
				|| (destinationPath.startsWith("META-INF/") && destinationPath.endsWith(".SF"))) { //$NON-NLS-1$//$NON-NLS-2$
			return;
		}
		try {
			addZipEntry(jarEntry, zipFile, destinationPath, areDirectoryEntriesIncluded, isCompressed, jarOutputStream, directories);
		} catch (IOException ex) {
			if (ex instanceof ZipException && ex.getMessage() != null && ex.getMessage().startsWith("duplicate entry:")) {//$NON-NLS-1$
				// ignore duplicates in META-INF (*.SF, *.RSA)
				if (!destinationPath.startsWith("META-INF/")) { //$NON-NLS-1$
					addWarning(ex.getMessage(), ex, status);
				}
			} else
				addWarning(Messages.format(JarPackagerMessagesCore.FatJarBuilder_error_readingArchiveFile,
						new Object[] { BasicElementLabels.getResourceName(zipFile.getName()), ex.getLocalizedMessage() }), ex, status);
		}
	}

	/**
	 * Write the entry to the destinationPath of the given JarOutputStream. Extracted from
	 * org.eclipse.jdt.internal.ui.jarpackagerfat.JarWriter4
	 *
	 * @param zipEntry the jar entry to write
	 * @param zipFile the zipFile to extract
	 * @param path the destinationPath in the jar file
	 * @param areDirectoryEntriesIncluded the directory entries are included
	 * @param isCompressed the jar is compressed
	 * @param jarOutputStream the destination JarOutputStream
	 * @param directories the temporary set saves existing directories
	 *
	 * @throws IOException If an I/O error occurred
	 *
	 * @since 1.14
	 */
	public static void addZipEntry(ZipEntry zipEntry, ZipFile zipFile, String path,
			boolean areDirectoryEntriesIncluded, boolean isCompressed,
			JarOutputStream jarOutputStream, Set<String> directories) throws IOException {
		if (areDirectoryEntriesIncluded) {
			addDirectories(path, jarOutputStream, directories);
		}
		JarEntry newEntry= new JarEntry(path.replace(File.separatorChar, '/'));

		if (isCompressed) {
			newEntry.setMethod(ZipEntry.DEFLATED);
			// Entry is filled automatically.
		} else {
			newEntry.setMethod(ZipEntry.STORED);
			newEntry.setSize(zipEntry.getSize());
			newEntry.setCrc(zipEntry.getCrc());
		}

		long lastModified= System.currentTimeMillis();

		// Set modification time
		newEntry.setTime(lastModified);
		try (InputStream content= zipFile.getInputStream(zipEntry);) {
			jarOutputStream.putNextEntry(newEntry);
			content.transferTo(jarOutputStream);
		}
	}

	/**
	 * Creates the directory entries for the given path and writes it to the current archive.
	 * Extracted from org.eclipse.jdt.ui.jarpackager.JarWriter3
	 *
	 * @param destPath the path to add
	 * @param jarOutputStream the destination JarOutputStream
	 * @param directories the temporary set saves existing directories
	 *
	 * @throws IOException if an I/O error has occurred
	 *
	 * @since 1.14
	 */
	public static void addDirectories(String destPath, JarOutputStream jarOutputStream, Set<String> directories) throws IOException {
		String path= destPath.replace(File.separatorChar, '/');
		int lastSlash= path.lastIndexOf('/');
		List<JarEntry> entryDirectories= new ArrayList<>(2);
		while (lastSlash != -1) {
			path= path.substring(0, lastSlash + 1);
			if (!directories.add(path)) {
				break;
			}
			JarEntry newEntry= new JarEntry(path);
			newEntry.setMethod(ZipEntry.STORED);
			newEntry.setSize(0);
			newEntry.setCrc(0);
			newEntry.setTime(System.currentTimeMillis());
			entryDirectories.add(newEntry);

			lastSlash= path.lastIndexOf('/', lastSlash - 1);
		}

		for (int i= entryDirectories.size() - 1; i >= 0; --i) {
			jarOutputStream.putNextEntry(entryDirectories.get(i));
		}
	}

	/**
	 * add a warning message into the MultiStatus.
	 *
	 * @param message the message to add
	 * @param error the reason of the message
	 * @param status the <code>MultiStatus</code> to write
	 *
	 * @since 1.14
	 */
	private final static void addWarning(String message, Throwable error, MultiStatus status) {
		status.add(new Status(IStatus.WARNING, JavaManipulation.ID_PLUGIN, IJavaStatusConstants.INTERNAL_ERROR, message, error));
	}

}

