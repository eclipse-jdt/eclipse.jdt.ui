/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.text;

import org.eclipse.jdt.core.refactoring.IChange;

/**
 * Common interface for all text buffer changes. A text buffer change operates on <code>ITextBuffer
 * </code> and groups together various <codeSimpleTextChange</code> objects.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public interface ITextBufferChange extends IChange {

	/**
	 * Replaces the text [offset, length] with the given text.
	 * @param name the changes name. The name is mainly used to render the change in the 
	 *  user interface. The name can be <code>null</code> indicating that the change
	 *  doesn't have a name.
	 * @param offset the starting offset of the text to be replaced. The offset must not
	 *  be negative.
	 * @param length the length of the text to be replaced. The length must not be negative.
	 * @param text the new text.
	 */
	public void addReplace(String name, int offset, int length, String text);
	
	/**
	 * Deletes the text [offset, length].
	 * @param name the changes name. The name is mainly used to render the change in the 
	 *  user interface. The name can be <code>null</code> indicating that the change
	 *  doesn't have a name.
	 * @param pos the starting offset of the text to be deleted. The offset must not be
	 *  negative.
	 * @param length the length of the text to be deleted. The length must not be negative.
	 */
	public void addDelete(String name, int offset, int length);
	
	/**
	 * Inserts the given text a the given pos.
	 * @param name the changes name. The name is mainly used to render the change in the 
	 *  user interface. The name can be <code>null</code> indicating that the change
	 *  doesn't have a name.
	 * @param offset the offset where the new text is to be inserted. The offset must not
	 * be negative.
	 * @param text the text to be inserted.
	 */
	public void addInsert(String name, int offset, String text);
	
	/**
	 * Moves the text [pos, length] to the new position to.	
	 * @param name the changes name. The name is mainly used to render the change in the 
	 *  user interface. The name can be <code>null</code> indicating that the change
	 *  doesn't have a name.
	 * @param offset the starting offset of the text to be moved. The offset must not
	 *  be negative.
	 * @param length the length of the new text to be moved. The length must not be
	 *  negative.
	 * @param to the target position. The target position must not be negative.
	 */
	public void addMove(String name, int offset, int length, int to);
	
	/**
	 * Adds a simple text change object to this text modifier.
	 *
	 * @param change the simple text change to add. The change must not be <code>null</code>.
	 */
	public void addSimpleTextChange(SimpleTextChange change);
	
	/**
	 * Returns <code>true</code> if no change has been added to the receiver by calling 
	 * <code>addReplace</code>, <code>addDelete</code>, <code>addInsert</code>,
	 * <code>addMove</code> or <code>addSimpleTextChange</code>. Returns <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> iff nothing has been added to the receiver.
	 */
	public boolean isEmpty();
}