/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;



import java.util.ResourceBundle;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;

import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.GoToNextPreviousMemberAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectionAction;



public class CompilationUnitEditorActionContributor extends BasicEditorActionContributor {
	
	private RetargetTextEditorAction fStructureSelectEnclosingAction;
	private RetargetTextEditorAction fStructureSelectNextAction;
	private RetargetTextEditorAction fStructureSelectPreviousAction;
	private RetargetTextEditorAction fStructureSelectHistoryAction;	
	private RetargetTextEditorAction fGotoNextMemberAction;	
	private RetargetTextEditorAction fGotoPreviousMemberAction;	

	
	public CompilationUnitEditorActionContributor() {
		super();
		
		ResourceBundle b= JavaEditorMessages.getResourceBundle();
				
		fStructureSelectEnclosingAction= new RetargetTextEditorAction(b, "StructureSelectEnclosing."); //$NON-NLS-1$
		fStructureSelectEnclosingAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_ENCLOSING);
		fStructureSelectNextAction= new RetargetTextEditorAction(b, "StructureSelectNext."); //$NON-NLS-1$
		fStructureSelectNextAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_NEXT);
		fStructureSelectPreviousAction= new RetargetTextEditorAction(b, "StructureSelectPrevious."); //$NON-NLS-1$
		fStructureSelectPreviousAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_PREVIOUS);
		fStructureSelectHistoryAction= new RetargetTextEditorAction(b, "StructureSelectHistory."); //$NON-NLS-1$
		fStructureSelectHistoryAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_LAST);
		fGotoNextMemberAction= new RetargetTextEditorAction(b, "GotoNextMember."); //$NON-NLS-1$
		fGotoNextMemberAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_NEXT_MEMBER);
		fGotoPreviousMemberAction= new RetargetTextEditorAction(b, "GotoPreviousMember."); //$NON-NLS-1$
		fGotoPreviousMemberAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_PREVIOUS_MEMBER);
	}

	/*
	 * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {		
		super.contributeToMenu(menu);
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			MenuManager structureSelection= new MenuManager(JavaEditorMessages.getString("ExpandSelectionMenu.label")); //$NON-NLS-1$
			structureSelection.add(fStructureSelectEnclosingAction);
			structureSelection.add(fStructureSelectNextAction);
			structureSelection.add(fStructureSelectPreviousAction);
			structureSelection.add(fStructureSelectHistoryAction);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, structureSelection);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fGotoPreviousMemberAction);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fGotoNextMemberAction);
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
		 
		fStructureSelectEnclosingAction.setAction(getAction(textEditor, StructureSelectionAction.ENCLOSING));
		fStructureSelectNextAction.setAction(getAction(textEditor, StructureSelectionAction.NEXT));
		fStructureSelectPreviousAction.setAction(getAction(textEditor, StructureSelectionAction.PREVIOUS));
		fStructureSelectHistoryAction.setAction(getAction(textEditor, StructureSelectionAction.HISTORY));		
		fGotoNextMemberAction.setAction(getAction(textEditor, GoToNextPreviousMemberAction.NEXT_MEMBER));
		fGotoPreviousMemberAction.setAction(getAction(textEditor, GoToNextPreviousMemberAction.PREVIOUS_MEMBER));
		
		// Source menu.
		IActionBars bars= getActionBars();		
		bars.setGlobalActionHandler(JdtActionConstants.COMMENT, getAction(textEditor, "Comment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.UNCOMMENT, getAction(textEditor, "Uncomment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(JdtActionConstants.FORMAT, getAction(textEditor, "Format")); //$NON-NLS-1$
	}
}