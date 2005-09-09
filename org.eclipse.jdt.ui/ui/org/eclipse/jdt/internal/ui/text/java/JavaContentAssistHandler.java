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
package org.eclipse.jdt.internal.ui.text.java;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import org.eclipse.jface.action.IAction;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * 
 * @since 3.2
 */
public final class JavaContentAssistHandler extends AbstractHandler {
	
	public JavaContentAssistHandler() {
	}

	/*
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final JavaEditor editor= getActiveJavaEditor();
		if (editor == null)
			return null;
		
		String computerId= event.getParameter("org.eclipse.jdt.ui.specific_content_assist.category_id"); //$NON-NLS-1$
		if (computerId == null)
			return null;
		
		IAction action= editor.getAction("ContentAssistProposal"); //$NON-NLS-1$
		if (action == null || !action.isEnabled())
			return null;
		
		Collection categories= CompletionProposalComputerRegistry.getDefault().getProposalCategories();
		boolean[] oldstates= new boolean[categories.size()];
		int i= 0;
		for (Iterator it1= categories.iterator(); it1.hasNext();) {
			CompletionProposalCategory cat= (CompletionProposalCategory) it1.next();
			oldstates[i++]= cat.isEnabled();
			cat.setEnabled(cat.getId().equals(computerId));
		}
		
		try {
			ITextOperationTarget target= editor.getViewer().getTextOperationTarget();
			if (target.canDoOperation(ISourceViewer.CONTENTASSIST_PROPOSALS))
				target.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
		} finally {
			i= 0;
			for (Iterator it1= categories.iterator(); it1.hasNext();) {
				CompletionProposalCategory cat= (CompletionProposalCategory) it1.next();
				cat.setEnabled(oldstates[i++]);
			}
		}
		
		return null;
	}

	private JavaEditor getActiveJavaEditor() {
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page= window.getActivePage();
			if (page != null) {
				IEditorPart editor= page.getActiveEditor();
				if (editor instanceof JavaEditor)
					return (JavaEditor) editor;
			}
		}
		return null;
	}

}
