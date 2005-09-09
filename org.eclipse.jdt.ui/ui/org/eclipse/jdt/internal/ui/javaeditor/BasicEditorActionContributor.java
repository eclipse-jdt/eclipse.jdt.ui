/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;


import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

import org.eclipse.ui.ide.IDEActionFactory;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.java.CompletionProposalCategory;
import org.eclipse.jdt.internal.ui.text.java.CompletionProposalComputerRegistry;


public class BasicEditorActionContributor extends BasicJavaEditorActionContributor {


	protected RetargetAction fRetargetContentAssist;
	protected RetargetTextEditorAction fContentAssist;
	protected RetargetTextEditorAction fContextInformation;
	protected RetargetTextEditorAction fCorrectionAssist;
	protected RetargetTextEditorAction fChangeEncodingAction;


	public BasicEditorActionContributor() {

		fRetargetContentAssist= new RetargetAction(JdtActionConstants.CONTENT_ASSIST,  JavaEditorMessages.ContentAssistProposal_label);
		fRetargetContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		markAsPartListener(fRetargetContentAssist);

		fContentAssist= new RetargetTextEditorAction(JavaEditorMessages.getBundleForConstructedKeys(), "ContentAssistProposal."); //$NON-NLS-1$
		fContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		fContentAssist.setImageDescriptor(JavaPluginImages.DESC_ELCL_CODE_ASSIST);
		fContentAssist.setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CODE_ASSIST);

		fContextInformation= new RetargetTextEditorAction(JavaEditorMessages.getBundleForConstructedKeys(), "ContentAssistContextInformation."); //$NON-NLS-1$
		fContextInformation.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);

		fCorrectionAssist= new RetargetTextEditorAction(JavaEditorMessages.getBundleForConstructedKeys(), "CorrectionAssistProposal."); //$NON-NLS-1$
		fCorrectionAssist.setActionDefinitionId(IJavaEditorActionDefinitionIds.CORRECTION_ASSIST_PROPOSALS);

		fChangeEncodingAction= new RetargetTextEditorAction(JavaEditorMessages.getBundleForConstructedKeys(), "Editor.ChangeEncodingAction."); //$NON-NLS-1$
	}

	/*
	 * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {

		super.contributeToMenu(menu);

		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.add(fChangeEncodingAction);
			IMenuManager caMenu= new MenuManager(JavaEditorMessages.BasicEditorActionContributor_specific_content_assist_menu, "specific_content_assist"); //$NON-NLS-1$
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, caMenu);
			
			caMenu.add(fRetargetContentAssist);
			Collection descriptors= CompletionProposalComputerRegistry.getDefault().getProposalCategories();
			for (Iterator it= descriptors.iterator(); it.hasNext();) {
				final CompletionProposalCategory cat= (CompletionProposalCategory) it.next();
				if (cat.isSeparateCommand() && cat.hasComputers()) {
					IAction caAction= new SpecificContentAssistAction(cat);
					caMenu.add(caAction);
				}
			}
			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fCorrectionAssist);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fContextInformation);
		}
	}

	/*
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);

		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;

		fContentAssist.setAction(getAction(textEditor, "ContentAssistProposal")); //$NON-NLS-1$
		fContextInformation.setAction(getAction(textEditor, "ContentAssistContextInformation")); //$NON-NLS-1$
		fCorrectionAssist.setAction(getAction(textEditor, "CorrectionAssistProposal")); //$NON-NLS-1$

		fChangeEncodingAction.setAction(getAction(textEditor, ITextEditorActionConstants.CHANGE_ENCODING));

		IActionBars actionBars= getActionBars();
		actionBars.setGlobalActionHandler(JdtActionConstants.SHIFT_RIGHT, getAction(textEditor, "ShiftRight")); //$NON-NLS-1$
		actionBars.setGlobalActionHandler(JdtActionConstants.SHIFT_LEFT, getAction(textEditor, "ShiftLeft")); //$NON-NLS-1$

		actionBars.setGlobalActionHandler(IDEActionFactory.ADD_TASK.getId(), getAction(textEditor, IDEActionFactory.ADD_TASK.getId()));
		actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(), getAction(textEditor, IDEActionFactory.BOOKMARK.getId()));
	}

	/*
	 * @see IEditorActionBarContributor#init(IActionBars, IWorkbenchPage)
	 */
	public void init(IActionBars bars, IWorkbenchPage page) {
		super.init(bars, page);
		// register actions that have a dynamic editor.
		bars.setGlobalActionHandler(JdtActionConstants.CONTENT_ASSIST, fContentAssist);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.BasicJavaEditorActionContributor#dispose()
	 * @since 3.1
	 */ 
	public void dispose() {
		if (fRetargetContentAssist != null) {
			fRetargetContentAssist.dispose();
			fRetargetContentAssist= null;
		}
		super.dispose();
	}
}
