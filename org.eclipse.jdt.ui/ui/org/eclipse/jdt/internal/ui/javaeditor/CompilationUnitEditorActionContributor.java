package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.ResourceBundle;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

public class CompilationUnitEditorActionContributor extends BasicEditorActionContributor {
	
	protected OpenOnSelectionAction fOpenOnSelection;
	protected OpenOnSelectionAction fOpenOnTypeSelection;
	protected RetargetTextEditorAction fAddImportOnSelection;
	protected RetargetTextEditorAction fOrganizeImports;
	protected RetargetTextEditorAction fShowJavaDoc;
	protected TogglePresentationAction fTogglePresentation;
	protected ToggleTextHoverAction fToggleTextHover;
	protected GotoErrorAction fPreviousError;
	protected GotoErrorAction fNextError;
	
	protected RetargetTextEditorAction fDisplay;
	protected RetargetTextEditorAction fInspect;
	
	
	public CompilationUnitEditorActionContributor() {
		super();
		
		ResourceBundle bundle= JavaEditorMessages.getResourceBundle();
		
		fOpenOnSelection= new OpenOnSelectionAction();
		fOpenOnTypeSelection= new OpenHierarchyOnSelectionAction();
		fAddImportOnSelection= new RetargetTextEditorAction(bundle, "AddImportOnSelectionAction."); //$NON-NLS-1$
		fOrganizeImports= new RetargetTextEditorAction(bundle, "OrganizeImportsAction."); //$NON-NLS-1$
		fShowJavaDoc= new RetargetTextEditorAction(bundle, "ShowJavaDoc."); //$NON-NLS-1$
		fTogglePresentation= new TogglePresentationAction();
		fToggleTextHover= new ToggleTextHoverAction();
		fPreviousError= new GotoErrorAction("PreviousError.", false); //$NON-NLS-1$
		fPreviousError.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_PREV_ERROR);
		fNextError= new GotoErrorAction("NextError.", true); //$NON-NLS-1$
		fNextError.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_NEXT_ERROR);
		
		fDisplay= new RetargetTextEditorAction(bundle, "DisplayAction."); //$NON-NLS-1$	
		fInspect= new RetargetTextEditorAction(bundle, "InpsectAction."); //$NON-NLS-1$
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
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fNextError);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fPreviousError);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fShowJavaDoc);			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fAddImportOnSelection);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fOrganizeImports);
			editMenu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));	
			editMenu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, fInspect);		
			editMenu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, fDisplay);
		}
	}
	
	/**
	 * @see EditorActionBarContributor#contributeToToolBar(IToolBarManager)
	 */
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(new Separator());
		tbm.add(fTogglePresentation);
		tbm.add(fToggleTextHover);
		tbm.add(fNextError);
		tbm.add(fPreviousError);
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
		
		fAddImportOnSelection.setAction(getAction(textEditor,"AddImportOnSelection")); //$NON-NLS-1$
		fOrganizeImports.setAction(getAction(textEditor, "OrganizeImports")); //$NON-NLS-1$
		fShowJavaDoc.setAction(getAction(textEditor, "ShowJavaDoc")); //$NON-NLS-1$
		
		fTogglePresentation.setEditor(textEditor);
		fToggleTextHover.setEditor(textEditor);
		fPreviousError.setEditor(textEditor);
		fNextError.setEditor(textEditor);
		
		IAction updateAction= getAction(textEditor, "Display"); //$NON-NLS-1$
		if (updateAction instanceof IUpdate) {
			((IUpdate)updateAction).update();
		}
		fDisplay.setAction(updateAction); 
		updateAction= getAction(textEditor, "Inspect"); //$NON-NLS-1$
		if (updateAction instanceof IUpdate) {
			((IUpdate)updateAction).update();
		}
		fInspect.setAction(updateAction);
	}
}