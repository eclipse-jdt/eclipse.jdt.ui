/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

public interface IUndoTextEdits {

	/**
	 * Add the <code>TextEdit</code>s managed by this undo object  to 
	 * the given text buffer editor.
	 * 
	 * @param manipulator the text manipulator performing the undo
	 */
	public void addTo(TextBufferEditor editor) throws CoreException;
}

