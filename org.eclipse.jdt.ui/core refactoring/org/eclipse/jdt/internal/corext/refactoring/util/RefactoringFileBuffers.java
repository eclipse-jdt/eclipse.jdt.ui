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
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.jdt.core.ICompilationUnit;

/**
 * Helper methods to deal with file buffer in the refactoring area.
 */
public class RefactoringFileBuffers {
	
	public static ITextFileBuffer getTextFileBuffer(ICompilationUnit unit) throws CoreException {
		IResource resource= unit.getResource();
		if (resource == null || resource.getType() != IResource.FILE)
			return null;
		IFile file= (IFile)resource;
		return FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath());
	}
}
