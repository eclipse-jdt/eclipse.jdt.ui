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

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.search.SearchUsagesInFileAction;

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
			 * @see RetargetAction#propogateChange(PropertyChangeEvent)
			 */
			protected void propogateChange(PropertyChangeEvent event) {
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
	private RetargetTextEditorAction fShowReferencesAction;	
	private RetargetTextEditorAction fGotoMatchingBracket;
	private RetargetTextEditorAction fShowOutline;
	private RetargetTextEditorAction fOpenStructure;
	
	private RetargetAction fRetargetShowJavaDoc;
	private RetargetTextEditorAction fShowJavaDoc;
	



	
	
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
		
		fShowReferencesAction= new RetargetTextEditorAction(b, "ShowReferencesInFile."); //$NON-NLS-1$
		fShowReferencesAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_REFERENCES);
		
		fShowJavaDoc= new RetargetTextEditorAction(b, "ShowJavaDoc."); //$NON-NLS-1$
		fShowJavaDoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		
		fShowOutline= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ShowOutline."); //$NON-NLS-1$
		fShowOutline.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
		
		fOpenStructure= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "OpenStructure."); //$NON-NLS-1$
		fOpenStructure.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE);
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
			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fGotoMatchingBracket);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fRetargetShowJavaDoc);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fShowOutline);
			editMenu.appendToGroup(IWorkbenchActionConstants.FIND_EXT, fShowReferencesAction);
		}
		
		IMenuManager navigateMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
		if (navigateMenu != null) {
			navigateMenu.appendToGroup("open.ext", fOpenStructure); //$NON-NLS-1$
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
		fShowReferencesAction.setAction(getAction(textEditor, SearchUsagesInFileAction.SHOWREFERENCES));
		fShowJavaDoc.setAction(getAction(textEditor, "ShowJavaDoc")); //$NON-NLS-1$
		fShowOutline.setAction(getAction(textEditor, IJavaEditorActionDefinitionIds.SHOW_OUTLINE));
		fOpenStructure.setAction(getAction(textEditor, IJavaEditorActionDefinitionIds.OPEN_STRUCTURE));

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