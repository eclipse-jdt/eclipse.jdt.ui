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


import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

import org.eclipse.jdt.internal.ui.search.SearchUsagesInFileAction;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

public class ClassFileEditorActionContributor extends BasicTextEditorActionContributor {
	
	protected RetargetTextEditorAction fShowJavaDoc;
	private RetargetTextEditorAction fShowReferencesAction;	
	protected TogglePresentationAction fTogglePresentationAction;
	
	public ClassFileEditorActionContributor() {
		super();
		
		fShowJavaDoc= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ShowJavaDoc."); //$NON-NLS-1$
		fShowJavaDoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		fTogglePresentationAction= new TogglePresentationAction();				
		fShowReferencesAction= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ShowReferencesInFile."); //$NON-NLS-1$
		fShowReferencesAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_REFERENCES);
	}
	
	/*
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		
		super.contributeToMenu(menu);
		
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {

			editMenu.add(new Separator(IContextMenuConstants.GROUP_OPEN));			
			editMenu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
			editMenu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
				
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fShowJavaDoc);

			editMenu.appendToGroup(IWorkbenchActionConstants.FIND_EXT, fShowReferencesAction);
		}
	}
	
	/*
	 * @see EditorActionBarContributor#contributeToToolBar(IToolBarManager)
	 */
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(new Separator());
		tbm.add(fTogglePresentationAction);		
	}
	
	/*
	 * @see EditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		
		super.setActiveEditor(part);
		
		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;
			
		ClassFileEditor classFileEditor= null;	
		if (part instanceof ClassFileEditor)
			classFileEditor= (ClassFileEditor)part;
		
		fShowJavaDoc.setAction(getAction(textEditor, "ShowJavaDoc")); //$NON-NLS-1$
		fTogglePresentationAction.setEditor(textEditor);		
		fShowReferencesAction.setAction(getAction(textEditor, SearchUsagesInFileAction.SHOWREFERENCES));

		if (classFileEditor != null) {
			IActionBars bars= getActionBars();
			classFileEditor.fActionGroups.fillActionBars(bars);
		}
	}
	
	/*
	 * @see IEditorActionBarContributor#dispose()
	 */
	public void dispose() {
		setActiveEditor(null);
		super.dispose();
	}
}