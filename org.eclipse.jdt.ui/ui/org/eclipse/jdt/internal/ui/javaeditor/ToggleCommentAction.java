/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * An action which toggles the single line comment prefixes on the selected lines.
 * 
 * @since 3.0
 */
public final class ToggleCommentAction extends TextEditorAction {
	
	/** The text operation target */
	private ITextOperationTarget fOperationTarget;
	/** The comment prefixes */
	private String[] fCommentPrefixes;
	
	/**
	 * Creates and initializes the action for the given text editor. The action
	 * configures its visual representation from the given resource bundle.
	 *
	 * @param bundle the resource bundle
	 * @param prefix a prefix to be prepended to the various resource keys
	 *   (described in <code>ResourceAction</code> constructor), or 
	 *   <code>null</code> if none
	 * @param editor the text editor
	 * @see ResourceAction#ResourceAction
	 */
	public ToggleCommentAction(ResourceBundle bundle, String prefix, ITextEditor editor, String[] commentPrefixes) {
		super(bundle, prefix, editor);
		fCommentPrefixes= commentPrefixes;
	}
	
	/**
	 * Implementation of the <code>IAction</code> prototype. Checks if the selected
	 * lines are all commented or not and uncomment/comments them respectively.
	 */
	public void run() {
		if (fOperationTarget == null)
			return;
			
		ITextEditor editor= getTextEditor();
		if (!(editor instanceof JavaEditor))
			return;

		if (!validateEditorInputState())
			return;
		
		final int operationCode;
		if (isSelectionCommented(editor.getSelectionProvider().getSelection()))
			operationCode= ITextOperationTarget.STRIP_PREFIX;
		else
			operationCode= ITextOperationTarget.PREFIX;
		
		Shell shell= editor.getSite().getShell();
		if (!fOperationTarget.canDoOperation(operationCode)) {
			if (shell != null)
				MessageDialog.openError(shell, JavaEditorMessages.getString("ToggleComment.error.title"), JavaEditorMessages.getString("ToggleComment.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		
		Display display= null;
		if (shell != null && !shell.isDisposed()) 
			display= shell.getDisplay();
	
		BusyIndicator.showWhile(display, new Runnable() {
			public void run() {
				fOperationTarget.doOperation(operationCode);
			}
		});
	}
	
	/**
	 * Is the given selection single-line commented?
	 *
	 * @param selection Selection to check
	 * @return <code>true</code> iff all selected lines are single-line commented
	 */
	private boolean isSelectionCommented(ISelection selection) {
		if (!(selection instanceof ITextSelection))
			return false;
			
		ITextSelection ts= (ITextSelection) selection;
		if (ts.getStartLine() < 0 || ts.getEndLine() < 0)
			return false;
		
		IDocument document= getTextEditor().getDocumentProvider().getDocument(getTextEditor().getEditorInput());
		OUTER: for (int i= ts.getStartLine(); i <= ts.getEndLine(); i++) {
			for (int j= 0; j < fCommentPrefixes.length; j++) {
				try {
					if (fCommentPrefixes[j].length() == 0)
						continue;
					String s= document.get(document.getLineOffset(i), document.getLineLength(i));
					int index= s.indexOf(fCommentPrefixes[j]);
					if (index >= 0 && s.substring(0, index).trim().length() == 0)
						continue OUTER;
				} catch (BadLocationException e) {
					// should not happen
					JavaPlugin.log(e);
				}
			}
			return false;
		}
		return true;
	}

	/**
	 * Implementation of the <code>IUpdate</code> prototype method discovers
	 * the operation through the current editor's
	 * <code>ITextOperationTarget</code> adapter, and sets the enabled state
	 * accordingly.
	 */
	public void update() {
		super.update();
		
		if (!canModifyEditor()) {
			setEnabled(false);
			return;
		}
		
		ITextEditor editor= getTextEditor();
		if (fOperationTarget == null && editor != null)
			fOperationTarget= (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);
			
		boolean isEnabled= (fOperationTarget != null && fOperationTarget.canDoOperation(ITextOperationTarget.PREFIX) && fOperationTarget.canDoOperation(ITextOperationTarget.STRIP_PREFIX));
		setEnabled(isEnabled);
	}
	
	/*
	 * @see TextEditorAction#setEditor(ITextEditor)
	 */
	public void setEditor(ITextEditor editor) {
		super.setEditor(editor);
		fOperationTarget= null;
	}
}
