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

import java.util.ResourceBundle;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * Action that toggles its editor's smart typing mode.
 * @since 3.0
 */
public class ToggleSmartTypingAction extends TextEditorAction {
	/**
	 * Creates a new <code>ToggleSmartTypingAction</code>.
	 */
	public ToggleSmartTypingAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
		// make sure we get the check style right here, even if editor is null
		setChecked(false);
		updateEditor(false);
	}
	
	/*
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		updateEditor(true);			
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.TextEditorAction#setEditor(org.eclipse.ui.texteditor.ITextEditor)
	 */
	public void setEditor(ITextEditor editor) {
		super.setEditor(editor);
		update();		
	}

	/*
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		super.update();
		if (isEnabled()) {
			ITextEditor ed= getTextEditor();
			if (ed instanceof ITextEditorExtension) {
				if (((ITextEditorExtension)ed).isEditorInputReadOnly()) {
					setEnabled(false);
				}
			} else if (ed != null) {
				if (!ed.isEditable()) {
					setEnabled(false); 
				}
			}
		}
		updateEditor(false);
	}

	/**
	 * Updates the action ui and the editor.
	 * @param toggle if <code>true</code>, the editor's smart typing state is toggled.
	 */
	private void updateEditor(boolean toggle) {
		CompilationUnitEditor editor;
		ITextEditor ed= getTextEditor();
		if (ed instanceof CompilationUnitEditor) {
			editor= (CompilationUnitEditor)ed;
		} else return;
		
		boolean smart= editor.isSmartTyping();
		if (toggle) {
			smart= !smart;
			editor.setSmartTyping(smart);
		}
		setChecked(smart);
	}
}
