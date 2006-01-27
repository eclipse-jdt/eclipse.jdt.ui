/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;


import org.eclipse.jface.action.Action;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.internal.ui.text.java.CompletionProposalCategory;
import org.eclipse.jdt.internal.ui.text.java.CompletionProposalComputerRegistry;

/**
 * 
 * @since 3.2
 */
final class SpecificContentAssistAction extends Action {
	private final CompletionProposalCategory fCategory;
	private final SpecificContentAssistExecutor fExecutor= new SpecificContentAssistExecutor(CompletionProposalComputerRegistry.getDefault());
	private JavaEditor fEditor;
	
	public SpecificContentAssistAction(CompletionProposalCategory category) {
		fCategory= category;
		setText(category.getName());
		setImageDescriptor(category.getImageDescriptor());
		setActionDefinitionId("org.eclipse.jdt.ui.specific_content_assist.command"); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		ITextEditor editor= getActiveEditor();
		if (editor == null)
			return;
		
		fExecutor.invokeContentAssist(editor, fCategory.getId());
		
		return;
	}

	private ITextEditor getActiveEditor() {
		return fEditor;
	}

	/**
	 * Sets the active editor part.
	 * 
	 * @param part the editor, possibly <code>null</code>
	 */
	public void setActiveEditor(IEditorPart part) {
		JavaEditor editor;
		if (part instanceof JavaEditor)
			editor= (JavaEditor) part;
		else
			editor= null;
		fEditor= editor;
		setEnabled(computeEnablement(fEditor));
	}
	
	private boolean computeEnablement(ITextEditor editor) {
		if (editor == null)
			return false;
		ITextOperationTarget target= (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);
		return target != null && target.canDoOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
	}
}