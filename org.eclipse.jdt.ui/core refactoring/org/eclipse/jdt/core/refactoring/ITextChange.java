/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.core.refactoring;

import org.eclipse.jdt.core.JavaModelException;

/**
 * Represents a generic text change in the workbench. Text changes work
 * on text files. The provide method to retrieve the current content of
 * a text file and to retrieve a preview.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public interface ITextChange extends IChange {

	/**
	 * Returns the current text content this change is working on.
	 */
	public String getCurrentContent() throws JavaModelException;
	
	/**
	 * Returns a preview of the text content after performing this change
	 * without actually modifying the underlying text.
	 */
	public String getPreview() throws JavaModelException;	
}