/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Ferenc Hechler <ferenc_hechler@users.sourceforge.net> - [jar exporter] export directory entries in "Runnable JAR File" - https://bugs.eclipse.org/bugs/show_bug.cgi?id=243163
 *     Microsoft Corporation - moved some methods to JarPackagerUtilCore for jdt.core.manipulation use
 *******************************************************************************/
package org.eclipse.jdt.ui.jarpackager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.jarpackager.JarPackagerUtilCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerMessages;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;


/**
 * Creates a JAR file for the given JAR package data.
 * <p>
 * Clients may subclass.
 * </p>
 *
 * @see org.eclipse.jdt.ui.jarpackager.JarPackageData
 * @since 3.2
 */
public class JarWriter3 {

	private Set<String> fDirectories= new HashSet<>();

	private JarOutputStream fJarOutputStream;

	private JarPackageData fJarPackage;

	/**
	* Gets the fJarOutputStream of this JarWriter3
	*
	* @return the fJarOutputStream of this JarWriter3
	*
	* @since 3.22
	*/
	public JarOutputStream getJarOutputStream() {
		return fJarOutputStream;
	}

	/**
	* Gets the fDirectories of this JarWriter3
	*
	* @return the fDirectories of this JarWriter3
	*
	* @since 3.22
	*/
	public Set<String> getDirectories(){
		return fDirectories;
	}

	/**
	 * Creates an instance which is used to create a JAR based
	 * on the given JarPackage.
	 *
	 * @param jarPackage		the JAR specification
	 * @param parent			the shell used to display question dialogs,
	 *				 			or <code>null</code> if "false/no/cancel" is the answer
	 * 							and no dialog should be shown
	 * @throws	CoreException	to signal any other unusual termination.
	 * 							This can also be used to return information
	 * 							in the status object.
	 */
	public JarWriter3(JarPackageData jarPackage, Shell parent) throws CoreException {
		Assert.isNotNull(jarPackage, "The JAR specification is null"); //$NON-NLS-1$
		fJarPackage= jarPackage;
		Assert.isTrue(fJarPackage.isValid(), "The JAR package specification is invalid"); //$NON-NLS-1$
		if (!canCreateJar(parent))
			throw new OperationCanceledException("Cannot create JAR with path: " + fJarPackage.getAbsoluteJarLocation()); //$NON-NLS-1$

		try {
			if (fJarPackage.usesManifest() && fJarPackage.areGeneratedFilesExported()) {
				Manifest manifest= fJarPackage.getManifestProvider().create(fJarPackage);
				fJarOutputStream= new JarOutputStream(new BufferedOutputStream(new FileOutputStream(fJarPackage.getAbsoluteJarLocation().toFile())), manifest);
			} else
				fJarOutputStream= new JarOutputStream(new BufferedOutputStream(new FileOutputStream(fJarPackage.getAbsoluteJarLocation().toFile())));
			String comment= jarPackage.getComment();
			if (comment != null)
				fJarOutputStream.setComment(comment);
			if (fJarPackage.isRefactoringAware()) {
				Assert.isTrue(fJarPackage.areDirectoryEntriesIncluded());
				final IPath metaPath= new Path(JarPackagerUtil.getMetaEntry());
				addDirectories(metaPath);
				addHistory(fJarPackage, new Path(JarPackagerUtil.getRefactoringsEntry()), new NullProgressMonitor());
			}
		} catch (IOException exception) {
			throw JarPackagerUtil.createCoreException(exception.getLocalizedMessage(), exception);
		}
	}

	/**
	 * Creates the directory entries for the given path and writes it to the current archive.
	 *
	 * @param destinationPath the path to add
	 *
	 * @throws IOException if an I/O error has occurred
	 */
	protected void addDirectories(IPath destinationPath) throws IOException {
		addDirectories(destinationPath.toString());
	}

	/**
	 * Creates the directory entries for the given path and writes it to the current archive.
	 *
	 * @param destPath the path to add
	 *
	 * @throws IOException if an I/O error has occurred
	 * @since 3.5
	 */
	protected void addDirectories(String destPath) throws IOException {
		String path= destPath.replace(File.separatorChar, '/');
		int lastSlash= path.lastIndexOf('/');
		List<JarEntry> directories= new ArrayList<>(2);
		while (lastSlash != -1) {
			path= path.substring(0, lastSlash + 1);
			if (!fDirectories.add(path))
				break;

			JarEntry newEntry= new JarEntry(path);
			newEntry.setMethod(ZipEntry.STORED);
			newEntry.setSize(0);
			newEntry.setCrc(0);
			newEntry.setTime(System.currentTimeMillis());
			directories.add(newEntry);

			lastSlash= path.lastIndexOf('/', lastSlash - 1);
		}

		for (int i= directories.size() - 1; i >= 0; --i) {
			fJarOutputStream.putNextEntry(directories.get(i));
		}
	}

	/**
	 * Creates the directory entries for the given path and writes it to the
	 * current archive.
	 *
	 * @param resource
	 *            the resource for which the parent directories are to be added
	 * @param destinationPath
	 *            the path to add
	 *
	 * @throws IOException
	 *             if an I/O error has occurred
	 * @throws CoreException
	 *             if accessing the resource failes
	 */
	protected void addDirectories(IResource resource, IPath destinationPath) throws IOException, CoreException {
		IContainer parent= null;
		String path= destinationPath.toString().replace(File.separatorChar, '/');
		int lastSlash= path.lastIndexOf('/');
		List<JarEntry> directories= new ArrayList<>(2);
		while (lastSlash != -1) {
			path= path.substring(0, lastSlash + 1);
			if (!fDirectories.add(path))
				break;

			parent= resource.getParent();
			long timeStamp= System.currentTimeMillis();
			URI location= parent.getLocationURI();
			if (location != null) {
				IFileInfo info= EFS.getStore(location).fetchInfo();
				if (info.exists())
					timeStamp= info.getLastModified();
			}

			JarEntry newEntry= new JarEntry(path);
			newEntry.setMethod(ZipEntry.STORED);
			newEntry.setSize(0);
			newEntry.setCrc(0);
			newEntry.setTime(timeStamp);
			directories.add(newEntry);

			lastSlash= path.lastIndexOf('/', lastSlash - 1);
		}

		for (int i= directories.size() - 1; i >= 0; --i) {
			fJarOutputStream.putNextEntry(directories.get(i));
		}
	}

	/**
	 * Creates a new JarEntry with the passed path and contents, and writes it
	 * to the current archive.
	 *
	 * @param	resource			the file to write
	 * @param	path				the path inside the archive
	 *
     * @throws	IOException			if an I/O error has occurred
	 * @throws	CoreException 		if the resource can-t be accessed
	 */
	protected void addFile(IFile resource, IPath path) throws IOException, CoreException {
		JarEntry newEntry= new JarEntry(path.toString().replace(File.separatorChar, '/'));
		byte[] allBytes;
		try (InputStream contents= resource.getContents(false)) {
			allBytes= contents.readAllBytes();
		}
		if (fJarPackage.isCompressed()) {
			newEntry.setMethod(ZipEntry.DEFLATED);
			// Entry is filled automatically.
		} else {
			newEntry.setMethod(ZipEntry.STORED);
			JarPackagerUtil.setCrcAndSize(newEntry, allBytes);
		}

		long lastModified= System.currentTimeMillis();
		URI locationURI= resource.getLocationURI();
		if (locationURI != null) {
			IFileInfo info= EFS.getStore(locationURI).fetchInfo();
			if (info.exists())
				lastModified= info.getLastModified();
		}

		// Set modification time
		newEntry.setTime(lastModified);

		fJarOutputStream.putNextEntry(newEntry);
		fJarOutputStream.write(allBytes);
	}

	/**
	 * Write the given entry describing the given content to the
	 * current archive
	 *
	 * @param   entry            the entry to write
	 * @param   content          the content to write
	 *
	 * @throws IOException       If an I/O error occurred
	 *
	 * @since 3.4
	 */
	protected void addEntry(JarEntry entry, InputStream content) throws IOException {
		JarPackagerUtilCore.addEntry(entry, content, fJarOutputStream);
	}

	/**
	 * Creates a new JAR file entry containing the refactoring history.
	 *
	 * @param data
	 *            the jar package data
	 * @param path
	 *            the path of the refactoring history file within the archive
	 * @param monitor
	 *            the progress monitor to use
	 * @throws IOException
	 *             if no temp file could be written
	 * @throws CoreException
	 *             if an error occurs while transforming the refactorings
	 */
	private void addHistory(final JarPackageData data, final IPath path, final IProgressMonitor monitor) throws IOException, CoreException {
		Assert.isNotNull(data);
		Assert.isNotNull(path);
		Assert.isNotNull(monitor);
		final RefactoringDescriptorProxy[] proxies= data.getRefactoringDescriptors();
		Arrays.sort(proxies, (first, second) -> {
			final long delta= first.getTimeStamp() - second.getTimeStamp();
			if (delta > 0)
				return 1;
			else if (delta < 0)
				return -1;
			return 0;
		});
		File file= File.createTempFile("history", null); //$NON-NLS-1$
		try (OutputStream output= new BufferedOutputStream(new FileOutputStream(file))) {
			RefactoringCore.getHistoryService().writeRefactoringDescriptors(proxies, output, RefactoringDescriptor.NONE, false, monitor);
			writeMetaData(data, file, path);
		} catch (IOException exception) {
			// Do nothing
		} finally {
			file.delete();
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
			return parent != null && JarPackagerUtil.askForOverwritePermission(parent, fJarPackage.getAbsoluteJarLocation(), true);
		}

		// Test if directory exists
		String path= file.getAbsolutePath();
		int separatorIndex = path.lastIndexOf(File.separator);
		if (separatorIndex == -1) // i.e.- default directory, which is fine
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

	/**
	 * Closes the archive and does all required cleanup.
	 *
	 * @throws CoreException
	 *             to signal any other unusual termination. This can also be
	 *             used to return information in the status object.
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

	private void registerInWorkspaceIfNeeded() {
		IPath jarPath= fJarPackage.getAbsoluteJarLocation();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			// The Jar is always put into the local file system. So it can only be
			// part of a project if the project is local as well. So using getLocation
			// is currently save here.
			IPath projectLocation= project.getLocation();
			if (projectLocation != null && projectLocation.isPrefixOf(jarPath)) {
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

	/**
	 * Writes the passed resource to the current archive.
	 *
	 * @param resource
	 *            the file to be written
	 * @param destinationPath
	 *            the path for the file inside the archive
	 * @throws CoreException
	 *             to signal any other unusual termination. This can also be
	 *             used to return information in the status object.
	 */
	public void write(IFile resource, IPath destinationPath) throws CoreException {
		try {
			if (fJarPackage.areDirectoryEntriesIncluded())
				addDirectories(resource, destinationPath);
			addFile(resource, destinationPath);
		} catch (IOException ex) {
			// Ensure full path is visible
			String message= null;
			if (ex.getLocalizedMessage() != null)
				message= Messages.format(JarPackagerMessages.JarWriter_writeProblemWithMessage, new Object[] {BasicElementLabels.getPathLabel(resource.getFullPath(), false), ex.getLocalizedMessage()});
			else
				message= Messages.format(JarPackagerMessages.JarWriter_writeProblem, BasicElementLabels.getPathLabel(resource.getFullPath(), false));
			throw JarPackagerUtil.createCoreException(message, ex);
		}
	}

	/**
	 * Writes the meta file to the JAR file.
	 *
	 * @param data
	 *            the jar package data
	 * @param file
	 *            the file containing the meta data
	 * @param path
	 *            the path of the meta file within the archive
	 * @throws FileNotFoundException
	 *             if the meta file could not be found
	 * @throws IOException
	 *             if an input/output error occurs
	 */
	private void writeMetaData(final JarPackageData data, final File file, final IPath path) throws FileNotFoundException, IOException {
		Assert.isNotNull(data);
		Assert.isNotNull(file);
		Assert.isNotNull(path);
		final JarEntry entry= new JarEntry(path.toString().replace(File.separatorChar, '/'));
		byte[] allBytes= Files.readAllBytes(file.toPath());
		if (data.isCompressed())
			entry.setMethod(ZipEntry.DEFLATED);
		else {
			entry.setMethod(ZipEntry.STORED);
			JarPackagerUtil.setCrcAndSize(entry, allBytes);
		}
		entry.setTime(System.currentTimeMillis());
		fJarOutputStream.putNextEntry(entry);
		fJarOutputStream.write(allBytes);
	}
}
