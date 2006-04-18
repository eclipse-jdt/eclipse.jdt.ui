/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;

import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.QuickTemplateProcessor;

public class SurroundWithTemplateActionGroup extends ActionGroup {

	private final CompilationUnitEditor fEditor;

	public SurroundWithTemplateActionGroup(CompilationUnitEditor editor) {
		fEditor= editor;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void fillContextMenu(IMenuManager menu) {
		final IAction[] actions= getActions();

		if (actions == null)
			return;
		
		MenuManager subMenu = new MenuManager(ActionMessages.SurroundWithTemplateActionGroup_SurroundWithTemplateSubMenuName, "org.eclipse.jdt.ui.surround.with.template.menu"); //$NON-NLS-1$
		for (int i= 0; i < actions.length; i++) {
			subMenu.add(actions[i]);
		}
		menu.appendToGroup(GenerateActionGroup.GROUP_CODE, subMenu);
	}

	private IAction[] getActions() {
		ISelectionProvider selectionProvider= fEditor.getSelectionProvider();
		if (selectionProvider == null)
			return null;
		
		ISelection selection= selectionProvider.getSelection();
		if (!(selection instanceof ITextSelection))
			return null;
		
		ITextSelection textSelection= (ITextSelection)selection;
		if (textSelection.getLength() == 0)
			return null;
		
		ICompilationUnit cu= JavaUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
		if (cu == null)
			return null;
		
		QuickTemplateProcessor quickTemplateProcessor= new QuickTemplateProcessor();
		IInvocationContext context= new AssistContext(cu, textSelection.getOffset(), textSelection.getLength());
		
		try {
			IJavaCompletionProposal[] proposals= quickTemplateProcessor.getAssists(context, null);
			if (proposals == null || proposals.length == 0)
				return null;
			
			return getActionsFromProposals(proposals, context.getSelectionOffset());
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	private IAction[] getActionsFromProposals(IJavaCompletionProposal[] proposals, final int offset) {
		List result= new ArrayList();
		
		for (int i= 0, j= 1; i < proposals.length; i++) {
			if (proposals[i] instanceof ICompletionProposalExtension2) {
				final ICompletionProposalExtension2 proposal= (ICompletionProposalExtension2)proposals[i];
				
				StringBuffer actionName= new StringBuffer();
				if (j<10) {
					actionName.append('&').append(j).append(' ');
				}
				actionName.append(proposals[i].getDisplayString());
				
				Action action= new Action(actionName.toString()) {
					/**
					 * {@inheritDoc} 
					 */
					public void run() {
						proposal.apply(fEditor.getViewer(), (char)0, 0, offset);
					}
				};
				
				result.add(action);
				j++;
			}
		}
		if (result.size() == 0)
			return null;
		
		return (IAction[])result.toArray(new IAction[result.size()]);
	}

}