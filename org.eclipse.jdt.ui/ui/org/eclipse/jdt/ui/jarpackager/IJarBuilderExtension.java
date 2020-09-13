/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.jdt.ui.jarpackager;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;


/**
 * Extends {@link org.eclipse.jdt.ui.jarpackager.IJarBuilder} with the following functions:
 * <ul>
 * <li>Write a file to the JAR to build</li>
 * </ul>
 *
 * @since 3.5
 */
public interface IJarBuilderExtension {

	/**
	 * Add the given file to the archive at the given path
	 *
	 * @param file
	 *        the file to be written. It is guaranteed, that the file is not a directory.
	 * @param destinationPath
	 *        the path for the file inside the archive
	 * @throws CoreException
	 *        thrown when the file could not be written
	 */
	void writeFile(File file, IPath destinationPath) throws CoreException;

}
