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


import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;

import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class BasicEditorActionContributor extends BasicJavaEditorActionContributor {

	
	protected RetargetAction fRetargetContentAssist;
	protected RetargetTextEditorAction fContentAssist;
	protected RetargetTextEditorAction fContextInformation;
	protected RetargetTextEditorAction fCorrectionAssist;
	protected RetargetTextEditorAction fChangeEncodingAction;
	
	/** Convert to Windows action. */
	protected RetargetTextEditorAction fConvertToWindows;
	/** Convert to UNIX action. */
	protected RetargetTextEditorAction fConvertToUNIX;
	/** Convert to MAC action. */
	protected RetargetTextEditorAction fConvertToMac;
	
	
	public BasicEditorActionContributor() {
		
		fRetargetContentAssist= new RetargetAction(JdtActionConstants.CONTENT_ASSIST,  JavaEditorMessages.getString("ContentAssistProposal.label")); //$NON-NLS-1$
		fRetargetContentAssist.setActionDefinitionId(IJavaEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		markAsPartListener(fRetargetContentAssist);
		
		fContentAssist= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ContentAssistProposal."); //$NON-NLS-1$
		fContentAssist.setActionDefinitionId(IJavaEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS); 
		fContentAssist.setImageDescriptor(JavaPluginImages.DESC_ELCL_CODE_ASSIST);
		fContentAssist.setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CODE_ASSIST);
		
		fContextInformation= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ContentAssistContextInformation."); //$NON-NLS-1$
		fContextInformation.setActionDefinitionId(IJavaEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
		
		fCorrectionAssist= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "CorrectionAssistProposal."); //$NON-NLS-1$
		fCorrectionAssist.setActionDefinitionId(IJavaEditorActionDefinitionIds.CORRECTION_ASSIST_PROPOSALS);
		
		fChangeEncodingAction= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "Editor.ChangeEncodingAction."); //$NON-NLS-1$
		
		// line delimiter conversion
		fConvertToWindows= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "Editor.ConvertToWindows."); //$NON-NLS-1$ 
		fConvertToUNIX= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "Editor.ConvertToUNIX."); //$NON-NLS-1$ 
		fConvertToMac= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "Editor.ConvertToMac."); //$NON-NLS-1$
	}
	
	/*
	 * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		
		super.contributeToMenu(menu);
		
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			MenuManager subMenu= new MenuManager(JavaEditorMessages.getString("Editor.ConvertLineDelimiters.label")); //$NON-NLS-1$
			subMenu.add(fConvertToWindows);
			subMenu.add(fConvertToUNIX);
			subMenu.add(fConvertToMac);
			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fRetargetContentAssist);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fCorrectionAssist);			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fContextInformation);
			
			editMenu.add(subMenu);
			editMenu.add(fChangeEncodingAction);
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

		// line delimiter conversion
		fConvertToWindows.setAction(getAction(textEditor, JdtActionConstants.CONVERT_LINE_DELIMITERS_TO_WINDOWS));
		fConvertToUNIX.setAction(getAction(textEditor, JdtActionConstants.CONVERT_LINE_DELIMITERS_TO_UNIX));
		fConvertToMac.setAction(getAction(textEditor, JdtActionConstants.CONVERT_LINE_DELIMITERS_TO_MAC));
		
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
}
