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
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.StatusLineContributionItem;
import org.eclipse.ui.texteditor.TextEditorAction;

public class CompilationUnitEditorActionContributor extends BasicEditorActionContributor {
	
	/** The smart editing status item */
	private StatusLineContributionItem fStatusItem;
	/** The smart typing toggle action */
	private TextEditorAction fSmartTypingAction;
	

	public CompilationUnitEditorActionContributor() {
		super();
		fSmartTypingAction= new ToggleSmartTypingAction(JavaEditorMessages.getResourceBundle(), "ToggleSmartTypingAction.", null); //$NON-NLS-1$
		fSmartTypingAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.TOGGLE_SMART_TYPING);
		WorkbenchHelp.setHelp(fSmartTypingAction, IJavaHelpContextIds.TOGGLE_SMART_TYPING_ACTION);
		
		fStatusItem= new StatusLineContributionItem(IJavaEditorActionConstants.STATUS_CATEGORY_SMART_TYPING);
		fStatusItem.setActionHandler(fSmartTypingAction);
	}

	/*
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		// unregister status field with previous editor
		IEditorPart previous= getActiveEditorPart();
		if (previous instanceof ITextEditorExtension) {
			ITextEditorExtension extension= (ITextEditorExtension) previous;
			extension.setStatusField(null, IJavaEditorActionConstants.STATUS_CATEGORY_SMART_TYPING);
		}

		super.setActiveEditor(part);
		
		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;
		 
		// Source menu.
		IActionBars bars= getActionBars();		
		bars.setGlobalActionHandler(JdtActionConstants.COMMENT, getAction(textEditor, "Comment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.UNCOMMENT, getAction(textEditor, "Uncomment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.FORMAT, getAction(textEditor, "Format")); //$NON-NLS-1$
		
		// Edit menu - connect our actions with the editor and have the editor register the action
		// with the keybinding service.
		if (JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_TYPING)) {
			fSmartTypingAction.setEditor(textEditor);
			if (textEditor != null) {
				textEditor.setAction(IJavaEditorActionConstants.TOGGLE_SMART_TYPING, fSmartTypingAction);
			}
			
			// register status field
			if (part instanceof ITextEditorExtension) {
				ITextEditorExtension extension= (ITextEditorExtension) part;
				extension.setStatusField(fStatusItem, IJavaEditorActionConstants.STATUS_CATEGORY_SMART_TYPING);
			}
		}
	}
	
	/*
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		super.contributeToMenu(menu);
		if (JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_TYPING)) {
			IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
			if (editMenu != null) {
				editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fSmartTypingAction);
			}
		}
	}

	/*
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToStatusLine(org.eclipse.jface.action.IStatusLineManager)
	 */
	public void contributeToStatusLine(IStatusLineManager statusLineManager) {
		super.contributeToStatusLine(statusLineManager);
		if (JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_TYPING)) {
			if (statusLineManager != null) {
				statusLineManager.insertAfter(ITextEditorActionConstants.STATUS_CATEGORY_ELEMENT_STATE, fStatusItem);
			}
		}
	}
}
