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

import org.eclipse.jdt.launching.IRuntimeContainerComparator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 *
 */
public class UserLibraryClasspathContainer implements IClasspathContainer, IRuntimeContainerComparator {
	
	public static final String CONTAINER_ID= "org.eclipse.jdt.USER_LIBRARY"; //$NON-NLS-1$
	
	private String name;
	
	public UserLibraryClasspathContainer(String libName) {
		name= libName;
	}
	
	private UserLibrary getUserLibrary() {
		return UserLibraryManager.getUserLibrary(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	public IClasspathEntry[] getClasspathEntries() {
		UserLibrary library= getUserLibrary();
		if (library != null) {
			return library.getEntries();
		}
		return new IClasspathEntry[0];
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
	 */
	public int getKind() {
		UserLibrary library= getUserLibrary();
		if (library != null && library.isSystemLibrary()) {
			return K_SYSTEM;
		}
		return K_APPLICATION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
	 */
	public IPath getPath() {
		return new Path(CONTAINER_ID).append(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IRuntimeContainerComparator#isDuplicate(org.eclipse.core.runtime.IPath)
	 */
	public boolean isDuplicate(IPath containerPath) {
		return getPath().equals(containerPath);
	}
}
