package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.IWorkbenchPart;

/**
 * Places the result of an evaluation in the debug inspector
 */
public class InspectAction extends org.eclipse.jdt.internal.debug.ui.display.InspectAction {

	public InspectAction(IWorkbenchPart workbenchPart) {
		super(JavaEditorMessages.getResourceBundle(), "Inspect.", workbenchPart); //$NON-NLS-1$
	}	
}
