/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

public abstract class TextEdit {
	
	/**
	 * Connects this text edit to the given <code>TextBuffer</code>. A 
	 * text edit must not keep a reference to the passed text buffer. It
	 * is guaranteed that the buffer passed to <code>perform<code> is
	 * equal to the buffer passed to this method. But they don't have to
	 * be identical.
	 * <p>
	 * Note that this method <b>should only be called</b> by <code>
	 * TextBuffer</code>.
	 *<p>
	 * This default implementation does nothing. Subclasses may override
	 * if needed.
	 *  
	 * @param buffer the text buffer this text edit will work on
	 */
	public void connect(TextBuffer buffer) throws CoreException {
		// does nothing
	}
	
	/**
	 * Returns the <code>TextPositions</code> that this text edit is going to
	 * manipulate. If this method is called before the <code>TextEdit</code>
	 * has been added to a <code>TextBuffer</code> it may return <cod>
	 * null</code> to indicate this situation.
	 * 
	 * @return the <code>TextPosition</code>s this <code>TextEdit is going
	 * 	to manipulate
	 */
	public abstract TextPosition[] getTextPositions();
	
	/**
	 * Performs the text edit. Note that this method <b>should only be called</b> 
	 * by <code>TextBuffer</code>. 
	 * 
	 * @param buffer the actual buffer to manipulate
	 * @return a text edit that can undo this text edit
	 */
	public abstract TextEdit perform(TextBuffer buffer) throws CoreException;
	
	/**
	 * This method gets called after all <code>TextEdit</code>s added to a text buffer
	 * are executed. Implementors of this method can do some clean-up or can release
	 * allocated resources that are now longer needed.
	 * <p>
	 * This default implementation does nothing.
	 */
	public void performed() {
		// do nothing
	}
		
	/**
     * Creates and returns a copy of this object. The copy method should
     * be implemented in a way so that the copy can be added to a different 
     * <code>TextBuffer</code> without causing any harm to the object 
     * from which the copy has been created.
     * 
     * @return a copy of this object.
     */
	public abstract TextEdit copy();	
	
	/**
	 * Returns the language element modified by this text edit. The method
	 * may return <code>null</code> if the modification isn't related to a
	 * language element or if the content of the modified text buffer doesn't
	 * follow any syntax.
	 * <p>
	 * This default implementation returns <code>null</code>
	 * 
	 * @return the language element modified by this text edit
	 */
	public Object getModifiedLanguageElement() {
		return null;
	}	
}

