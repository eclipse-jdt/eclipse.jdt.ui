/*
 * (c) Copyright IBM Corp. 2000, 2001, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jdt.core.ISourceRange;

/**
 * A file context can be used to annotate a </code>RefactoringStatusEntry<code> with
 * detailed information about an error detected in an <code>IFile</code>.
 */
public class FileContext extends Context {

	private IFile fFile;
	private ISourceRange fSourceRange;

	private FileContext(IFile file, ISourceRange range) {
		fFile= file;
		fSourceRange= range;
	}

	/**
	 * Creates an status entry context for the given file and source range
	 * 
	 * @param file the file that has caused the error
	 * @param range the source range of the error inside the given file
	 * @return the status entry context or <code>Context.NULL_CONTEXT</code> if the
	 * 	context cannot be created
	 */
	public static Context create(IFile file, ISourceRange range) {
		if (file == null)
			return NULL_CONTEXT;
		return new FileContext(file, range);
	}
	
	/**
	 * Creates an status entry context for the given file and source range
	 * 
	 * @param resource the resource that has caused the error
	 * @param range the source range of the error inside the given file
	 * @return the status entry context or <code>Context.NULL_CONTEXT</code> if the
	 * 	context cannot be created
	 */
	public static Context create(IResource resource, ISourceRange range) {
		if (resource instanceof IFile)
			return create((IFile)resource, range);
		return NULL_CONTEXT;
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

