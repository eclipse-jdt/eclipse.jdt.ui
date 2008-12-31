/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import org.eclipse.jface.action.IStatusLineManager;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.ui.editors.text.TextEditorActionContributor;

import org.eclipse.jdt.ui.actions.JdtActionConstants;


/**
 * Action contributor for Properties file editor.
 *
 * @since 3.1
 */
public class PropertiesFileEditorActionContributor extends TextEditorActionContributor {


	/*
	 * @see EditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);

		IActionBars actionBars= getActionBars();
		IStatusLineManager manager= actionBars.getStatusLineManager();
		manager.setMessage(null);
		manager.setErrorMessage(null);

		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor)part;

		actionBars.setGlobalActionHandler(JdtActionConstants.OPEN, getAction(textEditor, JdtActionConstants.OPEN));
	}

	/*
	 * @see IEditorActionBarContributor#dispose()
	 */
	public void dispose() {
		setActiveEditor(null);
		super.dispose();
	}
}
