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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.GoToNextPreviousMemberAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectionAction;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;

/**
 * Common base class for action contributors for Java editors.
 */
public class BasicJavaEditorActionContributor extends BasicTextEditorActionContributor {
	
	
		/**
		 * Extends retarget action to make sure that state required for a toolbar actions is
		 * also copied over from the actual action handler.
		 */
		protected static class RetargetToolbarAction extends RetargetAction {
					
			private String fDefaultLabel;
					
			public RetargetToolbarAction(ResourceBundle bundle, String prefix, String actionId, boolean checkStyle) {
				super(actionId, getLabel(bundle, prefix));
				fDefaultLabel= getText();
				if (checkStyle)
					setChecked(true);
			}
					
			private static String getLabel(ResourceBundle bundle, String prefix) {
				final String labelKey= "label"; //$NON-NLS-1$
				try {
					return bundle.getString(prefix + labelKey);
				} catch (MissingResourceException e) {
					return labelKey;
				}
			}
					
			/*
			 * @see RetargetAction#propagateChange(PropertyChangeEvent)
			 */
			protected void propagateChange(PropertyChangeEvent event) {
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
			
			/*
			 * @see RetargetAction#setActionHandler(IAction)
			 */
			protected void setActionHandler(IAction newHandler) {
						
				// default behavior
				super.setActionHandler(newHandler);
						
				// update all the remaining issues
				if (newHandler != null) {
					setText(newHandler.getText());
					setToolTipText(newHandler.getToolTipText());
					setDescription(newHandler.getDescription());				
					setImageDescriptor(newHandler.getImageDescriptor());
					setHoverImageDescriptor(newHandler.getHoverImageDescriptor());
					setDisabledImageDescriptor(newHandler.getDisabledImageDescriptor());
					setMenuCreator(newHandler.getMenuCreator());
					if (newHandler.getStyle() == IAction.AS_CHECK_BOX)
						setChecked(newHandler.isChecked());
				} else {
					setText(fDefaultLabel);
					setToolTipText(fDefaultLabel);
					setDescription(fDefaultLabel);
					setChecked(false);
				}
			}
		};
		
		
	private List fRetargetToolbarActions= new ArrayList();
	private List fPartListeners= new ArrayList();
	
	private TogglePresentationAction fTogglePresentation;
	private GotoErrorAction fPreviousError;
	private GotoErrorAction fNextError;
	private RetargetTextEditorAction fGotoMatchingBracket;
	private RetargetTextEditorAction fShowOutline;
	private RetargetTextEditorAction fOpenStructure;
	
	private RetargetAction fRetargetShowJavaDoc;
	private RetargetTextEditorAction fShowJavaDoc;
	
	private RetargetTextEditorAction fStructureSelectEnclosingAction;
	private RetargetTextEditorAction fStructureSelectNextAction;
	private RetargetTextEditorAction fStructureSelectPreviousAction;
	private RetargetTextEditorAction fStructureSelectHistoryAction;	

	private RetargetTextEditorAction fGotoNextMemberAction;	
	private RetargetTextEditorAction fGotoPreviousMemberAction;	
	
	public BasicJavaEditorActionContributor() {
		super();
		
		ResourceBundle b= JavaEditorMessages.getResourceBundle();
		
		RetargetAction a= new RetargetToolbarAction(b, "TogglePresentation.", IJavaEditorActionConstants.TOGGLE_PRESENTATION, true); //$NON-NLS-1$
		a.setActionDefinitionId(IJavaEditorActionDefinitionIds.TOGGLE_PRESENTATION);
		JavaPluginImages.setToolImageDescriptors(a, "segment_edit.gif"); //$NON-NLS-1$
		fRetargetToolbarActions.add(a);
		markAsPartListener(a);
		
		a= new RetargetToolbarAction(b, "NextError.", IJavaEditorActionConstants.NEXT_ERROR, false); //$NON-NLS-1$
		a.setActionDefinitionId("org.eclipse.ui.navigate.next"); 
		a.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_NEXT_ERROR);
		fRetargetToolbarActions.add(a);
		markAsPartListener(a);
		
		a= new RetargetToolbarAction(b, "PreviousError.", IJavaEditorActionConstants.PREVIOUS_ERROR, false); //$NON-NLS-1$
		a.setActionDefinitionId("org.eclipse.ui.navigate.previous");
		a.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_PREV_ERROR);
		fRetargetToolbarActions.add(a);
		markAsPartListener(a);
		
		fRetargetShowJavaDoc= new RetargetAction(JdtActionConstants.SHOW_JAVA_DOC, JavaEditorMessages.getString("ShowJavaDoc.label")); //$NON-NLS-1$
		fRetargetShowJavaDoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		markAsPartListener(fRetargetShowJavaDoc);
		
		// actions that are "contributed" to editors, they are considered belonging to the active editor
		fTogglePresentation= new TogglePresentationAction();
		
		fPreviousError= new GotoErrorAction("PreviousError.", false); //$NON-NLS-1$
		fPreviousError.setActionDefinitionId("org.eclipse.ui.navigate.previous");
		fPreviousError.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_PREV_ERROR);
		
		fNextError= new GotoErrorAction("NextError.", true); //$NON-NLS-1$
		fNextError.setActionDefinitionId("org.eclipse.ui.navigate.next");
		fNextError.setImageDescriptor(JavaPluginImages.DESC_TOOL_GOTO_NEXT_ERROR);
		
		fGotoMatchingBracket= new RetargetTextEditorAction(b, "GotoMatchingBracket."); //$NON-NLS-1$
		fGotoMatchingBracket.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_MATCHING_BRACKET);
		
		fShowJavaDoc= new RetargetTextEditorAction(b, "ShowJavaDoc."); //$NON-NLS-1$
		fShowJavaDoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		
		fShowOutline= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ShowOutline."); //$NON-NLS-1$
		fShowOutline.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
		
		fOpenStructure= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "OpenStructure."); //$NON-NLS-1$
		fOpenStructure.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE);
		
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
	
	protected final void markAsPartListener(RetargetAction action) {
		fPartListeners.add(action);
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
		bars.setGlobalActionHandler(ITextEditorActionConstants.NEXT, fNextError);
		bars.setGlobalActionHandler(ITextEditorActionConstants.PREVIOUS, fPreviousError);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.TOGGLE_PRESENTATION, fTogglePresentation);
		// http://dev.eclipse.org/bugs/show_bug.cgi?id=18968
		bars.setGlobalActionHandler(IJavaEditorActionConstants.NEXT_ERROR, fNextError);
		bars.setGlobalActionHandler(IJavaEditorActionConstants.PREVIOUS_ERROR, fPreviousError);
		
		bars.setGlobalActionHandler(JdtActionConstants.SHOW_JAVA_DOC, fShowJavaDoc);
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
			
			MenuManager structureSelection= new MenuManager(JavaEditorMessages.getString("ExpandSelectionMenu.label"), "expandSelection"); //$NON-NLS-1$ //$NON-NLS-2$ 
			structureSelection.add(fStructureSelectEnclosingAction);
			structureSelection.add(fStructureSelectNextAction);
			structureSelection.add(fStructureSelectPreviousAction);
			structureSelection.add(fStructureSelectHistoryAction);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, structureSelection);
			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fRetargetShowJavaDoc);
		}

		IMenuManager navigateMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
		if (navigateMenu != null) {
			navigateMenu.appendToGroup(IWorkbenchActionConstants.SHOW_EXT, fShowOutline);
	}
	
		IMenuManager gotoMenu= menu.findMenuUsingPath("navigate/goTo"); //$NON-NLS-1$
		if (gotoMenu != null) {
			gotoMenu.add(new Separator("additions2"));  //$NON-NLS-1$
			gotoMenu.appendToGroup("additions2", fGotoPreviousMemberAction); //$NON-NLS-1$
			gotoMenu.appendToGroup("additions2", fGotoNextMemberAction); //$NON-NLS-1$
			gotoMenu.appendToGroup("additions2", fGotoMatchingBracket); //$NON-NLS-1$
		}
	}
	
	/*
	 * @see EditorActionBarContributor#contributeToToolBar(IToolBarManager)
	 */
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(new Separator());
		Iterator e= fRetargetToolbarActions.iterator();
		while (e.hasNext())
			tbm.add((IAction) e.next());
	}
	
	/*
	 * @see EditorActionBarContributor#setActiveEditor(IEditorPart)
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
			
		fTogglePresentation.setEditor(textEditor);
		fPreviousError.setEditor(textEditor);
		fNextError.setEditor(textEditor);
		
		fGotoMatchingBracket.setAction(getAction(textEditor, GotoMatchingBracketAction.GOTO_MATCHING_BRACKET));
		fShowJavaDoc.setAction(getAction(textEditor, "ShowJavaDoc")); //$NON-NLS-1$
		fShowOutline.setAction(getAction(textEditor, IJavaEditorActionDefinitionIds.SHOW_OUTLINE));
		fOpenStructure.setAction(getAction(textEditor, IJavaEditorActionDefinitionIds.OPEN_STRUCTURE));

		fStructureSelectEnclosingAction.setAction(getAction(textEditor, StructureSelectionAction.ENCLOSING));
		fStructureSelectNextAction.setAction(getAction(textEditor, StructureSelectionAction.NEXT));
		fStructureSelectPreviousAction.setAction(getAction(textEditor, StructureSelectionAction.PREVIOUS));
		fStructureSelectHistoryAction.setAction(getAction(textEditor, StructureSelectionAction.HISTORY));
				
		fGotoNextMemberAction.setAction(getAction(textEditor, GoToNextPreviousMemberAction.NEXT_MEMBER));
		fGotoPreviousMemberAction.setAction(getAction(textEditor, GoToNextPreviousMemberAction.PREVIOUS_MEMBER));
		
		if (part instanceof JavaEditor) {
			JavaEditor javaEditor= (JavaEditor) part;
			javaEditor.getActionGroup().fillActionBars(getActionBars());
		}
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