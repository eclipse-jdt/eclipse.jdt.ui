/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaStatusConstants;

/**
 * Creates a JAR file
 */
public class JarWriter {

	private JarOutputStream fJarOutputStream;
	private JarPackage fJarPackage;

	/**
	 * Creates an instance which is used to create a JAR based
	 * on the given JarPackage
	 *
	 * @param	jarPackage the JAR specification
	 * @param	parent	the parent for the dialog,
	 * 			or <code>null</code> if no dialog should be presented
	 * @throws	java.io.IOException
	 * @throws	org.eclipse.core.runtime.CoreException	if the supplied manifest can't be read
	 * @throws	org.eclipse.core.runtime.OperationCanceledException
	 * 				if user selected not to overwrite the existing JAR
	 */
	public JarWriter(JarPackage jarPackage, Shell parent) throws IOException, CoreException, OperationCanceledException {
		Assert.isNotNull(jarPackage, "The JAR specification is null"); //$NON-NLS-1$
		fJarPackage= jarPackage;
		Assert.isTrue(fJarPackage.isValid(), "The JAR package specification is invalid"); //$NON-NLS-1$
		if (!fJarPackage.canCreateJar(parent))
			throw new OperationCanceledException();
		if (fJarPackage.usesManifest() && fJarPackage.areClassFilesExported()) {
			Manifest manifest=  fJarPackage.getManifestProvider().create(fJarPackage);
			fJarOutputStream= new JarOutputStream(new FileOutputStream(fJarPackage.getJarLocation().toOSString()), manifest);
		} else
			fJarOutputStream= new JarOutputStream(new FileOutputStream(fJarPackage.getJarLocation().toOSString()));
	}

	/**
	 * Creates an instance which is used to create a JAR based
	 * on the given JarPackage
	 *
	 * @param jarPackage the JAR specification
	 * @throws	java.io.IOException
	 * @throws	org.eclipse.core.runtime.CoreException
	 */
	public JarWriter(JarPackage jarPackage) throws IOException, CoreException {
		this(jarPackage, null);
	}

	/**
	 * Does all required cleanup
	 *
	 * @exception java.io.IOException
	 */
	public void close() throws IOException {
		if (fJarOutputStream != null)
			fJarOutputStream.close();
	}

	/**
	 * Writes the passed resource to the current archive
	 *
	 * @param resource org.eclipse.core.resources.IFile
	 * @param destinationPath java.lang.String
	 * @exception java.io.IOException
	 * @exception org.eclipse.core.runtime.CoreException
	 */
	public void write(IFile resource, IPath destinationPath) throws IOException, CoreException {
		ByteArrayOutputStream output= null;
		BufferedInputStream contentStream= null;
		
		try {
			output= new ByteArrayOutputStream();
			if (!resource.isLocal(IResource.DEPTH_ZERO))
				throw new IOException(JarPackagerMessages.getFormattedString("JarWriter.error.fileNotAccessible", resource.getFullPath())); //$NON-NLS-1$
			contentStream= new BufferedInputStream(resource.getContents(false));
			int chunkSize= 4096;
			byte[] readBuffer= new byte[chunkSize];
			int count;
			while ((count= contentStream.read(readBuffer, 0, chunkSize)) > 0)
				output.write(readBuffer, 0, count);
		} finally {
			if (output != null)
				output.close();
			if (contentStream != null)
				contentStream.close();
		}

		try {
			IPath fileLocation= resource.getLocation();
			long lastModified= System.currentTimeMillis();
			if (fileLocation != null) {
				File file= new File(resource.getLocation().toOSString());
				if (file.exists())
					lastModified= file.lastModified();
			}
			write(destinationPath, output.toByteArray(), lastModified);
		} catch (IOException ex) {
			// Ensure full path is visible
			String message= JarPackagerMessages.getFormattedString("JarWriter.writeProblem", resource.getFullPath()); //$NON-NLS-1$
			if (ex.getMessage() != null)
				message += ": " + ex.getMessage();  //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 
											JavaStatusConstants.INTERNAL_ERROR, message, ex));
		}		
	}

	/**
	 * Creates a new JarEntry with the passed pathname and contents, and write it
	 * to the current archive
	 *
	 * @param pathname java.lang.String
	 * @param contents byte[]
	 * @exception java.io.IOException
	 */
	protected void write(IPath path, byte[] contents, long lastModified) throws IOException, CoreException {
		JarEntry newEntry= new JarEntry(path.toString().replace(File.separatorChar, '/'));

		if (fJarPackage.isCompressed())
			newEntry.setMethod(JarEntry.DEFLATED);
			// Entry is filled automatically.
		else {
			newEntry.setMethod(JarEntry.STORED);
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
}
