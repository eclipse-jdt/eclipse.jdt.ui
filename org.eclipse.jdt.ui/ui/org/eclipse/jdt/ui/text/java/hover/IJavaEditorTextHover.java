package org.eclipse.jdt.ui.text.java.hover;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.ITextHover;

import org.eclipse.ui.IEditorPart;

/**
 * Provides a hover popup which appears on top of an editor with relevant
 * display information. If the text hover does not provide information no
 * hover popup is shown.<p>
 * Clients may implement this interface.
 *
 * @see IEditorPart
 * @see ITextHover
 */
public interface IJavaEditorTextHover extends ITextHover {

	/**
	 * Sets the editor for which the hover is shown.
	 * 
	 * @param editor the editor on which the hover popup should be shown
	 */
	void setEditor(IEditorPart editor);

}

