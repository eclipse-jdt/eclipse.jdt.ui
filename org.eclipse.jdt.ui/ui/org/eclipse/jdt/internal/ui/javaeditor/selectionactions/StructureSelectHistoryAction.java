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
package org.eclipse.jdt.internal.ui.javaeditor.selectionactions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditorMessages;

public class StructureSelectHistoryAction extends Action implements IUpdate {
	private JavaEditor fEditor;
	private SelectionHistory fHistory;
	
	public StructureSelectHistoryAction(JavaEditor editor, SelectionHistory history) {
		super(JavaEditorMessages.getString("StructureSelectHistory.label")); //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("StructureSelectHistory.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("StructureSelectHistory.description")); //$NON-NLS-1$
		Assert.isNotNull(history);
		Assert.isNotNull(editor);
		fHistory= history;
		fEditor= editor;
		update();
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.STRUCTURED_SELECTION_HISTORY_ACTION);
	}
	
	public void update() {
		setEnabled(!fHistory.isEmpty());
	}
	
	public void run() {
		ISourceRange old= fHistory.getLast();
		if (old != null) {
			try {
				fHistory.ignoreSelectionChanges();
				fEditor.selectAndReveal(old.getOffset(), old.getLength());
			} finally {
				fHistory.listenToSelectionChanges();
			}
		}
	}
}
