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
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.core.resources.IFile;

import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * A special action that makes JavaHistoryAction available inside a text editor.
 */
public class JavaHistoryEditorAction extends Action implements IUpdate {
	
	private JavaEditor fEditor;
	private JavaHistoryAction fAction;
	private String fTitle;
	private String fMessage;

	public JavaHistoryEditorAction(JavaEditor editor, JavaHistoryAction action, String text, String title, String message) {
		Assert.isNotNull(editor);
		Assert.isNotNull(action);
		Assert.isNotNull(title);
		Assert.isNotNull(message);
		fEditor= editor;
		fAction= action;
		fTitle= title;
		fMessage= message;
		setText(text);
		setEnabled(checkEnabled());
	}

	public void run() {
		updateDelegate();
		if (!isEnabled()) {
			MessageDialog.openInformation(fEditor.getEditorSite().getShell(), fTitle, fMessage);
			return;
		}
		fAction.run(this);
	}

	public void update() {
		setEnabled(checkEnabled());
	}
	
	private void updateDelegate() {
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
	
	private boolean checkEnabled() {
		ICompilationUnit unit= SelectionConverter.getInputAsCompilationUnit(fEditor);
		IFile file= fAction.getFile(unit);
		return fAction.isEnabled(file);
	}	
}
