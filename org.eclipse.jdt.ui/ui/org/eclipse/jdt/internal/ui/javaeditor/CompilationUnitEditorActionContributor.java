package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import java.util.ResourceBundle;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CompilationUnitEditorActionContributor extends BasicEditorActionContributor {
	
	protected OpenOnSelectionAction fOpenOnSelection;
	protected OpenOnSelectionAction fOpenOnTypeSelection;
	protected RetargetTextEditorAction fAddImportOnSelectionAction;
	protected RetargetTextEditorAction fOrganizeImportsAction;
	protected TogglePresentationAction fTogglePresentationAction;
	
	
	public CompilationUnitEditorActionContributor() {
		super();
		
		ResourceBundle bundle= JavaPlugin.getResourceBundle();
		
		fOpenOnSelection= new OpenOnSelectionAction(bundle, "Editor.OpenOnSelection.");
		fOpenOnTypeSelection= new OpenHierarchyOnSelectionAction(bundle, "Editor.OpenHierarchyOnSelection.");
		fAddImportOnSelectionAction= new RetargetTextEditorAction(bundle, "AddImportOnSelectionAction.");
		fOrganizeImportsAction= new RetargetTextEditorAction(bundle, "OrganizeImportsAction.");
		fTogglePresentationAction= new TogglePresentationAction(bundle, "Editor.TogglePresentation.");
	}
	
	/**
	 * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		
		super.contributeToMenu(menu);
		
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpenOnSelection);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpenOnTypeSelection);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fAddImportOnSelectionAction);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fOrganizeImportsAction);
		}
		
	}
	
	/**
	 * @see EditorActionBarContributor#contributeToToolBar(IToolBarManager)
	 */
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(new Separator());
		tbm.add(fTogglePresentationAction);
	}
	
	/**
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		
		super.setActiveEditor(part);
		
		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;
		
		fOpenOnSelection.setContentEditor(textEditor);
		fOpenOnTypeSelection.setContentEditor(textEditor);
		
		fAddImportOnSelectionAction.setAction(getAction(textEditor,"AddImportOnSelection"));
		fOrganizeImportsAction.setAction(getAction(textEditor, "OrganizeImports"));
		
		fTogglePresentationAction.setEditor(textEditor);
	}
}