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

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.internal.ui.text.java.CompletionProposalCategory;
import org.eclipse.jdt.internal.ui.text.java.CompletionProposalComputerRegistry;

final class SpecificContentAssistAction extends Action {
	private final CompletionProposalCategory fCategory;
	private final SpecificContentAssistExecutor fExecutor= new SpecificContentAssistExecutor(CompletionProposalComputerRegistry.getDefault());

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
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page= window.getActivePage();
			if (page != null) {
				IEditorPart editor= page.getActiveEditor();
				if (editor instanceof ITextEditor)
					return (JavaEditor) editor;
			}
		}
		return null;
	}
}