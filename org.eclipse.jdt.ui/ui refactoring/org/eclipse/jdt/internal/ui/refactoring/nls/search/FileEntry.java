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

package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import org.eclipse.core.resources.IFile;

class FileEntry {

	private IFile fPropertiesFile;
	private String fMessage;

	public FileEntry(IFile propertiesFile, String message) {
		fPropertiesFile= propertiesFile;
		fMessage= message;
	}
	
	public IFile getPropertiesFile() {
		return fPropertiesFile;
	}

	public String getMessage() {
		return fMessage;
	}
	
	public String toString() {
		return fMessage;
	}
}
