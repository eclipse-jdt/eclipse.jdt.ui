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
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.ui.actions.JdtActionConstants;

public class CompilationUnitEditorActionContributor extends BasicEditorActionContributor {
	
	public CompilationUnitEditorActionContributor() {
		super();				
	}

	/*
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		
		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;
		 
		// Source menu.
		IActionBars bars= getActionBars();		
		bars.setGlobalActionHandler(JdtActionConstants.COMMENT, getAction(textEditor, "Comment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.UNCOMMENT, getAction(textEditor, "Uncomment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.TOGGLE_COMMENT, getAction(textEditor, "ToggleComment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.FORMAT, getAction(textEditor, "Format")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.ADD_BLOCK_COMMENT, getAction(textEditor, "AddBlockComment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.REMOVE_BLOCK_COMMENT, getAction(textEditor, "RemoveBlockComment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.INDENT, getAction(textEditor, "Indent")); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
