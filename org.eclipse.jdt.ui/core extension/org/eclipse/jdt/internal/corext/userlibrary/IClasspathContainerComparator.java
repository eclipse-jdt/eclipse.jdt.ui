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
package org.eclipse.jdt.internal.corext.userlibrary;

import org.eclipse.core.runtime.IPath;

/**
 * Determines if container entries are duplicates/redundant on a build and runtime
 * classpath. If an <code>IClasspathContainer</code> implements this interface,
 * the <code>isDuplicate</code> method is used to determine if containers are
 * duplicates/redundant. Otherwise, containers with the path are considered duplicates.
 * 
 * @since 3.0
 */
public interface IClasspathContainerComparator {
	
	/**
	 * Returns whether this container is a duplicate of the container
	 * identified by the given path.
	 * 
	 * @param containerPath the container to compare against
	 * @return whether this container is a duplicate of the conatiner
	 * identified by the given path
	 */
	public boolean isDuplicate(IPath containerPath);

}
