/*******************************************************************************
 * Copyright (c) 2000, 2001 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.Separator;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.editors.text.EncodingActionGroup;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;

import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class BasicEditorActionContributor extends BasicTextEditorActionContributor {
	
	protected RetargetAction fRetargetShowJavaDoc;
	protected RetargetAction fRetargetContentAssist;
	
	protected RetargetTextEditorAction fContentAssist;
	protected RetargetTextEditorAction fContextInformation;
	protected RetargetTextEditorAction fCorrectionAssist;
	protected RetargetTextEditorAction fShowJavaDoc;
	
	private EncodingActionGroup fEncodingActionGroup;
	
	private List fPartListeners= new ArrayList();



	public BasicEditorActionContributor() {
		
		fRetargetShowJavaDoc= new RetargetAction(JdtActionConstants.SHOW_JAVA_DOC, JavaEditorMessages.getString("ShowJavaDoc.label")); //$NON-NLS-1$
		fRetargetShowJavaDoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		fRetargetContentAssist= new RetargetAction(JdtActionConstants.CONTENT_ASSIST,  JavaEditorMessages.getString("ContentAssistProposal.label")); //$NON-NLS-1$
		fRetargetContentAssist.setActionDefinitionId(IJavaEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		
		fContentAssist= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ContentAssistProposal."); //$NON-NLS-1$
		fContentAssist.setActionDefinitionId(IJavaEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS); 
		fContentAssist.setImageDescriptor(JavaPluginImages.DESC_CLCL_CODE_ASSIST);
		fContentAssist.setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CODE_ASSIST);
		
		fContextInformation= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ContentAssistContextInformation."); //$NON-NLS-1$
		fContextInformation.setActionDefinitionId(IJavaEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
		fCorrectionAssist= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "CorrectionAssistProposal."); //$NON-NLS-1$
		fCorrectionAssist.setActionDefinitionId(IJavaEditorActionDefinitionIds.CORRECTION_ASSIST_PROPOSALS);
		fShowJavaDoc= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ShowJavaDoc."); //$NON-NLS-1$
		fShowJavaDoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		
		
		// character encoding
		fEncodingActionGroup= new EncodingActionGroup();
		
		markAsPartListener(fRetargetShowJavaDoc);
		markAsPartListener(fRetargetContentAssist);
	}
	
	protected final void markAsPartListener(RetargetAction action) {
		fPartListeners.add(action);
	}
	
	/*
	 * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		
		super.contributeToMenu(menu);
		
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.add(new Separator(IContextMenuConstants.GROUP_OPEN));
			editMenu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fRetargetContentAssist);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fCorrectionAssist);			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fContextInformation);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fRetargetShowJavaDoc);
		}		
	}
	
	/*
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
				
		IActionBars actionBars= getActionBars();
		IStatusLineManager manager= actionBars.getStatusLineManager();
		manager.setMessage(null);
		manager.setErrorMessage(null);
		
		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;
			
		fContentAssist.setAction(getAction(textEditor, "ContentAssistProposal")); //$NON-NLS-1$
		fContextInformation.setAction(getAction(textEditor, "ContentAssistContextInformation")); //$NON-NLS-1$
		fCorrectionAssist.setAction(getAction(textEditor, "CorrectionAssistProposal")); //$NON-NLS-1$
		fShowJavaDoc.setAction(getAction(textEditor, "ShowJavaDoc")); //$NON-NLS-1$
		
		actionBars.setGlobalActionHandler(JdtActionConstants.SHIFT_RIGHT, getAction(textEditor, "ShiftRight")); //$NON-NLS-1$
		actionBars.setGlobalActionHandler(JdtActionConstants.SHIFT_LEFT, getAction(textEditor, "ShiftLeft")); //$NON-NLS-1$
		
		// character encoding
		fEncodingActionGroup.retarget(textEditor);
	}
	
	/*
	 * @see IEditorActionBarContributor#init(IActionBars, IWorkbenchPage)
	 */
	public void init(IActionBars bars, IWorkbenchPage page) {
		Iterator e= fPartListeners.iterator();
		while (e.hasNext()) 
			page.addPartListener((RetargetAction) e.next());
			
		super.init(bars, page);
		
		// register actions that have a dynamic editor. 
		bars.setGlobalActionHandler(JdtActionConstants.SHOW_JAVA_DOC, fShowJavaDoc);
		bars.setGlobalActionHandler(JdtActionConstants.CONTENT_ASSIST, fContentAssist);
		// character encoding
		fEncodingActionGroup.fillActionBars(bars);
	}
	
	/*
	 * @see IEditorActionBarContributor#dispose()
	 */
	public void dispose() {
		Iterator e= fPartListeners.iterator();
		while (e.hasNext()) 
			getPage().removePartListener((RetargetAction) e.next());
		fPartListeners.clear();
		
		setActiveEditor(null);
		
		super.dispose();
	}
}