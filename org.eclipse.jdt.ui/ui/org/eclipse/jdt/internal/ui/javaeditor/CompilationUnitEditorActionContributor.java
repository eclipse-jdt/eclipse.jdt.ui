package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.OpenExternalJavadocAction;
import org.eclipse.jdt.internal.ui.actions.RetargetActionIDs;
import org.eclipse.jdt.internal.ui.javaeditor.structureselection.StructureSelectionAction;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.ui.texteditor.TextOperationAction;

public class CompilationUnitEditorActionContributor extends BasicEditorActionContributor {
	
	private class RetargetEditorAction extends RetargetAction {

		private IAction fAction;

		private IPropertyChangeListener fListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				update(event);
			}
		};

		public RetargetEditorAction(String actionId, String label) {
			super(actionId, label);
		}
	
		public RetargetEditorAction(String actionId, IAction action) {
			super(actionId, action.getText());
			setAction(action);	
		}
		
		public void setAction(IAction action) {
			if (fAction != null)
				fAction.removePropertyChangeListener(fListener);
			
			fAction= action;
			
			if (fAction != null) {
				setEnabled(fAction.isEnabled());
				setImageDescriptor(fAction.getImageDescriptor());
				setHoverImageDescriptor(fAction.getHoverImageDescriptor());
				setDisabledImageDescriptor(fAction.getDisabledImageDescriptor());
				setToolTipText(fAction.getToolTipText());
				setMenuCreator(fAction.getMenuCreator());
				if (fAction.getStyle() == IAction.AS_CHECK_BOX)
					setChecked(fAction.isChecked());
				setDescription(fAction.getDescription());				

				fAction.addPropertyChangeListener(fListener);
			}
		}

		private void update(PropertyChangeEvent event) {
			if (ENABLED.equals(event.getProperty())) {
				Boolean bool= (Boolean) event.getNewValue();
				setEnabled(bool.booleanValue());
			} else if (TEXT.equals(event.getProperty()))
				setText((String) event.getNewValue());
			else if (TOOL_TIP_TEXT.equals(event.getProperty()))
				setToolTipText((String) event.getNewValue());
			else if (CHECKED.equals(event.getProperty())) {
				Boolean bool= (Boolean) event.getNewValue();
				setChecked(bool.booleanValue());
			}
		}		
	}
	
	private boolean fListenersRegistered;
	
	private RetargetTextEditorAction fStructureSelectEnclosingAction;
	private RetargetTextEditorAction fStructureSelectNextAction;
	private RetargetTextEditorAction fStructureSelectPreviousAction;
	private RetargetTextEditorAction fStructureSelectHistoryAction;
	
	protected TogglePresentationAction fTogglePresentation;
	protected ToggleTextHoverAction fToggleTextHover;
	protected GotoErrorAction fPreviousError;
	protected GotoErrorAction fNextError;
	
	private RetargetAction fRetargetPreviousError;	
	private RetargetAction fRetargetNextError;
	private RetargetAction fRetargetTogglePresentation;
	private RetargetAction fRetargetToggleTextHover;	
	
	public CompilationUnitEditorActionContributor() {
		super();
		
		ResourceBundle bundle= JavaEditorMessages.getResourceBundle();
				
		fTogglePresentation= new TogglePresentationAction();
		fToggleTextHover= new ToggleTextHoverAction();
		fPreviousError= new GotoErrorAction("PreviousError.", false); //$NON-NLS-1$
		fPreviousError.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_PREV_ERROR);
		fNextError= new GotoErrorAction("NextError.", true); //$NON-NLS-1$
		fNextError.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_NEXT_ERROR);

		fRetargetTogglePresentation= new RetargetEditorAction(IJavaEditorActionConstants.TOGGLE_PRESENTATION, fTogglePresentation);
		fRetargetToggleTextHover= new RetargetEditorAction(IJavaEditorActionConstants.TOGGLE_TEXT_HOVER, fToggleTextHover);
		
		fRetargetPreviousError= new RetargetEditorAction(RetargetActionIDs.SHOW_PREVIOUS_PROBLEM, fPreviousError);
		fRetargetNextError= new RetargetEditorAction(RetargetActionIDs.SHOW_NEXT_PROBLEM, fNextError);
		
		fStructureSelectEnclosingAction= new RetargetTextEditorAction(bundle, "StructureSelectEnclosing."); //$NON-NLS-1$
		fStructureSelectNextAction= new RetargetTextEditorAction(bundle, "StructureSelectNext."); //$NON-NLS-1$
		fStructureSelectPreviousAction= new RetargetTextEditorAction(bundle, "StructureSelectPrevious."); //$NON-NLS-1$
		fStructureSelectHistoryAction= new RetargetTextEditorAction(bundle, "StructureSelectHistory."); //$NON-NLS-1$
	}

	private String getLabel(ResourceBundle bundle, String prefix) {
		final String labelKey= "label"; //$NON-NLS-1$
		try {
			return bundle.getString(prefix + labelKey);
		} catch (MissingResourceException e) {
			return labelKey;
		}
	}
	
	public void init(IActionBars bars) {
		super.init(bars);
		// register actions that have a dynamic editor. 
		bars.setGlobalActionHandler(IJavaEditorActionConstants.TOGGLE_PRESENTATION, fTogglePresentation);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.TOGGLE_TEXT_HOVER, fToggleTextHover);
		
		bars.setGlobalActionHandler(RetargetActionIDs.SHOW_NEXT_PROBLEM, fNextError);
		bars.setGlobalActionHandler(RetargetActionIDs.SHOW_PREVIOUS_PROBLEM, fPreviousError);
	}

	/**
	 * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {		
		super.contributeToMenu(menu);
		
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			addStructureSelection(editMenu);
		}
	}

	private void addStructureSelection(IMenuManager editMenu) {
		MenuManager structureSelection= new MenuManager(JavaEditorMessages.getString("ExpandSelectionMenu.label")); //$NON-NLS-1$
		structureSelection.add(fStructureSelectEnclosingAction);
		structureSelection.add(fStructureSelectNextAction);
		structureSelection.add(fStructureSelectPreviousAction);
		structureSelection.add(fStructureSelectHistoryAction);
		editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, structureSelection);
	}

	/**
	 * @see EditorActionBarContributor#contributeToToolBar(IToolBarManager)
	 */
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(new Separator());
		tbm.add(fRetargetTogglePresentation);
		tbm.add(fRetargetToggleTextHover);
		tbm.add(fRetargetNextError);
		tbm.add(fRetargetPreviousError);
	}
	
	/**
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {		
		super.setActiveEditor(part);
		
		registerListeners(part);
		
		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;

		CompilationUnitEditor cueditor= null;		
		if (part instanceof CompilationUnitEditor)
			cueditor= (CompilationUnitEditor)part;
		

		fTogglePresentation.setEditor(textEditor);
		fToggleTextHover.setEditor(textEditor);
		fPreviousError.setEditor(textEditor);
		fNextError.setEditor(textEditor);
		
		fStructureSelectEnclosingAction.setAction(getAction(textEditor, StructureSelectionAction.ENCLOSING));
		fStructureSelectNextAction.setAction(getAction(textEditor, StructureSelectionAction.NEXT));
		fStructureSelectPreviousAction.setAction(getAction(textEditor, StructureSelectionAction.PREVIOUS));
		fStructureSelectHistoryAction.setAction(getAction(textEditor, StructureSelectionAction.HISTORY));		

		IActionBars bars= getActionBars();		
		// Source menu.

		bars.setGlobalActionHandler(RetargetActionIDs.COMMENT, getAction(textEditor, "Comment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(RetargetActionIDs.UNCOMMENT, getAction(textEditor, "Uncomment")); //$NON-NLS-1$
		bars.setGlobalActionHandler(RetargetActionIDs.FORMAT, getAction(textEditor, "Format")); //$NON-NLS-1$

		bars.setGlobalActionHandler(RetargetActionIDs.ADD_IMPORT, getAction(textEditor, "AddImportOnSelection")); //$NON-NLS-1$
		bars.setGlobalActionHandler(RetargetActionIDs.ORGANIZE_IMPORTS, getAction(textEditor, "OrganizeImports")); //$NON-NLS-1$
		bars.setGlobalActionHandler(RetargetActionIDs.SURROUND_WITH_TRY_CATCH, getAction(textEditor, "SurroundWithTryCatch")); //$NON-NLS-1$
		
		// Navigate menu
	
		if (cueditor != null) {
			cueditor.fStandardActionGroups.fillActionBars(bars);
		}										
	}
	
	private void registerListeners(IEditorPart part) {
		if (fListenersRegistered)
			return;
		IWorkbenchPage page = part.getSite().getPage();	
		page.addPartListener(fRetargetTogglePresentation);
		page.addPartListener(fRetargetToggleTextHover);
		page.addPartListener(fRetargetNextError);
		page.addPartListener(fRetargetPreviousError);		
		fListenersRegistered= true;
	}
}