/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.jarpackager;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerMessages;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;

/**
 * Creates a JAR file for the given JAR package data.
 * 
 * Clients may subclass.
 * 
 * @see org.eclipse.jdt.ui.jarpackager.JarPackageData
 * @since 2.0
 */
public class JarWriter {
	private JarOutputStream fJarOutputStream;
	private JarPackageData fJarPackage;
	
	/**
	 * Creates an instance which is used to create a JAR based
	 * on the given JarPackage.
	 *
	 * @param jarPackage		the JAR specification
	 * @param parent			the shell used to display question dialogs,
	 *				 			or <code>null</code> if "false/no/cancel" is the answer
	 * 							and no dialog should be shown
	 * @throws	CoreException	to signal any other unusual termination.
	 * 								This can also be used to return information
	 * 								in the status object.
	 */
	public JarWriter(JarPackageData jarPackage, Shell parent) throws CoreException {
		Assert.isNotNull(jarPackage, "The JAR specification is null"); //$NON-NLS-1$
		fJarPackage= jarPackage;
		Assert.isTrue(fJarPackage.isValid(), "The JAR package specification is invalid"); //$NON-NLS-1$
		if (!canCreateJar(parent))
			throw new OperationCanceledException();

		try {
			if (fJarPackage.usesManifest() && fJarPackage.areGeneratedFilesExported()) {
				Manifest manifest=  fJarPackage.getManifestProvider().create(fJarPackage);
				fJarOutputStream= new JarOutputStream(new FileOutputStream(fJarPackage.getAbsoluteJarLocation().toOSString()), manifest);
			} else
				fJarOutputStream= new JarOutputStream(new FileOutputStream(fJarPackage.getAbsoluteJarLocation().toOSString()));
			String comment= jarPackage.getComment();
			if (comment != null)
				fJarOutputStream.setComment(comment);
		} catch (IOException ex) {
			throw JarPackagerUtil.createCoreException(ex.getLocalizedMessage(), ex);
		}
	}
	
	/**
	 * Closes the archive and does all required cleanup.
	 *
	 * @throws	CoreException	to signal any other unusual termination.
	 * 								This can also be used to return information
	 * 								in the status object.
	 */
	public void close() throws CoreException {
		if (fJarOutputStream != null)
			try {
				fJarOutputStream.close();
				registerInWorkspaceIfNeeded();
			} catch (IOException ex) {
				throw JarPackagerUtil.createCoreException(ex.getLocalizedMessage(), ex);
			}
	}
	
	/**
	 * Writes the passed resource to the current archive.
	 *
	 * @param resource			the file to be written
	 * @param destinationPath	the path for the file inside the archive
	 * @throws	CoreException	to signal any other unusual termination.
	 * 								This can also be used to return information
	 * 								in the status object.
	 */
	public void write(IFile resource, IPath destinationPath) throws CoreException {
		ByteArrayOutputStream output= null;
		BufferedInputStream contentStream= null;
		
		try {
			output= new ByteArrayOutputStream();
			if (!resource.isLocal(IResource.DEPTH_ZERO)) {
				String message= JarPackagerMessages.getFormattedString("JarWriter.error.fileNotAccessible", resource.getFullPath()); //$NON-NLS-1$
				throw JarPackagerUtil.createCoreException(message, null);
			}
			contentStream= new BufferedInputStream(resource.getContents(false));
			int chunkSize= 4096;
			byte[] readBuffer= new byte[chunkSize];
			int count;
			while ((count= contentStream.read(readBuffer, 0, chunkSize)) != -1)
				output.write(readBuffer, 0, count);
		} catch (IOException ex) {
			throw JarPackagerUtil.createCoreException(ex.getLocalizedMessage(), ex);
		} finally {
			try {
				if (output != null)
					output.close();
				if (contentStream != null)
					contentStream.close();
			} catch (IOException ex) {
				throw JarPackagerUtil.createCoreException(ex.getLocalizedMessage(), ex);
			}
		}
		try {
			IPath fileLocation= resource.getLocation();
			long lastModified= System.currentTimeMillis();
			if (fileLocation != null) {
				File file= new File(fileLocation.toOSString());
				if (file.exists())
					lastModified= file.lastModified();
			}
			write(destinationPath, output.toByteArray(), lastModified);
		} catch (IOException ex) {
			// Ensure full path is visible
			String message= null;
			if (ex.getLocalizedMessage() != null)
				message= JarPackagerMessages.getFormattedString("JarWriter.writeProblemWithMessage", resource.getFullPath(), ex.getLocalizedMessage()); //$NON-NLS-1$;
			else
				message= JarPackagerMessages.getFormattedString("JarWriter.writeProblem", resource.getFullPath()); //$NON-NLS-1$
			throw JarPackagerUtil.createCoreException(message, ex);
		}					
	}
	
	/**
	 * Creates a new JarEntry with the passed path and contents, and writes it
	 * to the current archive.
	 *
	 * @param	path			the path inside the archive
	 * @param	contents		the bytes to write
	 * @param	lastModified	a long which represents the last modification date
     * @throws	IOException		if an I/O error has occurred
	 */
	protected void write(IPath path, byte[] contents, long lastModified) throws IOException {
		JarEntry newEntry= new JarEntry(path.toString().replace(File.separatorChar, '/'));
		if (fJarPackage.isCompressed())
			newEntry.setMethod(ZipEntry.DEFLATED);
			// Entry is filled automatically.
		else {
			newEntry.setMethod(ZipEntry.STORED);
			newEntry.setSize(contents.length);
			CRC32 checksumCalculator= new CRC32();
			checksumCalculator.update(contents);
			newEntry.setCrc(checksumCalculator.getValue());
		}
		
		// Set modification time
		newEntry.setTime(lastModified);

		try {		
			fJarOutputStream.putNextEntry(newEntry);
			fJarOutputStream.write(contents);
		} finally  {
			/*
			 * Commented out because some JREs throw an NPE if a stream
			 * is closed twice. This works because
			 * a) putNextEntry closes the previous entry
			 * b) closing the stream closes the last entry
			 */
			// fJarOutputStream.closeEntry();
		}
	}
	
	/**
	 * Checks if the JAR file can be overwritten.
	 * If the JAR package setting does not allow to overwrite the JAR
	 * then a dialog will ask the user again.
	 * 
	 * @param	parent	the parent for the dialog,
	 * 			or <code>null</code> if no dialog should be presented
	 * @return	<code>true</code> if it is OK to create the JAR
	 */
	protected boolean canCreateJar(Shell parent) {
		File file= fJarPackage.getAbsoluteJarLocation().toFile();
		if (file.exists()) {
			if (!file.canWrite())
				return false;
			if (fJarPackage.allowOverwrite())
				return true;
			return parent != null && JarPackagerUtil.askForOverwritePermission(parent, fJarPackage.getAbsoluteJarLocation().toOSString());
		}
					
		// Test if directory exists
		String path= file.getAbsolutePath();
		int separatorIndex = path.lastIndexOf(File.separator);
		if (separatorIndex == -1) // ie.- default dir, which is fine
			return true;
		File directory= new File(path.substring(0, separatorIndex));
		if (!directory.exists()) {
			if (JarPackagerUtil.askToCreateDirectory(parent, directory))
				return directory.mkdirs();
			else
				return false;
		}
		return true;
	}

	private void registerInWorkspaceIfNeeded() {
		IPath jarPath= fJarPackage.getAbsoluteJarLocation();
		IProject[] projects= ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i= 0; i < projects.length; i++) {
			IProject project= projects[i];
			IPath projectLocation= project.getLocation();
			boolean isInProject= projectLocation != null && projectLocation.isPrefixOf(jarPath);
			if (isInProject) {
				try {
					jarPath= jarPath.removeFirstSegments(projectLocation.segmentCount());
					jarPath= jarPath.removeLastSegments(1);
					IResource containingFolder= project.findMember(jarPath);
					if (containingFolder != null && containingFolder.isAccessible())
						containingFolder.refreshLocal(IResource.DEPTH_ONE, null);
				} catch (CoreException ex) {
					// don't refresh the folder but log the problem
					JavaPlugin.log(ex);
				}
			}
		}
	}
}
