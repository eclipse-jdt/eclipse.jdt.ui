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
package org.eclipse.text.edits;

/**
 * A source modifier can be used to modify the source of
 * a move or copy edit before it gets inserted at the target 
 * position. This is useful if the text to be  copied has to 
 * be modified before it is inserted without changing the 
 * original source.
 */
public interface ISourceModifier {
	/**
	 * Implementors of this interface can add additional edits
	 * to the passed root edit. 
	 * 
	 * @param source the source to be copied or moved
	 * @param root the root edit containing the edits specified
	 *  for the source text to be copied or moved. Implementors
	 *  are allowed to add additional edits to the edit tree. They
	 *  must not remove any edits from the tree.
	 */
	public void addEdits(String source, TextEdit root);
	
	/**
	 * Creates a copy of this source modifier object. The copy will
	 * be used in a different text edit object. So it should be 
	 * created in a way that is doesn't conflict with other text edits
	 * refering to this source modifier.
	 */
	public ISourceModifier copy();
}
