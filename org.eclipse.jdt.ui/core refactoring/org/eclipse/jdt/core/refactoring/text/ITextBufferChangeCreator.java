/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.core.refactoring.text;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

/**
 * An interface that allows lazy creation of a text buffer change without knowing how <code>
 * ITextBufferChange</code> is actually implemented.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public interface ITextBufferChangeCreator {

	/**
	 * Creates a new <code>ITextBufferChange</code> object. This object can be used to
	 * make changes to the given compilation unit.
	 *
	 * @param name the name of the change to be created.
	 * @param cunit the compilation unit the text buffer change will work on. The
	 *  given value must not be <code>null</code>.
	 */
	public ITextBufferChange create(String name, ICompilationUnit cunit) throws JavaModelException;
	
}