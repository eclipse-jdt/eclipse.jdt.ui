/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javaeditor;

/**
 * Defines action IDs for private JavaEditor actions.
 */
public interface IJavaEditorActionConstants {

	/**
	 * ID of the action to toggle the style of the presentation.
	 */
	public static final String TOGGLE_PRESENTATION= "togglePresentation"; //$NON-NLS-1$

	/**
	 * ID of the action to toggle the visibility of the text hover.
	 */
	public static final String TOGGLE_TEXT_HOVER= "toggleTextHover"; //$NON-NLS-1$
	
	
	// http://dev.eclipse.org/bugs/show_bug.cgi?id=18968
	/**
	 * ID of the toolbar action to go to the previous error.
	 */
	public static final String PREVIOUS_ERROR= "gotoPreviousError"; //$NON-NLS-1$
	
	// http://dev.eclipse.org/bugs/show_bug.cgi?id=18968
	/**
	 * ID of the toolbar action to go to the next error.
	 */
	public static final String NEXT_ERROR= "gotoNextError"; //$NON-NLS-1$
}
