/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * A file context can be used to annotate a </code>RefactoringStatusEntry<code> with
 * detailed information about an error detected in an <code>IFile</code>.
 */
public class FileStatusContext extends Context {

	private IFile fFile;
	private ISourceRange fSourceRange;

	/**
	 * Creates an status entry context for the given file and source range
	 * 
	 * @param file the file that has caused the error. Must not be <code>
	 *  null</code>
	 * @param range the source range of the error inside the given file or
	 *  <code>null</code> if now source range is known
	 */
	public FileStatusContext(IFile file, ISourceRange range) {
		Assert.isNotNull(file);
		fFile= file;
		fSourceRange= range;
	}

	/**
	 * Returns the context's file.
	 * 
	 * @return the context's file
	 */
	public IFile getFile() {
		return fFile;
	}
	
	/**
	 * Returns the context's source range
	 * 
	 * @return the context's source range
	 */
	public ISourceRange getSourceRange() {
		return fSourceRange;
	}
	
	/* (non-Javadoc)
	 * Method declared on Context.
	 */
	public IAdaptable getCorrespondingElement() {
		return getFile();
	}	
}

