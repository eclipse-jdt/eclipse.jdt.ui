package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.OpenExternalJavadocAction;
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
				if (fAction.getStyle() == IAction.AS_CHECK_BOX && fAction.isChecked())
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
		}		
	}
	
	protected OpenOnSelectionAction fOpenOnSelection;
	protected OpenOnSelectionAction fOpenOnTypeSelection;
	protected RetargetTextEditorAction fAddImportOnSelection;
	protected RetargetTextEditorAction fOrganizeImports;
	protected TogglePresentationAction fTogglePresentation;
	protected ToggleTextHoverAction fToggleTextHover;
	protected GotoErrorAction fPreviousError;
	protected GotoErrorAction fNextError;
	
	/** The global actions to be connected with editor actions */
	private final static String[] ACTIONS= {
	};

	private RetargetAction fRetargetOpenOnSelection;	
	private RetargetAction fRetargetOpenOnTypeSelection;	
	private RetargetAction fRetargetPreviousError;	
	private RetargetAction fRetargetNextError;
	private RetargetEditorAction fRetargetShowJavadoc;
	private RetargetEditorAction fRetargetOpenExternalJavadoc;
	private RetargetAction fRetargetTogglePresentation;
	private RetargetAction fRetargetToggleTextHover;	
	private RetargetTextEditorAction fStructureSelectEnclosingAction;
	private RetargetTextEditorAction fStructureSelectNextAction;
	private RetargetTextEditorAction fStructureSelectPreviousAction;
	private RetargetTextEditorAction fStructureSelectHistoryAction;
	//protected RetargetTextEditorAction fDisplay;
	//protected RetargetTextEditorAction fInspect;
	
	
	public CompilationUnitEditorActionContributor() {
		super();
		
		ResourceBundle bundle= JavaEditorMessages.getResourceBundle();
				
		fOpenOnSelection= new OpenOnSelectionAction();
		fOpenOnTypeSelection= new OpenHierarchyOnSelectionAction();
		fAddImportOnSelection= new RetargetTextEditorAction(bundle, "AddImportOnSelectionAction."); //$NON-NLS-1$
		fOrganizeImports= new RetargetTextEditorAction(bundle, "OrganizeImportsAction."); //$NON-NLS-1$
		fTogglePresentation= new TogglePresentationAction();
		fToggleTextHover= new ToggleTextHoverAction();
		fPreviousError= new GotoErrorAction("PreviousError.", false); //$NON-NLS-1$
		fPreviousError.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_PREV_ERROR);
		fNextError= new GotoErrorAction("NextError.", true); //$NON-NLS-1$
		fNextError.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_NEXT_ERROR);

		fRetargetOpenOnSelection= new RetargetEditorAction(IJavaEditorActionConstants.OPEN_SELECTION, fOpenOnSelection);
		fRetargetOpenOnTypeSelection= new RetargetEditorAction(IJavaEditorActionConstants.OPEN_TYPE_HIERARCHY, fOpenOnTypeSelection);
		fRetargetTogglePresentation= new RetargetEditorAction(IJavaEditorActionConstants.TOGGLE_PRESENTATION, fTogglePresentation);
		fRetargetToggleTextHover= new RetargetEditorAction(IJavaEditorActionConstants.TOGGLE_TEXT_HOVER, fToggleTextHover);
		fRetargetPreviousError= new RetargetEditorAction(IJavaEditorActionConstants.PREVIOUS_ERROR, fPreviousError);
		fRetargetNextError= new RetargetEditorAction(IJavaEditorActionConstants.NEXT_ERROR, fNextError);
		fRetargetShowJavadoc= new RetargetEditorAction(IJavaEditorActionConstants.SHOW_JAVADOC, getLabel(bundle, "ShowJavaDoc."));
		fRetargetOpenExternalJavadoc= new RetargetEditorAction(IJavaEditorActionConstants.OPEN_JAVADOC, getLabel(bundle, "OpenExternalJavadoc."));
		
		fStructureSelectEnclosingAction= new RetargetTextEditorAction(bundle, "StructureSelectEnclosing.");
		fStructureSelectNextAction= new RetargetTextEditorAction(bundle, "StructureSelectNext.");
		fStructureSelectPreviousAction= new RetargetTextEditorAction(bundle, "StructureSelectPrevious.");
		fStructureSelectHistoryAction= new RetargetTextEditorAction(bundle, "StructureSelectHistory.");
		
		//fDisplay= new RetargetTextEditorAction(bundle, "DisplayAction."); //$NON-NLS-1$	
		//Inspect= new RetargetTextEditorAction(bundle, "InpsectAction."); //$NON-NLS-1$
	}

	private String getLabel(ResourceBundle bundle, String prefix) {
		final String labelKey= "label";
		try {
			return bundle.getString(prefix + labelKey);
		} catch (MissingResourceException e) {
			return labelKey;
		}
	}
	
	/**
	 * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		
		super.contributeToMenu(menu);
		
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			addStructureSelection(editMenu);
			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fRetargetOpenOnSelection);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fRetargetOpenOnTypeSelection);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fRetargetNextError);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fRetargetPreviousError);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fRetargetShowJavadoc);			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fRetargetOpenExternalJavadoc);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fAddImportOnSelection);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fOrganizeImports);
			editMenu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));	
			//editMenu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, fInspect);		
			//editMenu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, fDisplay);
		}
	}

	private void addStructureSelection(IMenuManager editMenu) {
		MenuManager structureSelection= new MenuManager("Expand &Selection to");
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
		
		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;
		
		if (part instanceof CompilationUnitEditor){
			CompilationUnitEditor editor= (CompilationUnitEditor)part;
			fStructureSelectEnclosingAction.setAction(editor.getAction(StructureSelectionAction.ENCLOSING));
			fStructureSelectNextAction.setAction(editor.getAction(StructureSelectionAction.NEXT));
			fStructureSelectPreviousAction.setAction(editor.getAction(StructureSelectionAction.PREVIOUS));
			fStructureSelectHistoryAction.setAction(editor.getAction(StructureSelectionAction.HISTORY));
		}	
		
		fOpenOnSelection.setContentEditor(textEditor);
		fOpenOnTypeSelection.setContentEditor(textEditor);
		
		fAddImportOnSelection.setAction(getAction(textEditor,"AddImportOnSelection")); //$NON-NLS-1$
		fOrganizeImports.setAction(getAction(textEditor, "OrganizeImports")); //$NON-NLS-1$
		fTogglePresentation.setEditor(textEditor);
		fToggleTextHover.setEditor(textEditor);
		fPreviousError.setEditor(textEditor);
		fNextError.setEditor(textEditor);

		TextOperationAction showJavadoc= (TextOperationAction) getAction(textEditor, "ShowJavaDoc");
		fRetargetShowJavadoc.setAction(showJavadoc);		

		OpenExternalJavadocAction openExternalJavadoc= (OpenExternalJavadocAction) getAction(textEditor, "OpenExternalJavadoc");
		openExternalJavadoc.setActivePart(null, textEditor);
		fRetargetOpenExternalJavadoc.setAction(openExternalJavadoc);

		IActionBars bars= getActionBars();		
		bars.setGlobalActionHandler(IJavaEditorActionConstants.OPEN_SELECTION, fOpenOnSelection);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.OPEN_TYPE_HIERARCHY, fOpenOnTypeSelection);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.TOGGLE_PRESENTATION, fTogglePresentation);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.TOGGLE_TEXT_HOVER, fToggleTextHover);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.NEXT_ERROR, fNextError);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.PREVIOUS_ERROR, fPreviousError);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.SHOW_JAVADOC, showJavadoc);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.OPEN_JAVADOC, openExternalJavadoc);

		IWorkbenchPage page = part.getSite().getPage();	
		page.addPartListener(fRetargetOpenOnSelection);		
		page.addPartListener(fRetargetOpenOnTypeSelection);		
		page.addPartListener(fRetargetTogglePresentation);
		page.addPartListener(fRetargetToggleTextHover);
		page.addPartListener(fRetargetNextError);
		page.addPartListener(fRetargetPreviousError);		
		page.addPartListener(fRetargetShowJavadoc);		
		page.addPartListener(fRetargetOpenExternalJavadoc);		
		
		//IAction updateAction= getAction(textEditor, "Display"); //$NON-NLS-1$
		//if (updateAction instanceof IUpdate) {
			//((IUpdate)updateAction).update();
		//}
		//fDisplay.setAction(updateAction); 
		//updateAction= getAction(textEditor, "Inspect"); //$NON-NLS-1$
		//if (updateAction instanceof IUpdate) {
			//((IUpdate)updateAction).update();
		//}
		//fInspect.setAction(updateAction);
	}
}