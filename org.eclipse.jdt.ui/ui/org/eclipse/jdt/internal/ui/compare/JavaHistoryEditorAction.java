/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

/**
 * A special action that makes JavaHistryAction available inside a text editor.
 */
public class JavaHistoryEditorAction extends Action implements IUpdate {
	
	private JavaEditor fEditor;
	private IActionDelegate fAction;

	public JavaHistoryEditorAction(JavaEditor editor, IActionDelegate action, String text) {
		Assert.isNotNull(editor);
		Assert.isNotNull(action);
		fEditor= editor;
		fAction= action;
		setText(text);
	}
	
	public void run() {
		fAction.run(this);
	}
	
	public void update() {
		IJavaElement element= null;
		try {
			element= SelectionConverter.getElementAtOffset(fEditor);
		} catch (JavaModelException e) {
		}
		ISelection selection= StructuredSelection.EMPTY;
		if (element != null) {
			selection= new StructuredSelection(element);
		}
		fAction.selectionChanged(this, selection);
	}
}
