/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.jdt.core.JavaModelException;

/**
 * Represents a generic text change in the workbench. Text changes work
 * on text files. The class provides method to retrieve the current content of
 * a text file and to retrieve a preview of the change.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public interface ITextChange extends IChange {

	/**
	 * Returns the text this change is working on.
	 * 
	 * @return the original text.
	 * @exception JavaModelException if text cannot be accessed
	 */
	public String getCurrentContent() throws JavaModelException;
	
	/**
	 * Returns a preview of the change without actually modifying the underlying text.
	 * 
	 * @return the change's preview
	 * @exception JavaModelException if the preview could not be created
	 */
	public String getPreview() throws JavaModelException;	
}