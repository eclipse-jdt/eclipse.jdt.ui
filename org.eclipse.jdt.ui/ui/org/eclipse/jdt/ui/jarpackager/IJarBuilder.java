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
 *******************************************************************************/
package org.eclipse.jdt.ui.jarpackager;

import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.widgets.Shell;



/**
 * A jar builder can be used to add elements to a
 * jar file which is about to be build.
 * <p>
 * The protocol defined by this interface is:
 * <ul>
 * <li>open is called</li>
 * <li>addFile and addJar is called multiple times</li>
 * <li>close is called</li>
 * </ul>
 * It is guaranteed that addFile and addJar is only called after
 * open is called and before close is called. Other methods may
 * be called any time.</p>
 * Implementors must be prepared that an instance if the implementation
 * is reused multiple times.<p>
 * 
 * <strong>EXPERIMENTAL</strong> This class or interface has been added as part
 * of a work in progress. This API may change at any given time. Please do not
 * use this API without consulting with the JDT/UI team. See bug 83258 for discussions.
 * 
 * @see org.eclipse.jdt.ui.jarpackager.JarPackageData
 * @since 3.4
 */
public interface IJarBuilder {

	/**
	 * @return the unique id of this builder
	 */
	public String getId();

	/**
	 * @return the manifest provider to build the manifest
	 */
	public IManifestProvider getManifestProvider();

	/**
	 * Called when building of the jar starts
	 * 
	 * @param jarPackage
	 *        the package to build
	 * @param shell
	 *        shell to show dialogs, <b>null</b> if no dialog must be shown
	 * @param status
	 *        a status to use to report status to the user
	 * @throws CoreException
	 */
	public void open(JarPackageData jarPackage, Shell shell, MultiStatus status) throws CoreException;

	/**
	 * Add the given resource to the archive at the given path
	 * 
	 * @param resource
	 *        the file to be written
	 * @param destinationPath
	 *        the path for the file inside the archive
	 * @throws CoreException 
	 */
	public void writeFile(IFile resource, IPath destinationPath) throws CoreException;

	/**
	 * Add the given archive to the archive which is about to be build
	 * 
	 * @param archive
	 *        the archive to add
	 * @param monitor
	 *        a monitor to report progress to
	 */
	public void writeArchive(ZipFile archive, IProgressMonitor monitor);

	/**
	 * Called when building of the jar finished.
	 * 
	 * @throws CoreException
	 */
	public void close() throws CoreException;

}
